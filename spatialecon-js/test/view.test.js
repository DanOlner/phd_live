// @ts-check

/**
 * The plane view, exercised headlessly against a recording canvas stub.
 *
 * Views are the part of this project with no natural headless form, which is exactly why
 * they tend to ship untested and break silently. A ~40-line fake 2-D context is enough to
 * catch the failures that actually happen — a typo'd method, a NaN coordinate, a shape not
 * drawn at all — without a browser or a dependency.
 *
 * It cannot tell us the picture *looks* right. That still needs eyes on `model.html`.
 */

import { World } from '../src/world.js';
import { planeView } from '../src/view/plane.js';
import { lineView } from '../src/view/line.js';
import { timeSeriesView } from '../src/view/timeseries.js';
import { correlateView } from '../src/view/correlate.js';
import { chooseView } from '../src/view/choose.js';
import { equalisingCutoff, equalisingLineApplies } from '../src/view/equalise.js';
import { demandForFirm, profilesFor } from '../src/view/profiles.js';
import { Rng } from '../src/rng.js';
import { ces, constrained, hillClimb } from '../src/econ.js';
import { f4, lastDecision } from '../src/scenarios/f4.js';
import { a1 } from '../src/scenarios/a1.js';
import { a3 } from '../src/scenarios/a3.js';
import { c1 } from '../src/scenarios/c1.js';
import { d1d } from '../src/scenarios/d1.js';
import { e1 } from '../src/scenarios/e1.js';
import { e7 } from '../src/scenarios/e7.js';
import { e8 } from '../src/scenarios/e8.js';
import { monocentric } from '../src/scenarios/monocentric.js';
import { c6, c6c } from '../src/scenarios/c6.js';
import { h1 } from '../src/scenarios/h1.js';
import { assertEqual, assertTrue, report, suite, test } from './tiny.js';

/** A 2-D context that records what was asked of it. */
function fakeCanvas(size = 600) {
  /** @type {{op: string, args: number[]}[]} */
  const calls = [];
  /** @param {string} op */
  const rec = (op) => (/** @type {number[]} */ ...args) => calls.push({ op, args });

  /** @type {string[]} */
  const text = [];
  /** @type {{text: string, x: number, y: number}[]} */
  const labels = [];
  const ctx = {
    fillStyle: '', strokeStyle: '', lineWidth: 0, font: '', textBaseline: '', textAlign: '',
    setTransform: rec('setTransform'), fillRect: rec('fillRect'), strokeRect: rec('strokeRect'),
    beginPath: rec('beginPath'), closePath: rec('closePath'), moveTo: rec('moveTo'),
    lineTo: rec('lineTo'), arc: rec('arc'), fill: rec('fill'), stroke: rec('stroke'),
    /** @param {string} t @param {number} x @param {number} y */
    fillText(t, x, y) { text.push(t); labels.push({ text: t, x, y }); },
    // Its argument is an array of dash lengths, not coordinates, so it is recorded as having
    // happened but contributes nothing to the every-argument-is-finite checks.
    setLineDash() { calls.push({ op: 'setLineDash', args: [] }); },
    save: rec('save'), restore: rec('restore'),
    translate: rec('translate'), rotate: rec('rotate'),
  };
  const canvas = {
    width: size, height: size, clientWidth: size, clientHeight: size,
    getContext: () => ctx,
  };
  return {
    canvas: /** @type {any} */ (canvas),
    calls,
    text,
    labels,
    /** Forget everything recorded so far. The accumulating views redraw the whole picture
     * every frame, so a test of what is on screen wants the last frame, not all of them. */
    clear() { calls.length = 0; text.length = 0; labels.length = 0; },
  };
}

/**
 * Run a scenario with a view attached, drawing each day the way the page does. The
 * accumulating charts build their picture from being drawn as the run goes, so a test that
 * draws only at the end tests nothing they do.
 * @param {import('../src/scenario.js').Scenario} scenario
 * @param {{draw: (w: World) => void}} view @param {number} days
 */
