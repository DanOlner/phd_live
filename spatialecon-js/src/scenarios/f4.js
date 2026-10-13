// @ts-check

/**
 * **F4 — bundle sampling, stepped.** Thesis §4.6.1.
 *
 * One Person, followed. Every candidate location they considered this turn, what each was
 * worth, which they took, and where that basket comes from. Step a day and watch them
 * resample.
 *
 * This is the core loop of the entire framework made visible, and it is the thing every
 * other model in the catalogue is built out of. C1's settlement, C6's rings, D1's Hotelling
 * split and E8's migration are all *this*, run five hundred times a day for two thousand
 * days. It has no headless form — there is no number to record, the whole content is the
 * picture — which is why the original never built it and why it is the obvious candidate for
 * the site's first interactive.
 *
 * What it settles, quickly and without argument:
 *
 * - **Nobody optimises over space.** Twenty-one candidates a turn out of a continuous plane,
 *   ten of them within a short radius. The equilibrium in C1 is reached by agents that never
 *   see more than a handful of alternatives at a time.
 * - **The externality is the mechanism.** The density term in every Bundle is computed from
 *   where everyone else currently stands. Nobody compares themselves to anybody; they just
 *   find everywhere else slightly worse or better because of where others went.
 * - **A rejected Bundle is not a worse deal, it is often no deal.** Where the density cost
 *   exceeds the wage the Bundle is discarded rather than scored negative, and that single
 *   rule is what produces C8's dispersal (§5.4.7).
 *
 * Small on purpose: forty People, so the sampled Bundles are individually legible. The
 * mechanism is identical at five hundred, where it is not.
 */

import { personBundle } from '../behaviour.js';
import { PLANE, defineModel } from '../config.js';
import { ces, smoothEoS } from '../econ.js';
import { pt, randomly } from '../space.js';

/** Whose decision is drawn. Person 0 has no special properties; it is just a choice. */
export const SUBJECT_ID = 0;

/**
 * The subject's most recent decision, republished each turn for the view.
 *
 * Module state rather than something hung off the Person, for the reason in `behaviour.js`:
 * Bundles are local to a decision everywhere else in the framework and should stay that way.
 * `from` is where the Person was standing when they decided. It has to be captured here
 * rather than read off the Person at draw time, because by then the move has already
 * happened and `person.position` *is* the chosen Bundle's location — so the picture would
 * show the choice and the outcome as the same mark and lose the arrow between them.
 *
 * @type {{from: import('../space.js').Pt, candidates: import('../agents.js').Bundle[], best: import('../agents.js').Bundle | null}}
 */
export const lastDecision = { from: { x: 0, y: 0 }, candidates: [], best: null };

/** @type {import('../scenario.js').Scenario} */
export const f4 = {
  id: 'f4',
  title: 'Bundle sampling, stepped',
  thesisRef: '§4.6.1',
  defaultDays: 60,

  notes: `Forty People, three Firms, one Person followed. Every Bundle they sampled this turn
is drawn at the location it refers to, sized and shaded by the utility it would yield; the
one they took is ringed, and lines run from it to each Firm it sources goods from with
thickness proportional to spend.

Use STEP rather than RUN. One day at a time is the point.

Watch three things.

Ten of the candidates are drawn within a short radius and ten from anywhere in the space, so
most turns the choice is between small local adjustments -- and the Person still ends up
somewhere sensible. Nobody searches the whole plane.

Bundles marked with a cross were not scored at all. Their density cost exceeded the wage, so
there was no budget left to buy anything with, and a Bundle with no budget is discarded
rather than given a negative score. Turn the density cost up and watch crosses spread inward
from the crowded middle: that single rule is the whole of C8's dispersal.

Every Bundle's density term is computed from where everyone else is standing right now. Step
a few days and the same location will be worth something different, because other People
moved. Nobody here compares themselves to anybody else -- that is the entire coordinating
mechanism.

One more thing the picture gives away for free. The affordable Bundles usually fall into a
large group that are all worth within a few percent of each other, and a handful that are
much worse. So the decision is nearly always a choice between near-equivalents, and the
winner is decided by a small margin. That is worth knowing before reading chapter 5: it is
why the spatial equilibrium is loose rather than sharp, and why a little demand noise is
enough to make a Firm misread it.`,

  config: () =>
    defineModel({
      spaceKind: PLANE, width: 1.0,
      peopleCount: 40, firmCount: 3,
      placePeople: randomly(),
      // Three fixed Firms in a triangle, well inside the space, so every sampled Bundle has
      // a genuinely different mix of delivered prices rather than the same one twice.
      placeFirms: (firms) => {
        const spots = [pt(0.25, 0.3), pt(0.75, 0.3), pt(0.5, 0.75)];
        firms.forEach((f, i) => f.placeAt(spots[i % spots.length]));
      },
      utility: ces(0.4),
      production: smoothEoS(),
      baseGoodCost: 1.0,
      wage: 1.0,
      deliveryCost: 0.25,
      commuteCost: 0.1,
      // Tuned so the picture teaches. At radius 1/8 the density circles blanket the whole
      // space, everyone pays everywhere, and the settlement flees to the boundary where there
      // are fewer neighbours -- true, and C8's mechanism, but it drags the subject off the
      // edge of the frame. At 1/12 with a cost of 60, about a fifth of the sampled Bundles
      // come back unaffordable while the population stays interior and legible.
      densityCost: 60,
      densityRadius: 1 / 12,
      peopleMobile: true,
      firmsMobile: false,
      personBehaviour: personBundle({
        watch(person, candidates, best) {
          if (person.id !== SUBJECT_ID) return;
          lastDecision.from = person.position;
          lastDecision.candidates = candidates;
          lastDecision.best = best;
        },
      }),
    }),

  setUp() {
    lastDecision.from = { x: 0, y: 0 };
    lastDecision.candidates = [];
    lastDecision.best = null;
  },

  record(r, w) {
    r.global('sampled', () => lastDecision.candidates.length);
    r.global('viable', () => lastDecision.candidates.filter((b) => b.isViable()).length);
    r.global('chosenUtility', () => lastDecision.best?.utility ?? 0);
    r.global('chosenDensity', () => lastDecision.best?.densityCost ?? 0);
    r.global('moved', () => w.person(SUBJECT_ID).distanceMovedLastTurn);
  },

  controls: (w) => [w.densityCostParam(), w.deliveryCostParam()],
};
