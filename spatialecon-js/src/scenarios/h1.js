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
 * twice Firm 0's wage; the page draws Firm 1 black and Firm 0 white, and calls them that
 * (`FIRM_NAMES`). The People and the Firms want different things, and that is the model:
 *
 * - **Both workforces are better off with the two settlements together**, the better-paid
 *   most of all. Love of variety, as in C7: both goods are then close and cheap to ship. It
 *   holds although meeting crowds the better-paid — their density cost rises, by less than
 *   variety repays.
 * - **The Firms are fighting over the better-paid's custom.** The better-paid buy about half
 *   their goods from each Firm when the two are together, and seven in ten from Firm 1 when
 *   apart. So Firm 0's crawl finds its revenue rising as it closes in, and Firm 1's as it gets
 *   away. Each workforce follows its Firm, being tied to it for work.
 * - **The rhythm is D1's attribution problem.** A Firm reads any change in its revenue as the
 *   verdict on its own last step. While Firm 0 closes in, Firm 1's revenue falls whatever
 *   Firm 1 does, so its crawl undoes each step and it stays put. Once Firm 0 has arrived and
 *   stops gaining, a step away finally pays, and Firm 1's crawl accelerates away for as long
 *   as it keeps paying. Then it is Firm 0 whose revenue falls whatever it does, until the
 *   breakaway runs out. Nothing settles.
 *
 * Meeting sorts as C6 does: the better-paid take the core and the others wrap round them.
 *
 * The defaults are the best fit found to the video: the share of time apart and engulfed, the
 * direction and strength of the chase, and the cloud scale, at about one model day per video
 * frame. It cycles about half as often as the video does, which is the part not reproduced.
 */

import { firmLocationCrawl, personBundle } from '../behaviour.js';
import { PLANE, defineModel, parameter } from '../config.js';
import { ces, smoothEoS } from '../econ.js';
import { MEAN_SD } from '../record.js';
import { distance, randomly, twoPointsApart } from '../space.js';

/** The video. */
export const VIDEO = 'https://www.youtube.com/watch?v=bb2Q5iXxOV4';

/**
 * Separation above which the two settlements are clearly apart, and below which one is
 * wrapped round the other. The thresholds the video's frames were scored at
 * (GREEN_BLOB_RED_BLOB.md §1), so a run here and the frames there are counted the same way.
 */
export const APART = 1.5;
export const ENGULFED = 0.35;

/**
 * What the page calls the two Firms, by index, and so how it draws them: Firm 0 as the
 * original's white plus glyph, Firm 1 — the better-paid workforce's employer — as its filled
 * one. A reader cannot see an index, so every chart, label and caption uses these instead,
 * and the plane and the strips take them from here so the three cannot disagree.
 */
export const FIRM_NAMES = /** @type {const} */ (['white', 'black']);

/** What the tie dial sets, and what it calls each setting. */
const TIES = /** @type {const} */ (['work', 'goods', 'both']);
const TIE_LABELS = ['work only (the reconstruction)', 'goods only (the runner-up)', 'both (as C6)'];

/**
 * The People tied to Firm `g`, in a fixed order — a chart drawing one line per Person needs
 * the same Person in the same place every day.
 * @param {import('../world.js').World} w @param {number} g
 */
