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
 * <b>B3 — the black hole.</b> Thesis §5.3.2.
 *
 * <p>20 People and 4 Firms, everyone mobile, random start, <b>no density cost</b>. Records
 * mean nearest-neighbour distance for each group over time.
 *
 * <p>Everything collapses onto a point. Distance is a cost like any other, so if nothing
 * opposes it, agents remove it — and in removing it they abolish space itself. This is the
 * result that motivates everything after it: the density cost of group C exists to give
 * distance something to trade against.
 *
 * <p>The second thing to watch is the <b>asymmetry in how fast they converge</b>. People
 * collapse quickly: each evaluates candidate locations directly and can see which is better.
 * Firms take far longer, because a Firm has no view of the map at all — it moves, waits, and
 * reads one number. That gap between deciding-with-information and groping-by-feedback is the
 * subject of group D, and it shows up here for free.
 */
public final class B3_BlackHole implements Scenario {

    @Override public String id()        { return "b3"; }
    @Override public String title()     { return "The black hole: space collapses"; }
    @Override public String thesisRef() { return "§5.3.2"; }
    @Override public int defaultDays()  { return 1000; }

    @Override
    public String notes() {
        return """
               20 People, 4 Firms, random start, everyone mobile, NO density cost.

               Watch peopleNN_mean and firmNN_mean (mean nearest-neighbour distance) both
               fall towards zero: everyone piles onto one spot and space stops existing.

               Watch the RATE. People collapse quickly -- they evaluate candidate locations
               and can see which is better. Firms take far longer, because a Firm sees only
               its own revenue and has to grope. Same objective, very different competence,
               and the difference is purely how much each can observe.

               This is the result the density cost of group C exists to prevent. Without
               something for distance to trade against, a spatial model has no space in it.""";
    }

    @Override
    public ModelConfig config() {
        return ModelConfig.builder()
                .space(Space.Kind.PLANE).width(1.0)
                .people(20).firms(4)
                .placePeople(Placement.random())
                .placeFirms(Placement.random())
                .utility(new Ces(0.5))
                .production(new SmoothEoS())
                .baseGoodCost(1.0)
                .wage(1.0)
                .deliveryCost(1.0)
                .densityCost(0)                       // nothing opposes agglomeration
                .personBehaviour(new PersonBundle())
                .firmBehaviour(new FirmLocationCrawl())
                .peopleMobile(true)
                .firmsMobile(true)
                .build();
    }

    @Override
    public void record(Recorder r, World w) {
        r.global("peopleNN_mean", () -> w.meanNearestNeighbour(w.people()));
        r.global("firmNN_mean", () -> w.meanNearestNeighbour(w.firms()));
        r.aggregate("peopleSpread", w.people(),
                p -> w.distance(p, w.person(0)), Aggregation.MEAN_SD);
        r.global("movementMean", w::meanPersonMovement);
        r.aggregate("utility", w.people(), p -> p.utility, Aggregation.MEAN_SD);
    }

    @Override
    public List<Parameter> controls(World world) {
        return List.of(world.deliveryCostParam(), world.densityCostParam());
    }
}
