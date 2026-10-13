// @ts-check

/**
 * **E8 — the transmission belt meets the density cost.** Thesis §6.4.2. The closing result.
 *
 * Two Firms a distance of 1 apart, 300 mobile People, ρ raised from 0.5. A more-efficient
 * producer emerges, People migrate to it, and a settlement forms around it — while they
 * continue to buy the other good in from a distance.
 *
 * What makes this the closing result is what it removes. In C7 the same two-core shift
 * happens, but only because the two Firms were *given* different distance costs by the
 * modeller. Here the Firms are identical in every respect. Which one wins is decided by the
 * model, from an arbitrarily small initial difference in price, amplified by the feedback
 * between demand and production scale. An assumption has been replaced by a mechanism.
 *
 * **Which ratio.** The published "Firm efficiency ratio" is the ratio of `productionLevel` —
 * total output f(T) — recovered by decompiling the shipped jar. Not output *per unit*, which
 * with the shipped `EoS_Curve = 0.001, EoS_Magnitude = 1` is `0.001·T + 1` and therefore
 * cannot exceed 1.3 for any split of 300 People's time. Both are recorded here, because the
 * difference between them is the whole reason this model looked weaker than it is.
 */

import { ces, smoothEoS } from '../econ.js';
import { PLANE } from '../config.js';
import { MEAN_SD, MIN_MAX } from '../record.js';
import { randomly, twoPointsApart } from '../space.js';
import { belt } from './belt.js';

const RHO_FROM = 0.5;
const RHO_TO = 0.9;
const DAYS = 4200;  // the sweep, plus room for the transition to settle

const u = ces(RHO_FROM);

/** Ratio of total output — the statistic the published figure plots.
 * @param {import('../world.js').World} w */
export function productionRatio(w) {
  const levels = w.firms.map((f) => f.productionLevel);
  const lo = Math.min(...levels), hi = Math.max(...levels);
  return lo <= 0 ? 1 : hi / lo;
}

/** Ratio of output per unit of input. Bounded by (curve·T + m)/m — for E8, by 1.3.
 * @param {import('../world.js').World} w */
export function perUnitRatio(w) {
  const eff = w.firms.map((f) => f.outputPerUnitTime);
  const lo = Math.min(...eff), hi = Math.max(...eff);
  return lo <= 0 ? 1 : hi / lo;
}

/** @type {import('../scenario.js').Scenario} */
export const e8 = {
  id: 'e8',
  title: 'Transmission belt plus density cost',
  thesisRef: '§6.4.2',
  defaultDays: DAYS,

  notes: `Two identical Firms 1 apart, 300 mobile People, distance cost 0.25, density cost
10, EoS curve 0.001, magnitude 1. Parameters from TransmissionPlusDensity1.properties as
shipped. rho rises from ${RHO_FROM} to ${RHO_TO} over the first 3000 days, then holds while the
transition completes.

Watch productionRatio: it sits at ~1.0 until rho reaches about 0.88, then jumps to ~33
within a few hundred days. The transition is close to vertical, which is the point -- this
is a catastrophic shift in production structure, not a gradual drift. It passes through 13
on the way up, at around day 2850, which is consistent with the thesis' figure being
captured during the jump rather than after it. Watch meanDistToFirm0 move at the same
moment as People migrate towards whichever Firm won.

TWO RATIOS are recorded, and the difference matters. productionRatio is the ratio of total
output f(T) -- the statistic the published figure plots, recovered by decompiling the jar.
perUnitRatio is the ratio of output per unit of input, which for this config is 0.001*T + 1
and so cannot exceed 1.3 however lopsided the split. Reading the second as though it were
the first is why this model appeared not to reproduce.

The Firms are identical. Nothing distinguishes them but an arbitrarily small difference in
starting price. Which one wins is chance, so rerolling should sometimes favour the other;
the SHAPE is what is reproducible, not the winner.

Compare with c7, where the same two-core outcome required the modeller to hand one Firm a
higher distance cost. Here that is endogenous.`,

  config: () =>
    belt(2, 300, RHO_FROM, {
      utility: u,
      // width 2 as shipped in TransmissionPlusDensity1.properties. The Java build used
      // width 1, which puts the two Firms (1 apart, centred) exactly on the space corners,
      // so the settlement that forms around the winner is clipped by the boundary.
      spaceKind: PLANE, width: 2.0,
      placeFirms: twoPointsApart(1.0),
      placePeople: randomly(),
      production: smoothEoS(0.001, 1.0), // as shipped
      deliveryCost: 0.25,
      densityCost: 10,
      densityRadius: 1 / 16,
      initialPriceRange: /** @type {[number, number]} */ ([0.45, 0.55]),
      peopleMobile: true,
    }),

  beforeDay(world, day) {
    // Sweep, then hold. The transition is still accelerating when rho arrives at 0.9 and
    // takes about another 600 days to settle -- at which point productionRatio reaches
    // 13.55, against the thesis' ~13.
    const sweepDays = 3000;
    const t = Math.min(day, sweepDays) / Math.max(sweepDays - 1, 1);
    u.rho = RHO_FROM + t * (RHO_TO - RHO_FROM);
  },

  record(r, w) {
    r.global('rho', () => u.rho);
    r.global('productionRatio', () => productionRatio(w));
    r.global('perUnitRatio', () => perUnitRatio(w));
    r.global('meanDistToFirm0', () =>
      w.people.reduce((a, p) => a + w.distance(p, w.firm(0)), 0) / w.people.length);
    r.global('firm0Production', () => w.firm(0).productionLevel);
    r.global('firm1Production', () => w.firm(1).productionLevel);
    r.aggregate('contributedTime', w.firms, (f) => f.contributedTime, MIN_MAX);
    r.aggregate('price', w.firms, (f) => f.goodCost, MIN_MAX);
    r.aggregate('utility', w.people, (p) => p.utility, MEAN_SD);
    r.aggregate('density', w.people, (p) => p.densityCost, MEAN_SD);
    r.global('movementMean', () => w.meanPersonMovement());

    r.perActor('personX', w.people, (p) => p.position.x);
    r.perActor('personY', w.people, (p) => p.position.y);
  },

  controls: (w) => [u.rhoParam(), w.deliveryCostParam(), w.densityCostParam()],
};
