package spatialecon.core.behaviour;

import spatialecon.core.econ.Production;
import spatialecon.core.engine.Firm;
import spatialecon.core.engine.World;

/**
 * Granular, damped stock targeting — the engine of the transmission belt (§6.3.1).
 *
 * <p>On every single sale, a Firm: hands over the goods, puts the labour it received straight
 * into production, and nudges its price towards whatever would keep stock at target. There is
 * no deliberation and no waiting. Leijonhufvud's "law of motion" rather than a decision.
 *
 * <p><b>Why the derivatives are there.</b> Targeting stock alone does not work, and the way it
 * fails is instructive: when stock reaches zero the price is at its furthest from equilibrium,
 * so the system sails through and comes back the other way. The result is a perfectly stable
 * oscillation that never settles. It is the classic undamped harmonic oscillator, sitting in
 * the middle of an economic model. Targeting the first and second differences as well —
 * aiming for stock, its rate of change, and the change in that rate all to be zero together —
 * damps it out.
 *
 * <pre>
 *   target = s + s' + s'' − markupTarget
 *   Δprice = |target| / responseDivisor,  signed so stock moves back towards target
 * </pre>
 *
 * <p>Two implementation details worth stating, because both look like mistakes and are not:
 *
 * <ul>
 *   <li><b>The differences are per-<em>sale</em>, not per-day.</b> The original called them
 *       "...BetweenDays", which is misleading — with 300 People this fires 300 times a day per
 *       Firm. Oscillation frequencies depend on it.
 *   <li><b>Magnitude and polarity come from different quantities.</b> The size of the price
 *       change is taken from the damped sum, but its <em>direction</em> from raw stock against
 *       the markup target. A literal reading of the thesis equation would use the damped sum
 *       for both. This reproduces the code that produced the figures.
 * </ul>
 *
 * <p>Firms may also skim a {@code markUp}: a per-sale increment to a running target, so stock
 * must keep rising and the Firm takes the difference (§6.3.2).
 */
public final class FirmStockTarget implements FirmBehaviour {

    /** The original's hardcoded {@code adjustCostAmount}: the {@code x} in Δp = target/x. */
    public static final double DEFAULT_RESPONSE_DIVISOR = 100;

    private final double responseDivisor;
    private final boolean useFirstDifference;
    private final boolean useSecondDifference;

    /** Fully damped, as every published transmission belt run was. */
    public FirmStockTarget() {
        this(DEFAULT_RESPONSE_DIVISOR, true, true);
    }

    public FirmStockTarget(double responseDivisor) {
        this(responseDivisor, true, true);
    }

    /**
     * @param useFirstDifference  include s'. Off gives the undamped oscillation of §6.3.1.
     * @param useSecondDifference include s''.
     */
    public FirmStockTarget(double responseDivisor,
                           boolean useFirstDifference, boolean useSecondDifference) {
        this.responseDivisor = responseDivisor;
        this.useFirstDifference = useFirstDifference;
        this.useSecondDifference = useSecondDifference;
    }

    /** Nothing scheduled. Everything happens in {@link #onSale}. */
    @Override
    public void act(Firm firm, World world) { }

    @Override
    public void onSale(Firm firm, double units, double input, World world) {
        Production production = world.config().production();

        firm.stockBeforeTransaction = firm.stock;

        // Hand over the goods.
        firm.stock -= units;

        // The labour received goes straight in, and the buyer's proportional share of the
        // resulting output comes straight back out as stock: g_a = f(T)·(t_a/T).
        firm.contributedTime += input;
        firm.stock += production.granularShare(firm.contributedTime, input);

        firm.productionLevel = production.outputFor(firm.contributedTime);
        firm.outputPerUnitTime = production.outputPerUnit(firm.contributedTime);

        // Differences across successive sales.
        firm.stockFirstDiffPrevious = firm.stockFirstDiff;
        firm.stockFirstDiff = firm.stock - firm.stockBeforeTransaction;
        firm.stockSecondDiff = firm.stockFirstDiff - firm.stockFirstDiffPrevious;

        // Markup ratchets: the target keeps rising, and the Firm keeps the difference.
        firm.movingMarkUpTarget += firm.markUp;

        double target = firm.stock - firm.movingMarkUpTarget;
        if (useFirstDifference) target += firm.stockFirstDiff;
        if (useSecondDifference) target += firm.stockSecondDiff;

        double change = Math.abs(target) / responseDivisor;
        change = firm.stock < firm.movingMarkUpTarget ? change : -change;

        double previousPrice = firm.goodCost;
        firm.goodCost += change;

        // A negative price is meaningless. The original fell back to the last price seen
        // while price was falling, rather than simply the previous price -- which matters,
        // because reverting to the previous price pins a crashing Firm in place forever.
        if (firm.goodCost <= 0) {
            firm.goodCost = firm.lastFallingPrice > 0 ? firm.lastFallingPrice : previousPrice / 2;
        }

        // A long run of rises means the Firm has lost so much demand it can no longer reach
        // its target, and is compounding the error. The runaway of §6.3.2.
        if (firm.goodCost > previousPrice) {
            firm.consecutivePriceRises++;
        } else {
            firm.consecutivePriceRises = 0;
            firm.lastFallingPrice = firm.goodCost;
        }
    }

    @Override
    public String describe() {
        String damping = useFirstDifference && useSecondDifference ? "s+s'+s''"
                : useFirstDifference ? "s+s'" : "s only (undamped)";
        return "granular stock target (%s, divisor %.0f)".formatted(damping, responseDivisor);
    }
}
