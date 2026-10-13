package spatialecon;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import spatialecon.core.behaviour.FirmLocationCrawl;
import spatialecon.core.behaviour.PersonBundle;
import spatialecon.core.config.ModelConfig;
import spatialecon.core.econ.Ces;
import spatialecon.core.econ.SmoothEoS;
import spatialecon.core.engine.Placement;
import spatialecon.core.engine.Space;
import spatialecon.core.engine.World;

/**
 * The "data, meaning, response" mechanism (§5.5).
 *
 * <p>The thesis' control case is the important one: a Firm on its own has no difficulty
 * finding its market, because in a static world any change in its revenue really is its own
 * doing. Difficulty appears only when a second Firm makes that inference unreliable.
 */
public class DmrTest {

    private static final double BAND_FROM = 0.25;
    private static final double BAND_TO = 0.75;
    private static final int PATIENCE = 4000;

    private static ModelConfig game(int firms, double rho, double noise, long seed) {
        return ModelConfig.builder()
                .space(Space.Kind.LINE).width(1.0)
                .people(50).firms(firms)
                .placePeople(Placement.evenlyAlongX(BAND_FROM, BAND_TO))
                .placeFirms(firms == 1 ? Placement.at(0, 0) : Placement.evenlyAlongX(0, 1))
                .utility(new Ces(rho)).production(new SmoothEoS())
                .baseGoodCost(1.0).wage(1.0).commuteCost(0).deliveryCost(20.0).densityCost(0)
                .demandNoise(noise)
                .personBehaviour(new PersonBundle())
                .firmBehaviour(new FirmLocationCrawl())
                .peopleMobile(false).firmsMobile(true)
                .seed(seed)
                .build();
    }

    private static int daysToReachMarket(int firms, double rho, double noise, long seed) {
        World w = new World(game(firms, rho, noise, seed));
        for (int day = 1; day <= PATIENCE; day++) {
            w.timeline().nextDay();
            boolean allIn = w.firms().stream().allMatch(f ->
                    f.position().x() >= BAND_FROM && f.position().x() <= BAND_TO);
            if (allIn) return day;
        }
        return PATIENCE;
    }

    private static double meanDays(int firms, double rho, double noise, int games) {
        double total = 0;
        for (int i = 0; i < games; i++) total += daysToReachMarket(firms, rho, noise, 1000L + i * 37L);
        return total / games;
    }

    /**
     * The control case: alone, a Firm walks straight in. Nothing else in the world moves, so
     * "my revenue rose" really does mean "my last move was good".
     */
    @Test
    public void aLoneFirmFindsItsMarketEasily() {
        double days = meanDays(1, 0.5, 0, 8);
        assertTrue("a single Firm should get in quickly, took " + days, days < 100);
    }

    /** Adding a competitor makes it harder, because revenue now moves for other reasons. */
    @Test
    public void asecondFirmMakesItHarder() {
        double alone = meanDays(1, 0.5, 0, 8);
        double competing = meanDays(2, 0.5, 0, 8);
        assertTrue("two Firms (" + competing + ") should be slower than one (" + alone + ")",
                competing > alone);
    }

    /** Noise degrades the signal further, on top of competition. */
    @Test
    public void noiseMakesItHarderStill() {
        double clean = meanDays(2, 0.5, 0, 8);
        double noisy = meanDays(2, 0.5, 0.5, 8);
        assertTrue("noisy (" + noisy + ") should be slower than clean (" + clean + ")",
                noisy > clean);
    }

    /**
     * A Firm does eventually get there. The mechanism is weak, not broken — which is the
     * thesis' actual position: Firms optimise, just badly.
     */
    @Test
    public void firmsDoEventuallySucceed() {
        assertTrue(daysToReachMarket(2, 0.5, 0.5, 12) < PATIENCE);
    }

    /**
     * <b>Documents an unresolved discrepancy, not a verified claim.</b>
     *
     * <p>§5.5.2 reports that under noise, games get harder as ρ falls (love of variety rises),
     * because each Firm's move then has a larger external effect on the other's demand. This
     * implementation shows the opposite: low ρ is as easy or easier.
     *
     * <p>There is a coherent mechanism for what is seen here. At high ρ demand is close to
     * winner-take-all, so a Firm out at the edge captures almost nothing, its revenue is tiny,
     * and the change it is trying to read is swamped. At low ρ every Person buys some of
     * everything, so even a distant Firm has a substantial revenue base and a readable signal.
     *
     * <p>The test asserts only the direction actually observed, so that if the setup is later
     * corrected the failure is loud. See the README for the full account.
     */
    @Test
    public void rhoDependenceRunsOppositeToTheThesisReport() {
        double highVariety = meanDays(2, 0.1, 0.5, 8);
        double substitutes = meanDays(2, 0.95, 0.5, 8);
        assertTrue("observed: high LoV (" + highVariety + ") is not harder than substitutes ("
                        + substitutes + "). The thesis reports the reverse; see README.",
                highVariety <= substitutes * 1.1);
    }
}
