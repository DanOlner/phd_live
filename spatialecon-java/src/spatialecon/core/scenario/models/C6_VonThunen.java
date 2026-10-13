package spatialecon.core.scenario.models;

import java.util.List;

import spatialecon.core.behaviour.PersonBundle;
import spatialecon.core.config.ModelConfig;
import spatialecon.core.config.Parameter;
import spatialecon.core.econ.Ces;
import spatialecon.core.econ.SmoothEoS;
import spatialecon.core.engine.Person;
import spatialecon.core.engine.Placement;
import spatialecon.core.engine.Space;
import spatialecon.core.engine.World;
import spatialecon.core.record.Aggregation;
import spatialecon.core.record.Recorder;
import spatialecon.core.scenario.Scenario;

/**
 * <b>C6 — von Thünen without landlords.</b> Thesis §5.4.5.
 *
 * <p>Four Firms at the centre, each on a different cost level. Every Person is permanently
 * assigned to one of them, a quarter each, and may neither work nor buy elsewhere. They sort
 * themselves into rings.
 *
 * <p>Rings like von Thünen's, from a density cost and almost nothing else — no landlords, no
 * bid-rent curves, no land market, no market of any kind. The thesis' point is parsimony:
 * von Thünen needed a web of assumptions about competitive bidding to get here, and this
 * needs a proximity cost and agents who can tell better from worse.
 *
 * <p>The sting is which way the sorting runs, and that it <b>depends on whether the cost is
 * spatial</b>:
 *
 * <ul>
 *   <li>Vary the <b>wage</b> (non-spatial): the better-paid buy <b>proximity</b>. A higher
 *       wage makes a given density cost proportionally smaller, so they can afford the centre.
 *   <li>Vary the <b>commute cost</b> (spatial): the effectively better-off buy <b>distance</b>.
 *       Cheap travel makes the outskirts affordable, and the outskirts are where space is.
 * </ul>
 *
 * <p>Same agents, same mechanism, opposite sorting — which is the §5.4.3 result made visible.
 * Run {@code c6} for the wage version and {@code c6c} for commuting.
 */
public final class C6_VonThunen implements Scenario {

    private final boolean varyCommute;

    public C6_VonThunen() {
        this(false);
    }

    private C6_VonThunen(boolean varyCommute) {
        this.varyCommute = varyCommute;
    }

    /** The commute-cost variant, where sorting runs the other way. */
    public static Scenario varyingCommute() {
        return new C6_VonThunen(true);
    }

    @Override public String id()        { return varyCommute ? "c6c" : "c6"; }
    @Override public String thesisRef() { return "§5.4.5"; }
    @Override public int defaultDays()  { return 1500; }

    @Override
    public String title() {
        return varyCommute
                ? "Von Thunen without landlords: varying commute cost"
                : "Von Thunen without landlords: varying wage";
    }

    @Override
    public String notes() {
        return varyCommute
                ? """
                  200 People, 4 central Firms. Each Person is tied to one Firm and may not
                  trade elsewhere. Commute cost ranges 0.25, 0.5, 0.75, 1.0 across the Firms
                  (base 0.25, multiplied by Firm index).

                  Watch distance_mean per group: the group with the CHEAPEST commute settles
                  FURTHEST out. Cheap travel buys distance, and distance is where the free
                  space is.

                  Compare with c6, where a higher wage buys proximity instead. Same mechanism,
                  opposite sorting, because one cost is spatial and the other is not."""
                : """
                  500 People, 4 central Firms. Each Person is tied to one Firm and may not
                  trade elsewhere. Wages range 0.5, 1.0, 1.5, 2.0 across the Firms (base 0.5,
                  multiplied by Firm index).

                  Watch distance_mean per group: the best-paid settle CLOSEST in. The density
                  cost is subtracted from the wage, so a bigger wage shrinks it in proportion
                  and makes the centre affordable.

                  Compare with c6c, where cheap commuting buys distance instead.

                  Note: the shipped vonThunen1.properties has base wage 1, which would give
                  1-4 rather than the 0.5-2 the thesis describes. The thesis figure's range
                  is used here.""";
    }

    @Override
    public ModelConfig config() {
        ModelConfig.Builder b = ModelConfig.builder()
                .space(Space.Kind.PLANE).width(2.0)
                .people(varyCommute ? 200 : 500).firms(4)
                .placePeople(Placement.random())
                .placeFirms(Placement.centre())
                .utility(new Ces(varyCommute ? 0.65 : 0.4))
                .production(new SmoothEoS())
                .baseGoodCost(1.0)
                .deliveryCost(varyCommute ? 0.8 : 0.25)
                .densityCost(10)
                .densityRadius(1.0 / 16)
                .personBehaviour(new PersonBundle())
                .peopleMobile(true);

        return varyCommute
                ? b.wage(1.0).commuteCost(0.25).rangeCommute(true).build()
                : b.wage(0.5).commuteCost(0).rangeWage(true).build();
    }

    /** Tie each Person to one Firm, round-robin, as {@code ActorMaker} did. */
    @Override
    public void setUp(World w) {
        int n = w.firms().size();
        for (int i = 0; i < w.people().size(); i++) {
            w.person(i).assignedFirm = w.firm(i % n);
        }
    }

    @Override
    public void record(Recorder r, World w) {
        r.perActor("distanceToCentre", w.people(), p -> w.distance(p, w.firm(0)));
        r.perActor("utility", w.people(), p -> p.utility);
        r.perActor("group", w.people(), p -> p.assignedFirm == null ? -1 : p.assignedFirm.id());

        // One distance and utility series per cost group: the rings, as numbers.
        for (int g = 0; g < w.firms().size(); g++) {
            final int group = g;
            List<Person> members = w.people().stream()
                    .filter(p -> p.assignedFirm == w.firm(group))
                    .toList();
            if (members.isEmpty()) continue;
            r.aggregate("distance_group" + g, members,
                    p -> w.distance(p, w.firm(0)), Aggregation.MEAN_SD);
            r.aggregate("utility_group" + g, members, p -> p.utility, Aggregation.MEAN_SD);
        }

        r.global("movementMean", w::meanPersonMovement);
    }

    @Override
    public List<Parameter> controls(World world) {
        return List.of(world.densityCostParam(), world.deliveryCostParam());
    }
}
