// @ts-check

/**
 * Tier 2: the models, checked by published claim rather than by trace.
 *
 * These assert what the thesis says happens, in windows wide enough to survive a reseed
 * (BUILD_PLANNER §6). They are the real verification of the port — tier 1 only proves the
 * arithmetic, and everything interesting in this thesis is emergent.
 *
 * Day counts are cut where a claim is already settled, to keep the suite near a minute.
 * Where a number from `spatialecon-java` is quoted, it is the independently-built Java
 * result, itself checked against the thesis figure.
 */

import { personBundle } from '../src/behaviour.js';
import { World } from '../src/world.js';
import { b3 } from '../src/scenarios/b3.js';
import { c1 } from '../src/scenarios/c1.js';
import { c6, c6c } from '../src/scenarios/c6.js';
import { d1a, d1b, d1c, d1d } from '../src/scenarios/d1.js';
import { playBatch, playGame } from '../src/scenarios/d2.js';
import { monocentric } from '../src/scenarios/monocentric.js';
import { at } from '../src/space.js';
import { assertClose, assertTrue, report, suite, test } from './tiny.js';

/**
 * Build and run a scenario headless.
 * @param {import('../src/scenario.js').Scenario} scenario @param {number} days
 * @param {number} [seed]
 */
function run(scenario, days, seed) {
  const config = scenario.config();
  if (seed !== undefined) config.seed = seed;
  const w = new World(config);
  scenario.setUp?.(w);
  for (let d = 0; d < days; d++) {
    scenario.beforeDay?.(w, d);
    w.timeline.nextDay();
  }
  return w;
}

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

/** @param {number[]} v */
const mean = (v) => v.reduce((a, b) => a + b, 0) / v.length;

// --- B: space and love of variety ----------------------------------------------------

suite('B — the black hole (§5.3.2)');

test('with nothing opposing distance costs, space collapses', () => {
  const w = run(b3, 600);
  const peopleNN = w.meanNearestNeighbour(w.people);
  const firmNN = w.meanNearestNeighbour(w.firms);
  // Random start on a unit plane gives mean nearest-neighbour around 0.1 for 20 People.
  assertTrue(`People should pile up, nearest-neighbour was ${peopleNN}`, peopleNN < 0.02);
  assertTrue(`Firms too, was ${firmNN}`, firmNN < 0.25);
});

test('People collapse faster than Firms — information beats groping', () => {
  // The asymmetry is the point: a Person evaluates candidate locations and can see which is
  // better; a Firm moves, waits, and reads one number. Same objective, different competence.
  const early = run(b3, 25);
  const peopleEarly = early.meanNearestNeighbour(early.people);
  const firmsEarly = early.meanNearestNeighbour(early.firms);
  const start = new World(b3.config());
  const peopleStart = start.meanNearestNeighbour(start.people);
  const firmsStart = start.meanNearestNeighbour(start.firms);

  const peopleProgress = 1 - peopleEarly / peopleStart;
  const firmProgress = 1 - firmsEarly / firmsStart;
  assertTrue(
    `People (${(peopleProgress * 100).toFixed(0)}%) should be further along than Firms (${(firmProgress * 100).toFixed(0)}%)`,
    peopleProgress > firmProgress,
  );
});

// --- C: the density cost -------------------------------------------------------------

suite('C — monocentric settlement (§5.4)');

test('a settlement forms and utility equalises across it', () => {
  const w = run(c1, 800);
  const utilities = w.people.map((p) => p.utility);
  const distances = w.people.map((p) => w.distance(p, w.firm(0)));
  const densities = w.people.map((p) => p.densityCost);

  // Java: 0.898. Thesis §5.4.1: "close to 0.9".
  assertClose('mean utility', 0.9, mean(utilities), 0.05);

  // Java: -0.986. The density profile is what makes equalisation possible.
  assertTrue(`density should fall with distance, corr was ${correlation(distances, densities)}`,
    correlation(distances, densities) < -0.95);

  // People are paying for proximity, not fleeing it.
  assertTrue(`mean density should be well above zero, was ${mean(densities)}`, mean(densities) > 0.01);

  // Flat-ish, but not perfectly: the fringe of CODE_ANATOMY §5.3.
  const utilityCorr = correlation(distances, utilities);
  assertTrue(`utility should be near-flat across distance, corr was ${utilityCorr}`,
    utilityCorr > -0.6);
});

test('the settlement reaches spatial equilibrium and stops moving', () => {
  const w = run(c1, 800);
  assertTrue(`movement should be near zero, was ${w.meanPersonMovement()}`,
    w.meanPersonMovement() < 1e-4);
});

