// @ts-check

/**
 * The arrow scatter. E8, thesis §6.4.2 — the closing result.
 *
 * Two model variables against each other, with consecutive samples joined in the order they
 * were taken and arrowheads laid along the path. The arrows are the whole reason this view
 * exists rather than a plain scatter: a cloud of points showing that production concentration
 * and mean distance to the winning Firm are correlated is a much weaker claim than a track
 * showing the pair sitting still for two thousand days and then travelling, fast, in one
 * direction. Correlation is what a scatter shows. *Direction of travel* is what E8 is about.
 *
 * What it makes visible, and what neither the plane view nor the time series does on its own:
 * People migrating and production concentrating are not two findings that happen to coincide,
 * they are one event. Watching two separate charts move at once requires the reader to hold
 * both; watching a single track turn a corner does not.
 *
 * Age is carried as greyscale — early samples pale, recent ones black — so a still frame
 * still reads in time order, which matters because the published figures were screen
 * captures. Double-encoded with the arrows themselves, per CODE_ANATOMY §5.9: the direction
 * is legible from the heads, and the *recency* from the ink.
 */

import { drawAxes, fitCanvas, format, padded } from './axis.js';

/** @typedef {{label: string, get: (world: import('../world.js').World) => number}} Axis */

/**
 * @param {HTMLCanvasElement} canvas
 * @param {object} spec
 * @param {Axis} spec.x
 * @param {Axis} spec.y
 * @param {number} [spec.arrowEvery] samples between arrowheads
 * @param {number} [spec.sampleEvery] days between samples; the track is a shape, not a series,
 *   and 4,000 daily points on a chart 900 pixels wide is mostly overdraw
 */
