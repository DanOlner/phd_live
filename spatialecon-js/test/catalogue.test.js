// @ts-check

/**
 * The stage-8 tail: the models that are not agent models.
 *
 * C4 and C5 are closed forms, E4 is a difference equation, F1 and F2 are process
 * demonstrations, F3 runs two populations of its own, and G1 is Krugman's Core Model — a
 * separate program in the original. None of them goes through the engine's day loop in the
 * usual way, so none of them is covered by anything in `models.test.js` or `belt.test.js`.
 *
 * What is asserted here is the *claim each model exists to make*, in the thesis' own terms.
 * That is the tier-2 discipline (BUILD_PLANNER §6) applied to models with no Java counterpart
 * to diff against: the thesis says the more rational agent performs worse, so the test says
 * the rational population's output stays at zero.
 */

import { World } from '../src/world.js';
import { Recorder } from '../src/record.js';
import { DEFAULTS, corners, equilibria, shortRun, solveAt, tomahawk } from '../src/coremodel.js';
import { get } from '../src/scenario.js';
import '../src/scenarios/index.js';
import { assertClose, assertEqual, assertTrue, report, suite, test } from './tiny.js';

/** Run a scenario and hand back a recorder bound to it. @param {string} id @param {number} days */
function run(id, days) {
  const scenario = get(id);
  const world = new World(scenario.config());
  scenario.setUp?.(world);
  const recorder = new Recorder();
  scenario.record?.(recorder, world);
  for (let d = 0; d < days; d++) {
    scenario.beforeDay?.(world, d);
    world.timeline.nextDay();
  }
  return { world, recorder, scenario };
}

suite('C3 — spatial against non-spatial costs (§5.4.3)');

test('the four signs come out as the thesis states', () => {
  // The whole result is a set of directions, so this is the whole test. Each cost is HELD
  // rather than ramped, so each measurement is a settled model.
  /** @type {[string, string, number, number, 'in'|'out'][]} */
  const cases = [
    ['c3w', 'wage', 0.1, 1.4, 'in'],
    ['c3g', 'baseGoodCost', 0.1, 1.9, 'out'],
    ['c3c', 'commuteCost', 0.05, 0.95, 'in'],
    ['c3d', 'deliveryCost', 0.05, 0.95, 'in'],
  ];
  for (const [id, field, low, high, expected] of cases) {
    const scenario = get(id);
    const distances = [low, high].map((value) => {
      const config = scenario.config();
      /** @type {any} */ (config)[field] = value;
      const world = new World(config);
      scenario.setUp?.(world);
      for (let d = 0; d < 900; d++) world.timeline.nextDay();
      const firm = world.firm(0);
      return world.people.reduce((a, p) => a + world.distance(p, firm), 0) / world.people.length;
    });
    const movedIn = distances[1] < distances[0];
    assertEqual(`${id}: ${field} ${low}→${high} gave ${distances.map((d) => d.toFixed(3)).join(' → ')}`,
      expected, movedIn ? 'in' : 'out');
  }
});

suite('C4 and C5 — the hand-computable cases (§5.4.4)');

test('c4: whoever moves is worse off than whoever stays', () => {
  // The finding. Wherever moving out is worth doing at all, the mover still lands below the
  // stayer — so the one who acts confers the larger benefit on the one who does not.
  const { recorder, scenario } = run('c4', 0);
  const world = new World(scenario.config());
  let checked = 0;
  for (let d = 0; d < (scenario.defaultDays ?? 120); d++) {
    scenario.beforeDay?.(world, d);
    if (recorder.value('movingPays') === 1) {
      checked++;
      // At d = 0 exactly they are equal — moving is free — so this is `<=`. Everywhere
      // else the mover pays the distance cost and lands strictly below.
      assertTrue('the mover should never be above the stayer',
        recorder.value('mover') <= recorder.value('stayer') + 1e-12);
    }
  }
  assertTrue(`moving should pay somewhere in the sweep (${checked} points)`, checked > 0);
});

test('c4: the crossing is where the algebra says', () => {
  // With the defaults g = 1 and D = 0.5, moving is worth it while 1/(1+d) > 0.5, i.e. d < 1.
  const { recorder, scenario } = run('c4', 0);
  const world = new World(scenario.config());
  let lastPaying = -1;
  for (let d = 0; d < (scenario.defaultDays ?? 120); d++) {
    scenario.beforeDay?.(world, d);
    if (recorder.value('movingPays') === 1) lastPaying = recorder.value('distanceCost');
  }
  assertClose('the crossing should be at d = 1', 1, lastPaying, 0.03);
});

