package spatialecon.core.record;

import java.util.List;

/**
 * How a series reduces many actors' values to the numbers stored for one timestep.
 *
 * <p>The original had a separate class per combination — 49 {@code io/*Bucket} classes and
 * 14 {@code variables/VariableStore*} classes, differing only in this choice and in which
 * field they read.
 */
public enum Aggregation {

    /** Keep every actor's value. One column per actor. Correlation plots need this. */
    PER_ACTOR("") {
        @Override public double[] reduce(List<Double> values) {
            double[] out = new double[values.size()];
            for (int i = 0; i < out.length; i++) out[i] = values.get(i);
            return out;
        }
        @Override public String[] columns(String name, int actorCount) {
            String[] cols = new String[actorCount];
            for (int i = 0; i < actorCount; i++) cols[i] = name + "_" + i;
            return cols;
        }
    },

    /** Mean only. */
    MEAN("mean") {
        @Override public double[] reduce(List<Double> values) {
            return new double[]{ mean(values) };
        }
        @Override public String[] columns(String name, int actorCount) {
            return new String[]{ name + "_mean" };
        }
    },

    /** Mean and population standard deviation. */
    MEAN_SD("mean+sd") {
        @Override public double[] reduce(List<Double> values) {
            double m = mean(values);
            double sumSq = 0;
            for (double v : values) sumSq += (v - m) * (v - m);
            double sd = values.isEmpty() ? 0 : Math.sqrt(sumSq / values.size());
            return new double[]{ m, sd };
        }
        @Override public String[] columns(String name, int actorCount) {
            return new String[]{ name + "_mean", name + "_sd" };
        }
    },

    /** Smallest and largest. */
    MIN_MAX("min+max") {
        @Override public double[] reduce(List<Double> values) {
            double lo = Double.POSITIVE_INFINITY, hi = Double.NEGATIVE_INFINITY;
            for (double v : values) { lo = Math.min(lo, v); hi = Math.max(hi, v); }
            if (values.isEmpty()) { lo = 0; hi = 0; }
            return new double[]{ lo, hi };
        }
        @Override public String[] columns(String name, int actorCount) {
            return new String[]{ name + "_min", name + "_max" };
        }
    },

    /**
     * Ratio of the two values, larger over smaller so the result is always ≥ 1.
     *
     * <p>The "Firm efficiency ratio" of §6.4.2. Only meaningful for exactly two actors.
     */
    RATIO_GTE_ONE("ratio") {
        @Override public double[] reduce(List<Double> values) {
            if (values.size() != 2) {
                throw new IllegalStateException("RATIO_GTE_ONE needs exactly 2 actors, got " + values.size());
            }
            double a = values.get(0), b = values.get(1);
            double lo = Math.min(a, b), hi = Math.max(a, b);
            return new double[]{ lo == 0 ? Double.NaN : hi / lo };
        }
        @Override public String[] columns(String name, int actorCount) {
            return new String[]{ name + "_ratio" };
        }
    };

    private final String description;

    Aggregation(String description) { this.description = description; }

    public String description() { return description; }

    /** Reduce this timestep's per-actor values to the stored numbers. */
    public abstract double[] reduce(List<Double> values);

    /** Column headings this aggregation produces. */
    public abstract String[] columns(String name, int actorCount);

    private static double mean(List<Double> values) {
        if (values.isEmpty()) return 0;
        double s = 0;
        for (double v : values) s += v;
        return s / values.size();
    }
}
