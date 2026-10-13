package spatialecon.core.scenario.models;

import java.util.List;

import spatialecon.core.config.ModelConfig;
import spatialecon.core.config.Parameter;
import spatialecon.core.econ.Ces;
import spatialecon.core.econ.SmoothEoS;
import spatialecon.core.engine.Placement;
import spatialecon.core.engine.Space;
import spatialecon.core.engine.World;
import spatialecon.core.record.Aggregation;
import spatialecon.core.record.Recorder;
import spatialecon.core.scenario.Scenario;

/**
 * <b>E5 / E6 — the transmission belt in two regions.</b> Thesis §6.4.
 *
 * <p>Two discrete regions a fixed distance apart, one Person in each. <b>e5</b> has one Firm
 * per region; <b>e6</b> has two, which lets each region develop its own internal structure.
 *
 * <p>Both ρ and distance cost can trigger the same regime change, from opposite directions.
 * Raising ρ makes goods substitutable enough that efficiency beats variety and one producer
 * takes over. Raising distance cost undoes it, because reaching the distant efficient Firm
 * costs more than the efficiency is worth. The run threads back and forth across the boundary
 * by alternately raising each, mapping the line between the regimes.
 *
 * <p>The four-Firm variant is the one the Core Model cannot match: production structure varies
 * <em>within</em> a region as well as between them, and it does so as a result of model-wide
 * interaction. A model whose regions are dimensionless points has no inside for structure to
 * live in.
 */
public final class E5_TwoRegions implements Scenario {

    private final String id;
    private final int firmsPerRegion;

    private final Ces ces;
    private double distanceCost = 0.1;
    private boolean raisingRho = true;
    private long dwellUntil = 0;

    /** Break and restore thresholds differ, so noise near the boundary cannot flip it. */
    private static final double BREAK_ABOVE = 1.15;
    private static final double RESTORE_BELOW = 1.02;

    /** Days to hold still after a crossing, so oscillations damp before sweeping again. */
    private static final int DWELL = 400;

    private E5_TwoRegions(String id, int firmsPerRegion, double rho) {
        this.id = id;
        this.firmsPerRegion = firmsPerRegion;
        this.ces = new Ces(rho);
    }

    public static Scenario oneFirmEach()  { return new E5_TwoRegions("e5", 1, 0.65); }
    public static Scenario twoFirmsEach() { return new E5_TwoRegions("e6", 2, 0.50); }

    @Override public String id()        { return id; }
    @Override public String thesisRef() { return "§6.4"; }
    @Override public int defaultDays()  { return 8000; }

    @Override
    public String title() {
        return "Transmission belt, two regions (%d Firm%s each)"
                .formatted(firmsPerRegion, firmsPerRegion == 1 ? "" : "s");
    }

    @Override
    public String notes() {
        return """
               Two regions separated by the space width, one Person in each, %d Firm(s) per
               region. Parameters from TwoRegionTransmission.properties as shipped in the
               %s jar.

               The run threads across the regime boundary: rho rises until symmetry breaks,
               then distance cost rises until it is restored, then rho again. Each crossing
               is a point on the line dividing the two regimes.

               Watch efficiencyRatio: 1.0 while production is even, above 1 once a producer
               dominates. Watch which variable was moving when it crossed -- both rho and
               distance cost can trigger the same change, from opposite directions.

               With two Firms per region, watch the per-region efficiency columns separately.
               Regions can differ from each other AND internally, which a model treating a
               region as a dimensionless point cannot represent.""".formatted(
                       firmsPerRegion, firmsPerRegion == 1 ? "SymmetryBreak" : "TwoRegionFourFirms");
    }

    @Override
    public ModelConfig config() {
        int firms = firmsPerRegion * 2;
        return TransmissionBelt.base(firms, 2, ces.rho())
                .utility(ces)
                .space(Space.Kind.TWO_REGION).width(1.0)
                .placePeople(Placement.splitBetweenRegions(0.5))
                .placeFirms(Placement.splitBetweenRegions(0.5))
                .production(new SmoothEoS(0.5, 0.5))     // as shipped: curve and magnitude 0.5
                .deliveryCost(distanceCost)
                .randomiseInitialPrices(0.55, 0.75)
                .build();
    }

    /**
     * Alternate between raising ρ and raising distance cost, switching whenever the model
     * crosses between regimes. This is the "thread" of the original's parameter sweep.
     */
    @Override
    public void beforeDay(World world, long day) {
        if (day < 300) return;                       // let the initial prices settle
        if (day < dwellUntil) return;                // holding still after a crossing

        double ratio = efficiencyRatio(world);
        if (raisingRho && ratio > BREAK_ABOVE) {
            raisingRho = false;
            dwellUntil = day + DWELL;
            return;
        }
        if (!raisingRho && ratio < RESTORE_BELOW) {
            raisingRho = true;
            dwellUntil = day + DWELL;
            return;
        }

        if (raisingRho) {
            ces.setRho(Math.min(0.99, ces.rho() + 0.00006));
        } else {
            distanceCost += 0.00006;
            world.setDeliveryCost(distanceCost);
        }
    }

    private static double efficiencyRatio(World w) {
        double lo = Double.MAX_VALUE, hi = 0;
        for (var f : w.firms()) {
            lo = Math.min(lo, f.outputPerUnitTime);
            hi = Math.max(hi, f.outputPerUnitTime);
        }
        return lo <= 0 ? 1 : hi / lo;
    }

    @Override
    public void record(Recorder r, World w) {
        r.global("rho", ces::rho);
        r.global("distanceCost", () -> w.config().deliveryCost());
        r.global("efficiencyRatio", () -> efficiencyRatio(w));
        r.global("threadingRho", () -> raisingRho ? 1 : 0);
        r.aggregate("efficiency", w.firms(), f -> f.outputPerUnitTime, Aggregation.MIN_MAX);
        r.aggregate("price", w.firms(), f -> f.goodCost, Aggregation.MIN_MAX);
        r.aggregate("contributedTime", w.firms(), f -> f.contributedTime, Aggregation.MIN_MAX);
        r.aggregate("utility", w.people(), p -> p.utility, Aggregation.MEAN_SD);
        r.perActor("firmEfficiency", w.firms(), f -> f.outputPerUnitTime);
    }

    @Override
    public List<Parameter> controls(World world) {
        return List.of(ces.rhoParam(), world.deliveryCostParam());
    }
}
