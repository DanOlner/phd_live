// @ts-check

/**
 * **B2 — the double dip.** Thesis §5.3.1.
 *
 * Same line of ten Firms, but now ρ is held at 0.5 and the *distance cost* is swept from 0
 * to 40. Records demand for the nearest few.
 *
 * Demand for a nearby Firm does not fall as distance costs rise. It *rises* first, as
 * spending is reallocated away from Firms that have become expensive, and only later falls
 * once the absolute cost dominates. Hence the dip.
 *
 * The exception is instructive: a Firm at distance zero pays no distance cost at all, so it
 * absorbs reallocated demand indefinitely and never turns over. `b2z` shows it.
 *
 * The general lesson is worth stating plainly: raising a cost does not move all agents in
 * the same direction. Some are made better off by a change that makes the system worse off,
 * which is the reason aggregate reasoning about space so often misleads.
 */

import { LINE, defineModel } from '../config.js';
import { ces, constrained, smoothEoS } from '../econ.js';
import { at, evenlyAlongX } from '../space.js';

const FIRMS = 10;
const SHOWN = 5;
const MAX_DISTANCE_COST = 40;
const DAYS = 160;

/** @param {boolean} nearestAtZero */
function make(nearestAtZero) {
  const u = ces(0.5);

  /** @param {import('../world.js').World} w */
  const demand = (w) =>
    constrained().optimise(
      w.firms.map((f) => f.priceAtDistance(w.distance(w.person(0), f))), 1.0, u, w.rng,
    );

  /** @type {import('../scenario.js').Scenario} */
  return {
    id: nearestAtZero ? 'b2z' : 'b2',
    title: nearestAtZero ? 'The double dip: a Firm at distance zero' : 'The double dip',
    thesisRef: '§5.3.1',
    defaultDays: DAYS,

    notes: `One Person at x=0. Ten Firms spaced from ${nearestAtZero ? '0' : '0.1'} to 1.
rho fixed at 0.5. Sweeps distance cost from 0 to 40.

Watch demand_0 and demand_1: they RISE before they fall. Spending reallocated from Firms
that have become expensive outweighs their own rising cost, until it does not.

Scenario b2z places the nearest Firm at 0 instead. It then pays no distance cost at all,
absorbs reallocated demand indefinitely, and never turns over.

A rising cost does not push every agent the same way.`,

    config: () =>
      defineModel({
        spaceKind: LINE, width: 1.0,
        peopleCount: 1, firmCount: FIRMS,
        placePeople: at(0, 0),
        placeFirms: evenlyAlongX(nearestAtZero ? 0 : 0.1, 1),
        utility: u,
        production: smoothEoS(),
        baseGoodCost: 1.0,
        deliveryCost: 0,
      }),

    // Through world.setDeliveryCost, never the config: Firms hold their own copy of every
    // cost, so writing to the config alone would leave this sweep completely inert and the
    // whole model a flat line. CODE_ANATOMY §7.11.
    beforeDay(world, day) {
      // Clamped: the run stops at defaultDays headless, but a page keeps going, and an
      // unclamped ramp walks the swept value straight out of its meaningful range.
      const t = Math.min(day, DAYS - 1) / Math.max(DAYS - 1, 1);
      world.setDeliveryCost(t * MAX_DISTANCE_COST);
    },

    record(r, w) {
      r.global('distanceCost', () => w.config.deliveryCost);
      for (let i = 0; i < SHOWN; i++) r.global(`demand_${i}`, () => demand(w)[i]);
      r.global('totalDemand', () => demand(w).reduce((a, b) => a + b, 0));
    },

    controls: (w) => [w.deliveryCostParam(), u.rhoParam()],
  };
}

export const b2 = make(false);
/** Variant with the nearest Firm at distance zero, which removes the dip entirely. */
export const b2z = make(true);
