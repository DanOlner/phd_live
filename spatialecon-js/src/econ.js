// @ts-check

/**
 * The economics: two utility functions, two production functions, two optimisers.
 *
 * These are the load-bearing pieces. Everything later — density costs, stock targeting,
 * regime transitions — is built on them, so an error here surfaces much later disguised as
 * an interesting result. They are also the only part of the model with no randomness in
 * it, which is why they are checked numerically rather than by claim (BUILD_PLANNER §6).
 */

import { ORIGINAL, parameter } from './config.js';

/**
 * @typedef {object} Utility
 * @property {(quantities: number[]) => number} utilityOf
 * @property {(prices: number[], budget: number) => number[]} optimalQuantities
 * @property {() => string} describe
 */

/**
 * @typedef {object} Production
 * @property {(labour: number) => number} outputFor
 * @property {(labour: number) => number} outputPerUnit
 * @property {(labour: number) => number} marginalAt
 * @property {(totalInput: number, myInput: number) => number} granularShare
 * @property {() => string} describe
 */

// --- Utility -------------------------------------------------------------------------

/**
 * Constant elasticity of substitution utility — the thesis workhorse (§2.2.3).
 *
 *     U = ( Σ c_i^ρ )^(1/ρ)
 *
 * ρ carries the whole substitutes-to-variety spectrum. As ρ → 1 goods become perfect
 * substitutes and love of variety vanishes; as ρ → 0 a mix is increasingly preferred. Note
 * the direction: high ρ means *low* love of variety, which is easy to state backwards.
 *
 * The constrained optimum is where the externality of the whole framework lives (§6.3.4).
 * Every good's price appears in the denominator of every other good's demand, so one Firm's
 * price change alters demand for all the others — not through timing or interaction, but
 * through the consumer's own arithmetic.
 *
 * ρ is per-instance rather than shared, so agents *could* differ in tastes. Every published
 * scenario hands them all the same object, preserving the thesis assumption of identical
 * tastes — but that is now a choice a scenario makes, not something the design enforces.
 *
 * @param {number} rho
 * @param {import('./config.js').Quirks} [quirks]
 */
export function ces(rho, quirks = ORIGINAL) {
  return {
    /** Mutable: scenarios sweep this in `beforeDay`. */
    rho,

    /** @param {number[]} quantities */
    utilityOf(quantities) {
      let total = 0;
      for (const q of quantities) {
        if (q > 0) total += Math.pow(q, this.rho);
      }
      return Math.pow(total, 1 / this.rho);
    },

    /** @param {number[]} prices @param {number} budget */
    optimalQuantities(prices, budget) {
      const out = new Array(prices.length).fill(0);
      if (prices.length === 0 || budget <= 0) return out;

      if (this.rho < 1) {
        // g_j = P_j^(1/(rho-1)) * Y  /  sum_i P_i^(rho/(rho-1))
        let denominator = 0;
        for (const p of prices) denominator += Math.pow(p, this.rho / (this.rho - 1));
        if (denominator === 0) return out;
        for (let i = 0; i < prices.length; i++) {
          out[i] = (Math.pow(prices[i], 1 / (this.rho - 1)) * budget) / denominator;
        }
        return out;
      }

      // Perfect substitutes: the cheapest takes everything.
      let cheapest = 0;
      for (let i = 1; i < prices.length; i++) {
        if (prices[i] < prices[cheapest]) cheapest = i;
      }
      // ORIGINAL multiplied price by budget here, which disagrees with the rho->1 limit of
      // the branch above (that limit is budget/price). The original is wrong — but it never
      // ran, because rho was clamped to 0.99 everywhere it could be set, so every figure
      // captioned "rho = 1" is really 0.99. CODE_ANATOMY §7.1. Use 0.99, not 1.
      out[cheapest] = quirks.substitutesUseLimitFormula
        ? budget / prices[cheapest]
        : prices[cheapest] * budget;
      return out;
    },

    /** Bindable ρ, for scenarios that expose it as a live control. */
    rhoParam() {
      return parameter({
        name: 'rho', label: 'rho (1 = substitutes)', min: 0.01, max: 1.0,
        get: () => this.rho, set: (v) => { this.rho = v; },
      });
    },

    describe() {
      return `CES (rho=${fmt(this.rho)})`;
    },
  };
}

