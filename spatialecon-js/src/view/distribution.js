// @ts-check

/**
 * Distributions — F1's wealth and F2's permutation gap. Thesis §4.7.2, §4.7.4.
 *
 * Two group F models produce a *shape over a population* rather than a trajectory or a
 * settlement, and the shape is the entire result. F1's answer is "a few very rich, most in
 * the bottom band"; F2's is "a pyramid centred on one day, not a spike". Neither is a claim
 * about any individual or any moment, so none of the existing views can carry it.
 *
 * Three modes, because those shapes want different framings:
 *
 * - **`sorted`** — every member of the population as a bar, ranked. Reads as a skyline, and
 *   makes the tail legible in a way a histogram of wealth does not: you can see *how few*
 *   are at the top by counting bars.
 * - **`histogram`** — raw values, binned here. The right choice when the question is about
 *   the shape of a spread rather than about who is where in it.
 * - **`bars`** — values plotted in the order given, untouched. For a distribution that has
 *   *already* been binned by the model, as F2's has: ten million gaps are accumulated into
 *   counts as they happen rather than stored. Passing those counts to `sorted` silently
 *   ranks them and turns a pyramid into a descending ramp, which is exactly what happened.
 *
 * Greyscale and double-encoded as everything else here (CODE_ANATOMY §5.9): in `sorted` mode
 * a bar's height and its ink both carry the value, so the distribution survives being printed
 * small or read at a glance.
 */

import { drawAxes, fitCanvas, format, padded } from './axis.js';

/**
 * @param {HTMLCanvasElement} canvas
 * @param {object} spec
 * @param {(world: import('../world.js').World) => number[]} spec.values
 * @param {'sorted'|'histogram'|'bars'} spec.mode
 * @param {[number, number]} [spec.xRange] for `bars`, what the bin positions mean
 * @param {string} spec.xLabel @param {string} [spec.yLabel]
 * @param {number} [spec.bins] histogram mode only
 * @param {(world: import('../world.js').World) => string[]} [spec.readout] lines drawn in the corner
 * @param {{at: number, label: string}} [spec.mark] a vertical rule on the x axis
 */
export function distributionView(canvas, { values, mode, xLabel, yLabel, bins = 40, readout, mark, xRange: givenRange }) {
  return {
    /** @param {import('../world.js').World} world */
    draw(world) {
      const raw = values(world).filter(Number.isFinite);
      const { ctx, w, h, dpr } = fitCanvas(canvas);
      if (raw.length === 0) return;

      const fontSize = Math.max(Math.round(h * 0.038), 9 * dpr);
      const box = { left: w * 0.12, right: w * 0.97, top: h * 0.07, bottom: h - fontSize * 3.4, dpr };

      /** @type {number[]} */
      let heights;
      /** @type {[number, number]} */
      let xRange;

      if (mode === 'bars') {
        // Already binned by the model. Do not touch the order.
        heights = raw;
        xRange = givenRange ?? [0, heights.length];
      } else if (mode === 'sorted') {
        heights = raw.slice().sort((a, b) => b - a);
        xRange = [0, heights.length];
      } else {
        const lo = Math.min(...raw);
        const hi = Math.max(...raw);
        const width = (hi - lo) || 1;
        heights = new Array(bins).fill(0);
        for (const v of raw) {
          const bin = Math.min(bins - 1, Math.floor(((v - lo) / width) * bins));
          heights[bin]++;
        }
        xRange = [lo, hi];
      }

      const yRange = padded(0, Math.max(...heights), 0.08);
      /** @param {number} v */
      const sx = (v) => box.left + ((v - xRange[0]) / (xRange[1] - xRange[0])) * (box.right - box.left);
      /** @param {number} v */
      const sy = (v) => box.bottom - ((v - yRange[0]) / (yRange[1] - yRange[0])) * (box.bottom - box.top);

      drawAxes(ctx, box, xRange, yRange, sx, sy, xLabel, yLabel);

      const slot = (box.right - box.left) / heights.length;
      const barWidth = Math.max(slot * 0.86, 1);
      const peak = Math.max(...heights, 1e-9);

      for (let i = 0; i < heights.length; i++) {
        const value = heights[i];
        if (value <= 0) continue;
        const x = box.left + i * slot + (slot - barWidth) / 2;
        const y = sy(value);
        // Height and ink both carry the value, as everywhere else in these views.
        const grey = Math.round(200 - (value / peak) * 200);
        ctx.fillStyle = `rgb(${grey},${grey},${grey})`;
        ctx.fillRect(x, y, barWidth, box.bottom - y);
      }

      if (mark && mark.at >= xRange[0] && mark.at <= xRange[1]) {
        ctx.strokeStyle = '#000';
        ctx.lineWidth = Math.max(1.4 * dpr, 1.4);
        ctx.setLineDash([6 * dpr, 4 * dpr]);
        ctx.beginPath();
        ctx.moveTo(sx(mark.at), box.top);
        ctx.lineTo(sx(mark.at), box.bottom);
        ctx.stroke();
        ctx.setLineDash([]);
        ctx.font = `${fontSize}px ui-monospace, Menlo, monospace`;
        ctx.fillStyle = '#000';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'top';
        ctx.fillText(mark.label, sx(mark.at), box.top);
        ctx.textAlign = 'left';
      }

      ctx.font = `${fontSize}px ui-monospace, Menlo, monospace`;
      ctx.textBaseline = 'top';
      ctx.textAlign = 'right';
      ctx.fillStyle = '#000';
      (readout?.(world) ?? []).forEach((line, i) => {
        ctx.fillText(line, box.right, box.top + fontSize * (0.2 + i * 1.3));
      });
      ctx.textAlign = 'left';
    },
  };
}

/**
 * The Gini coefficient — 0 is everyone equal, 1 is one person holding everything.
 *
 * Here so that F1 can state its result as a number rather than as an impression. The thesis'
 * point about this model is that the number comes out "not far off real countries", and that
 * the resemblance is exactly what makes it dangerous.
 *
 * @param {number[]} values
 */
export function gini(values) {
  const n = values.length;
  if (n === 0) return 0;
  const sorted = values.slice().sort((a, b) => a - b);
  let total = 0;
  let weighted = 0;
  for (let i = 0; i < n; i++) {
    total += sorted[i];
    weighted += (i + 1) * sorted[i];
  }
  if (total === 0) return 0;
  return (2 * weighted) / (n * total) - (n + 1) / n;
}

/** @param {number} v */
export function label(v) {
  return format(v);
}
