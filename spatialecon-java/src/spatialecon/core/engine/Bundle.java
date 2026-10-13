package spatialecon.core.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * A candidate location, with all the economics attached (§4.6.1).
 *
 * <p>A Person does not search the whole map. They assemble a handful of Bundles — a few
 * nearby, a few anywhere, plus one for where they already are — work out what each would be
 * worth, and move to the best. Each Bundle answers, from <em>this</em> spot: what is the best
 * wage I could get, what would every good cost me delivered here, what density would I incur,
 * and so what utility could I reach?
 *
 * <p>The whole framework hangs off this. It is why the density cost is computable at all —
 * every agent evaluates proximity from their own position — and why no market-clearing price
 * is needed, since two People at different spots simply face different prices.
 */
public final class Bundle {

    /** Offset from the Person's current position. Zero means "stay where I am". */
    public final Pt offset;

    /** Absolute location this Bundle refers to. */
    public final Pt location;

    /** Firms whose goods are available, parallel to {@link #prices}. */
    public final List<Firm> sellers = new ArrayList<>();

    /** Full delivered price per unit from each seller, at this location. */
    public final List<Double> prices = new ArrayList<>();

    /** Best employer found from here, or null if none is reachable. */
    public Firm employer;

    /** Budget: the wage from {@link #employer} after commuting, then less density cost. */
    public double budget;

    /** Time left to give an employer after commuting. */
    public double workTime;

    /** Density weight before scaling — the normalised neighbour sum. */
    public double rawDensityCost;

    /** Density cost actually charged: {@code densityCost × raw}. */
    public double densityCost;

    /** How many others fall inside the density radius here. */
    public int neighbours;

    /** Utility achievable here, once optimised. */
    public double utility;

    /** Optimal quantity of each good, parallel to {@link #sellers}. */
    public double[] quantities = new double[0];

    public Bundle(Pt offset, Pt location) {
        this.offset = offset;
        this.location = location;
    }

    public void addSeller(Firm firm, double price) {
        sellers.add(firm);
        prices.add(price);
    }

    public double[] pricesArray() {
        double[] out = new double[prices.size()];
        for (int i = 0; i < out.length; i++) out[i] = prices.get(i);
        return out;
    }

    public boolean hasSellers() { return !sellers.isEmpty(); }

    /**
     * Whether this location is worth considering at all.
     *
     * <p>A non-positive budget means the density cost here has swallowed the entire wage, so
     * the Bundle is discarded rather than allowed to go negative. That single rule produces
     * the dispersal of §5.4.7: raise the density cost far enough and every central location
     * fails this test, so People end up spread to the edges — not because they are pushed,
     * but because nowhere crowded is affordable.
     */
    public boolean isViable() { return budget > 0 && hasSellers(); }

    @Override
    public String toString() {
        return "Bundle@%s budget=%.4f density=%.4f utility=%.4f"
                .formatted(location, budget, densityCost, utility);
    }
}
