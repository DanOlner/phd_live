// @ts-check

/**
 * **E2 — markup and the tipping point.** Thesis §6.3.2.
 *
 * The honest failure, and the thesis says so. Everything up to here has Firms clearing their
 * stock and taking no profit. Give one a *markup* — have it target a stock level that rises a
 * little on every sale, and skim the increase — and ask what happens.
 *
 * What happens is that nothing stable does. There are two endings and neither is a
 * competitive equilibrium:
 *
 * - **Implicit collusion.** Both Firms raise price together, squeezing People until they can
 *   barely buy anything. Nothing stops it, because a Person in this model has no exogenous
 *   alternative — no savings, no outside option, no way to withhold their day — and therefore
 *   no sanction to apply.
 * - **Runaway.** One Firm raises its price past the point where it can still attract the
 *   demand it needs to hit its own rising target. Now it is failing by more each transaction,
 *   so it raises price again, and compounds. `consecutivePriceRises` counts the compounding.
 *
 * **The finding that is not about failure.** Watch the *other* Firm while Firm 0 marks up.
 * Its price *falls*. It picks up the demand Firm 0 has priced itself out of, and more input
 * means more efficient production under increasing returns, which means it can clear at a
 * lower price. So one Firm's markup does not merely redistribute — with economies of scale it
 * changes the *absolute* price level for everyone, because it changes where production
 * happens and therefore at what scale.
 *
 * Without economies of scale it would change relative prices only. That contrast is the model:
 * set the EoS curve to zero and Firm 1's price stops responding.
 */

import { ces, smoothEoS } from '../econ.js';
import { MEAN } from '../record.js';
import { atCentre } from '../space.js';
import { parameter } from '../config.js';
import { belt } from './belt.js';

const RHO = 0.5;
const DAYS = 4000;
const MAX_MARKUP = 0.15;

const u = ces(RHO);
const production = smoothEoS(0.5, 1);

/**
 * Firm 0's markup: how much its stock target rises on every transaction.
 *
 * It ratchets, so a constant markup is a *linearly rising* target, and the Firm has to keep
 * finding more demand to keep hitting it. That is the whole mechanism — a Firm taking profit
 * here is not charging a margin, it is chasing a receding goal.
 */
let markUp = 0;

/**
 * Set once the visitor moves the markup slider, which then stops the sweep.
 *
 * Needed because this scenario both *drives* a parameter and *exposes* it, and without this
 * the ramp overwrites the slider on the next day — the control appears to work and does
 * nothing. It also matters for what the model shows: the gradual ramp produces the squeeze,
 * because the Firm adapts as the target rises under it, while dropping the markup straight
 * onto 0.15 produces the runaway. Both endings are real and you need the slider to reach the
 * second one.
 */
let held = false;

/** @type {import('../scenario.js').Scenario} */
export const e2 = {
  id: 'e2',
  title: 'Markup and the tipping point',
  thesisRef: '§6.3.2',
  defaultDays: DAYS,

  notes: `Two Firms, one Person, no space. Firm 0's markup is raised from 0 to ${MAX_MARKUP}
over the run; Firm 1 never marks up. EoS curve 0.5.

The markup is per TRANSACTION and it RATCHETS: the Firm's stock target rises by that much on
every sale, so a constant markup is a linearly rising target and the Firm has to keep finding
more demand to keep hitting it. A Firm taking profit here is not charging a margin, it is
chasing a receding goal.

Watch price_0 and price_1 together. As Firm 0 marks up, its price rises and FIRM 1'S PRICE
FALLS. Firm 1 picks up the demand Firm 0 priced itself out of; more input means more
efficient production under increasing returns; so it clears at a lower price. One Firm's
markup changes the absolute price level for everyone, not just relative prices, because it
changes where production happens and therefore at what scale.

Set the EoS curve to 0 with the slider and that coupling disappears. Measured at a fixed
markup of 0.02, once settled, the amount Firm 1's price is dragged from its no-markup value:

  EoS curve 0     0.0000003   nothing at all
  EoS curve 0.5   0.0026
  EoS curve 1     0.0047

Zero effect at zero curve, growing with EoS -- as the thesis says. The coupling IS the
economies of scale. Without them a markup changes relative prices only.

(Settling matters for that first row. At 2000 days it reads 0.002, which is not a small
effect but an unfinished one -- at zero curve the equilibrium price is 1.0 and these runs
start near 0.66, so they are still travelling. Give it 6000 days and it is zero to seven
decimal places.)

Watch risingRuns. It counts consecutive price rises by Firm 0. A long run means the Firm has
lost so much demand it can no longer reach its own target at any price it can charge, so it
raises price again and compounds the error -- the runaway. There is no stable competitive
outcome here, and the thesis says so plainly: People have no exogenous alternative and
therefore no sanction.

TWO ENDINGS, and which one you get depends on how the markup arrives.

Left alone, the run ramps the markup slowly from 0 to 0.15, and you get the SQUEEZE: Firm 0's
price climbs to 1.27, Firm 1's falls to 0.76, and utility slides from 2.5 to 2.1. The Firm
adapts as its own target rises under it, and both Firms end up dearer than they started.

Drag the markup slider straight to 0.15 instead -- which stops the ramp -- and you get the
RUNAWAY. Firm 0's price reaches five thousand, it sells nothing at all, Firm 1 becomes the
sole producer at the one-Firm clearing price of 0.667, and utility falls to 1.50.

Neither is a competitive equilibrium, and the thesis says so plainly: People here have no
exogenous alternative -- no savings, no outside option, no way to withhold their day -- and
therefore no sanction to apply.`,

  config: () =>
    belt(2, 1, RHO, {
      utility: u,
      production,
      placeFirms: atCentre(),
      placePeople: atCentre(),
      initialPriceRange: /** @type {[number, number]} */ ([0.66, 0.67]),
    }),

  setUp(world) {
    markUp = 0;
    held = false;
    world.firm(0).markUp = 0;
    world.firm(1).markUp = 0;
  },

  beforeDay(world, day) {
    if (!held) {
      const t = Math.min(day, DAYS - 1) / Math.max(DAYS - 1, 1);
      markUp = t * MAX_MARKUP;
    }
    world.firm(0).markUp = markUp;
  },

  record(r, w) {
    r.global('markUp', () => markUp);
    r.global('price_0', () => w.firm(0).goodCost);
    r.global('price_1', () => w.firm(1).goodCost);
    r.global('target_0', () => w.firm(0).movingMarkUpTarget);
    r.global('input_0', () => w.firm(0).contributedTime);
    r.global('input_1', () => w.firm(1).contributedTime);
    r.global('risingRuns', () => w.firm(0).consecutivePriceRises);
    r.aggregate('utility', w.people, (p) => p.utility, MEAN);
  },

  controls: () => [
    u.rhoParam(),
    production.curveParam(),
    parameter({
      name: 'markup', label: "Firm 0's markup", min: 0, max: MAX_MARKUP * 2,
      get: () => markUp, set: (v) => { markUp = v; held = true; },
    }),
  ],
};
