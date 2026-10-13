// @ts-check

/**
 * The economics, checked against values that can be worked out by hand.
 *
 * Tier 1 of the verification scheme (BUILD_PLANNER §6): no randomness anywhere except the
 * optimiser tests, so these have exactly right answers rather than right shapes. Everything
 * later — density costs, stock targeting, regime transitions — is built on these functions,
 * so if one is subtly wrong the error surfaces much later disguised as an interesting
 * result.
 *
 * Ported from `spatialecon-java/test/spatialecon/EconTest.java`.
 */

import { AS_THESIS_STATES, ORIGINAL } from '../src/config.js';
import {
  ces, cobbDouglas, constrained, coreModelEoS, hillClimb, smoothEoS, stoneGeary,
} from '../src/econ.js';
import { Rng } from '../src/rng.js';
import { assertClose, assertTrue, report, suite, test } from './tiny.js';

const EPS = 1e-9;
const LOOSE = 1e-3;

// --- CES -----------------------------------------------------------------------------

suite('CES');

test('splits evenly at equal prices, for any rho', () => {
  for (const rho of [0.1, 0.25, 0.5, 0.75, 0.9]) {
    const q = ces(rho).optimalQuantities([1, 1, 1, 1], 1.0);
    for (const v of q) assertClose(`even split at rho=${rho}`, 0.25, v, EPS);
  }
});

test('exhausts the budget exactly', () => {
  const prices = [0.7, 1.3, 2.9, 0.4];
  const budget = 2.5;
  const q = ces(0.4).optimalQuantities(prices, budget);
  let spend = 0;
  for (let i = 0; i < q.length; i++) spend += q[i] * prices[i];
  assertClose('spend equals budget', budget, spend, EPS);
});

test('matches the closed form U = Y * n^(1/rho - 1)', () => {
  for (const n of [2, 3, 8, 16]) {
    for (const rho of [0.3, 0.5, 0.8]) {
      const u = ces(rho);
      const prices = new Array(n).fill(1.0);
      const actual = u.utilityOf(u.optimalQuantities(prices, 1.0));
      assertClose(`n=${n} rho=${rho}`, Math.pow(n, 1 / rho - 1), actual, EPS);
    }
  }
});

test('utility rises as rho falls (more love of variety)', () => {
  const prices = [1, 1, 1];
  let previous = -Infinity;
  for (const rho of [0.9, 0.7, 0.5, 0.3, 0.1]) {
    const u = ces(rho);
    const value = u.utilityOf(u.optimalQuantities(prices, 1.0));
    assertTrue(`utility should rise as rho falls (rho=${rho})`, value > previous);
    previous = value;
  }
});

test('utility rises with variety, at a decreasing rate', () => {
  const u = ces(0.75);
  let previousUtility = 0, previousGain = Infinity;
  for (let n = 1; n <= 12; n++) {
    const prices = new Array(n).fill(1.0);
    const value = u.utilityOf(u.optimalQuantities(prices, 1.0));
    if (n > 1) {
      const gain = value - previousUtility;
      assertTrue(`more variety is worth more (n=${n})`, gain > 0);
      assertTrue(`gain should shrink (n=${n})`, gain < previousGain);
      previousGain = gain;
    }
    previousUtility = value;
  }
});

test('favours cheaper goods', () => {
  const q = ces(0.5).optimalQuantities([1, 2, 4], 1.0);
  assertTrue('cheapest gets most', q[0] > q[1]);
  assertTrue('and so on down', q[1] > q[2]);
});

test('perfect substitutes: everything to the cheapest, both readings', () => {
  // ORIGINAL multiplies price by budget; the rho->1 limit divides. They agree only when the
  // price is 1, which is why this went unnoticed for twelve years. CODE_ANATOMY §7.1.
  const prices = [2.0, 0.5];

  const original = ces(1.0, ORIGINAL).optimalQuantities(prices, 1.0);
  assertClose('nothing for the dearer good', 0.0, original[0], EPS);
  assertClose('original formula: price * budget', 0.5, original[1], EPS);

  const limit = ces(1.0, AS_THESIS_STATES).optimalQuantities(prices, 1.0);
  assertClose('nothing for the dearer good', 0.0, limit[0], EPS);
  assertClose('limit formula: budget / price', 2.0, limit[1], EPS);
});

