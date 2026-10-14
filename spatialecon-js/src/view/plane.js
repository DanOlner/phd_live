// @ts-check

/**
 * The plane view: People and Firms in 2-D space. Models C1, C2, C6, C8, E8, B3.
 *
 * A faithful port of `PeopleDrawToSpace` + `FirmsDrawToSpace`, whose visual language is
 * documented in CODE_ANATOMY §5.9. Reference figure:
 * `ThesisCorrectionsLatex/ChapterTestingModels/Figures/IB_CEUS1_…__MainVizDensity.png`.
 *
 * Three things are carried over deliberately, because they are good information design
 * rather than limitations the author worked around:
 *
 * **Monochrome.** `Colours.java` is an empty stub, the colour ramps are commented out in
 * favour of lines marked `//b&w version`, and even `Actor.colour` resolves to a two-entry
 * grey ramp. The figures read well because of it.
 *
 * **Double encoding.** Marker size *and* greyscale both carry the same variable, over a 10×
 * size range and a full black-to-white ramp. It is why the figures survive being printed
 * small.
 *
 * **Density as ink.** Each Person is drawn twice: a faint circle at the density-cost radius,
 * and a marker at their position. Where People crowd, the circles overlap and the page goes
 * dark — so the crowding measure and the picture of it are the same object, with no legend
 * needed. This is the best idea in the original visualisation. It also makes C1's fringe
 * visible for free: the ring where the ink runs out is the cutoff distance beyond which
 * utility can no longer be equalised (CODE_ANATOMY §5.3).
 */

import { drawBuyLines } from './buylines.js';
import { drawEqualisingLine } from './equalise.js';

/** Marker size range, in model units, as in the original's `mapLowToHigh`. */
const MIN_MARKER = 0.005;
const MAX_MARKER = 0.05;

/** @typedef {'auto' | 'utility' | 'density' | 'neighbours' | 'group'} ShadeBy */

/**
 * The von Thünen palette — figures 5.15 and 5.16.
 *
 * Four greys, light to dark, one per cost group. **Not the palette in the source tree**:
 * `gl.nextActorColour` as left builds only two greys (0 and 150) and hands Firms 2 and 3
 * *random RGB*, which is plainly not what the figures show. Directly above it, commented
 * out, sits a four-entry list — 220, 190, 160, 130 — matching the published figures. Same
 * disease as CODE_ANATOMY §1: the snapshot is not the state any figure was made in.
 */
const GROUP_GREYS = [220, 190, 160, 130];

/**
 * One shape per cost group, so groups stay distinguishable in print where four greys alone
 * would not be. Sizes are the original's, chosen so the shapes look about equally large.
 * @type {('square'|'circle'|'triangle'|'pentagon')[]}
 */
const GROUP_SHAPES = ['square', 'circle', 'triangle', 'pentagon'];

/**
 * @param {HTMLCanvasElement} canvas
 * @param {object} [options]
 * @param {ShadeBy} [options.shadeBy]
 * @param {boolean} [options.showDensityCircles]
 * @param {boolean} [options.colour] hue for Firm identity only; quantities stay greyscale
 * @param {boolean} [options.showBuyLines] the trades themselves — `buylines.js`
 * @param {boolean} [options.showEqualisingLine] C1's cutoff ring — `equalise.js`
 * @param {boolean} [options.showMeanPoint] the 2011 renderer's ring at the mean of every actor — H1
 * @param {string[]} [options.firmFills] one fill per Firm, by index, in place of the white —
 *   H1 tells its two Firms apart by the original's two glyphs, white and filled
 */
