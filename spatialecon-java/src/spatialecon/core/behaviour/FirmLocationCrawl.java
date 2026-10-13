package spatialecon.core.behaviour;

import java.util.HashMap;
import java.util.Map;

import spatialecon.core.engine.Firm;
import spatialecon.core.engine.Pt;
import spatialecon.core.engine.Space;
import spatialecon.core.engine.World;

/**
 * A Firm gropes towards better revenue by moving and seeing what happens (§5.5, §4.7.7).
 *
 * <p>Perturb position, wait a turn, ask "did my revenue rise?". If yes, keep going the same
 * way and step a little further. If no, go back and start a fresh search in a new random
 * direction. The Firm knows nothing else — not where the People are, not what the other Firm
 * did, not even that another Firm exists.
 *
 * <p>This is the "data, meaning, response" mechanism the thesis takes from Jane Jacobs. The
 * datum is a revenue change; the assumed meaning is "my last action caused this"; the
 * response follows. Alone, a Firm has no trouble — the world is static, so any change really
 * is its own doing. Add one competitor and the meaning becomes unreliable, because revenue
 * now moves for reasons the Firm did not cause and cannot see. That is §5.5.2.
 *
 * <p><b>The step size matters more than it looks.</b> A new search draws a <em>signed</em>
 * magnitude uniformly from ±{@code CRAWL_RATE}/2, so most steps are well under the maximum
 * and some are nearly zero. It is not a unit direction times a fixed rate — that would make
 * Firms far better searchers than they are, and the whole point is how poorly they do. On a
 * line the sign is the direction; on a plane it is paired with a random angle.
 *
 * <p>Only location is perturbed. The original's {@code FirmActRespondAverage} contains fully
 * written branches for perturbing wage and price too, permanently unreachable behind a
 * hardcoded {@code choose = 2}; the thesis notes those "faced problems".
 */
public final class FirmLocationCrawl implements FirmBehaviour {

    /** The original's {@code startCrawlRate}. Steps are drawn from ±half this. */
    private static final double CRAWL_RATE = 0.025;

    /** How much a successful direction accelerates each turn. The original's {@code adjustStep}. */
    private static final double ADJUST_STEP = 0.0025;

    private final Map<Integer, Search> searches = new HashMap<>();

    private static final class Search {
        double step;          // signed; its sign is the direction of travel
        double accelerate;    // signed to match step, so success moves further the same way
        double angle;         // plane only
        boolean needsNewDirection = true;
        Pt previousPosition = Pt.ORIGIN;
        boolean hasMoved;
    }

    @Override
    public void act(Firm firm, World world) {
        Search search = searches.computeIfAbsent(firm.id(), id -> new Search());

        double revenueChange = firm.revenue - firm.revenueLastTurn;

        if (search.hasMoved) {
            if (revenueChange > 0) {
                search.step += search.accelerate;      // worked: same way, a little bolder
            } else {
                firm.placeAt(search.previousPosition); // did not work: undo and rethink
                search.needsNewDirection = true;
            }
        }

        if (world.config().firmsMobile()) {
            if (search.needsNewDirection) startNewSearch(search, world);
            search.previousPosition = firm.position();
            firm.moveTo(firm.position().plus(offset(search, world)), world.space());
            search.hasMoved = true;
        }

        // Revenue is counted per turn, so bank it and reset.
        firm.revenueLastTurn = firm.revenue;
        firm.revenue = 0;
    }

    /** Draw a fresh signed step, and on a plane a fresh angle to go with it. */
    private void startNewSearch(Search search, World world) {
        search.step = world.rng().jiggle(CRAWL_RATE);            // uniform in ±CRAWL_RATE/2
        search.accelerate = search.step < 0 ? -ADJUST_STEP : ADJUST_STEP;
        search.angle = world.rng().nextDouble() * 2 * Math.PI;
        search.needsNewDirection = false;
    }

    private Pt offset(Search search, World world) {
        if (world.space().kind() == Space.Kind.PLANE) {
            return new Pt(search.step * Math.cos(search.angle),
                          search.step * Math.sin(search.angle));
        }
        return new Pt(search.step, 0);
    }

    @Override
    public String describe() {
        return "location crawl (signed step up to %.4g, accelerating by %.4g)"
                .formatted(CRAWL_RATE / 2, ADJUST_STEP);
    }
}
