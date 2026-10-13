// @ts-check

/**
 * **H1 — green blob, red blob.** Not in the thesis. A run recorded as a video on 13 August
 * 2011, while the framework was being built, and uploaded as "Green blob red blob" with the
 * note *"Green blob likes red blob. A lot. In a slightly smothering way. Red blob is far from
 * sure about this."* The code that ran it is gone: the wiring was hand-edited in the usual way
 * and later overwritten. This is a reconstruction from the video's frames, the 2011 drawing
 * code that survives in the snapshot, and figures from the same fortnight, tested by scoring
 * runs with the statistics measured on the video. GREEN_BLOB_RED_BLOB.md has the case.
 *
 * Two Firms that grope for revenue, as in D1. Each has its own workforce, tied to it **for
 * work only** — C6's von Thünen tie applied to the labour channel and not to goods — so
 * everyone buys from both Firms and pays a density cost to live anywhere crowded. Firm 1 pays
 * twice Firm 0's wage. Three forces, each from an earlier model:
 *
 * - **Love of variety pulls the settlements together** (C7). Everyone wants some of both
 *   goods, and goods cost something to ship. The density cost pushes them apart (C1). ρ sets
 *   the balance.
 * - **The wage gap makes the pull one-sided.** The better-paid are the better customers, so
 *   Firm 0's crawl finds its revenue rising as it moves towards them, and its commuters follow
 *   it. That is the chase.
 * - **Meeting is C6.** Both workforces want the same central space and sort exactly as the
 *   von Thünen rings do: the better-paid take the core and the others wrap round them. Their
 *   crowding raises the better-paid's density cost, Firm 1's revenue falls, and its crawl
 *   breaks away. Nothing settles.
 *
 * The defaults are the best fit found to the video: the share of time apart and engulfed, the
 * direction and strength of the chase, and the cloud scale, at about one model day per video
 * frame. It cycles about half as often as the video does, which is the part not reproduced.
 */

import { firmLocationCrawl, personBundle } from '../behaviour.js';
import { PLANE, defineModel, parameter } from '../config.js';
import { ces, smoothEoS } from '../econ.js';
import { MEAN_SD } from '../record.js';
import { randomly, twoPointsApart } from '../space.js';

/** The video. */
export const VIDEO = 'https://www.youtube.com/watch?v=bb2Q5iXxOV4';

/** What the tie dial sets, and what it calls each setting. */
const TIES = /** @type {const} */ (['work', 'goods', 'both']);
const TIE_LABELS = ['work only (the reconstruction)', 'goods only (the runner-up)', 'both (as C6)'];

/**
 * The two settlements: each workforce's centre, how spread out it is, and how far apart the
 * two are in units of their own size.
 *
 * `separation` is the distance between the two centres over the sum of the two rms spreads,
 * so 1 is two clouds just touching. Below about 0.35 one is wrapped round the other; above
 * about 1.5 there is clear space between them. It is the measure the video was scored on, so
 * a run here and the frames there can be compared number for number.
 *
 * @param {import('../world.js').World} w
 */
export function settlements(w) {
  const groups = w.firms.map((firm) => {
    const members = w.people.filter((p) => p.assignedFirm === firm);
    const n = Math.max(members.length, 1);
    const x = members.reduce((a, p) => a + p.position.x, 0) / n;
    const y = members.reduce((a, p) => a + p.position.y, 0) / n;
    const spread = Math.sqrt(
      members.reduce((a, p) => a + (p.position.x - x) ** 2 + (p.position.y - y) ** 2, 0) / n);
    const utility = members.reduce((a, p) => a + p.utility, 0) / n;
    return { x, y, spread, utility };
  });
  const [a, b] = groups;
  const gap = Math.hypot(a.x - b.x, a.y - b.y);
  const size = a.spread + b.spread;
  return {
    groups,
    gap,
    separation: size > 0 ? gap / size : 0,
    /** Index of the workforce with the higher mean utility — the video's red blob. */
    leader: a.utility >= b.utility ? 0 : 1,
  };
}

