// @ts-check

/**
 * Krugman's Core Model — the comparator. Thesis §2.5.5.
 *
 * Ported from `Thesis_on_github/thesisModelsDanOlner2013/CoreModel/src/coremodelviz/Main.java`,
 * which is a separate self-contained program rather than part of the agent framework. It is
 * here for one reason: it is the benchmark the whole thesis is measured against, and the
 * thing the transmission belt is offered as an alternative to.
 *
 * **What it is.** Two regions. A share `d` of income goes on manufactures, produced by mobile
 * workers under increasing returns; the rest goes on agriculture, produced by immobile
 * farmers split `φ` between the regions. Shipping manufactures between regions costs `t` in
 * iceberg form — send `t` units to land one. `λ` is the share of manufacturing workers in
 * region 1, and the question is where λ settles.
 *
 * Three equations, solved by iterating to a fixed point at each λ:
 *
 *     Y₀ = d·λ·W₀ + (1−d)·φ                                   income
 *     I₀ = [λ·W₀^(1−e) + (1−λ)·t^(1−e)·W₁^(1−e)]^(1/(1−e))    price index
 *     W₀ = [Y₀·I₀^(e−1) + Y₁·t^(1−e)·I₁^(e−1)]^(1/e)          nominal wage
 *
 * and symmetrically for region 2. The real wage is `W·I^(−d)`, and workers move towards
 * whichever region pays more of it.
 *
 * **Why the thesis measures itself against this.** The Core Model gets agglomeration from
 * increasing returns interacting with transport costs, which is the right answer and a famous
 * one. What it cannot do is separate love of variety from economies of scale — `e` is doing
 * both jobs at once — or let production structure vary *within* a region, because a region is
 * a dimensionless point with no inside. The thesis' models keep those apart and give regions
 * an interior, which is what E5's four-Firm variant and E8 are for.
 *
 * **And note what it assumes that the agent models do not.** Every variety is produced at the
 * same scale by construction; there is no individual Firm anywhere in it; and λ moves because
 * a modeller says workers follow real wages, not because anyone decides anything. E3 produces
 * a catastrophic shift in production structure with none of that.
 */

/** @typedef {{d: number, e: number, t: number, phi: number, tolerance: number, maxIterations: number}} CoreParams */

/** Brakman's defaults, as the original ships them. T = 2.25 gives five equilibria. */
export const DEFAULTS = {
  d: 0.5,          // share of expenditure on manufactures
  e: 5,            // elasticity of substitution, = 1/(1−ρ)
  t: 2.25,         // iceberg transport cost
  phi: 0.5,        // share of farmers in region 1
  tolerance: 0.00005,
  maxIterations: 20000,
};

/**
 * Solve for nominal wages at one value of λ, and return the real wage ratio.
 *
 * The iteration is the original's, including the detail that Y₁ and I₁ are read on the first
 * pass before they have been assigned — Java's arrays start at zero, so the first pass uses
 * zero for both. It converges regardless and the fixed point does not depend on it, but it is
 * reproduced rather than tidied, because "tidied" is how a port stops matching.
 *
 * @param {number} lambda @param {CoreParams} p
 */
export function solveAt(lambda, p) {
  const { d, e, t, phi, tolerance, maxIterations } = p;
  const tPow = Math.pow(t, 1 - e);

  const W = [0.01, 0.01];
  const Y = [0, 0];
  const I = [0, 0];
  let iterations = 0;

  for (;;) {
    Y[0] = d * lambda * W[0] + (1 - d) * phi;
    I[0] = Math.pow(
      lambda * Math.pow(W[0], 1 - e) + (1 - lambda) * tPow * Math.pow(W[1], 1 - e),
      1 / (1 - e),
    );
    const lastW0 = W[0];
    W[0] = Math.pow(
      Y[0] * Math.pow(I[0], e - 1) + Y[1] * tPow * Math.pow(I[1], e - 1),
      1 / e,
    );

    Y[1] = d * (1 - lambda) * W[1] + (1 - d) * (1 - phi);
    I[1] = Math.pow(
      lambda * tPow * Math.pow(W[0], 1 - e) + (1 - lambda) * Math.pow(W[1], 1 - e),
      1 / (1 - e),
    );
    const lastW1 = W[1];
    W[1] = Math.pow(
      Y[0] * tPow * Math.pow(I[0], e - 1) + Y[1] * Math.pow(I[1], e - 1),
      1 / e,
    );

    iterations++;
    const settled = Math.abs(W[0] - lastW0) / lastW0 < tolerance
      && Math.abs(W[1] - lastW1) / lastW1 < tolerance;
    // The original has no cap and relies on convergence. One is added here because a browser
    // tab that stops responding is a worse failure than a slightly wrong point.
    if (settled || iterations >= maxIterations) break;
  }

  const realWage0 = W[0] * Math.pow(I[0], -d);
  const realWage1 = W[1] * Math.pow(I[1], -d);
  return {
    lambda,
    ratio: realWage0 / realWage1,
    nominalRatio: W[0] / W[1],
    iterations,
    converged: iterations < maxIterations,
  };
}

