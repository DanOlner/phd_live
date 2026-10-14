// @ts-check

/**
 * Which view a model gets, and what it plots.
 *
 * Lives in the view layer rather than on the `Scenario`, so a scenario stays runnable
 * headless with no view code loaded — and so the page and `tools/snapshot.mjs` cannot drift
 * apart about what a model looks like.
 *
 * The default is by space kind: a plane gets the plane view, a line the mirrored profile,
 * a dimensionless point a time series (there being nothing spatial to draw). A few models
 * override the time-series tracks, because what matters in them is a specific number
 * arriving at a specific value.
 */

import { ces } from '../econ.js';
import { Recorder } from '../record.js';
import { clearingPrice } from '../scenarios/belt.js';
import { productionRatio } from '../scenarios/e8.js';
import { SUBJECT_ID, lastDecision } from '../scenarios/f4.js';
import { currentWealth } from '../scenarios/f1.js';
import { binCentre, gapHistogram } from '../scenarios/f2.js';
import { coreParams } from '../scenarios/g1.js';
import { APART, FIRM_NAMES, firmSpeed, sales, settlements, workforce } from '../scenarios/h1.js';
import { LINE, POINT, TWO_REGION } from '../space.js';
import { coreModelView } from './coremodel.js';
import { correlateView } from './correlate.js';
import { curveView } from './curve.js';
import { distributionView, gini } from './distribution.js';
import { equalisingLineApplies } from './equalise.js';
import { lineView } from './line.js';
import { optimiserView } from './optimiser.js';
import { planeView } from './plane.js';
import { profilesFor } from './profiles.js';
import { samplerView } from './sampler.js';
import { stripsView } from './strips.js';
import { timeSeriesView } from './timeseries.js';

/** @typedef {import('./timeseries.js').Track} Track */

/** Mean over Firms. @param {import('../world.js').World} w @param {(f: any) => number} f */
const firmMean = (w, f) => w.firms.reduce((a, x) => a + f(x), 0) / Math.max(w.firms.length, 1);

/**
 * Time-series tracks per model, where the default is not informative enough.
 *
 * The second argument reads a quantity the scenario already declares in `record()`, by name.
 * Several of these models keep their whole state outside the engine — E4 is a difference
 * equation, F3 runs two populations of its own — so the recorder is the only honest way for
 * a chart to see them, and it guarantees the chart and the CSV plot the same thing.
 *
 * @typedef {(name: string) => (world: import('../world.js').World) => number} Reader
 * @type {Record<string, (world: import('../world.js').World, read: Reader) => {tracks: Track[], reference?: number, referenceLabel?: string}>}
 */
const TIME_SERIES = {
  // E1: the whole question is whether price arrives at the hand-deduced clearing value, so
  // that value goes on the chart as a dashed line rather than in a caption.
  belt: (w) => ({
    tracks: [
      { label: 'price', get: (world) => firmMean(world, (f) => f.goodCost) },
      { label: 'stock', get: (world) => firmMean(world, (f) => f.stock) },
    ],
    reference: clearingPrice(w.people.length / Math.max(w.firms.length, 1)),
    referenceLabel: 'clearing price',
  }),

  // E3: the transition is visible as two prices separating and the efficiency ratio leaving
  // 1.0 at the same moment.
  e3: () => ({
    tracks: [
      { label: 'price max', get: (w) => Math.max(...w.firms.map((f) => f.goodCost)) },
      { label: 'price min', get: (w) => Math.min(...w.firms.map((f) => f.goodCost)) },
      { label: 'eff ratio', get: (w) => {
        const eff = w.firms.map((f) => f.outputPerUnitTime);
        const lo = Math.min(...eff);
        return lo <= 0 ? 1 : Math.max(...eff) / lo;
      }, dash: true },
    ],
    reference: 2 / 3,
    referenceLabel: 'symmetric 2/3',
  }),

  // E2: the two prices are the model. Firm 0's climbs as it marks up and Firm 1's FALLS,
  // which is the finding, and a mean over both would hide it exactly.
  e2: (world, read) => ({
    tracks: [
      { label: 'price 0', get: read('price_0') },
      { label: 'price 1', get: read('price_1') },
      { label: 'utility', get: read('utility'), dash: true },
    ],
  }),

  // E4: a difference equation, so what matters is whether the perturbation dies. Both prices
  // against the symmetric fixed point they either return to or leave.
  e4: (world, read) => ({
    tracks: [
      { label: 'p 0', get: read('p_0') },
      { label: 'p 1', get: read('p_1') },
    ],
    reference: 2 / 3,
    referenceLabel: 'symmetric 2/3',
  }),

  // F3: one line that never leaves zero, and one that does.
  f3: (world, read) => ({
    tracks: [
      { label: 'blind', get: read('blindOutput') },
      { label: 'rational', get: read('rationalOutput') },
    ],
    reference: 0,
    referenceLabel: 'no production',
  }),
};

