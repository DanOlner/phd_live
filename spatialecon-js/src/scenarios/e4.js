// @ts-check

/**
 * **E4 — tracking down the transmission belt.** Thesis §6.3.4.
 *
 * The "why is this happening?" model, and the thesis' answer to its own methodological
 * complaint. E3 shows a catastrophic shift in production structure; showing it is the
 * *start* of an explanation, not the end. This is where the mechanism gets pinned down, and
 * it is pinned down by taking the agents away.
 *
 * What is left is four pairs of numbers and one law of motion:
 *
 *     p₀, p₁     prices
 *     c₀, c₁     demanded amounts, from the CES optimum at those prices
 *     t₀, t₁     input each Firm receives, c·p
 *     f₀, f₁     produced amounts, from the smooth EoS function
 *     Δp = k(c − f)
 *
 * No People, no Firms, no turns, no sampling, no noise, no space. A difference equation.
 *
 * At symmetry it sits still: each Firm receives half of a budget of one, so t = 0.5,
 * f(0.5) = 0.5² + 0.5 = 0.75, and demand at the equilibrium price 2/3 is 0.5/(2/3) = 0.75.
 * Supply equals demand and nothing moves. Perturb one price by a hair and:
 *
 * - **ρ = 0.73** — it comes straight back. The perturbation is self-correcting.
 * - **ρ = 0.80** — it runs away to a new pair of equilibria with one efficient producer.
 *
 * **This is what locates the externality.** Both Firms are updated simultaneously from the
 * same state, so it is not an artefact of who moved first, or of the permutation gap, or of
 * anything about agents at all. The only channel between the two Firms is that *both prices
 * appear in the same constrained optimum* — that is what CES demand is — and ρ sets how
 * strongly one price moves demand for the other good. Below the threshold the price
 * correction outruns the demand shift; above it, the demand shift outruns the price
 * correction. The instability lives inside the Person's utility function.
 *
 * The thesis calls this the answer to "emergence is not explanation", and it is the strongest
 * methodological claim in the chapter: an agent model produced the phenomenon, and then the
 * simplest possible non-agent model said what it was.
 */

import { POINT, defineModel, parameter } from '../config.js';
import { ces, smoothEoS } from '../econ.js';

const RHO_START = 0.73;
const STEP = 0.02;          // the "small constant" the law of motion is applied by
const DAYS = 4000;

const u = ces(RHO_START);
const production = smoothEoS(1, 1);

/** The symmetric fixed point, deducible by hand: t = 0.5, f = 0.75, p = t/f. */
const SYMMETRIC_PRICE = 2 / 3;

/** The whole state of the model. */
let prices = [SYMMETRIC_PRICE, SYMMETRIC_PRICE];
let demanded = [0, 0];
let produced = [0, 0];

/** How far the run is nudged off symmetry at the start. */
let perturbation = 0.002;

function reset() {
  prices = [SYMMETRIC_PRICE - perturbation, SYMMETRIC_PRICE + perturbation];
  demanded = [0, 0];
  produced = [0, 0];
}

/**
 * One application of the law of motion, to both Firms at once.
 *
 * Simultaneous by construction. If the two were updated in sequence the second would see the
 * first's new price, and any instability could be blamed on the ordering — which is exactly
 * the objection this model exists to rule out.
 */
function step() {
  demanded = u.optimalQuantities(prices, 1);
  const input = [demanded[0] * prices[0], demanded[1] * prices[1]];
  produced = [production.outputFor(input[0]), production.outputFor(input[1])];
  const next = [
    prices[0] + STEP * (demanded[0] - produced[0]),
    prices[1] + STEP * (demanded[1] - produced[1]),
  ];
  // A negative price is meaningless here as it is in the agent model; floor it rather than
  // let the difference equation walk through zero and produce nonsense downstream.
  prices = [Math.max(next[0], 1e-6), Math.max(next[1], 1e-6)];
}

/** @type {import('../scenario.js').Scenario} */
export const e4 = {
  id: 'e4',
  title: 'Tracking down the transmission belt',
  thesisRef: '§6.3.4',
  defaultDays: DAYS,

  notes: `No agents. Four pairs of numbers and one law of motion:

  c = the CES optimum at the current prices, budget 1
  t = c * p, the input each Firm receives
  f = t^2 + t, what that input produces
  p' = p + ${STEP} * (c - f)

The symmetric fixed point is deducible by hand. Each Firm gets half of a budget of one, so
t = 0.5 and f = 0.75; demand at price 2/3 is 0.5/(2/3) = 0.75. Supply equals demand and
nothing moves.

The run starts a hair off symmetry -- prices ${SYMMETRIC_PRICE.toFixed(4)} +/- ${0.002}.

Set rho to 0.73: the perturbation dies and both prices return to 2/3.
Set rho to 0.80: it runs away, and one Firm becomes the efficient producer.

That is E3's regime shift with the agents removed, which is the point. BOTH FIRMS ARE UPDATED
SIMULTANEOUSLY from the same state, so the instability cannot be an artefact of who moved
first, or of turn order, or of sampling, or of anything about agents at all.

The only channel between the two Firms is that both prices appear in the same constrained
optimum -- that is what CES demand IS -- and rho sets how strongly one price moves demand for
the other good. Below the threshold the price correction outruns the demand shift; above it,
the demand shift outruns the price correction.

So the externality is located exactly: it lives inside the Person's utility function. This is
the thesis' answer to "emergence is not explanation" -- an agent model produced the
phenomenon, and then the simplest possible non-agent model said what it was.

Measured over 4000 steps:

  rho 0.73   asymmetry decays to 5.5e-7; both prices return to 0.6667 and both Firms
             produce 0.750. The perturbation is gone.
  rho 0.80   asymmetry grows to 0.38 and then holds. Prices settle at 0.527 and 0.907,
             production at 1.703 and 0.113 -- one efficient producer, fifteen times the
             other. It DIVERGES TO A NEW PAIR OF EQUILIBRIA rather than running away.

The thesis reports convergence at rho 0.73 in about sixty iterations; here it takes a few
thousand, because the constant the law of motion is applied by is a free choice and this one
is smaller. It changes the speed and not the threshold, which sits between 0.73 and 0.76 in
both.`,

  config: () =>
    defineModel({
      spaceKind: POINT,
      peopleCount: 1, firmCount: 2,
      utility: u,
      production,
      baseGoodCost: SYMMETRIC_PRICE,
    }),

  setUp() {
    reset();
  },

  beforeDay() {
    step();
  },

  record(r) {
    r.global('rho', () => u.rho);
    r.global('p_0', () => prices[0]);
    r.global('p_1', () => prices[1]);
    r.global('c_0', () => demanded[0]);
    r.global('c_1', () => demanded[1]);
    r.global('f_0', () => produced[0]);
    r.global('f_1', () => produced[1]);
    r.global('gap_0', () => demanded[0] - produced[0]);
    r.global('gap_1', () => demanded[1] - produced[1]);
    // Distance from symmetry. Falling means the perturbation is dying; rising means it is not.
    r.global('asymmetry', () => Math.abs(prices[0] - prices[1]));
  },

  controls: () => [
    u.rhoParam(),
    parameter({
      name: 'kick', label: 'starting perturbation', min: 0, max: 0.05,
      get: () => perturbation,
      set: (v) => { perturbation = v; reset(); },
    }),
  ],
};
