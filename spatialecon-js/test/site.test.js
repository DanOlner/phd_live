// @ts-check

/**
 * The site layer: the walkthroughs, the pop-out content, and the precomputed sweeps.
 *
 * These are the parts most likely to rot silently. A step whose predicate never becomes true
 * is a caption nobody ever sees; a sweep JSON that stops matching its renderer is a blank
 * chart; a `found` panel that outlives the finding it describes is worse than no panel. None
 * of that throws, so none of it shows up anywhere else.
 */

import { readFileSync, existsSync } from 'node:fs';
import { World } from '../src/world.js';
import { CONTENT, contentFor } from '../src/content.js';
import { paintCurves } from '../src/view/curve.js';
import { sweepView } from '../src/view/sweep.js';
import { get, ids } from '../src/scenario.js';
import '../src/scenarios/index.js';
import { assertEqual, assertTrue, report, suite, test } from './tiny.js';

globalThis.window = /** @type {any} */ ({ devicePixelRatio: 1 });

/** A 2-D context that records nothing but survives being drawn on. */
function fakeCanvas(size = 600) {
  /** @type {string[]} */
  const text = [];
  const noop = () => {};
  const ctx = new Proxy(/** @type {any} */ ({
    fillStyle: '', strokeStyle: '', lineWidth: 0, font: '12px x',
    textBaseline: '', textAlign: '',
    /** @param {string} t */
    fillText(t) { text.push(t); },
  }), {
    get: (t, k) => (k in t ? t[k] : noop),
    set: (t, k, v) => { t[k] = v; return true; },
  });
  return {
    canvas: /** @type {any} */ ({
      width: size, height: size, clientWidth: size, clientHeight: size,
      getContext: () => ctx,
      getBoundingClientRect: () => ({ left: 0, top: 0, width: size, height: size }),
    }),
    text,
  };
}

suite('Walkthroughs fire on model state');

for (const id of Object.keys(CONTENT)) {
  const entry = CONTENT[id];
  if (!entry.steps) continue;
  // The family key ('d1') is not itself a registered model; walk it on its last variant.
  const modelId = ids().includes(id) ? id : ids().filter((x) => x.startsWith(id)).pop();
  if (!modelId) continue;

  test(`${id}: every caption is reachable within the default run`, () => {
    const scenario = get(modelId);
    const world = new World(scenario.config());
    scenario.setUp?.(world);

    const steps = /** @type {NonNullable<typeof entry.steps>} */ (entry.steps);
    let reached = -1;
    const days = scenario.defaultDays ?? 1000;
    for (let d = 0; d < days; d++) {
      scenario.beforeDay?.(world, d);
      world.timeline.nextDay();
      while (reached + 1 < steps.length && steps[reached + 1].when(world)) reached++;
      if (reached === steps.length - 1) break;
    }
    assertEqual(`${modelId} reached step ${reached + 1} of ${steps.length} in ${days} days`,
      steps.length - 1, reached);
  });
}

test('no caption reads the day number without declaring that it does', () => {
  // The rule that makes reroll possible at all (BUILD_PLANNER §3.2). A predicate reading
  // world.day is pinned to a trajectory, and a different seed moves the trajectory.
  //
  // The exception is a caption whose subject *is* elapsed time — "two thousand days in and
  // the Firms are still moving", which holds under every seed because they never settle.
  // Those declare `elapsed: true`. Everything else must test state, and this is what keeps
  // the distinction a decision rather than something that slid in.
  const offenders = [];
  for (const [id, entry] of Object.entries(CONTENT)) {
    for (const step of entry.steps ?? []) {
      if (step.elapsed) continue;
      const source = step.when.toString();
      if (/\.day\b/.test(source)) offenders.push(`${id}: ${source.replace(/\s+/g, ' ')}`);
    }
  }
  assertEqual(`captions reading world.day undeclared: ${offenders.join(' | ')}`,
    0, offenders.length);
});

test('and the declared ones really are about elapsed time', () => {
  // The flag is an escape hatch, so it should be rare and it should be used for what it says.
  const declared = Object.values(CONTENT)
    .flatMap((e) => e.steps ?? []).filter((s) => s.elapsed);
  assertTrue(`${declared.length} elapsed-time captions is more than a handful`,
    declared.length <= 5);
  assertTrue('every one of them actually reads the day',
    declared.every((s) => /\.day\b/.test(s.when.toString())));
});

suite('Pop-out content');

