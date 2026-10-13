package spatialecon.core.scenario;

import java.util.List;

import spatialecon.core.config.ModelConfig;
import spatialecon.core.config.Parameter;
import spatialecon.core.engine.World;
import spatialecon.core.record.Recorder;

/**
 * One model from the thesis, as a self-describing unit.
 *
 * <p>This is the whole point of the refactor. In the original, choosing a model meant
 * editing {@code Main.java}, {@code ActorMaker.java} and {@code Visualiser.java}, then
 * recompiling — so no checkout corresponded to any published figure, and two scenarios were
 * lost entirely. Here a model is a class that states its own parameters, what it records and
 * what it exposes for interaction, and any number of them coexist.
 *
 * <p>Scenarios are Java rather than config files because they describe structure — which
 * behaviours, which recordings, which controls — not just numbers. Numbers can still be
 * overridden from the command line for sweeps.
 *
 * <p>Implementations should hold references to the objects they build in {@link #config()}
 * (the utility function, say) so {@link #controls} can bind parameters to them.
 */
public interface Scenario {

    /** Short identifier used on the command line, matching THESIS_DIGEST: {@code "c8"}, {@code "e7"}. */
    String id();

    /** Human-readable name. */
    String title();

    /** Section of the thesis this reproduces, e.g. {@code "§5.4.7"}. */
    String thesisRef();

    /** Build the configuration. Called once per run. */
    ModelConfig config();

    /**
     * Adjust the world after construction, before the run starts.
     *
     * <p>For anything that depends on the built agents rather than the config: tying People
     * to particular Firms, seeding individual prices, imposing a starting asymmetry. The
     * original did this inline in {@code ActorMaker}, mixed in with construction.
     */
    default void setUp(World world) { }

    /** Declare what to record. Default records nothing. */
    default void record(Recorder recorder, World world) { }

    /** Parameters exposed for live control or sweeping. Default none. */
    default List<Parameter> controls(World world) { return List.of(); }

    /**
     * Called before each day, with the day about to run (0-based).
     *
     * <p>The hook parameter sweeps use: step a value here and the day index becomes the
     * sweep index, so a sweep is just a run and needs no separate driver. The function
     * explorers of group A use it to walk ρ or a good count across their range.
     */
    default void beforeDay(World world, long day) { }

    /** Sensible number of days for a headless run, unless overridden. */
    default int defaultDays() { return 1000; }

    /**
     * Anything a reader should know: reconstruction caveats, which jar the numbers came
     * from, known departures. Shown by {@code --info}. Empty by default.
     */
    default String notes() { return ""; }
}
