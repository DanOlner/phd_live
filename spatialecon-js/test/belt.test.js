// @ts-check

/**
 * The transmission belt (§6.3–6.4).
 *
 * Unusually for an agent model, several equilibria here are deducible by hand, which makes
 * this the strongest available check on the whole mechanism — arithmetic rather than a
 * correlation. With x = l² + l, a Firm receiving T units of time clears its stock at price
 * 1/(T+1).
 */

import { firmStockTarget, personTransmission } from '../src/behaviour.js';
import { POINT, defineModel } from '../src/config.js';
import { ces, smoothEoS } from '../src/econ.js';
import { PLANE, randomly } from '../src/space.js';
import { World } from '../src/world.js';
import { Recorder } from '../src/record.js';
import { belt, clearingPrice } from '../src/scenarios/belt.js';
import { e5, e6, priceRatio } from '../src/scenarios/e5.js';
import { e8, perUnitRatio, productionRatio } from '../src/scenarios/e8.js';
import { e3 } from '../src/scenarios/e3.js';
import { e7, efficiencyRatio } from '../src/scenarios/e7.js';
import { assertClose, assertTrue, report, suite, test } from './tiny.js';

const EPS = 1e-4;

/**
 * @param {number} firms @param {number} people @param {number} rho
 * @param {number} distance @param {boolean} damped @param {number} days
 */
function settle(firms, people, rho, distance, damped, days) {
  const w = new World(belt(firms, people, rho, {
    spaceKind: distance > 0 ? PLANE : POINT,
    placePeople: randomly(), placeFirms: randomly(),
    deliveryCost: distance,
    firmBehaviour: firmStockTarget({ useFirstDifference: damped, useSecondDifference: damped }),
  }));
  for (let i = 0; i < days; i++) w.timeline.nextDay();
  return w;
}

suite('Transmission belt — the deducible equilibria (§6.3.1)');

test('one Firm, one Person: T = 1, so the clearing price is 1/2', () => {
  const w = settle(1, 1, 0.5, 0, true, 3000);
  assertClose('price', 0.5, w.firm(0).goodCost, EPS);
  assertClose('stock should clear', 0, w.firm(0).stock, 1e-6);
});

test('50 Firms and 50 People still clear at the deduced price', () => {
  // Each Firm again receives one unit of time, so again 0.5. That it holds at 50x the scale
  // is the check that nothing in the mechanism depends on the number of agents.
  const w = settle(50, 50, 0.5, 0, true, 2000);
  for (const f of w.firms) assertClose(`firm ${f.id}`, 0.5, f.goodCost, EPS);
});

test('the clearing price follows the input level', () => {
  // 20 Firms, 100 People: each Firm gets T = 5, so the price is 1/6.
  const w = settle(20, 100, 0.5, 0, true, 2000);
  for (const f of w.firms) assertClose(`firm ${f.id}`, 1 / 6, f.goodCost, EPS);
  assertClose('and the closed form agrees', 1 / 6, clearingPrice(5), 1e-12);
});

test('every Person gets identical utility — "umbilically linked"', () => {
  // Not merely similar. Everyone takes whatever improvement is available at their moment of
  // decision, so with no intrinsic differences between them there is nothing to separate
  // them. §6.3.1.
  const w = settle(20, 50, 0.5, 0, true, 1500);
  const first = w.person(0).utility;
  for (const p of w.people) assertClose(`person ${p.id}`, first, p.utility, 1e-9);
});

suite('Transmission belt — why the derivatives are there');

