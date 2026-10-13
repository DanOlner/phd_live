// @ts-check

/**
 * **A2 — Cobb–Douglas and the Firm with no lever.** Thesis §2.2.2, §5.2.2.
 *
 * Two Firms. Firm 0 holds its price at 1; Firm 1 sweeps its own from 0.2 to 3. Records
 * demand for both.
 *
 * Demand for Firm 0's good does not move. Not approximately — exactly, to the last decimal.
 * And Firm 1's revenue does not move either: it is unit elastic, so every price cut is met
 * by precisely enough extra demand to leave takings unchanged.
 *
 * Which means a Firm facing Cobb–Douglas demand has no lever to pull. It cannot improve its
 * position by pricing, and — since space enters only through price — it has no reason to
 * move either. The choice of utility function has quietly disabled a whole class of agent
 * behaviour before any agent has done anything.
 *
 * The thesis makes a further point here worth carrying onto the website. An agent model
 * would eventually *reveal* this independence, but could never explain it. The explanation
 * is one line of algebra: p_M simply does not appear in demand for F. Simulation is not
 * always the tool with the most explanatory power.
 */

import { POINT, defineModel, parameter } from '../config.js';
import { cobbDouglas, smoothEoS } from '../econ.js';

const FIRM0_PRICE = 1.0;
const SWEEP_FROM = 0.2;
const SWEEP_TO = 3.0;
const DAYS = 100;

const u = cobbDouglas(0.5);
let firm1Price = SWEEP_FROM;

const demand = () => u.optimalQuantities([FIRM0_PRICE, firm1Price], 1.0);

/** @type {import('../scenario.js').Scenario} */
export const a2 = {
  id: 'a2',
  title: 'Cobb-Douglas: the Firm with no lever',
  thesisRef: '§2.2.2, §5.2.2',
  defaultDays: DAYS,

  notes: `Firm 0 holds price at 1.0. Firm 1 sweeps price from 0.2 to 3.0.

Watch demand0: it is flat. Changing one price cannot affect demand for the other good,
because the other price does not appear in its demand equation. Watch revenue1: also flat.
Unit elastic -- the price cut is exactly cancelled by the extra demand.

A Firm here cannot improve its revenue by pricing, so it also has no spatial incentive:
distance enters only through price.`,

  config: () =>
    defineModel({
      spaceKind: POINT,
      peopleCount: 1,
      firmCount: 2,
      utility: u,
      production: smoothEoS(),
      baseGoodCost: FIRM0_PRICE,
    }),

  // Clamped so the sweep holds at its end rather than running on for as long as the page is
  // open. Same reasoning in a1, a3 and a4.
  beforeDay(world, day) {
    const t = Math.min(day, DAYS - 1) / Math.max(DAYS - 1, 1);
    firm1Price = SWEEP_FROM + t * (SWEEP_TO - SWEEP_FROM);
  },

  record(r) {
    r.global('firm1Price', () => firm1Price);
    r.global('demand0', () => demand()[0]);
    r.global('demand1', () => demand()[1]);
    r.global('revenue0', () => demand()[0] * FIRM0_PRICE);
    r.global('revenue1', () => demand()[1] * firm1Price);
    r.global('utility', () => u.utilityOf(demand()));
  },

  controls: () => [
    u.deltaParam(),
    parameter({
      name: 'p1', label: 'Firm 1 price', min: 0.05, max: 5,
      get: () => firm1Price, set: (v) => { firm1Price = v; },
    }),
  ],
};
