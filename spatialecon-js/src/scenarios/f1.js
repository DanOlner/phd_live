// @ts-check

/**
 * **F1 — zero-intelligence money exchange.** Thesis §4.7.2.
 *
 * A hundred agents, a hundred units each. Repeatedly: pick an agent, and if they have
 * anything, hand one unit to another agent chosen at random. That is the entire model. No
 * preferences, no strategy, no production, no prices — nothing anyone would recognise as
 * economics.
 *
 * After a couple of hundred thousand exchanges the distribution is severely unequal: a few
 * very wealthy, most in the bottom band, and a Gini coefficient not far off a real country's.
 * It is a bounded random walk, and this is simply what bounded random walks look like.
 *
 * **The thesis is here to warn about this model, not to offer it.** The footnote is the
 * sharpest line in chapter 4, and it is about what one is entitled to conclude:
 *
 * > There is not a large gap between claiming it does to making justifications for wealth
 * > inequality as a natural, unavoidable consequence of money flow.
 *
 * The model *grows* inequality. It does not *explain* it. Nothing in it corresponds to
 * anything about how wealth is actually distributed, and the resemblance of the output to
 * real data is precisely what makes it dangerous — a good fit arrived at by a mechanism with
 * no claim to being the right one. Compare E4, which is the opposite case: an agent model
 * produced something, and then the simplest possible reduction said *what* it was.
 *
 * There is also a practical consequence, and it runs through the rest of the thesis. This is
 * one of the reasons money is abandoned entirely from chapter 6 onwards in favour of time as
 * the medium of exchange. A conserved quantity that agents pass around develops dynamics of
 * its own, and those dynamics swamp whatever the model was built to study.
 */

import { POINT, defineModel, parameter } from '../config.js';
import { ces, smoothEoS } from '../econ.js';

const AGENTS = 100;
const START = 100;
/** Exchanges per day. 200,000 total over the default run, as the thesis has it. */
let perDay = 200;
const DAYS = 1000;

/** @type {number[]} */
let wealth = [];
let exchanges = 0;

/** @param {import('../world.js').World} world */
function reset(world) {
  void world;
  wealth = new Array(AGENTS).fill(START);
  exchanges = 0;
}

/** @type {import('../scenario.js').Scenario} */
export const f1 = {
  id: 'f1',
  title: 'Zero-intelligence money exchange',
  thesisRef: '§4.7.2',
  defaultDays: DAYS,

  notes: `${AGENTS} agents, ${START} units each, 200 exchanges a day for ${DAYS} days --
200,000 exchanges in total.

Each exchange: pick an agent at random; if they have anything, give one unit to another agent
chosen at random. That is the whole model. No preferences, no strategy, no production, no
prices.

Watch the skyline. It starts perfectly flat -- everyone on ${START} -- and ends with a few
agents holding many times the average and most of the population in the bottom band. Watch
the Gini coefficient climb from 0.

Press reroll: a different set of agents gets rich, and the SHAPE is identical.

Now the important part, which is a warning rather than a result.

This model GROWS inequality. It does not EXPLAIN it. Nothing in it corresponds to anything
about how wealth is actually distributed, and the resemblance of its output to real data is
exactly what makes it dangerous -- a good fit reached by a mechanism with no claim to being
the right one. The thesis' footnote is the sharpest line in chapter 4: "There is not a large
gap between claiming it does to making justifications for wealth inequality as a natural,
unavoidable consequence of money flow."

Compare model e4, which is the opposite case: an agent model produced something surprising,
and the simplest possible reduction then said what it actually was.

There is a practical consequence too. This is one reason money is abandoned entirely from
chapter 6 onwards in favour of time. A conserved quantity that agents pass around develops
dynamics of its own, and those dynamics swamp whatever the model was built to study.`,

  // A shell config: this model has its own state and does not use the agent engine at all.
  // It is a scenario so that it is a page like any other.
  config: () =>
    defineModel({
      spaceKind: POINT,
      peopleCount: 1, firmCount: 1,
      utility: ces(0.5),
      production: smoothEoS(),
    }),

  setUp(world) {
    reset(world);
  },

  beforeDay(world) {
    for (let i = 0; i < perDay; i++) {
      const giver = Math.floor(world.rng.nextDouble() * AGENTS);
      if (wealth[giver] <= 0) continue;
      let receiver = Math.floor(world.rng.nextDouble() * AGENTS);
      if (receiver === giver) receiver = (receiver + 1) % AGENTS;
      wealth[giver] -= 1;
      wealth[receiver] += 1;
      exchanges++;
    }
  },

  record(r) {
    r.global('exchanges', () => exchanges);
    r.global('gini', () => giniOf(wealth));
    r.global('richest', () => Math.max(...wealth));
    r.global('poorest', () => Math.min(...wealth));
    r.global('withNothing', () => wealth.filter((v) => v === 0).length);
    // The share held by the top tenth — the number the resemblance to real data trades on.
    r.global('topTenthShare', () => {
      const sorted = wealth.slice().sort((a, b) => b - a);
      const top = sorted.slice(0, Math.max(1, Math.round(AGENTS / 10)));
      return top.reduce((a, v) => a + v, 0) / (AGENTS * START);
    });
    r.perActor('wealth', wealth.map((_, i) => i), (i) => wealth[i]);
  },

  controls: () => [
    // A real control, not decoration: winding it up is the fastest way to see that the
    // shape is a property of the process and not of how long it has been running.
    parameter({
      name: 'perDay', label: 'exchanges per day', min: 10, max: 2000,
      get: () => perDay, set: (v) => { perDay = Math.round(v); },
    }),
  ],
};

/** Exposed so the view can read the population without going through the recorder. */
export function currentWealth() {
  return wealth;
}

/** @param {number[]} values */
function giniOf(values) {
  const n = values.length;
  if (n === 0) return 0;
  const sorted = values.slice().sort((a, b) => a - b);
  let total = 0;
  let weighted = 0;
  for (let i = 0; i < n; i++) {
    total += sorted[i];
    weighted += (i + 1) * sorted[i];
  }
  if (total === 0) return 0;
  return (2 * weighted) / (n * total) - (n + 1) / n;
}
