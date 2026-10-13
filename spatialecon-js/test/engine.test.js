// @ts-check

/**
 * The engine: randomness, geography, agent construction, recording.
 *
 * Mostly structural rather than economic, but two of these guard real traps that already
 * cost time in the Java build — cost propagation to Firms (CODE_ANATOMY §7.11) and the
 * phase ordering of the day loop (§4.7.4).
 */

import { personBundle } from '../src/behaviour.js';
import { AS_THESIS_STATES, ORIGINAL, defineModel, parameter } from '../src/config.js';
import { ces, smoothEoS } from '../src/econ.js';
import { MEAN_SD, MIN_MAX, PER_ACTOR, RATIO_GTE_ONE, Recorder } from '../src/record.js';
import { Rng } from '../src/rng.js';
import {
  LINE, PLANE, POINT, TWO_REGION, centre, confine, distance, evenlyAlongX, pt, randomly,
  splitBetweenRegions, twoPointsApart,
} from '../src/space.js';
import { World } from '../src/world.js';
import { assertClose, assertEqual, assertTrue, report, suite, test } from './tiny.js';

const EPS = 1e-9;

/** A minimal runnable config. @param {object} [overrides] */
function config(overrides = {}) {
  return defineModel({
    utility: ces(0.5),
    production: smoothEoS(),
    ...overrides,
  });
}

// --- Rng -----------------------------------------------------------------------------

suite('Rng');

test('same seed gives the same run; a reroll gives a different one', () => {
  const a = new Rng(12), b = new Rng(12), c = new Rng(13);
  const drawA = [], drawB = [], drawC = [];
  for (let i = 0; i < 50; i++) {
    drawA.push(a.nextDouble());
    drawB.push(b.nextDouble());
    drawC.push(c.nextDouble());
  }
  assertTrue('same seed reproduces exactly', drawA.every((v, i) => v === drawB[i]));
  assertTrue('a different seed does not', drawA.some((v, i) => v !== drawC[i]));
});

test('draws stay in [0, 1)', () => {
  const rng = new Rng(1);
  for (let i = 0; i < 10000; i++) {
    const v = rng.nextDouble();
    assertTrue(`draw ${v} out of range`, v >= 0 && v < 1);
  }
});

test('jiggle is a signed magnitude, not a fixed step', () => {
  // The distinction that made the Firm location crawl too effective when got wrong:
  // +-magnitude/2, centred on zero, so a fresh direction is often much smaller than
  // nominal. CODE_ANATOMY §5.6.
  const rng = new Rng(5);
  let min = Infinity, max = -Infinity, sum = 0;
  const n = 20000;
  for (let i = 0; i < n; i++) {
    const v = rng.jiggle(0.025);
    min = Math.min(min, v);
    max = Math.max(max, v);
    sum += v;
  }
  assertTrue(`min ${min} should be >= -0.0125`, min >= -0.0125);
  assertTrue(`max ${max} should be < 0.0125`, max < 0.0125);
  assertClose('mean should be near zero', 0, sum / n, 1e-3);
});

test('shuffle produces a permutation, not a resample', () => {
  const rng = new Rng(3);
  const list = Array.from({ length: 200 }, (_, i) => i);
  rng.shuffle(list);
  assertEqual('length preserved', 200, list.length);
  assertEqual('all members present', 200, new Set(list).size);
  assertTrue('order actually changed', list.some((v, i) => v !== i));
});

// --- Space ---------------------------------------------------------------------------

suite('Space');

test('POINT space has no distance at all', () => {
  const s = { kind: POINT, width: 1 };
  assertClose('always zero', 0, distance(s, pt(0, 0), pt(5, 5)), EPS);
});

test('confine clamps rather than wraps', () => {
  // The original called the 2-D case a torus, but wrapping was disabled in every published
  // run. Restoring it would be a new feature, not a fix.
  const plane = { kind: PLANE, width: 1 };
  const p = confine(plane, pt(1.5, -0.5));
  assertClose('x clamped', 1, p.x, EPS);
  assertClose('y clamped', 0, p.y, EPS);

  const line = { kind: LINE, width: 1 };
  assertClose('y flattened on a line', 0, confine(line, pt(0.5, 0.9)).y, EPS);
});

test('two regions sit either side of the origin, width apart', () => {
  const s = { kind: TWO_REGION, width: 1 };
  assertClose('left region', -0.5, confine(s, pt(-0.2, 0)).x, EPS);
  assertClose('right region', 0.5, confine(s, pt(0.3, 0)).x, EPS);
  assertClose('separation is the width', 1.0, distance(s, pt(-0.5, 0), pt(0.5, 0)), EPS);
});

