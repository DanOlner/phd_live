// @ts-check

/**
 * **A3 — two ways to have increasing returns.** Thesis §2.2.5.
 *
 * Sweeps labour input from 0 to 6 and records total, average and marginal output for both
 * production functions side by side.
 *
 * The smooth form x = vl² + ml produces something for any positive input, and both its
 * average and marginal output rise. The Core Model form x = (l − α)/β has a constant
 * marginal by construction, an average that rises at a decreasing rate, and — the point of
 * the exercise — returns *negative* output for any input below α.
 *
 * Perfectly fine as algebra. Fatal in an agent model, where a Person supplies one unit of
 * labour and α is 3: no individual can ever reach positive output alone, so a Person who
 * reasons correctly about it never works at all. That is §4.7.6, and it is why the thesis
 * uses the smooth function throughout.
 */

import { POINT, defineModel } from '../config.js';
import { ces, coreModelEoS, smoothEoS } from '../econ.js';

const MAX_LABOUR = 6.0;
const DAYS = 120;

const smooth = smoothEoS(1, 1);
const coreModel = coreModelEoS(3, 1);
let labour = 0;

/** @type {import('../scenario.js').Scenario} */
export const a3 = {
  id: 'a3',
  title: 'Increasing returns: two production functions',
  thesisRef: '§2.2.5',
  defaultDays: DAYS,

  notes: `Sweeps labour input from 0 to 6, one step per day.

  smooth_*     : x = l^2 + l          (curve 1, magnitude 1)
  coreModel_*  : x = (l - 3) / 1      (alpha 3, beta 1)

Watch coreModel_total below l = 3: it is negative. Not clamped, because the negative region
is the thing worth seeing. An agent supplying one unit of labour can never produce anything
at all on their own, so an agent who reasons about it correctly declines to work and
production never starts.

Both marginals are computed numerically, so they apply to any production function we add
later.`,

  config: () =>
    defineModel({
      spaceKind: POINT,
      peopleCount: 1,
      firmCount: 1,
      utility: ces(0.5),
      production: smooth,
    }),

  beforeDay(world, day) {
    const t = Math.min(day, DAYS - 1) / Math.max(DAYS - 1, 1);
    labour = t * MAX_LABOUR;
  },

  record(r) {
    r.global('labour', () => labour);

    r.global('smooth_total', () => smooth.outputFor(labour));
    r.global('smooth_average', () => smooth.outputPerUnit(labour));
    r.global('smooth_marginal', () => smooth.marginalAt(labour));

    r.global('coreModel_total', () => coreModel.outputFor(labour));
    r.global('coreModel_average', () => coreModel.outputPerUnit(labour));
    r.global('coreModel_marginal', () => coreModel.marginalAt(labour));

    // 1 while the Core Model form can produce anything at all, 0 below alpha.
    r.global('coreModel_viable', () => (coreModel.canProduceAt(labour) ? 1 : 0));
  },

  controls: () => [smooth.curveParam(), smooth.magnitudeParam()],
};
