// @ts-check

/**
 * What agents do on their turn.
 *
 * Two Person behaviours and two Firm behaviours cover every model in the thesis — down from
 * the original's ten `FirmAct*` classes, of which only one was ever wired in (CODE_ANATOMY
 * §7.10). The other nine were abandoned attempts, several at marginal-cost/marginal-revenue
 * Firms, and are interesting only as evidence for the thesis' argument about what did not
 * work.
 *
 * The transmission belt's Person and Firm behaviours arrive in stage 4.
 */

import { Bundle, TIME_PER_DAY } from './agents.js';
import { constrained } from './econ.js';
import { LINE, PLANE, TWO_REGION, confine, regionOneX, regionZeroX } from './space.js';

/**
 * @typedef {object} Behaviour
 * @property {(actor: any, world: import('./world.js').World) => void} act
 * @property {(firm: any, units: number, input: number, world: import('./world.js').World) => void} [onSale]
 * @property {() => string} describe
 */

/** Does nothing. Lets a scenario hold agents fixed. @type {Behaviour} */
export const NONE = {
  act() {},
  describe() {
    return 'inactive';
  },
};

// --- Person: the Bundle cycle --------------------------------------------------------

/**
 * The Bundle cycle: how a Person acts in the partial models (§4.6.1).
 *
 * Sample some candidate locations, work out what each is worth, move to the best. Repeat
 * until nobody can improve by moving — a Nash equilibrium, reached with no market, no
 * auctioneer and no communication between agents.
 *
 * The order of operations matters and is not obvious:
 *
 * 1. Find the best wage reachable from the candidate spot, net of commuting.
 * 2. Subtract the density cost of living there. This shrinks the *budget*, so density is
 *    paid before anything is bought.
 * 3. Price every good delivered to that spot.
 * 4. Optimise, and score the location by the utility that yields.
 *
 * A candidate whose density cost exceeds its wage is discarded rather than scored negative —
 * the mechanism behind the dispersal of §5.4.7, and the reason C1's utility is not perfectly
 * flat (CODE_ANATOMY §5.3).
 *
 * Note what is absent: no Person ever compares themselves to another, or knows what another
 * is getting. The spatial equilibrium of §5.4.1 emerges purely from each acting on their own
 * choice set, with every move changing the choice set of whoever decides next. The
 * externality *is* the mechanism.
 *
 * @param {object} [options]
 * @param {number} [options.nearbySamples] @param {number} [options.nearbyRadius]
 * @param {number} [options.wideSamples] @param {import('./econ.js').Optimiser} [options.optimiser]
 * @param {(person: import('./agents.js').Person, candidates: Bundle[], best: Bundle | null) => void} [options.watch]
 *   Called once per Person per turn with everything they considered, after scoring and
 *   before moving. The Bundles are otherwise discarded — they are local to the decision —
 *   and F4 exists to *show* them, so something has to hand them out. A read-only hook rather
 *   than retained state on the Person: with 500 People and 21 Bundles each, keeping them all
 *   alive to serve one visualisation would be an odd tax on every other model.
 * @returns {Behaviour}
 */
export function personBundle({
  nearbySamples = 10,
  nearbyRadius = 0.2,
  wideSamples = 10,
  optimiser = constrained(),
  watch,
} = {}) {
  return {
    act(person, world) {
      const candidates = sample(person, world, nearbySamples, nearbyRadius, wideSamples);

      /** @type {Bundle | null} */
      let best = null;
      for (const b of candidates) {
        evaluate(b, person, world, optimiser);
        if (!b.isViable()) continue;
        if (best === null || b.utility > best.utility) best = b;
      }

      watch?.(person, candidates, best);

      if (best === null) {
        // Nowhere affordable. Stay put and record the failure honestly as zero utility.
        person.utility = 0;
        person.densityCost = 0;
        return;
      }

      person.utility = best.utility;
      person.densityCost = best.densityCost;
      person.neighboursInDensityRadius = best.neighbours;
      person.lastSellers = best.sellers;
      person.lastQuantities = best.quantities;

      buy(best);

      // Through the World, not `person.moveTo`, so the density index stays correct.
      if (world.config.peopleMobile) world.movePerson(person, best.location);
    },

    describe() {
      return `bundle sampling (${nearbySamples} near r=${nearbyRadius}, ${wideSamples} wide, ${optimiser.describe()})`;
    },
  };
}

