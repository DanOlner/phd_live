package spatialecon.core.scenario.models;

import spatialecon.core.config.ModelConfig;
import spatialecon.core.econ.Ces;
import spatialecon.core.econ.CoreModelEoS;
import spatialecon.core.econ.SmoothEoS;
import spatialecon.core.engine.Space;
import spatialecon.core.engine.World;
import spatialecon.core.record.Recorder;
import spatialecon.core.scenario.Scenario;

/**
 * <b>A3 — two ways to have increasing returns.</b> Thesis §2.2.5.
 *
 * <p>Sweeps labour input from 0 to 6 and records total, average and marginal output for both
 * production functions side by side.
 *
 * <p>The smooth form {@code x = vl² + ml} produces something for any positive input, and both
 * its average and marginal output rise. The Core Model form {@code x = (l − α)/β} has a
 * constant marginal by construction, an average that rises at a decreasing rate, and —
 * the point of the exercise — returns <em>negative</em> output for any input below α.
 *
 * <p>Perfectly fine as algebra. Fatal in an agent model, where a Person supplies one unit of
 * labour and α is 3: no individual can ever reach positive output alone, so a Person who
 * reasons correctly about it never works at all. That is §4.7.6, and it is why the thesis
 * uses the smooth function throughout.
 */
public final class A3_ProductionFunctions implements Scenario {

    private static final double MAX_LABOUR = 6.0;

    private final SmoothEoS smooth = new SmoothEoS(1, 1);
    private final CoreModelEoS coreModel = new CoreModelEoS(3, 1);
    private double labour;

    @Override public String id()        { return "a3"; }
    @Override public String title()     { return "Increasing returns: two production functions"; }
    @Override public String thesisRef() { return "§2.2.5"; }
    @Override public int defaultDays()  { return 120; }

    @Override
    public String notes() {
        return """
               Sweeps labour input from 0 to 6, one step per day.

               smooth_*     : x = l^2 + l          (curve 1, magnitude 1)
               coreModel_*  : x = (l - 3) / 1      (alpha 3, beta 1)

               Watch coreModel_total below l = 3: it is negative. Not clamped, because the
               negative region is the thing worth seeing. An agent supplying one unit of
               labour can never produce anything at all on their own, so an agent who
               reasons about it correctly declines to work and production never starts.

               Both marginals are computed numerically, so they apply to any production
               function we add later.""";
    }

    @Override
    public ModelConfig config() {
        return ModelConfig.builder()
                .space(Space.Kind.POINT)
                .people(1).firms(1)
                .utility(new Ces(0.5))
                .production(smooth)
                .build();
    }

    @Override
    public void beforeDay(World world, long day) {
        double t = day / (double) Math.max(defaultDays() - 1, 1);
        labour = t * MAX_LABOUR;
    }

    @Override
    public void record(Recorder r, World w) {
        r.global("labour", () -> labour);

        r.global("smooth_total",    () -> smooth.outputFor(labour));
        r.global("smooth_average",  () -> smooth.outputPerUnit(labour));
        r.global("smooth_marginal", () -> smooth.marginalAt(labour));

        r.global("coreModel_total",    () -> coreModel.outputFor(labour));
        r.global("coreModel_average",  () -> coreModel.outputPerUnit(labour));
        r.global("coreModel_marginal", () -> coreModel.marginalAt(labour));

        // 1 while the Core Model form can produce anything at all, 0 below alpha.
        r.global("coreModel_viable", () -> coreModel.canProduceAt(labour) ? 1 : 0);
    }
}
