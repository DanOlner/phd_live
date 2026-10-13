package spatialecon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import spatialecon.core.config.Quirks;
import spatialecon.core.econ.Ces;
import spatialecon.core.econ.CoreModelEoS;
import spatialecon.core.econ.Optimiser;
import spatialecon.core.econ.SmoothEoS;
import spatialecon.core.econ.StoneGeary;
import spatialecon.core.engine.Rng;

/**
 * The economics, checked against values that can be worked out by hand.
 *
 * <p>These are the load-bearing assertions of the refactor. Everything else — density costs,
 * stock targeting, regime transitions — is built on these functions, so if one of them is
 * subtly wrong the error surfaces much later disguised as an interesting result.
 */
public class EconTest {

    private static final double EPS = 1e-9;
    private static final double LOOSE = 1e-3;

    // --- CES ----------------------------------------------------------------------

    /** Equal prices give an even split, for any rho. The thesis relies on this throughout. */
    @Test
    public void cesSplitsEvenlyAtEqualPrices() {
        for (double rho : new double[]{ 0.1, 0.25, 0.5, 0.75, 0.9 }) {
            double[] q = new Ces(rho).optimalQuantities(new double[]{ 1, 1, 1, 1 }, 1.0);
            for (double v : q) {
                assertEquals("even split at rho=" + rho, 0.25, v, EPS);
            }
        }
    }

    /** The optimum spends the budget exactly — no more, no less. */
    @Test
    public void cesExhaustsTheBudget() {
        double[] prices = { 0.7, 1.3, 2.9, 0.4 };
        double budget = 2.5;
        double[] q = new Ces(0.4).optimalQuantities(prices, budget);
        double spend = 0;
        for (int i = 0; i < q.length; i++) spend += q[i] * prices[i];
        assertEquals(budget, spend, 1e-9);
    }

    /** Closed form for n equally priced goods: U = Y · n^(1/rho − 1). */
    @Test
    public void cesMatchesClosedFormForEqualPrices() {
        for (int n : new int[]{ 2, 3, 8, 16 }) {
            for (double rho : new double[]{ 0.3, 0.5, 0.8 }) {
                Ces ces = new Ces(rho);
                double[] prices = new double[n];
                Arrays.fill(prices, 1.0);
                double actual = ces.utilityOf(ces.optimalQuantities(prices, 1.0));
                double expected = Math.pow(n, 1 / rho - 1);
                assertEquals("n=" + n + " rho=" + rho, expected, actual, 1e-9);
            }
        }
    }

    /** Lower rho means more love of variety, so the same budget yields more utility. */
    @Test
    public void cesUtilityRisesAsRhoFalls() {
        double[] prices = { 1, 1, 1 };
        double previous = Double.NEGATIVE_INFINITY;
        for (double rho : new double[]{ 0.9, 0.7, 0.5, 0.3, 0.1 }) {
            Ces ces = new Ces(rho);
            double u = ces.utilityOf(ces.optimalQuantities(prices, 1.0));
            assertTrue("utility should rise as rho falls (rho=" + rho + ")", u > previous);
            previous = u;
        }
    }

    /** More varieties are worth more at fixed budget — but at a decreasing rate (§5.2.1). */
    @Test
    public void cesUtilityRisesWithVarietyAtDecreasingRate() {
        Ces ces = new Ces(0.75);
        double previousUtility = 0, previousGain = Double.MAX_VALUE;
        for (int n = 1; n <= 12; n++) {
            double[] prices = new double[n];
            Arrays.fill(prices, 1.0);
            double u = ces.utilityOf(ces.optimalQuantities(prices, 1.0));
            if (n > 1) {
                double gain = u - previousUtility;
                assertTrue("more variety is worth more (n=" + n + ")", gain > 0);
                assertTrue("gain should shrink (n=" + n + ")", gain < previousGain);
                previousGain = gain;
            }
            previousUtility = u;
        }
    }

    /** Cheaper goods get a larger share. */
    @Test
    public void cesFavoursCheaperGoods() {
        double[] q = new Ces(0.5).optimalQuantities(new double[]{ 1, 2, 4 }, 1.0);
        assertTrue(q[0] > q[1]);
        assertTrue(q[1] > q[2]);
    }