export function planeView(canvas, options = {}) {
  const ctx = /** @type {CanvasRenderingContext2D} */ (canvas.getContext('2d'));
  const firmFills = options.firmFills;
  let shadeBy = options.shadeBy ?? 'auto';
  let showDensityCircles = options.showDensityCircles ?? true;
  let colour = options.colour ?? false;
  // All default off. They answer specific questions, and left on they compete for ink with
  // the density picture, which is the default reading of these figures.
  let showBuyLines = options.showBuyLines ?? false;
  let showEqualisingLine = options.showEqualisingLine ?? false;
  let showMeanPoint = options.showMeanPoint ?? false;

  return {
    get shadeBy() { return shadeBy; },
    set shadeBy(v) { shadeBy = v; },
    get showDensityCircles() { return showDensityCircles; },
    set showDensityCircles(v) { showDensityCircles = v; },
    get colour() { return colour; },
    set colour(v) { colour = v; },
    get showBuyLines() { return showBuyLines; },
    set showBuyLines(v) { showBuyLines = v; },
    get showEqualisingLine() { return showEqualisingLine; },
    set showEqualisingLine(v) { showEqualisingLine = v; },
    get showMeanPoint() { return showMeanPoint; },
    set showMeanPoint(v) { showMeanPoint = v; },

    /** @param {import('../world.js').World} world */
    draw(world) {
      const width = world.space.width;
      const dpr = window.devicePixelRatio || 1;
      const cssSize = Math.min(canvas.clientWidth, canvas.clientHeight);
      if (canvas.width !== Math.round(cssSize * dpr)) {
        canvas.width = canvas.height = Math.round(cssSize * dpr);
      }

      const px = canvas.width / width; // model units -> device pixels
      /** @param {number} v */
      const s = (v) => v * px;

      ctx.setTransform(1, 0, 0, 1, 0, 0);
      ctx.fillStyle = '#fff';
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      // 'auto' picks the von Thünen view whenever People are tied to Firms, because in
      // those models the group IS the variable of interest and a density ramp hides it.
      const grouped = world.people.some((p) => p.assignedFirm);
      const mode = shadeBy === 'auto' ? (grouped ? 'group' : 'density') : shadeBy;
      const useGroups = mode === 'group' && grouped;

      const values = world.people.map((p) => valueOf(p, useGroups ? 'density' : mode));
      const lo = Math.min(...values, 0);
      const hi = Math.max(...values, 1e-9);

      // 1. The density radius of every Person, faint. Overlap is the picture of crowding.
      if (showDensityCircles && world.config.densityRadius > 0) {
        ctx.strokeStyle = 'rgba(0,0,0,0.31)'; // the original's stroke(0, 80)
        ctx.lineWidth = Math.max(s(0.002), 0.75);
        const r = s(world.config.densityRadius);
        for (const p of world.people) {
          ctx.beginPath();
          ctx.arc(s(p.position.x), s(p.position.y), r, 0, Math.PI * 2);
          ctx.stroke();
        }
      }

      // 1b. The trades, under everything else so they never obscure what they connect.
      if (showBuyLines) drawBuyLines(ctx, world, s, dpr);

      // 2. The People themselves: greyscale square sized by the same variable, or a white
      //    circle where utility is exactly zero -- the "nowhere was affordable" marker, and
      //    the thing to look for as the density cost rises in C8.
      ctx.lineWidth = Math.max(s(0.004), 0.5);
      world.people.forEach((p, i) => {
        const t = hi > lo ? (values[i] - lo) / (hi - lo) : 0.5;
        const x = s(p.position.x);
        const y = s(p.position.y);

        if (p.utility === 0) {
          ctx.fillStyle = '#fff';
          ctx.strokeStyle = '#000';
          ctx.beginPath();
          ctx.arc(x, y, s(0.015), 0, Math.PI * 2);
          ctx.fill();
          ctx.stroke();
          return;
        }

        ctx.strokeStyle = '#000';

        // Von Thünen: a shape and a grey per cost group, fixed size. What varies between
        // People here is which group they are in, not how much density they bought, so
        // encoding density in the marker would bury the result.
        if (useGroups && p.assignedFirm) {
          const g = groupIndexOf(world, p.assignedFirm);
          const grey = GROUP_GREYS[g % GROUP_GREYS.length];
          ctx.fillStyle = `rgb(${grey},${grey},${grey})`;
          groupShape(ctx, GROUP_SHAPES[g % GROUP_SHAPES.length], x, y, s(0.022));
          return;
        }

        const size = s(MIN_MARKER + t * (MAX_MARKER - MIN_MARKER));
        const grey = Math.round(t * 255);
        ctx.fillStyle = `rgb(${grey},${grey},${grey})`;
        ctx.fillRect(x - size / 2, y - size / 2, size, size);
        ctx.strokeRect(x - size / 2, y - size / 2, size, size);
      });

      // 3. Firms: the 12-vertex plus glyph, white with a black outline so it stays legible
      //    against the darkest part of the ink. `FirmsDrawToSpace` has both a filled and a
      //    white variant; the published figures use the white one. H1 uses both, one per
      //    Firm, so its page can name them the white Firm and the black.
      world.firms.forEach((f, i) => {
        const x = s(f.position.x);
        const y = s(f.position.y);
        const size = s(0.018);

        ctx.fillStyle = firmFills?.[i] ?? (colour ? firmHue(i, world.firms.length) : '#fff');
        ctx.strokeStyle = '#000';
        ctx.lineWidth = Math.max(s(0.005), 1.5);
        plusGlyph(ctx, x, y, size);
        ctx.fill();
        ctx.stroke();
      });

      // 4. The equalising cutoff, over everything: it is a claim *about* the settlement, so
      //    it should read as annotation rather than as part of the picture.
      if (showEqualisingLine) drawEqualisingLine(ctx, world, s, dpr);

      // 4b. The 2011 ring, annotation for the same reason.
      if (showMeanPoint) drawMeanPoint(ctx, world, s, dpr);

      // 5. The von Thünen legend: shape, then the cost that defines the group.
      if (useGroups) drawGroupLegend(ctx, world, px, firmFills);
    },
  };
}

