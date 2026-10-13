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
 * <b>E8 — the transmission belt meets the density cost.</b> Thesis §6.4.2. The closing result.
 *
 * <p>Two Firms a distance of 1 apart, 300 mobile People, ρ raised from 0.5 to 0.9. A
 * more-efficient producer emerges, People migrate to it, and a settlement forms around it —
 * while they continue to buy the other good in from a distance.
 *
 * <p>What makes this the closing result is what it removes. In C7 the same two-core shift
 * happens, but only because the two Firms were <em>given</em> different distance costs by the
 * modeller. Here the Firms are identical in every respect. Which one wins is decided by the
 * model, from an arbitrarily small initial difference in price, amplified by the feedback
 * between demand and production scale. An assumption has been replaced by a mechanism, which
 * is what "endogenous" means and what the whole chapter is for.
 *
 * <p>The EoS curve is set very low (0.001). With 300 People sending demand signals every day,
 * anything larger tips Firms into the runaway of §6.3.2 — a reminder that this model is
 * stable only in a fairly narrow band.
 */
public final class E8_BeltPlusDensity implements Scenario {

    private static final double RHO_FROM = 0.5;
    private static final double RHO_TO = 0.97;

    private final Ces ces = new Ces(RHO_FROM);

    @Override public String id()        { return "e8"; }
    @Override public String title()     { return "Transmission belt plus density cost"; }
    @Override public String thesisRef() { return "§6.4.2"; }
    @Override public int defaultDays()  { return 3000; }

    @Override
    public String notes() {
        return """
               Two identical Firms 1 apart, 300 mobile People, distance cost 0.25, density
               cost 10, EoS curve 0.001. Parameters from TransmissionPlusDensity1.properties
               as shipped. rho rises from %.2f to %.2f.

               Watch efficiencyRatio climb from 1 as one Firm pulls ahead, and meanDistToFirm0
               move as People migrate towards whichever won. The original plots exactly these
               two against each other, using arrows so the direction of model time is visible
               in a scatter -- worth reproducing on the website.

               The Firms are identical. Nothing distinguishes them but an arbitrarily small
               difference in starting price. Which one wins is chance, so re-running with a
               different --seed should sometimes favour the other; the SHAPE is what is
               reproducible, not the winner.

               Compare with c7, where the same two-core outcome required the modeller to hand
               one Firm a higher distance cost. Here that is endogenous.""".formatted(RHO_FROM, RHO_TO);
    }

    @Override
    public ModelConfig config() {
        return TransmissionBelt.base(2, 300, RHO_FROM)
                .utility(ces)
                .space(Space.Kind.PLANE).width(1.0)
                .placeFirms(Placement.twoPointsApart(1.0))
                .placePeople(Placement.random())
                .production(new SmoothEoS(0.001, 1.0))   // low curve: 300 buyers a day
                .deliveryCost(0.25)
                .densityCost(10).densityRadius(1.0 / 16)
                .randomiseInitialPrices(0.45, 0.55)
                .peopleMobile(true)
                .build();
    }

    @Override
    public void beforeDay(World world, long day) {
        double t = day / (double) Math.max(defaultDays() - 1, 1);
        ces.setRho(RHO_FROM + t * (RHO_TO - RHO_FROM));
    }

    @Override
    public void record(Recorder r, World w) {
        r.global("rho", ces::rho);
        r.global("efficiencyRatio", () -> {
            double a = w.firm(0).outputPerUnitTime, b = w.firm(1).outputPerUnitTime;
            double lo = Math.min(a, b), hi = Math.max(a, b);
            return lo <= 0 ? 1 : hi / lo;
        });
        r.global("meanDistToFirm0", () ->
                w.people().stream().mapToDouble(p -> w.distance(p, w.firm(0))).average().orElse(0));
        r.global("firm0Efficiency", () -> w.firm(0).outputPerUnitTime);
        r.global("firm1Efficiency", () -> w.firm(1).outputPerUnitTime);

        r.aggregate("price", w.firms(), f -> f.goodCost, Aggregation.MIN_MAX);
        r.aggregate("utility", w.people(), p -> p.utility, Aggregation.MEAN_SD);
        r.aggregate("density", w.people(), p -> p.densityCost, Aggregation.MEAN_SD);
        r.global("movementMean", w::meanPersonMovement);

        r.perActor("personX", w.people(), p -> p.position().x());
        r.perActor("personY", w.people(), p -> p.position().y());
    }

    @Override
    public List<Parameter> controls(World world) {
        return List.of(ces.rhoParam(), world.deliveryCostParam(), world.densityCostParam());
    }
}
