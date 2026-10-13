// @ts-check

/**
 * Function plots. Group A — the four models that are about a *function*, not a space.
 *
 * A1 sweeps ρ and asks what a fixed budget is worth; A2 sweeps one Firm's price and asks what
 * happens to demand for the other's; A3 sweeps labour input through two production functions;
 * A4 sweeps the hill-climber's iteration budget against a known answer. In every one, the
 * driving variable is a model quantity and the day number is only the mechanism that walks
 * it. Plotting these against time would put the wrong thing on the x axis: the reader wants
 * utility against ρ, not utility against day 63.
 *
 * These four had no view at all until now — they ran headless and wrote CSV, which is fine
 * for verification and useless as a page someone lands on. They are also the models where a
 * picture does the most work, because each one is carrying a single visual claim:
 *
 * - **A1** — utility climbing steeply as ρ falls, while quantity stays pinned flat.
 * - **A2** — a horizontal line. Demand for the other good does not move, at all, ever.
 * - **A3** — the Core Model curve crossing below zero, against the smooth one that does not.
 * - **A4** — most of the gain arriving in the first few iterations and the rest buying little.
 *
 * Where a closed form exists it is drawn dashed over the computed curve, so agreement is
 * something the reader sees rather than something the caption asserts. That is the same move
 * the time series makes with E1's clearing price.
 */

import { drawAxes, fitCanvas, format, logTicks, padded, spread } from './axis.js';

/** @typedef {{label: string, get: (world: import('../world.js').World) => number, dash?: boolean}} Curve */

/**
 * @param {HTMLCanvasElement} canvas
 * @param {object} spec
 * @param {Curve} spec.x the driving variable — what goes on the horizontal axis
 * @param {Curve[]} spec.curves
 * @param {string} [spec.yLabel]
 * @param {'linear'|'log'} [spec.yScale] `log` where the quantity spans orders of magnitude:
 *   A1's utility reaches 1.7e47 at ρ = 0.01 (that is 3^99, and correct), which on a linear
 *   axis flattens the entire rest of the sweep onto the x rule. A4 spans 0.2 down to 1e-8,
 *   where the interesting part — that the spread never quite reaches zero — is the small end.
 */
export function curveView(canvas, { x, curves, yLabel, yScale = 'linear' }) {
  /** @type {number[]} */
  const xs = [];
  /** @type {number[][]} */
  const ys = curves.map(() => []);
  let lastDay = -1;

  return {
    reset() {
      xs.length = 0;
      ys.forEach((s) => { s.length = 0; });
      lastDay = -1;
    },

    /** @param {import('../world.js').World} world */
    draw(world) {
      // Once per day, not once per frame — the sweep advances with the day, so a second
      // sample at the same x is a duplicate point, not a new one.
      if (world.day !== lastDay) {
        lastDay = world.day;
        xs.push(x.get(world));
        curves.forEach((c, i) => ys[i].push(c.get(world)));
      }
      paintCurves(canvas, {
        xLabel: x.label,
        curves: curves.map((c) => ({ label: c.label, dash: c.dash })),
        xs,
        ys,
        yLabel,
        yScale,
      });
    },
  };
}

/**
 * Draw a set of curves against a shared x axis. The picture, without the sampling.
 *
 * Split out so a *precomputed* sweep and a *live* one are the same chart rather than two
 * charts that resemble each other — BUILD_PLANNER §7.3 asks for exactly that, so a swept
 * figure and the model running underneath it read as siblings. `sweep.js` supplies arrays
 * from a JSON file; `curveView` above supplies arrays it accumulated from a running model.
 * Neither knows which it is.
 *
 * @param {HTMLCanvasElement} canvas
 * @param {object} spec
 * @param {string} spec.xLabel
 * @param {{label: string, dash?: boolean}[]} spec.curves
 * @param {number[]} spec.xs
 * @param {number[][]} spec.ys parallel to `curves`
 * @param {string} [spec.yLabel]
 * @param {'linear'|'log'} [spec.yScale]
 * @param {{at: number, label: string}} [spec.mark] a vertical rule — where a swept model was
 *   reported to change regime, say
 * @returns {{sx: (v: number) => number, xRange: [number, number], box: {left: number, right: number, top: number, bottom: number}} | null}
 *   the x mapping, so a caller can turn a click on the chart back into a swept value
 */
