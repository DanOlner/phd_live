package spatialecon.core.scenario.models;

import java.util.List;

import spatialecon.core.config.ModelConfig;
import spatialecon.core.config.Parameter;
import spatialecon.core.econ.Ces;
import spatialecon.core.engine.Placement;
import spatialecon.core.engine.Space;
import spatialecon.core.engine.World;
import spatialecon.core.record.Aggregation;
import spatialecon.core.record.Recorder;
import spatialecon.core.scenario.Scenario;

/**
 * <b>E3 — economies of scale against love of variety.</b> Thesis §6.3.3–6.3.4.
 *
 * <p>Two Firms, one Person, <b>no space at all</b>. ρ is raised slowly. Below a threshold the
 * two Firms produce symmetrically; above it the symmetry breaks and one becomes the efficient
 * producer, charging a much lower price and taking most of the day's time.
 *
 * <p>The claim this establishes is the chapter's sharpest: <b>you do not need space for a
 * catastrophic shift in production structure.</b> Dixit–Stiglitz cannot show this, because it
 * assumes all varieties are produced symmetrically at the same scale. Here the scale is real
 * and free to move, so it moves.
 *
 * <p>Where the instability comes from is worth being precise about, because "emergence"
 * explains nothing. Both Firms decide simultaneously and never observe each other. The
 * coupling is entirely inside the <em>Person's</em> CES demand: every price appears in every
 * good's demand, with ρ setting how strongly. Below the threshold a price perturbation is
 * self-correcting; above it, the demand shift outruns the price correction, the favoured Firm
 * gets more input, produces more efficiently, can charge less, and attracts more still.
 *
 * <p>The driver is visible in the Person's own utility: they shift demand because doing so
 * makes them better off. Efficiency gains reach welfare — which is what "transmission belt"
 * names.
 */
public final class E3_RegimeTransition implements Scenario {

    private static final double RHO_FROM = 0.60;
    private static final double RHO_TO = 0.90;

    private final Ces ces = new Ces(RHO_FROM);

    @Override public String id()        { return "e3"; }
    @Override public String title()     { return "EoS vs love of variety: production regime shift"; }
    @Override public String thesisRef() { return "§6.3.3, §6.3.4"; }
    @Override public int defaultDays()  { return 6000; }

    @Override
    public String notes() {
        return """
               Two Firms, one Person, NO SPACE. rho is raised slowly from %.2f to %.2f.

               Watch efficiencyRatio (higher output-per-unit over lower, so always >= 1).
               It sits at 1.0 while production is symmetric, then breaks away. Watch
               price_min/price_max separate at the same moment: the winner's price falls
               towards 0.5, which is the one-Firm clearing price, while the loser's climbs.

               Utility needs reading carefully. It declines throughout as a background trend,
               because raising rho reduces love of variety and that is worth less regardless.
               What matters is the LOCAL turn at the transition: it stops falling and starts
               rising. The Person is not a victim of the shift, they cause it -- they move
               their demand because doing so makes them better off.

               Threshold observed here: rho = 0.764. The thesis puts it near 0.75.
               Symmetric equilibrium price 0.6667, deducible by hand and hit exactly.

               No distance is involved anywhere. Increasing returns plus substitutability are
               enough on their own to reorganise production, which is precisely what a model
               assuming symmetric varieties cannot show.""".formatted(RHO_FROM, RHO_TO);
    }

    @Override
    public ModelConfig config() {
        return TransmissionBelt.base(2, 1, RHO_FROM)
                .utility(ces)
                .space(Space.Kind.POINT)
                .placeFirms(Placement.centre())
                .placePeople(Placement.centre())
                // A slight initial asymmetry is required: from exactly equal prices there is
                // nothing for the feedback to amplify and the model stays symmetric forever.
                .randomiseInitialPrices(0.64, 0.68)
                .build();
    }

    @Override
    public void beforeDay(World world, long day) {
        double t = day / (double) Math.max(defaultDays() - 1, 1);
        ces.setRho(RHO_FROM + t * (RHO_TO - RHO_FROM));
    }

    @Override
    public void record(Recorder r, World w) {
        r.global("rho", ces::rho);
        r.aggregate("price", w.firms(), f -> f.goodCost, Aggregation.MIN_MAX);
        r.aggregate("efficiency", w.firms(), f -> f.outputPerUnitTime, Aggregation.MIN_MAX);
        r.aggregate("efficiencyRatio", w.firms(), f -> f.outputPerUnitTime, Aggregation.RATIO_GTE_ONE);
        r.aggregate("contributedTime", w.firms(), f -> f.contributedTime, Aggregation.MIN_MAX);
        r.aggregate("stock", w.firms(), f -> f.stock, Aggregation.MEAN_SD);
        r.aggregate("utility", w.people(), p -> p.utility, Aggregation.MEAN);
    }

    @Override
    public List<Parameter> controls(World world) {
        return List.of(ces.rhoParam());
    }
}
