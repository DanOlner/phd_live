// @ts-check

/**
 * **B3 — the black hole.** Thesis §5.3.2.
 *
 * 20 People and 4 Firms, everyone mobile, random start, **no density cost**. Records mean
 * nearest-neighbour distance for each group over time.
 *
 * Everything collapses onto a point. Distance is a cost like any other, so if nothing opposes
 * it, agents remove it — and in removing it they abolish space itself. This is the result
 * that motivates everything after it: the density cost of group C exists to give distance
 * something to trade against.
 *
 * The second thing to watch is the **asymmetry in how fast they converge**. People collapse
 * quickly: each evaluates candidate locations directly and can see which is better. Firms
 * take far longer, because a Firm has no view of the map at all — it moves, waits, and reads
 * one number. That gap between deciding-with-information and groping-by-feedback is the
 * subject of group D, and it shows up here for free.
 */

import { PLANE, defineModel } from '../config.js';
import { ces, smoothEoS } from '../econ.js';
import { firmLocationCrawl, personBundle } from '../behaviour.js';
import { MEAN_SD } from '../record.js';
import { randomly } from '../space.js';

/** @type {import('../scenario.js').Scenario} */
export const b3 = {
  id: 'b3',
  title: 'The black hole: space collapses',
  thesisRef: '§5.3.2',
  defaultDays: 1000,

  notes: `20 People, 4 Firms, random start, everyone mobile, NO density cost.

Watch peopleNN_mean and firmNN_mean (mean nearest-neighbour distance) both fall towards
zero: everyone piles onto one spot and space stops existing.

Watch the RATE. People collapse quickly -- they evaluate candidate locations and can see
which is better. Firms take far longer, because a Firm sees only its own revenue and has to
grope. Same objective, very different competence, and the difference is purely how much each
can observe.

This is the result the density cost of group C exists to prevent. Without something for
distance to trade against, a spatial model has no space in it.`,

  config: () =>
    defineModel({
      spaceKind: PLANE, width: 1.0,
      peopleCount: 20, firmCount: 4,
      placePeople: randomly(),
      placeFirms: randomly(),
      utility: ces(0.5),
      production: smoothEoS(),
      baseGoodCost: 1.0,
      wage: 1.0,
      deliveryCost: 1.0,
      densityCost: 0, // nothing opposes agglomeration
      personBehaviour: personBundle(),
      firmBehaviour: firmLocationCrawl(),
      peopleMobile: true,
      firmsMobile: true,
    }),

  record(r, w) {
    r.global('peopleNN_mean', () => w.meanNearestNeighbour(w.people));
    r.global('firmNN_mean', () => w.meanNearestNeighbour(w.firms));
    r.aggregate('peopleSpread', w.people, (p) => w.distance(p, w.person(0)), MEAN_SD);
    r.global('movementMean', () => w.meanPersonMovement());
    r.aggregate('utility', w.people, (p) => p.utility, MEAN_SD);
  },

  controls: (w) => [w.deliveryCostParam(), w.densityCostParam()],
};
