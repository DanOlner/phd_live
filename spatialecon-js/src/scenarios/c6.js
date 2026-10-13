// @ts-check

/**
 * **C6 — von Thünen without landlords.** Thesis §5.4.5.
 *
 * Four Firms at the centre, each on a different cost level. Every Person is permanently
 * assigned to one of them, a quarter each, and may neither work nor buy elsewhere. They sort
 * themselves into rings.
 *
 * Rings like von Thünen's, from a density cost and almost nothing else — no landlords, no
 * bid-rent curves, no land market, no market of any kind. The thesis' point is parsimony: von
 * Thünen needed a web of assumptions about competitive bidding to get here, and this needs a
 * proximity cost and agents who can tell better from worse.
 *
 * The sting is which way the sorting runs, and that it **depends on whether the cost is
 * spatial**:
 *
 * - Vary the **wage** (non-spatial): the better-paid buy **proximity**. A higher wage makes a
 *   given density cost proportionally smaller, so they can afford the centre.
 * - Vary the **commute cost** (spatial): the effectively better-off buy **distance**. Cheap
 *   travel makes the outskirts affordable, and the outskirts are where space is.
 *
 * Same agents, same mechanism, opposite sorting — the §5.4.3 result made visible. `c6` is the
 * wage version, `c6c` the commuting one.
 */

import { personBundle } from '../behaviour.js';
import { PLANE, defineModel } from '../config.js';
import { ces, smoothEoS } from '../econ.js';
import { MEAN_SD } from '../record.js';
import { atCentre, randomly } from '../space.js';

/** @param {boolean} varyCommute */
function make(varyCommute) {
  /** @type {import('../scenario.js').Scenario} */
  return {
    id: varyCommute ? 'c6c' : 'c6',
    title: varyCommute
      ? 'Von Thunen without landlords: varying commute cost'
      : 'Von Thunen without landlords: varying wage',
    thesisRef: '§5.4.5',
    defaultDays: 1500,

    notes: varyCommute
      ? `200 People, 4 central Firms. Each Person is tied to one Firm and may not trade
elsewhere. Commute cost ranges 0.25, 0.5, 0.75, 1.0 across the Firms (base 0.25, multiplied
by Firm index).

Watch distance_groupN_mean: the group with the CHEAPEST commute settles FURTHEST out. Cheap
travel buys distance, and distance is where the free space is.

Compare with c6, where a higher wage buys proximity instead. Same mechanism, opposite
sorting, because one cost is spatial and the other is not.`
      : `500 People, 4 central Firms. Each Person is tied to one Firm and may not trade
elsewhere. Wages range 0.5, 1.0, 1.5, 2.0 across the Firms (base 0.5, multiplied by Firm
index).

Watch distance_groupN_mean: the best-paid settle CLOSEST in. The density cost is subtracted
from the wage, so a bigger wage shrinks it in proportion and makes the centre affordable.

Compare with c6c, where cheap commuting buys distance instead.

Note: the shipped vonThunen1.properties has base wage 1, which would give 1-4 rather than
the 0.5-2 the thesis describes. The thesis figure's range is used here.`,

    config: () =>
      defineModel({
        spaceKind: PLANE, width: 2.0,
        peopleCount: varyCommute ? 200 : 500,
        firmCount: 4,
        placePeople: randomly(),
        placeFirms: atCentre(),
        utility: ces(varyCommute ? 0.65 : 0.4),
        production: smoothEoS(),
        baseGoodCost: 1.0,
        deliveryCost: varyCommute ? 0.8 : 0.25,
        densityCost: 10,
        densityRadius: 1 / 16,
        personBehaviour: personBundle(),
        peopleMobile: true,
        ...(varyCommute
          ? { wage: 1.0, commuteCost: 0.25, rangeCommute: true }
          : { wage: 0.5, commuteCost: 0, rangeWage: true }),
      }),

    /** Tie each Person to one Firm, round-robin, as `ActorMaker` did. */
    setUp(w) {
      const n = w.firms.length;
      w.people.forEach((p, i) => { p.assignedFirm = w.firm(i % n); });
    },

    record(r, w) {
      r.perActor('distanceToCentre', w.people, (p) => w.distance(p, w.firm(0)));
      r.perActor('utility', w.people, (p) => p.utility);
      r.perActor('group', w.people, (p) => (p.assignedFirm ? p.assignedFirm.id : -1));

      // One distance and utility series per cost group: the rings, as numbers.
      for (let g = 0; g < w.firms.length; g++) {
        const members = w.people.filter((p) => p.assignedFirm === w.firm(g));
        if (members.length === 0) continue;
        r.aggregate(`distance_group${g}`, members, (p) => w.distance(p, w.firm(0)), MEAN_SD);
        r.aggregate(`utility_group${g}`, members, (p) => p.utility, MEAN_SD);
      }
      r.global('movementMean', () => w.meanPersonMovement());
    },

    controls: (w) => [w.densityCostParam(), w.deliveryCostParam()],
  };
}

export const c6 = make(false);
/** The commute-cost variant, where sorting runs the other way. */
export const c6c = make(true);
