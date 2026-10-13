// @ts-check

/**
 * **C2 — inside the density cost.** Thesis §5.4.2.
 *
 * The same run as C1, recorded one level down: how many neighbours each Person actually has
 * inside their radius, and how that count varies with distance from the centre.
 *
 * The count rises towards the centre, which is the trade-off made concrete — People accept
 * more neighbours in exchange for shorter delivery. Meanwhile the density cost each one pays
 * stays remarkably even, because that is what they are equalising.
 *
 * The original also produced a striking hexagonal packing here, People finding interstitial
 * gaps to minimise overlap. The thesis is careful to call it "economically uninteresting" — a
 * consequence of a uniform radius rule, not of anything economic. Worth showing on the
 * website precisely because it looks meaningful and is not: a good, cheap lesson in reading
 * emergent pattern. It is also invisible in CSV and only appears in the plane view.
 */

import { MEAN_SD, MIN_MAX } from '../record.js';
import { monocentric } from './monocentric.js';

/** @type {import('../scenario.js').Scenario} */
export const c2 = {
  id: 'c2',
  title: 'Inside the density cost',
  thesisRef: '§5.4.2',
  defaultDays: 1500,

  notes: `Same run as c1, recorded one level down.

neighbours_N is the discrete count inside each Person's density radius. Correlate it against
distanceToCentre: the count rises towards the centre.

That is the trade-off in raw form -- more neighbours accepted in exchange for cheaper
delivery -- while the density COST each Person pays stays even, because that is the quantity
being equalised.

The original also shows a hexagonal packing emerging here. It is a consequence of a uniform
radius, not of anything economic; the thesis calls it 'economically uninteresting' and it is
worth showing as exactly that. Watch for it in the plane view, not the numbers.`,

  config: () => monocentric(10),

  record(r, w) {
    r.perActor('distanceToCentre', w.people, (p) => w.distance(p, w.firm(0)));
    r.perActor('neighbours', w.people, (p) => p.neighboursInDensityRadius);
    r.perActor('density', w.people, (p) => p.densityCost);

    r.aggregate('neighbourStats', w.people, (p) => p.neighboursInDensityRadius, MEAN_SD);
    r.aggregate('neighbourRange', w.people, (p) => p.neighboursInDensityRadius, MIN_MAX);
    r.aggregate('densityStats', w.people, (p) => p.densityCost, MEAN_SD);
    r.global('movementMean', () => w.meanPersonMovement());
  },

  controls: (w) => [w.densityCostParam(), w.deliveryCostParam()],
};
