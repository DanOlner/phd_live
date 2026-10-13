// @ts-check

/**
 * **E3 — economies of scale against love of variety.** Thesis §6.3.3–6.3.4.
 *
 * Two Firms, one Person, **no space at all**. ρ is raised slowly. Below a threshold the two
 * Firms produce symmetrically; above it the symmetry breaks and one becomes the efficient
 * producer, charging a much lower price and taking most of the day's time.
 *
 * The claim this establishes is the chapter's sharpest: **you do not need space for a
 * catastrophic shift in production structure.** Dixit–Stiglitz cannot show this, because it
 * assumes all varieties are produced symmetrically at the same scale. Here the scale is real
 * and free to move, so it moves.
 *
 * Where the instability comes from is worth being precise about, because "emergence" explains
 * nothing. Both Firms decide simultaneously and never observe each other. The coupling is
 * entirely inside the *Person's* CES demand: every price appears in every good's demand, with
 * ρ setting how strongly. Below the threshold a price perturbation is self-correcting; above
 * it, the demand shift outruns the price correction, the favoured Firm gets more input,
 * produces more efficiently, can charge less, and attracts more still.
 *
 * The driver is visible in the Person's own utility: they shift demand because doing so makes
 * them better off. Efficiency gains reach welfare — which is what "transmission belt" names.
 */

import { ces } from '../econ.js';
import { MEAN, MEAN_SD, MIN_MAX, RATIO_GTE_ONE } from '../record.js';
import { atCentre } from '../space.js';
import { belt } from './belt.js';

const RHO_FROM = 0.6;
const RHO_TO = 0.9;
const DAYS = 6000;

const u = ces(RHO_FROM);

/** @type {import('../scenario.js').Scenario} */
export const e3 = {
  id: 'e3',
  title: 'EoS vs love of variety: production regime shift',
  thesisRef: '§6.3.3, §6.3.4',
  defaultDays: DAYS,

  notes: `Two Firms, one Person, NO SPACE. rho is raised slowly from ${RHO_FROM} to ${RHO_TO}.

Watch efficiencyRatio (higher output-per-unit over lower, so always >= 1). It sits at 1.0
while production is symmetric, then breaks away. Watch price_min/price_max separate at the
same moment: the winner's price falls towards 0.5, which is the one-Firm clearing price,
while the loser's climbs.

Utility needs reading carefully. It declines throughout as a background trend, because
raising rho reduces love of variety and that is worth less regardless. What matters is the
LOCAL turn at the transition: it stops falling and starts rising. The Person is not a victim
of the shift, they cause it -- they move their demand because doing so makes them better off.

The Java build broke at rho = 0.764; the thesis puts it near 0.75. Symmetric equilibrium
price 0.6667, deducible by hand and hit exactly.

No distance is involved anywhere. Increasing returns plus substitutability are enough on
their own to reorganise production, which is precisely what a model assuming symmetric
varieties cannot show.`,

  config: () =>
    belt(2, 1, RHO_FROM, {
      utility: u,
      placeFirms: atCentre(),
      placePeople: atCentre(),
      // A slight initial asymmetry is required: from exactly equal prices there is nothing
      // for the feedback to amplify and the model stays symmetric forever.
      initialPriceRange: /** @type {[number, number]} */ ([0.64, 0.68]),
    }),

  beforeDay(world, day) {
    // Clamped: the run stops at defaultDays headless, but a page keeps going, and an
    // unclamped ramp walks the swept value straight out of its meaningful range.
    const t = Math.min(day, DAYS - 1) / Math.max(DAYS - 1, 1);
    u.rho = RHO_FROM + t * (RHO_TO - RHO_FROM);
  },

  record(r, w) {
    r.global('rho', () => u.rho);
    r.aggregate('price', w.firms, (f) => f.goodCost, MIN_MAX);
    r.aggregate('efficiency', w.firms, (f) => f.outputPerUnitTime, MIN_MAX);
    r.aggregate('efficiencyRatio', w.firms, (f) => f.outputPerUnitTime, RATIO_GTE_ONE);
    r.aggregate('contributedTime', w.firms, (f) => f.contributedTime, MIN_MAX);
    r.aggregate('stock', w.firms, (f) => f.stock, MEAN_SD);
    r.aggregate('utility', w.people, (p) => p.utility, MEAN);
  },

  controls: () => [u.rhoParam()],
};
