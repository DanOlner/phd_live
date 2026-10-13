// @ts-check

/**
 * F4's view: one Person's decision, drawn. Thesis §4.6.1.
 *
 * The plane view shows where everyone ended up. This shows *how one of them got there* — the
 * candidate locations sampled this turn, what each was worth, and which won. It is the only
 * view in the set that draws a decision rather than a state.
 *
 * The visual grammar is the original's throughout (CODE_ANATOMY §5.9), because it already
 * solves this problem: quantities are double-encoded as size and ink, density is drawn as
 * overlapping radius circles so crowding and the picture of crowding are the same object,
 * and Firms are the twelve-vertex plus. What is new is only the distinction between a
 * candidate that was *scored* and one that was *discarded* — a filled disc against a cross —
 * because in this framework those are different in kind, not merely in degree. A Bundle
 * whose density cost exceeds the wage has no budget to buy anything with, so it is thrown
 * away rather than given a bad score, and that is the rule §5.4.7's dispersal runs on.
 *
 * The rest of the population is drawn faintly. They are not the subject, but they are not
 * scenery either: every candidate's density term is computed from exactly where they are
 * standing, so they are the reason the same location is worth something different tomorrow.
 */

import { plusGlyph } from './plane.js';

/** @typedef {import('../agents.js').Bundle} Bundle */

/**
 * @param {HTMLCanvasElement} canvas
 * @param {object} spec
 * @param {() => {from: import('../space.js').Pt, candidates: Bundle[], best: Bundle | null}} spec.decision
 * @param {number} spec.subjectId
 */