function runWithView(scenario, view, days) {
  const world = new World(scenario.config());
  scenario.setUp?.(world);
  for (let d = 0; d < days; d++) {
    scenario.beforeDay?.(world, d);
    world.timeline.nextDay();
    view.draw(world);
  }
  return world;
}

// The view reads devicePixelRatio off `window`; Node has neither.
globalThis.window = /** @type {any} */ ({ devicePixelRatio: 2 });

/** @param {number} [days] @param {object} [overrides] */
function settled(days = 30, overrides = {}) {
  const w = new World(monocentric(10, { peopleCount: 60, ...overrides }));
  for (let d = 0; d < days; d++) w.timeline.nextDay();
  return w;
}

suite('Plane view');

test('draws a density circle for every Person, plus their marker', () => {
  const { canvas, calls } = fakeCanvas();
  const world = settled();
  planeView(canvas, { shadeBy: 'density' }).draw(world);

  const arcs = calls.filter((c) => c.op === 'arc').length;
  assertTrue(`expected at least one circle per Person, got ${arcs} for ${world.people.length}`,
    arcs >= world.people.length);

  // Markers are squares: one fillRect for the background, then one per non-zero Person.
  const rects = calls.filter((c) => c.op === 'fillRect').length;
  assertTrue(`expected People drawn as squares, got ${rects} fillRects`, rects > 1);
});

test('the Firm is a twelve-vertex plus, as in the original', () => {
  const { canvas, calls } = fakeCanvas();
  planeView(canvas).draw(settled());
  const lineTos = calls.filter((c) => c.op === 'lineTo').length;
  // One moveTo + eleven lineTo per Firm.
  assertEqual('eleven lineTo calls for one Firm glyph', 11, lineTos);
});

test('every coordinate is finite and on the canvas', () => {
  // The failure mode that actually happens: a NaN position, or a model-unit value used
  // where a pixel was wanted, silently drawing nothing.
  const { canvas, calls } = fakeCanvas(600);
  planeView(canvas).draw(settled());
  for (const c of calls) {
    for (const a of c.args) {
      assertTrue(`${c.op} received a non-finite argument: ${a}`, Number.isFinite(a));
    }
  }
  const coords = calls.filter((c) => c.op === 'arc' || c.op === 'lineTo' || c.op === 'moveTo');
  assertTrue('shapes should be drawn inside the canvas', coords.every(
    (c) => c.args[0] >= -50 && c.args[0] <= 1250 && c.args[1] >= -50 && c.args[1] <= 1250,
  ));
});

test('zero-utility People get the white circle, not a square', () => {
  // The "nowhere was affordable" marker. It is what makes C8's dispersal legible: as the
  // density cost rises, the settlement visibly fills up with these.
  const { canvas, calls } = fakeCanvas();
  const crushed = settled(2, { densityCost: 1e6, peopleMobile: false });
  assertTrue('setup should actually crush someone',
    crushed.people.some((p) => p.utility === 0));

  const before = calls.length;
  planeView(canvas, { showDensityCircles: false }).draw(crushed);
  const arcs = calls.slice(before).filter((c) => c.op === 'arc').length;
  assertTrue(`expected white circles for the zero-utility People, got ${arcs} arcs`, arcs > 0);
});

test('shading by a different variable changes the picture', () => {
  const world = settled();
  const byDensity = fakeCanvas();
  const byUtility = fakeCanvas();
  planeView(byDensity.canvas, { shadeBy: 'density' }).draw(world);
  planeView(byUtility.canvas, { shadeBy: 'utility' }).draw(world);

  const sizes = (/** @type {{calls: {op: string, args: number[]}[]}} */ c) =>
    c.calls.filter((k) => k.op === 'fillRect').map((k) => k.args[2]).join(',');
  assertTrue('marker sizes should differ between the two variables',
    sizes(byDensity) !== sizes(byUtility));
});

suite('Plane view — von Thunen groups (figures 5.15, 5.16)');

