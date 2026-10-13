// @ts-check

/**
 * Shared setup for the transmission belt models (§6.3–6.4).
 *
 * People buy goods directly with time; Firms turn that time into output through smooth
 * economies of scale and adjust price on every sale to hold stock at target.
 *
 * Two defaults here are load-bearing rather than incidental:
 *
 * - **Randomised starting prices (0.1–1.5).** The thesis mentions this in passing for one
 *   figure; it turns out to decide whether spatial runs converge at all. From a uniform 3.0,
 *   several Firms overshoot into the runaway of §6.3.2 and never return. CODE_ANATOMY §7.9.
 * - **Normalised x = l² + l.** Curve and magnitude both 1, which is what makes the
 *   equilibrium prices deducible by hand — 0.5 for one Firm and one Person, 0.667 at
 *   symmetric two-Firm equilibrium. Scenarios needing a gentler curve override it.
 */

import { firmStockTarget, personTransmission } from '../behaviour.js';
import { POINT, defineModel } from '../config.js';
import { ces, smoothEoS } from '../econ.js';
import { randomly } from '../space.js';

/**
 * @param {number} firms @param {number} people @param {number} rho
 * @param {object} [overrides]
 */
export function belt(firms, people, rho, overrides = {}) {
  return defineModel({
    spaceKind: POINT, width: 1.0,
    peopleCount: people, firmCount: firms,
    placePeople: randomly(),
    placeFirms: randomly(),
    utility: ces(rho),
    production: smoothEoS(1, 1),
    baseGoodCost: 1.0,
    deliveryCost: 0,
    initialPriceRange: /** @type {[number, number]} */ ([0.1, 1.5]),
    personBehaviour: personTransmission(),
    firmBehaviour: firmStockTarget(),
    peopleMobile: false,
    ...overrides,
  });
}

/**
 * Price that clears stock when `t` units of time reach a Firm, for x = l² + l.
 *
 * A Person paying price p for q units contributes q·p of input. Their share of output is
 * f(T)·(t_a/T), so stock clears when q = f(T)/T · q·p, i.e. **p = T/f(T) = 1/(T+1)**.
 *
 * This is why the belt is the best-checkable part of the whole thesis: the answer is
 * arithmetic, not a correlation.
 *
 * @param {number} totalInput
 */
export function clearingPrice(totalInput) {
  return 1 / (totalInput + 1);
}
