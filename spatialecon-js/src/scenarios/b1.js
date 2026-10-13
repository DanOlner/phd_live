// @ts-check

/**
 * **B1 — love of variety meets distance.** Thesis §5.3.1.
 *
 * One Person at the left origin, ten Firms evenly spaced from 0 to 1, all charging the same
 * base price. Sweeps ρ and records how demand is spread across the ten.
 *
 * With no distance cost, demand is flat for every ρ: identical prices, identical shares.
 * Switch distance costs on and the profile tilts, because now the Firms differ in delivered
 * price. As ρ → 1 the tilt becomes a cliff, and the nearest Firm takes the lot.
 *
 * So the CES is doing double duty here. It is a preference structure, and it is also a
 * spatial interaction function — the thing that decides how far demand reaches. Which is why
 * a change in *tastes*, with no change in any cost, can redraw the map (see C7).
 */

import { LINE, defineModel } from '../config.js';
import { ces, constrained, smoothEoS } from '../econ.js';
import { at, evenlyAlongX } from '../space.js';

const FIRMS = 10;
const DAYS = 99;

const u = ces(0.99);

/** @param {import('../world.js').World} w @param {number} index */
const priceOf = (w, index) => {
  const f = w.firm(index);
  return f.priceAtDistance(w.distance({ x: 0, y: 0 }, f));
};

/** @param {import('../world.js').World} w */
const demand = (w) =>
  constrained().optimise(
    Array.from({ length: FIRMS }, (_, i) => priceOf(w, i)), 1.0, u, w.rng,
  );

/** @type {import('../scenario.js').Scenario} */
export const b1 = {
  id: 'b1',
  title: 'Love of variety meets distance',
  thesisRef: '§5.3.1',
  defaultDays: DAYS,

  notes: `One Person at x=0. Ten Firms evenly spaced from 0 to 1, all at base price 1.
Distance cost 1. Sweeps rho from 0.99 down to 0.01.

demand_0 is the nearest Firm, demand_9 the most distant.

Watch demand_0 as rho rises towards 1: it takes an increasing share until, at perfect
substitutability, it takes everything. Falling rho spreads demand back out over distance,
because a mix is worth paying delivery for.

Compare against b2, which holds rho fixed and sweeps the distance cost instead.`,

  config: () =>
    defineModel({
      spaceKind: LINE, width: 1.0,
      peopleCount: 1, firmCount: FIRMS,
      placePeople: at(0, 0),
      placeFirms: evenlyAlongX(0, 1),
      utility: u,
      production: smoothEoS(),
      baseGoodCost: 1.0,
      deliveryCost: 1.0,
    }),

  beforeDay(world, day) {
    // Clamped: the run stops at defaultDays headless, but a page keeps going, and an
    // unclamped ramp walks the swept value straight out of its meaningful range.
    const t = Math.min(day, DAYS - 1) / Math.max(DAYS - 1, 1);
    u.rho = 0.99 - t * 0.98;
  },

  record(r, w) {
    r.global('rho', () => u.rho);
    for (let i = 0; i < FIRMS; i++) {
      r.global(`demand_${i}`, () => demand(w)[i]);
      r.global(`price_${i}`, () => priceOf(w, i));
    }
    r.global('nearestShare', () => {
      const d = demand(w);
      const total = d.reduce((a, b) => a + b, 0);
      return total === 0 ? 0 : d[0] / total;
    });
  },

  controls: (w) => [u.rhoParam(), w.deliveryCostParam()],
};
