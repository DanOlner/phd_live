// @ts-check

/**
 * **C5 — three regions, three People.** Thesis §5.4.4.
 *
 * C4 with one more of everything, which is exactly enough to turn a single crossing into a
 * *sequence* of them. Region 1 holds the Firm; region 2 costs d to deliver to, region 3
 * costs 2d. Crowding costs D per other Person sharing your region.
 *
 * Three arrangements, all closed forms:
 *
 *     all three in region 1        U = (1 − 2D) / g       each
 *     two in region 1, one out     U = (1 − D) / g        the two who stay
 *                                  U = 1 / (g + d)        the one who moved
 *     one in each region           U = 1 / g              region 1
 *                                  U = 1 / (g + d)        region 2
 *                                  U = 1 / (g + 2d)       region 3
 *
 * What the chart draws is the *decision at each stage*: the payoff to staying put against
 * the payoff to being the next one to leave. Two crossings, and they happen at different
 * values of d, so as the distance cost falls the settlement dissolves in a definite order —
 * one Person leaves, then a second, and you can say which decision each crossing is.
 *
 * The C4 asymmetry compounds here. The first to leave pays d and relieves the two who stay
 * of one unit of crowding each. The second pays 2d and relieves the last one entirely. Each
 * mover is worse off than everyone they left behind, and the one who never moves ends up
 * best off of all — on 1/g, the highest utility available in the model, obtained by doing
 * nothing.
 *
 * No agents are simulated. As in C4, the curves are the algebra.
 */

import { POINT, defineModel, parameter } from '../config.js';
import { ces, smoothEoS } from '../econ.js';

const DAYS = 140;
// Far enough out that BOTH crossings sit comfortably inside the chart. With the default
// crowding cost of 0.3 the second departure stops paying at d = 0.21 and the first at
// d = 1.5, so a sweep ending at 1.5 puts one of them exactly on the right-hand edge.
const MAX_DISTANCE_COST = 2.5;

let distanceCost = 0;
let goodCost = 1.0;
let densityCost = 0.3;

/** Each of three sharing region 1: two other occupants to pay for. */
const allThree = () => (1 - 2 * densityCost) / goodCost;
/** Each of two sharing region 1. */
const twoLeft = () => (1 - densityCost) / goodCost;
/** Alone in region 1 — the best position in the model, and it takes no action to reach. */
const aloneAtCentre = () => 1 / goodCost;
/** The first to leave: one region out. */
const firstOut = () => 1 / (goodCost + distanceCost);
/** The second to leave: two regions out. */
const secondOut = () => 1 / (goodCost + 2 * distanceCost);

/** @type {import('../scenario.js').Scenario} */
export const c5 = {
  id: 'c5',
  title: 'Three regions, three People',
  thesisRef: '§5.4.4',
  defaultDays: DAYS,

  notes: `Three regions, three People, one Firm in region 1. Region 2 costs d to deliver to,
region 3 costs 2d. Crowding costs D per other Person in your region.

Sweeps d from 0 to ${MAX_DISTANCE_COST}. Five closed forms, paired into the two decisions:

  FIRST DECISION -- does anyone leave a crowd of three?
    all three   (1 - 2D) / g
    first out    1 / (g + d)

  SECOND DECISION -- once two remain, does one of those leave?
    two left    (1 - D) / g
    second out   1 / (g + 2d)

Each crossing is a specific Person deciding. Read from the right: at high distance cost all
three huddle at the centre. Lower it and "first out" rises past "all three" -- somebody
leaves. Lower it further and "second out" rises past "two left" -- a second follows, and now
one Person sits alone at the centre.

The two crossings are at different values of d, so the settlement dissolves in a definite
ORDER rather than all at once.

Then look at who ends up where. Each mover is worse off than everyone they left behind: the
first pays d and relieves the other two of a unit of crowding each; the second pays 2d and
relieves the last one entirely. The Person who never moves finishes on 1/g -- the highest
utility anywhere in the model -- and reaches it by doing nothing at all.

There are no agents in this model. The curves are the algebra.`,

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
    r.global('allThree', () => allThree());
    r.global('firstOut', () => firstOut());
    r.global('twoLeft', () => twoLeft());
    r.global('secondOut', () => secondOut());
    r.global('aloneAtCentre', () => aloneAtCentre());
    // How many have left, at this distance cost, resolving the decisions in order.
    r.global('departures', () => {
      if (firstOut() <= allThree()) return 0;
      return secondOut() > twoLeft() ? 2 : 1;
    });
  },

  controls: () => [
    parameter({
      name: 'g', label: 'good cost', min: 0.2, max: 3,
      get: () => goodCost, set: (v) => { goodCost = v; },
    }),
    parameter({
      name: 'D', label: 'crowding cost', min: 0, max: 0.45,
      get: () => densityCost, set: (v) => { densityCost = v; },
    }),
  ],
};
