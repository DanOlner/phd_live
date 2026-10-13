package spatialecon.core.scenario.models;

import java.util.List;

import spatialecon.core.behaviour.FirmLocationCrawl;
import spatialecon.core.behaviour.PersonBundle;
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
 * <b>D1 — two Firms optimising location on a line.</b> Thesis §5.5.1.
 *
 * <p>Fifty People spread evenly along a line of length one. Two Firms may change
 * <em>only</em> their position — price and wage are fixed at 1 — and do it by the crawl of
 * {@link FirmLocationCrawl}: move, wait a turn, keep going if revenue rose, otherwise revert.
 *
 * <p>Four cases, matching the thesis figures. Registered separately because the outcomes
 * differ qualitatively rather than in degree:
 *
 * <table border="1">
 *   <tr><th>id</th><th>ρ</th><th>distance cost</th><th>expected</th></tr>
 *   <tr><td>d1a</td><td>0.1</td><td>1</td><td>both lock onto a tiny central spot</td></tr>
 *   <tr><td>d1b</td><td>0.99</td><td>1</td><td>central on average, wandering widely</td></tr>
 *   <tr><td>d1c</td><td>0.1</td><td>20</td><td>centre-ish, much more spread</td></tr>
 *   <tr><td>d1d</td><td>0.99</td><td>20</td><td><b>Hotelling-like split to opposite ends</b></td></tr>
 * </table>
 *
 * <p>The thesis' point is that none of this resembles Hotelling's clean deduction. Firms are
 * weak optimisers, their positions wander, and averaging over ten thousand steps hides how
 * erratic the underlying behaviour is. Hence the standard deviation and the barcode of
 * actual positions in the original figures — both are recorded here.
 *
 * <p>Only location is perturbed, as in the original, where the branches for wage and price
 * were written but unreachable. §5.5.2 (model d2) takes up why.
 */
public final class D1_TwoFirmsOnALine implements Scenario {

    /**
     * What the thesis means by "rho = 1".
     *
     * <p>Not 1.0. The original clamped rho to 0.99 in both places it could be changed —
     * {@code UtilityMouseChanger} and {@code CES_Sweeper} — so rho = 1 exactly was never
     * reachable and no published figure was produced at it. Using 1.0 here would take a
     * code path the thesis never ran, and the results differ qualitatively: at 1.0 the Firms
     * co-locate (mean separation 0.05) where at 0.99 they split (0.34). See Quirks.
     */
    private static final double NEAR_SUBSTITUTES = 0.99;

    private final String id;
    private final double rho;
    private final double distanceCost;
    private final String expectation;

    private final Ces ces;

    private D1_TwoFirmsOnALine(String id, double rho, double distanceCost, String expectation) {
        this.id = id;
        this.rho = rho;
        this.distanceCost = distanceCost;
        this.expectation = expectation;
        this.ces = new Ces(rho);
    }

    public static Scenario highVarietyNearMarket() {
        return new D1_TwoFirmsOnALine("d1a", 0.1, 1,
                "both lock onto a tiny central spot");
    }

    public static Scenario substitutesNearMarket() {
        return new D1_TwoFirmsOnALine("d1b", NEAR_SUBSTITUTES, 1,
                "central on average, but wandering the whole line");
    }

    public static Scenario highVarietyFarMarket() {
        return new D1_TwoFirmsOnALine("d1c", 0.1, 20,
                "centre-ish, considerably more spread out");
    }

    public static Scenario substitutesFarMarket() {
        return new D1_TwoFirmsOnALine("d1d", NEAR_SUBSTITUTES, 20,
                "Hotelling-like split towards opposite ends");
    }

    @Override public String id()        { return id; }
    @Override public String thesisRef() { return "§5.5.1"; }
    @Override public int defaultDays()  { return 10_000; }

    @Override
    public String title() {
        return "Two Firms on a line (rho=%.2f, distance cost=%.0f)".formatted(rho, distanceCost);
    }

    @Override
    public String notes() {
        return """
               50 People evenly spread on a line of length 1, fixed. Two mobile Firms, price
               and wage fixed at 1, changing position only.

               rho = %.2f, distance cost = %.0f.
               Expected: %s.

               firmX_0 / firmX_1 are the Firms' positions each day -- the raw material for
               the original's 'barcode' of attempted locations. firmSpread_mean/sd give the
               averaged view; compare the two, because the mean is much tidier than the
               behaviour underneath it.

               Note the Firms never settle. The crawl always perturbs, so a Firm that has
               found a good spot keeps testing and keeps giving ground. That restlessness is
               the finding, not noise in the measurement.""".formatted(rho, distanceCost, expectation);
    }

    @Override
    public ModelConfig config() {
        return ModelConfig.builder()
                .space(Space.Kind.LINE).width(1.0)
                .people(50).firms(2)
                .placePeople(Placement.evenlyAlongX(0, 1))
                .placeFirms(Placement.evenlyAlongX(0.25, 0.75))
                .utility(ces)
                .production(new SmoothEoS())
                .baseGoodCost(1.0)
                .wage(1.0)
                .commuteCost(0)
                .deliveryCost(distanceCost)
                .densityCost(0)
                .personBehaviour(new PersonBundle())
                .firmBehaviour(new FirmLocationCrawl())
                .peopleMobile(false)
                .firmsMobile(true)
                .build();
    }

    @Override
    public void record(Recorder r, World w) {
        r.global("firmX_0", () -> w.firm(0).position().x());
        r.global("firmX_1", () -> w.firm(1).position().x());
        r.global("separation", () -> Math.abs(w.firm(0).position().x() - w.firm(1).position().x()));
        r.aggregate("firmSpread", w.firms(), f -> f.position().x(), Aggregation.MEAN_SD);
        r.aggregate("firmRevenue", w.firms(), f -> f.revenueLastTurn, Aggregation.MEAN_SD);

        // Demand across the line, for the profile drawn above and below the axis.
        r.perActor("personX", w.people(), p -> p.position().x());
        r.perActor("personUtility", w.people(), p -> p.utility);
    }

    @Override
    public List<Parameter> controls(World world) {
        return List.of(ces.rhoParam(), world.deliveryCostParam());
    }
}
