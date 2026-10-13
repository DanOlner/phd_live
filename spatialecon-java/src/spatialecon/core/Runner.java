package spatialecon.core;

import java.nio.file.Path;
import java.util.List;

import spatialecon.core.config.ModelConfig;
import spatialecon.core.config.Parameter;
import spatialecon.core.engine.World;
import spatialecon.core.record.Recorder;
import spatialecon.core.scenario.Scenario;
import spatialecon.core.scenario.ScenarioRegistry;

/**
 * Command-line entry point. Headless: no Processing, no window, no display needed.
 *
 * <pre>
 *   run --list                       every scenario
 *   run --info c8                    one scenario's parameters and notes
 *   run c8                           run it for its default days
 *   run c8 --days 5000 --out data/   run and write CSV
 *   run c8 --seed 7                  a different random draw
 * </pre>
 */
public final class Runner {

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || has(args, "--help") || has(args, "-h")) {
            printUsage();
            return;
        }

        if (has(args, "--list")) {
            listScenarios();
            return;
        }

        String id = firstNonFlag(args);
        if (id == null) {
            System.err.println("No scenario given. Try --list.");
            System.exit(2);
        }

        Scenario scenario = ScenarioRegistry.get(id);

        if (has(args, "--info")) {
            printInfo(scenario);
            return;
        }

        int days = intArg(args, "--days", scenario.defaultDays());
        String out = stringArg(args, "--out", null);
        Long seed = has(args, "--seed") ? (long) intArg(args, "--seed", 12) : null;
        int sampleEvery = intArg(args, "--sample-every", 1);

        run(scenario, days, sampleEvery, seed, out);
    }

    private static void run(Scenario scenario, int days, int sampleEvery,
                            Long seedOverride, String outDir) throws Exception {

        ModelConfig config = scenario.config();
        if (seedOverride != null) {
            // Seed is structural, so rebuild rather than mutate. Scenarios are cheap.
            config = ModelConfig.builder()
                    .seed(seedOverride)
                    .space(config.space().kind()).width(config.space().width())
                    .people(config.peopleCount()).firms(config.firmCount())
                    .placePeople(config.peoplePlacement()).placeFirms(config.firmPlacement())
                    .utility(config.utility()).production(config.production())
                    .personBehaviour(config.personBehaviour()).firmBehaviour(config.firmBehaviour())
                    .quirks(config.quirks())
                    .deliveryCost(config.deliveryCost()).commuteCost(config.commuteCost())
                    .baseGoodCost(config.baseGoodCost()).wage(config.wage())
                    .densityCost(config.densityCost()).densityRadius(config.densityRadius())
                    .demandNoise(config.demandNoise())
                    .peopleMobile(config.peopleMobile()).firmsMobile(config.firmsMobile())
                    .rangeDelivery(config.rangeDelivery()).rangeGoodCost(config.rangeGoodCost())
                    .rangeWage(config.rangeWage()).rangeCommute(config.rangeCommute())
                    .build();
        }

        World world = new World(config);
        scenario.setUp(world);
        Recorder recorder = new Recorder().sampleEvery(sampleEvery);
        scenario.record(recorder, world);

        System.out.println("== " + scenario.id() + ": " + scenario.title()
                + "  (" + scenario.thesisRef() + ")");
        System.out.println(config.summary());
        System.out.println("phases: " + String.join(" -> ", world.timeline().phaseNames()));
        System.out.println("running " + days + " days"
                + (sampleEvery > 1 ? ", sampling every " + sampleEvery : ""));
        System.out.println();

        long start = System.nanoTime();
        for (int d = 0; d < days; d++) {
            scenario.beforeDay(world, d);
            world.timeline().nextDay();
            recorder.sample(world.day());
        }
        double seconds = (System.nanoTime() - start) / 1e9;

        System.out.printf("done: %d days in %.2fs (%.0f days/sec)%n",
                days, seconds, days / Math.max(seconds, 1e-9));

        if (!recorder.isEmpty()) {
            System.out.println("series: " + String.join(", ", recorder.names()));
            System.out.printf("mean People movement last day: %.6f%n", world.meanPersonMovement());
        }

        if (outDir != null) {
            Path file = Path.of(outDir, scenario.id() + ".csv");
            recorder.writeCsv(file);
            System.out.println("wrote " + file.toAbsolutePath());
        }
    }

    private static void listScenarios() {
        System.out.println("Scenarios (ids match THESIS_DIGEST.md):\n");
        for (Scenario s : ScenarioRegistry.all()) {
            System.out.printf("  %-8s %-46s %s%n", s.id(), s.title(), s.thesisRef());
        }
        System.out.println("\n--info <id> for detail.");
    }

    private static void printInfo(Scenario s) {
        System.out.println("== " + s.id() + ": " + s.title() + "  (" + s.thesisRef() + ")");
        if (!s.notes().isEmpty()) System.out.println("\n" + s.notes());
        World w = new World(s.config());
        s.setUp(w);
        System.out.println("\n" + w.config().summary());
        List<Parameter> controls = s.controls(w);
        if (!controls.isEmpty()) {
            System.out.println("\ncontrols:");
            for (Parameter p : controls) {
                System.out.printf("  %-10s %-28s %.4g  [%.4g .. %.4g]%n",
                        p.name(), p.label(), p.get(), p.min(), p.max());
            }
        }
        System.out.println("\ndefault days: " + s.defaultDays());
    }

    private static void printUsage() {
        System.out.println("""
            spatialecon — thesis models, headless runner

              run --list                        list scenarios
              run --info <id>                   show one scenario's setup
              run <id> [options]                run it

            options:
              --days N          days to run (default: scenario's own)
              --out DIR         write DIR/<id>.csv
              --seed N          override the random seed
              --sample-every N  record every nth day (default 1)
            """);
    }

    // --- tiny arg helpers ---------------------------------------------------------

    private static boolean has(String[] args, String flag) {
        for (String a : args) if (a.equals(flag)) return true;
        return false;
    }

    private static String stringArg(String[] args, String flag, String fallback) {
        for (int i = 0; i < args.length - 1; i++) if (args[i].equals(flag)) return args[i + 1];
        return fallback;
    }

    private static int intArg(String[] args, String flag, int fallback) {
        String v = stringArg(args, flag, null);
        return v == null ? fallback : Integer.parseInt(v);
    }

    private static String firstNonFlag(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                // skip this flag's value if it takes one
                if (!args[i].equals("--info") && !args[i].equals("--list") && i + 1 < args.length) i++;
                continue;
            }
            return args[i];
        }
        return null;
    }

    private Runner() { }
}
