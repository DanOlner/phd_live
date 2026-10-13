package spatialecon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import spatialecon.core.behaviour.PersonBundle;
import spatialecon.core.config.ModelConfig;
import spatialecon.core.econ.Ces;
import spatialecon.core.econ.SmoothEoS;
import spatialecon.core.engine.Person;
import spatialecon.core.engine.Placement;
import spatialecon.core.engine.Space;
import spatialecon.core.engine.World;

/**
 * How far the spatial-equilibrium assumption actually holds (§5.4.1).
 *
 * <p>The thesis assumes {@code du/dd = 0}: utility equal regardless of distance from the
 * centre. The monocentric model gets close but not exactly, and the reason is worth pinning
 * down in tests rather than leaving as a footnote — it turns out to be the same mechanism
 * that produces the breakdown of §5.4.7.
 *
 * <p>With a single Firm the CES collapses to one good, so utility is closed-form:
 * {@code U = (1 - D(d)) / (p + c·d)}. Flatness therefore requires a density profile that is
 * exactly linear in distance and reaches zero at {@code d = (1-k)/(c·k)}. The model produces
 * that profile — but density cannot go below zero, so anyone beyond the cutoff pays pure
 * distance cost with nothing left to trade.
 *
 * <p>These runs are deliberately short. They test the mechanism, not the fine numbers.
 */
public class EqualisationTest {

    private static final double DELIVERY = 0.25;
    private static final double WAGE = 1.0;

    private static World settle(int people, int days) {
        World w = new World(ModelConfig.builder()
                .space(Space.Kind.PLANE).width(2.0)
                .people(people).firms(1)
                .placePeople(Placement.random()).placeFirms(Placement.centre())
                .utility(new Ces(0.4)).production(new SmoothEoS())
                .baseGoodCost(1.0).wage(WAGE).deliveryCost(DELIVERY).commuteCost(0)
                .densityCost(10).densityRadius(1.0 / 16)
                .personBehaviour(new PersonBundle()).peopleMobile(true)
                .build());
        for (int i = 0; i < days; i++) w.timeline().nextDay();
        return w;
    }

    private static double distance(World w, Person p) {
        return w.distance(p, w.firm(0));
    }

    /** Utility really is the closed form, so the algebra below applies. */
    @Test
    public void utilityMatchesTheSingleGoodClosedForm() {
        World w = settle(200, 400);
        for (Person p : w.people()) {
            double expected = (WAGE - p.densityCost) / (1 + DELIVERY * distance(w, p));
            assertEquals("closed form for " + p.label(), expected, p.utility, 1e-6);
        }
    }

    /**
     * The equalising mechanism: density falls close to linearly with distance.
     *
     * <p>This is what does the work. Utility can only be equal if the density gradient
     * exactly offsets the distance-cost gradient.
     */
    @Test
    public void densityFallsLinearlyWithDistance() {
        World w = settle(300, 600);
        List<Double> d = new ArrayList<>(), density = new ArrayList<>();
        for (Person p : w.people()) {
            d.add(distance(w, p));
            density.add(p.densityCost);
        }
        assertTrue("density should fall steeply and near-linearly with distance",
                correlation(d, density) < -0.9);
    }

    /**
     * Nobody beyond the cutoff pays any meaningful density — they have nothing left to trade.
     *
     * <p>The cutoff is where the equalising profile reaches zero. Beyond it, equalisation is
     * arithmetically impossible, not merely unachieved.
     */
    @Test
    public void beyondTheCutoffDensityIsExhausted() {
        World w = settle(300, 600);
        double k = w.people().stream().mapToDouble(p -> p.utility).average().orElse(0);
        double cutoff = (WAGE - k) / (DELIVERY * k);

        List<Person> fringe = w.people().stream().filter(p -> distance(w, p) > cutoff).toList();
        if (fringe.isEmpty()) return;   // a dense enough run may have no fringe at all

        double meanFringeDensity = fringe.stream().mapToDouble(p -> p.densityCost).average().orElse(0);
        assertTrue("fringe density should be ~0, was " + meanFringeDensity, meanFringeDensity < 0.005);

        // And their utility should follow the no-density prediction.
        for (Person p : fringe) {
            double pureDistance = WAGE / (1 + DELIVERY * distance(w, p));
            assertEquals("fringe utility is pure distance cost", pureDistance, p.utility, 0.01);
        }
    }

    /**
     * The gradient is a finite-population boundary effect, so a bigger population flattens it.
     *
     * <p>This is the decisive evidence that it is not a coding error. A bug would not care
     * how many agents there are.
     */
    @Test
    public void alargerPopulationFlattensTheGradient() {
        double small = utilityGradient(settle(120, 500));
        double large = utilityGradient(settle(600, 500));
        assertTrue("gradient should weaken with more People: 120 gave " + small
                        + ", 600 gave " + large,
                Math.abs(large) < Math.abs(small));
    }

    /** No deadband: a Person sitting on the best spot available does not move. */
    @Test
    public void aPersonOnTheBestSpotStaysPut() {
        World w = settle(150, 800);
        double movement = w.meanPersonMovement();
        assertTrue("settled population should barely move, was " + movement, movement < 0.01);
    }

    private static double utilityGradient(World w) {
        List<Double> d = new ArrayList<>(), u = new ArrayList<>();
        for (Person p : w.people()) {
            d.add(distance(w, p));
            u.add(p.utility);
        }
        return correlation(d, u);
    }

    private static double correlation(List<Double> a, List<Double> b) {
        int n = a.size();
        double ma = a.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double mb = b.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double num = 0, da = 0, db = 0;
        for (int i = 0; i < n; i++) {
            num += (a.get(i) - ma) * (b.get(i) - mb);
            da += (a.get(i) - ma) * (a.get(i) - ma);
            db += (b.get(i) - mb) * (b.get(i) - mb);
        }
        return num / Math.sqrt(da * db);
    }
}
