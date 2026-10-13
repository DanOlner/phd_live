package spatialecon.core.scenario.models;

import java.util.List;

import spatialecon.core.config.ModelConfig;
import spatialecon.core.config.Parameter;
import spatialecon.core.econ.Ces;
import spatialecon.core.econ.Optimiser;
import spatialecon.core.econ.SmoothEoS;
import spatialecon.core.engine.Placement;
import spatialecon.core.engine.Space;
import spatialecon.core.engine.World;
import spatialecon.core.record.Recorder;
import spatialecon.core.scenario.Scenario;

/**
 * <b>B2 — the double dip.</b> Thesis §5.3.1.
 *
 * <p>Same line of ten Firms, but now ρ is held at 0.5 and the <em>distance cost</em> is swept
 * from 0 to 40. Records demand for the nearest few.
 *
 * <p>Demand for a nearby Firm does not fall as distance costs rise. It <em>rises</em> first,
 * as spending is reallocated away from Firms that have become expensive, and only later
 * falls once the absolute cost dominates. Hence the dip.
 *
 * <p>The exception is instructive: a Firm at distance zero pays no distance cost at all, so it
 * absorbs reallocated demand indefinitely and never turns over. Set {@code nearestAtZero} to
 * see it — the run places Firms from 0.1 by default, matching the thesis' first figure, and
 * the toggle reproduces its second.
 *
 * <p>The general lesson is worth stating plainly: raising a cost does not move all agents in
 * the same direction. Some are made better off by a change that makes the system worse off,
 * which is the reason aggregate reasoning about space so often misleads.
 */
public final class B2_DoubleDip implements Scenario {

    private static final int FIRMS = 10;
    private static final int SHOWN = 5;
    private static final double MAX_DISTANCE_COST = 40;

    /** Place the nearest Firm at 0 rather than 0.1, removing its distance cost entirely. */
    private final boolean nearestAtZero;

    private final Ces ces = new Ces(0.5);

    public B2_DoubleDip() {
        this(false);
    }

    private B2_DoubleDip(boolean nearestAtZero) {
        this.nearestAtZero = nearestAtZero;
    }

    /** Variant with the nearest Firm at distance zero, which removes the dip entirely. */
    public static Scenario withFirmAtZero() {
        return new B2_DoubleDip(true);
    }

    @Override public String id()        { return nearestAtZero ? "b2z" : "b2"; }
    @Override public String thesisRef() { return "§5.3.1"; }
    @Override public int defaultDays()  { return 160; }

    @Override
    public String title() {
        return nearestAtZero ? "The double dip: a Firm at distance zero" : "The double dip";
    }

    @Override
    public String notes() {
        return """
               One Person at x=0. Ten Firms spaced from %s to 1. rho fixed at 0.5.
               Sweeps distance cost from 0 to 40.

               Watch demand_0 and demand_1: they RISE before they fall. Spending reallocated
               from Firms that have become expensive outweighs their own rising cost, until
               it does not.

               Scenario b2z places the nearest Firm at 0 instead. It then pays no distance
               cost at all, absorbs reallocated demand indefinitely, and never turns over.

               A rising cost does not push every agent the same way.""".formatted(
                       nearestAtZero ? "0" : "0.1");
    }

    @Override
    public ModelConfig config() {
        return ModelConfig.builder()
                .space(Space.Kind.LINE).width(1.0)
                .people(1).firms(FIRMS)
                .placePeople(Placement.at(0, 0))
                .placeFirms(Placement.evenlyAlongX(nearestAtZero ? 0 : 0.1, 1))
                .utility(ces)
                .production(new SmoothEoS())
                .baseGoodCost(1.0)
                .deliveryCost(0)
                .build();
    }

    @Override
    public void beforeDay(World world, long day) {
        double t = day / (double) Math.max(defaultDays() - 1, 1);
        world.setDeliveryCost(t * MAX_DISTANCE_COST);
    }

    @Override
    public void record(Recorder r, World w) {
        r.global("distanceCost", () -> w.config().deliveryCost());
        for (int i = 0; i < SHOWN; i++) {
            int index = i;
            r.global("demand_" + i, () -> demand(w)[index]);
        }
        r.global("totalDemand", () -> {
            double total = 0;
            for (double v : demand(w)) total += v;
            return total;
        });
    }

    private double[] demand(World w) {
        double[] prices = new double[FIRMS];
        for (int i = 0; i < FIRMS; i++) {
            prices[i] = w.firm(i).priceAtDistance(w.distance(w.person(0), w.firm(i)));
        }
        return Optimiser.constrained().optimise(prices, 1.0, ces, w.rng());
    }

    @Override
    public List<Parameter> controls(World world) {
        return List.of(world.deliveryCostParam(), ces.rhoParam());
    }
}
