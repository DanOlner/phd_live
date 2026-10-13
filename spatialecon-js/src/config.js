// @ts-check

/**
 * Quirks, Parameters, and the model config.
 *
 * Replaces `utils/gl.java` (131 public static fields, four near-duplicate setUp methods)
 * and the 29 `*MouseChanger` classes. Nothing here is global: two models can run on one
 * page, which the original could not do.
 */

import { LINE, PLANE, POINT, TWO_REGION, atCentre, randomly } from './space.js';

// --- Quirks --------------------------------------------------------------------------

/**
 * @typedef {object} Quirks
 * @property {boolean} densityDivisorIncludesFirms
 *   Divisor for the summed density weights. ORIGINAL: Firms + People. THESIS (§4.4):
 *   "the total number of People in the model run". 0.2%–0.7% at published agent counts.
 * @property {boolean} commuteIsRoundTrip
 *   ORIGINAL: `remainingTime = 1 - distance * commuteCost` — one way, despite the code's
 *   own comment two lines above reading `//2*distance = commute cost`. THESIS (§4.6.1):
 *   `l_af = t_a - 2*t_af`, and the half-day reachability cutoff that follows.
 * @property {boolean} noiseIsFullAmplitude
 *   ORIGINAL: ±noise/2 of the optimum. THESIS (§5.5.2): "randomised in a range of plus or
 *   minus half its total value", which needs ±noise.
 * @property {boolean} substitutesUseLimitFormula
 *   ORIGINAL: `price * budget` at ρ ≥ 1. LIMIT: the ρ→1⁻ limit is `budget / price`. The
 *   original is wrong but never ran — ρ was clamped to 0.99. CODE_ANATOMY §7.1.
 */

/**
 * Points where the 2013 implementation departs from what the thesis text states, each
 * documented in CODE_ANATOMY §7. Rather than silently choosing one reading, both are
 * implemented and selectable, so the difference can be measured rather than argued about.
 *
 * Flipping a flag is a legitimate experiment, not a bug fix — so far none of them has been
 * shown to change a published result, which is itself worth knowing: the discrepancies are
 * real but immaterial, and the thesis' conclusions do not rest on them.
 *
 * @type {Quirks}
 */
export const ORIGINAL = Object.freeze({
  densityDivisorIncludesFirms: true,
  commuteIsRoundTrip: false,
  noiseIsFullAmplitude: false,
  substitutesUseLimitFormula: false,
});

/** Implements what the thesis text describes, where the two differ. @type {Quirks} */
export const AS_THESIS_STATES = Object.freeze({
  densityDivisorIncludesFirms: false,
  commuteIsRoundTrip: true,
  noiseIsFullAmplitude: true,
  substitutesUseLimitFormula: true,
});

/** Human-readable summary of any departures from ORIGINAL. @param {Quirks} q */
export function describeQuirks(q) {
  const notes = [];
  if (!q.densityDivisorIncludesFirms) notes.push('density divisor = People only');
  if (q.commuteIsRoundTrip) notes.push('commute doubled');
  if (q.noiseIsFullAmplitude) notes.push('noise at full amplitude');
  if (q.substitutesUseLimitFormula) notes.push('rho>=1 uses budget/price');
  return notes.length === 0 ? 'original 2013 behaviour' : `modified: ${notes.join('; ')}`;
}

// --- Parameter -----------------------------------------------------------------------

/**
 * A named, live-tunable model value.
 *
 * Replaces the 29 `*MouseChanger` classes of the original, which differed only in which
 * field of `gl` they poked. A scenario declares the parameters it exposes; the view layer
 * binds them to drags and sliders, and the headless runner sweeps them. Neither needs to
 * know what the parameter actually is.
 *
 * `kind` is carried from the start so the view layer can render numbers, toggles and
 * choices through one seam — the Java's Parameter was double-only, which left view toggles
 * and quirk flags with nowhere to live.
 *
 * @typedef {object} Parameter
 * @property {string} name
 * @property {string} label
 * @property {'number'|'boolean'|'choice'} kind
 * @property {number} min
 * @property {number} max
 * @property {string[]} [choices]
 * @property {() => number} get
 * @property {(v: number) => void} set
 * @property {(delta: number) => void} nudge
 * @property {(t: number) => void} setNormalised
 * @property {() => number} getNormalised
 */

/**
 * @param {object} spec
 * @param {string} spec.name
 * @param {() => number} spec.get
 * @param {(v: number) => void} spec.set
 * @param {string} [spec.label]
 * @param {'number'|'boolean'|'choice'} [spec.kind]
 * @param {number} [spec.min]
 * @param {number} [spec.max]
 * @param {string[]} [spec.choices]
 * @returns {Parameter}
 */
export function parameter({ name, get, set, label, kind = 'number', min = 0, max = 1, choices }) {
  const p = {
    name,
    label: label ?? name,
    kind,
    min,
    max,
    choices,
    get,
    /** Set, clamped to [min, max]. @param {number} v */
    set(v) {
      set(Math.min(max, Math.max(min, v)));
    },
    /** Nudge by a delta, clamped. Used by drag controls. @param {number} delta */
    nudge(delta) {
      p.set(p.get() + delta);
    },
    /** Set from a normalised 0–1 position. Used by sweeps and sliders. @param {number} t */
    setNormalised(t) {
      p.set(min + t * (max - min));
    },
    /** Current value as a normalised 0–1 position. */
    getNormalised() {
      return max > min ? (p.get() - min) / (max - min) : 0;
    },
  };
  return p;
}