test('the limit form is what rho->1 actually converges to', () => {
  const prices = [2.0, 0.5];
  const approached = ces(0.999).optimalQuantities(prices, 1.0)[1];
  const limit = ces(1.0, AS_THESIS_STATES).optimalQuantities(prices, 1.0)[1];
  assertClose('rho=0.999 should be near the limit', limit, approached, 0.05);
});

// --- Cobb-Douglas / Stone-Geary ------------------------------------------------------

suite('Cobb-Douglas and Stone-Geary');

test('demands are independent: one price cannot move the other demand', () => {
  const cd = cobbDouglas(0.5);
  const firstGood = cd.optimalQuantities([1.0, 1.0], 1.0)[0];
  for (const otherPrice of [0.2, 0.5, 1.0, 2.0, 5.0, 20.0]) {
    const q = cd.optimalQuantities([1.0, otherPrice], 1.0);
    assertClose('demand for good 0 must not move', firstGood, q[0], EPS);
  }
});

test('unit elastic: revenue is invariant to price, so a Firm has no lever', () => {
  const cd = cobbDouglas(0.4);
  const expected = 0.6; // (1 - delta) * budget
  for (const price of [0.25, 0.5, 1.0, 2.0, 8.0]) {
    const q = cd.optimalQuantities([1.0, price], 1.0);
    assertClose(`revenue invariant at price ${price}`, expected, q[1] * price, EPS);
  }
});

test('delta is literally the income share of the first good', () => {
  for (const delta of [0.2, 0.5, 0.8]) {
    const q = cobbDouglas(delta).optimalQuantities([2.0, 3.0], 10.0);
    assertClose('share on good 0', delta * 10.0, q[0] * 2.0, EPS);
    assertClose('share on good 1', (1 - delta) * 10.0, q[1] * 3.0, EPS);
  }
});

test('reduces to Cobb-Douglas at gamma = 0', () => {
  const prices = [1.5, 0.8];
  const a = cobbDouglas(0.6).optimalQuantities(prices, 3.0);
  const b = stoneGeary(0.6, 0).optimalQuantities(prices, 3.0);
  assertClose('good 0', a[0], b[0], EPS);
  assertClose('good 1', a[1], b[1], EPS);
});

test('with gamma > 0 preferences are not homothetic', () => {
  const sg = stoneGeary(0.5, 1.0);
  const prices = [1.0, 1.0];
  const share = (/** @type {number[]} */ q) => (q[0] + q[1] === 0 ? 0 : q[0] / (q[0] + q[1]));
  const low = share(sg.optimalQuantities(prices, 1.0));
  const high = share(sg.optimalQuantities(prices, 100.0));
  assertTrue('the mix should shift with income', Math.abs(low - high) > 0.01);
});

// --- Production ----------------------------------------------------------------------

suite('Production');

test('smooth EoS matches the deduced value: t^2 + t, so 0.5 gives 0.75', () => {
  assertClose('at 0.5', 0.75, smoothEoS(1, 1).outputFor(0.5), EPS);
  assertClose('at 1.0', 2.0, smoothEoS(1, 1).outputFor(1.0), EPS);
});

test('zero curve gives constant returns', () => {
  const linear = smoothEoS(0, 2);
  assertClose('per unit at 1', 2.0, linear.outputPerUnit(1.0), EPS);
  assertClose('per unit at 50', 2.0, linear.outputPerUnit(50.0), EPS);
  assertClose('marginal at 10', 2.0, linear.marginalAt(10.0), 1e-6);
});

test('with a positive curve, output per unit rises with input', () => {
  const eos = smoothEoS(1, 1);
  let previous = 0;
  for (let l = 0.5; l <= 5; l += 0.5) {
    const perUnit = eos.outputPerUnit(l);
    assertTrue(`output per unit should rise at l=${l}`, perUnit > previous);
    previous = perUnit;
  }
});

test('granular shares sum to total output', () => {
  const eos = smoothEoS(1, 1);
  const total = 3.0;
  let summed = 0;
  for (const c of [0.5, 1.0, 1.5]) summed += eos.granularShare(total, c);
  assertClose('shares sum to f(T)', eos.outputFor(total), summed, EPS);
});

test('Core Model production is negative below alpha', () => {
  const cm = coreModelEoS(3, 1);
  assertTrue('one unit of labour cannot produce', cm.outputFor(1.0) < 0);
  assertTrue('two cannot either', cm.outputFor(2.0) < 0);
  assertClose('exactly at alpha, output is zero', 0.0, cm.outputFor(3.0), EPS);
  assertTrue('above alpha it finally produces', cm.outputFor(4.0) > 0);
  assertTrue('not viable just below alpha', !cm.canProduceAt(2.99));
  assertTrue('viable at alpha', cm.canProduceAt(3.0));
});