test('each cost group gets its own shape and grey, with a legend', () => {
  // Figure 5.15: 0.5 = light square, 1 = circle, 1.5 = triangle, 2 = darker pentagon. The
  // shapes matter as much as the greys — four greys alone are not separable in print, and
  // the group IS the result in these models.
  const { canvas, calls, text } = fakeCanvas();
  const w = new World(c6.config());
  c6.setUp?.(w);
  for (let d = 0; d < 30; d++) w.timeline.nextDay();

  planeView(canvas, { shadeBy: 'auto', showDensityCircles: false }).draw(w);

  // Squares for group 0, circles for 1, triangles for 2, pentagons for 3 — plus the Firm
  // glyph. Every shape family should be present.
  assertTrue('squares drawn', calls.some((c) => c.op === 'fillRect'));
  assertTrue('circles drawn', calls.some((c) => c.op === 'arc'));
  const closes = calls.filter((c) => c.op === 'closePath').length;
  assertTrue(`triangles and pentagons drawn (${closes} closed paths)`, closes > 2);

  assertTrue(`legend names the varying cost, got: ${text.join(' | ')}`,
    text.some((t) => t.includes('wage')));
  for (const wage of ['0.5', '1', '1.5', '2']) {
    assertTrue(`legend shows wage ${wage}`, text.some((t) => t === `= ${wage}`));
  }
});

test('the commute variant labels its own cost, not the wage', () => {
  // Figure 5.16 is the same picture keyed to a spatial cost, and the sorting reverses.
  const { canvas, text } = fakeCanvas();
  const w = new World(c6c.config());
  c6c.setUp?.(w);
  w.timeline.nextDay();
  planeView(canvas).draw(w);

  assertTrue(`legend should name the commute cost, got: ${text.join(' | ')}`,
    text.some((t) => t.includes('commute')));
  assertTrue('and show its four levels',
    ['0.25', '0.5', '0.75', '1'].every((v) => text.some((t) => t === `= ${v}`)));
});

test('auto picks groups only where People are tied to Firms', () => {
  const grouped = fakeCanvas();
  const w = new World(c6.config());
  c6.setUp?.(w);
  w.timeline.nextDay();
  planeView(grouped.canvas).draw(w);
  assertTrue('c6 gets a legend', grouped.text.length > 0);

  const plain = fakeCanvas();
  planeView(plain.canvas).draw(settled(5));
  assertEqual('c1 does not', 0, plain.text.length);
});

suite('Line view (§5.5.1, §6.4.1)');

test('draws mirrored demand profiles either side of the rule', () => {
  const { canvas, calls, text } = fakeCanvas();
  const w = new World(d1d.config());
  for (let d = 0; d < 200; d++) w.timeline.nextDay();

  lineView(canvas, profilesFor(w)).draw(w);

  // One bar per Person per side, so both halves should be populated.
  const bars = calls.filter((c) => c.op === 'fillRect');
  const mid = 600 / 2;
  assertTrue('bars above the rule', bars.some((b) => b.args[1] < mid));
  assertTrue('bars below the rule', bars.some((b) => b.args[1] > mid));
  assertTrue(`both Firms labelled, got: ${text.join(' | ')}`,
    text.some((t) => t.includes('firm 0')) && text.some((t) => t.includes('firm 1')));
});

test('the profiles come from what People actually bought', () => {
  // Not recomputed in the view: the Person records their purchase, so under demand noise
  // the picture shows the noisy quantities -- which is right, since that is what a Firm is
  // trying to read.
  const w = new World(d1d.config());
  w.timeline.nextDay();
  const profile = demandForFirm(0)(w);
  assertEqual('one entry per Person', w.people.length, profile.values.length);
  assertTrue('and someone bought something', profile.values.some((v) => v > 0));
});

