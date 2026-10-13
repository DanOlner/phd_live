package spatialecon.core.engine;

/**
 * A producer. Sells one good and, in the transmission belt models, turns the time it
 * receives into output.
 *
 * <p>The two Firm behaviours in the thesis are quite different animals and share almost no
 * state. A Firm in the partial models is a fixed point of supply with unlimited stock that
 * gropes for a better location once a day (§5.5). A Firm in the transmission belt never
 * moves, but adjusts price on <em>every single sale</em> to chase a stock target (§6.3.1).
 * Both sets of fields live here; a scenario's chosen behaviour uses one or the other.
 */
public final class Firm extends Actor {

    /** Stock level for partial models, standing in for "supply is never a constraint". */
    public static final double EFFECTIVELY_UNLIMITED_STOCK = 100_000;

    // --- Prices -------------------------------------------------------------------

    /** Base price per unit, before any distance cost. */
    public double goodCost;

    /** Cost to move one unit of the good one unit of distance. */
    public double deliveryCost;

    /** Cost to a Person of covering one unit of distance to work here. */
    public double commuteCost;

    /** Wage offered for a full day's labour. */
    public double wage;

    // --- Production and stock (transmission belt only) ----------------------------

    /** Current stock. Starts at zero for transmission Firms; may legitimately go negative. */
    public double stock;

    /** Running total of time currently left with this Firm by People. */
    public double contributedTime;

    /** Total output at the current level of contributed time. */
    public double productionLevel;

    /** Output per unit of input — the efficiency plotted in §6.4.1. */
    public double outputPerUnitTime;

    /** Revenue this turn, and the previous turn's, for the location-crawl feedback. */
    public double revenue, revenueLastTurn;

    // --- Stock targeting state ----------------------------------------------------
    // Named for what they are: differences between consecutive *transactions*, not days.
    // The original called these "...BetweenDays", which is misleading — the granular
    // adjustment fires on every purchase, so with 300 People that is 300 updates a day
    // per Firm (CODE_ANATOMY §5.6).

    /** Stock before the current transaction. */
    public double stockBeforeTransaction;

    /** First difference of stock across transactions. */
    public double stockFirstDiff;

    /** Previous first difference, kept to form the second. */
    public double stockFirstDiffPrevious;

    /** Second difference of stock across transactions. */
    public double stockSecondDiff;

    /** Per-transaction profit target. Zero means "just clear stock". */
    public double markUp;

    /** Running total the markup ratchets up; the Firm skims the difference (§6.3.2). */
    public double movingMarkUpTarget;

    /** Consecutive price rises. A long run indicates the runaway of §6.3.2. */
    public int consecutivePriceRises;

    /** Last price recorded while price was falling; the fallback if a price goes negative. */
    public double lastFallingPrice;

    public Firm(int id) {
        super(id);
    }

    /** Total price to a buyer at this distance: {@code P = p + c·d} (§4.3.1). */
    public double priceAtDistance(double distance) {
        return goodCost + deliveryCost * distance;
    }

    @Override
    public String label() {
        return "Firm " + id();
    }
}