/**
 * Actually make the purchases and take the job.
 *
 * A Firm's revenue is quantity times its *base* price: the distance cost is "iceberg", paid
 * by the buyer and melting away rather than arriving as income. So moving closer to buyers
 * raises a Firm's revenue only because they can then afford more — which is the entire
 * signal `firmLocationCrawl` has to work from.
 *
 * @param {Bundle} bundle
 */
function buy(bundle) {
  for (let i = 0; i < bundle.sellers.length; i++) {
    const quantity = i < bundle.quantities.length ? bundle.quantities[i] : 0;
    if (quantity <= 0) continue;
    const seller = bundle.sellers[i];
    seller.revenue += quantity * seller.goodCost;
  }
  if (bundle.employer) bundle.employer.contributedTime += bundle.workTime;
}

/**
 * Candidate locations: where I am, a few nearby, a few anywhere.
 * @param {import('./agents.js').Person} person @param {import('./world.js').World} world
 * @param {number} nearbySamples @param {number} nearbyRadius @param {number} wideSamples
 */
function sample(person, world, nearbySamples, nearbyRadius, wideSamples) {
  /** @type {Bundle[]} */
  const out = [];
  const space = world.space;
  const here = person.position;

  // Evaluated first, and the comparison below is a strict `>`, so ties keep the incumbent.
  // The original behaves the same way; there is no deadband beyond that.
  out.push(new Bundle({ x: 0, y: 0 }, here));

  if (!world.config.peopleMobile) return out;

  // Two discrete regions: the only alternative is the other region, so offer that rather
  // than sampling coordinates that would land nowhere.
  if (space.kind === TWO_REGION) {
    const otherX = here.x === regionZeroX(space) ? regionOneX(space) : regionZeroX(space);
    out.push(new Bundle({ x: otherX - here.x, y: 0 }, { x: otherX, y: 0 }));
    return out;
  }

  for (let i = 0; i < nearbySamples; i++) {
    const offset = space.kind === LINE
      ? { x: world.rng.between(-nearbyRadius, nearbyRadius), y: 0 }
      : world.rng.pointInCircle(nearbyRadius);
    out.push(new Bundle(offset, confine(space, { x: here.x + offset.x, y: here.y + offset.y })));
  }

  for (let i = 0; i < wideSamples; i++) {
    const anywhere = space.kind === LINE
      ? { x: world.rng.upTo(space.width), y: 0 }
      : { x: world.rng.upTo(space.width), y: world.rng.upTo(space.width) };
    const c = confine(space, anywhere);
    out.push(new Bundle({ x: c.x - here.x, y: c.y - here.y }, c));
  }

  return out;
}

/**
 * Price everything from this candidate spot and find the utility it supports.
 * @param {Bundle} bundle @param {import('./agents.js').Person} person
 * @param {import('./world.js').World} world @param {import('./econ.js').Optimiser} optimiser
 */
function evaluate(bundle, person, world, optimiser) {
  const config = world.config;

  findBestWage(bundle, person, world);

  // One pass for both: they scan the same neighbours over the same radius.
  const density = world.densityAndNeighboursAt(bundle.location, person);
  bundle.rawDensityCost = config.densityCost > 0 ? density.raw : 0;
  bundle.densityCost = config.densityCost * bundle.rawDensityCost;
  bundle.neighbours = density.neighbours;
  bundle.budget -= bundle.densityCost;

  if (bundle.budget <= 0) return; // unaffordable; leave it non-viable

  for (const firm of world.firms) {
    // In the von Thünen scenarios a Person may only buy from their assigned Firm — unless
    // the tie covers the job alone, as in H1, where everyone buys from everyone.
    if (person.assignedFirm && config.tie !== 'work' && firm !== person.assignedFirm) continue;
    bundle.addSeller(firm, firm.priceAtDistance(world.distance(bundle.location, firm)));
  }
  if (!bundle.hasSellers()) return;

  const utility = config.utility;
  bundle.quantities = optimiser.optimise(bundle.prices, bundle.budget, utility, world.rng);

  // Utility is scored on the CLEAN optimum, before any noise is applied. The original did
  // the same, and it is the right way round: the noise represents imprecision in carrying
  // out the purchase, not in knowing what you wanted. It also matters for what follows —
  // People's utility stays clean while the demand Firms observe does not, which is exactly
  // the asymmetry model d2 is about.
  bundle.utility = utility.utilityOf(bundle.quantities);

  applyDemandNoise(bundle, world);
}

