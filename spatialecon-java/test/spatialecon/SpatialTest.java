package spatialecon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import spatialecon.core.behaviour.PersonBundle;
import spatialecon.core.config.ModelConfig;
import spatialecon.core.config.Quirks;
import spatialecon.core.econ.Ces;
import spatialecon.core.econ.SmoothEoS;
import spatialecon.core.engine.Person;
import spatialecon.core.engine.Placement;
import spatialecon.core.engine.Pt;
import spatialecon.core.engine.Space;
import spatialecon.core.engine.World;

/** Space, density cost, and the wiring that carries costs to Firms. */
public class SpatialTest {

    private static final double EPS = 1e-9;

    private static ModelConfig.Builder base() {
        return ModelConfig.builder()
                .space(Space.Kind.PLANE).width(1.0)
                .people(2).firms(1)
                .placePeople(Placement.at(0.5, 0.5))
                .placeFirms(Placement.centre())
                .utility(new Ces(0.5))
                .production(new SmoothEoS())
                .personBehaviour(new PersonBundle());
    }

    // --- Density cost -------------------------------------------------------------

    /** A neighbour on the same spot contributes weight 1; one at the radius contributes 0. */
    @Test
    public void densityWeightTapersLinearlyToZeroAtTheRadius() {
        World w = new World(base().people(2).densityCost(1).densityRadius(0.1).build());
        w.person(0).placeAt(new Pt(0.5, 0.5));
        w.person(1).placeAt(new Pt(0.5, 0.5));           // co-located
        double divisor = w.config().densityDivisor();
        assertEquals(1.0 / divisor, w.rawDensityAt(new Pt(0.5, 0.5), w.person(0)), EPS);

        w.person(1).placeAt(new Pt(0.55, 0.5));          // halfway to the radius
        assertEquals(0.5 / divisor, w.rawDensityAt(new Pt(0.5, 0.5), w.person(0)), EPS);

        w.person(1).placeAt(new Pt(0.61, 0.5));          // beyond it
        assertEquals(0.0, w.rawDensityAt(new Pt(0.5, 0.5), w.person(0)), EPS);
    }

    /** The divisor quirk: Firms in or out changes the answer, in the direction expected. */
    @Test
    public void densityDivisorFollowsTheQuirk() {
        ModelConfig original = base().people(9).firms(1)
                .densityCost(1).densityRadius(0.5).quirks(Quirks.ORIGINAL).build();
        ModelConfig asStated = base().people(9).firms(1)
                .densityCost(1).densityRadius(0.5).quirks(Quirks.AS_THESIS_STATES).build();

        assertEquals("original divides by People + Firms", 10, original.densityDivisor());
        assertEquals("thesis text divides by People alone", 9, asStated.densityDivisor());

        World a = new World(original);
        World b = new World(asStated);
        for (int i = 0; i < 9; i++) {
            a.person(i).placeAt(new Pt(0.5, 0.5));
            b.person(i).placeAt(new Pt(0.5, 0.5));
        }
        double da = a.rawDensityAt(new Pt(0.5, 0.5), a.person(0));
        double db = b.rawDensityAt(new Pt(0.5, 0.5), b.person(0));
        assertTrue("including Firms must give a smaller density", da < db);
        assertEquals("and the ratio is exactly the divisor ratio", 9.0 / 10.0, da / db, EPS);
    }

    /** Density is a genuine radius query: distant People contribute nothing. */
    @Test
    public void densityIsLocal() {
        World w = new World(base().people(3).densityCost(1).densityRadius(0.05).build());
        w.person(0).placeAt(new Pt(0.5, 0.5));
        w.person(1).placeAt(new Pt(0.9, 0.9));
        w.person(2).placeAt(new Pt(0.1, 0.1));
        assertEquals(0.0, w.rawDensityAt(new Pt(0.5, 0.5), w.person(0)), EPS);
        assertEquals(0, w.countNeighbours(new Pt(0.5, 0.5), w.person(0), 0.05));
    }

    /** Zero multiplier switches the whole mechanism off. */
    @Test
    public void zeroDensityCostCostsNothing() {
        World w = new World(base().people(5).densityCost(0).densityRadius(0.5).build());
        for (int i = 0; i < 5; i++) w.person(i).placeAt(new Pt(0.5, 0.5));
        assertEquals(0.0, w.densityCostAt(new Pt(0.5, 0.5), w.person(0)), EPS);
    }

    // --- Cost propagation ---------------------------------------------------------