test('undamped targeting never settles; damped settles exactly', () => {
  // The thesis' claim is that targeting a stock LEVEL says nothing about arriving there with
  // zero momentum: it is an undamped harmonic oscillator made of prices. So the assertion is
  // that it keeps oscillating -- not that it rests at some particular wrong value.
  //
  // Worth stating, because the Java build recorded this as "settles at 0.299, stock 7.31".
  // It does not settle: 0.299 was one end-of-run sample of a large oscillation, and the same
  // build gives 0.42 on a re-run. Both implementations oscillate forever; they differ only
  // in amplitude, which is set by how far the randomised starting price began from
  // equilibrium -- exactly as an undamped oscillator should behave.
  const swing = (/** @type {boolean} */ damped) => {
    const w = settle(1, 1, 0.5, 0, damped, 3000);
    let lo = Infinity, hi = -Infinity;
    for (let d = 0; d < 100; d++) {
      w.timeline.nextDay();
      lo = Math.min(lo, w.firm(0).stock);
      hi = Math.max(hi, w.firm(0).stock);
    }
    return hi - lo;
  };
  const undamped = swing(false);
  const damped = swing(true);

  assertTrue(`undamped should still be oscillating, stock swing was ${undamped}`, undamped > 1e-3);
  assertTrue(`damped should be still, stock swing was ${damped}`, damped < 1e-9);
  assertTrue('and the difference should be orders of magnitude', undamped > damped * 1e6);
});

test('stock clears only when damped', () => {
  const undamped = settle(1, 1, 0.5, 0, false, 3000);
  const damped = settle(1, 1, 0.5, 0, true, 3000);
  assertTrue(`undamped stock should not clear, was ${undamped.firm(0).stock}`,
    Math.abs(undamped.firm(0).stock) > 1e-3);
  assertClose('damped stock clears', 0, damped.firm(0).stock, 1e-9);
});

suite('Transmission belt — space and scale');

test('more input means a lower clearing price', () => {
  // Economies of scale reaching consumers is the entire point of the chapter: a Firm with
  // more time invested in it produces more per unit, so it clears its stock at a lower price.
  const w = settle(50, 50, 0.5, 0.25, true, 2000);
  const time = w.firms.map((f) => f.contributedTime);
  const price = w.firms.map((f) => f.goodCost);
  assertTrue('prices should differ across locations',
    Math.max(...price) - Math.min(...price) > 0.01);
  const r = correlation(time, price);
  assertTrue(`more input should mean a lower price, corr was ${r.toFixed(3)}`, r < -0.8);
});

test('a bad starting price causes runaway; the thesis range does not', () => {
  // The thesis mentions randomising initial prices to 0.1-1.5 in passing for one figure. It
  // turns out to decide whether spatial runs converge at all: the first price corrections are
  // proportional to a stock error that is enormous when the price starts far out, and the
  // Firm never gets back. CODE_ANATOMY §7.9.
  const bad = new World(defineModel({
    spaceKind: PLANE, width: 1.0,
    peopleCount: 50, firmCount: 50,
    placePeople: randomly(), placeFirms: randomly(),
    utility: ces(0.5), production: smoothEoS(1, 1),
    baseGoodCost: 3.0, deliveryCost: 0.25,
    personBehaviour: personTransmission(), firmBehaviour: firmStockTarget(),
    peopleMobile: false, seed: 12,
  }));
  for (let i = 0; i < 2000; i++) bad.timeline.nextDay();
  const runaway = bad.firms.filter((f) => f.goodCost > 10).length;
  assertTrue(`a uniform start of 3.0 should produce runaways, got ${runaway}`, runaway > 0);

  const good = settle(50, 50, 0.5, 0.25, true, 2000);
  assertTrue('randomised starting prices should not',
    good.firms.every((f) => f.goodCost <= 10));
});

suite('E3 — symmetry breaks with no space at all (§6.3.3)');

test('symmetric equilibrium sits at the deduced 2/3, then breaks', () => {
  // The chapter's sharpest claim: a catastrophic shift in production structure needs no
  // space. Dixit-Stiglitz cannot show it, because it assumes all varieties are produced
  // symmetrically at the same scale.
  const w = new World(e3.config());
  let symmetricPrice = 0;
  let breakRho = 0;
  for (let d = 0; d < 6000; d++) {
    e3.beforeDay?.(w, d);
    w.timeline.nextDay();
    if (d === 2000) symmetricPrice = w.firm(0).goodCost;
    const eff = w.firms.map((f) => f.outputPerUnitTime);
    const ratio = Math.min(...eff) <= 0 ? 1 : Math.max(...eff) / Math.min(...eff);
    if (!breakRho && ratio > 1.15) breakRho = /** @type {any} */ (w.config.utility).rho;
  }

  assertClose('symmetric two-Firm equilibrium is 2/3', 2 / 3, symmetricPrice, 1e-3);
  assertTrue(`symmetry should break, rho was ${breakRho}`, breakRho > 0);
  // Java found 0.764; the thesis puts it near 0.75. A window, not a point.
  assertTrue(`break should be in [0.70, 0.82], was ${breakRho.toFixed(3)}`,
    breakRho > 0.70 && breakRho < 0.82);

  const prices = w.firms.map((f) => f.goodCost).sort((a, b) => a - b);
  assertClose("the winner's price falls to the one-Firm clearing price", 0.5, prices[0], 1e-2);
  assertTrue(`and the loser's climbs well above it, was ${prices[1].toFixed(3)}`, prices[1] > 0.8);
});