test('many-agent line models get efficiency against utility', () => {
  const { canvas, text } = fakeCanvas();
  const w = new World(e7.config());
  for (let d = 0; d < 50; d++) w.timeline.nextDay();
  lineView(canvas, profilesFor(w)).draw(w);
  assertTrue(`labelled by profile, got: ${text.join(' | ')}`,
    text.some((t) => t.includes('efficiency')) && text.some((t) => t.includes('utility')));
});

test('every line-view coordinate is finite', () => {
  const { canvas, calls } = fakeCanvas();
  const w = new World(d1d.config());
  for (let d = 0; d < 100; d++) w.timeline.nextDay();
  lineView(canvas, profilesFor(w)).draw(w);
  for (const c of calls) {
    for (const a of c.args) {
      assertTrue(`${c.op} received a non-finite argument: ${a}`, Number.isFinite(a));
    }
  }
});

suite('Time-series view (§6.3.1)');

test('plots the deduced clearing price as a reference line', () => {
  // E1's whole question is whether the series arrives at a hand-deducible value, so that
  // value belongs on the chart.
  const { canvas, text } = fakeCanvas();
  const w = new World(e1.config());
  const view = timeSeriesView(canvas, {
    tracks: [{ label: 'price', get: (world) => world.firm(0).goodCost }],
    reference: 0.5,
    referenceLabel: 'clearing price',
  });
  for (let d = 0; d < 50; d++) { w.timeline.nextDay(); view.draw(w); }
  assertTrue(`reference labelled, got: ${text.join(' | ')}`,
    text.some((t) => t.includes('clearing price')));
  assertTrue('and the series labelled with its latest value',
    text.some((t) => t.startsWith('price ')));
});

test('samples once per day however often it is drawn', () => {
  // The view is redrawn every animation frame, including while the model is paused.
  // Sampling on draw rather than on day would pile up duplicate points whenever the frame
  // rate outran the model — which it does on every settled model — and quietly distort both
  // the axis and the shape of the line.
  const { canvas } = fakeCanvas();
  const w = new World(e1.config());
  let sampled = 0;
  const view = timeSeriesView(canvas, {
    tracks: [{ label: 'price', get: (world) => { sampled++; return world.firm(0).goodCost; } }],
  });

  for (let d = 0; d < 5; d++) {
    w.timeline.nextDay();
    for (let frame = 0; frame < 4; frame++) view.draw(w); // four frames per day
  }
  assertEqual('five days, five samples, twenty draws', 5, sampled);

  view.draw(w); // paused: no new day
  assertEqual('a paused redraw adds nothing', 5, sampled);
});

suite('Curve view — group A (§2.2)');

test('plots against the swept parameter, not the day', () => {
  // The distinction the whole view exists for. A1 sweeps rho from 0.99 down to 0.01 over 99
  // days: if the x axis were time its ticks would run 0..99, and if it is the parameter they
  // run 0..1 and the curve is a function.
  const fake = fakeCanvas();
  const { canvas, labels } = fake;
  const chosen = chooseView(a1, new World(a1.config()));
  const view = chosen.make(canvas);
  const world = runWithView(a1, view, a1.defaultDays ?? 99);
  fake.clear();
  view.draw(world);

  assertTrue('x axis is labelled rho', labels.some((l) => l.text === 'rho'));
  // Plain decimal labels are the ticks. If time were on the x axis they would run to 99;
  // rho's range puts every one of them at or below 1.
  const ticked = labels.filter((l) => /^[\d.]+$/.test(l.text)).map((l) => Number(l.text));
  assertTrue(`ticks should span rho's range, got ${ticked.join(', ')}`,
    ticked.length > 1 && Math.max(...ticked) <= 1.05);
});

