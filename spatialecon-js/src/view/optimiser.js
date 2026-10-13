// @ts-check

/**
 * The hill-climber, in the space it actually searches. A4's second panel; thesis §4.6.1, §5.2.3.
 *
 * A4's main chart says the search converges and how fast. It cannot say what the search *is*,
 * and that is the thing worth showing, because the hill-climber is not a detail of the
 * implementation — it is the thesis' claim about what an agent can plausibly do. A Person is
 * not assumed to solve a constrained optimisation. They try a basket, try a nearby basket,
 * keep whichever is better, and get tired. Everything else in the framework is built to work
 * with an agent that reasons like that.
 *
 * Two goods, so the search space is a plane and can simply be drawn:
 *
 * - **The budget line** p₀q₀ + p₁q₁ = Y. Every affordable basket is on or below it; the
 *   climber never leaves it, because each candidate is normalised back onto it before being
 *   scored. That is worth seeing — the constraint is not enforced by rejection, it is built
 *   into how a candidate is generated.
 * - **The analytic optimum**, from `constrained()`, as a ring. The target.
 * - **The path**, joined in the order the climb accepted it, darkening as it goes.
 *
 * What the picture shows, and what the numbers in A4 only imply: the accepted steps bunch
 * almost entirely at the start. The climber lands in the right neighbourhood in a handful of
 * moves and then spends the remaining nine hundred-odd iterations shuffling. The residue it
 * never quite removes is where §5.5.2's demand noise comes from — so this view is also the
 * origin of the noise that makes D2's Firms misread their own revenue.
 *
 * It runs its own climb on its own generator rather than reading one out of the model. A view
 * that drew from `world.rng` would consume draws and shift every subsequent number in the
 * run, which is the sort of coupling that makes a "display option" quietly change results.
 */

import { Rng } from '../rng.js';
import { constrained, hillClimb } from '../econ.js';
import { drawAxes, fitCanvas, format, padded } from './axis.js';

/**
 * @param {HTMLCanvasElement} canvas
 * @param {object} spec
 * @param {number[]} spec.prices exactly two
 * @param {number} spec.budget
 * @param {import('../econ.js').Utility} spec.utility
 * @param {(world: import('../world.js').World) => number} spec.iterationsOf
 * @param {number} [spec.seed]
 */
export function optimiserView(canvas, { prices, budget, utility, iterationsOf, seed = 12345 }) {
  /** @type {{i: number, q: number[]}[]} */
  let path = [];
  let iterations = 0;
  let lastDay = -1;
  let target = [0, 0];

  return {
    reset() {
      path = [];
      iterations = 0;
      lastDay = -1;
    },

    /** @param {import('../world.js').World} world */
    draw(world) {
      if (world.day !== lastDay) {
        lastDay = world.day;
        iterations = Math.max(1, Math.round(iterationsOf(world)));
        // A fresh generator each day from a fixed seed: the same iteration budget always
        // shows the same climb, so dragging the budget back and forth compares like with
        // like instead of reshuffling the picture.
        const rng = new Rng(seed);
        const climber = hillClimb(iterations);
        climber.optimise(prices, budget, utility, rng);
        path = climber.lastPath;
        target = constrained().optimise(prices, budget, utility, rng);
      }

      const { ctx, w, h, dpr } = fitCanvas(canvas);
      if (path.length === 0) return;

      const fontSize = Math.max(Math.round(h * 0.038), 9 * dpr);
      const box = { left: w * 0.13, right: w * 0.97, top: h * 0.06, bottom: h - fontSize * 3.4, dpr };

      // The whole affordable region, not just where the climb went: the budget line is the
      // constraint, and a chart cropped to the path would hide how little of it is explored.
      const xRange = padded(0, budget / prices[0], 0.04);
      const yRange = padded(0, budget / prices[1], 0.04);
      /** @param {number} v */
      const sx = (v) => box.left + ((v - xRange[0]) / (xRange[1] - xRange[0])) * (box.right - box.left);
      /** @param {number} v */
      const sy = (v) => box.bottom - ((v - yRange[0]) / (yRange[1] - yRange[0])) * (box.bottom - box.top);

      drawAxes(ctx, box, xRange, yRange, sx, sy, 'quantity of good 0', 'quantity of good 1');

      // The budget line.
      ctx.strokeStyle = '#000';
      ctx.lineWidth = Math.max(dpr, 1);
      ctx.setLineDash([6 * dpr, 4 * dpr]);
      ctx.beginPath();
      ctx.moveTo(sx(budget / prices[0]), sy(0));
      ctx.lineTo(sx(0), sy(budget / prices[1]));
      ctx.stroke();
      ctx.setLineDash([]);

      // The climb, darkening with each accepted step.
      ctx.lineWidth = Math.max(1.6 * dpr, 1.6);
      for (let k = 1; k < path.length; k++) {
        const age = k / (path.length - 1);
        const grey = Math.round(180 - age * 180);
        ctx.strokeStyle = `rgb(${grey},${grey},${grey})`;
        ctx.beginPath();
        ctx.moveTo(sx(path[k - 1].q[0]), sy(path[k - 1].q[1]));
        ctx.lineTo(sx(path[k].q[0]), sy(path[k].q[1]));
        ctx.stroke();

        ctx.fillStyle = `rgb(${grey},${grey},${grey})`;
        ctx.beginPath();
        ctx.arc(sx(path[k].q[0]), sy(path[k].q[1]), 2.4 * dpr, 0, Math.PI * 2);
        ctx.fill();
      }

      // Where it started: hollow, and always a long way out, because the first slice is drawn
      // at random rather than from anything sensible.
      const first = path[0];
      ctx.strokeStyle = '#000';
      ctx.lineWidth = Math.max(dpr, 1);
      ctx.beginPath();
      ctx.arc(sx(first.q[0]), sy(first.q[1]), 4 * dpr, 0, Math.PI * 2);
      ctx.stroke();

      // The answer the algebra gives, for nothing, in one step.
      ctx.lineWidth = Math.max(1.8 * dpr, 1.8);
      ctx.beginPath();
      ctx.arc(sx(target[0]), sy(target[1]), 7 * dpr, 0, Math.PI * 2);
      ctx.stroke();

      ctx.font = `${fontSize}px ui-monospace, Menlo, monospace`;
      ctx.textBaseline = 'middle';
      ctx.textAlign = 'right';
      ctx.fillStyle = '#000';
      // Offset off the ring rather than level with it: the climb converges *onto* the target,
      // so a label on the same line is written straight through the end of the path.
      ctx.fillText('constrained optimum', sx(target[0]) - 12 * dpr, sy(target[1]) - fontSize * 1.2);

      ctx.textAlign = 'left';
      ctx.textBaseline = 'top';
      ctx.fillStyle = '#444';
      const last = path[path.length - 1].q;
      const lines = [
        `${iterations} iterations`,
        `${path.length} accepted`,
        `at ${format(last[0])}, ${format(last[1])}`,
      ];
      lines.forEach((line, i) => ctx.fillText(line, box.left + fontSize * 0.5, box.top + fontSize * (0.4 + i * 1.3)));
    },
  };
}
