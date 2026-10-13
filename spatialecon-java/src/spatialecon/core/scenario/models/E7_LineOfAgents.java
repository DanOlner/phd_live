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
 * <b>E7 — nineteen Firms and nineteen People on a line.</b> Thesis §6.4.1.
 *
 * <p>Matched pairs at nineteen evenly spaced positions, so one pair sits dead centre. Distance
 * cost selects how many efficient producers emerge and where — one, two, or many.
 *
 * <p>Before any transition, the centre is already advantaged: an agent in the middle can reach
 * more Firms cheaply, so gets more utility, and the central Firm receives the most input.
 * Geography alone produces inequality, with no increasing returns needed.
 *
 * <p>The welfare result is the one worth putting on a website. Transitions to more efficient
 * production <b>never make any individual Person worse off</b>. Aggregate welfare rises and
 * nobody pays for it: larger-scale production is cheaper for everyone within reach, and those
 * out of reach are simply unaffected. That is a positive externality, and it is unusual —
 * most structural change in economics creates losers.
 */
public final class E7_LineOfAgents implements Scenario {

    private static final int AGENTS = 19;
    private static final double DISTANCE_FROM = 8.0;
    private static final double DISTANCE_TO = 3.5;

    private final Ces ces = new Ces(0.7);

    @Override public String id()        { return "e7"; }
    @Override public String title()     { return "Nineteen Firms and People on a line"; }
    @Override public String thesisRef() { return "§6.4.1"; }
    @Override public int defaultDays()  { return 6000; }

    @Override
    public String notes() {
        return """
               19 Firms and 19 People at matched positions on a line of length 1. rho 0.7,
               EoS curve and magnitude 0.1, as shipped in 1DLineManyAgentsTransmission.

               Distance cost falls from %.1f to %.1f across the run. Watch efficiencyRatio
               and efficiency_max: at high distance costs production is spread evenly, and as
               costs fall the centre takes off.

               Before any transition, look at utility across People: the middle is already
               better off, because it can reach more Firms cheaply. Geography produces
               inequality on its own.

               The welfare claim to check is minUtility. It should not fall when a transition
               happens. Efficient production is a positive externality -- cheaper goods for
               everyone in reach, and no effect on anyone else.""".formatted(DISTANCE_FROM, DISTANCE_TO);
    }

    @Override
    public ModelConfig config() {
        return TransmissionBelt.base(AGENTS, AGENTS, 0.7)
                .utility(ces)
                .space(Space.Kind.LINE).width(1.0)
                .placePeople(Placement.evenlyAlongX(0, 1))
                .placeFirms(Placement.evenlyAlongX(0, 1))
                .production(new SmoothEoS(0.1, 0.1))     // as shipped
                .deliveryCost(DISTANCE_FROM)
                .randomiseInitialPrices(0.3, 0.7)
                .build();
    }

    @Override
    public void beforeDay(World world, long day) {
        if (day < 500) return;                    // settle first
        double t = (day - 500) / (double) Math.max(defaultDays() - 500, 1);
        world.setDeliveryCost(DISTANCE_FROM + t * (DISTANCE_TO - DISTANCE_FROM));
    }

    @Override
    public void record(Recorder r, World w) {
        r.global("distanceCost", () -> w.config().deliveryCost());
        r.global("efficiencyRatio", () -> {
            double lo = Double.MAX_VALUE, hi = 0;
            for (var f : w.firms()) {
                lo = Math.min(lo, f.outputPerUnitTime);
                hi = Math.max(hi, f.outputPerUnitTime);
            }
            return lo <= 0 ? 1 : hi / lo;
        });
        r.global("centralFirmEfficiency", () -> w.firm(AGENTS / 2).outputPerUnitTime);
        r.global("minUtility", () -> w.people().stream().mapToDouble(p -> p.utility).min().orElse(0));

        r.aggregate("efficiency", w.firms(), f -> f.outputPerUnitTime, Aggregation.MIN_MAX);
        r.aggregate("utility", w.people(), p -> p.utility, Aggregation.MEAN_SD);
        r.aggregate("price", w.firms(), f -> f.goodCost, Aggregation.MIN_MAX);

        // Per-position, for the profile drawn above and below the line in the original.
        r.perActor("firmEfficiency", w.firms(), f -> f.outputPerUnitTime);
        r.perActor("personUtility", w.people(), p -> p.utility);
    }

    @Override
    public List<Parameter> controls(World world) {
        return List.of(ces.rhoParam(), world.deliveryCostParam());
    }
}
