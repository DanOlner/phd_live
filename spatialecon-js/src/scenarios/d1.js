// @ts-check

/**
 * **D1 — two Firms optimising location on a line.** Thesis §5.5.1.
 *
 * Fifty People spread evenly along a line of length one. Two Firms may change *only* their
 * position — price and wage are fixed at 1 — and do it by the crawl of `firmLocationCrawl`:
 * move, wait a turn, keep going if revenue rose, otherwise revert.
 *
 * Four cases, matching the thesis figures. Registered separately because the outcomes differ
 * qualitatively rather than in degree:
 *
 * | id | ρ | distance cost | expected |
 * |---|---|---|---|
 * | d1a | 0.1 | 1 | both lock onto a tiny central spot |
 * | d1b | 0.99 | 1 | central on average, wandering widely |
 * | d1c | 0.1 | 20 | centre-ish, much more spread |
 * | d1d | 0.99 | 20 | **Hotelling-like split to opposite ends** |
 *
 * The thesis' point is that none of this resembles Hotelling's clean deduction. Firms are
 * weak optimisers, their positions wander, and averaging over ten thousand steps hides how
 * erratic the underlying behaviour is. Hence the standard deviation and the barcode of actual
 * positions in the original figures — both are recorded here.
 */

import { firmLocationCrawl, personBundle } from '../behaviour.js';
import { LINE, defineModel } from '../config.js';
import { ces, smoothEoS } from '../econ.js';
import { MEAN_SD } from '../record.js';
import { evenlyAlongX } from '../space.js';

/**
 * What the thesis means by "rho = 1".
 *
 * Not 1.0. The original clamped rho to 0.99 in both places it could be changed —
 * `UtilityMouseChanger` and `CES_Sweeper` — so rho = 1 exactly was never reachable and no
 * published figure was produced at it. Using 1.0 here would take a code path the thesis never
 * ran, and the results differ qualitatively: at 1.0 the Firms co-locate (mean separation
 * 0.05) where at 0.99 they split (0.34). CODE_ANATOMY §7.1.
 */
const NEAR_SUBSTITUTES = 0.99;

/**
 * @param {string} id @param {number} rho @param {number} distanceCost @param {string} expectation
 * @returns {import('../scenario.js').Scenario}
 */
function make(id, rho, distanceCost, expectation) {
  const u = ces(rho);
  return {
    id,
    title: `Two Firms on a line (rho=${rho.toFixed(2)}, distance cost=${distanceCost})`,
    thesisRef: '§5.5.1',
    defaultDays: 10000,

    notes: `50 People evenly spread on a line of length 1, fixed. Two mobile Firms, price and
wage fixed at 1, changing position only.

rho = ${rho.toFixed(2)}, distance cost = ${distanceCost}.
Expected: ${expectation}.

firmX_0 / firmX_1 are the Firms' positions each day -- the raw material for the original's
'barcode' of attempted locations. firmSpread_mean/sd give the averaged view; compare the
two, because the mean is much tidier than the behaviour underneath it.

Note the Firms never settle. The crawl always perturbs, so a Firm that has found a good spot
keeps testing and keeps giving ground. That restlessness is the finding, not noise in the
measurement.`,

    config: () =>
      defineModel({
        spaceKind: LINE, width: 1.0,
        peopleCount: 50, firmCount: 2,
        placePeople: evenlyAlongX(0, 1),
        placeFirms: evenlyAlongX(0.25, 0.75),
        utility: u,
        production: smoothEoS(),
        baseGoodCost: 1.0,
        wage: 1.0,
        commuteCost: 0,
        deliveryCost: distanceCost,
        densityCost: 0,
        personBehaviour: personBundle(),
        firmBehaviour: firmLocationCrawl(),
        peopleMobile: false,
        firmsMobile: true,
      }),

    record(r, w) {
      r.global('firmX_0', () => w.firm(0).position.x);
      r.global('firmX_1', () => w.firm(1).position.x);
      r.global('separation', () => Math.abs(w.firm(0).position.x - w.firm(1).position.x));
      r.aggregate('firmSpread', w.firms, (f) => f.position.x, MEAN_SD);
      r.aggregate('firmRevenue', w.firms, (f) => f.revenueLastTurn, MEAN_SD);
      // Demand across the line, for the profile drawn above and below the axis.
      r.perActor('personX', w.people, (p) => p.position.x);
      r.perActor('personUtility', w.people, (p) => p.utility);
    },

    controls: (w) => [u.rhoParam(), w.deliveryCostParam()],
  };
}

export const d1a = make('d1a', 0.1, 1, 'both lock onto a tiny central spot');
export const d1b = make('d1b', NEAR_SUBSTITUTES, 1, 'central on average, but wandering the whole line');
export const d1c = make('d1c', 0.1, 20, 'centre-ish, considerably more spread out');
export const d1d = make('d1d', NEAR_SUBSTITUTES, 20, 'Hotelling-like split towards opposite ends');
