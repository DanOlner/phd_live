// @ts-check

/**
 * Stacked time series on one time axis. H1's second panel.
 *
 * Several quantities over the same days, a strip each, so that what happens in one can be
 * read against what happens in the others at the same moment. In H1 that is the whole case:
 * the People's utility, the Firms' revenue and speed, and the separation all of it produces,
 * one under another, so a breakaway can be seen to be one event in all four. One canvas
 * rather than a canvas per strip, because separate charts each size their own label margin,
 * and their time axes stop lining up the moment two labels differ in length.
 *
 * Two rules carried over from `timeseries.js`, for its reasons: a rolling window, and bounds
 * that only grow. Each strip has bounds of its own, set by its bold tracks alone.
 *
 * Per strip, optionally:
 *
 * - **A cloud**: one faint line per agent, under the bold tracks. It does not set the bounds.
 *   A run starts from a random scatter, and the first few days' stragglers would otherwise
 *   fix the scale for good and flatten everything after them; instead they run off the strip
 *   and are clipped at its edge. Drawn at most one vertex every couple of pixels, chosen by
 *   the sample's place in the whole run rather than on screen, so the same samples are kept
 *   as the window scrolls and the lines do not shimmer.
 * - **A log axis**, for a quantity whose interesting values sit close to zero.
 * - **A reference line**, as the time series has.
 *
 * And for the panel as a whole, one shading predicate: grey bands behind every strip on the
 * samples where it held, which is what lets the eye line the strips up.
 *
 * Redrawn only when there is something new to draw. The page calls every view every frame,
 * paused or not, and a cloud of 160 lines is not free.
 */

import { format, spread } from './axis.js';

/** Plotted floor for a log strip; anything at or below it sits on the floor. */
const LOG_FLOOR = 1e-3;

/** Pixels per cloud vertex, at the most. */
const CLOUD_PIXELS = 2;

/**
 * @typedef {object} StripTrack
 * @property {string} label
 * @property {(world: import('../world.js').World) => number} get
 * @property {boolean} [dash]
 * @property {number} [grey] 0 for black, the default, up to 255 for white
 * @property {boolean} [hollow] white edged with black, for something the page calls white:
 *   a white line on a white page has to be drawn by its edges, as the plane draws a white Firm
 */

/**
 * @typedef {object} Strip
 * @property {string} title
 * @property {StripTrack[]} tracks bold lines; these set the strip's bounds
 * @property {(world: import('../world.js').World) => number[]} [cloud] one value per agent,
 *   the same agent in the same place every day
 * @property {number} [weight] share of the panel's height, relative to the others; default 1
 * @property {boolean} [includeZero] for a quantity that cannot go below it, a speed say: the
 *   axis starts there rather than at whatever the smallest value so far happened to be
 * @property {boolean} [log]
 * @property {number} [reference]
 * @property {string} [referenceLabel]
 */

/**
 * @param {HTMLCanvasElement} canvas
 * @param {object} spec
 * @param {Strip[]} spec.strips top to bottom
 * @param {number} [spec.window] samples kept; 0 keeps everything
 * @param {(world: import('../world.js').World) => boolean} [spec.shade]
 */