test('a log axis keeps forty-seven orders of magnitude on the canvas', () => {
  // A1's utility is 3^(1/rho − 1), which reaches 1.7e47. On a linear axis every other point
  // in the sweep lands on the x rule; the failure is silent, and looks like a model that
  // does nothing until it explodes.
  const fake = fakeCanvas(600);
  const { canvas, calls, text } = fake;
  const chosen = chooseView(a1, new World(a1.config()));
  const view = chosen.make(canvas);
  const world = runWithView(a1, view, a1.defaultDays ?? 99);
  fake.clear();
  view.draw(world);

  for (const c of calls) {
    for (const a of c.args) {
      assertTrue(`${c.op} received a non-finite argument: ${a}`, Number.isFinite(a));
    }
  }
  const drawn = calls.filter((c) => c.op === 'lineTo' || c.op === 'moveTo');
  assertTrue('the curve stays inside the canvas',
    drawn.every((c) => c.args[1] >= -50 && c.args[1] <= 1250));
  assertTrue(`decade ticks are labelled, got: ${text.join(' | ')}`,
    text.some((t) => t.includes('e+')));
});

test('curves ending at the same value keep separate labels', () => {
  // A1's closed form lands on the computed utility exactly — which is the check being made,
  // so both labels have to be readable. Before this they were written on top of each other.
  const fake = fakeCanvas();
  const { canvas, labels } = fake;
  const chosen = chooseView(a1, new World(a1.config()));
  const view = chosen.make(canvas);
  const world = runWithView(a1, view, a1.defaultDays ?? 99);
  fake.clear();
  view.draw(world);

  const series = labels.filter((l) => l.text.startsWith('utility ') || l.text.startsWith('closed form '));
  assertEqual('both series labelled', 2, series.length);
  assertTrue(`labels must not sit on top of each other (${series.map((l) => l.y).join(', ')})`,
    Math.abs(series[0].y - series[1].y) > 8);
});

test('a3 keeps the zero rule, because the Core Model curve crosses it', () => {
  const fake = fakeCanvas();
  const { canvas, calls } = fake;
  const chosen = chooseView(a3, new World(a3.config()));
  const view = chosen.make(canvas);
  const world = runWithView(a3, view, a3.defaultDays ?? 120);
  fake.clear();
  view.draw(world);

  // Output below alpha is negative and is not clamped; the axis must therefore show points
  // both above and below the zero rule.
  const ys = calls.filter((c) => c.op === 'lineTo').map((c) => c.args[1]);
  const zeroRow = calls.filter((c) => c.op === 'moveTo').map((c) => c.args[1]);
  assertTrue('the negative region is drawn', Math.max(...ys) > Math.max(...zeroRow) - 1);
});

suite('Arrow scatter — E8 (§6.4.2)');

test('joins consecutive samples in time order, with arrowheads', () => {
  const { canvas, calls } = fakeCanvas();
  const view = correlateView(canvas, {
    x: { label: 'x', get: (w) => w.day },
    y: { label: 'y', get: (w) => w.day * 2 },
    sampleEvery: 1, arrowEvery: 5,
  });
  runWithView(e1, view, 40);

  // One closed path per arrowhead: 40 samples, an arrowhead every 5.
  const heads = calls.filter((c) => c.op === 'closePath').length;
  assertTrue(`expected arrowheads along the track, got ${heads}`, heads >= 6);
  assertTrue('and the track itself', calls.filter((c) => c.op === 'lineTo').length > 30);
});

test('a stalled track draws no arrowhead', () => {
  // Zero-length segments have no direction to point in. Normalising one gives NaN, which
  // silently poisons every coordinate downstream of it.
  const { canvas, calls } = fakeCanvas();
  const view = correlateView(canvas, {
    x: { label: 'x', get: () => 1 },
    y: { label: 'y', get: () => 1 },
    sampleEvery: 1, arrowEvery: 2,
  });
  runWithView(e1, view, 20);
  for (const c of calls) {
    for (const a of c.args) {
      assertTrue(`${c.op} received a non-finite argument: ${a}`, Number.isFinite(a));
    }
  }
});

test('samples on the given interval, and from the first day it sees', () => {
  const { canvas } = fakeCanvas();
  let sampled = 0;
  const view = correlateView(canvas, {
    x: { label: 'x', get: (w) => { sampled++; return w.day; } },
    y: { label: 'y', get: (w) => w.day },
    sampleEvery: 10,
  });
  runWithView(e1, view, 40);
  // Day 1 (the first the view sees), then 10, 20, 30, 40.
  assertEqual('forty days, sampled every tenth', 5, sampled);
});

