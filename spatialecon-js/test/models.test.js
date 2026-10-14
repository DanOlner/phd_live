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
import { APART, ENGULFED, firmSpeed, h1, sales, settlements } from '../src/scenarios/h1.js';
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

// --- H: curiosities ------------------------------------------------------------------

/**
 * Run H1 and score it the way its source video was scored (GREEN_BLOB_RED_BLOB.md §1, §5):
 * each day, where the two settlements and the two Firms are; then, over four-day windows,
 * which way each moved along the line joining the settlements.
 *
 * The chased workforce is whichever leads on utility most of the run — the video's red blob.
 * Windows below come from eight seeds at these settings, with a few points to spare.
 *
 * And what drives it (GREEN_BLOB_RED_BLOB.md §4): each workforce's utility and each Firm's
 * revenue apart against together, where the circles shop, and how fast each Firm moves —
 * and whether its revenue rose — in the run-up to each engulfment and at each parting.
 *
 * @param {(c: import('../src/config.js').ModelConfig) => void} [tweak] @param {number} [days]
 */
function scoreChase(tweak, days = 2000) {
  const config = h1.config();
  tweak?.(config);
  const w = new World(config);
  h1.setUp?.(w);
  const frames = [];
  for (let d = 0; d < days; d++) {
    w.timeline.nextDay();
    frames.push({
      s: settlements(w),
      firms: w.firms.map((f) => ({ x: f.position.x, y: f.position.y })),
      revenue: w.firms.map((f) => f.revenueLastTurn),
      sales: sales(w),
      speed: w.firms.map((_, g) => firmSpeed(w, g)),
    });
  }
  const scored = frames.slice(400); // the settlements forming
  const k = 4;
  const hi = scored.filter((f) => f.s.leader === 1).length >= scored.length / 2 ? 1 : 0;
  const lo = 1 - hi;

  let n = 0, lead = 0, away = 0, toward = 0;
  let firmAway = 0, firmToward = 0, hiMoves = 0, loMoves = 0;
  let far = 0, engulfed = 0, changes = 0, state = 0;
  for (let t = k; t < scored.length; t++) {
    const now = scored[t], then = scored[t - k];
    const a = now.s.groups, b = then.s.groups;
    const gap = Math.hypot(a[hi].x - a[lo].x, a[hi].y - a[lo].y) || 1e-9;
    const ux = (a[hi].x - a[lo].x) / gap, uy = (a[hi].y - a[lo].y) / gap;
    /** Movement from q to p along the line from the chaser to the chased.
     * @param {{x: number, y: number}} p @param {{x: number, y: number}} q */
    const along = (p, q) => (p.x - q.x) * ux + (p.y - q.y) * uy;
    /** @param {{x: number, y: number}} p @param {{x: number, y: number}} q */
    const moved = (p, q) => Math.hypot(p.x - q.x, p.y - q.y) > 1e-12;

    n++;
    if (now.s.leader === hi) lead++;
    if (along(a[hi], b[hi]) > 0) away++;
    if (along(a[lo], b[lo]) > 0) toward++;
    if (moved(now.firms[hi], then.firms[hi])) {
      hiMoves++;
      if (along(now.firms[hi], then.firms[hi]) > 0) firmAway++;
    }
    if (moved(now.firms[lo], then.firms[lo])) {
      loMoves++;
      if (along(now.firms[lo], then.firms[lo]) > 0) firmToward++;
    }

    const sep = now.s.separation;
    if (sep > 1.5) far++;
    if (sep < 0.35) engulfed++;
    const st = sep > 0.75 ? 1 : sep < 0.35 ? -1 : 0;
    if (st !== 0 && st !== state) {
      if (state !== 0) changes++;
      state = st;
    }
  }

  // What drives it. The circles are workforce 1, and Firm 1 their employer.
  /** @param {number[]} v */
  const average = (v) => v.reduce((a, x) => a + x, 0) / v.length;
  const together = scored.filter((f) => f.s.separation < 1);
  const apart = scored.filter((f) => f.s.separation > APART);
  /** @param {(f: typeof scored[number]) => number} get */
  const apartOverTogether = (get) => average(apart.map(get)) / average(together.map(get));
  /** Share of the circles' purchases made at Firm 1. @param {typeof scored[number]} f */
  const atFirm1 = (f) => f.sales[1][1] / (f.sales[0][1] + f.sales[1][1]);

  // Onsets, with hysteresis: wrapped round from outside, parted past touching from inside.
  /** @type {number[]} */
  const meetings = [];
  /** @type {number[]} */
  const partings = [];
  let wrapped = scored[0].s.separation < ENGULFED;
  for (let t = 1; t < scored.length; t++) {
    const sep = scored[t].s.separation;
    if (!wrapped && sep < ENGULFED) { meetings.push(t); wrapped = true; }
    else if (wrapped && sep > 1) { partings.push(t); wrapped = false; }
  }
  /** Mean of `get` over the days `from` to `to` either side of every onset.
   * @param {number[]} onsets @param {number} from @param {number} to @param {(t: number) => number} get */
  const around = (onsets, from, to, get) => {
    const v = [];
    for (const t of onsets) {
      if (t + from < 1 || t + to >= scored.length) continue;
      for (let j = t + from; j <= t + to; j++) v.push(get(j));
    }
    return v.length > 0 ? average(v) : NaN;
  };
  /** @param {number} f */
  const speed = (f) => (/** @type {number} */ t) => scored[t].speed[f];
  /** Whether Firm f's revenue rose on day t: the crawl's whole verdict. @param {number} f */
  const rose = (f) => (/** @type {number} */ t) => (scored[t].revenue[f] > scored[t - 1].revenue[f] ? 1 : 0);

  return {
    lead: lead / n, away: away / n, toward: toward / n,
    firmAway: hiMoves ? firmAway / hiMoves : 0, firmToward: loMoves ? firmToward / loMoves : 0,
    far: far / n, engulfed: engulfed / n, changes,
    utilityApart: [0, 1].map((g) => apartOverTogether((f) => f.s.groups[g].utility)),
    revenueApart: [0, 1].map((g) => apartOverTogether((f) => f.revenue[g])),
    circlesAtFirm1: { together: average(together.map(atFirm1)), apart: average(apart.map(atFirm1)) },
    runUp: {
      f0: around(meetings, -20, -1, speed(0)), f1: around(meetings, -20, -1, speed(1)),
      f1Rose: around(meetings, -10, -1, rose(1)),
    },
    breakaway: {
      f1: around(partings, 0, 9, speed(1)),
      f1Rose: around(partings, -5, 5, rose(1)), f0Rose: around(partings, 0, 5, rose(0)),
    },
    meetings: meetings.length,
  };
}