    /**
     * The rho >= 1 branch, both ways.
     *
     * <p>ORIGINAL multiplies price by budget; the rho→1⁻ limit divides. They agree only when
     * the price is 1, which is why this went unnoticed. See CODE_ANATOMY §7.1 — unresolved,
     * so the test pins current behaviour rather than asserting which is right.
     */
    @Test
    public void perfectSubstitutesGiveEverythingToTheCheapest() {
        double[] prices = { 2.0, 0.5 };

        double[] original = new Ces(1.0, Quirks.ORIGINAL).optimalQuantities(prices, 1.0);
        assertEquals("nothing for the dearer good", 0.0, original[0], EPS);
        assertEquals("original formula: price * budget", 0.5, original[1], EPS);

        double[] limit = new Ces(1.0, Quirks.AS_THESIS_STATES).optimalQuantities(prices, 1.0);
        assertEquals(0.0, limit[0], EPS);
        assertEquals("limit formula: budget / price", 2.0, limit[1], EPS);
    }

    /** The limit form is what rho→1⁻ actually converges to, which is the case for it. */
    @Test
    public void limitFormulaMatchesTheApproachFromBelow() {
        double[] prices = { 2.0, 0.5 };
        double approached = new Ces(0.999).optimalQuantities(prices, 1.0)[1];
        double limit = new Ces(1.0, Quirks.AS_THESIS_STATES).optimalQuantities(prices, 1.0)[1];
        assertEquals("rho=0.999 should be near the limit", limit, approached, 0.05);
    }

    // --- Cobb-Douglas / Stone-Geary -----------------------------------------------

    /** The result of §5.2.2: one good's price does not affect the other's demand. */
    @Test
    public void cobbDouglasDemandsAreIndependent() {
        StoneGeary cd = StoneGeary.cobbDouglas(0.5);
        double firstGood = cd.optimalQuantities(new double[]{ 1.0, 1.0 }, 1.0)[0];
        for (double otherPrice : new double[]{ 0.2, 0.5, 1.0, 2.0, 5.0, 20.0 }) {
            double[] q = cd.optimalQuantities(new double[]{ 1.0, otherPrice }, 1.0);
            assertEquals("demand for good 0 must not move", firstGood, q[0], EPS);
        }
    }

    /** Unit elasticity: revenue is invariant to price. So a Firm has no lever. */
    @Test
    public void cobbDouglasIsUnitElastic() {
        StoneGeary cd = StoneGeary.cobbDouglas(0.4);
        double expected = 0.6;   // (1 - delta) * budget
        for (double price : new double[]{ 0.25, 0.5, 1.0, 2.0, 8.0 }) {
            double[] q = cd.optimalQuantities(new double[]{ 1.0, price }, 1.0);
            assertEquals("revenue invariant at price " + price, expected, q[1] * price, EPS);
        }
    }

    /** Delta is literally the share of income going to the first good. */
    @Test
    public void cobbDouglasShareParameterIsTheIncomeShare() {
        for (double delta : new double[]{ 0.2, 0.5, 0.8 }) {
            double[] q = StoneGeary.cobbDouglas(delta)
                    .optimalQuantities(new double[]{ 2.0, 3.0 }, 10.0);
            assertEquals(delta * 10.0, q[0] * 2.0, EPS);
            assertEquals((1 - delta) * 10.0, q[1] * 3.0, EPS);
        }
    }

    /** With gamma = 0 the Stone-Geary form is exactly Cobb-Douglas. */
    @Test
    public void stoneGearyReducesToCobbDouglasAtZeroGamma() {
        double[] prices = { 1.5, 0.8 };
        double[] a = StoneGeary.cobbDouglas(0.6).optimalQuantities(prices, 3.0);
        double[] b = new StoneGeary(0.6, 0).optimalQuantities(prices, 3.0);
        assertEquals(a[0], b[0], EPS);
        assertEquals(a[1], b[1], EPS);
    }

    /** With gamma > 0 preferences are no longer homothetic: the mix shifts with income. */
    @Test
    public void stoneGearyIsNotHomothetic() {
        StoneGeary sg = new StoneGeary(0.5, 1.0);
        double[] prices = { 1.0, 1.0 };
        double lowIncomeShare = shareOfFirstGood(sg.optimalQuantities(prices, 1.0));
        double highIncomeShare = shareOfFirstGood(sg.optimalQuantities(prices, 100.0));
        assertTrue("the mix should shift with income", Math.abs(lowIncomeShare - highIncomeShare) > 0.01);
    }

