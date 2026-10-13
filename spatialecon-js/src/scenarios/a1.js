// @ts-check

/**
 * **A1 — CES and love of variety.** Thesis §2.2.3, §5.2.1.
 *
 * Sweeps ρ from near 1 down to near 0 and records, at each step, the utility a budget of one
 * buys across a fixed number of equally priced goods.
 *
 * Two things to see. First, ρ carries the entire substitutes-to-variety spectrum on its own:
 * as ρ → 1 utility falls towards the budget itself, because goods become perfect substitutes
 * and a mix is worth no more than the same quantity of any one of them. Second, the
 * quantities never move — with equal prices the optimal split is even for every ρ. What
 * changes is what that same basket is *worth*.
 *
 * The analytic check: for n equally priced goods and budget Y, utility is Y · n^(1/ρ − 1).
 * At ρ = 0.5 with 3 goods and Y = 1 that is exactly 3.
 */

import { POINT, defineModel } from '../config.js';
import { ces, constrained, smoothEoS } from '../econ.js';

const GOODS = 3;
const DAYS = 99;

const u = ces(0.99);
const optimiser = constrained();
const prices = new Array(GOODS).fill(1.0);

/** @type {import('../scenario.js').Scenario} */
export const a1 = {
  id: 'a1',
  title: 'CES and love of variety',
  thesisRef: '§2.2.3, §5.2.1',
  defaultDays: DAYS,

  notes: `Sweeps rho from 0.99 down to 0.01, one step per day.
Records utility, the per-good quantity, and total spend.

Watch: utility rises steeply as rho falls (variety is worth more), while the quantity
bought of each good does not move at all. Spend stays at the budget throughout, which is
the check that the optimum is genuinely constrained.

Note the direction of rho: HIGH rho means LOW love of variety.`,

  config: () =>
    defineModel({
      spaceKind: POINT,
      peopleCount: 1,
      firmCount: GOODS,
      utility: u,
      production: smoothEoS(),
      baseGoodCost: 1.0,
      deliveryCost: 0,
    }),

  /**
   * Step rho down from 0.99 towards 0.01 across the run, then hold.
   *
   * The clamp matters now there is a live view. Headless the run stops at `defaultDays`, but
   * on a page it keeps going, and an unclamped ramp walks rho straight through zero into the
   * negative — where CES is not merely wrong but meaningless.
   */
  beforeDay(world, day) {
    const t = Math.min(day, DAYS - 1) / Math.max(DAYS - 1, 1);
    u.rho = 0.99 - t * 0.98;
  },

  record(r, w) {
    r.global('rho', () => u.rho);
    r.global('utility', () => u.utilityOf(optimiser.optimise(prices, 1.0, u, w.rng)));
    r.global('quantityPerGood', () => optimiser.optimise(prices, 1.0, u, w.rng)[0]);
    r.global('totalSpend', () => {
      const q = optimiser.optimise(prices, 1.0, u, w.rng);
      let spend = 0;
      for (let i = 0; i < q.length; i++) spend += q[i] * prices[i];
      return spend;
    });
    // Closed form, for comparison: Y · n^(1/rho − 1)
    r.global('analyticUtility', () => Math.pow(GOODS, 1 / u.rho - 1));
  },

  controls: () => [u.rhoParam()],
};