/**
 * Perturb the quantities actually bought (§5.5.2).
 *
 * Noise arises naturally when the hill-climber is used — it stops short of the exact optimum
 * and wobbles. Running it for every Person on every turn is expensive, so the thesis adds an
 * explicit noise term to the constrained optimum instead, to get the same effect cheaply.
 *
 * Amplitude is a Quirks choice: the original perturbed by ±noise/2 of each quantity, where
 * the thesis text describes ±noise.
 *
 * @param {Bundle} bundle @param {import('./world.js').World} world
 */
function applyDemandNoise(bundle, world) {
  const noise = world.config.demandNoise;
  if (noise <= 0) return;
  const amplitude = world.config.quirks.noiseIsFullAmplitude ? 2 * noise : noise;
  for (let i = 0; i < bundle.quantities.length; i++) {
    bundle.quantities[i] += bundle.quantities[i] * world.rng.jiggle(amplitude);
  }
}

/**
 * Best wage reachable from this spot: wage × timeLeftAfterCommuting, maximised over Firms.
 *
 * The round-trip factor is a Quirks choice: the thesis states l = t − 2·t_commute, the
 * original code charged it one way. A Firm more than a full day away (or half a day, with
 * the round trip) leaves no time to work and is simply unreachable.
 *
 * @param {Bundle} bundle @param {import('./agents.js').Person} person
 * @param {import('./world.js').World} world
 */
function findBestWage(bundle, person, world) {
  const config = world.config;
  const roundTrip = config.quirks.commuteIsRoundTrip ? 2 : 1;

  bundle.employer = null;
  bundle.budget = 0;
  bundle.workTime = 0;

  for (const firm of world.firms) {
    // The labour half of the von Thünen tie, open when the tie covers goods alone.
    if (person.assignedFirm && config.tie !== 'goods' && firm !== person.assignedFirm) continue;

    const d = world.distance(bundle.location, firm);
    const timeLeft = TIME_PER_DAY - roundTrip * d * firm.commuteCost;
    if (timeLeft <= 0) continue;

    const wage = firm.wage * timeLeft;
    if (wage > bundle.budget) {
      bundle.budget = wage;
      bundle.workTime = timeLeft;
      bundle.employer = firm;
    }
  }
}

// --- Firm: the DMR location crawl ----------------------------------------------------

/** The original's `startCrawlRate`. Steps are drawn from ±half this. */
const CRAWL_RATE = 0.025;

/** How much a successful direction accelerates each turn. The original's `adjustStep`. */
const ADJUST_STEP = 0.0025;

/**
 * A Firm gropes towards better revenue by moving and seeing what happens (§5.5, §4.7.7).
 *
 * Perturb position, wait a turn, ask "did my revenue rise?". If yes, keep going the same way
 * and step a little further. If no, go back and start a fresh search in a new random
 * direction. The Firm knows nothing else — not where the People are, not what the other Firm
 * did, not even that another Firm exists.
 *
 * This is the "data, meaning, response" mechanism the thesis takes from Jane Jacobs. The
 * datum is a revenue change; the assumed meaning is "my last action caused this"; the
 * response follows. Alone, a Firm has no trouble — the world is static, so any change really
 * is its own doing. Add one competitor and the meaning becomes unreliable, because revenue
 * now moves for reasons the Firm did not cause and cannot see. That is §5.5.2.
 *
 * **The step size matters more than it looks.** A new search draws a *signed* magnitude
 * uniformly from ±CRAWL_RATE/2, so most steps are well under the maximum and some are nearly
 * zero. It is not a unit direction times a fixed rate — that would make Firms far better
 * searchers than they are, and the whole point is how poorly they do. On a line the sign is
 * the direction; on a plane it is paired with a random angle. Getting this wrong made the
 * Java's DMR games converge in a fraction of the days and quietly flattered the result.
 *
 * Only location is perturbed. The original's `FirmActRespondAverage` contains fully written
 * branches for perturbing wage and price too, permanently unreachable behind a hardcoded
 * `choose = 2`; the thesis notes those "faced problems".
 *
 * @returns {Behaviour}
 */
