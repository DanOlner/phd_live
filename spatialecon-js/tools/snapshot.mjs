// @ts-check

/**
 * Render a model to SVG, headless.
 *
 * Not a second renderer — that would be a maintenance liability and would prove nothing. It
 * is a fake Canvas 2-D context that records the calls the real view makes, then replays them
 * as SVG. So the picture is produced by `src/view/plane.js` itself, and if the browser
 * shows something different, this is wrong rather than merely out of date.
 *
 * Useful for three things: eyeballing a model without a browser, generating the stills for
 * the model index, and diffing a view change over time.
 *
 *   node tools/snapshot.mjs c1                       # -> snapshots/c1.svg
 *   node tools/snapshot.mjs c8 --days 2900 --shade utility
 *   node tools/snapshot.mjs e8 --aux                 # the second panel
 *   node tools/snapshot.mjs c1 --thumb               # -> snapshots/thumbs/c1.svg
 *   node tools/snapshot.mjs --sweep e3-rho           # a precomputed sweep, same renderer
 *   node tools/snapshot.mjs c1 --out docs/c1.svg
 */

import { mkdir, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';

import { readFileSync } from 'node:fs';

import { get } from '../src/scenario.js';
import { chooseView } from '../src/view/choose.js';
import { sweepView } from '../src/view/sweep.js';
import { World } from '../src/world.js';
import '../src/scenarios/index.js';

const SIZE = 900;
const THUMB_SIZE = 340;

const args = process.argv.slice(2);
/** @param {string} flag @param {string | null} fallback */
const arg = (flag, fallback) => {
  const i = args.indexOf(flag);
  return i >= 0 && i + 1 < args.length ? args[i + 1] : fallback;
};

/**
 * Render a precomputed sweep instead of a model.
 *
 * The same reason snapshot exists at all: these charts ship to the site, and being able to
 * look at one without a browser is what catches a squashed axis or a label off the edge
 * before a visitor does. It goes through `sweepView`, so it is the site's own renderer.
 */
const sweepId = arg('--sweep', null);

const id = args.find((a) => !a.startsWith('--') && args[args.indexOf(a) - 1]?.startsWith('--') !== true)
  ?? args.find((a) => !a.startsWith('--'));
if (!id && !sweepId) {
  console.error('usage: node tools/snapshot.mjs <model> [--days N] [--shade density|utility|neighbours] [--aux] [--out FILE]');
  process.exit(2);
}
/** Render the model's second panel instead of its main view, where it has one (e8). */
const wantsAux = args.includes('--aux');

/**
 * Thumbnail mode, for the catalogue's stills: smaller, no caption, integer coordinates.
 *
 * The precision is the point. These files are vector, so their size is set by the number of
 * shapes and the length of each coordinate, not by the pixel dimensions — a 500-Person plane
 * is a thousand shapes whatever size it is drawn at, and E3's time series is a single path
 * with six thousand points. At two decimal places a full set of stills is several megabytes
 * on one page. At integer precision, on a canvas small enough that a whole pixel is the
 * resolution anyway, it is a fifth of that and looks the same.
 */
const thumb = args.includes('--thumb');

const scenario = sweepId ? null : get(id);
const days = Number(arg('--days', sweepId ? '0' : String(scenario.defaultDays ?? 1000)));
// 'auto' so a snapshot shows what the page shows: group markers on the von Thünen models,
// the density ramp elsewhere.
const shadeBy = /** @type {'auto'|'density'|'utility'|'neighbours'|'group'} */ (arg('--shade', 'auto'));
const fallbackOut = thumb
  ? `snapshots/thumbs/${id}.svg`
  : `snapshots/${id}${wantsAux ? '-aux' : ''}.svg`;
const out = resolve(arg('--out', fallbackOut) ?? fallbackOut);

/** A recording 2-D context: captures geometry *and* the style live at each call. */
function recordingContext() {
  /** @type {string[]} */
  const svg = [];
  /** @type {string[]} */
  let path = [];

  // Just enough of a transform stack for the rotated axis labels: the views only ever
  // translate then rotate, so a triple is sufficient and an SVG transform can carry it.
  let state = { tx: 0, ty: 0, rot: 0 };
  /** @type {{tx: number, ty: number, rot: number}[]} */
  const stack = [];

  // Dashes carry meaning in these charts — the closed form over the computed curve in A1, the
  // deduced clearing price in E1 — so they have to survive into the SVG, not just the canvas.
  /** @type {number[]} */
  let dash = [];

  const ctx = {
    fillStyle: '#000', strokeStyle: '#000', lineWidth: 1,
    font: '12px monospace', textBaseline: 'middle', textAlign: 'left',
    // Every view calls this exactly once, first thing, so it is a reliable frame boundary —
    // and dropping the previous frame here is what lets the accumulating views be drawn on
    // every day of the run (which is how they build their picture) while the file still
    // holds only the last frame.
    setTransform() { svg.length = 0; state = { tx: 0, ty: 0, rot: 0 }; stack.length = 0; },
    save() { stack.push({ ...state }); },
    restore() { state = stack.pop() ?? { tx: 0, ty: 0, rot: 0 }; },
    /** @param {number} x @param {number} y */
    translate(x, y) { state.tx += x; state.ty += y; },
    /** @param {number} radians */
    rotate(radians) { state.rot += radians; },
    /** @param {string} text @param {number} x @param {number} y */
    fillText(text, x, y) {
      const px = Number.parseFloat(ctx.font) || 12;
      // 'right' was missing here, so every right-aligned label — the whole y-axis tick
      // column — rendered left-aligned and sat across the axis rule.
      const anchor = { center: 'middle', right: 'end' }[ctx.textAlign] ?? 'start';
      const transform = state.tx || state.ty || state.rot
        ? ` transform="translate(${n(state.tx)} ${n(state.ty)}) rotate(${n((state.rot * 180) / Math.PI)})"`
        : '';
      svg.push(`<text x="${n(x)}" y="${n(y)}"${transform} font-family="ui-monospace, Menlo, monospace" `
        + `font-size="${n(px)}" text-anchor="${anchor}" dominant-baseline="middle" fill="${ctx.fillStyle}">`
        + `${text.replace(/[<&>]/g, (c) => ({ '<': '&lt;', '&': '&amp;', '>': '&gt;' })[c] ?? c)}</text>`);
    },
    /** @param {number} x @param {number} y @param {number} w @param {number} h */
    fillRect(x, y, w, h) {
      svg.push(`<rect x="${n(x)}" y="${n(y)}" width="${n(w)}" height="${n(h)}" fill="${ctx.fillStyle}"/>`);
    },
    /** @param {number} x @param {number} y @param {number} w @param {number} h */
    strokeRect(x, y, w, h) {
      svg.push(`<rect x="${n(x)}" y="${n(y)}" width="${n(w)}" height="${n(h)}" fill="none" stroke="${ctx.strokeStyle}" stroke-width="${n(ctx.lineWidth)}"/>`);
    },
    /** @param {number[]} pattern */
    setLineDash(pattern) { dash = pattern ?? []; },
    beginPath() { path = []; },
    /** @param {number} x @param {number} y */
    moveTo(x, y) { path.push(`M${n(x)} ${n(y)}`); },
    /** @param {number} x @param {number} y */
    lineTo(x, y) {
      // A repeated identical command is a no-op, and at thumbnail precision a six-thousand
      // point time series collapses to a few hundred distinct ones. Dropping them is what
      // makes a still small enough to put twenty-nine of on one page.
      const command = `L${n(x)} ${n(y)}`;
      if (path[path.length - 1] !== command) path.push(command);
    },
    closePath() { path.push('Z'); },
    /** @param {number} x @param {number} y @param {number} r */
    arc(x, y, r) {
      // Every arc the view draws is a full circle.
      path.push(`M${n(x - r)} ${n(y)}a${n(r)} ${n(r)} 0 1 0 ${n(2 * r)} 0a${n(r)} ${n(r)} 0 1 0 ${n(-2 * r)} 0`);
    },
    fill() {
      if (path.length) svg.push(`<path d="${path.join('')}" fill="${ctx.fillStyle}"/>`);
    },
    stroke() {
      if (path.length) {
        const dashes = dash.length ? ` stroke-dasharray="${dash.map(n).join(' ')}"` : '';
        svg.push(`<path d="${path.join('')}" fill="none" stroke="${ctx.strokeStyle}" stroke-width="${n(ctx.lineWidth)}"${dashes}/>`);
      }
    },
  };
  return { ctx, svg };
}

/** @param {number} v */
function n(v) {
  return thumb ? Math.round(v) : Math.round(v * 100) / 100;
}

const { ctx, svg } = recordingContext();
globalThis.window = /** @type {any} */ ({ devicePixelRatio: 1 });

/** @param {number} width @param {number} height */
const fakeCanvas = (width, height) => /** @type {any} */ ({
  width, height, clientWidth: width, clientHeight: height, getContext: () => ctx,
});

/** @param {string} body @param {number} width @param {number} height @param {string} [label] */
function document_(body, width, height, label) {
  const captionRow = label ? 26 : 0;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height + captionRow}" viewBox="0 0 ${width} ${height + captionRow}">
<rect width="100%" height="100%" fill="#fff"/>
${body}${label ? `
<text x="8" y="${height + 18}" font-family="ui-monospace, Menlo, monospace" font-size="12" fill="#666">${label}</text>` : ''}
</svg>
`;
}

// A precomputed sweep short-circuits everything below: there is no model to run, only a JSON
// file to paint.
if (sweepId) {
  const doc = JSON.parse(readFileSync(`data/sweeps/${sweepId}.json`, 'utf8'));
  const width = SIZE;
  const height = Math.round(width / 1.7);
  const view = sweepView(fakeCanvas(width, height), doc);
  view.draw();
  const target = resolve(arg('--out', `snapshots/sweeps/${sweepId}.svg`) ?? '');
  await mkdir(dirname(target), { recursive: true });
  await writeFile(target, document_(svg.join('\n'), width, height,
    `${doc.id} · ${doc.title} · ${doc.points} points`));
  console.log(`wrote ${target}  (${svg.length} shapes)`);
  process.exit(0);
}

const config = scenario.config();
const world = new World(config);
scenario.setUp?.(world);

// The page's own rule, not a copy of it. Snapshot used to decide for itself which view a
// model got, which meant every new view had to be added in two places and the two could
// silently disagree about what a model looks like.
const chosen = chooseView(scenario, world);
const panel = wantsAux ? chosen.aux : chosen;
if (wantsAux && !chosen.aux) {
  console.error(`${id} has no second panel`);
  process.exit(2);
}

const W = thumb ? THUMB_SIZE : SIZE;
const H = Math.round(W / (panel.aspect || 1));
const canvas = fakeCanvas(W, H);
const view = panel.make(canvas);
if ('shadeBy' in view) view.shadeBy = shadeBy;
// The density rings are half the shapes in a 500-Person plane and read as grey mush at
// thumbnail size. The markers alone still show the shape of the settlement, which is all a
// card has room to say.
if (thumb && 'showDensityCircles' in view) view.showDensityCircles = false;

// Optional layers, by the same keys the page's checkboxes set.
for (const key of (arg('--overlays', '') ?? '').split(',').map((k) => k.trim()).filter(Boolean)) {
  if (!(key in view)) {
    console.error(`${id} has no overlay "${key}" (available: ${(chosen.overlays ?? []).map((o) => o.key).join(', ') || 'none'})`);
    process.exit(2);
  }
  view[key] = true;
}

// Accumulating views (the time series, the curve, the arrow scatter) build their picture
// from being drawn as the run goes, so they are drawn every day rather than once at the end.
// The plane holds no history and only needs the final frame. A view that can take its sample
// without drawing (H1's strips) is only asked to, and drawn once below.
const accumulates = typeof (/** @type {any} */ (view).reset) === 'function';
const observe = /** @type {any} */ (view).observe;

process.stdout.write(`${id}: running ${days} days`);
for (let d = 0; d < days; d++) {
  scenario.beforeDay?.(world, d);
  world.timeline.nextDay();
  if (typeof observe === 'function') observe(world);
  else if (accumulates) view.draw(world);
}
console.log(` — done`);
view.draw(world);

const shading = 'shadeBy' in view ? ` · shaded by ${shadeBy}` : '';
const caption = `${scenario.id} · ${scenario.title} · ${scenario.thesisRef} · day ${world.day}${shading}`;
const doc = document_(svg.join('\n'), W, H, thumb ? '' : caption);

await mkdir(dirname(out), { recursive: true });
await writeFile(out, doc);
console.log(`wrote ${out}  (${svg.length} shapes)`);
