package spatialecon.core.engine;

import java.util.List;

/**
 * How a group of actors is laid out at the start of a run.
 *
 * <p>In the original these were static methods on {@code SpaceTools}, invoked either from a
 * hardcoded switch in {@code Main} or by reflection from a config line. Here a scenario
 * names one directly.
 */
@FunctionalInterface
public interface Placement {

    void place(List<? extends Actor> actors, Space space, Rng rng);

    /** All on the centre point. The monocentric Firm (§5.4.1). */
    static Placement centre() {
        return (actors, space, rng) -> actors.forEach(a -> a.placeAt(space.centre()));
    }

    /** All on one given point. */
    static Placement at(double x, double y) {
        return (actors, space, rng) -> actors.forEach(a -> a.placeAt(new Pt(x, y)));
    }

    /**
     * Evenly spaced along the x axis between two fractions of width, inclusive.
     *
     * <p>{@code evenlyAlongX(0, 1)} spreads across the whole line — the 19 People and 19
     * Firms of §6.4.1, or the 50 People of §5.5.1. {@code evenlyAlongX(0.25, 0.75)} gives
     * the restricted band the Firms must find in the DMR game (§5.5.2).
     */
    static Placement evenlyAlongX(double fromFraction, double toFraction) {
        return (actors, space, rng) -> {
            int n = actors.size();
            double from = fromFraction * space.width(), to = toFraction * space.width();
            for (int i = 0; i < n; i++) {
                double t = n == 1 ? 0.5 : (double) i / (n - 1);
                actors.get(i).placeAt(new Pt(from + t * (to - from), 0));
            }
        };
    }

    /** Two points a given distance apart, alternating. The two-Firm scenarios (§5.4.6, §6.4.2). */
    static Placement twoPointsApart(double separation) {
        return (actors, space, rng) -> {
            double cx = space.centre().x(), cy = space.centre().y();
            for (int i = 0; i < actors.size(); i++) {
                double dx = (i % 2 == 0 ? -separation / 2 : separation / 2);
                actors.get(i).placeAt(new Pt(cx + dx, cy));
            }
        };
    }

    /** Uniformly at random across the space. */
    static Placement random() {
        return (actors, space, rng) -> actors.forEach(a ->
                a.placeAt(space.confine(new Pt(rng.upTo(space.width()), rng.upTo(space.width())))));
    }

    /** Evenly around a circle of this radius, centred on the space. */
    static Placement inCircle(double radius) {
        return (actors, space, rng) -> {
            int n = actors.size();
            Pt c = space.centre();
            for (int i = 0; i < n; i++) {
                double theta = 2 * Math.PI * i / n;
                actors.get(i).placeAt(new Pt(c.x() + radius * Math.cos(theta),
                                             c.y() + radius * Math.sin(theta)));
            }
        };
    }

    /**
     * Split between the two discrete regions.
     *
     * @param shareInRegionZero 0 puts everyone in region 1, 1 puts everyone in region 0,
     *                          0.5 splits them evenly.
     */
    static Placement splitBetweenRegions(double shareInRegionZero) {
        return (actors, space, rng) -> {
            int n = actors.size();
            int inZero = (int) Math.round(n * shareInRegionZero);
            for (int i = 0; i < n; i++) {
                double x = i < inZero ? space.regionZeroX() : space.regionOneX();
                actors.get(i).placeAt(new Pt(x, 0));
            }
        };
    }
}
