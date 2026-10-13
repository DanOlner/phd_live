// @ts-check

/**
 * **E7 — nineteen Firms and nineteen People on a line.** Thesis §6.4.1.
 *
 * Matched pairs at nineteen evenly spaced positions, so one pair sits dead centre. Distance
 * cost selects how many efficient producers emerge and where — one, two, or many.
 *
 * Before any transition, the centre is already advantaged: an agent in the middle can reach
 * more Firms cheaply, so gets more utility, and the central Firm receives the most input.
 * Geography alone produces inequality, with no increasing returns needed.
 *
 * The welfare result is the one worth putting on a website. Transitions to more efficient
 * production **never make any individual Person worse off**. Aggregate welfare rises and
 * nobody pays for it: larger-scale production is cheaper for everyone within reach, and those
 * out of reach are simply unaffected. That is a positive externality, and it is unusual —
 * most structural change in economics creates losers.
 *
 * Verified here in *trend* rather than instant by instant: minimum utility roughly triples
 * across the run, so nobody ends worse off, but individual samples show local dips while
 * efficiency is rising. The strong reading — never worse off at any moment — is not confirmed.
 */

import { ces, smoothEoS } from '../econ.js';
import { MEAN_SD, MIN_MAX } from '../record.js';
import { LINE } from '../config.js';
import { evenlyAlongX } from '../space.js';
import { belt } from './belt.js';

const AGENTS = 19;
const DISTANCE_FROM = 8.0;
const DISTANCE_TO = 3.5;
const DAYS = 6000;

const u = ces(0.7);

/** @type {import('../scenario.js').Scenario} */
export const e7 = {
  id: 'e7',
  title: 'Nineteen Firms and People on a line',
  thesisRef: '§6.4.1',
  defaultDays: DAYS,

  notes: `19 Firms and 19 People at matched positions on a line of length 1. rho 0.7, EoS
curve and magnitude 0.1, as shipped in 1DLineManyAgentsTransmission.

Distance cost falls from ${DISTANCE_FROM} to ${DISTANCE_TO} across the run. Watch
efficiencyRatio and efficiency_max: at high distance costs production is spread evenly, and
as costs fall the centre takes off.

Before any transition, look at utility across People: the middle is already better off,
because it can reach more Firms cheaply. Geography produces inequality on its own.

The welfare claim to check is minUtility. It should not fall when a transition happens.
Efficient production is a positive externality -- cheaper goods for everyone in reach, and no
effect on anyone else.`,

  config: () =>
    belt(AGENTS, AGENTS, 0.7, {
      utility: u,
      spaceKind: LINE, width: 1.0,
      placePeople: evenlyAlongX(0, 1),
      placeFirms: evenlyAlongX(0, 1),
      production: smoothEoS(0.1, 0.1), // as shipped
      deliveryCost: DISTANCE_FROM,
      initialPriceRange: /** @type {[number, number]} */ ([0.3, 0.7]),
    }),

  beforeDay(world, day) {
    if (day < 500) return; // settle first
    const t = (day - 500) / Math.max(DAYS - 500, 1);
    world.setDeliveryCost(DISTANCE_FROM + t * (DISTANCE_TO - DISTANCE_FROM));
  },

  record(r, w) {
    r.global('distanceCost', () => w.config.deliveryCost);
    r.global('efficiencyRatio', () => efficiencyRatio(w));
    r.global('centralFirmEfficiency', () => w.firm(Math.floor(AGENTS / 2)).outputPerUnitTime);
    r.global('minUtility', () => Math.min(...w.people.map((p) => p.utility)));

    r.aggregate('efficiency', w.firms, (f) => f.outputPerUnitTime, MIN_MAX);
    r.aggregate('utility', w.people, (p) => p.utility, MEAN_SD);
    r.aggregate('price', w.firms, (f) => f.goodCost, MIN_MAX);

    // Per-position, for the profile drawn above and below the line in the original.
    r.perActor('firmEfficiency', w.firms, (f) => f.outputPerUnitTime);
    r.perActor('personUtility', w.people, (p) => p.utility);
  },

  controls: (w) => [u.rhoParam(), w.deliveryCostParam()],
};

/** Higher output-per-unit over lower, so always >= 1. @param {import('../world.js').World} w */
export function efficiencyRatio(w) {
  let lo = Infinity, hi = 0;
  for (const f of w.firms) {
    lo = Math.min(lo, f.outputPerUnitTime);
    hi = Math.max(hi, f.outputPerUnitTime);
  }
  return lo <= 0 ? 1 : hi / lo;
}
