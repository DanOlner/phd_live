package spatialecon.core.scenario.models;

import java.util.List;

import spatialecon.core.behaviour.FirmStockTarget;
import spatialecon.core.config.ModelConfig;
import spatialecon.core.config.Parameter;
import spatialecon.core.engine.Placement;
import spatialecon.core.engine.Space;
import spatialecon.core.engine.World;
import spatialecon.core.record.Aggregation;
import spatialecon.core.record.Recorder;
import spatialecon.core.scenario.Scenario;

/**
 * <b>E1 — oscillation and damping.</b> Thesis §6.3.1.
 *
 * <p>The transmission belt's control problem, in its simplest form. A Firm adjusts price on
 * every sale to hold stock at zero. Targeting stock alone does not work: by the time stock
 * reaches zero the price is at its furthest from equilibrium, so the system sails through and
 * comes back. It is an undamped harmonic oscillator that happens to be made of prices.
 *
 * <p>The fix is control theory rather than economics: target stock, its rate of change, and
 * the change in that rate, all to be zero at once. The thesis notes this is under-damped and
 * that properly tuning it would need the oscillation's angular frequency, which is not
 * reliably detectable — so the response magnitude is simply set by hand (divisor 100).
 *
 * <p>Three variants, each with a value that can be checked by hand:
 *
 * <ul>
 *   <li><b>e1</b> — one Firm, one Person, no distance. Equilibrium price <b>0.5</b>.
 *   <li><b>e1raw</b> — the same with damping off. Settles at the <em>wrong</em> price, because
 *       any point where stock happens to cross zero will do.
 *   <li><b>e1many</b> — 50 Firms, 50 People. Each Firm receives one unit of time, so each also
 *       settles at 0.5 — and every Person's utility is identical, the "umbilically linked"
 *       claim of §6.3.1.
 * </ul>
 */
public final class E1_Damping implements Scenario {

    private final String id;
    private final int firms;
    private final int people;
    private final boolean damped;

    private E1_Damping(String id, int firms, int people, boolean damped) {
        this.id = id;
        this.firms = firms;
        this.people = people;
        this.damped = damped;
    }

    public static Scenario damped()   { return new E1_Damping("e1", 1, 1, true); }
    public static Scenario undamped() { return new E1_Damping("e1raw", 1, 1, false); }
    public static Scenario manyAgents() { return new E1_Damping("e1many", 50, 50, true); }

    @Override public String id()        { return id; }
    @Override public String thesisRef() { return "§6.3.1"; }
    @Override public int defaultDays()  { return 2000; }

    @Override
    public String title() {
        return damped
                ? "Transmission belt: damped stock targeting (%d Firms, %d People)".formatted(firms, people)
                : "Transmission belt: undamped, targeting stock alone";
    }

    @Override
    public String notes() {
        double expected = TransmissionBelt.clearingPrice(people / (double) firms * 1.0);
        return """
               %d Firm(s), %d People, no distance costs. Damping: %s.

               Each Firm receives %.2f units of time, so the stock-clearing price is
               1/(t+1) = %.4f. That is the number to check.

               Watch price converge and |stock| fall to ~0. With damping off, the price still
               stops moving -- but at whatever value stock happened to cross zero at, which is
               not the clearing price. Targeting a level says nothing about arriving there
               with zero momentum.

               In the many-agent variant, watch utility_sd: every Person's utility is
               identical at every instant. They are not merely similar; they move in lockstep,
               because each takes whatever improvement is available at their moment of
               decision.""".formatted(firms, people, damped ? "s + s' + s''" : "s only",
                       people / (double) firms, expected);
    }

    @Override
    public ModelConfig config() {
        return TransmissionBelt.base(firms, people, 0.5)
                .space(Space.Kind.POINT)
                .placeFirms(Placement.centre())
                .placePeople(Placement.centre())
                .firmBehaviour(new FirmStockTarget(FirmStockTarget.DEFAULT_RESPONSE_DIVISOR,
                        damped, damped))
                .build();
    }

    @Override
    public void record(Recorder r, World w) {
        r.aggregate("price", w.firms(), f -> f.goodCost, Aggregation.MEAN_SD);
        r.aggregate("stock", w.firms(), f -> f.stock, Aggregation.MEAN_SD);
        r.aggregate("absStock", w.firms(), f -> Math.abs(f.stock), Aggregation.MEAN);
        r.aggregate("contributedTime", w.firms(), f -> f.contributedTime, Aggregation.MEAN_SD);
        r.aggregate("outputPerUnit", w.firms(), f -> f.outputPerUnitTime, Aggregation.MEAN);
        r.aggregate("utility", w.people(), p -> p.utility, Aggregation.MEAN_SD);
        r.global("clearingPrice", () -> TransmissionBelt.clearingPrice(
                w.firms().stream().mapToDouble(f -> f.contributedTime).average().orElse(0)));
    }

    @Override
    public List<Parameter> controls(World world) {
        return List.of(world.deliveryCostParam());
    }
}
