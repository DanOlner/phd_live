// @ts-check

/**
 * **C8 — where spatial equilibrium stops being utility equilibrium.** Thesis §5.4.7.
 *
 * The same monocentric run as C1, with the density multiplier stepped up through 10, 100 and
 * 500 across the run. This is the model surprising its author, and the thesis says so.
 *
 * At 10, utility is flat across distance: the clean trade-off of C1. At 100, People on the
 * outskirts start refusing density altogether and take the free space at the edge; a second
 * band appears and utility begins to slope. At 500, nobody accepts any density at all and the
 * settlement disperses completely, utility falling steadily with distance.
 *
 * The mechanism is the viability rule in the Bundle: a candidate whose density cost exceeds
 * the wage is discarded rather than scored negative, so once the multiplier is high enough,
 * every central location simply drops out of consideration. It is C1's fringe, grown until it
 * swallows the settlement — the two models are one continuum, not two regimes.
 *
 * What survives is still a **Nash equilibrium** — nobody can improve by moving alone. It is
 * no longer a **utility equilibrium** — some are permanently better off than others, holding
 * good spots that latecomers cannot bid away because there is nothing to bid with. There is
 * also path dependence: who ends up where depends on who moved first.
 *
 * The point for the thesis is sharp. Spatial equilibrium is the assumption urban economics
 * rests on — Glaeser's "no potential for arbitrage across space". It holds here, but only
 * inside a parameter range, and the model finds its own edge.
 */

import { MEAN_SD } from '../record.js';
import { monocentric } from './monocentric.js';

const STEPS = [10, 100, 500];
const DAYS = 3000;

/** @type {import('../scenario.js').Scenario} */
export const c8 = {
  id: 'c8',
  title: 'Spatial equilibrium is not always utility equilibrium',
  thesisRef: '§5.4.7',
  defaultDays: DAYS,

  notes: `As c1, but the density multiplier steps 10 -> 100 -> 500 across the run, with an
equal number of days at each. Ships as the MonoCentricDensity jar.

Watch utilityStats_sd. Near zero in the first third (everyone equally well off), then rising
as bands form, and highest at 500 where utility falls steadily with distance from the centre.

Watch densityStats_mean fall towards zero in the final third: nobody is accepting any
density at all. And watch distanceStats_mean rise -- the settlement is spreading to the edges.

The end state is still a Nash equilibrium: no individual can improve by moving alone. It is
simply no longer one where everybody does equally well.`,

  config: () => monocentric(STEPS[0]),

  /** Step the density multiplier at even intervals through the run. */
  beforeDay(world, day) {
    const phase = Math.floor((day * STEPS.length) / Math.max(DAYS, 1));
    // Density is not a per-Firm cost, so this one may go straight to the config.
    world.config.densityCost = STEPS[Math.min(phase, STEPS.length - 1)];
  },

  record(r, w) {
    r.global('densityMultiplier', () => w.config.densityCost);

    r.perActor('distanceToCentre', w.people, (p) => w.distance(p, w.firm(0)));
    r.perActor('utility', w.people, (p) => p.utility);
    r.perActor('density', w.people, (p) => p.densityCost);

    r.aggregate('utilityStats', w.people, (p) => p.utility, MEAN_SD);
    r.aggregate('densityStats', w.people, (p) => p.densityCost, MEAN_SD);
    r.aggregate('distanceStats', w.people, (p) => w.distance(p, w.firm(0)), MEAN_SD);
    r.global('movementMean', () => w.meanPersonMovement());
  },

  controls: (w) => [w.densityCostParam(), w.deliveryCostParam()],
};
