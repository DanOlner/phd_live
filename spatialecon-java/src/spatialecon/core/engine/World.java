package spatialecon.core.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import spatialecon.core.config.ModelConfig;
import spatialecon.core.config.Parameter;

/**
 * One model run: the config, the agents, the geography and the clock.
 *
 * <p>This is what the original scattered across {@code gl}'s statics, {@code ActorMaker} and
 * {@code Main}. Constructing a World builds the agents, applies per-Firm cost ranging, lays
 * everyone out and wires the day loop. Nothing here is static, so runs are independent.
 */
public final class World {

    private final ModelConfig config;
    private final Space space;
    private final Rng rng;
    private final List<Person> people = new ArrayList<>();
    private final List<Firm> firms = new ArrayList<>();
    private final Timeline timeline;

    public World(ModelConfig config) {
        this.config = config;
        this.space = config.space();
        this.rng = new Rng(config.seed());

        int id = 0;
        for (int i = 0; i < config.peopleCount(); i++) people.add(new Person(id++));
        for (int i = 0; i < config.firmCount(); i++)   firms.add(makeFirm(id++, i));

        config.peoplePlacement().place(people, space, rng);
        config.firmPlacement().place(firms, space, rng);

        this.timeline = new Timeline(rng)
                .phase("people", people, p -> config.personBehaviour().act(p, this))
                .phase("firms",  firms,  f -> config.firmBehaviour().act(f, this));
    }

    /** Build one Firm, then apply the ranging rules to it. */
    private Firm makeFirm(int id, int index) {
        Firm f = new Firm(id);
        f.stock = 0;
        applyCosts(f, index);
        double[] range = config.initialPriceRange();
        if (range != null) f.goodCost = rng.between(range[0], range[1]);
        f.lastFallingPrice = f.goodCost;
        return f;
    }

    /**
     * Push the config's costs onto a Firm, multiplied by its index if ranging is on.
     *
     * @param index position in the Firm list; ranging multiplies the base cost by index+1
     */
    private void applyCosts(Firm f, int index) {
        double multiplier = index + 1;
        // Leave prices alone when they were randomised at setup, or when a Firm is setting
        // its own price dynamically -- clobbering those would undo the whole mechanism.
        if (config.initialPriceRange() == null) {
            f.goodCost = config.baseGoodCost() * (config.rangeGoodCost() ? multiplier : 1);
        }
        f.deliveryCost = config.deliveryCost() * (config.rangeDelivery() ? multiplier : 1);
        f.wage         = config.wage()         * (config.rangeWage()     ? multiplier : 1);
        f.commuteCost  = config.commuteCost()  * (config.rangeCommute()  ? multiplier : 1);
    }

    /**
     * Re-apply the config's costs to every Firm.
     *
     * <p>Needed because a Firm carries its own copy of each cost — it has to, since ranging
     * makes them differ. Changing a cost on the config alone therefore does nothing until it
     * is pushed out here. The original had the same split and solved it the same way, with
     * each mouse control iterating {@code gl.firms} and setting the field on each.
     *
     * <p>Note this overwrites any per-Firm cost a scenario set directly in
     * {@link spatialecon.core.scenario.Scenario#setUp}. Ranging is currently the only
     * supported way to give Firms different costs.
     */
    public void refreshFirmCosts() {
        for (int i = 0; i < firms.size(); i++) applyCosts(firms.get(i), i);
    }

    // --- Live cost changes. Always go through these, never the config directly. ---

    public void setDeliveryCost(double v) { config.setDeliveryCost(v); refreshFirmCosts(); }
    public void setCommuteCost(double v)  { config.setCommuteCost(v);  refreshFirmCosts(); }
    public void setBaseGoodCost(double v) { config.setBaseGoodCost(v); refreshFirmCosts(); }
    public void setWage(double v)         { config.setWage(v);         refreshFirmCosts(); }

    /** Distance cost, bound so that changes actually reach the Firms. */
    public Parameter deliveryCostParam() {
        return Parameter.of("deliv", config::deliveryCost, this::setDeliveryCost)
                .range(0, 50).label("distance cost");
    }

    public Parameter commuteCostParam() {
        return Parameter.of("commute", config::commuteCost, this::setCommuteCost).range(0, 5);
    }