    private static double shareOfFirstGood(double[] q) {
        double total = q[0] + q[1];
        return total == 0 ? 0 : q[0] / total;
    }

    // --- Production ---------------------------------------------------------------

    /** The normalised form the thesis deduces with in §6.3.1: t^2 + t, so 0.5 gives 0.75. */
    @Test
    public void smoothEoSMatchesTheDeducedValue() {
        assertEquals(0.75, new SmoothEoS(1, 1).outputFor(0.5), EPS);
        assertEquals(2.0, new SmoothEoS(1, 1).outputFor(1.0), EPS);
    }

    /** Zero curve means constant returns — the control case for the EoS results. */
    @Test
    public void zeroCurveGivesConstantReturns() {
        SmoothEoS linear = new SmoothEoS(0, 2);
        assertEquals(2.0, linear.outputPerUnit(1.0), EPS);
        assertEquals(2.0, linear.outputPerUnit(50.0), EPS);
        assertEquals(2.0, linear.marginalAt(10.0), 1e-6);
    }

    /** With a positive curve, output per unit rises with input. That is the whole mechanism. */
    @Test
    public void smoothEoSHasRisingOutputPerUnit() {
        SmoothEoS eos = new SmoothEoS(1, 1);
        double previous = 0;
        for (double l = 0.5; l <= 5; l += 0.5) {
            double perUnit = eos.outputPerUnit(l);
            assertTrue("output per unit should rise at l=" + l, perUnit > previous);
            previous = perUnit;
        }
    }

    /** Granular allocation: the shares of all contributors sum to total output. */
    @Test
    public void granularSharesSumToTotalOutput() {
        SmoothEoS eos = new SmoothEoS(1, 1);
        double total = 3.0;
        double[] contributions = { 0.5, 1.0, 1.5 };
        double summed = 0;
        for (double c : contributions) summed += eos.granularShare(total, c);
        assertEquals(eos.outputFor(total), summed, 1e-9);
    }

    /** The Core Model form goes negative below alpha. The reason the thesis abandoned it. */
    @Test
    public void coreModelProductionIsNegativeBelowFixedLabour() {
        CoreModelEoS cm = new CoreModelEoS(3, 1);
        assertTrue("one unit of labour cannot produce", cm.outputFor(1.0) < 0);
        assertTrue("two cannot either", cm.outputFor(2.0) < 0);
        assertEquals("exactly at alpha, output is zero", 0.0, cm.outputFor(3.0), EPS);
        assertTrue("above alpha it finally produces", cm.outputFor(4.0) > 0);

        assertTrue(!cm.canProduceAt(2.99));
        assertTrue(cm.canProduceAt(3.0));
    }

    /** Its marginal output is constant by construction — unlike the smooth form's. */
    @Test
    public void coreModelMarginalIsConstant() {
        CoreModelEoS cm = new CoreModelEoS(3, 2);
        assertEquals(0.5, cm.marginalAt(4.0), 1e-6);
        assertEquals(0.5, cm.marginalAt(40.0), 1e-6);
    }

    // --- Optimisers ---------------------------------------------------------------

    /** The two methods agree. This is the claim of §5.2.3. */
    @Test
    public void hillClimberAgreesWithTheAnalyticOptimum() {
        Ces ces = new Ces(0.5);
        Rng rng = new Rng(42);
        double[] prices = { 1, 1, 1, 1 };

        double[] exact = Optimiser.constrained().optimise(prices, 1.0, ces, rng);
        double[] searched = Optimiser.hillClimb(4000).optimise(prices, 1.0, ces, rng);

        for (int i = 0; i < prices.length; i++) {
            assertEquals("good " + i, exact[i], searched[i], LOOSE);
        }
    }

    /** More iterations get closer. Noisy, so compare a batch rather than single runs. */
    @Test
    public void moreIterationsMeansLessError() {
        Ces ces = new Ces(0.5);
        double[] prices = { 1, 1 };
        double coarse = meanAbsoluteError(Optimiser.hillClimb(5), ces, prices, 0.5, 200);
        double fine = meanAbsoluteError(Optimiser.hillClimb(2000), ces, prices, 0.5, 200);
        assertTrue("2000 iterations (" + fine + ") should beat 5 (" + coarse + ")", fine < coarse);
    }

