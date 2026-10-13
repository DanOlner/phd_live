package spatialecon.core.engine;

/** An immutable point. Actors hold one and replace it when they move. */
public record Pt(double x, double y) {

    public static final Pt ORIGIN = new Pt(0, 0);

    public double distanceTo(Pt other) {
        double dx = x - other.x, dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public Pt plus(Pt other) { return new Pt(x + other.x, y + other.y); }
    public Pt plus(double dx, double dy) { return new Pt(x + dx, y + dy); }

    @Override
    public String toString() {
        return String.format("(%.4f, %.4f)", x, y);
    }
}
