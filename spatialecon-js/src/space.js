// @ts-check

/**
 * The geography a model run takes place in, and how agents are laid out in it.
 *
 * Four kinds, as the thesis framework describes (§4.2). They differ only in how they
 * constrain location choice; distance is Euclidean throughout.
 *
 * Two simplifications carried over deliberately from the original:
 *
 * - The original called the 2-D case a torus, but wrapping was disabled in every published
 *   run (`wrapSpace=false`). It is named PLANE here and wrapping is not implemented — if it
 *   is ever wanted, that is a new feature, not a restoration.
 * - Movement between locations is free. Only moving *goods*, and commuting, cost anything
 *   (§4.3.2).
 */

/** Dimensionless. All agents are co-located; distance is always zero. */
export const POINT = 'POINT';
/** Continuous 1-D line of length `width`. */
export const LINE = 'LINE';
/** Two discrete points separated by `width`. */
export const TWO_REGION = 'TWO_REGION';
/** Continuous 2-D plane, `width` on each side. */
export const PLANE = 'PLANE';

/**
 * @typedef {{x: number, y: number}} Pt
 * @typedef {{kind: string, width: number}} Space
 * @typedef {(actors: {placeAt: (p: Pt) => void}[], space: Space, rng: import('./rng.js').Rng) => void} Placement
 */

/** @param {number} x @param {number} y @returns {Pt} */
export function pt(x, y) {
  return { x, y };
}

export const ORIGIN = Object.freeze({ x: 0, y: 0 });

/** @param {Pt} a @param {Pt} b */
export function distanceBetween(a, b) {
  const dx = a.x - b.x, dy = a.y - b.y;
  return Math.sqrt(dx * dx + dy * dy);
}

/** Centre of the space. Firms are commonly placed here in the monocentric models. @param {Space} s */
export function centre(s) {
  switch (s.kind) {
    case POINT: return pt(0, 0);
    case LINE: return pt(s.width / 2, 0);
    case TWO_REGION: return pt(0, 0);
    default: return pt(s.width / 2, s.width / 2);
  }
}

/** x of region 0. Regions sit either side of the origin, so `width` is their separation. @param {Space} s */
export function regionZeroX(s) {
  return -s.width / 2;
}

/** x of region 1. @param {Space} s */
export function regionOneX(s) {
  return s.width / 2;
}

/** Distance between two points. Always zero in a POINT space. @param {Space} s @param {Pt} a @param {Pt} b */
export function distance(s, a, b) {
  return s.kind === POINT ? 0 : distanceBetween(a, b);
}

/**
 * Constrain a point to the space. Out-of-bounds coordinates are clamped, not wrapped; on a
 * line the y coordinate is flattened to zero.
 * @param {Space} s @param {Pt} p
 */
export function confine(s, p) {
  switch (s.kind) {
    case POINT: return pt(0, 0);
    case LINE: return pt(clamp(p.x, 0, s.width), 0);
    case TWO_REGION: return pt(p.x < 0 ? regionZeroX(s) : regionOneX(s), 0);
    default: return pt(clamp(p.x, 0, s.width), clamp(p.y, 0, s.width));
  }
}

/** @param {number} v @param {number} lo @param {number} hi */
function clamp(v, lo, hi) {
  return v < lo ? lo : Math.min(v, hi);
}

// --- Placements ----------------------------------------------------------------------
//
// In the original these were static methods on SpaceTools, invoked either from a hardcoded
// switch in Main or by reflection from a config line. Here a scenario names one directly.

/** All on the centre point. The monocentric Firm (§5.4.1). @returns {Placement} */
export function atCentre() {
  return (actors, space) => {
    const c = centre(space);
    for (const a of actors) a.placeAt(c);
  };
}

/** All on one given point. @param {number} x @param {number} y @returns {Placement} */
export function at(x, y) {
  return (actors) => {
    for (const a of actors) a.placeAt(pt(x, y));
  };
}

/**
 * Evenly spaced along the x axis between two fractions of width, inclusive.
 *
 * `evenlyAlongX(0, 1)` spreads across the whole line — the 19 People and 19 Firms of
 * §6.4.1, or the 50 People of §5.5.1. `evenlyAlongX(0.25, 0.75)` gives the restricted band
 * the Firms must find in the DMR game (§5.5.2).
 *
 * @param {number} fromFraction @param {number} toFraction @returns {Placement}
 */
export function evenlyAlongX(fromFraction, toFraction) {
  return (actors, space) => {
    const n = actors.length;
    const from = fromFraction * space.width, to = toFraction * space.width;
    for (let i = 0; i < n; i++) {
      const t = n === 1 ? 0.5 : i / (n - 1);
      actors[i].placeAt(pt(from + t * (to - from), 0));
    }
  };
}

/** Two points a given distance apart, alternating. §5.4.6, §6.4.2. @param {number} separation @returns {Placement} */
export function twoPointsApart(separation) {
  return (actors, space) => {
    const c = centre(space);
    for (let i = 0; i < actors.length; i++) {
      const dx = i % 2 === 0 ? -separation / 2 : separation / 2;
      actors[i].placeAt(pt(c.x + dx, c.y));
    }
  };
}

/** Uniformly at random across the space. @returns {Placement} */
export function randomly() {
  return (actors, space, rng) => {
    for (const a of actors) {
      a.placeAt(confine(space, pt(rng.upTo(space.width), rng.upTo(space.width))));
    }
  };
}

/** Evenly around a circle of this radius, centred on the space. @param {number} radius @returns {Placement} */
export function inCircle(radius) {
  return (actors, space) => {
    const n = actors.length;
    const c = centre(space);
    for (let i = 0; i < n; i++) {
      const theta = (2 * Math.PI * i) / n;
      actors[i].placeAt(pt(c.x + radius * Math.cos(theta), c.y + radius * Math.sin(theta)));
    }
  };
}

/**
 * Split between the two discrete regions.
 * @param {number} shareInRegionZero 0 puts everyone in region 1, 1 in region 0, 0.5 splits.
 * @returns {Placement}
 */
export function splitBetweenRegions(shareInRegionZero) {
  return (actors, space) => {
    const n = actors.length;
    const inZero = Math.round(n * shareInRegionZero);
    for (let i = 0; i < n; i++) {
      actors[i].placeAt(pt(i < inZero ? regionZeroX(space) : regionOneX(space), 0));
    }
  };
}
