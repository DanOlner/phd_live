// @ts-check

/**
 * Collects named series over a run and writes them out.
 *
 * Replaces `io/` and `variables/` — 63 classes, 6,329 lines, differing only in how they
 * reduced across actors and which field they read. The original pulled fields by reflection
 * on a name string, so a typo gave a silent empty column; here the accessor is a function.
 *
 *     r.perActor('utility', w.people, p => p.utility);
 *     r.aggregate('meanUtility', w.people, p => p.utility, MEAN_SD);
 *     r.global('rho', () => ces.rho);
 */

/**
 * @typedef {object} Aggregation
 * @property {string} description
 * @property {(values: number[]) => number[]} reduce
 * @property {(name: string, actorCount: number) => string[]} columns
 */

/** Keep every actor's value. One column per actor. Correlation plots need this. @type {Aggregation} */
export const PER_ACTOR = {
  description: '',
  reduce: (values) => values.slice(),
  columns: (name, actorCount) =>
    Array.from({ length: actorCount }, (_, i) => `${name}_${i}`),
};

/** Mean only. @type {Aggregation} */
export const MEAN = {
  description: 'mean',
  reduce: (values) => [mean(values)],
  columns: (name) => [`${name}_mean`],
};

/** Mean and population standard deviation. @type {Aggregation} */
export const MEAN_SD = {
  description: 'mean+sd',
  reduce: (values) => {
    const m = mean(values);
    let sumSq = 0;
    for (const v of values) sumSq += (v - m) * (v - m);
    const sd = values.length === 0 ? 0 : Math.sqrt(sumSq / values.length);
    return [m, sd];
  },
  columns: (name) => [`${name}_mean`, `${name}_sd`],
};

/** Smallest and largest. @type {Aggregation} */
export const MIN_MAX = {
  description: 'min+max',
  reduce: (values) => {
    if (values.length === 0) return [0, 0];
    let lo = Infinity, hi = -Infinity;
    for (const v of values) { lo = Math.min(lo, v); hi = Math.max(hi, v); }
    return [lo, hi];
  },
  columns: (name) => [`${name}_min`, `${name}_max`],
};

/**
 * Ratio of the two values, larger over smaller so the result is always ≥ 1.
 * The "Firm efficiency ratio" of §6.4.2. Only meaningful for exactly two actors.
 * @type {Aggregation}
 */
export const RATIO_GTE_ONE = {
  description: 'ratio',
  reduce: (values) => {
    if (values.length !== 2) {
      throw new Error(`RATIO_GTE_ONE needs exactly 2 actors, got ${values.length}`);
    }
    const [a, b] = values;
    const lo = Math.min(a, b), hi = Math.max(a, b);
    return [lo === 0 ? NaN : hi / lo];
  },
  columns: (name) => [`${name}_ratio`],
};

/** @param {number[]} values */
function mean(values) {
  if (values.length === 0) return 0;
  let s = 0;
  for (const v of values) s += v;
  return s / values.length;
}

/** One named quantity, sampled from a group of actors over time. */
export class Series {
  /**
   * @param {string} name @param {any[]} actors @param {(a: any) => number} accessor
   * @param {Aggregation} aggregation
   * @param {number} [window] rolling window; -1 keeps everything (the original's `followAverage`)
   */
  constructor(name, actors, accessor, aggregation, window = -1) {
    this.name = name;
    this.actors = actors;
    this.accessor = accessor;
    this.aggregation = aggregation;
    this.window = window;
    /** @type {number[][]} */
    this.rows = [];
    /** @type {number[]} */
    this.days = [];
  }

  /** Sample now and append a row. @param {number} day */
  sample(day) {
    const values = this.actors.map(this.accessor);
    this.rows.push(this.aggregation.reduce(values));
    this.days.push(day);
    if (this.window > 0 && this.rows.length > this.window) {
      this.rows.shift();
      this.days.shift();
    }
  }

  get size() {
    return this.rows.length;
  }

  columns() {
    return this.aggregation.columns(this.name, this.actors.length);
  }

  /** Most recent row, or null if nothing has been sampled. */
  latest() {
    return this.rows.length === 0 ? null : this.rows[this.rows.length - 1];
  }