export function correlateView(canvas, { x, y, arrowEvery = 8, sampleEvery = 20 }) {
  /** @type {{x: number, y: number, day: number}[]} */
  const track = [];
  let lo = { x: Infinity, y: Infinity };
  let hi = { x: -Infinity, y: -Infinity };
  let lastDay = -1;

  return {
    /** Refit the axes to what is on screen. The time series' `m` key, same reasoning. */
    rescale() {
      lo = { x: Infinity, y: Infinity };
      hi = { x: -Infinity, y: -Infinity };
      for (const p of track) {
        lo = { x: Math.min(lo.x, p.x), y: Math.min(lo.y, p.y) };
        hi = { x: Math.max(hi.x, p.x), y: Math.max(hi.y, p.y) };
      }
    },

    reset() {
      track.length = 0;
      lastDay = -1;
      lo = { x: Infinity, y: Infinity };
      hi = { x: -Infinity, y: -Infinity };
    },

    /** @param {import('../world.js').World} world */
    draw(world) {
      // The first sample is taken whenever the view first sees the world, so the track starts
      // where the run does rather than at the first multiple of `sampleEvery`.
      if (world.day !== lastDay && (track.length === 0 || world.day % Math.max(sampleEvery, 1) === 0)) {
        lastDay = world.day;
        const px = x.get(world);
        const py = y.get(world);
        if (Number.isFinite(px) && Number.isFinite(py)) {
          track.push({ x: px, y: py, day: world.day });
          lo = { x: Math.min(lo.x, px), y: Math.min(lo.y, py) };
          hi = { x: Math.max(hi.x, px), y: Math.max(hi.y, py) };
        }
      }

      const { ctx, w, h, dpr } = fitCanvas(canvas);
      if (track.length < 2) return;

      const fontSize = Math.max(Math.round(h * 0.035), 9 * dpr);
      const box = {
        left: w * 0.12,
        right: w * 0.97,
        top: h * 0.06,
        bottom: h - fontSize * 3.4,
        dpr,
      };

      const xRange = padded(lo.x, hi.x);
      const yRange = padded(lo.y, hi.y);
      /** @param {number} v */
      const sx = (v) => box.left + ((v - xRange[0]) / (xRange[1] - xRange[0])) * (box.right - box.left);
      /** @param {number} v */
      const sy = (v) => box.bottom - ((v - yRange[0]) / (yRange[1] - yRange[0])) * (box.bottom - box.top);

      drawAxes(ctx, box, xRange, yRange, sx, sy, x.label, y.label);

      // The track, segment by segment, darkening with age. The pale end stops at 170 rather
      // than at white: the first two thousand days are the part where nothing happens, and a
      // reader has to be able to see that they are *there* to see that they are flat.
      ctx.lineWidth = Math.max(dpr * 1.5, 1.5);
      for (let i = 1; i < track.length; i++) {
        const age = i / (track.length - 1);          // 0 oldest, 1 newest
        const grey = Math.round(170 - age * 170);
        ctx.strokeStyle = `rgb(${grey},${grey},${grey})`;
        ctx.beginPath();
        ctx.moveTo(sx(track[i - 1].x), sy(track[i - 1].y));
        ctx.lineTo(sx(track[i].x), sy(track[i].y));
        ctx.stroke();

        if (i % Math.max(arrowEvery, 1) === 0) {
          arrowhead(ctx, sx(track[i - 1].x), sy(track[i - 1].y), sx(track[i].x), sy(track[i].y),
            `rgb(${grey},${grey},${grey})`, 9 * dpr);
        }
      }

      ctx.font = `${fontSize}px ui-monospace, Menlo, monospace`;
      ctx.textBaseline = 'middle';

      // Where it began: hollow, so it cannot be mistaken for the head. Its label goes below
      // the marker — the start of the track is where the points are densest, and a label
      // written across them hides exactly the stationary phase it is naming.
      const first = track[0];
      ctx.strokeStyle = '#000';
      ctx.lineWidth = Math.max(dpr, 1);
      ctx.beginPath();
      ctx.arc(sx(first.x), sy(first.y), 4 * dpr, 0, Math.PI * 2);
      ctx.stroke();
      ctx.fillStyle = '#666';
      ctx.textAlign = 'center';
      ctx.fillText(`day ${first.day}`, sx(first.x), Math.max(sy(first.y) - fontSize * 1.4, box.top + fontSize));

      // Where it is now: filled, labelled with both values.
      const last = track[track.length - 1];
      ctx.fillStyle = '#000';
      ctx.beginPath();
      ctx.arc(sx(last.x), sy(last.y), 4.5 * dpr, 0, Math.PI * 2);
      ctx.fill();
      // Placed by which half of the plot the head is in, rather than by measuring the text:
      // the headless recorder has no `measureText`, and a mis-measured label runs off the
      // canvas — which is what happened when this guessed at the monospace advance instead.
      const rightHalf = sx(last.x) > (box.left + box.right) / 2;
      ctx.textAlign = rightHalf ? 'right' : 'left';
      ctx.fillText(`day ${last.day}  ${format(last.x)}, ${format(last.y)}`,
        sx(last.x) + (rightHalf ? -8 : 8) * dpr,
        Math.max(sy(last.y) - fontSize * 1.2, box.top + fontSize));
      ctx.textAlign = 'left';
    },
  };
}

/**
 * A small filled triangle at (x2, y2) pointing along the segment.
 * @param {CanvasRenderingContext2D} ctx
 * @param {number} x1 @param {number} y1 @param {number} x2 @param {number} y2
 * @param {string} fill @param {number} size
 */
function arrowhead(ctx, x1, y1, x2, y2, fill, size) {
  const dx = x2 - x1;
  const dy = y2 - y1;
  const length = Math.hypot(dx, dy);
  if (length < 1e-6) return; // a stalled track has no direction to point in
  const ux = dx / length;
  const uy = dy / length;
  ctx.fillStyle = fill;
  ctx.beginPath();
  ctx.moveTo(x2, y2);
  ctx.lineTo(x2 - ux * size - uy * size * 0.5, y2 - uy * size + ux * size * 0.5);
  ctx.lineTo(x2 - ux * size + uy * size * 0.5, y2 - uy * size - ux * size * 0.5);
  ctx.closePath();
  ctx.fill();
}
