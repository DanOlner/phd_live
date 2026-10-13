package spatialecon.core.record;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import spatialecon.core.engine.Actor;

/**
 * Collects named series over a run and writes them out.
 *
 * <p>Replaces {@code io/} and {@code variables/} — 63 classes, 6,329 lines. A scenario
 * declares what it wants recorded:
 *
 * <pre>{@code
 * r.perActor("utility", w.people(), p -> p.utility);
 * r.aggregate("meanUtility", w.people(), p -> p.utility, Aggregation.MEAN_SD);
 * r.aggregate("efficiency", w.firms(), f -> f.productionLevel, Aggregation.RATIO_GTE_ONE);
 * }</pre>
 */
public final class Recorder {

    private final Map<String, Series<?>> series = new LinkedHashMap<>();
    private int sampleEvery = 1;

    /** Sample every nth day. Default 1. */
    public Recorder sampleEvery(int days) {
        this.sampleEvery = Math.max(1, days);
        return this;
    }

    /** One column per actor. What correlation plots need. */
    public <A extends Actor> Series<A> perActor(String name, List<A> actors,
                                                ToDoubleFunction<A> accessor) {
        return add(new Series<>(name, actors, accessor, Aggregation.PER_ACTOR, -1));
    }

    /** Reduced across actors by the given aggregation. */
    public <A extends Actor> Series<A> aggregate(String name, List<A> actors,
                                                 ToDoubleFunction<A> accessor,
                                                 Aggregation aggregation) {
        return add(new Series<>(name, actors, accessor, aggregation, -1));
    }

    /** As {@link #aggregate}, but keeping only the last {@code window} samples. */
    public <A extends Actor> Series<A> rolling(String name, List<A> actors,
                                               ToDoubleFunction<A> accessor,
                                               Aggregation aggregation, int window) {
        return add(new Series<>(name, actors, accessor, aggregation, window));
    }

    /** A single global value, not tied to any actor — a parameter being swept, say. */
    public Series<Actor> global(String name, java.util.function.DoubleSupplier value) {
        Actor placeholder = new Actor(-1) { };
        return add(new Series<>(name, List.of(placeholder),
                a -> value.getAsDouble(), Aggregation.MEAN, -1));
    }

    private <A extends Actor> Series<A> add(Series<A> s) {
        if (series.putIfAbsent(s.name(), s) != null) {
            throw new IllegalArgumentException("duplicate series name: " + s.name());
        }
        return s;
    }

    /** Sample every series, if this day is a sampling day. */
    public void sample(long day) {
        if (day % sampleEvery != 0) return;
        for (Series<?> s : series.values()) s.sample(day);
    }

    public Series<?> get(String name) {
        Series<?> s = series.get(name);
        if (s == null) throw new IllegalArgumentException("no series named " + name
                + " (have: " + series.keySet() + ")");
        return s;
    }

    public List<String> names() { return new ArrayList<>(series.keySet()); }

    public boolean isEmpty() { return series.isEmpty(); }

    /**
     * Write every series to one CSV, wide format: a {@code day} column, then each series'
     * columns side by side. Series sampled the same number of times line up by row.
     */
    public void writeCsv(Path file) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            List<Series<?>> all = new ArrayList<>(series.values());
            if (all.isEmpty()) return;

            List<String> header = new ArrayList<>();
            header.add("day");
            for (Series<?> s : all) header.addAll(List.of(s.columns()));
            out.println(String.join(",", header));

            int rowCount = all.stream().mapToInt(Series::size).max().orElse(0);
            for (int row = 0; row < rowCount; row++) {
                StringBuilder line = new StringBuilder();
                Series<?> first = all.get(0);
                line.append(row < first.days().size() ? first.days().get(row) : "");
                for (Series<?> s : all) {
                    double[] vals = row < s.size() ? s.rows().get(row) : new double[s.columns().length];
                    for (double v : vals) line.append(',').append(format(v));
                }
                out.println(line);
            }
        }
    }

    private static String format(double v) {
        if (Double.isNaN(v)) return "";
        return v == Math.rint(v) && Math.abs(v) < 1e15
                ? String.valueOf((long) v)
                : String.format("%.6g", v);
    }
}