test('every entry belongs to a real model, or is a family others inherit', () => {
  const registered = new Set(ids());
  const orphans = Object.keys(CONTENT).filter((id) =>
    !registered.has(id) && !ids().some((x) => contentFor(x) === CONTENT[id]));
  assertEqual(`content for nothing: ${orphans.join(', ')}`, 0, orphans.length);
});

test('variants inherit their family rather than falling back to nothing', () => {
  assertTrue('d1b inherits d1', contentFor('d1b') === CONTENT.d1);
  assertTrue('e6 inherits e5', contentFor('e6') === CONTENT.e5);
  assertTrue('e1raw inherits e1', contentFor('e1raw') === CONTENT.e1);
  assertTrue('an unknown id yields an empty entry, not undefined',
    typeof contentFor('nope') === 'object');
});

test('the models with a disagreement to report actually report it', () => {
  // Decision 4: the disagreements go in. These three are the ones the rebuild turned up, and
  // a `found` panel quietly disappearing is exactly the regression worth catching.
  assertTrue('d2 records the unreproduced rho direction',
    (CONTENT.d2.found ?? '').includes('opposite'));
  assertTrue('e8 records the wrong-statistic resolution',
    (CONTENT.e8.found ?? '').includes('1.3'));
  assertTrue('c1 records the fringe', (CONTENT.c1.found ?? '').includes('fringe')
    || (CONTENT.c1.found ?? '').includes('cutoff'));
});

suite('Precomputed sweeps');

const manifestPath = 'data/sweeps/index.json';
const haveSweeps = existsSync(manifestPath);

test('the manifest lists sweeps that exist and name real models', () => {
  assertTrue('run `node tools/sweep.mjs` to generate data/sweeps/', haveSweeps);
  if (!haveSweeps) return;
  const manifest = JSON.parse(readFileSync(manifestPath, 'utf8'));
  assertTrue('at least one sweep', manifest.length > 0);
  for (const entry of manifest) {
    assertTrue(`${entry.id}.json exists`, existsSync(`data/sweeps/${entry.id}.json`));
    const doc = JSON.parse(readFileSync(`data/sweeps/${entry.id}.json`, 'utf8'));
    const models = Array.isArray(doc.model) ? doc.model : [doc.model];
    for (const m of models) {
      assertTrue(`${entry.id} names a registered model, got '${m}'`, ids().includes(m));
    }
  }
});

test('every series is the same length as its x axis', () => {
  if (!haveSweeps) return;
  for (const entry of JSON.parse(readFileSync(manifestPath, 'utf8'))) {
    const doc = JSON.parse(readFileSync(`data/sweeps/${entry.id}.json`, 'utf8'));
    for (const series of doc.series) {
      assertEqual(`${entry.id}/${series.label} length`, doc.xs.length, series.values.length);
      assertTrue(`${entry.id}/${series.label} is all finite`,
        series.values.every((v) => Number.isFinite(v)));
    }
  }
});

test('and paints through the same code as the live charts', () => {
  if (!haveSweeps) return;
  for (const entry of JSON.parse(readFileSync(manifestPath, 'utf8'))) {
    const doc = JSON.parse(readFileSync(`data/sweeps/${entry.id}.json`, 'utf8'));
    const { canvas, text } = fakeCanvas();
    sweepView(canvas, doc).draw();
    assertTrue(`${entry.id} labels its axis`, text.includes(doc.xLabel));
    for (const series of doc.series) {
      assertTrue(`${entry.id} labels '${series.label}'`,
        text.some((t) => t.startsWith(series.label)));
    }
  }
});

test('a click maps back to a swept value', () => {
  // What makes a sweep chart more than a picture: click at a value and set the live model
  // there. If the inverse mapping is wrong the model silently jumps somewhere else.
  if (!haveSweeps) return;
  const doc = JSON.parse(readFileSync('data/sweeps/c8-density.json', 'utf8'));
  const { canvas } = fakeCanvas(600);
  const view = sweepView(canvas, doc);
  view.draw();

  const painted = paintCurves(canvas, {
    xLabel: doc.xLabel,
    curves: doc.series.map((s) => ({ label: s.label, dash: s.dash })),
    xs: doc.xs,
    ys: doc.series.map((s) => s.values),
  });
  assertTrue('the chart painted', painted !== null);
  if (!painted) return;

  const target = 200;
  const back = view.valueAt(painted.sx(target), (painted.box.top + painted.box.bottom) / 2);
  assertTrue(`clicking at density ${target} should read back as ${target}, got ${back}`,
    back !== null && Math.abs(back - target) < 1);

  assertEqual('a click outside the plot reads as nothing', null, view.valueAt(1, 1));
});

report();