/**
 * Two-good utility with an optional subsistence term (§2.2.2):
 *
 *     U = F^δ · (M + γ)^(1−δ)
 *
 * At γ = 0 this is plain Cobb–Douglas, which is how the thesis uses it. The γ term is the
 * "Engel function" variant whose constrained optima the thesis derives and then cuts; it
 * makes preferences non-homothetic, so the mix shifts with income rather than staying in
 * fixed proportions.
 *
 * The property the thesis actually exploits is what Cobb–Douglas *disables*. The price of
 * one good does not appear in demand for the other, so each sector is independently unit
 * elastic: cut a price, demand rises exactly enough to leave revenue unchanged. A Firm
 * facing Cobb–Douglas demand therefore has no lever — and, spatially, no reason to move
 * (§5.2.2). A utility function can silently switch off a whole class of agent behaviour.
 *
 * @param {number} delta
 * @param {number} [gamma]
 */
export function stoneGeary(delta, gamma = 0) {
  if (delta <= 0 || delta >= 1) {
    throw new Error(`delta must be strictly between 0 and 1, got ${delta}`);
  }
  return {
    delta,
    gamma,

    /** @param {number[]} quantities */
    utilityOf(quantities) {
      require2(quantities.length);
      const f = Math.max(quantities[0], 0);
      const m = Math.max(quantities[1], 0);
      return Math.pow(f, this.delta) * Math.pow(m + this.gamma, 1 - this.delta);
    },

    /**
     * F = δY/p_F, M = (1−δ)Y/p_M — plus the γ correction when non-zero.
     *
     * Note what is absent: p_M does not appear in F, nor p_F in M. That absence is the
     * whole result of §5.2.2, and it is visible here rather than something you have to run
     * a simulation to discover.
     *
     * @param {number[]} prices @param {number} budget
     */
    optimalQuantities(prices, budget) {
      require2(prices.length);
      const pF = prices[0], pM = prices[1];
      if (pF <= 0 || pM <= 0 || budget <= 0) return [0, 0];

      if (this.gamma === 0) {
        return [(this.delta * budget) / pF, ((1 - this.delta) * budget) / pM];
      }
      // With a subsistence term the budget is effectively augmented by gamma*pM, and the
      // subsistence quantity then subtracted back off the second good.
      const effective = budget + this.gamma * pM;
      const f = (this.delta * effective) / pF;
      const m = ((1 - this.delta) * effective) / pM - this.gamma;
      return [Math.max(f, 0), Math.max(m, 0)];
    },

    deltaParam() {
      return parameter({
        name: 'delta', label: 'share of spend on good 1', min: 0.01, max: 0.99,
        get: () => this.delta, set: (v) => { this.delta = v; },
      });
    },

    describe() {
      return this.gamma === 0
        ? `Cobb-Douglas (delta=${fmt(this.delta)})`
        : `Stone-Geary (delta=${fmt(this.delta)}, gamma=${fmt(this.gamma)})`;
    },
  };
}

/** Plain Cobb–Douglas: U = F^δ · M^(1−δ). @param {number} delta */
export function cobbDouglas(delta) {
  return stoneGeary(delta, 0);
}

/** @param {number} n */
function require2(n) {
  if (n !== 2) throw new Error(`StoneGeary is a two-good function, got ${n}`);
}

// --- Production ----------------------------------------------------------------------

/**
 * Shared derived quantities. Spread into each production function so `this.outputFor`
 * resolves to that function's own form.
 */
const productionDefaults = {
  /**
   * Output per unit of input — the "efficiency" plotted above the line in §6.4.1.
   * Zero input gives zero rather than NaN.
   * @param {number} labour
   * @this {{outputFor: (l: number) => number}}
   */
  outputPerUnit(labour) {
    return labour === 0 ? 0 : this.outputFor(labour) / labour;
  },

  /**
   * Marginal output, by central difference. Numeric rather than analytic so it works for
   * any form. The thesis plots total, average and marginal together to show the two
   * functions' different shapes (§2.2.5).
   * @param {number} labour
   * @this {{outputFor: (l: number) => number}}
   */
  marginalAt(labour) {
    const h = 1e-6;
    return (this.outputFor(labour + h) - this.outputFor(labour - h)) / (2 * h);
  },

  /**
   * The share of output attributable to one agent's contribution, given the producer's
   * current total input.
   *
   * This is the mechanism that lets economies of scale work without anyone needing a
   * strategy (§4.7.5): total input goes through the production function, and the buying
   * agent takes their proportional slice of the result. g_a = f(T_p) · (t_a / T_p).
   *
   * @param {number} totalInput @param {number} myInput
   * @this {{outputFor: (l: number) => number}}
   */
  granularShare(totalInput, myInput) {
    return totalInput === 0 ? 0 : this.outputFor(totalInput) * (myInput / totalInput);
  },
};

