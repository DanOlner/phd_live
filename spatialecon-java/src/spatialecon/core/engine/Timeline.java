package spatialecon.core.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The day loop.
 *
 * <p>A day is a sequence of <em>phases</em>. Within a phase the actors are permuted and each
 * acts once; phases run in the order they were added. Nothing acts simultaneously — when an
 * agent decides, everyone else is momentarily static (§4.5.2).
 *
 * <p>The phase split is not cosmetic. Permuting People and Firms in one pot leaves a Firm
 * comparing its revenue against "yesterday" when a variable number of People have acted in
 * between; the thesis found the fix was to permute them separately, so every Person has
 * acted before any Firm does (§4.7.4). In the original that was two adjacent lines in
 * {@code Main.java} and easy to miss. Here it is the structure of the loop.
 */
public final class Timeline {

    /** A group of actors that all act, in random order, before the next group starts. */
    private record Phase(String name, List<? extends Actor> actors, Consumer<Actor> action) { }

    private final List<Phase> phases = new ArrayList<>();
    private final Rng rng;
    private long day;

    public Timeline(Rng rng) {
        this.rng = rng;
    }

    /** Add a phase. Order of addition is the order phases run in. */
    @SuppressWarnings("unchecked")
    public <A extends Actor> Timeline phase(String name, List<A> actors, Consumer<A> action) {
        phases.add(new Phase(name, actors, (Consumer<Actor>) action));
        return this;
    }

    public long day() { return day; }

    /** Run one day: every phase, in order, each internally permuted. */
    public void nextDay() {
        for (Phase p : phases) {
            List<Actor> order = new ArrayList<>(p.actors());
            rng.shuffle(order);
            for (Actor a : order) p.action().accept(a);
        }
        day++;
    }

    /** Names of the phases in run order, for logging. */
    public List<String> phaseNames() {
        return phases.stream().map(Phase::name).toList();
    }
}