    /**
     * The quirk of §5.2.3: more goods converge faster, because the slices vary less.
     *
     * <p>True in <em>absolute</em> error, and only early on. Measured here: at 5–200
     * iterations the 8-good case sits at roughly a third the absolute error of the 2-good
     * case, which is the "less scope for large variation" the thesis describes.
     *
     * <p>It is not true in relative terms at low iteration counts (8 goods is slightly
     * worse), and it reverses entirely once both have converged — see
     * {@link #theMoreGoodsAdvantageReversesOnceConverged}. The thesis illustrates the early
     * part of the curve, so the claim stands as made; this pins down its scope.
     */
    @Test
    public void moreGoodsConvergeFasterEarlyOn() {
        Ces ces = new Ces(0.5);
        for (int iterations : new int[]{ 5, 20, 60 }) {
            double twoGoods = absoluteError(ces, 2, iterations);
            double eightGoods = absoluteError(ces, 8, iterations);
            assertTrue("at " + iterations + " iterations, 8 goods (" + eightGoods
                            + ") should beat 2 (" + twoGoods + ")",
                    eightGoods < twoGoods);
        }
    }

    /**
     * The other end of the same curve: given enough iterations, fewer goods wins.
     *
     * <p>At 1000 iterations the 2-good case is essentially exact (order 1e-6) while the
     * 8-good case is two orders worse. Worth knowing before reading anything into the
     * early-iteration advantage — and worth showing on the website, since a plot that stops
     * at 200 iterations tells the opposite story to one that runs to 2000.
     */
    @Test
    public void theMoreGoodsAdvantageReversesOnceConverged() {
        Ces ces = new Ces(0.5);
        double twoGoods = absoluteError(ces, 2, 1000);
        double eightGoods = absoluteError(ces, 8, 1000);
        assertTrue("2 goods should be near-exact by 1000 iterations, was " + twoGoods,
                twoGoods < 1e-4);
        assertTrue("and should now beat 8 goods (" + eightGoods + ")", twoGoods < eightGoods);
    }

    /** The hill-climber never overspends — it allocates shares of a fixed budget. */
    @Test
    public void hillClimberStaysWithinBudget() {
        Ces ces = new Ces(0.5);
        Rng rng = new Rng(7);
        double[] prices = { 0.5, 1.5, 3.0 };
        double budget = 2.0;
        double[] q = Optimiser.hillClimb(500).optimise(prices, budget, ces, rng);
        double spend = 0;
        for (int i = 0; i < q.length; i++) spend += q[i] * prices[i];
        assertEquals(budget, spend, 1e-9);
    }

    /** Best-so-far never decreases: the ratchet only keeps improvements. */
    @Test
    public void hillClimberTraceIsMonotonic() {
        Ces ces = new Ces(0.5);
        Optimiser.HillClimb climber = (Optimiser.HillClimb) Optimiser.hillClimb(500);
        climber.optimise(new double[]{ 1, 1, 1 }, 1.0, ces, new Rng(3));
        double[] trace = climber.lastTrace();
        for (int i = 1; i < trace.length; i++) {
            assertTrue("best-so-far must not fall at step " + i, trace[i] >= trace[i - 1] - EPS);
        }
    }

    // --- helpers ------------------------------------------------------------------

    private static double meanAbsoluteError(Optimiser optimiser, Ces ces, double[] prices,
                                            double target, int batch) {
        Rng rng = new Rng(99);
        double sum = 0;
        for (int i = 0; i < batch; i++) {
            sum += Math.abs(optimiser.optimise(prices, 1.0, ces, rng)[0] - target);
        }
        return sum / batch;
    }

    /** Mean absolute deviation of the first good's quantity from its analytic optimum. */
    private static double absoluteError(Ces ces, int goods, int iterations) {
        double[] prices = new double[goods];
        Arrays.fill(prices, 1.0);
        return meanAbsoluteError(Optimiser.hillClimb(iterations), ces, prices, 1.0 / goods, 300);
    }
}
