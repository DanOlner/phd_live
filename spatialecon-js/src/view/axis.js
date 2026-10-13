// @ts-check

/**
 * Shared plumbing for the two x–y charts, `curve.js` and `correlate.js`.
 *
 * The time series plots a value against the day, and its x axis is therefore never anything
 * but "time" — it needs no tick machinery. These two plot a value against *another model
 * variable*, so both axes are quantities a reader has to be able to read off. That is the
 * whole of what lives here: canvas sizing, tick choice, and number formatting.
 *
 * Not folded back into `timeseries.js`. Its axis behaviour is deliberately different — bounds
 * that grow and never shrink, so a settling series is not made to look restless by an axis
 * that keeps refitting — and merging the two would mean parameterising that difference for
 * no gain.
 */

/**
 * Match the backing store to the CSS box, and hand back a cleared context.
 * @param {HTMLCanvasElement} canvas
 */
export function fitCanvas(canvas) {
  const ctx = /** @type {CanvasRenderingContext2D} */ (canvas.getContext('2d'));
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
  return { ctx, w, h, dpr };
}

/**
 * Pad a range, and give it width if it has none. A sweep that has not started yet, or a
 * variable that has not moved, otherwise divides by zero and draws nothing at all.
 * @param {number} lo @param {number} hi @param {number} [fraction]
 * @returns {[number, number]}
 */
export function padded(lo, hi, fraction = 0.06) {
  if (!Number.isFinite(lo) || !Number.isFinite(hi)) return [0, 1];
  if (hi <= lo) {
    const span = Math.max(Math.abs(hi), 1) * 0.5;
    return [lo - span, hi + span];
  }
  const margin = (hi - lo) * fraction;
  return [lo - margin, hi + margin];
}

/**
 * Round tick values inside a range — 1, 2 or 5 times a power of ten. Ticks at the raw data
 * bounds would be honest but unreadable: an axis labelled 0.4713 tells a reader less than one
 * labelled 0.5, and the point of these charts is that the numbers can be read off them.
 * @param {number} lo @param {number} hi @param {number} [target] roughly how many
 */
export function ticks(lo, hi, target = 5) {
  if (!(hi > lo)) return [lo];
  const raw = (hi - lo) / Math.max(target, 1);
  const magnitude = Math.pow(10, Math.floor(Math.log10(raw)));
  const step = [1, 2, 5, 10].map((m) => m * magnitude).find((s) => s >= raw) ?? magnitude * 10;
  /** @type {number[]} */
  const out = [];
  for (let v = Math.ceil(lo / step) * step; v <= hi + step * 1e-9; v += step) {
    out.push(Math.abs(v) < step * 1e-9 ? 0 : v); // snap the tick that should be exactly zero
  }
  return out;
}

/**
 * Decade ticks for a logarithmic axis, thinned so a wide range does not become a solid band
 * of labels. A1 spans forty-seven orders of magnitude; every decade labelled would be
 * illegible and would say nothing a tick every fifth decade does not.
 * @param {number} lo @param {number} hi both already log10'd
 */
export function logTicks(lo, hi) {
  const first = Math.ceil(lo);
  const last = Math.floor(hi);
  const decades = last - first + 1;
  const stride = Math.max(1, Math.ceil(decades / 6));
  /** @type {number[]} */
  const out = [];
  for (let e = first; e <= last; e += stride) out.push(e);
  return out;
}

/** @param {number} v */
export function format(v) {
  if (!Number.isFinite(v)) return '--';
  if (v === 0) return '0';
  if (Math.abs(v) >= 10000 || Math.abs(v) < 0.001) return v.toExponential(1);
  return Number(v.toPrecision(4)).toString();
}

/**
 * Push a column of labels apart so none is written over another, keeping their order and
 * staying inside the plot. Several curves converging on the same value is the normal case
 * here, not an edge case — it is what A1's closed form agreeing with the computed utility
 * looks like, and what A4's four series all reaching zero looks like — so the labels that
 * report it have to survive it.
 *
 * @param {number[]} ys desired positions @param {number} top @param {number} bottom
 * @param {number} gap minimum separation
 */