/**
 * Read a quantity the scenario already declares in `record()`, by name.
 *
 * Rebinding on world identity rather than caching one recorder: `reset` and `reroll` build a
 * new World, and a series bound to the old one would keep reporting the run the visitor just
 * discarded. Rebuilt only when the world actually changes, so a redraw costs nothing.
 *
 * @param {import('../scenario.js').Scenario} scenario
 * @returns {(name: string) => (world: import('../world.js').World) => number}
 */
function declared(scenario) {
  /** @type {import('../world.js').World | null} */
  let bound = null;
  /** @type {Recorder} */
  let recorder = new Recorder();
  return (name) => (world) => {
    if (world !== bound) {
      recorder = new Recorder();
      scenario.record?.(recorder, world);
      bound = world;
    }
    return recorder.value(name);
  };
}

/**
 * Group A: what goes on each axis. The day number never does — these models sweep a
 * parameter, and the parameter is the x axis. See `curve.js`.
 *
 * Series are named as the scenario's own `record()` names them, so the chart cannot quietly
 * plot something other than what the CSV contains; the second entry is what the chart calls
 * it, which is a different job and wants to be short.
 *
 * @typedef {{name: string, as?: string, dash?: boolean}} Plotted
 * @type {Record<string, {x: string, xAs?: string, curves: Plotted[], yLabel: string, yScale?: 'linear'|'log'}>}
 */
