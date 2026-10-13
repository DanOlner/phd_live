package spatialecon.core.scenario.models;

import spatialecon.core.behaviour.FirmStockTarget;
import spatialecon.core.behaviour.PersonTransmission;
import spatialecon.core.config.ModelConfig;
import spatialecon.core.econ.Ces;
import spatialecon.core.econ.SmoothEoS;
import spatialecon.core.engine.Placement;
import spatialecon.core.engine.Space;

/**
 * Shared setup for the transmission belt models (§6.3–6.4).
 *
 * <p>People buy goods directly with time; Firms turn that time into output through smooth
 * economies of scale and adjust price on every sale to hold stock at target.
 *
 * <p>Two defaults here are load-bearing rather than incidental:
 *
 * <ul>
 *   <li><b>Randomised starting prices (0.1–1.5).</b> The thesis mentions this in passing for
 *       one figure; it turns out to decide whether spatial runs converge at all. From a
 *       uniform 3.0, several Firms overshoot into the runaway of §6.3.2 and never return.
 *   <li><b>Normalised {@code x = l² + l}.</b> Curve and magnitude both 1, which is what makes
 *       the equilibrium prices deducible by hand — 0.5 for one Firm and one Person, 0.667 at
 *       symmetric two-Firm equilibrium. Scenarios needing a gentler curve override it.
 * </ul>
 */
final class TransmissionBelt {

    static ModelConfig.Builder base(int firms, int people, double rho) {
        return ModelConfig.builder()
                .space(Space.Kind.POINT).width(1.0)
                .people(people).firms(firms)
                .placePeople(Placement.random())
                .placeFirms(Placement.random())
                .utility(new Ces(rho))
                .production(new SmoothEoS(1, 1))
                .baseGoodCost(1.0)
                .deliveryCost(0)
                .randomiseInitialPrices(0.1, 1.5)
                .personBehaviour(new PersonTransmission())
                .firmBehaviour(new FirmStockTarget())
                .peopleMobile(false);
    }

    /**
     * Price that clears stock when {@code t} units of time reach a Firm, for {@code x = l²+l}.
     *
     * <p>A Person paying price {@code p} for {@code q} units contributes {@code q·p} of input.
     * Their share of output is {@code f(T)·(t_a/T)}, so stock clears when
     * {@code q = f(T)/T · q·p}, i.e. {@code p = T/f(T) = 1/(T+1)}.
     */
    static double clearingPrice(double totalInput) {
        return 1.0 / (totalInput + 1);
    }

    private TransmissionBelt() { }
}
