// @ts-check

/**
 * The Core Model's short-run figure. G1, thesis §2.5.5.
 *
 * The real wage ratio w₁/w₂ across every possible split of manufacturing workers, at one
 * transport cost. Where it crosses 1 neither region pays better and nobody has a reason to
 * move — an equilibrium. Which way workers are pulled either side of a crossing decides
 * whether it is stable, and that is drawn: arrows along the top point the way λ moves.
 *
 * At the shipped T = 2.25 there are five equilibria and the chart shows three of them. The
 * other two are the corners, λ = 0 and λ = 1, which are equilibria by construction rather
 * than by crossing: with every manufacturing worker already in one region there is nobody
 * left to move. Reading the arrows at the two ends tells you they are stable.
 *
 * Recomputed whenever a parameter changes rather than accumulated over days — this is a
 * *solved* curve, not a trajectory, and there is nothing for the model's clock to do.
 */

import { DEFAULTS, equilibria, shortRun } from '../coremodel.js';
import { drawAxes, fitCanvas, format, padded } from './axis.js';

/**
 * @param {HTMLCanvasElement} canvas
 * @param {object} spec
 * @param {() => import('../coremodel.js').CoreParams} spec.params
 * @param {number} [spec.steps]
 */
export function coreModelView(canvas, { params, steps = 400 }) {
  /** @type {ReturnType<typeof shortRun>} */
  let curve = [];
  /** @type {string} */
  let computedFor = '';

  return {
    reset() {
      curve = [];
      computedFor = '';
    },

    draw() {
      const p = params();
      // The parameters, not the day, are what this depends on. Recompute only when they move.
      const key = `${p.t}|${p.e}|${p.d}|${p.phi}`;
      if (key !== computedFor) {
        curve = shortRun(p, steps);
        computedFor = key;
      }

      const { ctx, w, h, dpr } = fitCanvas(canvas);
      if (curve.length < 2) return;

      const fontSize = Math.max(Math.round(h * 0.036), 9 * dpr);
      const box = { left: w * 0.13, right: w * 0.97, top: h * 0.12, bottom: h - fontSize * 3.4, dpr };

      const ratios = curve.map((c) => c.ratio);
      const xRange = /** @type {[number, number]} */ ([0, 1]);
      const yRange = padded(Math.min(...ratios, 1), Math.max(...ratios, 1), 0.12);

      /** @param {number} v */
      const sx = (v) => box.left + ((v - xRange[0]) / (xRange[1] - xRange[0])) * (box.right - box.left);
      /** @param {number} v */
      const sy = (v) => box.bottom - ((v - yRange[0]) / (yRange[1] - yRange[0])) * (box.bottom - box.top);

      drawAxes(ctx, box, xRange, yRange, sx, sy,
        'lambda (share of manufacturing in region 1)', 'real wage ratio w1 / w2');

      // Parity: where neither region pays better.
      ctx.strokeStyle = '#000';
      ctx.lineWidth = Math.max(dpr, 1);
      ctx.setLineDash([6 * dpr, 4 * dpr]);
      ctx.beginPath();
      ctx.moveTo(box.left, sy(1));
      ctx.lineTo(box.right, sy(1));
      ctx.stroke();
      ctx.setLineDash([]);

      ctx.strokeStyle = '#000';
      ctx.lineWidth = Math.max(1.8 * dpr, 1.8);
      ctx.beginPath();
      curve.forEach((c, i) => {
        const x = sx(c.lambda);
        const y = sy(c.ratio);
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      });
      ctx.stroke();

      // Which way workers are pulled: right where region 1 pays more, left where it pays less.
      // This is what makes a crossing stable or unstable, so it is drawn rather than asserted.
      const arrowY = box.top - fontSize * 0.2;
      const arrowSize = Math.max(4 * dpr, 4);
      ctx.fillStyle = '#666';
      for (let i = 0; i < 21; i++) {
        const lambda = i / 20;
        const at = curve[Math.min(curve.length - 1, Math.round(lambda * (curve.length - 1)))];
        if (!at || Math.abs(at.ratio - 1) < 1e-9) continue;
        const dir = at.ratio > 1 ? 1 : -1;
        const x = sx(lambda);
        ctx.beginPath();
        ctx.moveTo(x + dir * arrowSize, arrowY);
        ctx.lineTo(x - dir * arrowSize, arrowY - arrowSize * 0.7);
        ctx.lineTo(x - dir * arrowSize, arrowY + arrowSize * 0.7);
        ctx.closePath();
        ctx.fill();
      }

      // The equilibria: filled where stable, hollow where not.
      const found = equilibria(curve);
      ctx.lineWidth = Math.max(1.8 * dpr, 1.8);
      for (const e of found) {
        ctx.beginPath();
        ctx.arc(sx(e.lambda), sy(1), 6 * dpr, 0, Math.PI * 2);
        if (e.stable) {
          ctx.fillStyle = '#000';
          ctx.fill();
        } else {
          ctx.fillStyle = '#fff';
          ctx.fill();
          ctx.strokeStyle = '#000';
          ctx.stroke();
        }
      }

      ctx.font = `${fontSize}px ui-monospace, Menlo, monospace`;
      ctx.textBaseline = 'top';
      ctx.textAlign = 'left';
      ctx.fillStyle = '#000';
      const lines = [
        `T = ${format(p.t)}   e = ${format(p.e)}`,
        `${found.length} interior equilibria, ${found.filter((e) => e.stable).length} stable`,
        'plus the corners at 0 and 1',
      ];
      lines.forEach((line, i) => {
        ctx.fillText(line, box.left + fontSize * 0.5, box.bottom - fontSize * (3.6 - i * 1.3));
      });
    },
  };
}

export { DEFAULTS };
