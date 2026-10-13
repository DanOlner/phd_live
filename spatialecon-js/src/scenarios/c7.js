// @ts-check

/**
 * **C7 — love of variety shapes settlement.** Thesis §5.4.6.
 *
 * Two Firms a distance of 1 apart. They differ in one respect only: reaching the first costs
 * 2 per unit of distance, reaching the second costs 4. Five hundred People, free to move, no
 * commuting. ρ is swept from 0.99 down towards 0 and back up again.
 *
 * **Changing tastes alone redraws the map.** No cost changes anywhere in the run. What moves
 * is only how much People value having some of both goods, and it is enough to relocate the
 * entire population from one Firm to the other.
 *
 * The direction is the part worth sitting with, because it is backwards from the obvious
 * guess. With *strong* love of variety — ρ near 0 — a Person must have some of both goods
 * whatever they cost, so the sensible thing is to stand next to the good that is *expensive
 * to ship* and import the cheap-to-ship one from a distance. So People cluster around the
 * dearer Firm. With *weak* love of variety — ρ near 1 — the goods are near-substitutes,
 * nobody needs both, and everyone crowds the Firm whose good is cheapest delivered.
 *
 * Two distinct cores appear and swap dominance, and the sweep runs back up so any hysteresis
 * shows as the two passes failing to lie on top of each other. The thesis expects some:
 * once a population has committed to one core, the density it creates there is itself a
 * reason to stay.
 *
 * **What E8 removes.** This model gets its two-core outcome by *being handed* two different
 * distance costs — the modeller decided one Firm would be dearer to reach. E8 produces the
 * same shift with two Firms identical in every respect, where which one wins is settled by
 * the model from an arbitrarily small difference in starting price. That is the difference
 * between an assumption and a mechanism, and it is why E8 closes the thesis and this does not.
 *
 * **Reconstructed.** C7's config is the second of the two that are lost (CODE_ANATOMY §4).
 * The Firms, their separation, the two distance costs and the ρ sweep are all stated in the
 * thesis text; the density cost and radius are C1's, and `rangeDelivery` turns a single base
 * of 2 into the required 2 and 4 — which is what that flag exists for.
 */

import { personBundle } from '../behaviour.js';
import { PLANE, defineModel } from '../config.js';
import { ces, smoothEoS } from '../econ.js';
import { MEAN, MEAN_SD } from '../record.js';
import { randomly, twoPointsApart } from '../space.js';

const RHO_HIGH = 0.99;
const RHO_LOW = 0.05;
const DAYS = 6000;

const u = ces(RHO_HIGH);

/** @type {import('../scenario.js').Scenario} */
export const c7 = {
  id: 'c7',
  title: 'Love of variety shapes settlement',
  thesisRef: '§5.4.6',
  defaultDays: DAYS,

  notes: `Two Firms 1 apart, 500 People, no commuting. Firm 0 costs 2 per unit distance to
reach, Firm 1 costs 4 -- the ONLY difference between them. rho sweeps ${RHO_HIGH} down to
${RHO_LOW} and back.

No cost changes at any point in this run. The only thing that moves is taste.

Watch distToCheap and distToDear. At high rho the goods are near-substitutes, nobody needs
both, and People crowd Firm 0 -- the one that is cheap to reach. As rho falls, love of
variety strengthens: now a Person must have some of BOTH goods whatever they cost, and the
sensible place to stand is next to the good that is EXPENSIVE to ship, importing the cheap
one from a distance. The population moves to Firm 1.

That is backwards from the obvious guess, and it is the finding: changing tastes alone, with
no cost change at all, redraws the map. Two distinct cores appear and swap dominance.

The sweep runs back up again, so hysteresis shows as the two passes failing to lie on top of
each other. Expect some -- once a population has committed to a core, the density it has
created there is itself a reason to stay.

Compare with e8, which produces the same two-core shift from two IDENTICAL Firms, deciding
which wins from an arbitrarily small difference in starting price. Here the asymmetry was
handed to the model by the modeller. There it is a mechanism. That is why e8 closes the
thesis and this does not.

C7's config is LOST (CODE_ANATOMY section 4). The Firms, their separation, the two distance
costs and the rho sweep are from the thesis text; the density cost and radius are C1's.`,

  config: () =>
    defineModel({
      spaceKind: PLANE, width: 2.0,
      peopleCount: 500, firmCount: 2,
      placePeople: randomly(),
      placeFirms: twoPointsApart(1.0),
      utility: u,
      production: smoothEoS(),
      baseGoodCost: 1.0,
      wage: 1.0,
      commuteCost: 0,
      // Base 2, ranged: Firm 0 gets 2, Firm 1 gets 4. Exactly what this flag is for —
      // see the note on `_applyCosts` in world.js.
      deliveryCost: 2.0,
      rangeDelivery: true,
      densityCost: 10,
      densityRadius: 1 / 16,
      personBehaviour: personBundle(),
      peopleMobile: true,
    }),

  /** Down for the first half, back up for the second. */
  beforeDay(world, day) {
    const half = DAYS / 2;
    const t = day < half ? day / half : Math.max(0, (DAYS - day) / half);
    u.rho = RHO_HIGH + t * (RHO_LOW - RHO_HIGH);
  },

  record(r, w) {
    const cheap = w.firm(0);   // distance cost 2
    const dear = w.firm(1);    // distance cost 4
    r.global('rho', () => u.rho);
    r.global('falling', () => (w.day < DAYS / 2 ? 1 : 0));
    r.aggregate('distToCheap', w.people, (p) => w.distance(p, cheap), MEAN);
    r.aggregate('distToDear', w.people, (p) => w.distance(p, dear), MEAN);
    // Positive means the population sits nearer the DEAR Firm, which is the strong-variety
    // regime. The sign change is the map being redrawn.
    r.global('leaning', () =>
      (w.people.reduce((a, p) => a + w.distance(p, cheap) - w.distance(p, dear), 0)
        / w.people.length));
    r.aggregate('utility', w.people, (p) => p.utility, MEAN_SD);
    r.aggregate('density', w.people, (p) => p.densityCost, MEAN);
    r.perActor('personX', w.people, (p) => p.position.x);
    r.perActor('personY', w.people, (p) => p.position.y);
  },

  controls: (w) => [u.rhoParam(), w.densityCostParam()],
};
