package spatialecon.core.scenario.models;

import java.util.Arrays;

import spatialecon.core.config.ModelConfig;
import spatialecon.core.econ.Ces;
import spatialecon.core.econ.Optimiser;
import spatialecon.core.econ.SmoothEoS;
import spatialecon.core.engine.Rng;
import spatialecon.core.engine.Space;
import spatialecon.core.engine.World;
import spatialecon.core.record.Recorder;
import spatialecon.core.scenario.Scenario;

/**
 * <b>A4 — hill-climber against the analytic optimum.</b> Thesis §4.6.1, §5.2.3.
 *
 * <p>Steps the hill-climber's iteration budget upwards and, at each step, runs a batch of
 * optimisations from fresh random starts. Records how close the mean lands to the known
 * answer and how much spread is left.
 *
 * <p>With n equally priced goods and a budget of 1, the analytic optimum is 1/n of each, so
 * every run has a target to be scored against. The two methods agree; the search just takes
 * a thousand iterations and never quite stops wobbling, while the algebra is exact and
 * essentially free.
 *
 * <p>Two details the thesis draws out. Most of the gain arrives almost immediately and the
 * rest of the run buys very little. And more goods converge <em>faster</em> — slicing a
 * budget across many goods leaves each slice less room to vary — which is why this records
 * two good counts at once.
 *
 * <p>The residual spread is not a defect. It is where the demand noise of §5.5.2 comes from,
 * and that noise is what makes Firms misread their own revenue signals.
 */
public final class A4_OptimiserComparison implements Scenario {

    private static final int MAX_ITERATIONS = 2000;
    private static final int BATCH = 200;
    private static final int FEW_GOODS = 2;
    private static final int MANY_GOODS = 8;
    private static final double RHO = 0.5;

    private final Ces ces = new Ces(RHO);
    private int iterations = 1;

    /** Mean and SD of one good's quantity over a batch, plus mean utility. */
    private record Batch(double mean, double sd, double meanUtility) { }

    @Override public String id()        { return "a4"; }
    @Override public String title()     { return "Hill-climber vs constrained optimiser"; }
    @Override public String thesisRef() { return "§4.6.1, §5.2.3"; }
    @Override public int defaultDays()  { return 40; }

    @Override
    public String notes() {
        return """
               Steps the hill-climber's iteration budget from 1 to 2000 across the run,
               running 200 optimisations at each step from fresh random starts.

               All goods cost 1 and the budget is 1, so the analytic optimum is 1/n each:
               0.5 for the 2-good case, 0.125 for the 8-good case. Those are the targets.

               Watch: most of the improvement arrives in the first few iterations and the
               rest of the budget buys very little. Watch also that the 8-good case converges
               FASTER than the 2-good one — more goods means smaller budget slices means less
               room for the random walk to wander.

               sd2 and sd8 never quite reach zero. That residue is exactly the demand noise
               that makes Firms misread their revenue in model d2.""";
    }

    @Override
    public ModelConfig config() {
        return ModelConfig.builder()
                .space(Space.Kind.POINT)
                .people(1).firms(FEW_GOODS)
                .utility(ces)
                .production(new SmoothEoS())
                .baseGoodCost(1.0)
                .build();
    }

    // Each batch is computed once per day and cached. Recording them separately would run
    // a fresh batch per column, so the reported mean and SD would describe different draws.
    private Batch few = new Batch(0, 0, 0);
    private Batch many = new Batch(0, 0, 0);

    @Override
    public void beforeDay(World world, long day) {
        double t = day / (double) Math.max(defaultDays() - 1, 1);
        iterations = Math.max(1, (int) Math.round(t * MAX_ITERATIONS));
        few = batch(FEW_GOODS, world.rng());
        many = batch(MANY_GOODS, world.rng());
    }

    @Override
    public void record(Recorder r, World w) {
        r.global("iterations", () -> iterations);

        // Lambdas, not method references: `few::mean` would bind to whichever Batch object
        // existed when record() ran, and never see the ones beforeDay() installs.
        r.global("analytic2", () -> 1.0 / FEW_GOODS);
        r.global("mean2", () -> few.mean());
        r.global("sd2",   () -> few.sd());
        r.global("absError2", () -> Math.abs(few.mean() - 1.0 / FEW_GOODS));

        r.global("analytic8", () -> 1.0 / MANY_GOODS);
        r.global("mean8", () -> many.mean());
        r.global("sd8",   () -> many.sd());
        r.global("absError8", () -> Math.abs(many.mean() - 1.0 / MANY_GOODS));

        // What the analytic method gets for the same problem, as the ceiling to approach.
        r.global("constrainedUtility2", () -> {
            double[] prices = evenPrices(FEW_GOODS);
            return ces.utilityOf(Optimiser.constrained().optimise(prices, 1.0, ces, w.rng()));
        });
        r.global("hillClimbUtility2", () -> few.meanUtility());
    }

    /** Run a batch of hill-climbs and summarise the first good's quantity. */
    private Batch batch(int goods, Rng rng) {
        Optimiser optimiser = Optimiser.hillClimb(iterations);
        double[] prices = evenPrices(goods);
        double[] firstGood = new double[BATCH];
        double utilitySum = 0;

        for (int i = 0; i < BATCH; i++) {
            double[] q = optimiser.optimise(prices, 1.0, ces, rng);
            firstGood[i] = q[0];
            utilitySum += ces.utilityOf(q);
        }

        double mean = Arrays.stream(firstGood).average().orElse(0);
        double variance = Arrays.stream(firstGood).map(v -> (v - mean) * (v - mean)).sum() / BATCH;
        return new Batch(mean, Math.sqrt(variance), utilitySum / BATCH);
    }

    private static double[] evenPrices(int goods) {
        double[] prices = new double[goods];
        Arrays.fill(prices, 1.0);
        return prices;
    }
}
