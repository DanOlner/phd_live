// @ts-check

/**
 * **A4 — hill-climber against the analytic optimum.** Thesis §4.6.1, §5.2.3.
 *
 * Steps the hill-climber's iteration budget upwards and, at each step, runs a batch of
 * optimisations from fresh random starts. Records how close the mean lands to the known
 * answer and how much spread is left.
 *
 * With n equally priced goods and a budget of 1, the analytic optimum is 1/n of each, so
 * every run has a target to be scored against. The two methods agree; the search just takes
 * a thousand iterations and never quite stops wobbling, while the algebra is exact and
 * essentially free.
 *
 * Two details the thesis draws out. Most of the gain arrives almost immediately and the rest
 * of the run buys very little. And more goods converge *faster* — slicing a budget across
 * many goods leaves each slice less room to vary — which is why this records two good counts
 * at once. (That advantage reverses once both have converged; see the econ tests.)
 *
 * The residual spread is not a defect. It is where the demand noise of §5.5.2 comes from,
 * and that noise is what makes Firms misread their own revenue signals.
 */

import { POINT, defineModel } from '../config.js';
import { ces, constrained, hillClimb, smoothEoS } from '../econ.js';

const MAX_ITERATIONS = 2000;
const BATCH = 200;
const FEW_GOODS = 2;
const MANY_GOODS = 8;
const DAYS = 40;

const u = ces(0.5);
let iterations = 1;

/** @typedef {{mean: number, sd: number, meanUtility: number}} Batch */
/** @type {Batch} */
let few = { mean: 0, sd: 0, meanUtility: 0 };
/** @type {Batch} */
let many = { mean: 0, sd: 0, meanUtility: 0 };

/** @param {number} goods */
const evenPrices = (goods) => new Array(goods).fill(1.0);

/**
 * Run a batch of hill-climbs and summarise the first good's quantity.
 * @param {number} goods @param {import('../rng.js').Rng} rng @returns {Batch}
 */
function batch(goods, rng) {
  const optimiser = hillClimb(iterations);
  const prices = evenPrices(goods);
  const firstGood = new Array(BATCH);
  let utilitySum = 0;

  for (let i = 0; i < BATCH; i++) {
    const q = optimiser.optimise(prices, 1.0, u, rng);
    firstGood[i] = q[0];
    utilitySum += u.utilityOf(q);
  }

  const mean = firstGood.reduce((a, b) => a + b, 0) / BATCH;
  const variance = firstGood.reduce((a, v) => a + (v - mean) * (v - mean), 0) / BATCH;
  return { mean, sd: Math.sqrt(variance), meanUtility: utilitySum / BATCH };
}

/** @type {import('../scenario.js').Scenario} */
export const a4 = {
  id: 'a4',
  title: 'Hill-climber vs constrained optimiser',
  thesisRef: '§4.6.1, §5.2.3',
  defaultDays: DAYS,

  notes: `Steps the hill-climber's iteration budget from 1 to 2000 across the run, running
200 optimisations at each step from fresh random starts.

All goods cost 1 and the budget is 1, so the analytic optimum is 1/n each: 0.5 for the
2-good case, 0.125 for the 8-good case. Those are the targets.

Watch: most of the improvement arrives in the first few iterations and the rest of the
budget buys very little. Watch also that the 8-good case converges FASTER than the 2-good
one -- more goods means smaller budget slices means less room for the random walk to wander.

sd2 and sd8 never quite reach zero. That residue is exactly the demand noise that makes
Firms misread their revenue in model d2.`,

  config: () =>
    defineModel({
      spaceKind: POINT,
      peopleCount: 1,
      firmCount: FEW_GOODS,
      utility: u,
      production: smoothEoS(),
      baseGoodCost: 1.0,
    }),

  // Each batch is computed once per day and cached. Recording them separately would run a
  // fresh batch per column, so the reported mean and SD would describe different draws --
  // and cost four times the work for a worse answer.
  beforeDay(world, day) {
    // Clamped, and here it is not only about the picture: the cost of a day is 400 hill-climbs
    // of `iterations` steps each, so an unclamped ramp makes the page slower every second it
    // is left open.
    const t = Math.min(day, DAYS - 1) / Math.max(DAYS - 1, 1);
    iterations = Math.max(1, Math.round(t * MAX_ITERATIONS));
    few = batch(FEW_GOODS, world.rng);
    many = batch(MANY_GOODS, world.rng);
  },

  record(r, w) {
    r.global('iterations', () => iterations);

    // Arrow functions, not captured references: `few` is replaced each day, so anything
    // bound at record() time would report the empty batch for the whole run.
    r.global('analytic2', () => 1 / FEW_GOODS);
    r.global('mean2', () => few.mean);
    r.global('sd2', () => few.sd);
    r.global('absError2', () => Math.abs(few.mean - 1 / FEW_GOODS));

    r.global('analytic8', () => 1 / MANY_GOODS);
    r.global('mean8', () => many.mean);
    r.global('sd8', () => many.sd);
    r.global('absError8', () => Math.abs(many.mean - 1 / MANY_GOODS));

    // What the analytic method gets for the same problem, as the ceiling to approach.
    r.global('constrainedUtility2', () =>
      u.utilityOf(constrained().optimise(evenPrices(FEW_GOODS), 1.0, u, w.rng)));
    r.global('hillClimbUtility2', () => few.meanUtility);
  },

  controls: () => [u.rhoParam()],
};