test('reset drops the discarded run', () => {
  // reset and reroll build a new World. Without this the previous run stayed on the chart,
  // and its bounds went on stretching the axes for a run that no longer existed.
  const { canvas, labels } = fakeCanvas();
  const view = correlateView(canvas, {
    x: { label: 'x', get: (w) => w.day },
    y: { label: 'y', get: (w) => w.day },
    sampleEvery: 1,
  });
  runWithView(e1, view, 30);
  view.reset();
  runWithView(e1, view, 5);
  assertTrue(`head should report the short run, got: ${labels.map((l) => l.text).join(' | ')}`,
    labels.some((l) => l.text.startsWith('day 5 ')));
});

test('e8 gets the arrow scatter as its second panel', () => {
  // The plane view is still the main one — the settlement forming is worth watching — but it
  // cannot show that migration and concentration are one event.
  const chosen = chooseView(e8, new World(e8.config()));
  assertTrue('e8 has a second panel', Boolean(chosen.aux));
  assertTrue('and c1 does not', !chooseView(c1, new World(c1.config())).aux);
});

suite('Buy lines (§5.9, never published)');

test('one line per Person per Firm they actually bought from', () => {
  const world = settled(20);
  const { canvas, calls } = fakeCanvas();
  planeView(canvas, { showBuyLines: true, showDensityCircles: false }).draw(world);

  // The plane view draws no other line segments once the density circles are off, so every
  // moveTo/lineTo pair is a trade.
  const expected = world.people.reduce(
    (a, p) => a + p.lastQuantities.filter((q) => q > 0).length, 0);
  assertEqual('a line per non-zero purchase', expected, calls.filter((c) => c.op === 'lineTo').length
    - 11 * world.firms.length); // less the Firm glyphs, eleven lineTo each
});

test('the lines come from the purchase record, not from recomputing it', () => {
  // Under demand noise what a Person bought is not what the optimum says they wanted, and
  // this view must show the former — it is the only picture of the trade itself.
  const world = settled(20);
  const someone = world.people.find((p) => p.lastQuantities.some((q) => q > 0));
  assertTrue('someone bought something', Boolean(someone));
  if (someone) someone.lastQuantities = someone.lastQuantities.map(() => 0);

  const { canvas, calls } = fakeCanvas();
  planeView(canvas, { showBuyLines: true, showDensityCircles: false }).draw(world);
  const expected = world.people.reduce(
    (a, p) => a + p.lastQuantities.filter((q) => q > 0).length, 0);
  assertEqual('zeroing a record removes its lines', expected,
    calls.filter((c) => c.op === 'lineTo').length - 11 * world.firms.length);
});

suite('Equalising cutoff — C1 (§5.3)');

test('the median estimator reproduces the measured fringe', () => {
  // The check that this overlay is not simply drawing a plausible-looking circle: the fringe
  // fraction it reports should fall with population as the Java independently measured —
  // 43.2 / 20.4 / 7.2 per cent over 125 / 250 / 500 People.
  const measured = [];
  for (const n of [125, 250, 500]) {
    const config = c1.config();
    config.peopleCount = n;
    const w = new World(config);
    c1.setUp?.(w);
    for (let d = 0; d < 900; d++) w.timeline.nextDay();
    const found = equalisingCutoff(w);
    assertTrue(`a cutoff exists at n=${n}`, found !== null);
    measured.push(found ? found.fraction * 100 : NaN);
  }
  assertTrue(`the fringe should shrink with numbers, got ${measured.map((v) => v.toFixed(1)).join(', ')}%`,
    measured[0] > measured[1] && measured[1] > measured[2]);
  assertTrue(`and be in the right band at n=500, got ${measured[2].toFixed(1)}%`,
    measured[2] > 2 && measured[2] < 15);
});

