package spatialecon.core.scenario.models;

import java.util.List;

import spatialecon.core.config.ModelConfig;
import spatialecon.core.config.Parameter;
import spatialecon.core.econ.Ces;
import spatialecon.core.econ.Optimiser;
import spatialecon.core.econ.SmoothEoS;
import spatialecon.core.engine.Firm;
import spatialecon.core.engine.Placement;
import spatialecon.core.engine.Pt;
import spatialecon.core.engine.Space;
import spatialecon.core.engine.World;
import spatialecon.core.record.Recorder;
import spatialecon.core.scenario.Scenario;

/**
 * <b>B1 — love of variety meets distance.</b> Thesis §5.3.1.
 *
 * <p>One Person at the left origin, ten Firms evenly spaced from 0 to 1, all charging the
 * same base price. Sweeps ρ and records how demand is spread across the ten.
 *
 * <p>With no distance cost, demand is flat for every ρ: identical prices, identical shares.
 * Switch distance costs on and the profile tilts, because now the Firms differ in delivered
 * price. As ρ → 1 the tilt becomes a cliff, and the nearest Firm takes the lot.
 *
 * <p>So the CES is doing double duty here. It is a preference structure, and it is also a
 * spatial interaction function — the thing that decides how far demand reaches. Which is why
 * a change in <em>tastes</em>, with no change in any cost, can redraw the map (see C7).
 */
public final class B1_LoVMeetsDistance implements Scenario {

    private static final int FIRMS = 10;

    private final Ces ces = new Ces(0.99);

    @Override public String id()        { return "b1"; }
    @Override public String title()     { return "Love of variety meets distance"; }
    @Override public String thesisRef() { return "§5.3.1"; }
    @Override public int defaultDays()  { return 99; }

    @Override
    public String notes() {
        return """
               One Person at x=0. Ten Firms evenly spaced from 0 to 1, all at base price 1.
               Distance cost 1. Sweeps rho from 0.99 down to 0.01.

               demand_0 is the nearest Firm, demand_9 the most distant.

               Watch demand_0 as rho rises towards 1: it takes an increasing share until, at
               perfect substitutability, it takes everything. Falling rho spreads demand back
               out over distance, because a mix is worth paying delivery for.

               Compare against b2, which holds rho fixed and sweeps the distance cost instead.""";
    }

    @Override
    public ModelConfig config() {
        return ModelConfig.builder()
                .space(Space.Kind.LINE).width(1.0)
                .people(1).firms(FIRMS)
                .placePeople(Placement.at(0, 0))
                .placeFirms(Placement.evenlyAlongX(0, 1))
                .utility(ces)
                .production(new SmoothEoS())
                .baseGoodCost(1.0)
                .deliveryCost(1.0)
                .build();
    }

    @Override
    public void beforeDay(World world, long day) {
        double t = day / (double) Math.max(defaultDays() - 1, 1);
        ces.setRho(0.99 - t * 0.98);
    }

    @Override
    public void record(Recorder r, World w) {
        r.global("rho", ces::rho);
        for (int i = 0; i < FIRMS; i++) {
            int index = i;
            r.global("demand_" + i, () -> demand(w)[index]);
            r.global("price_" + i, () -> price(w, index));
        }
        r.global("nearestShare", () -> {
            double[] d = demand(w);
            double total = 0;
            for (double v : d) total += v;
            return total == 0 ? 0 : d[0] / total;
        });
    }

    private double price(World w, int index) {
        Firm f = w.firm(index);
        return f.priceAtDistance(w.distance(new Pt(0, 0), f));
    }

    private double[] demand(World w) {
        double[] prices = new double[FIRMS];
        for (int i = 0; i < FIRMS; i++) prices[i] = price(w, i);
        return Optimiser.constrained().optimise(prices, 1.0, ces, w.rng());
    }

    @Override
    public List<Parameter> controls(World world) {
        return List.of(ces.rhoParam(), world.deliveryCostParam());
    }
}
