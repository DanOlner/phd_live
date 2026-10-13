// @ts-check

/**
 * Time-series graph. Models E1, E2, E3 — and anything non-spatial.
 *
 * A port of `TimeSeriesGraph`. E1 and E3 take place in a POINT space, so there is nothing
 * spatial to draw: the whole result is a price converging, or failing to. Without this they
 * have no view at all.
 *
 * Two behaviours carried over deliberately:
 *
 * **Auto-scale that only grows.** Bounds expand to fit and shrink only when asked (`m`).
 * Right for a live model — an axis that rescales continuously makes a settling series look
 * like it never settles, which in E1 is precisely the thing being distinguished.
 *
 * **A rolling window, toggleable.** The original's `f` key. With 6,000 days on screen the
 * interesting first hundred are a single pixel; the window follows the front instead.
 */

import { spread } from './axis.js';

/** @typedef {{label: string, get: (world: import('../world.js').World) => number, dash?: boolean}} Track */

/**
 * @param {HTMLCanvasElement} canvas
 * @param {object} spec
 * @param {Track[]} spec.tracks
 * @param {number} [spec.window] days kept; 0 keeps everything
 * @param {number} [spec.reference] a horizontal line to compare against — the deduced value
 * @param {string} [spec.referenceLabel]
 */
export function timeSeriesView(canvas, { tracks, window: windowDays = 0, reference, referenceLabel }) {
  const ctx = /** @type {CanvasRenderingContext2D} */ (canvas.getContext('2d'));
  /** @type {number[][]} */
  const series = tracks.map(() => []);
  /** @type {number[]} */
  const days = [];
  let lo = Infinity;
  let hi = -Infinity;
  let lastDay = -1;

  return {
    /** Forget the accumulated bounds, so the axis refits what is on screen. The `m` key. */
    rescale() {
      lo = Infinity;
      hi = -Infinity;
    },

    reset() {
      series.forEach((s) => { s.length = 0; });
      days.length = 0;
      lastDay = -1;
      lo = Infinity;
      hi = -Infinity;
    },

    /** @param {import('../world.js').World} world */
    draw(world) {
      // Sample once per day even if drawn more often (paused, or several frames per day).
      if (world.day !== lastDay) {
        lastDay = world.day;
        days.push(world.day);
        tracks.forEach((t, i) => {
          const v = t.get(world);
          series[i].push(v);
          if (Number.isFinite(v)) {
            lo = Math.min(lo, v);
            hi = Math.max(hi, v);
          }
        });
        if (windowDays > 0 && days.length > windowDays) {
          days.shift();
          series.forEach((s) => s.shift());
        }
      }

      const dpr = (typeof window !== 'undefined' && window.devicePixelRatio) || 1;
      const w = Math.round(canvas.clientWidth * dpr);
      const h = Math.round(canvas.clientHeight * dpr);
      if (canvas.width !== w || canvas.height !== h) {
        canvas.width = w;
        canvas.height = h;
      }

      ctx.setTransform(1, 0, 0, 1, 0, 0);
      ctx.fillStyle = '#fff';
      ctx.fillRect(0, 0, w, h);
      if (days.length < 2) return;

      const padL = w * 0.02;
      const padY = h * 0.08;
      // Room for the right-hand labels, measured from the longest one rather than fixed at a
      // fraction of the width. It was 11%, which "clearing price" overruns at any canvas
      // size — the label was drawn off the edge of the picture on every belt model.
      const fontSize = Math.round(h * 0.035);
      let widest = 0;
      tracks.forEach((t, i) => {
        widest = Math.max(widest, `${t.label} ${format(series[i][series[i].length - 1])}`.length);
      });
      if (reference !== undefined) widest = Math.max(widest, (referenceLabel ?? format(reference)).length);
      const padR = Math.min((widest + 1.5) * fontSize * 0.68, w * 0.4);

      let top = hi, bottom = lo;
      if (reference !== undefined) {
        top = Math.max(top, reference);
        bottom = Math.min(bottom, reference);
      }
      if (!(top > bottom)) { top = bottom + 1; }
      const margin = (top - bottom) * 0.08;
      top += margin;
      bottom -= margin;

      /** @param {number} i */
      const sx = (i) => padL + (i / (days.length - 1)) * (w - padL - padR);
      /** @param {number} v */
      const sy = (v) => h - padY - ((v - bottom) / (top - bottom)) * (h - padY * 2);

      ctx.font = `${fontSize}px ui-monospace, Menlo, monospace`;
      ctx.textBaseline = 'middle';

      // Where each right-hand label goes. Collected first and written last: a settling model
      // is one whose series converge, so the labels reporting that they converged are exactly
      // the ones that end up on top of each other.
      /** @type {{y: number, text: string, grey: number}[]} */
      const labels = [];

      // The deduced value, where there is one. In E1 the whole question is whether the
      // series arrives at it, so it belongs on the chart rather than in the caption.
      if (reference !== undefined) {
        ctx.strokeStyle = '#000';
        ctx.lineWidth = Math.max(dpr, 1);
        ctx.setLineDash([6 * dpr, 4 * dpr]);
        ctx.beginPath();
        ctx.moveTo(padL, sy(reference));
        ctx.lineTo(w - padR, sy(reference));
        ctx.stroke();
        ctx.setLineDash([]);
        labels.push({ y: sy(reference), text: referenceLabel ?? format(reference), grey: 0 });
      }

      tracks.forEach((track, t) => {
        const values = series[t];
        // Greyscale by track index, so several series stay separable in print.
        const grey = Math.round((t / Math.max(tracks.length, 1)) * 150);
        ctx.strokeStyle = `rgb(${grey},${grey},${grey})`;
        ctx.lineWidth = Math.max(dpr * 1.4, 1.4);
        if (track.dash) ctx.setLineDash([4 * dpr, 3 * dpr]);
        ctx.beginPath();
        for (let i = 0; i < values.length; i++) {
          if (!Number.isFinite(values[i])) continue;
          const x = sx(i), y = sy(values[i]);
          if (i === 0) ctx.moveTo(x, y);
          else ctx.lineTo(x, y);
        }
        ctx.stroke();
        ctx.setLineDash([]);

        const last = values[values.length - 1];
        if (Number.isFinite(last)) {
          labels.push({ y: sy(last), text: `${track.label} ${format(last)}`, grey });
        }
      });

      const placed = spread(labels.map((l) => l.y), padY, h - padY, fontSize * 1.15);
      labels.forEach((l, i) => {
        ctx.fillStyle = `rgb(${l.grey},${l.grey},${l.grey})`;
        ctx.fillText(l.text, w - padR + 4 * dpr, placed[i]);
      });
    },
  };
}

/** @param {number} v */
function format(v) {
  if (v === 0) return '0';
  if (Math.abs(v) >= 1000 || Math.abs(v) < 0.001) return v.toExponential(1);
  return Number(v.toPrecision(4)).toString();
}