// --- Model config --------------------------------------------------------------------

/**
 * @typedef {object} ModelConfig
 * @property {import('./space.js').Space} space
 * @property {number} peopleCount
 * @property {number} firmCount
 * @property {import('./space.js').Placement} placePeople
 * @property {import('./space.js').Placement} placeFirms
 * @property {import('./econ.js').Utility} utility
 * @property {import('./econ.js').Production} production
 * @property {any} personBehaviour
 * @property {any} firmBehaviour
 * @property {number} seed
 * @property {Quirks} quirks
 * @property {number} deliveryCost
 * @property {number} commuteCost
 * @property {number} baseGoodCost
 * @property {number} wage
 * @property {number} densityCost
 * @property {number} densityRadius
 * @property {number} demandNoise
 * @property {boolean} peopleMobile
 * @property {boolean} firmsMobile
 * @property {boolean} rangeDelivery
 * @property {boolean} rangeGoodCost
 * @property {boolean} rangeWage
 * @property {boolean} rangeCommute
 * @property {[number, number] | null} initialPriceRange
 * @property {() => number} totalActors
 * @property {() => number} densityDivisor
 * @property {() => string} summary
 */

/**
 * Fill a partial model spec with defaults.
 *
 * The Java needed a 60-line builder for this; an object literal plus defaults does the same
 * job. Structural fields are fixed once a run starts. Costs stay mutable because scenarios
 * expose them as live Parameters — but **never set them directly on the config**, because a
 * Firm carries its own copy of every cost. Go through `world.setDeliveryCost` and friends,
 * which re-apply ranging. CODE_ANATOMY §7.11: getting this wrong silently disabled every
 * distance-cost control in the Java for two build stages.
 *
 * @param {Partial<ModelConfig> & {utility: import('./econ.js').Utility,
 *                                 production: import('./econ.js').Production,
 *                                 spaceKind?: string, width?: number}} spec
 * @returns {ModelConfig}
 */
export function defineModel(spec) {
  if (!spec.utility) throw new Error('scenario must set a utility function');
  if (!spec.production) throw new Error('scenario must set a production function');

  const kind = spec.spaceKind ?? PLANE;
  const width = spec.width ?? 1;

  const config = /** @type {ModelConfig} */ ({
    space: { kind, width },
    peopleCount: 1,
    firmCount: 1,
    placePeople: randomly(),
    placeFirms: atCentre(),
    personBehaviour: null,
    firmBehaviour: null,
    seed: 12, // the original's default
    quirks: ORIGINAL,
    deliveryCost: 0,
    commuteCost: 0,
    baseGoodCost: 1,
    wage: 1,
    densityCost: 0,
    densityRadius: 1 / 32,
    demandNoise: 0,
    peopleMobile: false,
    firmsMobile: false,
    rangeDelivery: false,
    rangeGoodCost: false,
    rangeWage: false,
    rangeCommute: false,

    /**
     * Range for randomising each Firm's starting price, or null for `baseGoodCost`.
     *
     * Not cosmetic. The transmission belt is sensitive to where prices start: from a
     * uniform 3.0 with distance costs, several Firms overshoot into the runaway of §6.3.2
     * and never recover, while the thesis' randomised 0.1–1.5 settles cleanly. Starting
     * near equilibrium keeps the first few price corrections small. CODE_ANATOMY §7.9.
     */
    initialPriceRange: null,

    ...spec,

    /** Total actors. The divisor for density cost under ORIGINAL quirks. */
    totalActors() {
      return this.peopleCount + this.firmCount;
    },

    /** Divisor for the density cost sum — the choice Quirks exists to make explicit. */
    densityDivisor() {
      return this.quirks.densityDivisorIncludesFirms ? this.totalActors() : this.peopleCount;
    },

    summary() {
      return [
        `space=${this.space.kind}(width=${this.space.width})  people=${this.peopleCount}  firms=${this.firmCount}`,
        `utility=${this.utility.describe()}  production=${this.production.describe()}`,
        `person=${this.personBehaviour?.describe?.() ?? 'none'}  firm=${this.firmBehaviour?.describe?.() ?? 'none'}`,
        `delivery=${n(this.deliveryCost)}  commute=${n(this.commuteCost)}  goodCost=${n(this.baseGoodCost)}  wage=${n(this.wage)}`,
        `density=${n(this.densityCost)} (radius ${this.densityRadius.toFixed(5)}, divisor ${this.densityDivisor()})  noise=${n(this.demandNoise)}`,
        `mobile: people=${this.peopleMobile} firms=${this.firmsMobile}   seed=${this.seed}`,
        `quirks: ${describeQuirks(this.quirks)}`,
      ].join('\n');
    },
  });

  // `spec` may pass spaceKind/width instead of a space object; normalise either way.
  if (!spec.space) config.space = { kind, width };
  return config;
}

/** @param {number} v */
function n(v) {
  return v.toFixed(4);
}

export { LINE, PLANE, POINT, TWO_REGION };