/** @param {number} x */
const pct = (x) => `${(100 * x).toFixed(0)}%`;
/** Neither direction favoured. @param {number} x */
const noBias = (x) => x > 0.4 && x < 0.6;

/** The default run, shared by the tests that read it. @type {ReturnType<typeof scoreChase> | null} */
let h1Baseline = null;
const baseline = () => (h1Baseline ??= scoreChase());

suite('H1 — green blob, red blob (a 2011 video)');

test('the better-paid workforce leads, gives ground, and is chased', () => {
  // The video: red moving away from green 73% of frames, green towards red 75%. Here, over
  // eight seeds: 57–68% and 57–69%, the better-paid ahead on utility every day.
  const r = baseline();
  assertTrue(`the better-paid should lead on utility throughout, did on ${pct(r.lead)}`, r.lead >= 0.95);
  assertTrue(`they should move away more often than not: ${pct(r.away)}`, r.away > 0.53);
  assertTrue(`and the others towards them: ${pct(r.toward)}`, r.toward > 0.53);
  // The mechanism, not just the motion: the chaser is the poorer workforce's Firm, courting
  // the richer customers, and its commuters follow it in.
  assertTrue(`the poorer workforce's Firm should step towards the richer: ${pct(r.firmToward)}`,
    r.firmToward > 0.54);
  assertTrue(`and the richer workforce's Firm away: ${pct(r.firmAway)}`, r.firmAway > 0.5);
});