test('centre is where the monocentric Firm goes', () => {
  assertClose('plane', 0.5, centre({ kind: PLANE, width: 1 }).x, EPS);
  assertClose('plane, width 2', 1.0, centre({ kind: PLANE, width: 2 }).y, EPS);
  assertClose('line', 0.5, centre({ kind: LINE, width: 1 }).x, EPS);
});

test('placements put actors where they say', () => {
  const space = { kind: LINE, width: 1 };
  const rng = new Rng(1);
  const make = (/** @type {number} */ n) =>
    Array.from({ length: n }, () => ({ position: pt(0, 0), placeAt(/** @type {any} */ p) { this.position = p; } }));

  const line = make(5);
  evenlyAlongX(0, 1)(line, space, rng);
  assertClose('first at 0', 0, line[0].position.x, EPS);
  assertClose('last at width', 1, line[4].position.x, EPS);
  assertClose('middle at 0.5', 0.5, line[2].position.x, EPS);

  const pair = make(2);
  twoPointsApart(1.0)(pair, { kind: PLANE, width: 1 }, rng);
  assertClose('separated by 1', 1.0, Math.abs(pair[0].position.x - pair[1].position.x), EPS);

  const regions = make(4);
  splitBetweenRegions(0.5)(regions, { kind: TWO_REGION, width: 1 }, rng);
  assertEqual('half in region 0', 2, regions.filter((a) => a.position.x < 0).length);
});

// --- World ---------------------------------------------------------------------------

suite('World');

test('ranging multiplies a base cost by Firm index + 1', () => {
  // How §5.4.6 gets delivery costs of 2 and 4 from a single base of 2, and how §5.4.5 gets
  // four wage bands from a base of 1. The headline parameter is a base, not what any
  // particular Firm sees.
  const w = new World(config({ firmCount: 3, deliveryCost: 2, rangeDelivery: true }));
  assertClose('firm 0', 2, w.firm(0).deliveryCost, EPS);
  assertClose('firm 1', 4, w.firm(1).deliveryCost, EPS);
  assertClose('firm 2', 6, w.firm(2).deliveryCost, EPS);
});

test('REGRESSION: a live cost change actually reaches the Firms', () => {
  // Firms hold their own copy of every cost -- they must, since ranging makes them differ.
  // Setting the config alone therefore changes nothing, silently. In the Java this
  // disabled every distance-cost control across two build stages and flattened B2's double
  // dip into a straight line. CODE_ANATOMY §7.11.
  const w = new World(config({ firmCount: 2, deliveryCost: 1, rangeDelivery: true }));

  w.config.deliveryCost = 99; // the wrong way round: config only
  assertClose('poking the config does nothing on its own', 1, w.firm(0).deliveryCost, EPS);

  w.setDeliveryCost(5); // the choke point
  assertClose('firm 0 sees it', 5, w.firm(0).deliveryCost, EPS);
  assertClose('firm 1 sees it, ranged', 10, w.firm(1).deliveryCost, EPS);
});

test('the bound Parameter goes through the choke point too', () => {
  const w = new World(config({ firmCount: 2, deliveryCost: 1, rangeDelivery: true }));
  w.deliveryCostParam().set(3);
  assertClose('firm 1 ranged from the parameter', 6, w.firm(1).deliveryCost, EPS);
});

test('the ranging step spaces the levels, and its default is the original', () => {
  // H1's wage-gap dial. The original only ever stepped by one — ×1, ×2, ×3 — so a step of 1
  // must reproduce every existing ranged model exactly.
  const original = new World(config({ firmCount: 3, wage: 1, rangeWage: true }));
  assertClose('step 1, firm 2 at ×3', 3, original.firm(2).wage, EPS);

  const half = new World(config({ firmCount: 3, wage: 1, rangeWage: true, rangeStep: 0.5 }));
  assertClose('step 0.5, firm 1 at ×1.5', 1.5, half.firm(1).wage, EPS);
  assertClose('step 0.5, firm 2 at ×2', 2, half.firm(2).wage, EPS);

  half.setRangeStep(0); // through the choke point, like every other live cost change
  assertTrue('step 0 puts every Firm on the base', half.firms.every((f) => Math.abs(f.wage - 1) < EPS));
});

