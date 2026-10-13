package spatialecon.core.scenario.models;

import java.util.List;

import spatialecon.core.config.ModelConfig;
import spatialecon.core.config.Parameter;
import spatialecon.core.engine.World;
import spatialecon.core.record.Aggregation;
import spatialecon.core.record.Recorder;
import spatialecon.core.scenario.Scenario;

/**
 * <b>C8 — where spatial equilibrium stops being utility equilibrium.</b> Thesis §5.4.7.
 *
 * <p>The same monocentric run as C1, with the density multiplier stepped up through 10, 100
 * and 500 across the run. This is the model surprising its author, and the thesis says so.
 *
 * <p>At 10, utility is flat across distance: the clean trade-off of C1. At 100, People on
 * the outskirts start refusing density altogether and take the free space at the edge; a
 * second band appears and utility begins to slope. At 500, nobody accepts any density at
 * all and the settlement disperses completely, utility falling steadily with distance.
 *
 * <p>The mechanism is the viability rule in the Bundle: a candidate whose density cost
 * exceeds the wage is discarded rather than scored negative, so once the multiplier is high
 * enough, every central location simply drops out of consideration.
 *
 * <p>What survives is still a <b>Nash equilibrium</b> — nobody can improve by moving alone.
 * It is no longer a <b>utility equilibrium</b> — some are permanently better off than others,
 * holding good spots that latecomers cannot bid away because there is nothing to bid with.
 * There is also path dependence: who ends up where depends on who moved first.
 *
 * <p>The point for the thesis is sharp. Spatial equilibrium is the assumption urban economics
 * rests on — Glaeser's "no potential for arbitrage across space". It holds here, but only
 * inside a parameter range, and the model finds its own edge.
 */
public final class C8_SpatialNotUtilityEquilibrium implements Scenario {

    private static final double[] STEPS = { 10, 100, 500 };

    @Override public String id()        { return "c8"; }
    @Override public String title()     { return "Spatial equilibrium is not always utility equilibrium"; }
    @Override public String thesisRef() { return "§5.4.7"; }
    @Override public int defaultDays()  { return 3000; }

    @Override
    public String notes() {
        return """
               As c1, but the density multiplier steps 10 -> 100 -> 500 across the run,
               with an equal number of days at each. Ships as the MonoCentricDensity jar.

               Watch utility_sd. Near zero in the first third (everyone equally well off),
               then rising as bands form, and highest at 500 where utility falls steadily
               with distance from the centre.

               Watch densityMean fall towards zero in the final third: nobody is accepting
               any density at all. And watch distanceMean rise -- the settlement is spreading
               to the edges.

               The end state is still a Nash equilibrium: no individual can improve by moving
               alone. It is simply no longer one where everybody does equally well.""";
    }

    @Override
    public ModelConfig config() {
        return Monocentric.base(STEPS[0]).build();
    }

    /** Step the density multiplier at even intervals through the run. */
    @Override
    public void beforeDay(World world, long day) {
        int phase = (int) ((day * STEPS.length) / Math.max(defaultDays(), 1));
        world.config().setDensityCost(STEPS[Math.min(phase, STEPS.length - 1)]);
    }

    @Override
    public void record(Recorder r, World w) {
        r.global("densityMultiplier", () -> w.config().densityCost());

        r.perActor("distanceToCentre", w.people(), p -> w.distance(p, w.firm(0)));
        r.perActor("utility", w.people(), p -> p.utility);
        r.perActor("density", w.people(), p -> p.densityCost);

        r.aggregate("utilityStats", w.people(), p -> p.utility, Aggregation.MEAN_SD);
        r.aggregate("densityStats", w.people(), p -> p.densityCost, Aggregation.MEAN_SD);
        r.aggregate("distanceStats", w.people(), p -> w.distance(p, w.firm(0)), Aggregation.MEAN_SD);
        r.global("movementMean", w::meanPersonMovement);
    }

    @Override
    public List<Parameter> controls(World world) {
        return List.of(world.densityCostParam(), world.deliveryCostParam());
    }
}
