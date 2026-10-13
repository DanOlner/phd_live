// @ts-check

/**
 * Precompute the parameter sweeps, and write one small JSON per figure.
 *
 * Decision 1 (BUILD_PLANNER §9): sweeps ship as *data*, not as something a visitor's browser
 * re-runs. The point of a swept figure is to illustrate what the sweep found; re-running
 * eight thousand days of E8 in a phone browser buys nothing a chart does not, and costs a
 * progress bar, a worker thread and a lot of goodwill.
 *
 * What ships instead: a committed JSON per sweep, rendered by `src/view/sweep.js` through the
 * *same* `paintCurves` the live charts use — so a swept figure and the model running below it
 * are visibly siblings — plus a link from every chart to this file and to the raw numbers.
 *
 *   node tools/sweep.mjs             # regenerate everything into data/sweeps/
 *   node tools/sweep.mjs --list
 *   node tools/sweep.mjs e3-rho      # just one
 *
 * Each sweep runs the *registered scenario*, not a private copy of it. Where a scenario ramps
 * a parameter itself, the sweep holds that parameter still through the same `Parameter` seam
 * the page's sliders use and runs one settled model per point; where the ramp *is* the figure
 * (E8), the sweep records the ramp. Both are stated per entry, because they are different
 * claims: one is "the model settles here for each value", the other "this is what one run
 * did on the way through".
 */

