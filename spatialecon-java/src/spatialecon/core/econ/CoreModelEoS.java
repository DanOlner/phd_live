package spatialecon.core.econ;

/**
 * The Core Model's production function (§2.2.5), rearranged for output:
 *
 * <pre>
 *   l = α + βx      →      x = (l − α) / β
 * </pre>
 *
 * <p>α is the fixed labour requirement, β the marginal. Perfectly serviceable algebraically,
 * and unusable in an agent model: below α it returns <em>negative</em> output. Nobody can
 * start producing, and no single agent supplying one unit of labour can ever get output
 * above zero on their own.
 *
 * <p>That is the whole "blind versus rational choice" argument of §4.7.6. An agent who
 * reasons about it correctly concludes there is no point working, and production never
 * starts. An agent who guesses randomly and repeats whatever seemed to help will eventually
 * stumble into a group large enough to clear α — and does better precisely by reasoning
 * less. It is why the thesis uses {@link SmoothEoS} instead.
 *
 * <p>Kept so that failure can be demonstrated rather than described.
 */
public final class CoreModelEoS implements Production {

    private final double fixedLabour;    // alpha
    private final double marginalLabour; // beta

    /** The thesis' worked example: α = 3, β = 1. */
    public CoreModelEoS() {
        this(3, 1);
    }

    public CoreModelEoS(double fixedLabour, double marginalLabour) {
        if (marginalLabour <= 0) {
            throw new IllegalArgumentException("marginal labour must be positive, got " + marginalLabour);
        }
        this.fixedLabour = fixedLabour;
        this.marginalLabour = marginalLabour;
    }

    public double fixedLabour()    { return fixedLabour; }
    public double marginalLabour() { return marginalLabour; }

    /**
     * Output for this labour input. <strong>Negative below α</strong> — deliberately not
     * clamped, because the negative region is the thing being demonstrated.
     */
    @Override
    public double outputFor(double labour) {
        return (labour - fixedLabour) / marginalLabour;
    }

    /** Whether any output is possible at this input. False below the fixed requirement. */
    public boolean canProduceAt(double labour) {
        return labour >= fixedLabour;
    }

    @Override
    public String describe() {
        return "core model EoS (alpha=%.4g, beta=%.4g)".formatted(fixedLabour, marginalLabour);
    }
}