test('the residual gradient is the fringe, and it shrinks with population', () => {
  // The decisive test that C1's imperfect flatness is a finite-population boundary effect
  // and not a bug: density cannot go below zero, so equalising is only possible out to the
  // cutoff. A boundary effect must wash out as numbers grow, and it does.
  const fringeShare = (/** @type {number} */ people) => {
    const w = new World(monocentric(10, { peopleCount: people }));
    for (let d = 0; d < 600; d++) w.timeline.nextDay();
    return w.people.filter((p) => p.densityCost < 1e-12).length / people;
  };
  const small = fringeShare(125);
  const large = fringeShare(500);
  assertTrue(`fringe should shrink with population: ${(small * 100).toFixed(1)}% -> ${(large * 100).toFixed(1)}%`,
    large < small);
});

test('C8: raising the density cost breaks utility equilibrium', () => {
  // Thesis §5.4.7, the model surprising its author. Java: corr goes -0.39 -> -0.96 -> -0.97
  // as the multiplier steps 10 -> 100 -> 500.
  const settleAt = (/** @type {number} */ densityCost) => {
    const w = new World(monocentric(densityCost));
    for (let d = 0; d < 900; d++) w.timeline.nextDay();
    const distances = w.people.map((p) => w.distance(p, w.firm(0)));
    return {
      corr: correlation(distances, w.people.map((p) => p.utility)),
      density: mean(w.people.map((p) => p.densityCost)),
      distance: mean(distances),
    };
  };
  const low = settleAt(10);
  const high = settleAt(500);

  assertTrue(`at 500 utility should fall steeply with distance, corr was ${high.corr}`,
    high.corr < -0.9);
  assertTrue(`and much more steeply than at 10 (${low.corr} vs ${high.corr})`,
    high.corr < low.corr);
  assertTrue(`at 500 nobody accepts density: ${high.density} vs ${low.density}`,
    high.density < low.density);
  assertTrue(`and the settlement spreads out: ${low.distance} -> ${high.distance}`,
    high.distance > low.distance);
});

suite('C6 — von Thunen without landlords (§5.4.5)');

test('a non-spatial advantage buys proximity', () => {
  // Wages 0.5/1.0/1.5/2.0 across the four Firms. Java: best-off settle closest, 0.10 vs 0.47.
  const w = run(c6, 900);
  const groupDistance = (/** @type {number} */ g) =>
    mean(w.people.filter((p) => p.assignedFirm === w.firm(g)).map((p) => w.distance(p, w.firm(0))));
  const poorest = groupDistance(0); // wage 0.5
  const richest = groupDistance(3); // wage 2.0
  assertTrue(`best-paid should settle closest in: ${richest.toFixed(3)} vs ${poorest.toFixed(3)}`,
    richest < poorest);
});

test('a spatial advantage buys distance — the same mechanism, reversed', () => {
  // Commute costs 0.25/0.5/0.75/1.0. Java: cheapest commute settles furthest out.
  const w = run(c6c, 900);
  const groupDistance = (/** @type {number} */ g) =>
    mean(w.people.filter((p) => p.assignedFirm === w.firm(g)).map((p) => w.distance(p, w.firm(0))));
  const cheapest = groupDistance(0); // commute 0.25
  const dearest = groupDistance(3); // commute 1.0
  assertTrue(`cheapest commute should settle furthest out: ${cheapest.toFixed(3)} vs ${dearest.toFixed(3)}`,
    cheapest > dearest);
});

// --- D: Firms groping ----------------------------------------------------------------

suite('D — two Firms on a line (§5.5.1)');

test('substitutes plus high distance costs give the Hotelling split', () => {
  // The headline of §5.5.1. Java: separation 0.343 at rho=0.99, cost 20; 0.065 at rho=0.1,
  // cost 1. Averaged over the run, because the Firms never settle -- the crawl always
  // perturbs, so a Firm that has found a good spot keeps testing and giving ground.
  const meanSeparation = (/** @type {import('../src/scenario.js').Scenario} */ s, /** @type {number} */ days) => {
    const w = new World(s.config());
    let total = 0;
    for (let d = 0; d < days; d++) {
      w.timeline.nextDay();
      total += Math.abs(w.firm(0).position.x - w.firm(1).position.x);
    }
    return total / days;
  };
  const tight = meanSeparation(d1a, 3000);  // rho 0.1,  cost 1
  const split = meanSeparation(d1d, 3000);  // rho 0.99, cost 20

  assertTrue(`high variety + near market should keep Firms together, was ${tight.toFixed(3)}`,
    tight < 0.2);
  assertTrue(`substitutes + far market should split them, was ${split.toFixed(3)}`,
    split > 0.25);
  assertTrue('and the split case should be much wider than the tight one', split > tight * 2);
});

