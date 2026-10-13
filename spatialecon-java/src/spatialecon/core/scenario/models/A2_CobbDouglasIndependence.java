package spatialecon.core.scenario.models;

import java.util.List;

import spatialecon.core.config.ModelConfig;
import spatialecon.core.config.Parameter;
import spatialecon.core.econ.SmoothEoS;
import spatialecon.core.econ.StoneGeary;
import spatialecon.core.engine.Space;
import spatialecon.core.engine.World;
import spatialecon.core.record.Recorder;
import spatialecon.core.scenario.Scenario;

/**
 * <b>A2 — Cobb–Douglas and the Firm with no lever.</b> Thesis §2.2.2, §5.2.2.
 *
 * <p>Two Firms. Firm 0 holds its price at 1; Firm 1 sweeps its own from 0.2 to 3. Records
 * demand for both.
 *
 * <p>Demand for Firm 0's good does not move. Not approximately — exactly, to the last
 * decimal. And Firm 1's revenue does not move either: it is unit elastic, so every price cut
 * is met by precisely enough extra demand to leave takings unchanged.
 *
 * <p>Which means a Firm facing Cobb–Douglas demand has no lever to pull. It cannot improve
 * its position by pricing, and — since space enters only through price — it has no reason to
 * move either. The choice of utility function has quietly disabled a whole class of agent
 * behaviour before any agent has done anything.
 *
 * <p>The thesis makes a further point here worth carrying onto the website. An agent model
 * would eventually <em>reveal</em> this independence, but could never explain it. The
 * explanation is one line of algebra: {@code p_M} simply does not appear in demand for
 * {@code F}. Simulation is not always the tool with the most explanatory power.
 */
public final class A2_CobbDouglasIndependence implements Scenario {

    private static final double FIRM0_PRICE = 1.0;
    private static final double SWEEP_FROM = 0.2;
    private static final double SWEEP_TO = 3.0;

    private final StoneGeary cobbDouglas = StoneGeary.cobbDouglas(0.5);
    private double firm1Price = SWEEP_FROM;

    @Override public String id()        { return "a2"; }
    @Override public String title()     { return "Cobb-Douglas: the Firm with no lever"; }
    @Override public String thesisRef() { return "§2.2.2, §5.2.2"; }
    @Override public int defaultDays()  { return 100; }

    @Override
    public String notes() {
        return """
               Firm 0 holds price at 1.0. Firm 1 sweeps price from 0.2 to 3.0.

               Watch demand0: it is flat. Changing one price cannot affect demand for the
               other good, because the other price does not appear in its demand equation.
               Watch revenue1: also flat. Unit elastic — the price cut is exactly cancelled
               by the extra demand.

               A Firm here cannot improve its revenue by pricing, so it also has no spatial
               incentive: distance enters only through price.""";
    }

    @Override
    public ModelConfig config() {
        return ModelConfig.builder()
                .space(Space.Kind.POINT)
                .people(1).firms(2)
                .utility(cobbDouglas)
                .production(new SmoothEoS())
                .baseGoodCost(FIRM0_PRICE)
                .build();
    }

    @Override
    public void beforeDay(World world, long day) {
        double t = day / (double) Math.max(defaultDays() - 1, 1);
        firm1Price = SWEEP_FROM + t * (SWEEP_TO - SWEEP_FROM);
    }

    @Override
    public void record(Recorder r, World w) {
        r.global("firm1Price", () -> firm1Price);
        r.global("demand0", () -> demand()[0]);
        r.global("demand1", () -> demand()[1]);
        r.global("revenue0", () -> demand()[0] * FIRM0_PRICE);
        r.global("revenue1", () -> demand()[1] * firm1Price);
        r.global("utility", () -> cobbDouglas.utilityOf(demand()));
    }

    private double[] demand() {
        return cobbDouglas.optimalQuantities(new double[]{ FIRM0_PRICE, firm1Price }, 1.0);
    }

    @Override
    public List<Parameter> controls(World world) {
        return List.of(
                cobbDouglas.deltaParam(),
                Parameter.of("p1", () -> firm1Price, v -> firm1Price = v)
                        .range(0.05, 5).label("Firm 1 price"));
    }
}
