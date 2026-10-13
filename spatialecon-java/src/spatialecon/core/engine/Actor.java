package spatialecon.core.engine;

/**
 * Anything with an identity and a location.
 *
 * <p>Deliberately thin. In the original, {@code Actor} accumulated fields for every
 * experiment ever tried — commented-out blocks for backyard production, contributed-time
 * wrappers, productivity. Here the base class carries identity and position only, and each
 * subtype owns the state its own behaviours need.
 */
public abstract class Actor {

    private final int id;
    private Pt position = Pt.ORIGIN;

    /** Distance moved on the most recent turn. Used to detect spatial equilibrium. */
    private double distanceMovedLastTurn;

    protected Actor(int id) {
        this.id = id;
    }

    public int id() { return id; }

    public Pt position() { return position; }

    /** Place without recording it as movement. For initial layout. */
    public void placeAt(Pt p) {
        this.position = p;
    }

    /**
     * Move, recording the distance covered.
     *
     * <p>Migration is free in this framework — no cost is charged here (§4.3.2).
     */
    public void moveTo(Pt p, Space space) {
        Pt confined = space.confine(p);
        distanceMovedLastTurn = space.distance(position, confined);
        this.position = confined;
    }

    public double distanceMovedLastTurn() { return distanceMovedLastTurn; }

    public double distanceTo(Actor other, Space space) {
        return space.distance(position, other.position);
    }

    /** Short label used in output and figures, e.g. {@code "Person 12"}. */
    public String label() {
        return getClass().getSimpleName() + " " + id;
    }

    @Override
    public String toString() {
        return label() + " at " + position;
    }
}
