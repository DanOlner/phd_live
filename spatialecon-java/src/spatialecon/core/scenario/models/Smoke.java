package spatialecon.core.scenario.models;

import java.util.List;

import spatialecon.core.behaviour.PersonBehaviour;
import spatialecon.core.config.ModelConfig;
import spatialecon.core.config.Parameter;
import spatialecon.core.econ.Ces;
import spatialecon.core.econ.SmoothEoS;
import spatialecon.core.engine.Firm;
import spatialecon.core.engine.Placement;
import spatialecon.core.engine.Space;
import spatialecon.core.engine.World;
import spatialecon.core.record.Aggregation;
import spatialecon.core.record.Recorder;
import spatialecon.core.scenario.Scenario;

/**
 * Not a thesis model — a scaffold check.
 *
 * <p>Exercises the machinery end to end: agents are built and placed, the day loop runs its
 * two phases in order, a behaviour reads the world and writes to agent state, the recorder
 * samples both per-actor and aggregated series, and parameters bind to live objects. If this
 * runs and the CSV looks sane, the plumbing is sound.
 *
 * <p>Delete once real scenarios exist, or keep as the smallest possible regression test.
 */
public final class Smoke implements Scenario {

    private final Ces ces = new Ces(0.5);
    private final SmoothEoS production = new SmoothEoS(1, 1);

    @Override public String id()         { return "smoke"; }
    @Override public String title()      { return "Scaffold check (not a thesis model)"; }
    @Override public String thesisRef()  { return "—"; }
    @Override public int defaultDays()   { return 50; }

    @Override
    public String notes() {
        return "Verifies the engine, recorder and parameter wiring. Each Person prices every "
             + "Firm from where they stand, takes the CES optimum for a budget of one day, and "
             + "stores the resulting utility. Nobody moves.";
    }

    @Override
    public ModelConfig config() {
        return ModelConfig.builder()
                .space(Space.Kind.PLANE).width(1.0)
                .people(20).firms(3)
                .placePeople(Placement.random())
                .placeFirms(Placement.inCircle(0.25))
                .utility(ces)
                .production(production)
                .baseGoodCost(1.0)
                .deliveryCost(0.5)
                .personBehaviour(new BuyFromEveryone())
                .peopleMobile(false)
                .build();
    }

    @Override
    public void record(Recorder r, World w) {
        r.perActor("utility", w.people(), p -> p.utility);
        r.aggregate("utilityMean", w.people(), p -> p.utility, Aggregation.MEAN_SD);
        r.aggregate("firmPrice", w.firms(), f -> f.goodCost, Aggregation.MIN_MAX);
    }

    @Override
    public List<Parameter> controls(World w) {
        return List.of(ces.rhoParam(), w.deliveryCostParam());
    }

    /**
     * Buy the CES-optimal mix from every Firm, paying the distance cost. No movement, no
     * production, no stock — just enough to prove demand flows through the machinery.
     */
    private static final class BuyFromEveryone implements PersonBehaviour {

        @Override
        public void act(spatialecon.core.engine.Person person, World world) {
            List<Firm> firms = world.firms();
            double[] prices = new double[firms.size()];
            for (int i = 0; i < firms.size(); i++) {
                Firm f = firms.get(i);
                prices[i] = f.priceAtDistance(world.distance(person, f));
            }
            double[] quantities = world.config().utility()
                    .optimalQuantities(prices, spatialecon.core.engine.Person.TIME_PER_DAY);
            person.utility = world.config().utility().utilityOf(quantities);
            person.densityCost = world.densityCostAt(person.position(), person);
        }

        @Override
        public String describe() { return "buy optimal mix from all Firms"; }
    }
}