export function firmLocationCrawl() {
  /** @type {Map<number, {step: number, accelerate: number, angle: number,
   *                      needsNewDirection: boolean, previousPosition: import('./space.js').Pt,
   *                      hasMoved: boolean}>} */
  const searches = new Map();

  return {
    act(firm, world) {
      let search = searches.get(firm.id);
      if (!search) {
        search = {
          step: 0, accelerate: 0, angle: 0,
          needsNewDirection: true, previousPosition: { x: 0, y: 0 }, hasMoved: false,
        };
        searches.set(firm.id, search);
      }

      const revenueChange = firm.revenue - firm.revenueLastTurn;

      if (search.hasMoved) {
        if (revenueChange > 0) {
          search.step += search.accelerate; // worked: same way, a little bolder
        } else {
          firm.placeAt(search.previousPosition); // did not work: undo and rethink
          search.needsNewDirection = true;
        }
      }

      if (world.config.firmsMobile) {
        if (search.needsNewDirection) {
          search.step = world.rng.jiggle(CRAWL_RATE); // uniform in ±CRAWL_RATE/2
          search.accelerate = search.step < 0 ? -ADJUST_STEP : ADJUST_STEP;
          search.angle = world.rng.nextDouble() * 2 * Math.PI;
          search.needsNewDirection = false;
        }
        search.previousPosition = firm.position;
        const offset = world.space.kind === PLANE
          ? { x: search.step * Math.cos(search.angle), y: search.step * Math.sin(search.angle) }
          : { x: search.step, y: 0 };
        firm.moveTo({ x: firm.position.x + offset.x, y: firm.position.y + offset.y }, world.space);
        search.hasMoved = true;
      }

      // Revenue is counted per turn, so bank it and reset.
      firm.revenueLastTurn = firm.revenue;
      firm.revenue = 0;
    },

    describe() {
      return `location crawl (signed step up to ${CRAWL_RATE / 2}, accelerating by ${ADJUST_STEP})`;
    },
  };
}

// --- The transmission belt -----------------------------------------------------------

/**
 * People buy goods directly with time — the demand side of the transmission belt (§6.3).
 *
 * No money, no wage, no separate labour market. A Person's day *is* the budget, and the goods
 * they get for it *are* the wage. The thesis arrives here after finding that treating money
 * as a real, conserved quantity produces runaway inequality dynamics that swamp everything
 * else (§4.7.2), and after establishing that the spatial distinction that matters is between
 * spatial and non-spatial costs rather than between goods and wages.
 *
 * Two mechanisms make economies of scale work without anyone needing a strategy:
 *
 * - **Time is left behind.** A Person's input stays with each Firm until their next turn,
 *   when they withdraw it and decide afresh. So a Firm's total input reflects everyone who
 *   has bought recently, not just whoever is buying now — which is what lets production run
 *   at a scale no individual chose.
 * - **Buying through a CES means buying from everyone.** Every Firm gets some share of every
 *   Person's day, so no Firm is ever starved by a marginal price difference. That sidesteps
 *   Bertrand competition, where the cheapest would take everything.
 *
 * Each purchase triggers the Firm's reflex, so prices move continuously rather than once a
 * turn.
 *
 * @param {object} [options]
 * @param {number} [options.nearbySamples] @param {number} [options.nearbyRadius]
 * @param {number} [options.wideSamples] @param {import('./econ.js').Optimiser} [options.optimiser]
 * @returns {Behaviour}
 */
