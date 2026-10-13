package spatialecon.core.config;

import spatialecon.core.behaviour.FirmBehaviour;
import spatialecon.core.behaviour.PersonBehaviour;
import spatialecon.core.econ.Production;
import spatialecon.core.econ.Utility;
import spatialecon.core.engine.Placement;
import spatialecon.core.engine.Space;

/**
 * Everything a model run needs to know about itself.
 *
 * <p>Replaces {@code utils/gl.java} — 131 public static fields and four near-duplicate
 * setUp methods. The statics split three ways: structural choices, which are final here and
 * fixed once a run starts; tunable costs, which stay mutable because scenarios expose them
 * as live {@link Parameter}s; and dead flags, which are simply gone.
 *
 * <p>Nothing is static. Two models can run in one process, which the original could not do.
 */
public final class ModelConfig {

    // --- Structure: fixed for the run ---------------------------------------------

    private final Space space;
    private final int peopleCount;
    private final int firmCount;
    private final Placement peoplePlacement;
    private final Placement firmPlacement;
    private final Utility utility;
    private final Production production;
    private final PersonBehaviour personBehaviour;
    private final FirmBehaviour firmBehaviour;
    private final long seed;
    private final Quirks quirks;

    // --- Costs and switches: tunable at runtime -----------------------------------

    private double deliveryCost;
    private double commuteCost;
    private double baseGoodCost;
    private double wage;
    private double densityCost;
    private double densityRadius;
    private double demandNoise;
    private boolean peopleMobile;
    private boolean firmsMobile;

    /**
     * Per-Firm cost ranging. When set, Firm <i>i</i> gets {@code base * (i+1)}.
     *
     * <p>This is how the two-Firm scenario of §5.4.6 gets its delivery costs of 2 and 4 from
     * a single base of 2, and how the four wage bands of §5.4.5 come from a base of 1.
     * Easy to miss, and it means the headline parameter is a base, not the value any
     * particular Firm sees.
     */
    private boolean rangeDelivery, rangeGoodCost, rangeWage, rangeCommute;

    /**
     * Range for randomising each Firm's starting price, or null for {@link #baseGoodCost}.
     *
     * <p>Not cosmetic. The transmission belt is sensitive to where prices start: from a
     * uniform 3.0 with distance costs, several Firms overshoot into the runaway of §6.3.2 and
     * never recover, while the thesis' randomised 0.1-1.5 settles cleanly. Starting near the
     * equilibrium keeps the first few price corrections small.
     */
    private final double[] initialPriceRange;

    private ModelConfig(Builder b) {
        this.initialPriceRange = b.initialPriceRange;
        this.space = new Space(b.spaceKind, b.width);
        this.peopleCount = b.peopleCount;
        this.firmCount = b.firmCount;
        this.peoplePlacement = b.peoplePlacement;
        this.firmPlacement = b.firmPlacement;
        this.utility = b.utility;
        this.production = b.production;
        this.personBehaviour = b.personBehaviour;
        this.firmBehaviour = b.firmBehaviour;
        this.seed = b.seed;
        this.quirks = b.quirks;
        this.deliveryCost = b.deliveryCost;
        this.commuteCost = b.commuteCost;
        this.baseGoodCost = b.baseGoodCost;
        this.wage = b.wage;
        this.densityCost = b.densityCost;
        this.densityRadius = b.densityRadius;
        this.demandNoise = b.demandNoise;
        this.peopleMobile = b.peopleMobile;
        this.firmsMobile = b.firmsMobile;
        this.rangeDelivery = b.rangeDelivery;
        this.rangeGoodCost = b.rangeGoodCost;
        this.rangeWage = b.rangeWage;
        this.rangeCommute = b.rangeCommute;
    }

    public static Builder builder() { return new Builder(); }

    // --- Structural accessors -----------------------------------------------------

    public Space space()                    { return space; }
    public int peopleCount()                { return peopleCount; }
    public int firmCount()                  { return firmCount; }
    public Placement peoplePlacement()      { return peoplePlacement; }
    public Placement firmPlacement()        { return firmPlacement; }
    public Utility utility()                { return utility; }
    public Production production()          { return production; }
    public PersonBehaviour personBehaviour() { return personBehaviour; }
    public FirmBehaviour firmBehaviour()    { return firmBehaviour; }
    public long seed()                      { return seed; }
    public Quirks quirks()                  { return quirks; }

    /** Total actors. The divisor for density cost under {@link Quirks#ORIGINAL}. */
    public int totalActors() { return peopleCount + firmCount; }

    /** Divisor for the density cost sum — the choice {@link Quirks} exists to make explicit. */
    public int densityDivisor() {
        return quirks.densityDivisorIncludesFirms() ? totalActors() : peopleCount;
    }

    // --- Tunable accessors --------------------------------------------------------

    public double deliveryCost()  { return deliveryCost; }
    public double commuteCost()   { return commuteCost; }
    public double baseGoodCost()  { return baseGoodCost; }
    public double wage()          { return wage; }
    public double densityCost()   { return densityCost; }
    public double densityRadius() { return densityRadius; }
    public double demandNoise()   { return demandNoise; }
    public boolean peopleMobile() { return peopleMobile; }
    public boolean firmsMobile()  { return firmsMobile; }

    public boolean rangeDelivery() { return rangeDelivery; }
    public boolean rangeGoodCost() { return rangeGoodCost; }
    public boolean rangeWage()     { return rangeWage; }
    public boolean rangeCommute()  { return rangeCommute; }

    /** Randomised starting price range, or null to use {@link #baseGoodCost}. */
    public double[] initialPriceRange() { return initialPriceRange; }