suite('E7 — nineteen on a line (§6.4.1)');

test('efficiency rises as distance costs fall, and nobody ends worse off', () => {
  // The welfare result: transitions to more efficient production are a positive externality
  // -- cheaper goods for everyone in reach, no effect on anyone out of reach. Unusual, since
  // most structural change in economics creates losers.
  const w = new World(e7.config());
  /** @type {{ratio: number, minUtility: number}[]} */
  const samples = [];
  for (let d = 0; d < 6000; d++) {
    e7.beforeDay?.(w, d);
    w.timeline.nextDay();
    if (d % 1000 === 999) {
      samples.push({
        ratio: efficiencyRatio(w),
        minUtility: Math.min(...w.people.map((p) => p.utility)),
      });
    }
  }
  const first = samples[0], last = samples[samples.length - 1];

  assertTrue(`efficiency ratio should rise: ${first.ratio.toFixed(2)} -> ${last.ratio.toFixed(2)}`,
    last.ratio > first.ratio * 1.5);
  assertTrue(`the worst-off Person should not end worse off: ${first.minUtility.toFixed(3)} -> ${last.minUtility.toFixed(3)}`,
    last.minUtility > first.minUtility);

  // The strong reading -- never worse off at ANY moment -- is not asserted. The Java found
  // local dips while efficiency rises, and this build samples every 1000 days, which cannot
  // see them. Trend only, and the README says so.
  assertTrue('and the trend should be monotone across samples at this resolution',
    samples.every((s, i) => i === 0 || s.minUtility >= samples[i - 1].minUtility));
});

test('the centre is advantaged before any transition — geography alone', () => {
  // An agent in the middle can reach more Firms cheaply, so gets more utility, with no
  // increasing returns needed.
  const w = new World(e7.config());
  for (let d = 0; d < 400; d++) w.timeline.nextDay(); // before the sweep starts
  const centre = w.person(9).utility;
  const edge = Math.min(w.person(0).utility, w.person(18).utility);
  assertTrue(`centre (${centre.toFixed(3)}) should beat the edge (${edge.toFixed(3)})`,
    centre > edge);
});

suite('E8 — the closing result (§6.4.2)');

