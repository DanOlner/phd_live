package spatialecon.core.econ;

import spatialecon.core.config.Parameter;

/**
 * The thesis production function (§2.2.5):
 *
 * <pre>
 *   x = v·l² + m·l
 * </pre>
 *
 * <p>{@code v} (curve) scales the increasing-returns term, {@code m} (magnitude) the linear
 * one. Setting {@code v = 0} gives constant returns, which is how the thesis isolates what
 * economies of scale are actually doing — several results only appear when {@code v > 0}.
 *
 * <p>"Smooth" is the point: any positive input yields positive output, so production can
 * start from nothing. The Core Model's fixed-labour form cannot, and that difference drives
 * the blind-versus-rational argument of §4.7.6.
 */
public final class SmoothEoS implements Production {

    private double curve;
    private double magnitude;

    /** Normalised form {@code x = l² + l} — used for the deducible examples in §6.3.1. */
    public SmoothEoS() {
        this(1, 1);
    }

    public SmoothEoS(double curve, double magnitude) {
        this.curve = curve;
        this.magnitude = magnitude;
    }

    public double curve()                { return curve; }
    public double magnitude()            { return magnitude; }
    public void setCurve(double v)       { this.curve = v; }
    public void setMagnitude(double v)   { this.magnitude = v; }

    public Parameter curveParam() {
        return Parameter.of("EoS_C", this::curve, this::setCurve).range(0, 1).label("EoS curve");
    }

    public Parameter magnitudeParam() {
        return Parameter.of("EoS_M", this::magnitude, this::setMagnitude).range(0, 5).label("EoS magnitude");
    }

    @Override
    public double outputFor(double labour) {
        return curve * labour * labour + magnitude * labour;
    }

    @Override
    public String describe() {
        return "smooth EoS (curve=%.4g, magnitude=%.4g)".formatted(curve, magnitude);
    }
}
