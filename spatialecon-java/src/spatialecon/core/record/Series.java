package spatialecon.core.record;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

import spatialecon.core.engine.Actor;

/**
 * One named quantity, sampled from a group of actors over time.
 *
 * <p>Replaces the {@code VariableStore} family. The original pulled fields by reflection on
 * a name string, which meant a typo produced a silent empty column; here the accessor is a
 * lambda and the compiler checks it.
 *
 * @param <A> the actor type sampled
 */
public final class Series<A extends Actor> {

    private final String name;
    private final List<A> actors;
    private final ToDoubleFunction<A> accessor;
    private final Aggregation aggregation;
    private final List<double[]> rows = new ArrayList<>();
    private final List<Long> days = new ArrayList<>();

    /** Rolling window; -1 keeps everything. Matches the original's {@code followAverage}. */
    private final int window;

    Series(String name, List<A> actors, ToDoubleFunction<A> accessor,
           Aggregation aggregation, int window) {
        this.name = name;
        this.actors = actors;
        this.accessor = accessor;
        this.aggregation = aggregation;
        this.window = window;
    }

    /** Sample now and append a row. */
    void sample(long day) {
        List<Double> values = new ArrayList<>(actors.size());
        for (A a : actors) values.add(accessor.applyAsDouble(a));
        rows.add(aggregation.reduce(values));
        days.add(day);
        if (window > 0 && rows.size() > window) {
            rows.remove(0);
            days.remove(0);
        }
    }

    public String name()             { return name; }
    public Aggregation aggregation() { return aggregation; }
    public int size()                { return rows.size(); }
    public List<double[]> rows()     { return rows; }
    public List<Long> days()         { return days; }

    public String[] columns() { return aggregation.columns(name, actors.size()); }

    /** Most recent row, or null if nothing has been sampled. */
    public double[] latest() {
        return rows.isEmpty() ? null : rows.get(rows.size() - 1);
    }

    /** Most recent value of the first column — the common case for scalar series. */
    public double latestValue() {
        double[] r = latest();
        return r == null || r.length == 0 ? Double.NaN : r[0];
    }
}
