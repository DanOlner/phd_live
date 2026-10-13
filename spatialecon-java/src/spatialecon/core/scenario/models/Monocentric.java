package spatialecon.core.scenario.models;

import spatialecon.core.behaviour.PersonBundle;
import spatialecon.core.config.ModelConfig;
import spatialecon.core.econ.Ces;
import spatialecon.core.econ.SmoothEoS;
import spatialecon.core.engine.Placement;
import spatialecon.core.engine.Space;

/**
 * Shared setup for the monocentric density-cost runs (§5.4).
 *
 * <p>C1, C2 and C8 are the same model looked at three ways — the only difference between C1
 * and C8 is the density cost multiplier. Building the config in one place keeps them from
 * drifting apart, which is exactly what happened to the original's configs.
 *
 * <p>Numbers from {@code CEUS1.properties} as shipped in the {@code MonoCentricDensity} jar:
 * 500 People, one Firm at the centre, a plane 2 wide, density radius 1/16, distance cost
 * 0.25, no commuting. Note the width of 2 — the density models double the space "to allow
 * People a more variable space to disperse into" (§4.2), so the radius of 1/16 here is the
 * same absolute distance as 1/32 in the unit-width models.
 */
final class Monocentric {

    /** As shipped: 500 People, one central Firm, plane of width 2. */
    static ModelConfig.Builder base(double densityCost) {
        return ModelConfig.builder()
                .space(Space.Kind.PLANE).width(2.0)
                .people(500).firms(1)
                .placePeople(Placement.random())
                .placeFirms(Placement.centre())
                .utility(new Ces(0.4))
                .production(new SmoothEoS())
                .baseGoodCost(1.0)
                .wage(1.0)
                .deliveryCost(0.25)
                .commuteCost(0)
                .densityCost(densityCost)
                .densityRadius(1.0 / 16)
                .personBehaviour(new PersonBundle())
                .peopleMobile(true);
    }

    private Monocentric() { }
}