export function paintCurves(canvas, { xLabel, curves, xs, ys, yLabel, yScale = 'linear', mark }) {
  const log = yScale === 'log';
  /** Non-positive values have no place on a log axis; they are dropped, not clamped. */
  const ty = (/** @type {number} */ v) => (log ? (v > 0 ? Math.log10(v) : NaN) : v);

  const { ctx, w, h, dpr } = fitCanvas(canvas);
  const finite = xs.filter(Number.isFinite);
  if (finite.length < 2) return null;

  const fontSize = Math.max(Math.round(h * 0.035), 9 * dpr);
  const box = {
    left: w * 0.11,
    right: w * 0.99 - longestLabel(ctx, curves, ys, fontSize),
    top: h * 0.05,
    bottom: h - fontSize * 3.4,
    dpr,
  };

  const xRange = padded(Math.min(...finite), Math.max(...finite));
  let lo = Infinity;
  let hi = -Infinity;
  for (const series of ys) {
    for (const v of series) {
      const t = ty(v);
      if (!Number.isFinite(t)) continue;
      lo = Math.min(lo, t);
      hi = Math.max(hi, t);
    }
  }
  const yRange = padded(lo, hi);

  /** @param {number} v */
  const sx = (v) => box.left + ((v - xRange[0]) / (xRange[1] - xRange[0])) * (box.right - box.left);
  /** Takes a value in *plot* space — already log10'd where the axis is logarithmic. */
  const sy = (v) => box.bottom - ((v - yRange[0]) / (yRange[1] - yRange[0])) * (box.bottom - box.top);

  drawAxes(ctx, box, xRange, yRange, sx, sy, xLabel, yLabel, log
    ? { yTicks: logTicks(yRange[0], yRange[1]), yFormat: (e) => format(Math.pow(10, e)) }
    : {});

  ctx.font = `${fontSize}px ui-monospace, Menlo, monospace`;
  ctx.textBaseline = 'middle';

  // A named value on the x axis — the ρ the thesis reports the regime changing at, say. Drawn
  // under the data, because it is a claim to compare the curves against, not a result.
  if (mark && mark.at >= xRange[0] && mark.at <= xRange[1]) {
    ctx.strokeStyle = '#999';
    ctx.lineWidth = Math.max(dpr, 1);
    ctx.setLineDash([4 * dpr, 4 * dpr]);
    ctx.beginPath();
    ctx.moveTo(sx(mark.at), box.top);
    ctx.lineTo(sx(mark.at), box.bottom);
    ctx.stroke();
    ctx.setLineDash([]);
    ctx.fillStyle = '#666';
    ctx.textAlign = 'center';
    ctx.fillText(mark.label, sx(mark.at), box.top + fontSize * 0.6);
    ctx.textAlign = 'left';
  }

  /** @type {{y: number, text: string, grey: number}[]} */
  const labels = [];

  curves.forEach((curve, c) => {
    // Greyscale by index, as everything else in these views: several curves have to stay
    // separable in a monochrome print, which is what the originals were.
    const grey = Math.round((c / Math.max(curves.length - 1, 1)) * 140);
    ctx.strokeStyle = `rgb(${grey},${grey},${grey})`;
    ctx.lineWidth = Math.max(dpr * 1.6, 1.6);
    if (curve.dash) ctx.setLineDash([5 * dpr, 4 * dpr]);

    ctx.beginPath();
    let down = false;
    for (let i = 0; i < xs.length; i++) {
      const vx = xs[i];
      const vy = ty(ys[c][i]);
      if (!Number.isFinite(vx) || !Number.isFinite(vy)) { down = false; continue; }
      if (down) ctx.lineTo(sx(vx), sy(vy));
      else { ctx.moveTo(sx(vx), sy(vy)); down = true; }
    }
    ctx.stroke();
    ctx.setLineDash([]);

    const lastX = xs[xs.length - 1];
    const lastY = ys[c][ys[c].length - 1];
    if (Number.isFinite(lastX) && Number.isFinite(ty(lastY))) {
      // A filled head, so a live sweep shows where it has got to. Double-encoded as
      // position and as the only filled mark on the curve, per §5.9.
      ctx.fillStyle = `rgb(${grey},${grey},${grey})`;
      ctx.beginPath();
      ctx.arc(sx(lastX), sy(ty(lastY)), Math.max(2.5 * dpr, 2.5), 0, Math.PI * 2);
      ctx.fill();
      labels.push({ y: sy(ty(lastY)), text: `${curve.label} ${format(lastY)}`, grey });
    }
  });

  // Written last and as a column, so curves that end at the same value — which is what
  // agreement between two of these series looks like — do not overwrite each other.
  const placed = spread(labels.map((l) => l.y), box.top, box.bottom, fontSize * 1.15);
  labels.forEach((l, i) => {
    ctx.fillStyle = `rgb(${l.grey},${l.grey},${l.grey})`;
    ctx.fillText(l.text, box.right + 5 * dpr, placed[i]);
  });

  return { sx, xRange, box };
}

/**
 * Room to keep the right-hand labels on the canvas.
 * @param {CanvasRenderingContext2D} ctx @param {Curve[]} curves @param {number[][]} ys @param {number} fontSize
 */
function longestLabel(ctx, curves, ys, fontSize) {
  let chars = 0;
  curves.forEach((c, i) => {
    const last = ys[i][ys[i].length - 1];
    chars = Math.max(chars, `${c.label} ${format(last)}`.length);
  });
  // Measured off the monospace advance rather than measureText, which the headless recording
  // context does not implement. 0.68 rather than the ~0.6 a monospace face actually uses:
  // the fallback font in a renderer without Menlo is wider, and a label running off the
  // canvas is a worse failure than a slightly narrow plot.
  return (chars + 1.5) * fontSize * 0.68;
}