test('and the two keep meeting and parting', () => {
  // The video: far apart 35% of frames, engulfed 11%, 20 changes of state. Eight seeds here:
  // far apart 17–43%, engulfed 6–14%, 11–22 changes in 1,600 days.
  const r = baseline();
  assertTrue(`clearly apart for a real share of the run: ${pct(r.far)}`, r.far > 0.1);
  assertTrue(`one wrapped round the other for a real share too: ${pct(r.engulfed)}`, r.engulfed > 0.03);
  assertTrue(`and back and forth repeatedly: ${r.changes} changes`, r.changes >= 6);
});

test('take away any one ingredient and the chase stops', () => {
  const equalWages = scoreChase((c) => { c.rangeStep = 0; });
  assertTrue(`equal wages: no stable leader (${pct(equalWages.lead)})`, equalWages.lead < 0.7);
  assertTrue(`equal wages: no chase (${pct(equalWages.away)} away, ${pct(equalWages.toward)} towards)`,
    noBias(equalWages.away) && noBias(equalWages.toward));

  const fixed = scoreChase((c) => { c.firmsMobile = false; });
  assertTrue(`Firms fixed: the settlements barely move (${pct(fixed.away)}, ${pct(fixed.toward)})`,
    fixed.away < 0.3 && fixed.toward < 0.3);

  const bothTied = scoreChase((c) => { c.tie = 'both'; });
  assertTrue(`both channels tied, as C6: the settlements repel (apart ${pct(bothTied.far)})`,
    bothTied.far > 0.9);
  assertTrue(`and nobody chases (${pct(bothTied.away)}, ${pct(bothTied.toward)})`,
    noBias(bothTied.away) && noBias(bothTied.toward));
});

test('the runner-up reconstruction chases harder but never parts or engulfs', () => {
  // Goods tied, work open: the wiring of the one 2011 preset that survives. The poorer
  // workforce commutes to the better-paying Firm, and the two clouds stay glued edge to edge.
  const r = scoreChase((c) => { c.tie = 'goods'; });
  assertTrue(`a harder chase: ${pct(r.toward)} towards`, r.toward > 0.7);
  assertTrue(`but never clearly apart: ${pct(r.far)}`, r.far < 0.02);
  assertTrue(`and never engulfed: ${pct(r.engulfed)}`, r.engulfed < 0.02);
});

test('rho is the dial: merged below the video, parted above it', () => {
  const tight = scoreChase((c) => { /** @type {any} */ (c.utility).rho = 0.45; });
  assertTrue(`rho 0.45: rarely apart (${pct(tight.far)}) and often engulfed (${pct(tight.engulfed)})`,
    tight.far < 0.15 && tight.engulfed > 0.1);
  const loose = scoreChase((c) => { /** @type {any} */ (c.utility).rho = 0.8; });
  assertTrue(`rho 0.8: parted most of the time (${pct(loose.far)})`, loose.far > 0.75);
});

test('the People would rather stay together, the better-paid included', () => {
  // Mean utility on days apart over days together. Eight seeds: circles 0.82–0.94, squares
  // 0.87–0.99. Meeting does crowd the circles, and their density cost rises, by less than
  // having both Firms close repays. So it is not the People who pull the two apart.
  const r = baseline();
  const [squares, circles] = r.utilityApart;
  assertTrue(`circles worse off apart: ${circles.toFixed(3)} of their utility together`, circles < 0.97);
  assertTrue(`and squares: ${squares.toFixed(3)}`, squares < 1);
});