test('it is offered only where its closed form exists', () => {
  // Two Firms and there is no single centre to measure distance from; no density cost and
  // there is nothing to trade against distance.
  assertTrue('c1 qualifies', equalisingLineApplies(settled(2)));
  assertTrue('a two-Firm model does not', !equalisingLineApplies(new World(d1d.config())));
  assertTrue('nor does one with no density cost',
    !equalisingLineApplies(new World(monocentric(0, { peopleCount: 20 }))));
});

suite('Bundle sampler — F4 (§4.6.1)');

test('draws every Bundle the subject considered, and rings the one taken', () => {
  const world = new World(f4.config());
  f4.setUp?.(world);
  for (let d = 0; d < 8; d++) world.timeline.nextDay();

  const { candidates, best } = lastDecision;
  assertEqual('ten near, ten wide, one for standing still', 21, candidates.length);
  assertTrue('and the chosen one is the best of them', Boolean(best)
    && candidates.filter((b) => b.isViable()).every((b) => b.utility <= (best?.utility ?? 0)));

  const { canvas, text } = fakeCanvas();
  chooseView(f4, world).make(canvas).draw(world);
  assertTrue(`the key counts them, got: ${text.join(' | ')}`,
    text.some((t) => t === '21 Bundles sampled'));
});

test('discarded Bundles are drawn as a different thing, not a low score', () => {
  // A Bundle whose density cost exceeds the wage has no budget and is thrown away rather
  // than scored. That distinction is the rule C8's dispersal runs on, so the picture has to
  // make it, and F4 is tuned so some appear.
  const world = new World(f4.config());
  f4.setUp?.(world);
  for (let d = 0; d < 8; d++) world.timeline.nextDay();

  const discarded = lastDecision.candidates.filter((b) => !b.isViable()).length;
  assertTrue(`some candidates should be unaffordable, got ${discarded} of 21`, discarded > 0);
  assertTrue('and some should not be', discarded < 21);
});

test('the decision is captured before the move, so it has a from and a to', () => {
  // By draw time the Person has already moved, and person.position IS the chosen Bundle's
  // location — so a view reading it back would draw the choice and its outcome as one mark.
  const world = new World(f4.config());
  f4.setUp?.(world);
  let moves = 0;
  for (let d = 0; d < 20; d++) {
    world.timeline.nextDay();
    const b = lastDecision.best;
    if (b && (b.location.x !== lastDecision.from.x || b.location.y !== lastDecision.from.y)) moves++;
  }
  assertTrue(`the subject should move at least once in twenty days, got ${moves}`, moves > 0);
});

suite('Optimiser view — A4 second panel (§4.6.1)');

test('the climb stays on the budget line', () => {
  // Not enforced by rejecting candidates: each is normalised back onto the constraint before
  // being scored, so every accepted point spends the budget exactly. Worth asserting,
  // because the picture is only honest if it is true.
  const climber = hillClimb(400);
  const rng = new Rng(7);
  climber.optimise([1, 2], 1, ces(0.5), rng);
  assertTrue('the path was recorded', climber.lastPath.length > 1);
  for (const step of climber.lastPath) {
    const spend = step.q[0] * 1 + step.q[1] * 2;
    assertTrue(`every accepted basket spends the budget, got ${spend}`, Math.abs(spend - 1) < 1e-9);
  }
});

test('accepted steps bunch at the start', () => {
  // A4's claim, and what the panel shows: the climber lands in the right neighbourhood
  // almost at once and spends the rest of its budget shuffling.
  const climber = hillClimb(2000);
  climber.optimise([1, 1], 1, ces(0.5), new Rng(11));
  const path = climber.lastPath;
  const early = path.filter((p) => p.i <= 200).length;
  assertTrue(`most acceptances should land in the first tenth, got ${early} of ${path.length}`,
    early > path.length / 2);
});

