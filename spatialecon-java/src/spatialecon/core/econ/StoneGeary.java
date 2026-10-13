package spatialecon.core.econ;

import spatialecon.core.config.Parameter;

/**
 * Two-good utility with an optional subsistence term (§2.2.2):
 *
 * <pre>
 *   U = F^δ · (M + γ)^(1−δ)
 * </pre>
 *
 * <p>At γ = 0 this is plain Cobb–Douglas, which is how the thesis uses it. The γ term is the
 * "Engel function" variant whose constrained optima the thesis derives and then cuts; it
 * makes preferences non-homothetic, so the mix shifts with income rather than staying in
 * fixed proportions. Kept because it is derived, costs nothing, and gives the website a
 * ready-made contrast with the homothetic case.
 *
 * <p>The property the thesis actually exploits is what Cobb–Douglas <em>disables</em>. The
 * price of one good does not appear in demand for the other, so each sector is independently
 * unit elastic: cut a price, demand rises exactly enough to leave revenue unchanged. A Firm
 * facing Cobb–Douglas demand therefore has no lever — and, spatially, no reason to move
 * (§5.2.2). A utility function can silently switch off a whole class of agent behaviour.
 */
public final class StoneGeary implements Utility {

    private double delta;
    private final double gamma;

    /** Plain Cobb–Douglas: {@code U = F^δ · M^(1−δ)}. */
    public static StoneGeary cobbDouglas(double delta) {
        return new StoneGeary(delta, 0);
    }

    public StoneGeary(double delta, double gamma) {
        if (delta <= 0 || delta >= 1) {
            throw new IllegalArgumentException("delta must be strictly between 0 and 1, got " + delta);
        }
        this.delta = delta;
        this.gamma = gamma;
    }

    public double delta()               { return delta; }
    public double gamma()               { return gamma; }
    public void setDelta(double d)      { this.delta = d; }

    public Parameter deltaParam() {
        return Parameter.of("delta", this::delta, this::setDelta)
                .range(0.01, 0.99).label("share of spend on good 1");
    }

    @Override
    public double utilityOf(double[] quantities) {
        require2(quantities.length);
        double f = Math.max(quantities[0], 0);
        double m = Math.max(quantities[1], 0);
        return Math.pow(f, delta) * Math.pow(m + gamma, 1 - delta);
    }

    /**
     * {@code F = δY/p_F}, {@code M = (1−δ)Y/p_M} — plus the γ correction when non-zero.
     *
     * <p>Note what is absent: {@code p_M} does not appear in {@code F}, nor {@code p_F} in
     * {@code M}. That absence is the whole result of §5.2.2, and it is visible here rather
     * than something you have to run a simulation to discover.
     */
    @Override
    public double[] optimalQuantities(double[] prices, double budget) {
        require2(prices.length);
        double pF = prices[0], pM = prices[1];
        if (pF <= 0 || pM <= 0 || budget <= 0) return new double[2];

        if (gamma == 0) {
            return new double[]{ delta * budget / pF, (1 - delta) * budget / pM };
        }
        // With a subsistence term the budget is effectively augmented by gamma*pM, and the
        // subsistence quantity then subtracted back off the second good.
        double effective = budget + gamma * pM;
        double f = delta * effective / pF;
        double m = (1 - delta) * effective / pM - gamma;
        return new double[]{ Math.max(f, 0), Math.max(m, 0) };
    }

    private static void require2(int n) {
        if (n != 2) throw new IllegalArgumentException("StoneGeary is a two-good function, got " + n);
    }

    @Override
    public String describe() {
        return gamma == 0
                ? "Cobb-Douglas (delta=%.4g)".formatted(delta)
                : "Stone-Geary (delta=%.4g, gamma=%.4g)".formatted(delta, gamma);
    }
}
