package spatialecon.core.econ;

import spatialecon.core.config.Parameter;
import spatialecon.core.config.Quirks;

/**
 * Constant elasticity of substitution utility — the thesis workhorse (§2.2.3).
 *
 * <pre>
 *   U = ( Σ c_i^ρ )^(1/ρ)
 * </pre>
 *
 * <p>ρ carries the whole substitutes-to-variety spectrum. As ρ → 1 goods become perfect
 * substitutes and love of variety vanishes; as ρ → 0 a mix is increasingly preferred. Note
 * the direction: high ρ means <em>low</em> love of variety, which is easy to state backwards.
 *
 * <p>The constrained optimum is where the externality of the whole framework lives (§6.3.4).
 * Every good's price appears in the denominator of every other good's demand, so one Firm's
 * price change alters demand for all the others — not through timing or interaction, but
 * through the consumer's own arithmetic.
 */
public final class Ces implements Utility {

    private double rho;
    private final Quirks quirks;

    public Ces(double rho) {
        this(rho, Quirks.ORIGINAL);
    }

    public Ces(double rho, Quirks quirks) {
        this.rho = rho;
        this.quirks = quirks;
    }

    public double rho()             { return rho; }
    public void setRho(double rho)  { this.rho = rho; }

    /** Bindable ρ, for scenarios that expose it as a live control. */
    public Parameter rhoParam() {
        return Parameter.of("rho", this::rho, this::setRho).range(0.01, 1.0).label("rho (1 = substitutes)");
    }

    @Override
    public double utilityOf(double[] quantities) {
        double total = 0;
        for (double q : quantities) {
            if (q > 0) total += Math.pow(q, rho);
        }
        return Math.pow(total, 1 / rho);
    }

    @Override
    public double[] optimalQuantities(double[] prices, double budget) {
        double[] out = new double[prices.length];
        if (prices.length == 0 || budget <= 0) return out;

        if (rho < 1) {
            // g_j = P_j^(1/(rho-1)) * Y  /  sum_i P_i^(rho/(rho-1))
            double denominator = 0;
            for (double p : prices) denominator += Math.pow(p, rho / (rho - 1));
            if (denominator == 0) return out;
            for (int i = 0; i < prices.length; i++) {
                out[i] = Math.pow(prices[i], 1 / (rho - 1)) * budget / denominator;
            }
            return out;
        }

        // Perfect substitutes: the cheapest takes everything.
        int cheapest = 0;
        for (int i = 1; i < prices.length; i++) {
            if (prices[i] < prices[cheapest]) cheapest = i;
        }
        // ORIGINAL multiplied price by budget here, which disagrees with the rho->1 limit
        // of the branch above (that limit is budget/price). See Quirks and CODE_ANATOMY §7.1
        // — unresolved, so the original stays the default.
        out[cheapest] = quirks.substitutesUseLimitFormula()
                ? budget / prices[cheapest]
                : prices[cheapest] * budget;
        return out;
    }

    @Override
    public String describe() {
        return "CES (rho=%.4g)".formatted(rho);
    }
}