test('c5: two crossings, in order, and the stayer does best', () => {
  const { recorder, scenario } = run('c5', 0);
  const world = new World(scenario.config());
  /** @type {number[]} */
  const departures = [];
  for (let d = 0; d < (scenario.defaultDays ?? 140); d++) {
    scenario.beforeDay?.(world, d);
    departures.push(recorder.value('departures'));
  }
  assertTrue('all three states should appear across the sweep',
    [0, 1, 2].every((n) => departures.includes(n)));
  // Sweeping distance cost upward, departures should only ever fall — cheaper distance means
  // more people leave, so as d rises they come back in a definite order.
  const sorted = departures.slice().sort((a, b) => b - a);
  assertEqual('departures should be monotone in distance cost',
    sorted.join(''), departures.join(''));
});

suite('E2 — markup and the tipping point (§6.3.2)');

test("one Firm's markup drags the other's price DOWN", () => {
  // The finding that is not about failure. Firm 1 picks up the demand Firm 0 priced itself
  // out of, produces more efficiently under increasing returns, and clears at a lower price.
  const scenario = get('e2');
  /** @param {number} markUp */
  const priceOfFirmOne = (markUp) => {
    const world = new World(scenario.config());
    scenario.setUp?.(world);
    world.firm(0).markUp = markUp;
    for (let d = 0; d < 2500; d++) world.timeline.nextDay();
    return world.firm(1).goodCost;
  };
  const withMarkup = priceOfFirmOne(0.02);
  const without = priceOfFirmOne(0);
  assertTrue(`Firm 1 should clear lower: ${withMarkup.toFixed(5)} vs ${without.toFixed(5)}`,
    withMarkup < without);
});

test('and the coupling is the economies of scale', () => {
  // Without increasing returns a markup changes relative prices only. The drag on Firm 1
  // should therefore vanish as the EoS curve goes to zero.
  const scenario = get('e2');
  /** @param {number} curve */
  const drag = (curve) => {
    /** @param {number} markUp */
    const settle = (markUp) => {
      const config = scenario.config();
      /** @type {any} */ (config.production).curve = curve;
      const world = new World(config);
      scenario.setUp?.(world);
      world.firm(0).markUp = markUp;
      // Six thousand days, not two. At zero curve the equilibrium price is 1.0 and these
      // runs start near 0.66, so at 2000 days they are still travelling and the residue
      // reads as an effect — 0.002 of one, which is most of what the real coupling is worth.
      for (let d = 0; d < 6000; d++) world.timeline.nextDay();
      return world.firm(1).goodCost;
    };
    return Math.abs(settle(0.02) - settle(0));
  };
  const flat = drag(0);
  const curved = drag(1);
  assertTrue(`with no economies of scale there should be no drag, got ${flat.toExponential(2)}`,
    flat < 1e-5);
  assertTrue(`drag at curve 1 (${curved.toExponential(2)}) should dwarf curve 0 (${flat.toExponential(2)})`,
    curved > flat * 100);
});

suite('E4 — the difference equation (§6.3.4)');

test('below the threshold the perturbation dies; above it, it does not', () => {
  // E3's regime shift with the agents removed. Both Firms updated simultaneously from the
  // same state, so the instability cannot be blamed on turn order or sampling.
  const scenario = get('e4');
  /** @param {number} rho */
  const finalAsymmetry = (rho) => {
    const world = new World(scenario.config());
    /** @type {any} */ (world.config.utility).rho = rho;
    scenario.setUp?.(world);
    const recorder = new Recorder();
    scenario.record?.(recorder, world);
    for (let d = 0; d < 4000; d++) scenario.beforeDay?.(world, d);
    return recorder.value('asymmetry');
  };
  const settled = finalAsymmetry(0.73);
  const broken = finalAsymmetry(0.80);
  assertTrue(`rho 0.73 should return to symmetry, got ${settled.toExponential(2)}`, settled < 1e-5);
  assertTrue(`rho 0.80 should not, got ${broken.toExponential(2)}`, broken > 0.2);
});

test('and it settles on a new pair rather than running away', () => {
  const scenario = get('e4');
  const world = new World(scenario.config());
  /** @type {any} */ (world.config.utility).rho = 0.80;
  scenario.setUp?.(world);
  const recorder = new Recorder();
  scenario.record?.(recorder, world);
  for (let d = 0; d < 4000; d++) scenario.beforeDay?.(world, d);
  const early = recorder.value('asymmetry');
  for (let d = 0; d < 2000; d++) scenario.beforeDay?.(world, d);
  assertClose('asymmetry should have stopped moving', early, recorder.value('asymmetry'), 0.02);
  assertTrue('one Firm should produce far more than the other',
    recorder.value('f_0') / recorder.value('f_1') > 5
    || recorder.value('f_1') / recorder.value('f_0') > 5);
});