/**
 * The thesis production function (§2.2.5):
 *
 *     x = v·l² + m·l
 *
 * `curve` scales the increasing-returns term, `magnitude` the linear one. Setting curve = 0
 * gives constant returns, which is how the thesis isolates what economies of scale are
 * actually doing — several results only appear when curve > 0.
 *
 * "Smooth" is the point: any positive input yields positive output, so production can start
 * from nothing. The Core Model's fixed-labour form cannot, and that difference drives the
 * blind-versus-rational argument of §4.7.6.
 *
 * @param {number} [curve] @param {number} [magnitude]
 */
export function smoothEoS(curve = 1, magnitude = 1) {
  return {
    ...productionDefaults,
    curve,
    magnitude,
    /** @param {number} labour */
    outputFor(labour) {
      return this.curve * labour * labour + this.magnitude * labour;
    },
    curveParam() {
      return parameter({
        name: 'EoS_C', label: 'EoS curve', min: 0, max: 1,
        get: () => this.curve, set: (v) => { this.curve = v; },
      });
    },

    magnitudeParam() {
      return parameter({
        name: 'EoS_M', label: 'EoS magnitude', min: 0, max: 5,
        get: () => this.magnitude, set: (v) => { this.magnitude = v; },
      });
    },

    describe() {
      return `smooth EoS (curve=${fmt(this.curve)}, magnitude=${fmt(this.magnitude)})`;
    },
  };
}

/**
 * The Core Model's production function (§2.2.5), rearranged for output:
 *
 *     l = α + βx      →      x = (l − α) / β
 *
 * α is the fixed labour requirement, β the marginal. Perfectly serviceable algebraically,
 * and unusable in an agent model: below α it returns *negative* output. Nobody can start
 * producing, and no single agent supplying one unit of labour can ever get output above
 * zero on their own.
 *
 * That is the whole "blind versus rational choice" argument of §4.7.6. An agent who reasons
 * about it correctly concludes there is no point working, and production never starts. An
 * agent who guesses randomly and repeats whatever seemed to help will eventually stumble
 * into a group large enough to clear α — and does better precisely by reasoning less.
 *
 * Kept so that failure can be demonstrated rather than described.
 *
 * @param {number} [fixedLabour] alpha @param {number} [marginalLabour] beta
 */
export function coreModelEoS(fixedLabour = 3, marginalLabour = 1) {
  if (marginalLabour <= 0) {
    throw new Error(`marginal labour must be positive, got ${marginalLabour}`);
  }
  return {
    ...productionDefaults,
    fixedLabour,
    marginalLabour,

    /**
     * Output for this labour input. **Negative below α** — deliberately not clamped,
     * because the negative region is the thing being demonstrated.
     * @param {number} labour
     */
    outputFor(labour) {
      return (labour - this.fixedLabour) / this.marginalLabour;
    },

    /** Whether any output is possible at this input. False below the fixed requirement. */
    /** @param {number} labour */
    canProduceAt(labour) {
      return labour >= this.fixedLabour;
    },

    describe() {
      return `core model EoS (alpha=${fmt(this.fixedLabour)}, beta=${fmt(this.marginalLabour)})`;
    },
  };
}

// --- Optimisers ----------------------------------------------------------------------

/**
 * @typedef {object} Optimiser
 * @property {(prices: number[], budget: number, utility: Utility, rng: import('./rng.js').Rng) => number[]} optimise
 * @property {() => string} describe
 * @property {number[]} [lastTrace]
 */

/**
 * The analytic constrained optimum. Exact and cheap — but causally backwards: the consumer
 * obtains their optimal quantities and only then feeds them back through the utility
 * function to discover how happy it made them.
 * @returns {Optimiser}
 */
export function constrained() {
  return {
    optimise(prices, budget, utility, _rng) {
      return utility.optimalQuantities(prices, budget);
    },
    describe() {
      return 'constrained (analytic)';
    },
  };
}

