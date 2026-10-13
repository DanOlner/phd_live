package spatialecon.core.econ;

import spatialecon.core.engine.Rng;

/**
 * How a consumer decides what to buy, given prices and a budget.
 *
 * <p>The thesis compares two, and the comparison is the point (§5.2.3). They reach the same
 * answer, and which is preferable is not obvious:
 *
 * <ul>
 *   <li>{@link Constrained} plugs prices into the analytic optimum. Exact and cheap — but
 *       causally backwards. The consumer obtains their optimal quantities and only then
 *       feeds them back through the utility function to discover how happy it made them.
 *   <li>{@link HillClimb} proposes bundles and keeps whichever is better. Far more plausible
 *       as an account of choosing, noisier, and roughly a thousand times the work. Its real
 *       advantage is that it can cope with a good running out, which the constrained form
 *       structurally cannot.
 * </ul>
 *
 * <p>The thesis' verdict is that the analytic method served better despite being the less
 * realistic one — "everything depends on the problem".
 */
public interface Optimiser {

    /**
     * Best affordable quantities of each good.
     *
     * @param prices full price per unit, including any distance cost
     * @param budget total spend available
     * @param utility preferences to maximise
     * @param rng     randomness, for search-based optimisers
     */
    double[] optimise(double[] prices, double budget, Utility utility, Rng rng);

    /** Short description for logs and figure captions. */
    String describe();

    /** The analytic constrained optimum. */
    static Optimiser constrained() {
        return new Constrained();
    }

    /** Stochastic search with the original's settings: 1000 iterations, jiggle 0.01 → 0.001. */
    static Optimiser hillClimb() {
        return new HillClimb();
    }

    /** Stochastic search with a given iteration budget. */
    static Optimiser hillClimb(int iterations) {
        return new HillClimb(iterations, 0.01, 0.001);
    }

    /** Delegates straight to the utility function's own constrained optimum. */
    final class Constrained implements Optimiser {
        @Override
        public double[] optimise(double[] prices, double budget, Utility utility, Rng rng) {
            return utility.optimalQuantities(prices, budget);
        }

        @Override
        public String describe() { return "constrained (analytic)"; }
    }

    /**
     * The "hill climber" of §4.6.1.
     *
     * <p>Slice the budget randomly across goods, jiggle the slices, keep the change if
     * utility improved, otherwise revert. Ratchets towards the optimum. The jiggle size
     * anneals from {@code startJiggle} to {@code endJiggle} across the run, so the search
     * coarsens early and refines late.
     *
     * <p>A quirk worth knowing (§5.2.3): <em>more</em> goods converge <em>faster</em>, because
     * slicing a budget across many goods gives each slice less room to vary.
     *
     * <p>This implementation has no stock handling. The original reallocated spend when a
     * good ran out — the capability the thesis credits it with — which only becomes
     * meaningful once Firms have finite stock.
     */
    final class HillClimb implements Optimiser {

        private final int iterations;
        private final double startJiggle;
        private final double endJiggle;

        /** A trace of best-so-far utility, one entry per iteration. Overwritten each call. */
        private double[] lastTrace = new double[0];

        HillClimb() {
            this(1000, 0.01, 0.001);
        }

        HillClimb(int iterations, double startJiggle, double endJiggle) {
            this.iterations = iterations;
            this.startJiggle = startJiggle;
            this.endJiggle = endJiggle;
        }

        public int iterations() { return iterations; }

        /** Best-so-far utility per iteration, from the most recent {@link #optimise} call. */
        public double[] lastTrace() { return lastTrace; }

        @Override
        public double[] optimise(double[] prices, double budget, Utility utility, Rng rng) {
            int n = prices.length;
            double[] trace = new double[Math.max(iterations, 0)];
            if (n == 0 || budget <= 0) {
                lastTrace = trace;
                return new double[n];
            }

            double[] slice = new double[n];
            for (int i = 0; i < n; i++) slice[i] = rng.nextDouble();
            normalise(slice);

            double[] best = spend(slice, prices, budget);
            double bestUtility = utility.utilityOf(best);

            double[] candidate = new double[n];
            double jiggle = startJiggle;
            double step = iterations > 0 ? (endJiggle - startJiggle) / iterations : 0;

            for (int i = 0; i < iterations; i++) {
                for (int j = 0; j < n; j++) {
                    candidate[j] = slice[j] + rng.jiggle(jiggle);
                    // bounce negatives back positive, as the original did
                    if (candidate[j] < 0) candidate[j] = -candidate[j];
                }
                normalise(candidate);

                double[] quantities = spend(candidate, prices, budget);
                double u = utility.utilityOf(quantities);

                if (u > bestUtility) {
                    bestUtility = u;
                    best = quantities;
                    System.arraycopy(candidate, 0, slice, 0, n);
                }
                trace[i] = bestUtility;
                jiggle += step;
            }

            lastTrace = trace;
            return best;
        }

        /** Turn budget shares into quantities at these prices. */
        private static double[] spend(double[] shares, double[] prices, double budget) {
            double[] q = new double[shares.length];
            for (int i = 0; i < shares.length; i++) {
                q[i] = prices[i] > 0 ? (shares[i] * budget) / prices[i] : 0;
            }
            return q;
        }

        /** Scale to sum 1, so the whole budget is always allocated. */
        private static void normalise(double[] shares) {
            double sum = 0;
            for (double s : shares) sum += s;
            if (sum <= 0) {
                java.util.Arrays.fill(shares, 1.0 / shares.length);
                return;
            }
            for (int i = 0; i < shares.length; i++) shares[i] /= sum;
        }

        @Override
        public String describe() {
            return "hill-climber (%d iterations, jiggle %.4g→%.4g)"
                    .formatted(iterations, startJiggle, endJiggle);
        }
    }
}