test('the Firms want opposite things, and the circles\' custom is what they want', () => {
  // Eight seeds: Firm 1 earns 6–14% more apart, Firm 0 12–28% less. Together the circles buy
  // 51% of their goods from Firm 1, in every seed; apart, 64–82%.
  const r = baseline();
  const [firm0, firm1] = r.revenueApart;
  assertTrue(`Firm 1 earns more apart: ${firm1.toFixed(3)} of its revenue together`, firm1 > 1.03);
  assertTrue(`Firm 0 earns less: ${firm0.toFixed(3)}`, firm0 < 0.92);
  const { together, apart } = r.circlesAtFirm1;
  assertTrue(`together the circles split their custom: ${pct(together)} at Firm 1`,
    Math.abs(together - 0.5) < 0.05);
  assertTrue(`apart, most of it goes to Firm 1: ${pct(apart)}`, apart > together + 0.08);
});

test('the rhythm is each crawl reading its rival\'s moves as the verdict on its own', () => {
  // D1's attribution problem. While Firm 0 closes in, Firm 1's revenue falls whatever Firm 1
  // does, so its crawl undoes each step: it hardly moves, and Firm 0 arrives. Once Firm 0 stops
  // gaining, a step away pays at last and Firm 1 bolts, and then Firm 0's revenue is the one
  // falling regardless. Eight seeds: in the 20 days before an engulfment Firm 1 moves 0.0025–
  // 0.0042 a day against Firm 0's 0.009–0.021, its revenue rising on 9–33% of days; at a
  // parting it moves five to eleven times as fast, its revenue rising on 80–100% of days,
  // and Firm 0's on 0–30%.
  const r = baseline();
  assertTrue(`some meetings to measure: ${r.meetings}`, r.meetings >= 3);
  assertTrue(`Firm 1 near-still in the run-up (${r.runUp.f1.toFixed(4)} a day) while Firm 0 closes in (${r.runUp.f0.toFixed(4)})`,
    r.runUp.f0 > 2 * r.runUp.f1);
  assertTrue(`its revenue falling most days: rose on ${pct(r.runUp.f1Rose)}`, r.runUp.f1Rose < 0.42);
  assertTrue(`then it bolts: ${r.breakaway.f1.toFixed(4)} a day, ${(r.breakaway.f1 / r.runUp.f1).toFixed(1)} times its run-up speed`,
    r.breakaway.f1 > 3 * r.runUp.f1);
  assertTrue(`its revenue rising nearly every day: ${pct(r.breakaway.f1Rose)}`, r.breakaway.f1Rose > 0.7);
  assertTrue(`and Firm 0's falling: rose on ${pct(r.breakaway.f0Rose)}`, r.breakaway.f0Rose < 0.4);
});

test('each Firm\'s takings by workforce add up to the revenue its crawl reads', () => {
  // `sales` is booked from what each Person bought; the crawl reads the revenue `buy` banked.
  // If the two disagree, the dashed lines on the revenue strip are measuring something else.
  const w = new World(h1.config());
  h1.setUp?.(w);
  for (let d = 0; d < 30; d++) {
    w.timeline.nextDay();
    const m = sales(w);
    w.firms.forEach((firm, f) => {
      assertClose(`day ${w.day}, Firm ${f}`, firm.revenueLastTurn, m[f][0] + m[f][1], 1e-9);
    });
  }
});

test('keeping the record of where the Firms have been changes nothing about the run', () => {
  // `firmSpeed` reads a trail kept by a day-start hook. It must draw no random numbers and
  // touch no agent, or the default run — the catalogue still, the walkthrough, every window
  // above — would quietly become a different run.
  const withTrail = new World(h1.config());
  h1.setUp?.(withTrail);
  const without = new World(h1.config());
  without.people.forEach((p, i) => { p.assignedFirm = without.firm(i % without.firms.length); });
  for (let d = 0; d < 150; d++) {
    withTrail.timeline.nextDay();
    without.timeline.nextDay();
  }
  const same = [...withTrail.people, ...withTrail.firms].every((a, i) => {
    const b = [...without.people, ...without.firms][i];
    return a.position.x === b.position.x && a.position.y === b.position.y;
  });
  assertTrue('every actor in the same place after 150 days', same);
  assertTrue(`and the trail measures something: Firm 0 at ${firmSpeed(withTrail, 0)}`,
    firmSpeed(withTrail, 0) > 0);
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