export function personTransmission({
  nearbySamples = 3,
  nearbyRadius = 0.2,
  wideSamples = 5,
  optimiser = constrained(),
} = {}) {
  /** What each Person currently has invested in each Firm, to withdraw next turn.
   * @type {Map<number, Map<number, number>>} */
  const leftWith = new Map();

  return {
    act(person, world) {
      withdrawPreviousInput(leftWith, person, world);

      /** @type {Bundle | null} */
      let best = null;
      for (const candidate of sample(person, world, nearbySamples, nearbyRadius, wideSamples)) {
        evaluateTransmission(candidate, person, world, optimiser);
        if (!candidate.isViable()) continue;
        if (best === null || candidate.utility > best.utility) best = candidate;
      }
      if (best === null) {
        person.utility = 0;
        return;
      }

      person.utility = best.utility;
      person.densityCost = best.densityCost;
      person.neighboursInDensityRadius = best.neighbours;
      person.lastSellers = best.sellers;
      person.lastQuantities = best.quantities;

      buyWithTime(best, person, world, leftWith);

      if (world.config.peopleMobile) world.movePerson(person, best.location);
    },

    describe() {
      return `buys goods with time (${nearbySamples} near r=${nearbyRadius}, ${wideSamples} wide, ${optimiser.describe()})`;
    },
  };
}

/**
 * Take back the time left with Firms last turn.
 *
 * Must happen before anything is priced, or the Person's own stale contribution would still
 * be inflating the production scale they are about to evaluate.
 *
 * @param {Map<number, Map<number, number>>} leftWith
 * @param {import('./agents.js').Person} person @param {import('./world.js').World} world
 */
function withdrawPreviousInput(leftWith, person, world) {
  const mine = leftWith.get(person.id);
  if (!mine) return;
  for (const firm of world.firms) {
    const amount = mine.get(firm.id);
    if (amount === undefined) continue;
    firm.contributedTime -= amount;
    // Floating point can leave a whisker below zero; the original clamped it too.
    if (firm.contributedTime < 0 && firm.contributedTime > -1e-9) firm.contributedTime = 0;
  }
  mine.clear();
}

/**
 * Price every Firm from this spot and find the best use of one day.
 * @param {Bundle} bundle @param {import('./agents.js').Person} person
 * @param {import('./world.js').World} world @param {import('./econ.js').Optimiser} optimiser
 */
function evaluateTransmission(bundle, person, world, optimiser) {
  bundle.budget = TIME_PER_DAY;

  if (world.config.densityCost > 0) {
    const density = world.densityAndNeighboursAt(bundle.location, person);
    bundle.rawDensityCost = density.raw;
    bundle.densityCost = world.config.densityCost * density.raw;
    bundle.neighbours = density.neighbours;
    bundle.budget -= bundle.densityCost;
  }
  if (bundle.budget <= 0) return;

  for (const firm of world.firms) {
    bundle.addSeller(firm, firm.priceAtDistance(world.distance(bundle.location, firm)));
  }
  if (!bundle.hasSellers()) return;

  const utility = world.config.utility;
  bundle.quantities = optimiser.optimise(bundle.prices, bundle.budget, utility, world.rng);
  bundle.utility = utility.utilityOf(bundle.quantities);
}

/**
 * Pay for the goods, and trigger each Firm's price reflex.
 *
 * What the Firm receives as productive input is `quantity × base price` — not what the Person
 * paid. The difference is the distance cost, which melts away: the buyer spends it and nobody
 * receives it.
 *
 * @param {Bundle} bundle @param {import('./agents.js').Person} person
 * @param {import('./world.js').World} world @param {Map<number, Map<number, number>>} leftWith
 */
function buyWithTime(bundle, person, world, leftWith) {
  let mine = leftWith.get(person.id);
  if (!mine) {
    mine = new Map();
    leftWith.set(person.id, mine);
  }

  for (let i = 0; i < bundle.sellers.length; i++) {
    const units = i < bundle.quantities.length ? bundle.quantities[i] : 0;
    if (units <= 0) continue;

    const seller = bundle.sellers[i];
    const input = units * seller.goodCost; // iceberg: delivery melts away

    seller.revenue += input;
    mine.set(seller.id, (mine.get(seller.id) ?? 0) + input);

    world.config.firmBehaviour?.onSale?.(seller, units, input, world);
  }
}

/** The original's hardcoded `adjustCostAmount`: the x in Δp = target/x. */
export const DEFAULT_RESPONSE_DIVISOR = 100;

