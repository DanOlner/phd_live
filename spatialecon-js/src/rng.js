// @ts-check

/**
 * The model's single source of randomness.
 *
 * One seeded generator per run, so a run is exactly repeatable and a reroll is the same
 * run with a different seed. That pairing is the point: E5 and E8 turn on which Firm
 * happens to win, so a site that can show "this run" and "another run" is teaching
 * something a static figure cannot.
 *
 * This does NOT reproduce the Java's stream, deliberately (BUILD_PLANNER §9.1). The two
 * implementations are compared by claim, not by trajectory. mulberry32 is chosen for being
 * short, fast, and reproducible within JavaScript, which is all that is needed.
 */
export class Rng {

  /** @param {number} seed */
  constructor(seed) {
    /** @type {number} */
    this.seed = seed;
    /** @type {number} */
    this._state = seed | 0;
  }

  /** Uniform in [0, 1). */
  nextDouble() {
    this._state = (this._state + 0x6d2b79f5) | 0;
    let t = Math.imul(this._state ^ (this._state >>> 15), 1 | this._state);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  }

  /** Uniform in [0, max). @param {number} max */
  upTo(max) {
    return this.nextDouble() * max;
  }

  /** Uniform in [lo, hi). @param {number} lo @param {number} hi */
  between(lo, hi) {
    return lo + this.nextDouble() * (hi - lo);
  }

  /**
   * Uniform in [-magnitude/2, +magnitude/2) — the "jiggle" pattern used throughout the
   * original. Note it is a *signed magnitude*, not a step of fixed size in a random
   * direction; getting that wrong makes the Firm location crawl far too effective
   * (CODE_ANATOMY §5.6).
   * @param {number} magnitude
   */
  jiggle(magnitude) {
    return (this.nextDouble() - 0.5) * magnitude;
  }

  /** Uniform integer in [0, bound). @param {number} bound */
  nextInt(bound) {
    return Math.floor(this.nextDouble() * bound);
  }

  nextBoolean() {
    return this.nextDouble() < 0.5;
  }

  /**
   * A uniformly distributed point inside a circle of this radius, centred on the origin.
   * @param {number} radius
   * @returns {{x: number, y: number}}
   */
  pointInCircle(radius) {
    // sqrt of the uniform draw, else points bunch towards the centre
    const r = radius * Math.sqrt(this.nextDouble());
    const theta = this.nextDouble() * 2 * Math.PI;
    return { x: r * Math.cos(theta), y: r * Math.sin(theta) };
  }

  /**
   * A uniformly random direction, as a vector of the given length.
   * @param {number} length
   * @returns {{x: number, y: number}}
   */
  vectorOfLength(length) {
    const theta = this.nextDouble() * 2 * Math.PI;
    return { x: length * Math.cos(theta), y: length * Math.sin(theta) };
  }

  /**
   * Shuffle in place, Fisher–Yates.
   *
   * Used once per phase per day to permute turn order. Note the consequence the thesis
   * draws out (§4.7.4): permuting each day means the gap between one agent's consecutive
   * turns is pyramidally distributed around one day, not fixed at one day — so any
   * behaviour comparing t to t−1 is working with a variable interval.
   *
   * @template T
   * @param {T[]} list
   */
  shuffle(list) {
    for (let i = list.length - 1; i > 0; i--) {
      const j = this.nextInt(i + 1);
      const tmp = list[i];
      list[i] = list[j];
      list[j] = tmp;
    }
  }
}