  /** Most recent value of the first column — the common case for scalar series. */
  latestValue() {
    const r = this.latest();
    return r === null || r.length === 0 ? NaN : r[0];
  }
}

export class Recorder {
  constructor() {
    /** @type {Map<string, Series>} */
    this.series = new Map();
    this._sampleEvery = 1;
  }

  /** Sample every nth day. Default 1. @param {number} days */
  sampleEvery(days) {
    this._sampleEvery = Math.max(1, days);
    return this;
  }

  /** One column per actor. What correlation plots need.
   * @param {string} name @param {any[]} actors @param {(a: any) => number} accessor */
  perActor(name, actors, accessor) {
    return this._add(new Series(name, actors, accessor, PER_ACTOR));
  }

  /** Reduced across actors by the given aggregation.
   * @param {string} name @param {any[]} actors @param {(a: any) => number} accessor
   * @param {Aggregation} aggregation */
  aggregate(name, actors, accessor, aggregation) {
    return this._add(new Series(name, actors, accessor, aggregation));
  }

  /** As `aggregate`, but keeping only the last `window` samples.
   * @param {string} name @param {any[]} actors @param {(a: any) => number} accessor
   * @param {Aggregation} aggregation @param {number} window */
  rolling(name, actors, accessor, aggregation, window) {
    return this._add(new Series(name, actors, accessor, aggregation, window));
  }

  /** A single global value, not tied to any actor — a parameter being swept, say.
   * @param {string} name @param {() => number} value */
  global(name, value) {
    return this._add(new Series(name, [null], () => value(), MEAN));
  }

  /** @param {Series} s */
  _add(s) {
    if (this.series.has(s.name)) throw new Error(`duplicate series name: ${s.name}`);
    this.series.set(s.name, s);
    return s;
  }

  /** Sample every series, if this day is a sampling day. @param {number} day */
  sample(day) {
    if (day % this._sampleEvery !== 0) return;
    for (const s of this.series.values()) s.sample(day);
  }

  /** @param {string} name */
  get(name) {
    const s = this.series.get(name);
    if (!s) throw new Error(`no series named ${name} (have: ${this.names().join(', ')})`);
    return s;
  }

  names() {
    return [...this.series.keys()];
  }

  /**
   * The current value of a named series, computed now rather than read back from the stored
   * rows. This is the seam the charts use: a scenario already declares every quantity worth
   * plotting, by name, in `record()`, so a view can ask for one of those names instead of
   * reimplementing the calculation and drifting away from it.
   * @param {string} name @param {number} [column] which of the aggregation's columns
   */
  value(name, column = 0) {
    const s = this.get(name);
    const reduced = s.aggregation.reduce(s.actors.map(s.accessor));
    return column < reduced.length ? reduced[column] : NaN;
  }

  get isEmpty() {
    return this.series.size === 0;
  }

  /**
   * Every series as one CSV, wide format: a `day` column, then each series' columns side by
   * side. Series sampled the same number of times line up by row.
   */
  toCsv() {
    const all = [...this.series.values()];
    if (all.length === 0) return '';

    const header = ['day', ...all.flatMap((s) => s.columns())];
    const lines = [header.join(',')];

    const rowCount = Math.max(...all.map((s) => s.size));
    const first = all[0];
    for (let row = 0; row < rowCount; row++) {
      const cells = [row < first.days.length ? String(first.days[row]) : ''];
      for (const s of all) {
        const vals = row < s.size ? s.rows[row] : new Array(s.columns().length).fill(NaN);
        for (const v of vals) cells.push(format(v));
      }
      lines.push(cells.join(','));
    }
    return lines.join('\n') + '\n';
  }
}

/** Matches the Java's formatting so the two CSVs diff cleanly. @param {number} v */
function format(v) {
  if (Number.isNaN(v)) return '';
  if (v === Math.round(v) && Math.abs(v) < 1e15) return String(v);
  return sixSigFigs(v);
}

/** @param {number} v */
function sixSigFigs(v) {
  const s = v.toPrecision(6);
  // Java's %g uses e+NN; JS uses e+N. Normalise the exponent to two digits.
  return s.replace(/e([+-])(\d)$/, 'e$10$2');
}
