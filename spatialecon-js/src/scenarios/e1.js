// @ts-check

/**
 * **E1 — oscillation and damping.** Thesis §6.3.1.
 *
 * The transmission belt's control problem, in its simplest form. A Firm adjusts price on
 * every sale to hold stock at zero. Targeting stock alone does not work: by the time stock
 * reaches zero the price is at its furthest from equilibrium, so the system sails through and
 * comes back. It is an undamped harmonic oscillator that happens to be made of prices.
 *
 * The fix is control theory rather than economics: target stock, its rate of change, and the
 * change in that rate, all to be zero at once. The thesis notes this is under-damped and that
 * properly tuning it would need the oscillation's angular frequency, which is not reliably
 * detectable — so the response magnitude is simply set by hand (divisor 100).
 *
 * Three variants, each with a value that can be checked by hand:
 *
 * - **e1** — one Firm, one Person, no distance. Equilibrium price **0.5**.
 * - **e1raw** — the same with damping off. Settles at the *wrong* price, because any point
 *   where stock happens to cross zero will do.
 * - **e1many** — 50 Firms, 50 People. Each Firm receives one unit of time, so each also
 *   settles at 0.5 — and every Person's utility is identical, the "umbilically linked" claim.
 */

import { firmStockTarget } from '../behaviour.js';
import { MEAN, MEAN_SD } from '../record.js';
import { atCentre } from '../space.js';
import { belt, clearingPrice } from './belt.js';

/** @param {string} id @param {number} firms @param {number} people @param {boolean} damped */
function make(id, firms, people, damped) {
  const expected = clearingPrice(people / firms);

  /** @type {import('../scenario.js').Scenario} */
  return {
    id,
    title: damped
      ? `Transmission belt: damped stock targeting (${firms} Firms, ${people} People)`
      : 'Transmission belt: undamped, targeting stock alone',
    thesisRef: '§6.3.1',
    defaultDays: 2000,

    notes: `${firms} Firm(s), ${people} People, no distance costs. Damping: ${damped ? "s + s' + s''" : 's only'}.

Each Firm receives ${(people / firms).toFixed(2)} units of time, so the stock-clearing price
is 1/(t+1) = ${expected.toFixed(4)}. That is the number to check.

Watch price converge and |stock| fall to ~0. With damping off, the price still stops moving
-- but at whatever value stock happened to cross zero at, which is not the clearing price.
Targeting a level says nothing about arriving there with zero momentum.

In the many-agent variant, watch utility_sd: every Person's utility is identical at every
instant. They are not merely similar; they move in lockstep, because each takes whatever
improvement is available at their moment of decision.`,

    config: () =>
      belt(firms, people, 0.5, {
        placeFirms: atCentre(),
        placePeople: atCentre(),
        firmBehaviour: firmStockTarget({
          useFirstDifference: damped, useSecondDifference: damped,
        }),
      }),

    record(r, w) {
      r.aggregate('price', w.firms, (f) => f.goodCost, MEAN_SD);
      r.aggregate('stock', w.firms, (f) => f.stock, MEAN_SD);
      r.aggregate('absStock', w.firms, (f) => Math.abs(f.stock), MEAN);
      r.aggregate('contributedTime', w.firms, (f) => f.contributedTime, MEAN_SD);
      r.aggregate('outputPerUnit', w.firms, (f) => f.outputPerUnitTime, MEAN);
      r.aggregate('utility', w.people, (p) => p.utility, MEAN_SD);
      r.global('clearingPrice', () =>
        clearingPrice(w.firms.reduce((a, f) => a + f.contributedTime, 0) / w.firms.length));
    },

    controls: (w) => [w.deliveryCostParam()],
  };
}

export const e1 = make('e1', 1, 1, true);
export const e1raw = make('e1raw', 1, 1, false);
export const e1many = make('e1many', 50, 50, true);
