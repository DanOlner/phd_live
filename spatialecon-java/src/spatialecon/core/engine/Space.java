package spatialecon.core.engine;

/**
 * The geography a model run takes place in.
 *
 * <p>Four kinds, as the thesis framework describes (§4.2). They differ only in how they
 * constrain location choice; distance is Euclidean throughout.
 *
 * <p>Two simplifications carried over deliberately from the original:
 *
 * <ul>
 *   <li>The original called the 2-D case a torus, but wrapping was disabled in every
 *       published run ({@code wrapSpace=false}). It is named {@link Kind#PLANE} here and
 *       wrapping is not implemented — if it is ever wanted, it is a new feature, not a
 *       restoration.
 *   <li>Movement between locations is free. Only moving <em>goods</em>, and commuting,
 *       cost anything (§4.3.2).
 * </ul>
 */
public final class Space {

    public enum Kind {
        /** Dimensionless. All agents are co-located; distance is always zero. */
        POINT,
        /** Continuous 1-D line of length {@code width}. */
        LINE,
        /** Two discrete points separated by {@code width}. */
        TWO_REGION,
        /** Continuous 2-D plane, {@code width} on each side. */
        PLANE
    }

    private final Kind kind;
    private final double width;

    public Space(Kind kind, double width) {
        this.kind = kind;
        this.width = width;
    }

    public Kind kind()    { return kind; }
    public double width() { return width; }

    /** Centre of the space. Firms are commonly placed here in the monocentric models. */
    public Pt centre() {
        return switch (kind) {
            case POINT      -> Pt.ORIGIN;
            case LINE       -> new Pt(width / 2, 0);
            case TWO_REGION -> new Pt(0, 0);
            case PLANE      -> new Pt(width / 2, width / 2);
        };
    }

    /** x of region 0. Regions sit either side of the origin, so {@code width} is their separation. */
    public double regionZeroX() { return -width / 2; }

    /** x of region 1. */
    public double regionOneX()  { return  width / 2; }

    /** Distance between two points. Always zero in a {@link Kind#POINT} space. */
    public double distance(Pt a, Pt b) {
        return kind == Kind.POINT ? 0 : a.distanceTo(b);
    }

    /**
     * Constrain a point to the space. Out-of-bounds coordinates are clamped, not wrapped;
     * on a line the y coordinate is flattened to zero.
     */
    public Pt confine(Pt p) {
        return switch (kind) {
            case POINT      -> Pt.ORIGIN;
            case LINE       -> new Pt(clamp(p.x(), 0, width), 0);
            case TWO_REGION -> new Pt(p.x() < 0 ? regionZeroX() : regionOneX(), 0);
            case PLANE      -> new Pt(clamp(p.x(), 0, width), clamp(p.y(), 0, width));
        };
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    @Override
    public String toString() {
        return kind + "(width=" + width + ")";
    }
}