    public void setDeliveryCost(double v)  { this.deliveryCost = v; }
    public void setCommuteCost(double v)   { this.commuteCost = v; }
    public void setBaseGoodCost(double v)  { this.baseGoodCost = v; }
    public void setWage(double v)          { this.wage = v; }
    public void setDensityCost(double v)   { this.densityCost = v; }
    public void setDensityRadius(double v) { this.densityRadius = v; }
    public void setDemandNoise(double v)   { this.demandNoise = v; }
    public void setPeopleMobile(boolean v) { this.peopleMobile = v; }
    public void setFirmsMobile(boolean v)  { this.firmsMobile = v; }

    // --- Ready-made parameter bindings --------------------------------------------

    public Parameter deliveryCostParam() {
        return Parameter.of("deliv", this::deliveryCost, this::setDeliveryCost)
                .range(0, 50).label("distance cost");
    }

    public Parameter densityCostParam() {
        return Parameter.of("density", this::densityCost, this::setDensityCost)
                .range(0, 500).label("density cost");
    }

    public Parameter commuteCostParam() {
        return Parameter.of("commute", this::commuteCost, this::setCommuteCost).range(0, 5);
    }

    public Parameter wageParam() {
        return Parameter.of("wage", this::wage, this::setWage).range(0, 5);
    }

    public Parameter goodCostParam() {
        return Parameter.of("price", this::baseGoodCost, this::setBaseGoodCost).range(0, 10);
    }

    public String summary() {
        return """
               space=%s  people=%d  firms=%d
               utility=%s  production=%s
               person=%s  firm=%s
               delivery=%.4f  commute=%.4f  goodCost=%.4f  wage=%.4f
               density=%.4f (radius %.5f, divisor %d)  noise=%.4f
               mobile: people=%b firms=%b   seed=%d
               quirks: %s"""
                .formatted(space, peopleCount, firmCount,
                        utility.describe(), production.describe(),
                        personBehaviour.describe(), firmBehaviour.describe(),
                        deliveryCost, commuteCost, baseGoodCost, wage,
                        densityCost, densityRadius, densityDivisor(), demandNoise,
                        peopleMobile, firmsMobile, seed, quirks.describe());
    }

    // --- Builder ------------------------------------------------------------------

    public static final class Builder {
        private Space.Kind spaceKind = Space.Kind.PLANE;
        private double width = 1;
        private int peopleCount = 1;
        private int firmCount = 1;
        private Placement peoplePlacement = Placement.random();
        private Placement firmPlacement = Placement.centre();
        private Utility utility;
        private Production production;
        private PersonBehaviour personBehaviour = PersonBehaviour.NONE;
        private FirmBehaviour firmBehaviour = FirmBehaviour.NONE;
        private long seed = 12;                 // the original's default
        private Quirks quirks = Quirks.ORIGINAL;
        private double deliveryCost = 0;
        private double commuteCost = 0;
        private double baseGoodCost = 1;
        private double wage = 1;
        private double densityCost = 0;
        private double densityRadius = 1.0 / 32;
        private double demandNoise = 0;
        private boolean peopleMobile = false;
        private boolean firmsMobile = false;
        private boolean rangeDelivery, rangeGoodCost, rangeWage, rangeCommute;
        private double[] initialPriceRange;

        public Builder space(Space.Kind kind)   { this.spaceKind = kind; return this; }
        public Builder width(double w)          { this.width = w; return this; }
        public Builder people(int n)            { this.peopleCount = n; return this; }
        public Builder firms(int n)             { this.firmCount = n; return this; }
        public Builder placePeople(Placement p) { this.peoplePlacement = p; return this; }
        public Builder placeFirms(Placement p)  { this.firmPlacement = p; return this; }
        public Builder utility(Utility u)       { this.utility = u; return this; }
        public Builder production(Production p) { this.production = p; return this; }
        public Builder personBehaviour(PersonBehaviour b) { this.personBehaviour = b; return this; }
        public Builder firmBehaviour(FirmBehaviour b)     { this.firmBehaviour = b; return this; }
        public Builder seed(long s)             { this.seed = s; return this; }
        public Builder quirks(Quirks q)         { this.quirks = q; return this; }
        public Builder deliveryCost(double v)   { this.deliveryCost = v; return this; }
        public Builder commuteCost(double v)    { this.commuteCost = v; return this; }
        public Builder baseGoodCost(double v)   { this.baseGoodCost = v; return this; }
        public Builder wage(double v)           { this.wage = v; return this; }
        public Builder densityCost(double v)    { this.densityCost = v; return this; }
        public Builder densityRadius(double v)  { this.densityRadius = v; return this; }
        public Builder demandNoise(double v)    { this.demandNoise = v; return this; }
        public Builder peopleMobile(boolean v)  { this.peopleMobile = v; return this; }
        public Builder firmsMobile(boolean v)   { this.firmsMobile = v; return this; }
        public Builder rangeDelivery(boolean v) { this.rangeDelivery = v; return this; }
        public Builder rangeGoodCost(boolean v) { this.rangeGoodCost = v; return this; }
        public Builder rangeWage(boolean v)     { this.rangeWage = v; return this; }
        public Builder rangeCommute(boolean v)  { this.rangeCommute = v; return this; }

        /** Randomise each Firm's starting price in this range. See the field's note. */
        public Builder randomiseInitialPrices(double from, double to) {
            this.initialPriceRange = new double[]{ from, to };
            return this;
        }

        public ModelConfig build() {
            if (utility == null)    throw new IllegalStateException("scenario must set a utility function");
            if (production == null) throw new IllegalStateException("scenario must set a production function");
            return new ModelConfig(this);
        }
    }
}