test('Core Model marginal is constant by construction', () => {
  const cm = coreModelEoS(3, 2);
  assertClose('at 4', 0.5, cm.marginalAt(4.0), 1e-6);
  assertClose('at 40', 0.5, cm.marginalAt(40.0), 1e-6);
});

// --- Optimisers ----------------------------------------------------------------------

suite('Optimisers');

/**
 * Mean absolute deviation of the first good's quantity from its analytic optimum.
 * @param {ReturnType<typeof hillClimb>} optimiser @param {any} u
 * @param {number[]} prices @param {number} target @param {number} batch
 */
function meanAbsoluteError(optimiser, u, prices, target, batch) {
  const rng = new Rng(99);
  let sum = 0;
  for (let i = 0; i < batch; i++) {
    sum += Math.abs(optimiser.optimise(prices, 1.0, u, rng)[0] - target);
  }
  return sum / batch;
}

/** @param {any} u @param {number} goods @param {number} iterations */
function absoluteError(u, goods, iterations) {
  const prices = new Array(goods).fill(1.0);
  return meanAbsoluteError(hillClimb(iterations), u, prices, 1 / goods, 300);
}

test('the hill-climber agrees with the analytic optimum', () => {
  const u = ces(0.5);
  const rng = new Rng(42);
  const prices = [1, 1, 1, 1];
  const exact = constrained().optimise(prices, 1.0, u, rng);
  const searched = hillClimb(4000).optimise(prices, 1.0, u, rng);
  for (let i = 0; i < prices.length; i++) {
    assertClose(`good ${i}`, exact[i], searched[i], LOOSE);
  }
});

test('more iterations means less error', () => {
  const u = ces(0.5);
  const prices = [1, 1];
  const coarse = meanAbsoluteError(hillClimb(5), u, prices, 0.5, 200);
  const fine = meanAbsoluteError(hillClimb(2000), u, prices, 0.5, 200);
  assertTrue(`2000 iterations (${fine}) should beat 5 (${coarse})`, fine < coarse);
});

test('more goods converge faster, early on (the §5.2.3 claim)', () => {
  // True in ABSOLUTE error and only early: slicing a budget across many goods gives each
  // slice less room to vary. Not true in relative terms, and it reverses once converged --
  // see the next test. The thesis illustrates the early part of the curve, so its claim
  // stands as made; these two tests pin its scope.
  const u = ces(0.5);
  for (const iterations of [5, 20, 60]) {
    const twoGoods = absoluteError(u, 2, iterations);
    const eightGoods = absoluteError(u, 8, iterations);
    assertTrue(
      `at ${iterations} iterations, 8 goods (${eightGoods}) should beat 2 (${twoGoods})`,
      eightGoods < twoGoods,
    );
  }
});

test('...and that advantage reverses once converged', () => {
  // At 1000 iterations the 2-good case is essentially exact while 8 goods sits orders
  // worse. Worth showing on the website: a plot stopping at 200 iterations tells the
  // opposite story to one running to 2000.
  const u = ces(0.5);
  const twoGoods = absoluteError(u, 2, 1000);
  const eightGoods = absoluteError(u, 8, 1000);
  assertTrue(`2 goods should be near-exact by 1000 iterations, was ${twoGoods}`, twoGoods < 1e-4);
  assertTrue(`and should now beat 8 goods (${eightGoods})`, twoGoods < eightGoods);
});

test('the hill-climber never overspends', () => {
  const u = ces(0.5);
  const rng = new Rng(7);
  const prices = [0.5, 1.5, 3.0];
  const budget = 2.0;
  const q = hillClimb(500).optimise(prices, budget, u, rng);
  let spend = 0;
  for (let i = 0; i < q.length; i++) spend += q[i] * prices[i];
  assertClose('spend equals budget', budget, spend, EPS);
});

test('best-so-far never decreases: the ratchet only keeps improvements', () => {
  const u = ces(0.5);
  const climber = hillClimb(500);
  climber.optimise([1, 1, 1], 1.0, u, new Rng(3));
  const trace = climber.lastTrace ?? [];
  for (let i = 1; i < trace.length; i++) {
    assertTrue(`best-so-far must not fall at step ${i}`, trace[i] >= trace[i - 1] - EPS);
  }
});

report();
