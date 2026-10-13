// @ts-check

/**
 * Shared setup for the monocentric density-cost runs (§5.4).
 *
 * C1, C2 and C8 are the same model looked at three ways — the only difference between C1 and
 * C8 is the density cost multiplier. Building the config in one place keeps them from
 * drifting apart, which is exactly what happened to the original's configs.
 *
 * Numbers from `CEUS1.properties` as shipped in the `MonoCentricDensity` jar: 500 People, one
 * Firm at the centre, a plane 2 wide, density radius 1/16, distance cost 0.25, no commuting.
 *
 * Note the width of 2 — the density models double the space "to allow People a more variable
 * space to disperse into" (§4.2), so the radius of 1/16 here is the same absolute distance as
 * 1/32 in the unit-width models. The thesis' "1/32 of the width of model space" holds for the
 * transmission models only.
 */

import { personBundle } from '../behaviour.js';
import { PLANE, defineModel } from '../config.js';
import { ces, smoothEoS } from '../econ.js';
import { atCentre, randomly } from '../space.js';

/**
 * As shipped: 500 People, one central Firm, plane of width 2.
 * @param {number} densityCost @param {object} [overrides]
 */
export function monocentric(densityCost, overrides = {}) {
  return defineModel({
    spaceKind: PLANE, width: 2.0,
    peopleCount: 500, firmCount: 1,
    placePeople: randomly(),
    placeFirms: atCentre(),
    utility: ces(0.4),
    production: smoothEoS(),
    baseGoodCost: 1.0,
    wage: 1.0,
    deliveryCost: 0.25,
    commuteCost: 0,
    densityCost,
    densityRadius: 1 / 16,
    personBehaviour: personBundle(),
    peopleMobile: true,
    ...overrides,
  });
}