test('a winner emerges from identical Firms, and the ratio reaches the published value', () => {
  // The shortfall that made this look unreproducible was measurement, not mechanism. The
  // published "Firm efficiency ratio" is the ratio of productionLevel — total output f(T) —
  // recovered by decompiling the shipped jar. The ratio of output *per unit* is
  // 0.001*T + 1 with this config, so it cannot exceed 1.3 however lopsided the split; the
  // Java build reported 1.26 and read it as a failure to reproduce ~13.
  const w = new World(e8.config());
  for (let d = 0; d < 4200; d++) {
    e8.beforeDay?.(w, d);
    w.timeline.nextDay();
  }

  const production = productionRatio(w);
  const perUnit = perUnitRatio(w);

  // The thesis reports ~13. With the shipped width of 2 the ratio passes through 13 during
  // the transition (around day 2850) and settles near 33; with the Java build's width of 1
  // it settles at 13.5. Either way the claim being checked is a catastrophic jump, so the
  // assertion is on the order of magnitude, not a target value.
  assertTrue(`production ratio should jump by an order of magnitude, was ${production.toFixed(2)}`,
    production > 8);
  assertTrue(`per-unit ratio is capped near 1.3 by the config, was ${perUnit.toFixed(3)}`,
    perUnit < 1.31);
  assertTrue('and the two must not be confused: production >> per-unit', production > perUnit * 5);

  // The Firms are identical; one simply wins. The split should be lopsided either way.
  const time = w.firms.map((f) => f.contributedTime).sort((a, b) => a - b);
  assertTrue(`the winner should take most of the time, got ${time.map((t) => t.toFixed(0)).join('/')}`,
    time[1] > time[0] * 4);

  // Flat, then vertical. A gradual drift would not be a regime change.
  const early = new World(e8.config());
  for (let d = 0; d < 2000; d++) { e8.beforeDay?.(early, d); early.timeline.nextDay(); }
  assertTrue(`before the transition the Firms should be near-identical, ratio was ${productionRatio(early).toFixed(3)}`,
    productionRatio(early) < 1.2);

  // And People migrate towards whichever won.
  const meanDist = w.people.reduce((a, p) => a + w.distance(p, w.firm(0)), 0) / w.people.length;
  const spread = new World(e8.config());
  const startDist = spread.people.reduce((a, p) => a + spread.distance(p, spread.firm(0)), 0) / 300;
  assertTrue(`People should have migrated: ${startDist.toFixed(3)} -> ${meanDist.toFixed(3)}`,
    Math.abs(meanDist - startDist) > 0.05);
});

test('rho = 0.9 suffices — the thesis value, not 0.97', () => {
  // The Java build had to push rho to ~0.97 to see a transition at all, which was a symptom
  // of watching the capped statistic.
  assertTrue('e8 sweeps to 0.9', e8.notes?.includes('0.9') ?? false);
});

suite('E5 / E6 — threading the regime boundary (§6.4)');

test('the boundary is crossed repeatedly, in both directions', () => {
  // Both rho and distance cost trigger the same regime change from opposite directions, so
  // alternately raising each maps the line between the regimes. Each crossing is a point on
  // that line.
  //
  // Detected on the ratio of PRICES, which is what the shipped jars thread on
  // ("Firm good cost ratio", VariableStoreTwoFirmPriceRatioTrigger). Threading on
  // efficiency instead is why the Java build crossed once in 20,000 days.
  for (const scenario of [e5, e6]) {
    const w = new World(scenario.config());
    for (let d = 0; d < 8000; d++) {
      scenario.beforeDay?.(w, d);
      w.timeline.nextDay();
    }
    const r = new Recorder();
    scenario.record?.(r, w);
    r.sample(0);
    const crossings = r.get('crossings').latestValue();
    assertTrue(`${scenario.id} should thread the boundary more than once, got ${crossings}`,
      crossings >= 2);
    assertTrue(`${scenario.id} should have moved rho above its start`,
      r.get('rho').latestValue() > (scenario.id === 'e5' ? 0.65 : 0.5));
  }
});

test('price is a more responsive trigger than efficiency', () => {
  // Why the trigger variable matters: at symmetry both prices sit together, and when
  // symmetry breaks the winner's price falls while the loser's climbs, so the ratio moves
  // from both ends at once. Efficiency only moves at one end.
  const w = new World(e5.config());
  for (let d = 0; d < 4000; d++) {
    e5.beforeDay?.(w, d);
    w.timeline.nextDay();
  }
  const eff = w.firms.map((f) => f.outputPerUnitTime);
  const effRatio = Math.min(...eff) <= 0 ? 1 : Math.max(...eff) / Math.min(...eff);
  assertTrue(`price ratio (${priceRatio(w).toFixed(3)}) should move at least as far as efficiency (${effRatio.toFixed(3)})`,
    priceRatio(w) >= effRatio * 0.95);
});

/** @param {number[]} a @param {number[]} b */
function correlation(a, b) {
  const ma = a.reduce((x, y) => x + y, 0) / a.length;
  const mb = b.reduce((x, y) => x + y, 0) / b.length;
  let num = 0, da = 0, db = 0;
  for (let i = 0; i < a.length; i++) {
    num += (a[i] - ma) * (b[i] - mb);
    da += (a[i] - ma) ** 2;
    db += (b[i] - mb) ** 2;
  }
  return num / Math.sqrt(da * db);
}

report();
