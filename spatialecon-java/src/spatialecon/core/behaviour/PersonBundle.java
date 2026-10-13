package spatialecon.core.behaviour;

import java.util.ArrayList;
import java.util.List;

import spatialecon.core.config.ModelConfig;
import spatialecon.core.econ.Optimiser;
import spatialecon.core.econ.Utility;
import spatialecon.core.engine.Bundle;
import spatialecon.core.engine.Firm;
import spatialecon.core.engine.Person;
import spatialecon.core.engine.Pt;
import spatialecon.core.engine.Space;
import spatialecon.core.engine.World;

/**
 * The Bundle cycle: how a Person acts in the partial models (§4.6.1).
 *
 * <p>Sample some candidate locations, work out what each is worth, move to the best. Repeat
 * until nobody can improve by moving — a Nash equilibrium, reached with no market, no
 * auctioneer and no communication between agents.
 *
 * <p>The order of operations matters and is not obvious:
 *
 * <ol>
 *   <li>Find the best wage reachable from the candidate spot, net of commuting.
 *   <li>Subtract the density cost of living there. This shrinks the <em>budget</em>, so
 *       density is paid before anything is bought.
 *   <li>Price every good delivered to that spot.
 *   <li>Optimise, and score the location by the utility that yields.
 * </ol>
 *
 * <p>A candidate whose density cost exceeds its wage is discarded rather than scored
 * negative — the mechanism behind the dispersal of §5.4.7.
 *
 * <p>Note what is absent: no Person ever compares themselves to another, or knows what
 * another is getting. The spatial equilibrium in §5.4.1 emerges purely from each acting on
 * their own choice set, with every move changing the choice set of whoever decides next.
 * The externality <em>is</em> the mechanism.
 */
public final class PersonBundle implements PersonBehaviour {

    private final int nearbySamples;
    private final double nearbyRadius;
    private final int wideSamples;
    private final Optimiser optimiser;

    /** The original's settings for the partial models: 10 nearby within 0.2, 10 anywhere. */
    public PersonBundle() {
        this(10, 0.2, 10, Optimiser.constrained());
    }

    public PersonBundle(int nearbySamples, double nearbyRadius, int wideSamples,
                        Optimiser optimiser) {
        this.nearbySamples = nearbySamples;
        this.nearbyRadius = nearbyRadius;
        this.wideSamples = wideSamples;
        this.optimiser = optimiser;
    }

    @Override
    public void act(Person person, World world) {
        List<Bundle> candidates = sample(person, world);

        Bundle best = null;
        for (Bundle b : candidates) {
            evaluate(b, person, world);
            if (!b.isViable()) continue;
            if (best == null || b.utility > best.utility) best = b;
        }

        if (best == null) {
            // Nowhere affordable. Stay put and record the failure honestly as zero utility.
            person.utility = 0;
            person.densityCost = 0;
            return;
        }

        person.utility = best.utility;
        person.densityCost = best.densityCost;
        person.neighboursInDensityRadius = best.neighbours;

        buy(best);

        if (world.config().peopleMobile()) {
            person.moveTo(best.location, world.space());
        }
    }

    /**
     * Actually make the purchases and take the job.
     *
     * <p>A Firm's revenue is quantity times its <em>base</em> price: the distance cost is
     * "iceberg", paid by the buyer and melting away rather than arriving as income. So moving
     * closer to buyers raises a Firm's revenue only because they can then afford more — which
     * is the entire signal {@link FirmLocationCrawl} has to work from.
     */
    private void buy(Bundle bundle) {
        for (int i = 0; i < bundle.sellers.size(); i++) {
            double quantity = i < bundle.quantities.length ? bundle.quantities[i] : 0;
            if (quantity <= 0) continue;
            Firm seller = bundle.sellers.get(i);
            seller.revenue += quantity * seller.goodCost;
        }
        if (bundle.employer != null) {
            bundle.employer.contributedTime += bundle.workTime;
        }
    }

