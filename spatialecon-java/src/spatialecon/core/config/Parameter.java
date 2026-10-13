package spatialecon.core.config;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * A named, live-tunable model value.
 *
 * <p>Replaces the 29 {@code *MouseChanger} classes of the original, which differed only in
 * which field of {@code gl} they poked. A scenario declares the parameters it exposes; the
 * viz layer binds them to mouse drags, and the headless runner sweeps them. Neither needs
 * to know what the parameter actually is.
 *
 * <pre>{@code
 * Parameter.of("rho", () -> ces.rho, v -> ces.rho = v).range(0.01, 1.0)
 * }</pre>
 */
public final class Parameter {

    private final String name;
    private final DoubleSupplier getter;
    private final DoubleConsumer setter;
    private double min = 0;
    private double max = 1;
    private String label;

    private Parameter(String name, DoubleSupplier getter, DoubleConsumer setter) {
        this.name = name;
        this.label = name;
        this.getter = getter;
        this.setter = setter;
    }

    public static Parameter of(String name, DoubleSupplier getter, DoubleConsumer setter) {
        return new Parameter(name, getter, setter);
    }

    /** Bounds for UI binding and sweeps. Values set through this Parameter are clamped. */
    public Parameter range(double min, double max) {
        this.min = min;
        this.max = max;
        return this;
    }

    /** Display name, if it should differ from the identifier. */
    public Parameter label(String label) {
        this.label = label;
        return this;
    }

    public String name()  { return name; }
    public String label() { return label; }
    public double min()   { return min; }
    public double max()   { return max; }

    public double get() { return getter.getAsDouble(); }

    /** Set, clamped to {@link #range}. */
    public void set(double v) {
        setter.accept(Math.min(max, Math.max(min, v)));
    }

    /** Nudge by a delta, clamped. Used by drag controls. */
    public void nudge(double delta) {
        set(get() + delta);
    }

    /** Set from a normalised 0–1 position. Used by sweeps and sliders. */
    public void setNormalised(double t) {
        set(min + t * (max - min));
    }

    /** Current value as a normalised 0–1 position within {@link #range}. */
    public double getNormalised() {
        return max > min ? (get() - min) / (max - min) : 0;
    }

    @Override
    public String toString() {
        return name + "=" + get();
    }
}
