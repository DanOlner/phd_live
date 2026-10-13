// @ts-check

/**
 * What the line view draws above and below its rule, per model family.
 *
 * Kept out of `line.js` so the view stays a renderer and these stay statements about the
 * economics — and out of the scenarios so a scenario stays runnable headless with no view
 * code loaded.
 */

/**
 * Demand for one Firm, at each Person's position along the line.
 *
 * This is D1's published figure: Firm 0's demand profile above the rule and Firm 1's
 * mirrored below. Read from what People actually bought, so under demand noise it shows the
 * noisy quantities — which is right, since that is what the Firms are trying to read.
 *
 * @param {number} firmIndex
 * @returns {import('./line.js').ProfileFn}
 */
export function demandForFirm(firmIndex) {
  return (world) => {
    const firm = world.firms[firmIndex];
    /** @type {number[]} */
    const positions = [];
    /** @type {number[]} */
    const values = [];
    for (const p of world.people) {
      const i = p.lastSellers.indexOf(firm);
      positions.push(p.position.x);
      values.push(i >= 0 ? (p.lastQuantities[i] ?? 0) : 0);
    }
    return { positions, values, label: `firm ${firmIndex} demand` };
  };
}

/**
 * Output per unit of input, at each Firm's position — the "efficiency" plotted above the
 * line in §6.4.1. Where the centre takes off, this is where you see it.
 * @type {import('./line.js').ProfileFn}
 */
export const firmEfficiency = (world) => ({
  positions: world.firms.map((f) => f.position.x),
  values: world.firms.map((f) => f.outputPerUnitTime),
  label: 'firm efficiency',
});

/**
 * Utility at each Person's position. Below the rule in E7, against efficiency above: the
 * pairing that shows the centre is better off before any transition happens, from geography
 * alone.
 * @type {import('./line.js').ProfileFn}
 */
export const personUtility = (world) => ({
  positions: world.people.map((p) => p.position.x),
  values: world.people.map((p) => p.utility),
  label: 'person utility',
});

/** Price at each Firm's position. @type {import('./line.js').ProfileFn} */
export const firmPrice = (world) => ({
  positions: world.firms.map((f) => f.position.x),
  values: world.firms.map((f) => f.goodCost),
  label: 'firm price',
});

/**
 * Which pair of profiles a model wants. Two-Firm line models get the mirrored demand
 * figure; many-agent ones get efficiency against utility.
 * @param {import('../world.js').World} world
 */
export function profilesFor(world) {
  if (world.firms.length === 2) {
    return { above: demandForFirm(0), below: demandForFirm(1) };
  }
  return { above: firmEfficiency, below: personUtility };
}
