package spatialecon.core.scenario.models;

import java.util.List;

import spatialecon.core.behaviour.FirmLocationCrawl;
import spatialecon.core.behaviour.PersonBundle;
import spatialecon.core.config.ModelConfig;
import spatialecon.core.config.Parameter;
import spatialecon.core.econ.Ces;
import spatialecon.core.econ.SmoothEoS;
import spatialecon.core.engine.Placement;
import spatialecon.core.engine.Space;
import spatialecon.core.engine.World;
import spatialecon.core.record.Recorder;
import spatialecon.core.scenario.Scenario;

/**
 * <b>D2 — the "data, meaning, response" game.</b> Thesis §5.5.2.
 *
 * <p>People occupy only the middle half of the line, from 0.25 to 0.75. Two Firms start at
 * opposite edges. A game ends when <em>both</em> have got inside the occupied band, or when
 * patience runs out. The question: how long does it take, and what makes it harder?
 *
 * <p>A single Firm has no difficulty at all. The world is static, so any change in its
 * revenue really is its own doing, and it walks straight to the middle. Add one competitor
 * and the datum stops meaning what the Firm assumes it means: revenue now moves for reasons
 * the Firm did not cause and cannot observe. The response follows the misreading.
 *
 * <p>Two findings, and the second is the interesting one:
 *
 * <ul>
 *   <li><b>With no noise, ρ makes no difference.</b> Clean demand signals, no confusion.
 *   <li><b>With noise, love of variety becomes the problem.</b> High ρ stays easy; as ρ → 0
 *       and variety matters more, each Firm's move has a bigger external effect on the
 *       other's demand, the signal degrades, and games take dramatically longer.
 * </ul>
 *
 * <p>So the difficulty is not the noise as such. It is noise <em>plus</em> the elasticity
 * that couples the two Firms' revenues together. This is the model that explains why Firms
 * in D1 are such weak optimisers, and it is the warning the transmission belt inherits — a
 * model full of interacting price signals is a model where Firms cannot easily read their
 * own effect.
 *
 * <p>Each recorded point plays a batch of complete games. Scaled down from the thesis' 100
 * randomisations and 10,000-day cap to keep a run to about a minute; the shape is the same.
 */
public final class D2_DmrGame implements Scenario {

    private static final int GAMES_PER_POINT = 24;
    private static final int PATIENCE = 4_000;
    private static final int PEOPLE = 50;
    private static final double BAND_FROM = 0.25;
    private static final double BAND_TO = 0.75;

    private final double noise;
    private final String id;

    private double rho = 0.05;
    private Result result = new Result(0, 0);

    /** Mean days to succeed, and the share of games that succeeded at all. */
    private record Result(double meanDays, double successRate) { }

    private D2_DmrGame(String id, double noise) {
        this.id = id;
        this.noise = noise;
    }

    public static Scenario clean()      { return new D2_DmrGame("d2", 0); }
    public static Scenario someNoise()  { return new D2_DmrGame("d2n", 0.1); }
    public static Scenario muchNoise()  { return new D2_DmrGame("d2nn", 0.5); }

    @Override public String id()        { return id; }
    @Override public String thesisRef() { return "§5.5.2"; }
    @Override public int defaultDays()  { return 20; }

    @Override
    public String title() {
        return "DMR game: two Firms find their market (noise=%.1f)".formatted(noise);
    }

    @Override
    public String notes() {
        return """
               50 People occupy only 0.25-0.75 of the line. Two Firms start at the edges.
               A game ends when both are inside the band, or after %d days. Distance cost 20.

               Demand noise = %.1f. Each of the %d recorded points sweeps rho and plays %d
               complete games, reporting the mean days to succeed.

               VERIFIED: competition and noise both make this harder. A lone Firm walks
               straight in; a second Firm slows it; noise slows it further (15 -> 17 -> 32
               mean days at noise 0 / 0.1 / 0.5).

               NOT REPRODUCED: the thesis reports that under noise, LOW rho (high love of
               variety) is markedly harder. Here rho makes little difference, and if anything
               low rho is easier. See the README -- treat the rho axis of this model as an
               open question, not a result.

               successRate below 1 means some games hit the patience limit -- those are
               counted at the cap, so the mean understates the difficulty.""".formatted(
                       PATIENCE, noise, defaultDays(), GAMES_PER_POINT);
    }

    /** The outer scenario is a shell; the real work happens in the sub-games. */
    @Override
    public ModelConfig config() {
        return gameConfig(rho, 12);
    }

    private ModelConfig gameConfig(double rho, long seed) {
        return ModelConfig.builder()
                .space(Space.Kind.LINE).width(1.0)
                .people(PEOPLE).firms(2)
                .placePeople(Placement.evenlyAlongX(BAND_FROM, BAND_TO))
                .placeFirms(Placement.evenlyAlongX(0, 1))     // the two edges
                .utility(new Ces(rho))
                .production(new SmoothEoS())
                .baseGoodCost(1.0)
                .wage(1.0)
                .commuteCost(0)
                .deliveryCost(20.0)          // as shipped in Hotelling_Line_DMR.properties
                .densityCost(0)
                .demandNoise(noise)
                .personBehaviour(new PersonBundle())
                .firmBehaviour(new FirmLocationCrawl())
                .peopleMobile(false)
                .firmsMobile(true)
                .seed(seed)
                .build();
    }

    /** Step rho across its range, then play a batch of games at that value. */
    @Override
    public void beforeDay(World world, long day) {
        double t = day / (double) Math.max(defaultDays() - 1, 1);
        rho = 0.05 + t * 0.94;
        result = playBatch(rho);
    }

    private Result playBatch(double rho) {
        double totalDays = 0;
        int succeeded = 0;
        for (int game = 0; game < GAMES_PER_POINT; game++) {
            int days = playGame(rho, 1000L + game * 37L);
            totalDays += days;
            if (days < PATIENCE) succeeded++;
        }
        return new Result(totalDays / GAMES_PER_POINT, succeeded / (double) GAMES_PER_POINT);
    }

    /** One game. Returns days taken, or {@link #PATIENCE} if both never got inside. */
    private int playGame(double rho, long seed) {
        World w = new World(gameConfig(rho, seed));
        for (int day = 1; day <= PATIENCE; day++) {
            w.timeline().nextDay();
            if (bothInsideBand(w)) return day;
        }
        return PATIENCE;
    }

    private boolean bothInsideBand(World w) {
        for (var firm : w.firms()) {
            double x = firm.position().x();
            if (x < BAND_FROM || x > BAND_TO) return false;
        }
        return true;
    }

    @Override
    public void record(Recorder r, World w) {
        r.global("rho", () -> rho);
        r.global("daysToSucceed", () -> result.meanDays());
        r.global("successRate", () -> result.successRate());
        r.global("noise", () -> noise);
    }

    @Override
    public List<Parameter> controls(World world) {
        return List.of(Parameter.of("noise", () -> noise, v -> { }).range(0, 1)
                .label("demand noise (fixed per variant)"));
    }
}