/**
 * The "hill climber" of §4.6.1.
 *
 * Slice the budget randomly across goods, jiggle the slices, keep the change if utility
 * improved, otherwise revert. Ratchets towards the optimum. The jiggle size anneals from
 * `startJiggle` to `endJiggle` across the run, so the search coarsens early and refines
 * late.
 *
 * Far more plausible as an account of choosing than the analytic form, noisier, and roughly
 * a thousand times the work. The thesis' verdict is that the analytic method served better
 * despite being the less realistic one — "everything depends on the problem".
 *
 * A quirk worth knowing (§5.2.3): *more* goods converge *faster*, because slicing a budget
 * across many goods gives each slice less room to vary. True early on and reversed once
 * both have converged — see the tests, which pin the scope.
 *
 * No stock handling. The original reallocated spend when a good ran out — the capability
 * the thesis credits it with — which only becomes meaningful once Firms have finite stock.
 *
 * @param {number} [iterations] @param {number} [startJiggle] @param {number} [endJiggle]
 * @returns {Optimiser}
 */
export function hillClimb(iterations = 1000, startJiggle = 0.01, endJiggle = 0.001) {
  return {
    iterations,

    /** Best-so-far utility per iteration, from the most recent `optimise` call. */
    lastTrace: /** @type {number[]} */ ([]),

    /**
     * The quantities each time the climb *improved*, with the iteration it happened on.
     *
     * Only the accepted steps, which is what "the climb" means — the rejected candidates are
     * a cloud around the current point and carry no information about the path. Short: a
     * 1,000-iteration climb usually accepts a few dozen times, and nearly all of those in the
     * first hundred iterations, which is itself the finding (§5.2.3). Read by `view/optimiser.js`.
     * @type {{i: number, q: number[]}[]}
     */
    lastPath: /** @type {{i: number, q: number[]}[]} */ ([]),

    optimise(prices, budget, utility, rng) {
      const n = prices.length;
      const trace = new Array(Math.max(iterations, 0)).fill(0);
      /** @type {{i: number, q: number[]}[]} */
      const path = [];
      this.lastPath = path;
      if (n === 0 || budget <= 0) {
        this.lastTrace = trace;
        return new Array(n).fill(0);
      }

      const slice = new Array(n);
      for (let i = 0; i < n; i++) slice[i] = rng.nextDouble();
      normalise(slice);

      let best = spend(slice, prices, budget);
      let bestUtility = utility.utilityOf(best);
      path.push({ i: 0, q: best.slice() });

      const candidate = new Array(n).fill(0);
      let jiggle = startJiggle;
      const step = iterations > 0 ? (endJiggle - startJiggle) / iterations : 0;

      for (let i = 0; i < iterations; i++) {
        for (let j = 0; j < n; j++) {
          candidate[j] = slice[j] + rng.jiggle(jiggle);
          // bounce negatives back positive, as the original did
          if (candidate[j] < 0) candidate[j] = -candidate[j];
        }
        normalise(candidate);

        const quantities = spend(candidate, prices, budget);
        const u = utility.utilityOf(quantities);

        if (u > bestUtility) {
          bestUtility = u;
          best = quantities;
          for (let k = 0; k < n; k++) slice[k] = candidate[k];
          path.push({ i: i + 1, q: quantities.slice() });
        }
        trace[i] = bestUtility;
        jiggle += step;
      }

      this.lastTrace = trace;
      return best;
    },

    describe() {
      return `hill-climber (${iterations} iterations, jiggle ${fmt(startJiggle)}→${fmt(endJiggle)})`;
    },
  };
}

/**
 * Turn budget shares into quantities at these prices.
 * @param {number[]} shares @param {number[]} prices @param {number} budget
 */
function spend(shares, prices, budget) {
  const q = new Array(shares.length);
  for (let i = 0; i < shares.length; i++) {
    q[i] = prices[i] > 0 ? (shares[i] * budget) / prices[i] : 0;
  }
  return q;
}

/** Scale to sum 1, so the whole budget is always allocated. @param {number[]} shares */
function normalise(shares) {
  let sum = 0;
  for (const s of shares) sum += s;
  if (sum <= 0) {
    shares.fill(1 / shares.length);
    return;
  }
  for (let i = 0; i < shares.length; i++) shares[i] /= sum;
}

/** Four significant figures, matching the Java's %.4g in summaries. @param {number} v */
function fmt(v) {
  if (v === 0) return '0';
  const rounded = Number(v.toPrecision(4));
  return String(rounded);
}
