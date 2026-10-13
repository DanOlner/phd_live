// @ts-check

/**
 * **C3 — spatial against non-spatial costs.** Thesis §5.4.3.
 *
 * Four sweeps of the same settlement, one per cost, each plotted against how far from the
 * centre the People end up. The result the thesis wants is a *sign*:
 *
 * | cost | kind | effect |
 * |---|---|---|
 * | wage — "the cost of money" | non-spatial | **agglomerating** |
 * | base good cost | non-spatial | **dispersive** |
 * | commute cost | spatial | **agglomerating** |
 * | goods distance cost | spatial | **agglomerating** |
 *
 * The mechanism is visible in the closed form C1 gives for one Firm: `U = (w − D(d)) / (p + cd)`.
 * A non-spatial cost enters as a constant — the wage on top, the base price in the
 * denominator — and changes how much the *distance* term is worth relative to the crowding
 * term. Raise the wage and crowding becomes cheap in proportion, so People will accept more
 * of it and pack in. Raise the base price and the `cd` term matters proportionally less, so
 * distance stops being worth paying to avoid and they spread out. A spatial cost enters
 * multiplied by distance and simply makes distance dearer, which pulls inward every time.
 *
 * **Why this model licenses the whole of chapter 6.** If wage and goods costs act on location
 * through the same channel — and differ only in whether they are multiplied by distance —
 * then there is no need to model a labour market and a goods market as separate things. That
 * is precisely the collapse the transmission belt performs when it throws money away and has
 * People buy goods directly with time. C3 is the argument that the collapse is allowed.
 *
 * **Reconstructed.** C3's config is one of the two that are lost (CODE_ANATOMY §4). The
 * *shape* — four sweeps against mean distance from the centre — is from the thesis text; the
 * population, the space and the density cost are C1's, on the grounds that C3 is C1 with a
 * cost being moved. The directions it produces are checked against the thesis in `test/`.
 *
 * The sweep runs up and back down, as the original's did, so any hysteresis shows as the two
 * passes failing to lie on top of each other.
 */

import { personBundle } from '../behaviour.js';
import { PLANE, defineModel } from '../config.js';
import { ces, smoothEoS } from '../econ.js';
import { MEAN, MEAN_SD } from '../record.js';
import { atCentre, randomly } from '../space.js';

const DAYS = 4000;
const PEOPLE = 300;

/** @typedef {'wage' | 'goodCost' | 'commute' | 'delivery'} Which */

/**
 * @param {string} id @param {Which} which @param {number} from @param {number} to
 * @param {string} kind @param {string} expected
 * @returns {import('../scenario.js').Scenario}
 */
function make(id, which, from, to, kind, expected) {
  let value = from;

  /** @param {import('../world.js').World} w */
  const apply = (w) => {
    if (which === 'wage') w.setWage(value);
    else if (which === 'goodCost') w.setBaseGoodCost(value);
    else if (which === 'commute') w.setCommuteCost(value);
    else w.setDeliveryCost(value);
  };

  return {
    id,
    title: `Sweeping ${kind === 'spatial' ? 'a spatial' : 'a non-spatial'} cost: ${which}`,
    thesisRef: '§5.4.3',
    defaultDays: DAYS,

    notes: `${PEOPLE} People, one Firm at the centre, C1's density cost. ${which} is swept
from ${from} to ${to} and back again over ${DAYS} days, and the recorded quantity is the mean
distance from the centre.

${which} is a ${kind.toUpperCase()} cost. Expected: ${expected}.

The four C3 models together make the thesis' point, which is about signs rather than
magnitudes. Non-spatial costs (wage, base price) and spatial costs (commuting, delivery) act
on where People end up through DIFFERENT channels, and wage and base price push in OPPOSITE
directions despite both being non-spatial.

Why it matters: if a wage and a goods price reach location through the same channel, there
is no need to model a labour market and a goods market separately. That is exactly the
collapse chapter 6 performs when it throws money away and has People buy goods with time.
This model is the argument that the collapse is allowed.

The sweep runs up and then back down. Where the two passes do not lie on top of each other,
the settlement has path dependence -- it matters which way the cost was moving.

C3's config is LOST (CODE_ANATOMY section 4). The shape is from the thesis text; the
population, space and density cost are C1's, because C3 is C1 with a cost being moved.`,

    config: () =>
      defineModel({
        spaceKind: PLANE, width: 2.0,
        peopleCount: PEOPLE, firmCount: 1,
        placePeople: randomly(),
        placeFirms: atCentre(),
        utility: ces(0.4),
        production: smoothEoS(),
        baseGoodCost: which === 'goodCost' ? from : 1.0,
        wage: which === 'wage' ? from : 1.0,
        deliveryCost: which === 'delivery' ? from : 0.25,
        commuteCost: which === 'commute' ? from : 0,
        densityCost: 10,
        densityRadius: 1 / 16,
        personBehaviour: personBundle(),
        peopleMobile: true,
      }),

    /** Up for the first half of the run, back down for the second. */
    beforeDay(world, day) {
      const half = DAYS / 2;
      const t = day < half ? day / half : Math.max(0, (DAYS - day) / half);
      value = from + t * (to - from);
      apply(world);
    },

    record(r, w) {
      const firm = w.firm(0);
      r.global(which, () => value);
      r.global('rising', () => (w.day < DAYS / 2 ? 1 : 0));
      r.aggregate('distanceFromCentre', w.people, (p) => w.distance(p, firm), MEAN_SD);
      r.aggregate('density', w.people, (p) => p.densityCost, MEAN);
      r.aggregate('utility', w.people, (p) => p.utility, MEAN);
      r.global('movement', () => w.meanPersonMovement());
    },

    controls: (w) => [w.densityCostParam(), w.deliveryCostParam()],
  };
}

export const c3w = make('c3w', 'wage', 0.05, 1.5, 'non-spatial',
  'agglomerating -- a higher wage makes crowding cheap in proportion, so People accept more of it');
export const c3g = make('c3g', 'goodCost', 0.05, 2.0, 'non-spatial',
  'dispersive -- a higher base price makes the distance term matter proportionally less');
export const c3c = make('c3c', 'commute', 0, 1.0, 'spatial',
  'agglomerating -- commuting is paid per unit of distance, so distance gets dearer');
export const c3d = make('c3d', 'delivery', 0, 1.0, 'spatial',
  'agglomerating -- delivery is paid per unit of distance, so distance gets dearer');
