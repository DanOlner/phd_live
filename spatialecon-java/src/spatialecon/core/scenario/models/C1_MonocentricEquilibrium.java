package spatialecon.core.scenario.models;

import java.util.List;

import spatialecon.core.config.ModelConfig;
import spatialecon.core.config.Parameter;
import spatialecon.core.engine.World;
import spatialecon.core.record.Aggregation;
import spatialecon.core.record.Recorder;
import spatialecon.core.scenario.Scenario;

/**
 * <b>C1 — the monocentric result.</b> Thesis §5.4.1.
 *
 * <p>500 People arrange themselves around a single central Firm, trading the density cost of
 * being close against the distance cost of being far. Nobody is told to form a settlement.
 *
 * <p>The payoff is the utility column. Density falls off with distance, distance costs rise
 * with it, and the two cancel: <b>utility ends up near-identical for everyone, wherever they
 * are.</b> A spatial equilibrium, with no market, no landlord, no bidding and no agent ever
 * comparing itself to another.
 *
 * <p>Worth being precise about why, because it is easy to over-read. Each Person only ever
 * takes the best option in their own choice set. But every move changes the choice set of
 * whoever decides next — moving in raises others' density, moving out lowers it. The
 * equalisation is an accumulation of those external effects, not a market finding a price.
 * The thesis is careful that this "does not allow agents to barter in the sense that
 * arbitrage might imply", yet reaches the arbitrage-free outcome anyway.
 *
 * <p>See C8 for where this breaks down.
 */
public final class C1_MonocentricEquilibrium implements Scenario {

    @Override public String id()        { return "c1"; }
    @Override public String title()     { return "Monocentric settlement and utility equilibrium"; }
    @Override public String thesisRef() { return "§5.4.1"; }
    @Override public int defaultDays()  { return 1500; }

    @Override
    public String notes() {
        return """
               500 People, one Firm at the centre of a 2x2 plane. Density cost 10, radius
               1/16, distance cost 0.25, no commuting. People mobile.

               Parameters from CEUS1.properties as shipped in the MonoCentricDensity jar,
               except the density multiplier, which that config sets to 100 (the section
               5.4.7 setting). 10 is the section 5.4.1 value and the one that produces the
               trade-off band.

               Watch utility_sd fall towards zero while utility_mean holds steady: People
               are equalising, not converging on a single spot. densityMean should settle
               well above zero -- they are paying for proximity, not avoiding it.

               movementMean is the spatial-equilibrium test of 5.4.3: below 0.0001 the
               settlement is considered settled.""";
    }

    @Override
    public ModelConfig config() {
        return Monocentric.base(10).build();
    }

    @Override
    public void record(Recorder r, World w) {
        // Per-Person, for the distance-vs-utility and distance-vs-density correlations
        // that are this model's signature figures.
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
        return List.of(
                world.densityCostParam(),
                world.deliveryCostParam(),
                world.wageParam());
    }
}
