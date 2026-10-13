package spatialecon.core.econ;

/**
 * Turns labour (People's time) into good output.
 *
 * <p>The thesis keeps two forms deliberately distinct (§2.2.5): a "smooth" function whose
 * output rises for any positive input, and the Core Model's fixed-plus-marginal form, which
 * returns negative output below its fixed labour requirement. That difference is not
 * incidental — it is the whole "blind versus rational choice" argument of §4.7.6, where the
 * fixed requirement deadlocks agents who reason about it and the smooth one does not.
 */
public interface Production {

    /** Total output from a total labour input. May be negative for Core Model style. */
    double outputFor(double labour);

    /** Short description for logs and figure captions. */
    String describe();

    /**
     * Output per unit of input — the "efficiency" plotted above the line in §6.4.1.
     * Zero input gives zero rather than NaN.
     */
    default double outputPerUnit(double labour) {
        return labour == 0 ? 0 : outputFor(labour) / labour;
    }

    /**
     * Marginal output — the extra produced by one more unit of input, by central difference.
     *
     * <p>Numeric rather than analytic so it works for any form. The thesis plots total,
     * average and marginal together to show the two functions' different shapes (§2.2.5):
     * the smooth form's average and marginal both rise, while the Core Model form has a
     * constant marginal by construction and an average that rises at a decreasing rate.
     */
    default double marginalAt(double labour) {
        double h = 1e-6;
        return (outputFor(labour + h) - outputFor(labour - h)) / (2 * h);
    }

    /**
     * The share of output attributable to one agent's contribution, given the producer's
     * current total input.
     *
     * <p>This is the mechanism that lets economies of scale work without anyone needing a
     * strategy (§4.7.5): total input goes through the production function, and the buying
     * agent takes their proportional slice of the result. {@code g_a = f(T_p) · (t_a / T_p)}.
     */
    default double granularShare(double totalInput, double myInput) {
        return totalInput == 0 ? 0 : outputFor(totalInput) * (myInput / totalInput);
    }
}