/**
 * Granular, damped stock targeting — the engine of the transmission belt (§6.3.1).
 *
 * On every single sale, a Firm: hands over the goods, puts the labour it received straight
 * into production, and nudges its price towards whatever would keep stock at target. There is
 * no deliberation and no waiting. Leijonhufvud's "law of motion" rather than a decision.
 *
 * **Why the derivatives are there.** Targeting stock alone does not work, and the way it fails
 * is instructive: when stock reaches zero the price is at its furthest from equilibrium, so
 * the system sails through and comes back the other way. The result is a perfectly stable
 * oscillation that never settles — the classic undamped harmonic oscillator, sitting in the
 * middle of an economic model. Targeting the first and second differences as well damps it.
 *
 *     target = s + s' + s'' − markupTarget
 *     Δprice = |target| / responseDivisor,  signed so stock moves back towards target
 *
 * Two implementation details worth stating, because both look like mistakes and are not:
 *
 * - **The differences are per-*sale*, not per-day.** The original called them
 *   "...BetweenDays", which is misleading — with 300 People this fires 300 times a day per
 *   Firm. Oscillation frequencies depend on it.
 * - **Magnitude and polarity come from different quantities.** The size of the price change
 *   is taken from the damped sum, but its *direction* from raw stock against the markup
 *   target. A literal reading of the thesis equation would use the damped sum for both. This
 *   reproduces the code that produced the figures.
 *
 * This is the thesis' third decision type (§4.5.2): a *reflex*, fired by the sale itself
 * rather than scheduled on the timeline. `act` therefore does nothing.
 *
 * @param {object} [options]
 * @param {number} [options.responseDivisor]
 * @param {boolean} [options.useFirstDifference] off gives the undamped oscillation of §6.3.1
 * @param {boolean} [options.useSecondDifference]
 * @returns {Behaviour}
 */
export function firmStockTarget({
  responseDivisor = DEFAULT_RESPONSE_DIVISOR,
  useFirstDifference = true,
  useSecondDifference = true,
} = {}) {
  return {
    /** Nothing scheduled. Everything happens in `onSale`. */
    act() {},

    onSale(firm, units, input, world) {
      const production = world.config.production;

      firm.stockBeforeTransaction = firm.stock;

      // Hand over the goods.
      firm.stock -= units;

      // The labour received goes straight in, and the buyer's proportional share of the
      // resulting output comes straight back out as stock: g_a = f(T)·(t_a/T).
      firm.contributedTime += input;
      firm.stock += production.granularShare(firm.contributedTime, input);

      firm.productionLevel = production.outputFor(firm.contributedTime);
      firm.outputPerUnitTime = production.outputPerUnit(firm.contributedTime);

      // Differences across successive sales.
      firm.stockFirstDiffPrevious = firm.stockFirstDiff;
      firm.stockFirstDiff = firm.stock - firm.stockBeforeTransaction;
      firm.stockSecondDiff = firm.stockFirstDiff - firm.stockFirstDiffPrevious;

      // Markup ratchets: the target keeps rising, and the Firm keeps the difference.
      firm.movingMarkUpTarget += firm.markUp;

      let target = firm.stock - firm.movingMarkUpTarget;
      if (useFirstDifference) target += firm.stockFirstDiff;
      if (useSecondDifference) target += firm.stockSecondDiff;

      let change = Math.abs(target) / responseDivisor;
      change = firm.stock < firm.movingMarkUpTarget ? change : -change;

      const previousPrice = firm.goodCost;
      firm.goodCost += change;

      // A negative price is meaningless. The original fell back to the last price seen while
      // price was falling, rather than simply the previous price -- which matters, because
      // reverting to the previous price pins a crashing Firm in place forever.
      if (firm.goodCost <= 0) {
        firm.goodCost = firm.lastFallingPrice > 0 ? firm.lastFallingPrice : previousPrice / 2;
      }

      // A long run of rises means the Firm has lost so much demand it can no longer reach its
      // target, and is compounding the error. The runaway of §6.3.2.
      if (firm.goodCost > previousPrice) {
        firm.consecutivePriceRises++;
      } else {
        firm.consecutivePriceRises = 0;
        firm.lastFallingPrice = firm.goodCost;
      }
    },

    describe() {
      const damping = useFirstDifference && useSecondDifference ? "s+s'+s''"
        : useFirstDifference ? "s+s'" : 's only (undamped)';
      return `granular stock target (${damping}, divisor ${responseDivisor})`;
    },
  };
}