/** @type {import('../scenario.js').Scenario} */
export const h1 = {
  id: 'h1',
  title: 'Green blob, red blob',
  thesisRef: 'a 2011 video',
  // There is no settled state to run to, so this is chosen for the catalogue still: the
  // default seed's frame on this day is a mid-field engulfment, the video's "smothering".
  // Every walkthrough caption has fired by about day 550.
  defaultDays: 2555,

  notes: `Not in the thesis: a run recorded as a video in August 2011 and reconstructed here.
${VIDEO}

160 People in two workforces of 80, each tied to its own Firm for WORK only. Everyone buys
from both Firms. Squares work for Firm 0 at wage 1; circles for Firm 1 at wage 2. Both Firms
crawl for revenue, as in D1.

Watch the circles: they are the video's red blob, and the squares are its green one. The
squares' Firm courts the circles -- they are the richer customers -- and its commuters follow
it in. When the two meet, the circles take the centre and the squares wrap round them, which
is C6's sorting. That crowding cuts the circles' budgets and their Firm's revenue, and its
crawl breaks away. Then it starts again.

The lower panel is how far apart the two settlements are, in units of their own size: 1 is
touching, well below 1 is one wrapped round the other.

The pair wanders, and the space has walls, as the original's did. Sooner or later a breakaway
runs into one, and from then on the chase works along the edge. The video is 33 seconds from
the middle of the field, drifting at the same rate as this model does. Press reset to see the
mid-field chase again.

The dials:
  rho        the glue. Below about 0.55 they merge for long spells; above about 0.65 they
             part for good. The video sits between.
  wage gap   1 removes the asymmetry, and the chase with it.
  tie        'goods only' is the runner-up reconstruction: a harder chase that never parts
             or engulfs. 'both' is C6's tie, and the two repel.
  Firms move off, and the settlements stop where they are.

Set 'shade People by' to utility to see what the video's colours encoded: the same lowest-to-
highest ramp across the whole population, in grey. The '2011 ring' overlay is the white
circle from the video. It is not an agent: it is the mean position of every actor.`,

  config: () =>
    defineModel({
      spaceKind: PLANE, width: 2.0,
      peopleCount: 160, firmCount: 2,
      placePeople: randomly(),
      placeFirms: twoPointsApart(0.3),
      // Built per run rather than shared, so reset and reroll put rho back with everything
      // else. A module-level utility would keep a visitor's rho across a reset while the
      // other dials returned to their defaults.
      utility: ces(0.6),
      production: smoothEoS(),
      baseGoodCost: 1.0,
      deliveryCost: 0.75,
      commuteCost: 1.0,
      // Base 1, ranged: Firm 0 pays 1, Firm 1 pays 1 + rangeStep = 2. The wage-gap dial
      // moves the step.
      wage: 1.0,
      rangeWage: true,
      // Width 2 and radius 1/16, the density models' convention: the video's density radius
      // is 1/32 of the window. A density cost of 5 matches its cloud scale.
      densityCost: 5,
      densityRadius: 1 / 16,
      tie: 'work',
      personBehaviour: personBundle(),
      firmBehaviour: firmLocationCrawl(),
      peopleMobile: true,
      firmsMobile: true,
    }),

  /** Round-robin, as `ActorMaker` did and as C6 does: half to each Firm. */
  setUp(w) {
    const n = w.firms.length;
    w.people.forEach((p, i) => { p.assignedFirm = w.firm(i % n); });
  },

  record(r, w) {
    r.global('separation', () => settlements(w).separation);
    r.global('gap', () => settlements(w).gap);
    r.global('leader', () => settlements(w).leader);
    r.global('firmGap', () => w.distance(w.firm(0), w.firm(1)));
    for (let g = 0; g < w.firms.length; g++) {
      r.global(`spread_group${g}`, () => settlements(w).groups[g].spread);
      r.global(`utility_group${g}`, () => settlements(w).groups[g].utility);
      r.global(`firm${g}_x`, () => w.firm(g).position.x);
      r.global(`firm${g}_y`, () => w.firm(g).position.y);
    }
    r.aggregate('utility', w.people, (p) => p.utility, MEAN_SD);
    r.global('rho', () => /** @type {any} */ (w.config.utility).rho);
    r.perActor('group', w.people, (p) => (p.assignedFirm ? p.assignedFirm.id : -1));
    r.perActor('personX', w.people, (p) => p.position.x);
    r.perActor('personY', w.people, (p) => p.position.y);
  },

  // The first two are on the drag, left and right. rho is capped at 0.99, as everywhere in the
  // original: rho = 1 takes the substitutes branch, which the 2013 code got wrong and never
  // ran (CODE_ANATOMY §7.1).
  controls: (w) => {
    const u = /** @type {any} */ (w.config.utility);
    return [
      parameter({
        name: 'rho', label: 'rho (1 = substitutes)', min: 0.05, max: 0.99,
        get: () => u.rho, set: (v) => { u.rho = v; },
      }),
      parameter({
        name: 'wageGap', label: 'wage gap (Firm 1 / Firm 0)', min: 1, max: 3,
        get: () => 1 + w.config.rangeStep, set: (v) => w.setRangeStep(v - 1),
      }),
      parameter({
        name: 'density', label: 'density cost', min: 0, max: 20,
        get: () => w.config.densityCost, set: (v) => { w.config.densityCost = v; },
      }),
      parameter({
        name: 'deliv', label: 'distance cost', min: 0, max: 3,
        get: () => w.config.deliveryCost, set: (v) => w.setDeliveryCost(v),
      }),
      parameter({
        name: 'tie', label: 'each workforce tied to its Firm for', kind: 'choice',
        choices: TIE_LABELS, min: 0, max: TIES.length - 1,
        get: () => TIES.indexOf(w.config.tie),
        set: (v) => { w.config.tie = TIES[Math.round(v)]; },
      }),
      parameter({
        name: 'firmsMove', label: 'Firms move', kind: 'boolean', min: 0, max: 1,
        get: () => (w.config.firmsMobile ? 1 : 0),
        set: (v) => { w.config.firmsMobile = v >= 0.5; },
      }),
    ];
  },
};
