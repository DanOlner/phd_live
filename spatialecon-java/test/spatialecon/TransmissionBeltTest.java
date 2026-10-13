package spatialecon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import spatialecon.core.behaviour.FirmStockTarget;
import spatialecon.core.behaviour.PersonTransmission;
import spatialecon.core.config.ModelConfig;
import spatialecon.core.econ.Ces;
import spatialecon.core.econ.SmoothEoS;
import spatialecon.core.engine.Placement;
import spatialecon.core.engine.Space;
import spatialecon.core.engine.World;

/**
 * The transmission belt (§6.3).
 *
 * <p>Unusually for an agent model, several equilibria here are deducible by hand, which makes
 * this the strongest available check on the whole mechanism. With {@code x = l² + l}, a Firm
 * receiving {@code T} units of time clears its stock at price {@code 1/(T+1)}.
 */
public class TransmissionBeltTest {

    private static final double EPS = 1e-4;

    private static World settle(int firms, int people, double rho, double distance,
                                boolean damped, int days) {
        World w = new World(ModelConfig.builder()
                .space(distance > 0 ? Space.Kind.PLANE : Space.Kind.POINT).width(1.0)
                .people(people).firms(firms)
                .placePeople(Placement.random()).placeFirms(Placement.random())
                .utility(new Ces(rho)).production(new SmoothEoS(1, 1))
                .baseGoodCost(1.0).deliveryCost(distance)
                .randomiseInitialPrices(0.1, 1.5)
                .personBehaviour(new PersonTransmission())
                .firmBehaviour(new FirmStockTarget(FirmStockTarget.DEFAULT_RESPONSE_DIVISOR,
                        damped, damped))
                .peopleMobile(false).seed(12)
                .build());
        for (int i = 0; i < days; i++) w.timeline().nextDay();
        return w;
    }

    /** One Firm, one Person: T = 1, so the clearing price is 1/2. Thesis §6.3.1. */
    @Test
    public void singleFirmSinglePersonClearsAtAHalf() {
        World w = settle(1, 1, 0.5, 0, true, 3000);
        assertEquals(0.5, w.firm(0).goodCost, EPS);
        assertEquals("stock should clear", 0.0, w.firm(0).stock, 1e-6);
    }

    /** Two Firms, one Person: each gets T = 0.5, f(0.5) = 0.75, so price = 2/3. */
    @Test
    public void symmetricTwoFirmEquilibriumIsTwoThirds() {
        World w = settle(2, 1, 0.5, 0, true, 3000);
        assertEquals(2.0 / 3, w.firm(0).goodCost, EPS);
        assertEquals(2.0 / 3, w.firm(1).goodCost, EPS);
    }

    /** 50 Firms and 50 People: each Firm again receives one unit of time, so again 0.5. */
    @Test
    public void manyAgentsStillClearAtTheDeducedPrice() {
        World w = settle(50, 50, 0.5, 0, true, 2000);
        for (var f : w.firms()) assertEquals(0.5, f.goodCost, EPS);
    }

    /** 20 Firms, 100 People: each Firm gets T = 5, so the price is 1/6. */
    @Test
    public void clearingPriceFollowsTheInputLevel() {
        World w = settle(20, 100, 0.5, 0, true, 2000);
        for (var f : w.firms()) assertEquals(1.0 / 6, f.goodCost, EPS);
    }

    /**
     * Utility is identical across People at every instant — "umbilically linked" (§6.3.1).
     *
     * <p>Not merely similar. Everyone takes whatever improvement is available at their moment
     * of decision, so with no intrinsic differences between them there is nothing to separate
     * them.
     */
    @Test
    public void everyPersonGetsIdenticalUtility() {
        World w = settle(20, 50, 0.5, 0, true, 1500);
        double first = w.person(0).utility;
        for (var p : w.people()) assertEquals(first, p.utility, 1e-9);
    }

    /**
     * Without damping the price still stops moving — at the wrong value, with stock uncleared.
     *
     * <p>Targeting a level says nothing about arriving there with zero momentum, which is the
     * whole argument for the derivative terms.
     */
    @Test
    public void undampedTargetingMissesTheClearingPrice() {
        World w = settle(1, 1, 0.5, 0, false, 3000);
        assertTrue("undamped price should not be 0.5, was " + w.firm(0).goodCost,
                Math.abs(w.firm(0).goodCost - 0.5) > 0.05);
        assertTrue("and stock should not have cleared", Math.abs(w.firm(0).stock) > 0.1);
    }

    /**
     * With distance, prices spread — and more input means a lower price.
     *
     * <p>Economies of scale reaching consumers is the entire point of the chapter: a Firm with
     * more time invested in it produces more per unit, so it clears its stock at a lower price.
     */
    @Test
    public void moreInputMeansALowerClearingPrice() {
        World w = settle(50, 50, 0.5, 0.25, true, 2000);
        int n = w.firms().size();
        double[] time = new double[n], price = new double[n];
        for (int i = 0; i < n; i++) {
            time[i] = w.firm(i).contributedTime;
            price[i] = w.firm(i).goodCost;
        }
        assertTrue("prices should differ across locations",
                java.util.Arrays.stream(price).max().getAsDouble()
                        - java.util.Arrays.stream(price).min().getAsDouble() > 0.01);
        assertTrue("more input should mean a lower price, corr was " + correlation(time, price),
                correlation(time, price) < -0.8);
    }

    /**
     * Starting prices matter, which is easy to miss.
     *
     * <p>From a uniform 3.0 with distance costs, Firms overshoot into the runaway of §6.3.2
     * and never recover. The thesis' randomised 0.1–1.5 settles cleanly. This is why
     * {@code randomiseInitialPrices} exists.
     */
    @Test
    public void aBadStartingPriceCausesRunaway() {
        World bad = new World(ModelConfig.builder()
                .space(Space.Kind.PLANE).width(1.0)
                .people(50).firms(50)
                .placePeople(Placement.random()).placeFirms(Placement.random())
                .utility(new Ces(0.5)).production(new SmoothEoS(1, 1))
                .baseGoodCost(3.0).deliveryCost(0.25)
                .personBehaviour(new PersonTransmission())
                .firmBehaviour(new FirmStockTarget())
                .peopleMobile(false).seed(12).build());
        for (int i = 0; i < 2000; i++) bad.timeline().nextDay();
        long runaway = bad.firms().stream().filter(f -> f.goodCost > 10).count();
        assertTrue("a uniform start of 3.0 should produce runaways, got " + runaway, runaway > 0);

        World good = settle(50, 50, 0.5, 0.25, true, 2000);
        assertTrue("randomised starting prices should not",
                good.firms().stream().noneMatch(f -> f.goodCost > 10));
    }

    private static double correlation(double[] a, double[] b) {
        double ma = java.util.Arrays.stream(a).average().orElse(0);
        double mb = java.util.Arrays.stream(b).average().orElse(0);
        double num = 0, da = 0, db = 0;
        for (int i = 0; i < a.length; i++) {
            num += (a[i] - ma) * (b[i] - mb);
            da += (a[i] - ma) * (a[i] - ma);
            db += (b[i] - mb) * (b[i] - mb);
        }
        return num / Math.sqrt(da * db);
    }
}
