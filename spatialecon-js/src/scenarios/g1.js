// @ts-check

/**
 * **G1 — Krugman's Core Model.** Thesis §2.5.5.
 *
 * The comparator. Not an agent model and not part of the framework — a separate program in
 * the original, ported here because it is the benchmark everything else in the thesis is
 * measured against, and because the transmission belt is offered as an alternative to it.
 *
 * Two regions, mobile manufacturing workers, immobile farmers, and iceberg transport costs.
 * λ is the share of manufacturing in region 1; the model solves for wages at every λ and asks
 * where workers stop wanting to move. See `src/coremodel.js` for the three equations.
 *
 * **The result, at the shipped T = 2.25.** Five equilibria: the two corners, an unstable pair
 * either side of centre, and a stable symmetric one at λ = 0.5. Lower the transport cost and
 * the symmetric equilibrium loses its stability while the corners keep theirs — the economy
 * tips into one region or the other and stays there. That is the tomahawk, and it is the
 * canonical result of geographical economics.
 *
 * **What the thesis takes from it, and what it does not.** The mechanism is right and famous:
 * increasing returns interacting with transport costs produce agglomeration, and the
 * transition is catastrophic rather than gradual. What the Core Model cannot do is separate
 * love of variety from economies of scale — the elasticity `e` does both jobs — or let
 * production structure vary *within* a region, because a region here is a dimensionless point
 * with no interior.
 *
 * Compare directly:
 *
 * - **E3** gets a catastrophic production shift with two Firms, one Person and no space at
 *   all, from increasing returns and substitutability alone.
 * - **E5's four-Firm variant** varies production structure inside a region, which a model
 *   whose regions are points cannot represent.
 * - **E8** produces the two-core outcome from Firms identical in every respect, deciding which
 *   wins from an arbitrarily small difference in starting price.
 *
 * And note what this model assumes that none of those do: every variety is produced at the
 * same scale by construction, there is no individual Firm anywhere in it, and λ moves because
 * the modeller says workers follow real wages — not because anybody decides anything.
 */

import { DEFAULTS } from '../coremodel.js';
import { equilibria, shortRun } from '../coremodel.js';
import { POINT, defineModel, parameter } from '../config.js';
import { ces, smoothEoS } from '../econ.js';

/** Live parameters. The model is solved, not simulated, so these are the whole state. */
const params = { ...DEFAULTS };

/** @returns {import('../coremodel.js').CoreParams} */
export function coreParams() {
  return params;
}

/** @type {import('../scenario.js').Scenario} */
export const g1 = {
  id: 'g1',
  title: "Krugman's Core Model: short run and long run",
  thesisRef: '§2.5.5',
  defaultDays: 1,

  notes: `NOT AN AGENT MODEL. This is Krugman's Core Model, a separate program in the original,
ported because it is the benchmark the rest of the thesis is measured against.

Two regions. A share d of income goes on manufactures made by mobile workers under increasing
returns; the rest on agriculture made by immobile farmers. Shipping manufactures costs T in
iceberg form -- send T units to land one. lambda is the share of manufacturing in region 1.

The chart solves for wages at every lambda and plots the real wage ratio w1/w2. Where it
crosses 1, neither region pays better and nobody moves: an equilibrium. Filled circles are
stable, hollow ones unstable. The arrows along the top show which way workers are pulled.

At the shipped T = 2.25 there are FIVE equilibria. Three are visible as crossings; the other
two are the corners at lambda 0 and 1, which are equilibria by construction -- with every
manufacturing worker already in one region there is nobody left to move. Read the arrows at
the two ends and you can see they are stable.

Drag T down and watch the symmetric equilibrium at 0.5 lose its stability while the corners
keep theirs. The second panel is the whole of that: every equilibrium at every transport
cost, black where stable and grey where not. The handle and the two blades give the tomahawk
its name, and it is the canonical result of geographical economics.

WHAT IT CANNOT DO, and why the thesis builds something else. The elasticity e is doing two
jobs at once -- it is love of variety AND economies of scale, inseparably. And a region here
is a dimensionless point, so production structure cannot vary within one.

Compare e3, which gets a catastrophic production shift with two Firms, one Person and no
space at all. Compare e5's four-Firm variant, where structure varies inside a region. Compare
e8, which produces the two-core outcome from Firms identical in every respect.

And note what this model assumes that none of those do: every variety is produced at the same
scale by construction, there is no individual Firm anywhere in it, and lambda moves because
the modeller says workers follow real wages -- not because anybody decides anything.`,

  // A shell. Nothing is simulated: the figure is solved from the parameters each time they
  // change, so the model's clock has nothing to do.
  config: () =>
    defineModel({
      spaceKind: POINT,
      peopleCount: 1, firmCount: 1,
      utility: ces(0.5),
      production: smoothEoS(),
    }),

  setUp() {
    Object.assign(params, DEFAULTS);
  },

  record(r) {
    const solved = () => shortRun(params, 200);
    r.global('T', () => params.t);
    r.global('e', () => params.e);
    r.global('interiorEquilibria', () => equilibria(solved()).length);
    r.global('stableInterior', () => equilibria(solved()).filter((x) => x.stable).length);
    r.global('symmetricStable', () => {
      const at = equilibria(solved()).find((x) => Math.abs(x.lambda - 0.5) < 0.02);
      return at && at.stable ? 1 : 0;
    });
    r.global('ratioAtZero', () => solved()[0].ratio);
    r.global('ratioAtHalf', () => solved()[100].ratio);
  },

  controls: () => [
    parameter({
      name: 'T', label: 'transport cost T', min: 1.01, max: 3.5,
      get: () => params.t, set: (v) => { params.t = v; },
    }),
    parameter({
      name: 'e', label: 'elasticity e', min: 2, max: 10,
      get: () => params.e, set: (v) => { params.e = v; },
    }),
    parameter({
      name: 'd', label: 'share on manufactures', min: 0.1, max: 0.9,
      get: () => params.d, set: (v) => { params.d = v; },
    }),
  ],
};