    /** Candidate locations: where I am, a few nearby, a few anywhere. */
    private List<Bundle> sample(Person person, World world) {
        List<Bundle> out = new ArrayList<>();
        Space space = world.space();
        Pt here = person.position();

        out.add(new Bundle(Pt.ORIGIN, here));

        if (!world.config().peopleMobile()) return out;

        // Two discrete regions: the only alternative is the other region, so offer that
        // rather than sampling coordinates that would land nowhere.
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

    /** Price everything from this candidate spot and find the utility it supports. */
    private void evaluate(Bundle bundle, Person person, World world) {
        ModelConfig config = world.config();

        findBestWage(bundle, person, world);

        bundle.rawDensityCost = world.rawDensityAt(bundle.location, person);
        bundle.densityCost = config.densityCost() * bundle.rawDensityCost;
        bundle.neighbours = world.countNeighbours(bundle.location, person, config.densityRadius());
        bundle.budget -= bundle.densityCost;

        if (bundle.budget <= 0) return;   // unaffordable; leave it non-viable

        for (Firm firm : world.firms()) {
            // In the von Thünen scenarios a Person may only trade with their assigned Firm.
            if (person.assignedFirm != null && firm != person.assignedFirm) continue;
            bundle.addSeller(firm, firm.priceAtDistance(world.distance(bundle.location, firm)));
        }
        if (!bundle.hasSellers()) return;

        Utility utility = config.utility();
        bundle.quantities = optimiser.optimise(bundle.pricesArray(), bundle.budget, utility, world.rng());

        // Utility is scored on the CLEAN optimum, before any noise is applied. The original
        // did the same (CESOneTypeUtility set the noisy amount on the Good but accumulated
        // utility from the clean value), and it is the right way round: the noise represents
        // imprecision in carrying out the purchase, not in knowing what you wanted. It also
        // matters for what follows — People's utility stays clean while the demand Firms
        // observe does not, which is exactly the asymmetry model d2 is about.
        bundle.utility = utility.utilityOf(bundle.quantities);

        applyDemandNoise(bundle, world);
    }

    /**
     * Perturb the quantities actually bought (§5.5.2).
     *
     * <p>Noise arises naturally when the hill-climber is used — it stops short of the exact
     * optimum and wobbles. Running it for every Person on every turn is expensive, so the
     * thesis adds an explicit noise term to the constrained optimum instead, to get the same
     * effect cheaply. That is what this reproduces.
     *
     * <p>Amplitude is a {@link spatialecon.core.config.Quirks} choice: the original perturbed
     * by ±noise/2 of each quantity, where the thesis text describes ±noise.
     */
    private void applyDemandNoise(Bundle bundle, World world) {
        double noise = world.config().demandNoise();
        if (noise <= 0) return;
        double amplitude = world.config().quirks().noiseIsFullAmplitude() ? 2 * noise : noise;
        for (int i = 0; i < bundle.quantities.length; i++) {
            bundle.quantities[i] += bundle.quantities[i] * world.rng().jiggle(amplitude);
        }
    }

    /**
     * Best wage reachable from this spot.
     *
     * <p>{@code wage × timeLeftAfterCommuting}, maximised over Firms. The round-trip factor
     * is a {@link spatialecon.core.config.Quirks} choice: the thesis states
     * {@code l = t − 2·t_commute}, the original code charged it one way.
     *
     * <p>A Firm more than a full day away (or half a day, with the round trip) leaves no time
     * to work and is simply unreachable.
     */
    private void findBestWage(Bundle bundle, Person person, World world) {
        ModelConfig config = world.config();
        double roundTrip = config.quirks().commuteIsRoundTrip() ? 2 : 1;

        bundle.employer = null;
        bundle.budget = 0;
        bundle.workTime = 0;

        for (Firm firm : world.firms()) {
            if (person.assignedFirm != null && firm != person.assignedFirm) continue;

            double distance = world.distance(bundle.location, firm);
            double timeLeft = Person.TIME_PER_DAY - roundTrip * distance * firm.commuteCost;
            if (timeLeft <= 0) continue;

            double wage = firm.wage * timeLeft;
            if (wage > bundle.budget) {
                bundle.budget = wage;
                bundle.workTime = timeLeft;
                bundle.employer = firm;
            }
        }
    }

    @Override
    public String describe() {
        return "bundle sampling (%d near r=%.3g, %d wide, %s)"
                .formatted(nearbySamples, nearbyRadius, wideSamples, optimiser.describe());
    }
}