export function stripsView(canvas, { strips, window: windowSize = 0, shade }) {
  const ctx = /** @type {CanvasRenderingContext2D} */ (canvas.getContext('2d'));

  /** @param {Strip} s @param {number} v */
  const plotted = (s, v) => (s.log ? Math.log10(Math.max(v, LOG_FLOOR)) : v);

  const state = strips.map((s) => ({
    /** One array per track. @type {number[][]} */
    series: s.tracks.map(() => /** @type {number[]} */ ([])),
    /** One row per sample, one value per agent. @type {number[][]} */
    cloud: /** @type {number[][]} */ ([]),
    lo: Infinity,
    hi: -Infinity,
  }));
  /** @type {boolean[]} */
  const shaded = [];
  /** Samples taken in the whole run, so a cloud vertex can be chosen by its place in it. */
  let total = 0;
  let lastDay = -1;
  let dirty = true;
  let lastW = -1;
  let lastH = -1;

  /** Bounds from what is on screen now, plus the reference lines. */
  function refit() {
    strips.forEach((s, k) => {
      const st = state[k];
      st.lo = Infinity;
      st.hi = -Infinity;
      if (s.reference !== undefined) include(k, s.reference);
      if (s.includeZero && !s.log) include(k, 0);
      st.series.forEach((values) => values.forEach((v) => include(k, v)));
    });
    dirty = true;
  }

  /** @param {number} k @param {number} v */
  function include(k, v) {
    const u = plotted(strips[k], v);
    if (!Number.isFinite(u)) return;
    state[k].lo = Math.min(state[k].lo, u);
    state[k].hi = Math.max(state[k].hi, u);
  }

  /** @param {import('../world.js').World} world */
  function sample(world) {
    shaded.push(shade ? shade(world) : false);
    strips.forEach((s, k) => {
      s.tracks.forEach((t, i) => {
        const v = t.get(world);
        state[k].series[i].push(v);
        include(k, v);
      });
      if (s.cloud) state[k].cloud.push(s.cloud(world).slice());
    });
    total++;
    if (windowSize > 0 && shaded.length > windowSize) {
      shaded.shift();
      for (const st of state) {
        st.series.forEach((values) => values.shift());
        if (st.cloud.length > 0) st.cloud.shift();
      }
    }
    dirty = true;
  }

  /**
   * Once per day, however often drawn. And not before the first day has run: until every
   * agent has acted once, utility is zero and no revenue has been counted, and a zero in the
   * bounds would squash every strip for the rest of the run.
   * @param {import('../world.js').World} world
   */
  function observe(world) {
    if (world.day !== lastDay && world.day > 0) {
      lastDay = world.day;
      sample(world);
    }
  }

  refit();

  return {
    /**
     * Take the day's sample without drawing. A headless run wants the picture once, at the
     * end, and a cloud redrawn on every day of a long run is nearly all the cost of it.
     */
    observe,

    /** Refit every strip to what is on screen, forgetting anything that has scrolled off. */
    rescale: refit,

    reset() {
      for (const st of state) {
        st.series.forEach((values) => { values.length = 0; });
        st.cloud.length = 0;
      }
      shaded.length = 0;
      total = 0;
      lastDay = -1;
      refit();
    },

    /** @param {import('../world.js').World} world */
    draw(world) {
      observe(world);

      const dpr = (typeof window !== 'undefined' && window.devicePixelRatio) || 1;
      const w = Math.round(canvas.clientWidth * dpr);
      const h = Math.round(canvas.clientHeight * dpr);
      if (!dirty && w === lastW && h === lastH) return;
      dirty = false;
      lastW = w;
      lastH = h;
      if (canvas.width !== w || canvas.height !== h) {
        canvas.width = w;
        canvas.height = h;
      }

      ctx.setTransform(1, 0, 0, 1, 0, 0);
      ctx.fillStyle = '#fff';
      ctx.fillRect(0, 0, w, h);

      const font = Math.min(Math.max(Math.round(w * 0.0135), Math.round(9 * dpr)), Math.round(12 * dpr));
      ctx.font = `${font}px ui-monospace, Menlo, monospace`;
      ctx.textBaseline = 'middle';
      ctx.textAlign = 'left';
      const charW = font * 0.62;

      // Fixed from the labels' names plus room for any value, not from the values on screen,
      // so the plot does not change width as a number gains a digit.
      let widest = 0;
      for (const s of strips) {
        for (const t of s.tracks) widest = Math.max(widest, t.label.length + 8);
        if (s.reference !== undefined) widest = Math.max(widest, (s.referenceLabel ?? '').length + 1);
      }
      const labelW = Math.min(widest * charW, w * 0.3);
      const rangeW = 6.5 * charW;
      const left = font * 0.5;
      const right = w - rangeW - labelW;

      const titleH = font * 1.7;
      const gap = font * 0.8;
      const usable = h - font * 0.8 - strips.length * titleH - (strips.length - 1) * gap;
      const weights = strips.reduce((a, s) => a + (s.weight ?? 1), 0);

      const n = shaded.length;
      /** @param {number} i */
      const sx = (i) => left + (n > 1 ? i / (n - 1) : 0) * (right - left);

      let y = font * 0.2;
      strips.forEach((s, k) => {
        ctx.fillStyle = '#555';
        ctx.fillText(s.title, left, y + titleH * 0.5);
        const top = y + titleH;
        const bottom = top + (usable * (s.weight ?? 1)) / weights;
        drawStrip(s, state[k], top, bottom);
        y = bottom + gap;
      });

      /**
       * @param {Strip} s @param {typeof state[number]} st
       * @param {number} top @param {number} bottom
       */
      function drawStrip(s, st, top, bottom) {
        let lo = st.lo, hi = st.hi;
        if (!(hi > lo)) {
          const mid = Number.isFinite(lo) ? lo : 0;
          lo = mid - 0.5;
          hi = mid + 0.5;
        }
        const margin = (hi - lo) * 0.08;
        const yLo = lo - margin, yHi = hi + margin;
        /** @param {number} u already in plotted units */
        const sy = (u) => bottom - ((u - yLo) / (yHi - yLo)) * (bottom - top);

        // The bands first, so everything else draws over them.
        ctx.fillStyle = '#ececec';
        const column = n > 1 ? (right - left) / (n - 1) : right - left;
        for (let i = 0; i < n; i++) {
          if (!shaded[i]) continue;
          let j = i;
          while (j + 1 < n && shaded[j + 1]) j++;
          const x0 = Math.max(left, sx(i) - column / 2);
          const x1 = Math.min(right, sx(j) + column / 2);
          ctx.fillRect(x0, top, Math.max(x1 - x0, 1), bottom - top);
          i = j;
        }

        ctx.strokeStyle = '#d4d4d4';
        ctx.lineWidth = Math.max(dpr, 1);
        ctx.strokeRect(left, top, right - left, bottom - top);

        // Scale, at the strip's right edge: the extremes of what it has plotted. Strips have
        // bounds of their own, and a reader has to be told so.
        const inverse = (/** @type {number} */ u) => (s.log ? Math.pow(10, u) : u);
        if (st.hi >= st.lo) {
          ctx.fillStyle = '#888';
          ctx.fillText(short(inverse(st.hi)), right + 4 * dpr, sy(st.hi));
          if (st.hi > st.lo) ctx.fillText(short(inverse(st.lo)), right + 4 * dpr, sy(st.lo));
        }

        /** @type {{y: number, text: string, grey: number}[]} */
        const labels = [];

        if (s.reference !== undefined) {
          const ry = sy(plotted(s, s.reference));
          ctx.strokeStyle = '#000';
          ctx.lineWidth = Math.max(dpr, 1);
          ctx.setLineDash([6 * dpr, 4 * dpr]);
          ctx.beginPath();
          ctx.moveTo(left, ry);
          ctx.lineTo(right, ry);
          ctx.stroke();
          ctx.setLineDash([]);
          labels.push({ y: ry, text: s.referenceLabel ?? format(s.reference), grey: 0 });
        }

        if (n >= 2 && st.cloud.length === n) {
          const most = Math.max(60, Math.floor((right - left) / (CLOUD_PIXELS * dpr)));
          const step = Math.max(1, Math.ceil(n / most));
          const first = total - n;
          /** @type {number[]} */
          const kept = [];
          for (let i = 0; i < n; i++) if ((first + i) % step === 0 || i === n - 1) kept.push(i);
          const xs = kept.map(sx);
          const agents = st.cloud[n - 1].length;
          ctx.strokeStyle = 'rgba(0,0,0,0.1)';
          ctx.lineWidth = Math.max(0.6 * dpr, 0.6);
          for (let a = 0; a < agents; a++) {
            ctx.beginPath();
            clippedPolyline(ctx, xs, kept.map((i) => sy(plotted(s, st.cloud[i][a]))), top, bottom);
            ctx.stroke();
          }
        }

        s.tracks.forEach((t, i) => {
          const values = st.series[i];
          if (values.length < 2) return;
          ctx.beginPath();
          let pen = false;
          for (let j = 0; j < values.length; j++) {
            const u = plotted(s, values[j]);
            if (!Number.isFinite(u)) { pen = false; continue; }
            if (pen) ctx.lineTo(sx(j), sy(u));
            else ctx.moveTo(sx(j), sy(u));
            pen = true;
          }
          const width = t.dash ? Math.max(1.1 * dpr, 1.1) : Math.max(1.5 * dpr, 1.5);
          // Over a cloud, a white edge first, so the mean reads as a line and not as the
          // densest part of the cloud.
          if (s.cloud) {
            ctx.strokeStyle = '#fff';
            ctx.lineWidth = width + 2.5 * dpr;
            ctx.stroke();
          }
          const grey = t.grey ?? 0;
          if (t.dash) ctx.setLineDash([4 * dpr, 3 * dpr]);
          if (t.hollow) {
            // The edges, then a white core inside them, wide enough to read as white.
            const core = width + 0.5 * dpr;
            ctx.strokeStyle = `rgb(${grey},${grey},${grey})`;
            ctx.lineWidth = core + 1.6 * dpr;
            ctx.stroke();
            ctx.strokeStyle = '#fff';
            ctx.lineWidth = core;
            ctx.stroke();
          } else {
            ctx.strokeStyle = `rgb(${grey},${grey},${grey})`;
            ctx.lineWidth = width;
            ctx.stroke();
          }
          ctx.setLineDash([]);

          const last = values[values.length - 1];
          if (Number.isFinite(last)) {
            labels.push({ y: sy(plotted(s, last)), text: `${t.label} ${format(last)}`, grey });
          }
        });

        const placed = spread(labels.map((l) => l.y), top + font * 0.5, bottom - font * 0.5, font * 1.15);
        labels.forEach((l, i) => {
          ctx.fillStyle = `rgb(${l.grey},${l.grey},${l.grey})`;
          ctx.fillText(l.text, right + rangeW, placed[i]);
        });
      }
    },
  };
}