const CURVES = {
  // Utility climbing as rho falls, over a quantity that does not move at all. The closed
  // form is dashed on top of the computed one: the agreement is the check.
  //
  // Log axis, because the honest numbers demand it. With three goods, utility is 3^(1/rho−1),
  // which at rho = 0.01 is 1.7e47. On a linear axis that single value flattens the other
  // ninety-eight days of the sweep onto the x rule and the model appears to do nothing until
  // it explodes. The claim being illustrated is that utility rises steeply and *without
  // bound* as love of variety strengthens; a log axis is what lets both halves of that be
  // seen at once.
  a1: {
    x: 'rho',
    curves: [
      { name: 'utility' },
      { name: 'analyticUtility', as: 'closed form', dash: true },
      { name: 'quantityPerGood', as: 'quantity' },
    ],
    yLabel: 'utility, quantity',
    yScale: 'log',
  },
  // The flat line that is the whole model. Firm 1's price sweeps a factor of fifteen and
  // demand for Firm 0's good does not move — nor does Firm 1's own revenue.
  a2: {
    x: 'firm1Price',
    xAs: "Firm 1's price",
    curves: [
      { name: 'demand0', as: 'demand 0' },
      { name: 'demand1', as: 'demand 1' },
      { name: 'revenue1', as: 'revenue 1', dash: true },
    ],
    yLabel: 'demand, revenue',
  },
  // The negative region below alpha, which is why the thesis does not use this function.
  // Linear, and it has to be: the point is a curve crossing zero, which a log axis cannot
  // show at all.
  a3: {
    x: 'labour',
    curves: [
      { name: 'smooth_total', as: 'smooth' },
      { name: 'smooth_marginal', as: 'smooth mrg', dash: true },
      { name: 'coreModel_total', as: 'core' },
      { name: 'coreModel_marginal', as: 'core mrg', dash: true },
    ],
    yLabel: 'output',
  },
  // Error and residual spread together: the gain arrives almost at once, eight goods
  // converge faster than two, and neither spread ever reaches zero. That last one is where
  // D2's demand noise comes from, so it belongs on the chart rather than in the notes — and
  // it is only visible on a log axis, since on a linear one everything past a few hundred
  // iterations sits indistinguishably on zero.
  a4: {
    x: 'iterations',
    curves: [
      { name: 'absError2', as: 'error n=2' },
      { name: 'sd2', as: 'spread n=2', dash: true },
      { name: 'absError8', as: 'error n=8' },
      { name: 'sd8', as: 'spread n=8', dash: true },
    ],
    yLabel: 'error, spread',
    yScale: 'log',
  },

  // C4: three closed forms and where they cross. Watch the mover's curve fall below the
  // stayer's everywhere that moving is worth doing — that is the externality, drawn.
  c4: {
    x: 'distanceCost',
    xAs: 'distance cost d',
    curves: [
      { name: 'together', as: 'both in 1' },
      { name: 'stayer', as: 'stayer' },
      { name: 'mover', as: 'mover', dash: true },
    ],
    yLabel: 'utility',
  },

  // C5: the same, paired into the two decisions. Each crossing is a specific Person leaving.
  c5: {
    x: 'distanceCost',
    xAs: 'distance cost d',
    curves: [
      { name: 'allThree', as: 'all three' },
      { name: 'firstOut', as: 'first out', dash: true },
      { name: 'twoLeft', as: 'two left' },
      { name: 'secondOut', as: 'second out', dash: true },
    ],
    yLabel: 'utility',
  },
};

/**
 * Models whose *published* figure is a curve but whose live picture is the settlement moving.
 * They get the plane view with the curve as a second panel — the swept result and the thing
 * being swept, at the same time.
 * @type {Record<string, {x: string, xAs?: string, curves: Plotted[], yLabel: string, title: string}>}
 */
const PLANE_CURVES = {
  c3w: { x: 'wage', xAs: 'wage', title: 'mean distance from the centre, against the wage',
    curves: [{ name: 'distanceFromCentre', as: 'distance' }], yLabel: 'distance' },
  c3g: { x: 'goodCost', xAs: 'base good cost', title: 'mean distance from the centre, against the base good cost',
    curves: [{ name: 'distanceFromCentre', as: 'distance' }], yLabel: 'distance' },
  c3c: { x: 'commute', xAs: 'commute cost', title: 'mean distance from the centre, against the commute cost',
    curves: [{ name: 'distanceFromCentre', as: 'distance' }], yLabel: 'distance' },
  c3d: { x: 'delivery', xAs: 'goods distance cost', title: 'mean distance from the centre, against the goods distance cost',
    curves: [{ name: 'distanceFromCentre', as: 'distance' }], yLabel: 'distance' },
  c7: {
    x: 'rho', xAs: 'rho (1 = perfect substitutes)',
    title: 'which Firm the population sits nearer, against rho',
    curves: [
      { name: 'distToCheap', as: 'to cheap Firm' },
      { name: 'distToDear', as: 'to dear Firm', dash: true },
    ],
    yLabel: 'mean distance',
  },
};

/**
 * @param {import('../scenario.js').Scenario} scenario
 * @param {import('../world.js').World} world a built world, for sizing decisions
 */
