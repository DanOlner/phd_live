// @ts-check

/**
 * **C4 — two regions, two People.** Thesis §5.4.4.
 *
 * The smallest model in the thesis, and the only one that can be worked out on paper. Two
 * regions; one Firm in region 1, offering a wage of 1 and a good at price g. Reaching region
 * 2 costs d per unit of good. Crowding costs D, paid by each Person when they share a region.
 * Utility is linear in goods, so it is simply what the budget buys.
 *
 * With two People there are only two sensible arrangements, and both have closed forms:
 *
 *     both in region 1     U = (1 − D) / g            each
 *     one in each          U = 1 / g                  the one who stays
 *                          U = 1 / (g + d)            the one who moves
 *
 * Sweeping d and reading off where the curves cross gives the whole equilibrium.
 *
 * **The point, and it is a large one.** Look at which curve the mover ends up on. Moving is
 * worth doing when `1/(g+d) > (1−D)/g` — and when it is, the person who moves lands *below*
 * the person who stayed. The mover pays the distance cost; the stayer keeps the central spot
 * and is relieved of the crowding. So the one who acts confers the larger benefit on the one
 * who does not.
 *
 * Nobody is being altruistic and nobody is coordinating. It falls out of each answering "am I
 * better off over there?" — and it means the equilibrium is produced *entirely* by
 * externalities. That is not an incidental feature of this toy. In an agent model, market
 * interaction is nothing but externalities: every agent's choice set is made of what the
 * others have already done. C1's five hundred People arranging themselves into a city is this
 * same fact, run five hundred times over and no longer legible.
 *
 * No agents are simulated here. The curves are the algebra, which is the point — this is the
 * case where the closed form explains and a simulation would only reveal.
 */

import { POINT, defineModel, parameter } from '../config.js';
import { ces, smoothEoS } from '../econ.js';

const DAYS = 120;
const MAX_DISTANCE_COST = 2.0;

let distanceCost = 0;
let goodCost = 1.0;
let densityCost = 0.5;

/** Both People in region 1, sharing the crowding. */
const together = () => (1 - densityCost) / goodCost;
/** One in each: the one who stays in region 1, alone and paying no distance cost. */
const stayer = () => 1 / goodCost;
/** One in each: the one who moves, alone but paying to have the good delivered. */
const mover = () => 1 / (goodCost + distanceCost);

/** @type {import('../scenario.js').Scenario} */
export const c4 = {
  id: 'c4',
  title: 'Two regions, two People',
  thesisRef: '§5.4.4',
  defaultDays: DAYS,

  notes: `Two regions, two People, one Firm in region 1. Wage 1, good cost g, crowding cost D
paid when both share a region, and d per unit to deliver the good to region 2. Utility is
linear, so it is just what the budget buys.

Sweeps d from 0 to ${MAX_DISTANCE_COST}. Three curves, all closed forms:

  together  (1 - D) / g       both in region 1
  stayer     1 / g            one in each; the one who stayed
  mover      1 / (g + d)      one in each; the one who moved

Watch where "mover" crosses "together". Left of that crossing, moving out is worth it;
right of it, sharing is. With the defaults it happens at d = 1.

Now watch WHERE the mover lands. Whenever moving is worth doing, the mover is still worse
off than the one who stayed -- the stayer gets the central spot AND is relieved of the
crowding. The person who acts confers the bigger benefit on the person who does not.

Nobody is coordinating and nobody is being generous. Each is only asking "am I better off
over there?". The equilibrium is made entirely of externalities, which is what market
interaction consists of in an agent model. C1 is this, five hundred times over.

There are no agents in this model. The curves are the algebra.`,

  // A one-Person, one-Firm shell in a dimensionless space. Nothing runs; the model exists so
  // that C4 is a page like any other, with the same controls and the same chart machinery.
  config: () =>
    defineModel({
      spaceKind: POINT,
      peopleCount: 1, firmCount: 1,
      utility: ces(0.5),
      production: smoothEoS(),
      baseGoodCost: 1.0,
    }),

  beforeDay(world, day) {
    const t = Math.min(day, DAYS - 1) / Math.max(DAYS - 1, 1);
    distanceCost = t * MAX_DISTANCE_COST;
  },

  record(r) {
    r.global('distanceCost', () => distanceCost);
    r.global('together', () => together());
    r.global('stayer', () => stayer());
    r.global('mover', () => mover());
    // 1 while somebody is better off moving out than sharing. The crossing is where this flips.
    r.global('movingPays', () => (mover() > together() ? 1 : 0));
    // What the stayer gains over the mover, wherever moving is the equilibrium.
    r.global('moverShortfall', () => (mover() > together() ? stayer() - mover() : 0));
  },

  controls: () => [
    parameter({
      name: 'g', label: 'good cost', min: 0.2, max: 3,
      get: () => goodCost, set: (v) => { goodCost = v; },
    }),
    parameter({
      name: 'D', label: 'crowding cost', min: 0, max: 0.95,
      get: () => densityCost, set: (v) => { densityCost = v; },
    }),
  ],
};
