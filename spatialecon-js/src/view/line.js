// @ts-check

/**
 * The line view: the second of the original's two view families. Models D1, D2, E5–E7.
 *
 * A port of `Hotelling1DPresentation` and `TransmissionDraw1DPresentationForManyAgents`,
 * documented in CODE_ANATOMY §5.9. Reference figure:
 * `ThesisCorrectionsLatex/ChapterTestingModels/Figures/MQ_modelday_98111_Hotelling_Line_DMR_…__MainVizrho1del20.png`
 *
 * A horizontal rule with one profile drawn as bars rising above it and a second mirrored
 * below, one bar per agent at that agent's position on the line, greyscale by value. Firms
 * sit on the rule as a white pennant with a rectangle above showing their **rolling mean
 * position ± SD**.
 *
 * That last part is the whole content of D1 and easy to leave out. A DMR Firm never settles —
 * the crawl always perturbs, so a Firm that has found a good spot keeps testing and keeps
 * giving ground. The mean says "central"; the spread says "and wandering the entire line".
 * Showing only the instantaneous position, or only the mean, tells you the opposite of what
 * the model found.
 */

/**
 * @typedef {{positions: number[], values: number[], label: string}} Profile
 * @typedef {(world: import('../world.js').World) => Profile} ProfileFn
 */

/** How many days of Firm positions the rolling mean/SD covers. The original's `followAverage`. */
const FOLLOW_WINDOW = 2000;

/**
 * @param {HTMLCanvasElement} canvas
 * @param {object} spec
 * @param {ProfileFn} spec.above
 * @param {ProfileFn} spec.below
 * @param {boolean} [spec.showFirmSpread] the rolling mean ± SD marker
 */
export function lineView(canvas, { above, below, showFirmSpread = true }) {
  const ctx = /** @type {CanvasRenderingContext2D} */ (canvas.getContext('2d'));
  /** @type {Map<number, number[]>} */
  const history = new Map();

  return {
    /** @param {import('../world.js').World} world */
    draw(world) {
      const dpr = (typeof window !== 'undefined' && window.devicePixelRatio) || 1;
      const w = Math.round(canvas.clientWidth * dpr);
      const h = Math.round(canvas.clientHeight * dpr);
      if (canvas.width !== w || canvas.height !== h) {
        canvas.width = w;
        canvas.height = h;
      }

      const width = world.space.width;
      const pad = w * 0.09;
      const plotW = w - pad * 2;
      /** @param {number} modelX */
      const sx = (modelX) => pad + (modelX / width) * plotW;

      const midGap = h * 0.035;
      const topRule = h / 2 - midGap;
      const bottomRule = h / 2 + midGap;
      const maxBar = h * 0.36;

      ctx.setTransform(1, 0, 0, 1, 0, 0);
      ctx.fillStyle = '#fff';
      ctx.fillRect(0, 0, w, h);

      const up = above(world);
      const down = below(world);
      // A shared scale, so the two halves are comparable — which is the point of mirroring
      // them rather than drawing two charts.
      const peak = Math.max(1e-9, ...up.values.map(Math.abs), ...down.values.map(Math.abs));

      drawGrid(ctx, pad, w, topRule, bottomRule, maxBar, peak, up.label, down.label);

      const barWidth = Math.max(plotW / Math.max(up.positions.length, down.positions.length) * 0.55, 1);
      drawBars(ctx, up, sx, topRule, -1, maxBar, peak, barWidth);
      drawBars(ctx, down, sx, bottomRule, 1, maxBar, peak, barWidth);

      // The rules themselves, drawn over the bars so the baseline stays crisp.
      ctx.strokeStyle = '#000';
      ctx.lineWidth = Math.max(dpr * 1.5, 1.5);
      line(ctx, pad, topRule, w - pad, topRule);
      line(ctx, pad, bottomRule, w - pad, bottomRule);

      // Firms: pennant on the rule, plus where they have been.
      world.firms.forEach((f, i) => {
        const x = sx(f.position.x);
        const rule = i === 0 || world.firms.length > 2 ? topRule : bottomRule;
        const dir = rule === topRule ? 1 : -1;

        if (showFirmSpread) {
          let past = history.get(f.id);
          if (!past) { past = []; history.set(f.id, past); }
          past.push(f.position.x);
          if (past.length > FOLLOW_WINDOW) past.shift();
          drawSpread(ctx, past, sx, rule, dir, midGap, dpr);
        }
        pennant(ctx, x, rule, dir, midGap * 0.9, dpr);
      });
    },

    reset() {
      history.clear();
    },
  };
}

/**
 * Bars from a baseline, one per agent, greyscale by value — light where the value is high.
 * Double-encoded like everything else in the original: height and shade carry the same
 * number, which is what lets the figures survive being printed small.
 *
 * @param {CanvasRenderingContext2D} ctx @param {Profile} profile
 * @param {(x: number) => number} sx @param {number} baseline @param {number} sign
 * @param {number} maxBar @param {number} peak @param {number} barWidth
 */