test('rho = 0.99, not 1.0 — the branch the thesis never ran', () => {
  // At rho exactly 1 the original computes `price * budget`, so moving AWAY from a buyer
  // increases what they buy. It never mattered, because rho was clamped to 0.99 everywhere
  // it could be set. Using 1.0 gives a qualitatively different answer, so the scenarios use
  // 0.99. CODE_ANATOMY §7.1.
  assertTrue('d1b and d1d must not use rho = 1.0',
    !d1b.title.includes('1.00') && !d1d.title.includes('1.00'));
  assertTrue('they should use 0.99', d1d.title.includes('0.99') && d1b.title.includes('0.99'));
  assertTrue('and the high-variety cases 0.10', d1a.title.includes('0.10') && d1c.title.includes('0.10'));
});

suite('D2 — the DMR game (§5.5.2)');

test('a lone Firm finds its market easily', () => {
  // Nothing else moves, so every revenue change really is the Firm's own doing. The datum
  // means what the Firm assumes it means.
  const days = playGame(0.5, 1000, 0);
  assertTrue(`two Firms should still manage it without noise, took ${days} days`, days < 500);
});

test('noise makes the game harder', () => {
  // The verified half of §5.5.2. Java: 15 -> 17 -> 32 mean days at noise 0 / 0.1 / 0.5.
  const clean = playBatch(0.5, 0, 8).meanDays;
  const noisy = playBatch(0.5, 0.5, 8).meanDays;
  assertTrue(`noise should slow the game: ${clean.toFixed(1)} -> ${noisy.toFixed(1)} days`,
    noisy > clean);
});

test('OPEN: rho does NOT reverse the difficulty, against §5.5.2', () => {
  // The thesis reports that under noise, LOW rho (high love of variety) is markedly harder,
  // because each Firm's move then has a larger external effect on the other's demand. Two
  // independent reimplementations disagree: rho makes little difference and low rho is if
  // anything easier.
  //
  // This asserts the OBSERVED direction on purpose. If the discrepancy is ever resolved in
  // the thesis' favour, this test fails loudly rather than the finding passing unnoticed.
  // CODE_ANATOMY §7.12.
  const lowRho = playBatch(0.1, 0.5, 8).meanDays;
  const highRho = playBatch(0.9, 0.5, 8).meanDays;
  assertTrue(
    `observed: low rho is not markedly harder (low ${lowRho.toFixed(1)} vs high ${highRho.toFixed(1)} days). `
      + 'If this now fails, the thesis claim may have been reproduced -- investigate, do not just widen it.',
    lowRho < highRho * 2,
  );
});

// --- the Bundle cycle itself ---------------------------------------------------------

suite('The Bundle cycle');

test('a Person with nowhere affordable records zero utility, not negative', () => {
  // The viability rule: a candidate whose density cost exceeds the wage is discarded rather
  // than scored negative. That single rule produces C8's dispersal.
  //
  // Forced by piling 50 People onto one point and pinning them there, so the crowding
  // cannot be escaped. Scattering them on an open plane does not test this: at radius 1/16
  // most People have no neighbours at all, and a huge multiplier times zero density is
  // still zero.
  const w = new World(monocentric(1e6, {
    peopleCount: 50, placePeople: at(1, 1), peopleMobile: false,
  }));
  w.timeline.nextDay();
  assertTrue('utilities must never go negative', w.people.every((p) => p.utility >= 0));
  assertTrue('crushing, inescapable density leaves nothing affordable',
    w.people.every((p) => p.utility === 0));

  // And the same crowd at a survivable multiplier does buy something, so the zero above is
  // the rule firing rather than the model being broken.
  const bearable = new World(monocentric(0.001, {
    peopleCount: 50, placePeople: at(1, 1), peopleMobile: false,
  }));
  bearable.timeline.nextDay();
  assertTrue('at a bearable density cost they trade normally',
    bearable.people.every((p) => p.utility > 0));
});

test('demand noise moves what is bought but not reported utility', () => {
  // People buy noisily but report utility as if they had not. Defensible -- the noise is
  // execution error, not preference error -- but it must be deliberate, and it is the
  // asymmetry model d2 turns on: Firms see noisy demand while utility plots stay clean.
  const build = (/** @type {number} */ noise) => {
    const w = new World(monocentric(10, {
      peopleCount: 60, demandNoise: noise, personBehaviour: personBundle(), seed: 5,
    }));
    for (let d = 0; d < 40; d++) w.timeline.nextDay();
    return w;
  };
  const clean = build(0);
  const noisy = build(0.5);
  assertTrue('Firm revenue should differ once demand is noisy',
    Math.abs(clean.firm(0).revenue - noisy.firm(0).revenue) > 0);
  assertTrue('utilities should still be finite and positive under noise',
    noisy.people.every((p) => p.utility >= 0 && Number.isFinite(p.utility)));
});

report();