/**
 * Which cost varies across the Firms, and each Firm's value of it. This is what the legend
 * labels — and the whole point of C6/C6c, since the sorting runs one way for a non-spatial
 * cost and the other way for a spatial one.
 * @param {import('../world.js').World} world
 */
function groupVariable(world) {
  const c = world.config;
  if (c.rangeCommute) return { name: 'commute cost', of: (/** @type {any} */ f) => f.commuteCost };
  if (c.rangeWage) return { name: 'wage', of: (/** @type {any} */ f) => f.wage };
  if (c.rangeDelivery) return { name: 'distance cost', of: (/** @type {any} */ f) => f.deliveryCost };
  return { name: 'Firm', of: (/** @type {any} */ f) => f.id };
}

/** @param {import('../world.js').World} world @param {any} firm */
function groupIndexOf(world, firm) {
  return world.firms.indexOf(firm);
}

/**
 * Where each Firm has a fill of its own, its glyph goes beside its workforce's shape, so the
 * legend says which Firm each group works for as well as what it is paid.
 *
 * @param {CanvasRenderingContext2D} ctx @param {import('../world.js').World} world
 * @param {number} px model units -> device pixels @param {string[]} [firmFills]
 */
function drawGroupLegend(ctx, world, px, firmFills) {
  const variable = groupVariable(world);
  const size = 0.022 * px;
  const rowHeight = size * 2.4;
  const x = size * 2.2;
  let y = size * 2.4;
  const textX = x + size * (firmFills ? 3 : 1.6);

  ctx.font = `${Math.round(size * 1.15)}px ui-monospace, Menlo, monospace`;
  ctx.textBaseline = 'middle';
  ctx.fillStyle = '#000';
  ctx.fillText(`${variable.name}:`, x - size * 1.2, y);
  y += rowHeight;

  world.firms.forEach((f, i) => {
    const grey = GROUP_GREYS[i % GROUP_GREYS.length];
    ctx.fillStyle = `rgb(${grey},${grey},${grey})`;
    ctx.strokeStyle = '#000';
    ctx.lineWidth = Math.max(0.004 * px, 0.5);
    groupShape(ctx, GROUP_SHAPES[i % GROUP_SHAPES.length], x, y, size);

    if (firmFills) {
      ctx.fillStyle = firmFills[i] ?? '#fff';
      plusGlyph(ctx, x + size * 1.65, y, size * 0.22);
      ctx.fill();
      ctx.stroke();
    }

    ctx.fillStyle = '#000';
    ctx.fillText(`= ${round(variable.of(f))}`, textX, y);
    y += rowHeight;
  });
}

/** @param {number} v */
function round(v) {
  return Number(v.toPrecision(3)).toString();
}

/**
 * One marker per cost group. Shapes and their relative sizes are the original's, tuned so
 * the four read as about equally large. Fills and strokes as already set on the context.
 * @param {CanvasRenderingContext2D} ctx
 * @param {'square'|'circle'|'triangle'|'pentagon'} kind
 * @param {number} x @param {number} y @param {number} u
 */
