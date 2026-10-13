package spatialecon.core.scenario.models;

import java.util.List;

import spatialecon.core.config.ModelConfig;
import spatialecon.core.config.Parameter;
import spatialecon.core.econ.Ces;
import spatialecon.core.econ.Optimiser;
import spatialecon.core.econ.SmoothEoS;
import spatialecon.core.engine.Space;
import spatialecon.core.engine.World;
import spatialecon.core.record.Recorder;
import spatialecon.core.scenario.Scenario;

/**
 * <b>A1 — CES and love of variety.</b> Thesis §2.2.3, §5.2.1.
 *
 * <p>Sweeps ρ from near 0 to 1 and records, at each step, the utility a budget of one buys
 * across a fixed number of equally priced goods.
 *
 * <p>Two things to see. First, ρ carries the entire substitutes-to-variety spectrum on its
 * own: as ρ → 1 utility falls towards the budget itself, because goods become perfect
 * substitutes and a mix is worth no more than the same quantity of any one of them. Second,
 * the quantities never move — with equal prices the optimal split is even for every ρ. What
 * changes is what that same basket is <em>worth</em>.
 *
 * <p>The analytic check: for n equally priced goods and budget Y, utility is
 * {@code Y · n^(1/ρ − 1)}. At ρ = 0.5 with 3 goods and Y = 1 that is exactly 3.
 */
public final class A1_CesExplorer implements Scenario {

    private static final int GOODS = 3;

    private final Ces ces = new Ces(0.99);

    @Override public String id()        { return "a1"; }
    @Override public String title()     { return "CES and love of variety"; }
    @Override public String thesisRef() { return "§2.2.3, §5.2.1"; }
    @Override public int defaultDays()  { return 99; }

    @Override
    public String notes() {
        return """
               Sweeps rho from 0.99 down to 0.01, one step per day.
               Records utility, the per-good quantity, and total spend.

               Watch: utility rises steeply as rho falls (variety is worth more), while the
               quantity bought of each good does not move at all. Spend stays at the budget
               throughout, which is the check that the optimum is genuinely constrained.

               Note the direction of rho: HIGH rho means LOW love of variety.""";
    }

    @Override
    public ModelConfig config() {
        return ModelConfig.builder()
                .space(Space.Kind.POINT)
                .people(1).firms(GOODS)
                .utility(ces)
                .production(new SmoothEoS())
                .baseGoodCost(1.0)
                .deliveryCost(0)
                .build();
    }

    /** Step rho down from 0.99 towards 0.01 across the run. */
    @Override
    public void beforeDay(World world, long day) {
        double t = day / (double) Math.max(defaultDays() - 1, 1);
        ces.setRho(0.99 - t * 0.98);
    }

    @Override
    public void record(Recorder r, World w) {
        Optimiser optimiser = Optimiser.constrained();
        double[] prices = new double[GOODS];
        java.util.Arrays.fill(prices, w.config().baseGoodCost());

        r.global("rho", ces::rho);
        r.global("utility", () -> {
            double[] q = optimiser.optimise(prices, 1.0, ces, w.rng());
            return ces.utilityOf(q);
        });
        r.global("quantityPerGood", () -> optimiser.optimise(prices, 1.0, ces, w.rng())[0]);
        r.global("totalSpend", () -> {
            double[] q = optimiser.optimise(prices, 1.0, ces, w.rng());
            double spend = 0;
            for (int i = 0; i < q.length; i++) spend += q[i] * prices[i];
            return spend;
        });
        // Closed form, for comparison: Y · n^(1/rho − 1)
        r.global("analyticUtility", () -> Math.pow(GOODS, 1 / ces.rho() - 1));
    }

    @Override
    public List<Parameter> controls(World world) {
        return List.of(ces.rhoParam());
    }
}
