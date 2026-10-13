package spatialecon.core.engine;

/**
 * A consumer. Has one unit of time per day and spends it to maximise utility.
 *
 * <p>What the time buys depends on the model. In the partial models it buys a wage, which
 * then buys goods. In the transmission belt it buys goods directly, so the goods
 * <em>are</em> the wage (§6.3). Both are the same question from the agent's side: what is
 * the most utility I can get for my day?
 */
public final class Person extends Actor {

    /** The day's budget. Always 1 — "one day per day" (§4.2). */
    public static final double TIME_PER_DAY = 1.0;

    /** Utility achieved on the most recent turn. */
    public double utility;

    /** Density cost incurred at the location chosen on the most recent turn. */
    public double densityCost;

    /** Number of others inside the density radius. Recorded for §5.4.2. */
    public int neighboursInDensityRadius;

    /**
     * Firm this Person is tied to in the von Thünen scenarios (§5.4.5), where each Person
     * is permanently assigned to one Firm and may neither work nor buy elsewhere.
     * Null in every other model.
     */
    public Firm assignedFirm;

    public Person(int id) {
        super(id);
    }

    @Override
    public String label() {
        return "Person " + id();
    }
}