export function workforce(w, g) {
  const firm = w.firm(g);
  return w.people.filter((p) => p.assignedFirm === firm);
}

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
  const groups = w.firms.map((_, g) => {
    const members = workforce(w, g);
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

/**
 * What each workforce bought from each Firm on the day just run, at the Firm's base price:
 * `sales(w)[f][g]` is Firm f's takings from workforce g.
 *
 * Booked as `buy` books revenue — quantity times base price, the delivery cost melting away
 * as iceberg — so each row sums to the revenue the Firm's crawl compares against yesterday's.
 * A Person with nowhere affordable bought nothing, and their record of the last purchase is a
 * previous day's, so they are skipped.
 *
 * @param {import('../world.js').World} w
 */
export function sales(w) {
  const out = w.firms.map(() => w.firms.map(() => 0));
  for (const p of w.people) {
    if (!p.assignedFirm || p.utility <= 0) continue;
    const g = w.firms.indexOf(p.assignedFirm);
    p.lastSellers.forEach((firm, i) => {
      const q = p.lastQuantities[i] ?? 0;
      if (q > 0) out[w.firms.indexOf(firm)][g] += q * firm.goodCost;
    });
  }
  return out;
}

/** How many days a Firm's speed is measured over. */
const SPEED_DAYS = 5;

/**
 * Each run's recent Firm positions, oldest first: where every Firm stood at the start of each
 * of the last few days. Kept by the day-start hook `setUp` installs.
 * @type {WeakMap<import('../world.js').World, import('../space.js').Pt[][]>}
 */
const trails = new WeakMap();

/**
 * How fast Firm `g` has moved over the last five days: net distance per day.
 *
 * Net is the point. The crawl moves its Firm every day, and when a step fails it puts the
 * Firm back and tries another direction — so a Firm whose revenue keeps falling steps out and
 * back on the spot, and its net movement is close to nothing. A Firm on a run of successes
 * accelerates, the crawl adding a little to the step each day it pays, and its speed climbs
 * with it. Measured from the run's own record rather than a chart's, because the page samples
 * its charts once a frame, several days apart at speed, and the CSV must read what the chart
 * reads.
 *
 * @param {import('../world.js').World} w @param {number} g
 */
export function firmSpeed(w, g) {
  const trail = trails.get(w);
  if (!trail || trail.length === 0) return 0;
  return distance(w.space, trail[0][g], w.firm(g).position) / trail.length;
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
from both Firms. Squares work for the white Firm at wage 1; circles for the black Firm at
wage 2. Both Firms crawl for revenue, as in D1.

Watch the circles: they are the video's red blob, and the squares are its green one. The
squares' Firm courts the circles -- they are the richer customers -- and its commuters follow
it in. When the two meet, the circles take the centre and the squares wrap round them, which
is C6's sorting. Then the circles' Firm breaks away, and they go with it.

The strips below say why. Grey bands are the spells apart; the white Firm's lines are white,
edged in black.
  utility     each Person faint, each workforce's mean bold, on its own scale. Both are
              lower apart than together, the circles' most: the People would rather stay
              together, and the better-paid most of all.
  revenue     the only thing either Firm can see. The white Firm's is higher together, the
              black Firm's apart. Dashed, what the circles bought at each: the custom the
              two are fighting over. The rest of each Firm's revenue is the squares'.
  speed       how fast each Firm is moving. The black Firm sits still while the white closes
              in -- its revenue is falling whatever it does, so its crawl undoes every step
              -- then bolts once the white Firm stops gaining.
  separation  how far apart the settlements are, in units of their own size, on a log scale:
              1 is touching, well below 1 is one wrapped round the other.

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

  /**
   * Round-robin, as `ActorMaker` did and as C6 does: half to each Firm. And the record of
   * where the Firms have been, for `firmSpeed`. A day-start hook draws no random numbers, so
   * keeping it changes nothing about the run.
   */
  setUp(w) {
    const n = w.firms.length;
    w.people.forEach((p, i) => { p.assignedFirm = w.firm(i % n); });

    /** @type {import('../space.js').Pt[][]} */
    const trail = [];
    trails.set(w, trail);
    w.timeline.onDayStart(() => {
      trail.push(w.firms.map((f) => ({ x: f.position.x, y: f.position.y })));
      if (trail.length > SPEED_DAYS) trail.shift();
    });
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
      // What each Firm acts on, and why it moves: the day's revenue as its crawl banked it,
      // who that revenue came from, and how fast the crawl has been carrying it.
      r.global(`revenue_firm${g}`, () => w.firm(g).revenueLastTurn);
      for (let k = 0; k < w.firms.length; k++) {
        r.global(`sales_firm${g}_group${k}`, () => sales(w)[g][k]);
      }
      r.global(`speed_firm${g}`, () => firmSpeed(w, g));
    }
    r.aggregate('utility', w.people, (p) => p.utility, MEAN_SD);
    r.global('rho', () => /** @type {any} */ (w.config.utility).rho);
    r.perActor('group', w.people, (p) => (p.assignedFirm ? p.assignedFirm.id : -1));
    r.perActor('personX', w.people, (p) => p.position.x);
    r.perActor('personY', w.people, (p) => p.position.y);
    r.perActor('personUtility', w.people, (p) => p.utility);
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
        name: 'wageGap', label: 'wage gap (black Firm / white)', min: 1, max: 3,
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
