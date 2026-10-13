// @ts-check

/**
 * **E5 / E6 — the transmission belt in two regions.** Thesis §6.4.
 *
 * Two discrete regions a fixed distance apart, one Person in each. **e5** has one Firm per
 * region; **e6** has two, which lets each region develop its own internal structure.
 *
 * Both ρ and distance cost can trigger the same regime change, from opposite directions.
 * Raising ρ makes goods substitutable enough that efficiency beats variety and one producer
 * takes over. Raising distance cost undoes it, because reaching the distant efficient Firm
 * costs more than the efficiency is worth. The run threads back and forth across the
 * boundary by alternately raising each, mapping the line between the regimes.
 *
 * The four-Firm variant is the one the Core Model cannot match: production structure varies
 * *within* a region as well as between them, and does so as a result of model-wide
 * interaction. A model whose regions are dimensionless points has no inside for structure to
 * live in.
 *
 * **What the crossing is detected on.** The published builds thread on
 * `VariableStoreTwoFirmPriceRatioTrigger`, labelled "Firm good cost ratio" — the ratio of
 * *prices*, recovered by decompiling the shipped jars. Not efficiency. Price is the more
 * responsive signal: at symmetry both sit at the same value, and when symmetry breaks the
 * winner's price falls while the loser's climbs, so the ratio moves from both ends at once.
 * Threading on efficiency instead is why an earlier build crossed the boundary once in
 * 20,000 days rather than repeatedly.
 */

import { ces, smoothEoS } from '../econ.js';
import { TWO_REGION } from '../config.js';
import { MEAN_SD, MIN_MAX } from '../record.js';
import { splitBetweenRegions } from '../space.js';
import { belt } from './belt.js';

/** Break and restore thresholds differ, so noise near the boundary cannot flip it. */
const BREAK_ABOVE = 1.15;
const RESTORE_BELOW = 1.02;

/** Days to hold still after a crossing, so oscillations damp before sweeping again. */
const DWELL = 400;

/** Ratio of the highest price to the lowest — the published trigger. @param {import('../world.js').World} w */
export function priceRatio(w) {
  const prices = w.firms.map((f) => f.goodCost);
  const lo = Math.min(...prices), hi = Math.max(...prices);
  return lo <= 0 ? 1 : hi / lo;
}

/** @param {string} id @param {number} firmsPerRegion @param {number} rho */
function make(id, firmsPerRegion, rho) {
  const u = ces(rho);
  let distanceCost = 0.1;
  let raisingRho = true;
  let dwellUntil = 0;
  let crossings = 0;

  /** @type {import('../scenario.js').Scenario} */
  return {
    id,
    title: `Transmission belt, two regions (${firmsPerRegion} Firm${firmsPerRegion === 1 ? '' : 's'} each)`,
    thesisRef: '§6.4',
    defaultDays: 8000,

    notes: `Two regions separated by the space width, one Person in each, ${firmsPerRegion}
Firm(s) per region. Parameters from TwoRegionTransmission.properties as shipped in the
${firmsPerRegion === 1 ? 'SymmetryBreak' : 'TwoRegionFourFirms'} jar.

The run threads across the regime boundary: rho rises until symmetry breaks, then distance
cost rises until it is restored, then rho again. Each crossing is a point on the line
dividing the two regimes.

Watch priceRatio: 1.0 while production is even, above ${BREAK_ABOVE} once a producer
dominates. Watch which variable was moving when it crossed -- both rho and distance cost can
trigger the same change, from opposite directions.

The crossing is detected on the ratio of PRICES, which is what the shipped jars thread on
("Firm good cost ratio"). Efficiency moves far less and threads poorly.`,

    config: () =>
      belt(firmsPerRegion * 2, 2, rho, {
        utility: u,
        spaceKind: TWO_REGION, width: 1.0,
        placePeople: splitBetweenRegions(0.5),
        placeFirms: splitBetweenRegions(0.5),
        production: smoothEoS(0.5, 0.5), // as shipped: curve and magnitude 0.5
        deliveryCost: distanceCost,
        initialPriceRange: /** @type {[number, number]} */ ([0.55, 0.75]),
      }),

    /**
     * Alternate between raising ρ and raising distance cost, switching whenever the model
     * crosses between regimes. This is the "thread" of the original's parameter sweep.
     */
    beforeDay(world, day) {
      if (day < 300) return;        // let the initial prices settle
      if (day < dwellUntil) return; // holding still after a crossing

      const ratio = priceRatio(world);
      if (raisingRho && ratio > BREAK_ABOVE) {
        raisingRho = false;
        dwellUntil = day + DWELL;
        crossings++;
        return;
      }
      if (!raisingRho && ratio < RESTORE_BELOW) {
        raisingRho = true;
        dwellUntil = day + DWELL;
        crossings++;
        return;
      }

      if (raisingRho) {
        u.rho = Math.min(0.99, u.rho + 0.00006);
      } else {
        distanceCost += 0.00006;
        world.setDeliveryCost(distanceCost);
      }
    },

    record(r, w) {
      r.global('rho', () => u.rho);
      r.global('distanceCost', () => w.config.deliveryCost);
      r.global('priceRatio', () => priceRatio(w));
      r.global('crossings', () => crossings);
      r.global('threadingRho', () => (raisingRho ? 1 : 0));
      r.aggregate('price', w.firms, (f) => f.goodCost, MIN_MAX);
      r.aggregate('efficiency', w.firms, (f) => f.outputPerUnitTime, MIN_MAX);
      r.aggregate('contributedTime', w.firms, (f) => f.contributedTime, MIN_MAX);
      r.aggregate('utility', w.people, (p) => p.utility, MEAN_SD);
      r.perActor('firmPrice', w.firms, (f) => f.goodCost);
    },

    controls: (w) => [u.rhoParam(), w.deliveryCostParam()],
  };
}

export const e5 = make('e5', 1, 0.65);
export const e6 = make('e6', 2, 0.5);
