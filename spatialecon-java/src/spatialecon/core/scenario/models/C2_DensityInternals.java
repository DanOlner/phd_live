package spatialecon.core.scenario.models;

import java.util.List;

import spatialecon.core.config.ModelConfig;
import spatialecon.core.config.Parameter;
import spatialecon.core.engine.World;
import spatialecon.core.record.Aggregation;
import spatialecon.core.record.Recorder;
import spatialecon.core.scenario.Scenario;

/**
 * <b>C2 — inside the density cost.</b> Thesis §5.4.2.
 *
 * <p>The same run as C1, recorded one level down: how many neighbours each Person actually
 * has inside their radius, and how that count varies with distance from the centre.
 *
 * <p>The count rises towards the centre, which is the trade-off made concrete — People
 * accept more neighbours in exchange for shorter delivery. Meanwhile the density cost each
 * one pays stays remarkably even, because that is what they are equalising.
 *
 * <p>The original also produced a striking hexagonal packing here, People finding
 * interstitial gaps to minimise overlap. The thesis is careful to call it "economically
 * uninteresting" — a consequence of a uniform radius rule, not of anything economic. Worth
 * showing on the website precisely because it looks meaningful and is not: a good, cheap
 * lesson in reading emergent pattern.
 */
public final class C2_DensityInternals implements Scenario {

    @Override public String id()        { return "c2"; }
    @Override public String title()     { return "Inside the density cost"; }
    @Override public String thesisRef() { return "§5.4.2"; }
    @Override public int defaultDays()  { return 1500; }

    @Override
    public String notes() {
        return """
               Same run as c1, recorded one level down.

               neighbours_N is the discrete count inside each Person's density radius.
               Correlate it against distanceToCentre: the count rises towards the centre.

               That is the trade-off in raw form -- more neighbours accepted in exchange for
               cheaper delivery -- while the density COST each Person pays stays even,
               because that is the quantity being equalised.

               The original also shows a hexagonal packing emerging here. It is a consequence
               of a uniform radius, not of anything economic; the thesis calls it
               'economically uninteresting' and it is worth showing as exactly that.""";
    }

    @Override
    public ModelConfig config() {
        return Monocentric.base(10).build();
    }

    @Override
    public void record(Recorder r, World w) {
        r.perActor("distanceToCentre", w.people(), p -> w.distance(p, w.firm(0)));
        r.perActor("neighbours", w.people(), p -> p.neighboursInDensityRadius);
        r.perActor("density", w.people(), p -> p.densityCost);

        r.aggregate("neighbourStats", w.people(), p -> p.neighboursInDensityRadius, Aggregation.MEAN_SD);
        r.aggregate("neighbourRange", w.people(), p -> p.neighboursInDensityRadius, Aggregation.MIN_MAX);
        r.aggregate("densityStats", w.people(), p -> p.densityCost, Aggregation.MEAN_SD);
        r.global("movementMean", w::meanPersonMovement);
    }

    @Override
    public List<Parameter> controls(World world) {
        return List.of(world.densityCostParam(), world.deliveryCostParam());
    }
}
