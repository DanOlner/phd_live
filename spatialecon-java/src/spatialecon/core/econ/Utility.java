package spatialecon.core.econ;

import java.util.List;

/**
 * A consumer's preferences.
 *
 * <p>Two entry points, matching the two methods the thesis compares (§5.2.3):
 *
 * <ul>
 *   <li>{@link #utilityOf} — given quantities, how happy am I? Used by the hill-climber,
 *       which proposes bundles and asks. Causally the sensible direction.
 *   <li>{@link #optimalQuantities} — given prices and a budget, what should I buy? The
 *       analytic constrained optimum. Faster and exact, but says nothing about the
 *       resulting utility, which then has to be read back through {@link #utilityOf}.
 * </ul>
 *
 * <p>Unlike the original, ρ is instance state rather than {@code static}, so agents
 * <em>could</em> differ in tastes. Every published scenario gives them all the same
 * instance, preserving the thesis assumption of identical tastes — but now that is a
 * choice the scenario makes, not something the type system enforces.
 */
public interface Utility {

    /** Utility from consuming these quantities. */
    double utilityOf(double[] quantities);

    /**
     * Constrained optimum: how much of each good to buy, given full prices (base plus any
     * distance cost) and a budget. Returns a new array parallel to {@code prices}.
     */
    double[] optimalQuantities(double[] prices, double budget);

    /** Short description for logs and figure captions, e.g. {@code "CES (rho=0.5)"}. */
    String describe();

    /** Convenience for the common case of a List rather than an array. */
    default double utilityOf(List<Double> quantities) {
        double[] a = new double[quantities.size()];
        for (int i = 0; i < a.length; i++) a[i] = quantities.get(i);
        return utilityOf(a);
    }
}
