package spatialecon.core.config;

/**
 * Points where the 2013 implementation departs from what the thesis text states.
 *
 * <p>Each is documented in {@code CODE_ANATOMY.md} §7. Rather than silently choosing one
 * reading, both are implemented and selectable, so the difference can be measured rather
 * than argued about. {@link #ORIGINAL} is the default everywhere: it reproduces the
 * behaviour that actually produced the published figures.
 *
 * <p>Flipping a flag is a legitimate experiment, not a bug fix — until one of them is
 * shown to change a published result, we do not know which reading the figures used.
 */
public record Quirks(
        /**
         * Divisor for the summed density weights.
         *
         * <p>ORIGINAL: {@code gl.numTotalActors}, i.e. Firms + People (SpaceTools.java:1623).
         * THESIS (§4.4): "divide by the total number of People in the model run".
         * Difference is 0.2%–0.7% at published agent counts.
         */
        boolean densityDivisorIncludesFirms,

        /**
         * Whether a commute costs the round trip.
         *
         * <p>ORIGINAL: {@code remainingTime = 1 - (distance * commuteCost)} — one way, despite
         * the code's own comment two lines above reading {@code //2*distance = commute cost}.
         * THESIS (§4.6.1): {@code l_af = t_a - 2*t_af}, and the half-day reachability cutoff
         * that follows from it.
         */
        boolean commuteIsRoundTrip,

        /**
         * Amplitude of demand noise.
         *
         * <p>ORIGINAL: {@code amount * (rand - 0.5) * noise}, giving ±noise/2 of the optimum.
         * THESIS (§5.5.2): "if noise=0.5, the optimal quantity is randomised in a range of
         * plus or minus half its total value", which needs ±noise.
         */
        boolean noiseIsFullAmplitude,

        /**
         * Demand when goods are perfect substitutes (ρ ≥ 1).
         *
         * <p>ORIGINAL: {@code cheapest.amount = price * budget} (CESOneTypeUtility.java).
         * LIMIT: the ρ→1⁻ limit of the constrained optimum is {@code budget / price}.
         * These disagree unless price = 1. Affects the two ρ=1 cases published in §5.5.1.
         *
         * <p>Unresolved — flagged to the author, awaiting comparison against the figures.
         */
        boolean substitutesUseLimitFormula
) {

    /** Reproduces the 2013 code. The default: this is what generated the thesis figures. */
    public static final Quirks ORIGINAL = new Quirks(true, false, false, false);

    /** Implements what the thesis text describes, where the two differ. */
    public static final Quirks AS_THESIS_STATES = new Quirks(false, true, true, true);

    /** Human-readable summary of any departures from {@link #ORIGINAL}. */
    public String describe() {
        if (this.equals(ORIGINAL)) return "original 2013 behaviour";
        var sb = new StringBuilder("modified: ");
        if (!densityDivisorIncludesFirms) sb.append("density divisor = People only; ");
        if (commuteIsRoundTrip) sb.append("commute doubled; ");
        if (noiseIsFullAmplitude) sb.append("noise at full amplitude; ");
        if (substitutesUseLimitFormula) sb.append("ρ≥1 uses budget/price; ");
        return sb.toString().trim();
    }
}