    /**
     * A live cost change must reach the Firms.
     *
     * <p>Regression test. Firms hold their own copy of each cost (they must, since ranging
     * makes them differ), so setting it on the config alone left prices unchanged — which
     * silently disabled every distance-cost control and flattened model b2 entirely.
     */
    @Test
    public void changingDeliveryCostReachesTheFirms() {
        World w = new World(base().firms(2).deliveryCost(1.0).build());
        assertEquals(1.0, w.firm(0).deliveryCost, EPS);

        w.setDeliveryCost(7.0);
        for (int i = 0; i < w.firms().size(); i++) {
            assertEquals("Firm " + i + " should see the new cost", 7.0, w.firm(i).deliveryCost, EPS);
        }
        assertEquals(1.0 + 7.0 * 0.25, w.firm(0).priceAtDistance(0.25), EPS);
    }

    /** Ranging multiplies by Firm index, and survives a live change. */
    @Test
    public void rangingMultipliesByFirmIndex() {
        World w = new World(base().firms(4).deliveryCost(2.0).rangeDelivery(true).build());
        assertEquals(2.0, w.firm(0).deliveryCost, EPS);
        assertEquals(4.0, w.firm(1).deliveryCost, EPS);
        assertEquals(8.0, w.firm(3).deliveryCost, EPS);

        w.setDeliveryCost(1.0);
        assertEquals(1.0, w.firm(0).deliveryCost, EPS);
        assertEquals(4.0, w.firm(3).deliveryCost, EPS);
    }

    /** The two-Firm case of §5.4.6: a base of 2 across two Firms gives costs of 2 and 4. */
    @Test
    public void rangingReproducesTheTwoFirmSetup() {
        World w = new World(base().firms(2).deliveryCost(2.0).rangeDelivery(true).build());
        assertEquals(2.0, w.firm(0).deliveryCost, EPS);
        assertEquals(4.0, w.firm(1).deliveryCost, EPS);
    }

    // --- Space --------------------------------------------------------------------

    @Test
    public void pointSpaceHasNoDistance() {
        Space point = new Space(Space.Kind.POINT, 1);
        assertEquals(0.0, point.distance(new Pt(0, 0), new Pt(5, 5)), EPS);
    }

    @Test
    public void twoRegionsSitEitherSideOfTheOriginAtWidthApart() {
        Space regions = new Space(Space.Kind.TWO_REGION, 3);
        assertEquals(-1.5, regions.regionZeroX(), EPS);
        assertEquals(1.5, regions.regionOneX(), EPS);
        assertEquals("width is the separation", 3.0,
                regions.distance(new Pt(regions.regionZeroX(), 0), new Pt(regions.regionOneX(), 0)), EPS);
    }

    @Test
    public void confineClampsRatherThanWrapping() {
        Space plane = new Space(Space.Kind.PLANE, 1);
        assertEquals(new Pt(1, 1), plane.confine(new Pt(5, 5)));
        assertEquals(new Pt(0, 0), plane.confine(new Pt(-5, -5)));

        Space line = new Space(Space.Kind.LINE, 1);
        assertEquals("a line has no y", new Pt(0.5, 0), line.confine(new Pt(0.5, 9)));
    }

    // --- Commuting ----------------------------------------------------------------

    /**
     * The commute quirk changes reachability, not just cost.
     *
     * <p>Under the round-trip reading a Firm more than half a day away leaves no time to
     * work at all, which is what §4.6.1 describes. The original charged one way, so the
     * limit falls at a full day.
     */
    @Test
    public void commuteQuirkChangesHowFarWorkReaches() {
        double distance = 0.6;   // beyond half a day, inside a full day, at commuteCost 1

        assertTrue("one-way: still reachable", worksAt(distance, Quirks.ORIGINAL));
        assertTrue("round trip: out of reach", !worksAt(distance, Quirks.AS_THESIS_STATES));
    }

    private boolean worksAt(double distance, Quirks quirks) {
        World w = new World(base()
                .people(1).firms(1)
                .placePeople(Placement.at(0, 0))
                .placeFirms(Placement.at(distance, 0))
                .space(Space.Kind.LINE).width(1.0)
                .wage(1.0).commuteCost(1.0).baseGoodCost(1.0)
                .quirks(quirks)
                .peopleMobile(false)
                .build());
        Person p = w.person(0);
        w.config().personBehaviour().act(p, w);
        return p.utility > 0;
    }

    /** Zero commute cost makes distance to work free, so any Firm is reachable. */
    @Test
    public void zeroCommuteCostMakesEveryFirmReachable() {
        assertTrue("a Firm 0.99 away should still be workable", commuteFreeUtility() > 0);
    }

    private double commuteFreeUtility() {
        World w = new World(base()
                .people(1).firms(1)
                .placePeople(Placement.at(0, 0))
                .placeFirms(Placement.at(0.99, 0))
                .space(Space.Kind.LINE).width(1.0)
                .wage(1.0).commuteCost(0).baseGoodCost(1.0).deliveryCost(0)
                .peopleMobile(false)
                .build());
        w.config().personBehaviour().act(w.person(0), w);
        return w.person(0).utility;
    }
}
