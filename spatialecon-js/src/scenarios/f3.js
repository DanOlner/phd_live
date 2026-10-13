// @ts-check

/**
 * **F3 — blind against rational choice.** Thesis §4.7.6.
 *
 * Core Model production, `x = (l − α)/β` with α = 3, and agents who can each supply one unit
 * of labour. Two populations run side by side, identical except in how they decide.
 *
 * - **Rational.** Each agent works out what joining a Firm would yield. With nobody working
 *   anywhere, joining alone gives `(1 − 3)/1 = −2`: worse than nothing. So nobody joins.
 *   Nobody ever joins. **Production never starts**, and every agent is behaving impeccably.
 * - **Blind.** Pick an employer at random. If you were better off this turn than last, do the
 *   same again; otherwise pick another at random. No reasoning about output at all. Four
 *   agents eventually stumble onto the same Firm, `(4 − 3)/1 = 1`, and production starts.
 *
 * **The more rational agent performs worse**, and not marginally — it never produces anything
 * at all. The deadlock is a coordination failure that correct individual reasoning cannot
 * escape, because the payoff to the first mover is strictly negative and the payoff only
 * turns positive once three others have already made the same mistake.
 *
 * This is the argument for the smooth production function used everywhere else in the thesis.
 * `x = vl² + ml` produces something for any positive input, so there is no threshold to
 * coordinate across, and *both* kinds of agent work under it. The Core Model form is
 * perfectly sound as algebra, where l is an aggregate and nobody has to be the first to move.
 * Put it in an agent model and it has a hole in it exactly where the agents are.
 *
 * A3 shows the same function's negative region as a curve. This shows what that region does
 * to a population.
 */

import { POINT, defineModel, parameter } from '../config.js';
import { ces, coreModelEoS, smoothEoS } from '../econ.js';

const AGENTS = 20;
// Ten Firms for twenty agents, so the average headcount is 2 -- below alpha. Coincidence
// has to be stumbled into rather than handed over by the initial scatter, which is the
// thesis' "four agents will eventually stumble onto the same Firm".
const FIRMS = 10;
const DAYS = 400;

let fixedLabour = 3;     // alpha
let production = coreModelEoS(fixedLabour, 1);

/** @typedef {{employer: number[], lastPayoff: number[], output: number}} Population */

/** @type {Population} */
let rational = { employer: [], lastPayoff: [], output: 0 };
/** @type {Population} */
let blind = { employer: [], lastPayoff: [], output: 0 };

/** @returns {Population} */
const empty = () => ({
  employer: new Array(AGENTS).fill(-1),
  lastPayoff: new Array(AGENTS).fill(0),
  output: 0,
});

function reset() {
  production = coreModelEoS(fixedLabour, 1);
  rational = empty();
  blind = empty();
}

/** Headcount per Firm. @param {Population} p */
function headcount(p) {
  const counts = new Array(FIRMS).fill(0);
  for (const f of p.employer) if (f >= 0) counts[f]++;
  return counts;
}

/**
 * What one agent gets from a Firm with `n` workers on it: an equal share of its output.
 * Negative below α, which is the whole point.
 * @param {number} n
 */
const payoffAt = (n) => (n <= 0 ? 0 : production.outputFor(n) / n);

/** @param {Population} p */
function totalOutput(p) {
  return headcount(p).reduce((a, n) => a + (n > 0 ? Math.max(production.outputFor(n), 0) : 0), 0);
}

/**
 * The rational population. Each agent asks what joining would actually yield, and declines
 * anything that would leave them worse off than not working.
 *
 * The result is a deadlock nobody is responsible for. Every individual decision is correct.
 */