    public Parameter goodCostParam() {
        return Parameter.of("price", config::baseGoodCost, this::setBaseGoodCost).range(0, 10);
    }

    public Parameter wageParam() {
        return Parameter.of("wage", config::wage, this::setWage).range(0, 5);
    }

    /** Density cost is not per-Firm, so it needs no propagation. */
    public Parameter densityCostParam() {
        return config.densityCostParam();
    }

    public ModelConfig config()  { return config; }
    public Space space()         { return space; }
    public Rng rng()             { return rng; }
    public Timeline timeline()   { return timeline; }
    public long day()            { return timeline.day(); }

    public List<Person> people() { return Collections.unmodifiableList(people); }
    public List<Firm> firms()    { return Collections.unmodifiableList(firms); }

    public Person person(int i)  { return people.get(i); }
    public Firm firm(int i)      { return firms.get(i); }

    /** Distance between two actors, under this space's rules. */
    public double distance(Actor a, Actor b) {
        return space.distance(a.position(), b.position());
    }

    /** Distance from a point to an actor. Used when pricing a candidate location. */
    public double distance(Pt from, Actor to) {
        return space.distance(from, to.position());
    }

    /**
     * People within the density radius of a point, excluding one.
     *
     * <p>Genuinely a radius query, unlike the goods search — which every published config
     * short-circuited with {@code peopleConsiderAll}, so that every Person saw every Firm at
     * any distance (CODE_ANATOMY §5.2). Density is local; the goods market is not.
     */
    public List<Person> peopleWithin(Pt centre, double radius, Person exclude) {
        List<Person> found = new ArrayList<>();
        for (Person p : people) {
            if (p == exclude) continue;
            if (space.distance(centre, p.position()) < radius) found.add(p);
        }
        return found;
    }

    /**
     * The normalised density weight at a point, before scaling (§4.4).
     *
     * <p>Each neighbour inside the radius contributes a weight tapering linearly from 1
     * (co-located) to 0 (at the radius). The sum is divided by the divisor — which is the
     * subject of {@link spatialecon.core.config.Quirks}, since the original used Firms plus
     * People where the thesis text says People alone.
     *
     * <p>Note this is genuinely a radius query. The goods search is not: every published
     * config set {@code peopleConsiderAll}, so every Person saw every Firm at any distance
     * (CODE_ANATOMY §5.2). Proximity is local; the market is not.
     */
    public double rawDensityAt(Pt point, Person exclude) {
        if (config.densityCost() <= 0) return 0;
        double radius = config.densityRadius();
        double weight = 0;
        for (Person other : people) {
            if (other == exclude) continue;
            double d = space.distance(point, other.position());
            if (d < radius) weight += 1 - (d / radius);
        }
        return weight / config.densityDivisor();
    }

    /** How many others fall inside a radius of this point. Recorded for §5.4.2. */
    public int countNeighbours(Pt point, Person exclude, double radius) {
        int count = 0;
        for (Person other : people) {
            if (other == exclude) continue;
            if (space.distance(point, other.position()) < radius) count++;
        }
        return count;
    }

    /** Density cost actually charged at a point: the raw weight, scaled. */
    public double densityCostAt(Pt point, Person forPerson) {
        return config.densityCost() * rawDensityAt(point, forPerson);
    }

    /**
     * Mean distance from each actor to its nearest neighbour of the same kind.
     *
     * <p>The measure behind the "black hole" result of §5.3.2: with nothing to counteract
     * distance costs, this collapses towards zero as everyone piles onto one spot.
     */
    public double meanNearestNeighbour(List<? extends Actor> group) {
        if (group.size() < 2) return 0;
        double total = 0;
        for (Actor a : group) {
            double nearest = Double.MAX_VALUE;
            for (Actor b : group) {
                if (a == b) continue;
                nearest = Math.min(nearest, space.distance(a.position(), b.position()));
            }
            total += nearest;
        }
        return total / group.size();
    }

    /** Mean distance moved by People last turn — the spatial-equilibrium test of §5.4.3. */
    public double meanPersonMovement() {
        return people.stream().mapToDouble(Actor::distanceMovedLastTurn).average().orElse(0);
    }
}