export function samplerView(canvas, { decision, subjectId }) {
  const ctx = /** @type {CanvasRenderingContext2D} */ (canvas.getContext('2d'));

  return {
    /** @param {import('../world.js').World} world */
    draw(world) {
      const width = world.space.width;
      const dpr = (typeof window !== 'undefined' && window.devicePixelRatio) || 1;
      const cssSize = Math.min(canvas.clientWidth, canvas.clientHeight);
      if (canvas.width !== Math.round(cssSize * dpr)) {
        canvas.width = canvas.height = Math.round(cssSize * dpr);
      }
      const px = canvas.width / width;
      /** @param {number} v */
      const s = (v) => v * px;

      ctx.setTransform(1, 0, 0, 1, 0, 0);
      ctx.fillStyle = '#fff';
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      const subject = world.people[subjectId];
      const { from, candidates, best } = decision();

      // 1. Everyone's density radius. This is what makes the crosses explicable: where the
      //    ink is darkest is where a candidate is most likely to have been unaffordable.
      if (world.config.densityRadius > 0) {
        ctx.strokeStyle = 'rgba(0,0,0,0.22)';
        ctx.lineWidth = Math.max(s(0.002), 0.75);
        const r = s(world.config.densityRadius);
        for (const p of world.people) {
          ctx.beginPath();
          ctx.arc(s(p.position.x), s(p.position.y), r, 0, Math.PI * 2);
          ctx.stroke();
        }
      }

      // 2. The other People, small and pale — present, but not the subject.
      ctx.fillStyle = 'rgba(0,0,0,0.35)';
      for (const p of world.people) {
        if (p === subject) continue;
        const size = s(0.012);
        ctx.fillRect(s(p.position.x) - size / 2, s(p.position.y) - size / 2, size, size);
      }

      // 3. Where the goods in the chosen basket come from, weight proportional to spend.
      //    Drawn from the Bundle's location rather than the Person's, because that is where
      //    the prices in it were quoted: the Bundle is a question about somewhere else.
      if (best) {
        let heaviest = 0;
        for (let i = 0; i < best.quantities.length; i++) {
          const spend = best.quantities[i] * best.prices[i];
          if (spend > heaviest) heaviest = spend;
        }
        for (let i = 0; i < best.sellers.length && heaviest > 0; i++) {
          const spend = best.quantities[i] * best.prices[i];
          if (spend <= 0) continue;
          const t = spend / heaviest;
          ctx.lineWidth = Math.max(s(0.012) * t, 0.6 * dpr);
          ctx.strokeStyle = `rgba(0,0,0,${(0.2 + 0.5 * t).toFixed(3)})`;
          ctx.beginPath();
          ctx.moveTo(s(best.location.x), s(best.location.y));
          ctx.lineTo(s(best.sellers[i].position.x), s(best.sellers[i].position.y));
          ctx.stroke();
        }
      }

      // 4. The candidates. Scored ones as discs, sized and shaded by the utility they would
      //    have yielded; discarded ones as a cross, which is a different claim entirely.
      const scored = candidates.filter((b) => b.isViable());
      let lo = Infinity, hi = -Infinity;
      for (const b of scored) { lo = Math.min(lo, b.utility); hi = Math.max(hi, b.utility); }

      for (const b of candidates) {
        const x = s(b.location.x);
        const y = s(b.location.y);

        if (!b.isViable()) {
          const r = s(0.012);
          ctx.strokeStyle = 'rgba(0,0,0,0.55)';
          ctx.lineWidth = Math.max(1.2 * dpr, 1.2);
          ctx.beginPath();
          ctx.moveTo(x - r, y - r); ctx.lineTo(x + r, y + r);
          ctx.moveTo(x + r, y - r); ctx.lineTo(x - r, y + r);
          ctx.stroke();
          continue;
        }

        const t = hi > lo ? (b.utility - lo) / (hi - lo) : 1;
        // Dark and large is *good* here, the opposite way round from the density ramp on the
        // plane view — because here the variable is utility, and the best candidate should be
        // the one the eye lands on.
        const grey = Math.round(200 - t * 200);
        ctx.fillStyle = `rgb(${grey},${grey},${grey})`;
        ctx.strokeStyle = 'rgba(0,0,0,0.5)';
        ctx.lineWidth = Math.max(dpr, 1);
        ctx.beginPath();
        ctx.arc(x, y, s(0.008 + t * 0.014), 0, Math.PI * 2);
        ctx.fill();
        ctx.stroke();
      }

      // 5. Where the subject decided *from*, hollow, and the move they made. By draw time
      //    they have already moved, so without this the "from" and the "chosen" are the same
      //    mark and the decision looks like it had no alternative.
      const moved = best && (Math.abs(best.location.x - from.x) > 1e-12
        || Math.abs(best.location.y - from.y) > 1e-12);
      if (moved && best) {
        ctx.strokeStyle = '#000';
        ctx.lineWidth = Math.max(1.6 * dpr, 1.6);
        ctx.setLineDash([5 * dpr, 4 * dpr]);
        ctx.beginPath();
        ctx.moveTo(s(from.x), s(from.y));
        ctx.lineTo(s(best.location.x), s(best.location.y));
        ctx.stroke();
        ctx.setLineDash([]);
      }

      const size = s(0.03);
      ctx.strokeStyle = '#000';
      ctx.lineWidth = Math.max(1.8 * dpr, 1.8);
      ctx.strokeRect(s(from.x) - size / 2, s(from.y) - size / 2, size, size);

      // 6. The one taken, ringed. Nothing is filled over it: the chosen Bundle is already the
      //    largest, darkest disc by construction, and covering it with a marker made it look
      //    smaller than its near-equals, which reads as the Person having picked wrongly.
      if (best) {
        ctx.strokeStyle = '#000';
        ctx.lineWidth = Math.max(2 * dpr, 2);
        ctx.beginPath();
        ctx.arc(s(best.location.x), s(best.location.y), s(0.045), 0, Math.PI * 2);
        ctx.stroke();
      } else if (subject) {
        // Nowhere was affordable: the white circle the plane view uses for the same thing.
        ctx.fillStyle = '#fff';
        ctx.beginPath();
        ctx.arc(s(subject.position.x), s(subject.position.y), s(0.02), 0, Math.PI * 2);
        ctx.fill();
        ctx.stroke();
      }

      // 7. Firms.
      ctx.lineWidth = Math.max(s(0.005), 1.5);
      for (const f of world.firms) {
        ctx.fillStyle = '#fff';
        ctx.strokeStyle = '#000';
        plusGlyph(ctx, s(f.position.x), s(f.position.y), s(0.018));
        ctx.fill();
        ctx.stroke();
      }

      drawKey(ctx, px, candidates.length, scored.length, best);
    },
  };
}

/**
 * @param {CanvasRenderingContext2D} ctx @param {number} px
 * @param {number} sampled @param {number} scored @param {Bundle | null} best
 */
function drawKey(ctx, px, sampled, scored, best) {
  const font = Math.max(Math.round(0.026 * px), 10);
  ctx.font = `${font}px ui-monospace, Menlo, monospace`;
  ctx.textBaseline = 'top';
  ctx.textAlign = 'left';
  ctx.fillStyle = '#000';
  const lines = [
    `${sampled} Bundles sampled`,
    `${scored} affordable, ${sampled - scored} discarded`,
    best ? `chosen: U = ${round(best.utility)}, density ${round(best.densityCost)}` : 'nowhere affordable',
  ];
  lines.forEach((line, i) => ctx.fillText(line, font * 0.6, font * (0.6 + i * 1.3)));
}

/** @param {number} v */
function round(v) {
  return Number(v.toPrecision(3)).toString();
}
