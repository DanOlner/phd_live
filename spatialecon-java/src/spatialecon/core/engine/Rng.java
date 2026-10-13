package spatialecon.core.engine;

import java.util.List;
import java.util.Random;

/**
 * The model's single source of randomness.
 *
 * <p>One seeded generator per run, so a run is exactly repeatable and a Monte Carlo sweep
 * is just the same run with the seed stepped. The original used a static {@code Randoms}
 * class for this; here it is owned by the {@link World}, so two models can run in one
 * process without interfering.
 */
public final class Rng {

    private final long seed;
    private final Random random;

    public Rng(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }

    public long seed() { return seed; }

    /** Uniform in [0, 1). */
    public double nextDouble() { return random.nextDouble(); }

    /** Uniform in [0, max). */
    public double upTo(double max) { return random.nextDouble() * max; }

    /** Uniform in [lo, hi). */
    public double between(double lo, double hi) { return lo + random.nextDouble() * (hi - lo); }

    /** Uniform in [-half, +half) — the "jiggle" pattern used throughout the original. */
    public double jiggle(double magnitude) { return (random.nextDouble() - 0.5) * magnitude; }

    /** Uniform integer in [0, bound). */
    public int nextInt(int bound) { return random.nextInt(bound); }

    public boolean nextBoolean() { return random.nextBoolean(); }

    /** A uniformly distributed point inside a circle of this radius, centred on the origin. */
    public Pt pointInCircle(double radius) {
        // sqrt of the uniform draw, else points bunch towards the centre
        double r = radius * Math.sqrt(random.nextDouble());
        double theta = random.nextDouble() * 2 * Math.PI;
        return new Pt(r * Math.cos(theta), r * Math.sin(theta));
    }

    /** A uniformly random direction, as a vector of the given length. */
    public Pt vectorOfLength(double length) {
        double theta = random.nextDouble() * 2 * Math.PI;
        return new Pt(length * Math.cos(theta), length * Math.sin(theta));
    }

    /**
     * Shuffle in place, Fisher–Yates.
     *
     * <p>Used once per phase per day to permute turn order. Note the consequence the thesis
     * draws out (§4.7.4): permuting each day means the gap between one agent's consecutive
     * turns is pyramidally distributed around one day, not fixed at one day — so any
     * behaviour comparing t to t−1 is working with a variable interval.
     */
    public <T> void shuffle(List<T> list) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            T tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }
}