/**
 * Draw a polyline, clipped to a horizontal band, into the current path.
 *
 * By hand rather than with `ctx.clip()`, so that the recording contexts the tests and the
 * snapshot tool use — which know nothing of clipping regions — draw exactly what the page
 * does. A segment leaving the band is cut where it crosses the edge; one wholly outside is
 * not drawn; a non-finite value lifts the pen.
 *
 * @param {Pick<CanvasRenderingContext2D, 'moveTo' | 'lineTo'>} ctx
 * @param {number[]} xs @param {number[]} ys @param {number} top @param {number} bottom
 */
export function clippedPolyline(ctx, xs, ys, top, bottom) {
  /** Whether the pen is down at the end of the last segment drawn. */
  let pen = false;
  for (let k = 1; k < xs.length; k++) {
    const x0 = xs[k - 1], y0 = ys[k - 1], x1 = xs[k], y1 = ys[k];
    if (![x0, y0, x1, y1].every(Number.isFinite)) { pen = false; continue; }
    let t0 = 0, t1 = 1;
    const dy = y1 - y0;
    if (dy === 0) {
      if (y0 < top || y0 > bottom) { pen = false; continue; }
    } else {
      const a = (top - y0) / dy, b = (bottom - y0) / dy;
      t0 = Math.max(0, Math.min(a, b));
      t1 = Math.min(1, Math.max(a, b));
      if (t0 > t1) { pen = false; continue; }
    }
    if (!pen || t0 > 0) ctx.moveTo(x0 + (x1 - x0) * t0, y0 + dy * t0);
    ctx.lineTo(x0 + (x1 - x0) * t1, y0 + dy * t1);
    pen = t1 === 1;
  }
}

/** Three figures: enough to read a scale off, short enough for a narrow column. @param {number} v */
function short(v) {
  if (!Number.isFinite(v)) return '--';
  if (v === 0) return '0';
  if (Math.abs(v) >= 10000 || Math.abs(v) < 0.001) return v.toExponential(1);
  return Number(v.toPrecision(3)).toString();
}