test('the tie closes the channels it names, and only those', () => {
  // One Person midway between two Firms, the second paying twice as much. Which Firm employs
  // them shows whether the labour channel is open; how many they buy from shows the goods one.
  /** @param {'both' | 'work' | 'goods'} tie */
  const oneDay = (tie) => {
    const w = new World(config({
      firmCount: 2, peopleCount: 1, tie,
      placeFirms: twoPointsApart(0.4),
      placePeople: (/** @type {any[]} */ actors) => { for (const a of actors) a.placeAt(pt(0.5, 0.5)); },
      wage: 1, rangeWage: true, commuteCost: 0.1, deliveryCost: 0.1,
      personBehaviour: personBundle(),
    }));
    w.person(0).assignedFirm = w.firm(0);
    w.timeline.nextDay();
    return {
      sellers: w.person(0).lastSellers.length,
      employer: w.firms.findIndex((f) => f.contributedTime > 0),
    };
  };

  const both = oneDay('both');
  assertEqual("'both' (C6): works for its own Firm", 0, both.employer);
  assertEqual("'both': buys only from it", 1, both.sellers);

  const work = oneDay('work');
  assertEqual("'work' (H1): still works for its own Firm, though the other pays double", 0, work.employer);
  assertEqual("'work': buys from both", 2, work.sellers);

  const goods = oneDay('goods');
  assertEqual("'goods': free to take the better wage", 1, goods.employer);
  assertEqual("'goods': buys only from its own Firm", 1, goods.sellers);
});

test('randomised initial prices land in range and differ', () => {
  // Not cosmetic: from a uniform 3.0 the transmission belt runs away and never recovers,
  // while the thesis' 0.1-1.5 settles cleanly. CODE_ANATOMY §7.9.
  const w = new World(config({ firmCount: 20, initialPriceRange: [0.1, 1.5] }));
  const prices = w.firms.map((f) => f.goodCost);
  assertTrue('all in range', prices.every((p) => p >= 0.1 && p < 1.5));
  assertTrue('and they differ', new Set(prices).size > 1);
  assertTrue('lastFallingPrice seeded', w.firms.every((f) => f.lastFallingPrice === f.goodCost));
});

test('density weight tapers linearly from 1 to 0 across the radius', () => {
  const w = new World(config({
    spaceKind: PLANE, width: 1, peopleCount: 2, firmCount: 0,
    densityCost: 1, densityRadius: 0.1,
  }));
  w.people[0].placeAt(pt(0.5, 0.5));

  // Through the World, so the spatial index tracks the move.
  w.movePerson(w.people[1], pt(0.5, 0.5));
  assertClose('co-located weighs 1 before the divisor',
    1 / w.config.densityDivisor(), w.rawDensityAt(pt(0.5, 0.5), w.people[0]), EPS);

  w.movePerson(w.people[1], pt(0.55, 0.5)); // half the radius away
  assertClose('half a radius away weighs 0.5',
    0.5 / w.config.densityDivisor(), w.rawDensityAt(pt(0.5, 0.5), w.people[0]), EPS);

  w.movePerson(w.people[1], pt(0.7, 0.5)); // beyond the radius
  assertClose('beyond the radius contributes nothing',
    0, w.rawDensityAt(pt(0.5, 0.5), w.people[0]), EPS);
});

test('the spatial index cannot silently go stale', () => {
  // The index makes the density query ~9x cheaper and is what lets C1 run on a canvas, but
  // an index needs maintaining. `movePerson` keeps it right; anything that goes round it
  // (a scenario's setUp, a test) is repaired at the start of the next day.
  const w = new World(config({
    spaceKind: PLANE, width: 1, peopleCount: 2, firmCount: 0,
    densityCost: 1, densityRadius: 0.1,
  }));
  w.people[0].placeAt(pt(0.5, 0.5));
  w.people[1].placeAt(pt(0.9, 0.9)); // out of band: index still thinks it is elsewhere

  w.timeline.nextDay();
  assertClose('reindexed, so the distant Person is correctly absent',
    0, w.rawDensityAt(pt(0.5, 0.5), w.people[0]), EPS);

  w.people[1].placeAt(pt(0.5, 0.5));
  w.timeline.nextDay();
  assertClose('and correctly present once it moves in',
    1 / w.config.densityDivisor(), w.rawDensityAt(pt(0.5, 0.5), w.people[0]), EPS);
});

test('the index finds exactly what a full scan would', () => {
  // The whole claim of the optimisation: same answer, less work.
  const w = new World(config({
    spaceKind: PLANE, width: 2, peopleCount: 400, firmCount: 1,
    densityCost: 10, densityRadius: 1 / 16, placePeople: randomly(),
  }));
  const radius = w.config.densityRadius;
  for (let trial = 0; trial < 40; trial++) {
    const probe = pt(w.rng.upTo(2), w.rng.upTo(2));
    let bruteWeight = 0, bruteCount = 0;
    for (const other of w.people) {
      const d = Math.hypot(probe.x - other.position.x, probe.y - other.position.y);
      if (d < radius) { bruteWeight += 1 - d / radius; bruteCount++; }
    }
    const indexed = w.densityAndNeighboursAt(probe, null);
    assertClose('same weight', bruteWeight / w.config.densityDivisor(), indexed.raw, 1e-12);
    assertEqual('same neighbour count', bruteCount, indexed.neighbours);
  }
});