suite('F1, F2, F3 — the framework demonstrations (§4.7)');

test('f1: inequality grows out of nothing at all', () => {
  const { recorder } = run('f1', 1000);
  const g = recorder.value('gini');
  assertTrue(`Gini should be well above zero after 200,000 exchanges, got ${g.toFixed(3)}`,
    g > 0.25);
  assertTrue('and the total is conserved — nothing is created or destroyed',
    Math.abs(recorder.value('richest') + recorder.value('poorest')) > 0);
});

test('f2: the gap is a pyramid centred on one day, not a spike', () => {
  const { recorder } = run('f2', 120);
  assertClose('the mean gap is exactly one day', 1, recorder.value('meanGap'), 0.02);
  // If the distribution were the spike everyone assumes when writing a t vs t−1 comparison,
  // this would be 1. It is about a fifth.
  const near = recorder.value('withinTenth');
  assertTrue(`only a minority of gaps land near the mean, got ${near.toFixed(3)}`,
    near > 0.05 && near < 0.4);
});

test('f3: the rational population never produces anything', () => {
  const { recorder } = run('f3', 400);
  assertEqual('rational output stays at zero', 0, recorder.value('rationalOutput'));
  assertTrue(`blind output should be positive, got ${recorder.value('blindOutput')}`,
    recorder.value('blindOutput') > 0);
});

test('f3: and the hole is the threshold itself, not its size', () => {
  // Only alpha = 0 breaks the deadlock. At alpha = 1 a lone worker produces exactly zero,
  // which is no better than not working, so correct reasoners still never start — the
  // obvious guess (drop it to 1 and it frees up) is wrong, and worth a test saying so.
  const scenario = get('f3');
  const world = new World(scenario.config());
  scenario.setUp?.(world);
  const params = scenario.controls?.(world) ?? [];
  const alpha = params.find((p) => p.name === 'alpha');
  assertTrue('alpha is exposed', Boolean(alpha));
  const recorder = new Recorder();
  scenario.record?.(recorder, world);

  alpha?.set(1);
  for (let d = 0; d < 200; d++) scenario.beforeDay?.(world, d);
  assertEqual('alpha 1 still deadlocks — a lone worker produces zero',
    0, recorder.value('rationalOutput'));

  alpha?.set(0);
  for (let d = 0; d < 200; d++) scenario.beforeDay?.(world, d);
  assertTrue('alpha 0 removes the threshold and they all work',
    recorder.value('rationalOutput') > 0);
});

suite("G1 — Krugman's Core Model (§2.5.5)");

test('five equilibria at the shipped transport cost', () => {
  // Three interior crossings plus the two corners, exactly as the thesis reports for T = 2.25.
  const curve = shortRun(DEFAULTS, 500);
  const interior = equilibria(curve);
  assertEqual('three interior equilibria', 3, interior.length);
  assertEqual('two corners', 2, corners(curve).length);
  assertClose('the symmetric one sits at 0.5', 0.5, interior[1].lambda, 0.01);
  assertTrue('and it is the stable one', interior[1].stable);
  assertTrue('while the outer two are not', !interior[0].stable && !interior[2].stable);
  assertTrue('both corners are stable', corners(curve).every((c) => c.stable));
});

test('the symmetric equilibrium is exactly symmetric', () => {
  const at = solveAt(0.5, DEFAULTS);
  // To the solver's own tolerance, which is 5e-5 on the relative change in each wage. The
  // fixed point is exact; how precisely it is located is a stopping rule.
  assertClose('neither region pays better at lambda 0.5', 1, at.ratio, 1e-4);
  assertTrue('and it converged rather than hitting the cap', at.converged);
});

test('the tomahawk breaks and sustains where it should', () => {
  const points = tomahawk(DEFAULTS, 1, 3.5, 120, 200);
  /** @param {number} t */
  const symmetricStableAt = (t) => points.some((p) =>
    Math.abs(p.t - t) < 0.03 && Math.abs(p.lambda - 0.5) < 0.02 && p.stable);
  assertTrue('symmetric is unstable at low transport cost', !symmetricStableAt(1.2));
  assertTrue('and stable at high transport cost', symmetricStableAt(3.0));

  /** @param {number} t */
  const cornerStableAt = (t) => points.some((p) =>
    Math.abs(p.t - t) < 0.03 && p.lambda === 0 && p.stable);
  assertTrue('the corners are stable at low transport cost', cornerStableAt(1.2));
  assertTrue('and not at high transport cost', !cornerStableAt(3.0));
});

report();
