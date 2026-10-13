// @ts-check

/**
 * **C1 — the monocentric result.** Thesis §5.4.1.
 *
 * 500 People arrange themselves around a single central Firm, trading the density cost of
 * being close against the distance cost of being far. Nobody is told to form a settlement.
 *
 * The payoff is the utility column. Density falls off with distance, distance costs rise with
 * it, and the two cancel: **utility ends up near-identical for everyone, wherever they are.**
 * A spatial equilibrium, with no market, no landlord, no bidding and no agent ever comparing
 * itself to another.
 *
 * Worth being precise about why, because it is easy to over-read. Each Person only ever takes
 * the best option in their own choice set. But every move changes the choice set of whoever
 * decides next — moving in raises others' density, moving out lowers it. The equalisation is
 * an accumulation of those external effects, not a market finding a price. The thesis is
 * careful that this "does not allow agents to barter in the sense that arbitrage might
 * imply", yet reaches the arbitrage-free outcome anyway.
 *
 * **It is not perfectly flat, and the reason is worth showing.** Density cannot go below
 * zero, so equalisation is only possible out to the distance where the density profile hits
 * it. Beyond that cutoff, People pay pure distance cost and their utility falls. They are not
 * in disequilibrium — no move improves them — they are outside the range over which
 * equalising is possible. The fringe shrinks as the population grows (43% at 125 People, 2%
 * at 1000), so `du/dd = 0` is a large-numbers idealisation. CODE_ANATOMY §5.3, and the same
 * mechanism that produces C8.
 */

import { MEAN_SD } from '../record.js';
import { monocentric } from './monocentric.js';

/** @type {import('../scenario.js').Scenario} */
export const c1 = {
  id: 'c1',
  title: 'Monocentric settlement and utility equilibrium',
  thesisRef: '§5.4.1',
  defaultDays: 1500,

  notes: `500 People, one Firm at the centre of a 2x2 plane. Density cost 10, radius 1/16,
distance cost 0.25, no commuting. People mobile.

Parameters from CEUS1.properties as shipped in the MonoCentricDensity jar, except the
density multiplier, which that config sets to 100 (the 5.4.7 setting). 10 is the 5.4.1 value
and the one that produces the trade-off band.

Watch utility_sd fall towards zero while utility_mean holds steady: People are equalising,
not converging on a single spot. densityStats_mean should settle well above zero -- they are
paying for proximity, not avoiding it.

movementMean is the spatial-equilibrium test of 5.4.3: below 0.0001 the settlement is
considered settled.

The utility gradient that remains is the fringe effect, not a bug -- see the class comment.`,

  config: () => monocentric(10),

  record(r, w) {
    // Per-Person, for the distance-vs-utility and distance-vs-density correlations that are
    // this model's signature figures.
    r.perActor('distanceToCentre', w.people, (p) => w.distance(p, w.firm(0)));
    r.perActor('utility', w.people, (p) => p.utility);
    r.perActor('density', w.people, (p) => p.densityCost);

    r.aggregate('utilityStats', w.people, (p) => p.utility, MEAN_SD);
    r.aggregate('densityStats', w.people, (p) => p.densityCost, MEAN_SD);
    r.aggregate('distanceStats', w.people, (p) => w.distance(p, w.firm(0)), MEAN_SD);
    r.global('movementMean', () => w.meanPersonMovement());
  },

  controls: (w) => [w.densityCostParam(), w.deliveryCostParam(), w.wageParam()],
};