test('and it converges on what the algebra gives', () => {
  const rng = new Rng(3);
  const climbed = hillClimb(4000).optimise([1, 1], 1, ces(0.5), rng);
  const exact = constrained().optimise([1, 1], 1, ces(0.5), rng);
  assertTrue(`hill-climb ${climbed[0]} vs constrained ${exact[0]}`,
    Math.abs(climbed[0] - exact[0]) < 0.01);
});

suite('H1 — the 2011 ring and the separation trace');

/** H1 a few days in, workforces tied. */
function h1World(days = 20) {
  const w = new World(h1.config());
  h1.setUp?.(w);
  for (let d = 0; d < days; d++) w.timeline.nextDay();
  return w;
}

test('the 2011 ring sits at the mean of every actor, People and Firms alike', () => {
  // `drawMeanCentrePoint` in the 2011 renderer: not an agent, which is the point of drawing it.
  const { canvas, calls } = fakeCanvas(600);
  const w = h1World();
  planeView(canvas, { showMeanPoint: true, showDensityCircles: false }).draw(w);

  const actors = [...w.people, ...w.firms];
  const px = canvas.width / w.space.width;
  const mx = (px * actors.reduce((a, p) => a + p.position.x, 0)) / actors.length;
  const my = (px * actors.reduce((a, p) => a + p.position.y, 0)) / actors.length;
  const rings = calls.filter((c) => c.op === 'arc'
    && Math.abs(c.args[0] - mx) < 1e-6 && Math.abs(c.args[1] - my) < 1e-6);
  assertEqual('a filled circle inside a ring, at the mean point', 2, rings.length);
});

test('and it is off unless asked for, and offered only on H1', () => {
  const { canvas, calls } = fakeCanvas(600);
  const w = h1World();
  planeView(canvas, { showDensityCircles: false }).draw(w);
  const actors = [...w.people, ...w.firms];
  const px = canvas.width / w.space.width;
  const mx = (px * actors.reduce((a, p) => a + p.position.x, 0)) / actors.length;
  assertEqual('nothing drawn at the mean by default', 0,
    calls.filter((c) => c.op === 'arc' && Math.abs(c.args[0] - mx) < 1e-6).length);

  const offered = chooseView(h1, w).overlays ?? [];
  assertTrue('h1 offers the ring', offered.some((o) => o.key === 'showMeanPoint'));
  const probe = new World(c6.config());
  c6.setUp?.(probe);
  assertTrue('c6 does not', !(chooseView(c6, probe).overlays ?? []).some((o) => o.key === 'showMeanPoint'));
});

test('the second panel traces the separation against touching', () => {
  // The rhythm of meeting and parting is the behaviour, and a single frame cannot show it.
  const w = new World(h1.config());
  h1.setUp?.(w);
  const aux = chooseView(h1, w).aux;
  assertTrue('h1 has a second panel', Boolean(aux));
  if (!aux) return;
  const { canvas, text } = fakeCanvas();
  const view = aux.make(canvas);
  for (let d = 0; d < 60; d++) { w.timeline.nextDay(); view.draw(w); }
  assertTrue(`the reference is labelled, got: ${text.join(' | ')}`,
    text.some((t) => t.includes('touching')));
  assertTrue('and the series with its latest value', text.some((t) => t.startsWith('separation ')));
});

test('the workforces get group shapes, and the legend names the wage', () => {
  // Identity by shape and grey, as C6 — never a colour ramp for a quantity (BUILD_PLANNER §4.2).
  const { canvas, text } = fakeCanvas();
  planeView(canvas).draw(h1World());
  assertTrue(`legend names the wage, got: ${text.join(' | ')}`, text.some((t) => t.includes('wage')));
  assertTrue('and shows both levels', ['1', '2'].every((v) => text.some((t) => t === `= ${v}`)));
});

suite('Plane view — edge cases');

test('survives an empty and a single-agent world', () => {
  const { canvas } = fakeCanvas();
  const view = planeView(canvas);
  view.draw(new World(monocentric(10, { peopleCount: 0 })));
  view.draw(new World(monocentric(0, { peopleCount: 1 })));
  assertTrue('no throw', true);
});

report();