import { mkdir, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';

import { get } from '../src/scenario.js';
import { equalisingCutoff } from '../src/view/equalise.js';
import { World } from '../src/world.js';
import { productionRatio } from '../src/scenarios/e8.js';
import { DEFAULTS, tomahawk } from '../src/coremodel.js';
import '../src/scenarios/index.js';

const OUT = resolve('data/sweeps');

/**
 * Build a world for a scenario, optionally overriding config fields, and run it.
 * @param {string} id @param {object} [overrides] @param {number} [days]
 * @param {(world: World, day: number) => void} [each]
 */
function run(id, overrides = {}, days = 0, each) {
  const scenario = get(id);
  const config = scenario.config();
  Object.assign(config, overrides);
  const world = new World(config);
  scenario.setUp?.(world);
  const total = days || scenario.defaultDays || 1000;
  for (let d = 0; d < total; d++) {
    each?.(world, d);
    world.timeline.nextDay();
  }
  return world;
}

/** A scenario's exposed parameter, by name. @param {string} id @param {World} world @param {string} name */
function control(id, world, name) {
  const p = (get(id).controls?.(world) ?? []).find((c) => c.name === name);
  if (!p) throw new Error(`${id} exposes no parameter '${name}'`);
  return p;
}

/** Pearson correlation. @param {number[]} a @param {number[]} b */
function correlation(a, b) {
  const n = a.length;
  const ma = a.reduce((s, v) => s + v, 0) / n;
  const mb = b.reduce((s, v) => s + v, 0) / n;
  let num = 0, da = 0, db = 0;
  for (let i = 0; i < n; i++) {
    num += (a[i] - ma) * (b[i] - mb);
    da += (a[i] - ma) ** 2;
    db += (b[i] - mb) ** 2;
  }
  return da === 0 || db === 0 ? 0 : num / Math.sqrt(da * db);
}

/** @param {number} from @param {number} to @param {number} steps */
function span(from, to, steps) {
  return Array.from({ length: steps }, (_, i) => from + (i / (steps - 1)) * (to - from));
}

/**
 * @typedef {object} Sweep
 * @property {string} id @property {string} title
 * @property {string|string[]} model which model pages this figure belongs on
 * @property {string} kind 'settled' — one run per point — or 'trace', one run recorded through
 * @property {string} note what the figure is claiming
 * @property {{at: number, label: string}} [mark]
 * @property {'linear'|'log'} [yScale]
 * @property {() => Promise<{xLabel: string, xs: number[], series: {label: string, values: number[], dash?: boolean}[], yLabel?: string}>} compute
 */

/** @type {Sweep[]} */
const SWEEPS = [
  {
    id: 'e3-rho',
    title: 'Where the production regime breaks',
    model: 'e3',
    kind: 'settled',
    yScale: 'log',
    mark: { at: 0.75, label: 'thesis ~0.75' },
    note: `One settled run per value of rho, with rho held still rather than ramped: each
point answers "where does this model end up if love of variety is fixed here?". Below the
threshold the two Firms produce symmetrically and the efficiency ratio sits at exactly 1;
above it one Firm takes over.

Holding rho still is the whole reason this sweep is worth precomputing, and it sharpens the
result. The efficiency ratio is 1.000 to three decimals up to rho = 0.73, first departs at
0.74, and is clearly gone by 0.75 — where the thesis puts the break. The live model ramps
rho continuously and breaks at 0.766, and the Java's ramped run at 0.764. Both ramps lag,
which is what a ramp through a threshold does. So the ramp tells you the model shifts and
this tells you where.

Log axis, because the loser's price runs away. Once symmetry breaks, the winner's price
falls to 0.5 — the one-Firm clearing price — while the loser, unable to reach its stock
target at any demand it can still attract, compounds its own error upward past 30. That is
the runaway of §6.3.2, and on a linear axis it flattens everything else to nothing.`,
    async compute() {
      const xs = span(0.60, 0.90, 31);
      const ratio = [], priceLo = [], priceHi = [];
      for (const rho of xs) {
        const scenario = get('e3');
        const world = new World(scenario.config());
        scenario.setUp?.(world);
        control('e3', world, 'rho').set(rho);
        // No beforeDay: rho is held, not ramped. 4,000 days is comfortably past settling —
        // the live ramp crosses in a few hundred once it reaches the threshold.
        for (let d = 0; d < 4000; d++) world.timeline.nextDay();
        const eff = world.firms.map((f) => f.outputPerUnitTime);
        const price = world.firms.map((f) => f.goodCost);
        ratio.push(Math.min(...eff) > 0 ? Math.max(...eff) / Math.min(...eff) : 1);
        priceLo.push(Math.min(...price));
        priceHi.push(Math.max(...price));
      }
      return {
        xLabel: 'rho (1 = perfect substitutes)',
        xs,
        yLabel: 'ratio, price',
        series: [
          { label: 'efficiency ratio', values: ratio },
          { label: 'price max', values: priceHi, dash: true },
          { label: 'price min', values: priceLo, dash: true },
        ],
      };
    },
  },

  {
    id: 'c1-fringe',
    title: 'The fringe thins with numbers',
    model: 'c1',
    kind: 'settled',
    note: `One settled run per population. The share of People beyond the equalising cutoff
d*, where density has fallen to zero and there is nothing left to trade against distance.

They are not out of equilibrium — no move improves them. They are outside the range over
which equalisation is possible, which is a finite-population effect and must therefore
vanish with numbers. It does. The Java measured 43.2 / 20.4 / 7.2 / 1.9 per cent at
125 / 250 / 500 / 1000 People, from an independent implementation.`,
    yScale: 'log',
    async compute() {
      const xs = [125, 250, 500, 1000, 2000];
      const fringe = [], cutoff = [], level = [];
      for (const n of xs) {
        const world = run('c1', { peopleCount: n }, 1500);
        const found = equalisingCutoff(world);
        fringe.push(found ? found.fraction * 100 : NaN);
        cutoff.push(found ? found.cutoff : NaN);
        level.push(found ? found.level : NaN);
      }
      return {
        xLabel: 'People',
        xs,
        yLabel: 'per cent, distance, utility',
        series: [
          { label: 'fringe %', values: fringe },
          { label: 'cutoff d*', values: cutoff, dash: true },
          { label: 'equalised U*', values: level, dash: true },
        ],
      };
    },
  },

  {
    id: 'c8-density',
    title: 'Density cost against the settlement',
    model: ['c1', 'c8'],
    kind: 'settled',
    note: `One settled run per density cost, from C1's value of 10 up to C8's 500. The
correlation between a Person's utility and their distance from the Firm is flat while the
settlement equalises and turns sharply negative as it disperses — because past the cutoff
there is no density left to give up, and a more distant Person is simply worse off.

C1 and C8 are the same model at two points on this curve, and the curve has no kink in it.

Note the flat one. Mean utility does not move across a fifty-fold rise in the density cost —
0.898 at 10, 0.882 at 500. The People absorb all of it by moving apart: mean distance from
the Firm doubles instead. Nobody is made worse off by crowding because nobody stays crowded,
which is what it means for this to be an equilibrium rather than a burden.`,
    async compute() {
      const xs = [10, 25, 50, 100, 200, 350, 500];
      const corr = [], meanU = [], meanD = [];
      for (const cost of xs) {
        const world = run('c1', { densityCost: cost }, 1500);
        const firm = world.firm(0);
        const d = world.people.map((p) => world.distance(p, firm));
        const u = world.people.map((p) => p.utility);
        corr.push(correlation(d, u));
        meanU.push(u.reduce((s, v) => s + v, 0) / u.length);
        meanD.push(d.reduce((s, v) => s + v, 0) / d.length);
      }
      return {
        xLabel: 'density cost',
        xs,
        yLabel: 'correlation, utility, distance',
        series: [
          { label: 'corr(dist, utility)', values: corr },
          { label: 'mean utility', values: meanU, dash: true },
          { label: 'mean distance', values: meanD, dash: true },
        ],
      };
    },
  },

  {
    id: 'd1-distance',
    title: 'Two Firms on a line: separation, and how reliably',
    // All four variants: the figure sweeps distance cost at both values of rho, so its four
    // ends are exactly the four cases, and it belongs on each of their pages.
    model: ['d1a', 'd1b', 'd1c', 'd1d'],
    kind: 'settled',
    note: `Distance cost against the separation the two Firms end up at, for both values of
rho the thesis uses — so the four corners of the d1a–d1d table are the four ends of these
curves.

Two things had to be got right for this figure to mean anything, and both are the thesis'
own point about these models rather than measurement fussiness.

The Firms never settle: the crawl always perturbs, so a Firm that has found a good spot
keeps testing and keeps giving ground. Reading the separation off the final day is therefore
one draw of a wandering process. What is plotted is the separation averaged over the second
half of each run, and averaged again over several seeds.

And the spread is not error — it is the result. The dashed curves are how much the
separation wanders *within* a run, and at rho = 0.99 the wandering is worst exactly where
the separation is smallest: 0.155 at a distance cost of 1, falling to 0.07 by 20. That is
the §5.5.2 finding in one number. When goods are near-perfect substitutes and distance is
cheap, a Firm's position barely affects its revenue, so the crawl has almost no signal to
follow and the Firms roam. Raising the distance cost sharpens the signal, and they both
separate further and hold position better. The weak-incentive corner is the unreliable one.

The four corners of these curves are the four cases d1a–d1d: locked centrally together at
low rho and cheap distance, roaming at high rho and cheap distance, spread at low rho and
dear distance, and the Hotelling-like split at high rho and dear distance — 0.376 here
against 0.343 in the independently-built Java.

Distance cost 0 is left out. With no distance cost a Firm's location cannot affect its
revenue at all, so the crawl gets no signal, nothing moves, and the "result" is just where
the Firms were placed.`,
    async compute() {
      const xs = [1, 2, 5, 10, 20, 30, 40];
      const SEEDS = 4;
      const HALF = 2000; // days averaged over, from day 2000 to 4000
      /** @type {Record<string, number[]>} */
      const out = { lowMean: [], lowSd: [], highMean: [], highSd: [] };

      for (const cost of xs) {
        for (const [rho, keyMean, keySd] of /** @type {[number, string, string][]} */ ([
          [0.1, 'lowMean', 'lowSd'], [0.99, 'highMean', 'highSd'],
        ])) {
          const means = [], sds = [];
          for (let seed = 0; seed < SEEDS; seed++) {
            const scenario = get('d1d');
            const config = scenario.config();
            config.deliveryCost = cost;
            config.seed = seed;
            const world = new World(config);
            scenario.setUp?.(world);
            control('d1d', world, 'rho').set(rho);

            /** @type {number[]} */
            const seen = [];
            for (let d = 0; d < 2 * HALF; d++) {
              world.timeline.nextDay();
              if (d >= HALF) {
                seen.push(Math.abs(world.firm(0).position.x - world.firm(1).position.x)
                  / world.space.width);
              }
            }
            const m = seen.reduce((s, v) => s + v, 0) / seen.length;
            means.push(m);
            sds.push(Math.sqrt(seen.reduce((s, v) => s + (v - m) ** 2, 0) / seen.length));
          }
          out[keyMean].push(means.reduce((s, v) => s + v, 0) / SEEDS);
          out[keySd].push(sds.reduce((s, v) => s + v, 0) / SEEDS);
        }
      }

      return {
        xLabel: 'distance cost',
        xs,
        yLabel: 'separation, as a fraction of the line',
        series: [
          { label: 'sep, rho 0.99', values: out.highMean },
          { label: 'wander, rho 0.99', values: out.highSd, dash: true },
          { label: 'sep, rho 0.1', values: out.lowMean },
          { label: 'wander, rho 0.1', values: out.lowSd, dash: true },
        ],
      };
    },
  },

  {
    id: 'g1-tomahawk',
    title: 'The tomahawk: every equilibrium at every transport cost',
    model: 'g1',
    kind: 'settled',
    note: `The canonical result of geographical economics, and the figure the whole thesis is
measured against. Five hundred transport costs, five hundred splits of the workforce at each,
and every point where neither region pays a better real wage.

Black is stable, grey unstable. Read it from the right. At high transport costs the only
equilibrium is the symmetric one — half the manufacturing in each region — and it is stable.
Lower the cost and at a break point that symmetric equilibrium turns unstable while two new
stable branches peel away to the extremes: everything in one region or the other. The handle
and the two blades are what give it its name.

A scatter rather than a curve, and it has to be: at one transport cost there may be one
equilibrium or three, so there is no function to draw and joining the points in order would
invent branches that do not exist.

What the Core Model cannot do is the reason for the rest of the catalogue. The elasticity e
is love of variety and economies of scale at once, inseparably; and a region here is a
dimensionless point, so production structure cannot vary within one. Compare e3, e5 and e8.`,
    async compute() {
      const points = tomahawk(DEFAULTS, 1, 3.5, 500, 500);
      return {
        xLabel: 'transport cost T',
        yLabel: 'lambda at equilibrium',
        xs: points.map((p) => p.t),
        series: [],
        scatter: points.map((p) => ({ x: p.t, y: p.lambda, stable: p.stable })),
      };
    },
  },

  {
    id: 'c3-costs',
    title: 'Spatial against non-spatial costs',
    model: ['c3w', 'c3g', 'c3c', 'c3d'],
    kind: 'settled',
    note: `One settled run per point, for each of the four costs, against the mean distance
People end up from the centre. Each cost is swept across its own range and the x axis is that
range normalised to 0–1, because the four have nothing else in common.

The result is a set of SIGNS, not magnitudes:

  wage             non-spatial   agglomerating
  base good cost   non-spatial   DISPERSIVE
  commute cost     spatial       agglomerating
  goods distance   spatial       agglomerating

Non-spatial costs enter the closed form as constants — the wage on top, the base price in the
denominator of U = (w − D(d)) / (p + cd) — and change how much the distance term is worth
relative to the crowding term. Raise the wage and crowding is cheap in proportion, so People
accept more of it and pack in. Raise the base price and the cd term matters proportionally
less, so distance stops being worth paying to avoid. A spatial cost is multiplied by distance
and simply makes distance dearer, which pulls inward every time.

Why this licenses the whole of chapter 6: if a wage and a goods price reach location through
the same channel, differing only in whether they are multiplied by distance, then there is no
need to model a labour market and a goods market as separate things. That collapse is exactly
what the transmission belt performs.

C3's config is lost; this is reconstructed from the thesis text on C1's population and space.`,
    async compute() {
      const steps = 9;
      const xs = Array.from({ length: steps }, (_, i) => i / (steps - 1));
      /** @type {Record<string, number[]>} */
      const out = { wage: [], goodCost: [], commute: [], delivery: [] };
      /** @type {[string, string, number, number][]} */
      const sweeps = [
        ['c3w', 'wage', 0.05, 1.5],
        ['c3g', 'goodCost', 0.05, 2.0],
        ['c3c', 'commute', 0, 1.0],
        ['c3d', 'delivery', 0, 1.0],
      ];
      for (const [id, key, from, to] of sweeps) {
        for (const x of xs) {
          const value = from + x * (to - from);
          const scenario = get(id);
          const config = scenario.config();
          if (key === 'wage') config.wage = value;
          else if (key === 'goodCost') config.baseGoodCost = value;
          else if (key === 'commute') config.commuteCost = value;
          else config.deliveryCost = value;
          const world = new World(config);
          scenario.setUp?.(world);
          // No beforeDay: the cost is HELD, so each point is a settled model rather than a
          // moment in a ramp.
          for (let d = 0; d < 1200; d++) world.timeline.nextDay();
          const firm = world.firm(0);
          out[key].push(
            world.people.reduce((a, p) => a + world.distance(p, firm), 0) / world.people.length);
        }
      }
      return {
        xLabel: 'each cost across its own range, normalised',
        yLabel: 'mean distance from the centre',
        xs,
        series: [
          { label: 'wage', values: out.wage },
          { label: 'base good cost', values: out.goodCost, dash: true },
          { label: 'commute', values: out.commute },
          { label: 'goods distance', values: out.delivery, dash: true },
        ],
      };
    },
  },

  {
    id: 'e8-rho',
    title: 'The closing result, as rho rises',
    model: 'e8',
    kind: 'trace',
    // Log, because the two quantities differ by an order of magnitude and both are the point.
    // On a linear axis the production ratio's climb to 33 flattens mean distance into the
    // baseline, so the migration — which is half the finding — reads as nothing happening.
    yScale: 'log',
    mark: { at: 0.88, label: 'transition' },
    note: `NOT one run per point. This is a single run of E8 recorded on the way through, as
rho is raised from 0.5 to 0.9 — which is what the published figure is, and the reason the
shape matters more than any individual value.

The production ratio sits at 1.0 for two-thirds of the run and then goes near-vertical. Mean
distance to Firm 0 moves at the same moment, because the People migrating and the production
concentrating are one event.

The near-vertical section at the right-hand edge is not a plotting artefact. The ramp stops
at rho = 0.9 and the run continues for another twelve hundred days, during which the
transition finishes — so those points share an x and differ in y. It is a trace of a run,
not a function of rho, and this is where the difference shows.

Which Firm wins is chance. Rerun the live model and the ratio may invert; the shape will not.`,
    async compute() {
      const scenario = get('e8');
      const xs = [], ratio = [], distance = [];
      const world = run('e8', {}, scenario.defaultDays, (w, day) => {
        scenario.beforeDay?.(w, day);
        if (day % 20 !== 0) return;
        const utility = /** @type {any} */ (w.config.utility);
        xs.push(utility.rho);
        ratio.push(productionRatio(w));
        distance.push(w.people.reduce((s, p) => s + w.distance(p, w.firm(0)), 0) / w.people.length);
      });
      void world;
      return {
        xLabel: 'rho (1 = perfect substitutes)',
        xs,
        yLabel: 'ratio, distance',
        series: [
          { label: 'production ratio', values: ratio },
          { label: 'mean distance', values: distance, dash: true },
        ],
      };
    },
  },
];

// --- driver ---------------------------------------------------------------------------

const args = process.argv.slice(2);
if (args.includes('--list')) {
  for (const s of SWEEPS) console.log(`${s.id.padEnd(14)} ${s.model.padEnd(5)} ${s.title}`);
  process.exit(0);
}

const wanted = args.filter((a) => !a.startsWith('--'));
const todo = wanted.length ? SWEEPS.filter((s) => wanted.includes(s.id)) : SWEEPS;
if (todo.length === 0) {
  console.error(`no such sweep. Known: ${SWEEPS.map((s) => s.id).join(', ')}`);
  process.exit(2);
}

await mkdir(OUT, { recursive: true });
/** @type {{id: string, title: string, model: string, points: number, seconds: string}[]} */
const index = [];

for (const sweep of todo) {
  process.stdout.write(`${sweep.id}: `);
  const started = process.hrtime.bigint();
  const computed = await sweep.compute();
  const seconds = Number(process.hrtime.bigint() - started) / 1e9;

  const doc = {
    id: sweep.id,
    title: sweep.title,
    model: sweep.model,
    kind: sweep.kind,
    note: sweep.note,
    mark: sweep.mark,
    yScale: sweep.yScale ?? 'linear',
    ...computed,
    // Provenance travels with the numbers: a chart on the site links back to both.
    generatedBy: 'tools/sweep.mjs',
    points: computed.xs.length,
  };
  await writeFile(resolve(OUT, `${sweep.id}.json`), `${JSON.stringify(doc, null, 1)}\n`);
  console.log(`${computed.xs.length} points in ${seconds.toFixed(1)}s`);
  index.push({
    id: sweep.id, title: sweep.title, model: sweep.model,
    points: computed.xs.length, seconds: seconds.toFixed(1),
  });
}

// Only rewrite the manifest when everything was regenerated, so running one sweep does not
// silently drop the others from the site.
if (wanted.length === 0) {
  await writeFile(resolve(OUT, 'index.json'), `${JSON.stringify(index, null, 1)}\n`);
  console.log(`\nwrote ${index.length} sweeps + index.json to data/sweeps/`);
}