export function chooseView(scenario, world) {
  const kind = world.space.kind;

  // Krugman's Core Model is solved rather than simulated: the figure is a function of the
  // parameters, and the model's clock has nothing to do.
  if (scenario.id === 'g1') {
    return {
      make: (/** @type {HTMLCanvasElement} */ c) => coreModelView(c, { params: coreParams }),
      aspect: 1.6,
      shadeOptions: /** @type {string[]} */ ([]),
    };
  }

  // F1 and F2 produce a shape over a population rather than a trajectory. See `distribution.js`.
  if (scenario.id === 'f1') {
    return {
      make: (/** @type {HTMLCanvasElement} */ c) => distributionView(c, {
        mode: 'sorted',
        values: () => currentWealth(),
        xLabel: 'agents, richest first',
        yLabel: 'units held',
        readout: () => {
          const w = currentWealth();
          const total = w.reduce((a, v) => a + v, 0);
          const top = w.slice().sort((a, b) => b - a).slice(0, Math.max(1, Math.round(w.length / 10)));
          return [
            `Gini ${gini(w).toFixed(3)}`,
            `richest ${Math.max(...w)}`,
            `top tenth holds ${((top.reduce((a, v) => a + v, 0) / total) * 100).toFixed(1)}%`,
          ];
        },
      }),
      aspect: 1.7,
      shadeOptions: /** @type {string[]} */ ([]),
    };
  }

  if (scenario.id === 'f2') {
    return {
      make: (/** @type {HTMLCanvasElement} */ c) => distributionView(c, {
        // 'bars', not 'sorted' — the model bins these as they happen, and sorting the
        // counts turns the pyramid into a descending ramp.
        mode: 'bars',
        values: () => gapHistogram(),
        xRange: /** @type {[number, number]} */ ([0, 2]),
        xLabel: "gap between one agent's consecutive actions, in days",
        yLabel: 'agents',
        mark: { at: 1, label: 'one day' },
        readout: () => {
          const h = gapHistogram();
          const total = h.reduce((a, v) => a + v, 0);
          if (total === 0) return ['no gaps sampled yet'];
          const peak = h.indexOf(Math.max(...h));
          return [`${total.toLocaleString('en-GB')} gaps`, `peak at ${binCentre(peak).toFixed(2)} days`];
        },
      }),
      aspect: 1.7,
      shadeOptions: /** @type {string[]} */ ([]),
    };
  }

  // F4 is a plane model but not a plane picture: what it draws is one Person's decision, not
  // the population's state. Nothing else in the catalogue does that.
  if (scenario.id === 'f4') {
    return {
      make: (/** @type {HTMLCanvasElement} */ c) => samplerView(c, {
        decision: () => lastDecision,
        subjectId: SUBJECT_ID,
      }),
      aspect: 1,
      shadeOptions: /** @type {string[]} */ ([]),
    };
  }

  // Function plots come before the space test: A1–A4 live in a POINT space too, but what
  // they are about is a curve, not a series.
  const curveSpec = CURVES[scenario.id];
  if (curveSpec) {
    const read = declared(scenario);
    return {
      make: (/** @type {HTMLCanvasElement} */ c) => curveView(c, {
        x: { label: curveSpec.xAs ?? curveSpec.x, get: read(curveSpec.x) },
        curves: curveSpec.curves.map((p) => ({ label: p.as ?? p.name, get: read(p.name), dash: p.dash })),
        yLabel: curveSpec.yLabel,
        yScale: curveSpec.yScale,
      }),
      aspect: 1.5,
      shadeOptions: /** @type {string[]} */ ([]),
      // A4 measures how close the hill-climber gets. Its second panel shows what it is doing
      // to get there: the search drawn in the space it searches, against the answer the
      // algebra gives for free. The iteration budget is read back out of the model by name —
      // A4 already records it — rather than the panel re-deriving the sweep and drifting.
      aux: scenario.id === 'a4' ? {
        title: 'the hill-climber in quantity space, against the constrained optimum',
        aspect: 1.25,
        make: (/** @type {HTMLCanvasElement} */ c) => optimiserView(c, {
          prices: [1, 1],          // as a4 sets them; equal, so the optimum is half of each
          budget: 1,
          utility: ces(0.5),
          iterationsOf: read('iterations'),
        }),
      } : undefined,
    };
  }

  if (kind === LINE) {
    return {
      make: (/** @type {HTMLCanvasElement} */ c) => lineView(c, profilesFor(world)),
      aspect: 1.45,
      shadeOptions: /** @type {string[]} */ ([]),
    };
  }

  // Two regions are two dimensionless points: there is no interior to draw, and the result
  // is prices separating and rejoining as the run threads the regime boundary.
  if (kind === TWO_REGION) {
    return {
      make: (/** @type {HTMLCanvasElement} */ c) => timeSeriesView(c, {
        tracks: [
          { label: 'price max', get: (w) => Math.max(...w.firms.map((f) => f.goodCost)) },
          { label: 'price min', get: (w) => Math.min(...w.firms.map((f) => f.goodCost)) },
          { label: 'rho', get: (w) => /** @type {any} */ (w.config.utility).rho, dash: true },
        ],
      }),
      aspect: 1.7,
      shadeOptions: /** @type {string[]} */ ([]),
    };
  }

  if (kind === POINT) {
    const spec = (TIME_SERIES[scenario.id] ?? TIME_SERIES.belt)(world, declared(scenario));
    return {
      make: (/** @type {HTMLCanvasElement} */ c) => timeSeriesView(c, spec),
      aspect: 1.7,
      shadeOptions: /** @type {string[]} */ ([]),
    };
  }

  // Overlays answer a specific question and are off by default (`plane.js`). The equalising
  // line is offered only where its closed form exists — one Firm, and a density cost to
  // trade against it — because on any other model the ring it draws would be meaningless.
  const overlays = [{ key: 'showBuyLines', label: 'buy lines' }];
  if (equalisingLineApplies(world)) {
    overlays.push({ key: 'showEqualisingLine', label: 'equalising cutoff' });
  }
  // The white ring from H1's source video. Offered only there: on any other model the mean of
  // every actor is just a point, with nothing to explain.
  if (scenario.id === 'h1') {
    overlays.push({ key: 'showMeanPoint', label: '2011 ring (mean of all actors)' });
  }

  const planeCurve = PLANE_CURVES[scenario.id];
  const readPlane = planeCurve ? declared(scenario) : null;

  // H1's two Firms are told apart by the original's two glyphs, white and filled, so its page
  // can call them the white Firm and the black and its charts can match. Every other model
  // draws its Firms white, as the published figures do.
  const firmFills = scenario.id === 'h1'
    ? FIRM_NAMES.map((name) => (name === 'black' ? '#000' : '#fff'))
    : undefined;

  return {
    make: (/** @type {HTMLCanvasElement} */ c) => planeView(c, { firmFills }),
    aspect: 1,
    shadeOptions: ['auto', 'density', 'utility', 'neighbours', 'group'],
    overlays,
    aux: AUX[scenario.id] ?? (planeCurve && readPlane ? {
      title: planeCurve.title,
      aspect: 1.6,
      make: (/** @type {HTMLCanvasElement} */ c) => curveView(c, {
        x: { label: planeCurve.xAs ?? planeCurve.x, get: readPlane(planeCurve.x) },
        curves: planeCurve.curves.map((s) => ({
          label: s.as ?? s.name, get: readPlane(s.name), dash: s.dash,
        })),
        yLabel: planeCurve.yLabel,
      }),
    } : undefined),
  };
}