test('Quirks decides the density divisor, and defaults to the original', () => {
  const original = new World(config({ peopleCount: 500, firmCount: 1, quirks: ORIGINAL }));
  assertEqual('original divides by Firms + People', 501, original.config.densityDivisor());

  const asStated = new World(config({ peopleCount: 500, firmCount: 1, quirks: AS_THESIS_STATES }));
  assertEqual('the thesis text says People alone', 500, asStated.config.densityDivisor());
});

test('the day loop runs People fully before any Firm', () => {
  // The §4.7.4 two-bin fix, made structural. Permuting both in one pot leaves a Firm
  // comparing revenue against "yesterday" with a variable number of People in between.
  /** @type {string[]} */
  const order = [];
  const w = new World(config({
    peopleCount: 3, firmCount: 3,
    personBehaviour: { act: () => order.push('P'), describe: () => 'test' },
    firmBehaviour: { act: () => order.push('F'), describe: () => 'test' },
  }));
  w.timeline.nextDay();
  assertEqual('one turn each', 'PPPFFF', order.join(''));
  assertEqual('day advanced', 1, w.day);
});

test('mean nearest neighbour collapses as agents pile up', () => {
  // The measure behind the black hole of §5.3.2.
  const w = new World(config({ spaceKind: PLANE, width: 1, peopleCount: 4, firmCount: 0 }));
  const corners = [pt(0, 0), pt(1, 0), pt(0, 1), pt(1, 1)];
  w.people.forEach((p, i) => p.placeAt(corners[i]));
  assertClose('spread out', 1.0, w.meanNearestNeighbour(w.people), EPS);

  w.people.forEach((p) => p.placeAt(pt(0.5, 0.5)));
  assertClose('collapsed', 0.0, w.meanNearestNeighbour(w.people), EPS);
});

// --- Parameter -----------------------------------------------------------------------

suite('Parameter');

test('clamps to its range and normalises both ways', () => {
  let value = 5;
  const p = parameter({
    name: 't', min: 0, max: 10, get: () => value, set: (v) => { value = v; },
  });
  p.set(20);
  assertClose('clamped high', 10, p.get(), EPS);
  p.set(-4);
  assertClose('clamped low', 0, p.get(), EPS);
  p.setNormalised(0.25);
  assertClose('set from a 0-1 position', 2.5, p.get(), EPS);
  assertClose('and read back', 0.25, p.getNormalised(), EPS);
  p.nudge(1);
  assertClose('nudged', 3.5, p.get(), EPS);
});

// --- Recorder ------------------------------------------------------------------------

suite('Recorder');

test('aggregations reduce as advertised', () => {
  assertClose('mean', 2, MEAN_SD.reduce([1, 2, 3])[0], EPS);
  assertClose('population sd', Math.sqrt(2 / 3), MEAN_SD.reduce([1, 2, 3])[1], EPS);
  assertEqual('min/max', '1,3', MIN_MAX.reduce([2, 1, 3]).join(','));
  assertClose('ratio is always >= 1', 4, RATIO_GTE_ONE.reduce([0.5, 2])[0], EPS);
  assertEqual('per actor keeps everything', 3, PER_ACTOR.reduce([1, 2, 3]).length);
});

test('CSV has a day column then each series side by side', () => {
  const people = [{ u: 1 }, { u: 3 }];
  const r = new Recorder();
  r.global('rho', () => 0.5);
  r.aggregate('utility', people, (p) => p.u, MEAN_SD);
  r.perActor('each', people, (p) => p.u);

  r.sample(1);
  r.sample(2);

  const lines = r.toCsv().trim().split('\n');
  assertEqual('header', 'day,rho_mean,utility_mean,utility_sd,each_0,each_1', lines[0]);
  assertEqual('two rows sampled', 3, lines.length);
  assertTrue('values present', lines[1].startsWith('1,0.500000,2,1,1,3'));
});

test('sampleEvery thins the record', () => {
  const r = new Recorder().sampleEvery(10);
  r.global('x', () => 1);
  for (let day = 0; day < 100; day++) r.sample(day);
  assertEqual('sampled every 10th day', 10, r.get('x').size);
});

test('a rolling window keeps only the last n samples', () => {
  const r = new Recorder();
  r.rolling('x', [{ v: 1 }], (a) => a.v, MEAN_SD, 5);
  for (let day = 0; day < 50; day++) r.sample(day);
  assertEqual('window held', 5, r.get('x').size);
  assertEqual('and it is the recent end', 49, r.get('x').days[4]);
});

report();