export function spread(ys, top, bottom, gap) {
  const order = ys.map((y, i) => ({ y, i })).sort((a, b) => a.y - b.y);
  for (let k = 1; k < order.length; k++) {
    order[k].y = Math.max(order[k].y, order[k - 1].y + gap);
  }
  const overflow = order.length > 0 ? order[order.length - 1].y - bottom : 0;
  if (overflow > 0) {
    for (const o of order) o.y = Math.max(top, o.y - overflow);
  }
  const out = new Array(ys.length);
  for (const o of order) out[o.i] = o.y;
  return out;
}

/**
 * Axis rules, ticks and labels, drawn in the original's monochrome.
 *
 * The x rule sits on y = 0 when the plotted range crosses it, and at the foot of the plot
 * otherwise. A3 is why: the Core Model production function returns negative output below α,
 * and a chart that shows the curve going negative without showing what it went negative
 * *past* has hidden the entire point of the model.
 *
 * @param {CanvasRenderingContext2D} ctx
 * @param {object} box
 * @param {number} box.left @param {number} box.right @param {number} box.top @param {number} box.bottom
 * @param {number} box.dpr
 * @param {[number, number]} xRange @param {[number, number]} yRange
 * @param {(v: number) => number} sx @param {(v: number) => number} sy
 * @param {string} [xLabel] @param {string} [yLabel]
 * @param {{yTicks?: number[], yFormat?: (v: number) => string}} [opts]
 *   for a logarithmic y axis, where the ticks are decades and the label is 10^e rather than
 *   the plotted exponent
 */
export function drawAxes(ctx, box, xRange, yRange, sx, sy, xLabel, yLabel, opts = {}) {
  const { left, right, top, bottom, dpr } = box;
  const fontSize = Math.max(Math.round((bottom - top) * 0.035), 9 * dpr);
  ctx.font = `${fontSize}px ui-monospace, Menlo, monospace`;
  ctx.lineWidth = Math.max(dpr, 1);

  const xt = ticks(xRange[0], xRange[1]);
  const yt = opts.yTicks ?? ticks(yRange[0], yRange[1]);
  const yFormat = opts.yFormat ?? format;

  // Faint grid first, so the data draws over it.
  ctx.strokeStyle = '#e4e4e4';
  ctx.beginPath();
  for (const t of xt) {
    ctx.moveTo(sx(t), top);
    ctx.lineTo(sx(t), bottom);
  }
  for (const t of yt) {
    ctx.moveTo(left, sy(t));
    ctx.lineTo(right, sy(t));
  }
  ctx.stroke();

  const zeroInside = yRange[0] < 0 && yRange[1] > 0 && !opts.yTicks;
  const baseline = zeroInside ? sy(0) : bottom;

  ctx.strokeStyle = '#000';
  ctx.beginPath();
  ctx.moveTo(left, baseline);
  ctx.lineTo(right, baseline);
  ctx.moveTo(left, top);
  ctx.lineTo(left, bottom);
  ctx.stroke();

  ctx.fillStyle = '#444';
  ctx.textBaseline = 'top';
  ctx.textAlign = 'center';
  for (const t of xt) {
    ctx.fillText(format(t), sx(t), bottom + 4 * dpr);
  }

  ctx.textBaseline = 'middle';
  ctx.textAlign = 'right';
  for (const t of yt) {
    ctx.fillText(yFormat(t), left - 4 * dpr, sy(t));
  }

  ctx.fillStyle = '#000';
  if (xLabel) {
    ctx.textAlign = 'center';
    ctx.textBaseline = 'bottom';
    ctx.fillText(xLabel, (left + right) / 2, bottom + fontSize * 2.4);
  }
  if (yLabel) {
    ctx.save();
    ctx.translate(left - fontSize * 3.2, (top + bottom) / 2);
    ctx.rotate(-Math.PI / 2);
    ctx.textAlign = 'center';
    ctx.textBaseline = 'bottom';
    ctx.fillText(yLabel, 0, 0);
    ctx.restore();
  }
  ctx.textAlign = 'left';
}