/** H1's Firms by the name the page gives them. @param {number} g */
const h1Firm = (g) => `${FIRM_NAMES[g]} Firm`;
/** The white Firm's lines are drawn hollow, as the plane draws its glyph. @param {number} g */
const h1Hollow = (g) => FIRM_NAMES[g] === 'white';

/**
 * Second panels. A model earns one when its result is a *relationship* between two things the
 * main view can only show one of at a time.
 *
 * E8 is the case that needs it. The plane shows People migrating; a time series would show
 * production concentrating; neither shows that these are one event rather than two that
 * happen to coincide. The arrow scatter does, because the track sits still for two thousand
 * days and then turns a corner.
 *
 * @type {Record<string, {make: (c: HTMLCanvasElement) => {draw: (w: import('../world.js').World) => void}, aspect: number, title: string}>}
 */
const AUX = {
  e8: {
    title: 'production ratio against mean distance to Firm 0',
    aspect: 1.35,
    make: (c) => correlateView(c, {
      x: {
        label: 'mean distance to Firm 0',
        get: (w) => w.people.reduce((a, p) => a + w.distance(p, w.firm(0)), 0) / Math.max(w.people.length, 1),
      },
      // The published statistic: total output, not output per unit. CODE_ANATOMY §3.2.
      y: { label: 'production ratio', get: productionRatio },
    }),
  },

  // H1: the plane shows where the two settlements are at one moment. What it cannot show is
  // why they keep meeting and parting, which takes a time axis and the quantities each side
  // acts on: the People's utility, and the Firms' revenue — the only thing a Firm can see —
  // with the speed its crawl moves at, over the separation they all produce. One panel, so
  // a breakaway lines up as one event in every strip. A rolling window, because the run
  // never ends and the cycle is what matters, not its history.
  h1: {
    title: 'Why they meet and part: the same days in every strip. Grey bands are the spells apart.',
    aspect: 1.55,
    make: (c) => stripsView(c, {
      window: 1500,
      shade: (w) => settlements(w).separation > APART,
      strips: [
        // One strip per workforce, each on its own scale. The circles' utility is about
        // twice the squares', and everyone in a workforce gets nearly the same as the rest
        // of it, so on a shared axis the squares' swings flatten and the faint lines vanish
        // under the means.
        {
          title: `utility, circles (${h1Firm(1)}, wage 2): each Person faint, mean bold`,
          cloud: (w) => workforce(w, 1).map((p) => p.utility),
          tracks: [{ label: 'mean', get: (w) => settlements(w).groups[1].utility }],
        },
        {
          title: `utility, squares (${h1Firm(0)}, wage 1), on their own scale`,
          cloud: (w) => workforce(w, 0).map((p) => p.utility),
          tracks: [{ label: 'mean', get: (w) => settlements(w).groups[0].utility }],
        },
        // Revenue is the goods sold, too: stock is unlimited and the base price is 1. The
        // dashed lines are both the circles' purchases, one at each Firm: together they split
        // their custom evenly, apart most of it goes to their own. The squares' part of each
        // Firm's revenue is the gap between its solid line and its dashed one. The white
        // Firm's lines go first, so the black ones cross over them.
        {
          title: 'Firm revenue (= goods sold); dashed, what the circles bought',
          weight: 1.25,
          tracks: [
            { label: h1Firm(0), get: (w) => w.firm(0).revenueLastTurn, hollow: h1Hollow(0) },
            { label: h1Firm(1), get: (w) => w.firm(1).revenueLastTurn, hollow: h1Hollow(1) },
            { label: `circles at ${FIRM_NAMES[0]}`, get: (w) => sales(w)[0][1], dash: true, hollow: h1Hollow(0) },
            { label: `circles at ${FIRM_NAMES[1]}`, get: (w) => sales(w)[1][1], dash: true, hollow: h1Hollow(1) },
          ],
        },
        {
          title: 'how fast each Firm is moving (net, over five days)',
          weight: 0.8,
          includeZero: true,
          tracks: [
            { label: h1Firm(0), get: (w) => firmSpeed(w, 0), hollow: h1Hollow(0) },
            { label: h1Firm(1), get: (w) => firmSpeed(w, 1), hollow: h1Hollow(1) },
          ],
        },
        // Log, because touching (1) and engulfed (below 0.35) are the values that matter and
        // a spell apart reaches 9: on a linear axis both would sit in the bottom few pixels.
        {
          title: 'separation, in units of their own size (log scale)',
          log: true,
          reference: 1,
          referenceLabel: 'touching',
          tracks: [{ label: 'separation', get: (w) => settlements(w).separation }],
        },
      ],
    }),
  },
};