function stepRational() {
  const counts = headcount(rational);
  for (let a = 0; a < AGENTS; a++) {
    const here = rational.employer[a];
    const staying = here >= 0 ? payoffAt(counts[here]) : 0;
    let bestFirm = -1;
    let best = staying;
    for (let f = 0; f < FIRMS; f++) {
      if (f === here) continue;
      const joining = payoffAt(counts[f] + 1);
      if (joining > best) { best = joining; bestFirm = f; }
    }
    // Working at all has to beat not working, which below alpha it never does.
    if (bestFirm >= 0 && best > 0) rational.employer[a] = bestFirm;
    else if (staying <= 0) rational.employer[a] = -1;
  }
  rational.output = totalOutput(rational);
}

/**
 * The blind population. No reasoning about output: better than last turn, do it again;
 * worse, try something else.
 *
 * @param {import('../world.js').World} world
 */
function stepBlind(world) {
  const counts = headcount(blind);
  for (let a = 0; a < AGENTS; a++) {
    const here = blind.employer[a];
    const payoff = here >= 0 ? payoffAt(counts[here]) : 0;
    const improved = payoff > blind.lastPayoff[a];
    if (here < 0 || !improved) {
      blind.employer[a] = Math.floor(world.rng.nextDouble() * FIRMS);
    }
    blind.lastPayoff[a] = payoff;
  }
  blind.output = totalOutput(blind);
}

/** @type {import('../scenario.js').Scenario} */
export const f3 = {
  id: 'f3',
  title: 'Blind against rational choice',
  thesisRef: '§4.7.6',
  defaultDays: DAYS,

  notes: `${AGENTS} agents, ${FIRMS} Firms, Core Model production x = (l - alpha) / 1 with
alpha = 3. Each agent supplies one unit of labour. Two populations run side by side,
identical except in how they decide.

RATIONAL agents work out what joining would yield. With nobody working anywhere, joining
alone gives (1 - 3) = -2, which is worse than not working. So nobody joins. Nobody ever
joins. rationalOutput stays at zero for the entire run, and every single decision in that
population is correct.

BLIND agents pick an employer at random and ask only whether they are better off than last
turn. If yes, do the same again; if no, pick another. Four eventually stumble onto the same
Firm, (4 - 3) = 1, and production starts.

The more rational agent performs worse -- and not marginally. It never produces anything at
all. The deadlock is a coordination failure that correct individual reasoning cannot escape,
because the payoff to the first mover is strictly negative and only turns positive once three
others have already made the same mistake.

This is the argument for the smooth production function the thesis uses everywhere else.
x = vl^2 + ml produces something for any positive input, so there is no threshold to
coordinate across and BOTH kinds of agent work under it.

Drag alpha down and watch where the deadlock breaks. It is NOT at 1: with alpha = 1 a lone
worker still produces exactly zero, which is no better than not working, so the rational
population stays deadlocked. It breaks at alpha = 0, where a single worker produces something
and there is no threshold left to coordinate across.

That is the point sharpened. The hole is the threshold itself, not its size -- any positive
alpha is enough to stop a population of correct reasoners from ever starting.

The Core Model form is perfectly sound as algebra, where l is an aggregate and nobody has to
be the first to move. Put it in an agent model and it has a hole in it exactly where the
agents are. Model a3 shows that function's negative region as a curve; this shows what the
region does to a population.`,

  config: () =>
    defineModel({
      spaceKind: POINT,
      peopleCount: 1, firmCount: 1,
      utility: ces(0.5),
      production: smoothEoS(),
    }),

  setUp() {
    reset();
  },

  beforeDay(world) {
    stepRational();
    stepBlind(world);
  },

  record(r) {
    r.global('alpha', () => fixedLabour);
    r.global('rationalOutput', () => rational.output);
    r.global('blindOutput', () => blind.output);
    r.global('rationalWorking', () => rational.employer.filter((f) => f >= 0).length);
    r.global('blindWorking', () => blind.employer.filter((f) => f >= 0).length);
    r.global('blindBiggestFirm', () => Math.max(...headcount(blind)));
  },

  controls: () => [
    parameter({
      name: 'alpha', label: 'fixed labour requirement', min: 0, max: 6,
      get: () => fixedLabour,
      set: (v) => { fixedLabour = Math.round(v); reset(); },
    }),
  ],
};
