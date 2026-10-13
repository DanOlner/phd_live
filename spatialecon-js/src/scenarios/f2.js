// @ts-check

/**
 * **F2 — the permutation gap.** Thesis §4.7.4.
 *
 * A concrete, unglamorous agent-modelling gotcha, and one that would silently corrupt any
 * model built the obvious way.
 *
 * Agents act one at a time, and the order is reshuffled every day so that nobody has a
 * permanent advantage. Reshuffling is the right thing to do. But now ask how much time passes
 * between one agent's consecutive actions. If they go last today and first tomorrow, almost
 * none. If they go first today and last tomorrow, nearly two days. Averaged over the
 * population it is exactly one day — and it is *never* one day for anybody in particular.
 *
 * The distribution is a **pyramid centred on one day, not a spike at it**. Which means any
 * agent comparing "my revenue now" against "my revenue last turn" — the DMR crawl of every
 * Firm in group D, for instance — is quietly dividing by a denominator that varies by a
 * factor of two, and does not know it.
 *
 * **The fix the thesis uses** is to permute People and Firms in separate bins, so that all
 * the People act, then all the Firms. That does not remove the spread within each bin, but it
 * stops a Firm's observation window from straddling an unknown number of Person actions,
 * which is the part that actually distorts the feedback.
 *
 * This is one of three "decision types" the thesis distinguishes (§4.5.2) — instant, feedback,
 * and reflex — and the reason the distinction is worth making. A *feedback* decision is the
 * one this bites; a *reflex*, like the transmission belt's price response, fires on the event
 * itself and never asks how long ago anything happened.
 */

import { POINT, defineModel, parameter } from '../config.js';
import { ces, smoothEoS } from '../econ.js';

const AGENTS = 10000;
const DAYS = 1000;
const BINS = 60;

/** Each agent's position in yesterday's order, or -1 on the first day. @type {number[]} */
let previous = [];
/** Counts of gaps, binned over [0, 2] days. @type {number[]} */
let histogram = [];
let sampled = 0;
let sumOfGaps = 0;

/** Permute People and Firms in separate bins, as the thesis' fix does. */
let separateBins = false;

function reset() {
  previous = new Array(AGENTS).fill(-1);
  histogram = new Array(BINS).fill(0);
  sampled = 0;
  sumOfGaps = 0;
}

/**
 * One day's ordering, and the gaps it implies.
 *
 * The gap between an agent's action on consecutive days, counted in turns, is
 * `(N − i) + j` — the turns left after them yesterday, plus the turns before them today.
 * Divided by N that is a number of days, averaging 1 and ranging over (0, 2).
 *
 * @param {import('../world.js').World} world
 */
function day(world) {
  const order = new Array(AGENTS);
  for (let i = 0; i < AGENTS; i++) order[i] = i;
  // Fisher–Yates on the world's own generator, so reroll reshuffles this model too.
  for (let i = AGENTS - 1; i > 0; i--) {
    const j = Math.floor(world.rng.nextDouble() * (i + 1));
    const t = order[i]; order[i] = order[j]; order[j] = t;
  }

  // The thesis' fix: two bins, each internally permuted. Modelled here by halving the
  // population into two groups and letting each group's positions run only within its half —
  // which is what separate bins mean for the gap an agent can see.
  const span = separateBins ? AGENTS / 2 : AGENTS;

  for (let position = 0; position < AGENTS; position++) {
    const agent = order[position];
    const localPosition = separateBins ? position % span : position;
    if (previous[agent] >= 0) {
      const gapInTurns = (span - previous[agent]) + localPosition;
      const gapInDays = gapInTurns / span;
      sumOfGaps += gapInDays;
      sampled++;
      const bin = Math.min(BINS - 1, Math.max(0, Math.floor((gapInDays / 2) * BINS)));
      histogram[bin]++;
    }
    previous[agent] = localPosition;
  }
}

/** The binned gaps, for the view. Bin centres are in days. */
export function gapHistogram() {
  return histogram;
}

/** Bin centre, in days, for bin `i`. @param {number} i */
export function binCentre(i) {
  return ((i + 0.5) / BINS) * 2;
}

/** @type {import('../scenario.js').Scenario} */
export const f2 = {
  id: 'f2',
  title: 'The permutation gap',
  thesisRef: '§4.7.4',
  defaultDays: DAYS,

  notes: `${AGENTS.toLocaleString('en-GB')} agents, ${DAYS} days, the acting order reshuffled
every day. The histogram is the gap between one agent's consecutive actions, measured in days.

It is a PYRAMID CENTRED ON ONE DAY, not a spike at it. The mean is exactly 1 -- the dashed
line -- and it is almost never 1 for anybody in particular. Go last today and first tomorrow
and barely any time has passed; go first today and last tomorrow and nearly two days have.

Reshuffling the order is the right thing to do: without it, whoever goes first has a
permanent advantage. This is the price of doing it.

Why it matters. Any agent comparing "my revenue now" with "my revenue last turn" is dividing
by a denominator that varies by a factor of two and does not know it. That is exactly what
every Firm in group D does -- move, wait a turn, ask whether revenue rose. It is a real
source of the noise those Firms are misreading.

The fix, and the checkbox: permute People and Firms in SEPARATE BINS, so all the People act
and then all the Firms. Turn it on and watch the pyramid narrow. It does not remove the
spread, because there is still an order within each bin; what it removes is a Firm's
observation window straddling an unknown number of Person actions, which is the part that
distorts the feedback.

This is why the thesis distinguishes three decision types (section 4.5.2). A FEEDBACK
decision is the one this bites. A REFLEX -- the transmission belt's price response, which
fires on the sale itself -- never asks how long ago anything happened, and is immune.`,

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
    day(world);
  },

  record(r) {
    r.global('sampled', () => sampled);
    r.global('meanGap', () => (sampled === 0 ? 0 : sumOfGaps / sampled));
    r.global('separateBins', () => (separateBins ? 1 : 0));
    // The share of agents whose gap was within a tenth of a day of the mean. If the
    // distribution were the spike everyone assumes, this would be 1.
    r.global('withinTenth', () => {
      if (sampled === 0) return 0;
      let inside = 0;
      for (let i = 0; i < BINS; i++) {
        if (Math.abs(binCentre(i) - 1) <= 0.1) inside += histogram[i];
      }
      return inside / sampled;
    });
  },

  controls: () => [
    parameter({
      name: 'bins', label: 'separate People/Firm bins', kind: 'boolean',
      get: () => (separateBins ? 1 : 0),
      set: (v) => { separateBins = v >= 0.5; reset(); },
    }),
  ],
};