/**
 * The real wage ratio across λ ∈ [0, 1) at one transport cost — the short-run figure.
 * @param {CoreParams} p @param {number} [steps]
 */
export function shortRun(p, steps = 500) {
  const out = new Array(steps);
  for (let i = 0; i < steps; i++) out[i] = solveAt(i / steps, p);
  return out;
}

/**
 * Where the real wage ratio crosses 1, and whether each crossing is stable.
 *
 * Workers move towards the higher real wage, so a crossing is **stable** when the ratio is
 * above 1 just below it — meaning workers to the left are pulled right, back towards it — and
 * unstable when it is below. That test is the original's, and it is the whole of what makes
 * the tomahawk a tomahawk: the same sweep produces both the black stable branches and the
 * grey unstable ones.
 *
 * @param {{lambda: number, ratio: number}[]} curve
 */
export function equilibria(curve) {
  /** @type {{lambda: number, stable: boolean}[]} */
  const found = [];
  for (let i = 1; i < curve.length; i++) {
    const before = curve[i - 1].ratio - 1;
    const here = curve[i].ratio - 1;
    if (before * here < 0) {
      found.push({ lambda: curve[i].lambda, stable: before > 0 });
    }
  }
  return found;
}

/**
 * The corner equilibria, λ = 0 and λ = 1.
 *
 * These are equilibria by construction rather than by crossing: with every manufacturing
 * worker already in one region there is nobody left to move, whatever the wages say. Which
 * means the original's detection — which walks the curve looking for sign changes — cannot
 * see them, and its tomahawk is drawn without the two stable branches that give the figure
 * its name.
 *
 * They are added here, and flagged as a deliberate departure. The test is the same one the
 * crossings use, applied at the ends: λ = 0 is stable when the ratio just above it is below 1
 * (workers are pulled back down towards it), and λ = 1 stable when the ratio just below it is
 * above 1.
 *
 * @param {{lambda: number, ratio: number}[]} curve
 */
export function corners(curve) {
  if (curve.length < 2) return [];
  return [
    { lambda: 0, stable: curve[1].ratio < 1 },
    { lambda: 1, stable: curve[curve.length - 1].ratio > 1 },
  ];
}

/**
 * The long-run figure: every equilibrium across a sweep of transport costs.
 *
 * The canonical result of geographical economics. At high transport costs the only
 * equilibrium is λ = 0.5, the two regions symmetric. Lower it past a break point and the
 * symmetric equilibrium turns unstable while two new stable ones appear at the extremes —
 * everything in one region or the other. The handle and the two blades give it its name.
 *
 * Includes the corners (see `corners`), which the original's crossing-detection could not.
 *
 * @param {CoreParams} p
 * @param {number} [from] @param {number} [to] @param {number} [steps] @param {number} [lambdaSteps]
 */
export function tomahawk(p, from = 1, to = 3.5, steps = 500, lambdaSteps = 500) {
  /** @type {{t: number, lambda: number, stable: boolean}[]} */
  const points = [];
  for (let i = 0; i < steps; i++) {
    let t = from + ((to - from) / steps) * i;
    // The original's guard: T = 1 exactly means costless transport and the price indices
    // collapse, so it steps just off it.
    if (t === 1) t = 1.01;
    const curve = shortRun({ ...p, t }, lambdaSteps);
    for (const e of [...equilibria(curve), ...corners(curve)]) {
      points.push({ t, lambda: e.lambda, stable: e.stable });
    }
  }
  return points;
}
