package spatialecon.core.behaviour;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import spatialecon.core.econ.Optimiser;
import spatialecon.core.econ.Utility;
import spatialecon.core.engine.Bundle;
import spatialecon.core.engine.Firm;
import spatialecon.core.engine.Person;
import spatialecon.core.engine.Pt;
import spatialecon.core.engine.Space;
import spatialecon.core.engine.World;

/**
 * People buy goods directly with time — the demand side of the transmission belt (§6.3).
 *
 * <p>No money, no wage, no separate labour market. A Person's day <em>is</em> the budget, and
 * the goods they get for it <em>are</em> the wage. The thesis arrives here after finding that
 * treating money as a real, conserved quantity produces runaway inequality dynamics that
 * swamp everything else (§4.7.2), and after establishing that the spatial distinction that
 * matters is between spatial and non-spatial costs rather than between goods and wages.
 *
 * <p>Two mechanisms make economies of scale work without anyone needing a strategy:
 *
 * <ul>
 *   <li><b>Time is left behind.</b> A Person's input stays with each Firm until their next
 *       turn, when they withdraw it and decide afresh. So a Firm's total input reflects
 *       everyone who has bought recently, not just whoever is buying now — which is what lets
 *       production run at a scale no individual chose.
 *   <li><b>Buying through a CES means buying from everyone.</b> Every Firm gets some share of
 *       every Person's day, so no Firm is ever starved by a marginal price difference. That
 *       sidesteps Bertrand competition, where the cheapest would take everything.
 * </ul>
 *
 * <p>Each purchase triggers the Firm's reflex, so prices move continuously rather than once a
 * turn.
 */
public final class PersonTransmission implements PersonBehaviour {

    private final int nearbySamples;
    private final double nearbyRadius;
    private final int wideSamples;
    private final Optimiser optimiser;

    /** What each Person currently has invested in each Firm, to withdraw next turn. */
    private final Map<Integer, Map<Integer, Double>> leftWith = new HashMap<>();

    /** The original's settings for the transmission models: 3 nearby within 0.2, 5 anywhere. */
    public PersonTransmission() {
        this(3, 0.2, 5, Optimiser.constrained());
    }

    public PersonTransmission(int nearbySamples, double nearbyRadius, int wideSamples,
                              Optimiser optimiser) {
        this.nearbySamples = nearbySamples;
        this.nearbyRadius = nearbyRadius;
        this.wideSamples = wideSamples;
        this.optimiser = optimiser;
    }

    @Override
    public void act(Person person, World world) {
        withdrawPreviousInput(person, world);

        Bundle best = null;
        for (Bundle candidate : sample(person, world)) {
            evaluate(candidate, person, world);
            if (!candidate.isViable()) continue;
            if (best == null || candidate.utility > best.utility) best = candidate;
        }
        if (best == null) {
            person.utility = 0;
            return;
        }

        person.utility = best.utility;
        person.densityCost = best.densityCost;
        person.neighboursInDensityRadius = best.neighbours;

        buy(best, person, world);

        if (world.config().peopleMobile()) {
            person.moveTo(best.location, world.space());
        }
    }

    /**
     * Take back the time left with Firms last turn.
     *
     * <p>Must happen before anything is priced, or the Person's own stale contribution would
     * still be inflating the production scale they are about to evaluate.
     */
    private void withdrawPreviousInput(Person person, World world) {
        Map<Integer, Double> mine = leftWith.get(person.id());
        if (mine == null) return;
        for (Firm firm : world.firms()) {
            Double amount = mine.get(firm.id());
            if (amount == null) continue;
            firm.contributedTime -= amount;
            // Floating point can leave a whisker below zero; the original clamped it too.
            if (firm.contributedTime < 0 && firm.contributedTime > -1e-9) firm.contributedTime = 0;
        }
        mine.clear();
    }

    /** Candidate locations. Most transmission runs hold People still, giving just one. */
    private List<Bundle> sample(Person person, World world) {
        List<Bundle> out = new ArrayList<>();
        Space space = world.space();
        Pt here = person.position();
        out.add(new Bundle(Pt.ORIGIN, here));

        if (!world.config().peopleMobile()) return out;

        if (space.kind() == Space.Kind.TWO_REGION) {
            double otherX = here.x() == space.regionZeroX() ? space.regionOneX() : space.regionZeroX();
            out.add(new Bundle(new Pt(otherX - here.x(), 0), new Pt(otherX, 0)));
            return out;
        }

        for (int i = 0; i < nearbySamples; i++) {
            Pt offset = space.kind() == Space.Kind.LINE
                    ? new Pt(world.rng().between(-nearbyRadius, nearbyRadius), 0)
                    : world.rng().pointInCircle(nearbyRadius);
            out.add(new Bundle(offset, space.confine(here.plus(offset))));
        }
        for (int i = 0; i < wideSamples; i++) {
            Pt anywhere = space.kind() == Space.Kind.LINE
                    ? new Pt(world.rng().upTo(space.width()), 0)
                    : new Pt(world.rng().upTo(space.width()), world.rng().upTo(space.width()));
            Pt confined = space.confine(anywhere);
            out.add(new Bundle(new Pt(confined.x() - here.x(), confined.y() - here.y()), confined));
        }
        return out;
    }

    /** Price every Firm from this spot and find the best use of one day. */
    private void evaluate(Bundle bundle, Person person, World world) {
        bundle.budget = Person.TIME_PER_DAY;

        if (world.config().densityCost() > 0) {
            bundle.rawDensityCost = world.rawDensityAt(bundle.location, person);
            bundle.densityCost = world.config().densityCost() * bundle.rawDensityCost;
            bundle.neighbours = world.countNeighbours(bundle.location, person,
                    world.config().densityRadius());
            bundle.budget -= bundle.densityCost;
        }
        if (bundle.budget <= 0) return;

        for (Firm firm : world.firms()) {
            bundle.addSeller(firm, firm.priceAtDistance(world.distance(bundle.location, firm)));
        }
        if (!bundle.hasSellers()) return;

        Utility utility = world.config().utility();
        bundle.quantities = optimiser.optimise(bundle.pricesArray(), bundle.budget, utility,
                world.rng());
        bundle.utility = utility.utilityOf(bundle.quantities);
    }

    /**
     * Pay for the goods, and trigger each Firm's price reflex.
     *
     * <p>What the Firm receives as productive input is {@code quantity × base price} — not
     * what the Person paid. The difference is the distance cost, which melts away: the buyer
     * spends it and nobody receives it.
     */
    private void buy(Bundle bundle, Person person, World world) {
        Map<Integer, Double> mine = leftWith.computeIfAbsent(person.id(), id -> new HashMap<>());

        for (int i = 0; i < bundle.sellers.size(); i++) {
            double units = i < bundle.quantities.length ? bundle.quantities[i] : 0;
            if (units <= 0) continue;

            Firm seller = bundle.sellers.get(i);
            double input = units * seller.goodCost;   // iceberg: delivery melts away

            seller.revenue += input;
            mine.merge(seller.id(), input, Double::sum);

            world.config().firmBehaviour().onSale(seller, units, input, world);
        }
    }

    @Override
    public String describe() {
        return "buys goods with time (%d near r=%.3g, %d wide, %s)"
                .formatted(nearbySamples, nearbyRadius, wideSamples, optimiser.describe());
    }
}