function groupShape(ctx, kind, x, y, u) {
  if (kind === 'square') {
    ctx.fillRect(x - u / 2, y - u / 2, u, u);
    ctx.strokeRect(x - u / 2, y - u / 2, u, u);
    return;
  }
  ctx.beginPath();
  if (kind === 'circle') {
    ctx.arc(x, y, (u * 1.2) / 2, 0, Math.PI * 2);
  } else if (kind === 'triangle') {
    const r = u * 0.8;
    ctx.moveTo(x, y - r);
    ctx.lineTo(x + 0.8660254037844386 * r, y + 0.5 * r);
    ctx.lineTo(x - 0.8660254037844386 * r, y + 0.5 * r);
    ctx.closePath();
  } else {
    // Regular pentagon, point up. s1=sin72 c1=cos72 s2=sin36 c2=cos36, as the original.
    const r = u * 0.7;
    const s1 = 0.9510565162951535, c1 = 0.30901699437494745;
    const s2 = 0.5877852522924731, c2 = 0.8090169943749475;
    ctx.moveTo(x, y - r);
    ctx.lineTo(x - s1 * r, y - c1 * r);
    ctx.lineTo(x - s2 * r, y + c2 * r);
    ctx.lineTo(x + s2 * r, y + c2 * r);
    ctx.lineTo(x + s1 * r, y - c1 * r);
    ctx.closePath();
  }
  ctx.fill();
  ctx.stroke();
}

/**
 * The original's cross: twelve vertices, arms 4 units long and 1.5 wide.
 *
 * Exported because a Firm has to look like a Firm everywhere — `sampler.js` draws them too,
 * and a second hand-rolled glyph would drift.
 * @param {CanvasRenderingContext2D} ctx @param {number} x @param {number} y @param {number} u
 */
export function plusGlyph(ctx, x, y, u) {
  /** @type {[number, number][]} */
  const v = [
    [0.75, -0.75], [2, -0.75], [2, 0.75], [0.75, 0.75], [0.75, 2], [-0.75, 2],
    [-0.75, 0.75], [-2, 0.75], [-2, -0.75], [-0.75, -0.75], [-0.75, -2], [0.75, -2],
  ];
  ctx.beginPath();
  v.forEach(([dx, dy], i) => {
    const px = x + dx * u, py = y + dy * u;
    if (i === 0) ctx.moveTo(px, py);
    else ctx.lineTo(px, py);
  });
  ctx.closePath();
}

/**
 * The ring from the August 2011 video (H1): the mean position of every actor, People and
 * Firms alike, as `SpaceTools.drawMeanCentrePoint` drew it — a filled circle inside a ring,
 * white on that renderer's dark ground and so black on white here.
 *
 * It is not an agent. Between two settlements of equal size the mean falls in the gap, which
 * is what made it look like one, and in the video it vanished whenever the settlements
 * overlapped because People's raised utility squares covered it.
 *
 * @param {CanvasRenderingContext2D} ctx @param {import('../world.js').World} world
 * @param {(v: number) => number} s model units -> device pixels @param {number} dpr
 */
function drawMeanPoint(ctx, world, s, dpr) {
  const actors = [...world.people, ...world.firms];
  if (actors.length === 0) return;
  const x = s(actors.reduce((a, p) => a + p.position.x, 0) / actors.length);
  const y = s(actors.reduce((a, p) => a + p.position.y, 0) / actors.length);
  // The original's 15-pixel ring on a window about 500 pixels across, scaled to this one.
  const r = Math.max(s(world.space.width * 0.015), 4 * dpr);
  ctx.lineWidth = Math.max(1.5 * dpr, 1);
  ctx.strokeStyle = '#000';
  ctx.fillStyle = '#fff';
  ctx.beginPath();
  ctx.arc(x, y, r, 0, Math.PI * 2);
  ctx.fill();
  ctx.stroke();
  ctx.beginPath();
  ctx.arc(x, y, r * 0.55, 0, Math.PI * 2);
  ctx.stroke();
}

/**
 * Hue for Firm *identity* only, never for a quantity. The original's two-grey palette was
 * straining to do exactly this, and it is the one place colour genuinely helps.
 * @param {number} i @param {number} n
 */
function firmHue(i, n) {
  return `hsl(${Math.round((360 * i) / Math.max(n, 1))}, 70%, 45%)`;
}

/** @param {import('../agents.js').Person} p @param {ShadeBy} by */
function valueOf(p, by) {
  if (by === 'utility') return p.utility;
  if (by === 'neighbours') return p.neighboursInDensityRadius;
  return p.densityCost; // 'density', and the fallback when 'group' has no groups to use
}