function drawBars(ctx, profile, sx, baseline, sign, maxBar, peak, barWidth) {
  ctx.lineWidth = 1;
  for (let i = 0; i < profile.positions.length; i++) {
    const v = Math.abs(profile.values[i]);
    const t = v / peak;
    const height = t * maxBar;
    if (height < 0.5) continue;
    const grey = Math.round(t * 235);
    const x = sx(profile.positions[i]) - barWidth / 2;
    ctx.fillStyle = `rgb(${grey},${grey},${grey})`;
    ctx.strokeStyle = '#000';
    ctx.fillRect(x, baseline + (sign < 0 ? -height : 0), barWidth, height);
    ctx.strokeRect(x, baseline + (sign < 0 ? -height : 0), barWidth, height);
  }
}

/**
 * Rolling mean position with a standard-deviation bar. The difference between a Firm that
 * has settled and one that is still wandering.
 * @param {CanvasRenderingContext2D} ctx @param {number[]} past
 * @param {(x: number) => number} sx @param {number} rule @param {number} dir
 * @param {number} gap @param {number} dpr
 */
function drawSpread(ctx, past, sx, rule, dir, gap, dpr) {
  if (past.length < 2) return;
  const mean = past.reduce((a, b) => a + b, 0) / past.length;
  const variance = past.reduce((a, v) => a + (v - mean) ** 2, 0) / past.length;
  const sd = Math.sqrt(variance);

  const y = rule - dir * gap * 0.55;
  const height = gap * 0.45;
  const left = sx(mean - sd);
  const right = sx(mean + sd);

  ctx.fillStyle = '#fff';
  ctx.strokeStyle = '#000';
  ctx.lineWidth = Math.max(dpr, 1);
  ctx.fillRect(left, y - height / 2, Math.max(right - left, 1), height);
  ctx.strokeRect(left, y - height / 2, Math.max(right - left, 1), height);
}

/**
 * The Firm marker: a white pennant pointing at its position on the rule.
 * @param {CanvasRenderingContext2D} ctx @param {number} x @param {number} rule
 * @param {number} dir @param {number} size @param {number} dpr
 */
function pennant(ctx, x, rule, dir, size, dpr) {
  ctx.fillStyle = '#fff';
  ctx.strokeStyle = '#000';
  ctx.lineWidth = Math.max(dpr * 1.5, 1.5);
  ctx.beginPath();
  ctx.moveTo(x, rule);
  ctx.lineTo(x - size * 0.55, rule - dir * size);
  ctx.lineTo(x + size * 0.55, rule - dir * size);
  ctx.closePath();
  ctx.fill();
  ctx.stroke();
}

/**
 * Gridlines and the numeric scale, on the right as in the original — with the lower axis
 * running downwards, since it is a mirror.
 * @param {CanvasRenderingContext2D} ctx @param {number} pad @param {number} w
 * @param {number} topRule @param {number} bottomRule @param {number} maxBar
 * @param {number} peak @param {string} aboveLabel @param {string} belowLabel
 */
function drawGrid(ctx, pad, w, topRule, bottomRule, maxBar, peak, aboveLabel, belowLabel) {
  const ticks = niceTicks(peak);
  ctx.font = `${Math.round(pad * 0.24)}px ui-monospace, Menlo, monospace`;
  ctx.textBaseline = 'middle';

  for (const value of ticks) {
    const offset = (value / peak) * maxBar;
    ctx.strokeStyle = '#bbb';
    ctx.lineWidth = 1;
    line(ctx, pad, topRule - offset, w - pad, topRule - offset);
    line(ctx, pad, bottomRule + offset, w - pad, bottomRule + offset);

    ctx.fillStyle = '#555';
    ctx.fillText(format(value), w - pad + pad * 0.14, topRule - offset);
    ctx.fillText(format(value), w - pad + pad * 0.14, bottomRule + offset);
  }

  ctx.save();
  ctx.fillStyle = '#000';
  ctx.textAlign = 'center';
  ctx.translate(pad * 0.42, topRule - maxBar / 2);
  ctx.rotate(-Math.PI / 2);
  ctx.fillText(aboveLabel, 0, 0);
  ctx.restore();

  ctx.save();
  ctx.fillStyle = '#000';
  ctx.textAlign = 'center';
  ctx.translate(pad * 0.42, bottomRule + maxBar / 2);
  ctx.rotate(-Math.PI / 2);
  ctx.fillText(belowLabel, 0, 0);
  ctx.restore();
  ctx.textAlign = 'left';
}

/** @param {number} peak */
function niceTicks(peak) {
  const step = Math.pow(10, Math.floor(Math.log10(peak))) / 2;
  /** @type {number[]} */
  const out = [];
  for (let v = 0; v <= peak * 1.001; v += step) out.push(v);
  return out.length > 8 ? out.filter((_, i) => i % 2 === 0) : out;
}

/** @param {number} v */
function format(v) {
  return v === 0 ? '0' : Number(v.toPrecision(2)).toString();
}

/** @param {CanvasRenderingContext2D} ctx @param {number} x1 @param {number} y1 @param {number} x2 @param {number} y2 */
function line(ctx, x1, y1, x2, y2) {
  ctx.beginPath();
  ctx.moveTo(x1, y1);
  ctx.lineTo(x2, y2);
  ctx.stroke();
}
