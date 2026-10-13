// @ts-check

/**
 * The agents, and the Bundle they reason with.
 *
 * Classes rather than plain objects, deliberately and only here: there are thousands of
 * these, and declaring every field in the constructor keeps their shape stable, which is
 * what lets the engine run a 500-Person model at frame rate.
 */

import { ORIGIN, confine, distance } from './space.js';

/**
 * Anything with an identity and a location.
 *
 * Deliberately thin. In the original, `Actor` accumulated fields for every experiment ever
 * tried — commented-out blocks for backyard production, contributed-time wrappers,
 * productivity. Here the base carries identity and position only, and each subtype owns the
 * state its own behaviours need.
 */
export class Actor {
  /** @param {number} id */
  constructor(id) {
    /** @type {number} */
    this.id = id;
    /** @type {import('./space.js').Pt} */
    this.position = ORIGIN;
    /** Distance moved on the most recent turn. Used to detect spatial equilibrium. */
    this.distanceMovedLastTurn = 0;
  }

  /** Place without recording it as movement. For initial layout. @param {import('./space.js').Pt} p */
  placeAt(p) {
    this.position = p;
  }

  /**
   * Move, recording the distance covered. Migration is free in this framework — no cost is
   * charged here (§4.3.2).
   * @param {import('./space.js').Pt} p @param {import('./space.js').Space} space
   */
  moveTo(p, space) {
    const confined = confine(space, p);
    this.distanceMovedLastTurn = distance(space, this.position, confined);
    this.position = confined;
  }

  /** Short label used in output and figures. */
  label() {
    return `Actor ${this.id}`;
  }
}

/**
 * A consumer. Has one unit of time per day and spends it to maximise utility.
 *
 * What the time buys depends on the model. In the partial models it buys a wage, which then
 * buys goods. In the transmission belt it buys goods directly, so the goods *are* the wage
 * (§6.3). Both are the same question from the agent's side: what is the most utility I can
 * get for my day?
 */
export class Person extends Actor {
  /** @param {number} id */
  constructor(id) {
    super(id);
    /** Utility achieved on the most recent turn. */
    this.utility = 0;
    /** Density cost incurred at the location chosen on the most recent turn. */
    this.densityCost = 0;
    /** Number of others inside the density radius. Recorded for §5.4.2. */
    this.neighboursInDensityRadius = 0;
    /**
     * Firm this Person is tied to in the von Thünen scenarios (§5.4.5), where each Person
     * is permanently assigned to one Firm and may neither work nor buy elsewhere.
     * Null in every other model.
     * @type {Firm | null}
     */
    this.assignedFirm = null;

    /**
     * What this Person actually bought on their last turn, parallel arrays.
     *
     * Kept because it is the only record of the trade itself rather than its consequences —
     * everything else on Person and Firm is an outcome. D1's published figure is a profile
     * of exactly this (demand for each Firm across the line), and the unpublished
     * `PeopleBuyLines` view draws it directly.
     *
     * Note these are the *noisy* quantities where demand noise is on: what was bought, not
     * what was wanted. Utility is scored on the clean optimum before noise, which is the
     * asymmetry model d2 turns on.
     *
     * @type {Firm[]}
     */
    this.lastSellers = [];
    /** @type {number[]} */
    this.lastQuantities = [];
  }

  label() {
    return `Person ${this.id}`;
  }
}

/** The day's budget. Always 1 — "one day per day" (§4.2). */
export const TIME_PER_DAY = 1.0;

/** Stock level for partial models, standing in for "supply is never a constraint". */
export const EFFECTIVELY_UNLIMITED_STOCK = 100000;

/**
 * A producer. Sells one good and, in the transmission belt models, turns the time it
 * receives into output.
 *
 * The two Firm behaviours in the thesis are quite different animals and share almost no
 * state. A Firm in the partial models is a fixed point of supply with unlimited stock that
 * gropes for a better location once a day (§5.5). A Firm in the transmission belt never
 * moves, but adjusts price on *every single sale* to chase a stock target (§6.3.1). Both
 * sets of fields live here; a scenario's chosen behaviour uses one or the other.
 */
export class Firm extends Actor {
  /** @param {number} id */
  constructor(id) {
    super(id);

    // --- Prices ---
    /** Base price per unit, before any distance cost. */
    this.goodCost = 0;
    /** Cost to move one unit of the good one unit of distance. */
    this.deliveryCost = 0;
    /** Cost to a Person of covering one unit of distance to work here. */
    this.commuteCost = 0;
    /** Wage offered for a full day's labour. */
    this.wage = 0;

    // --- Production and stock (transmission belt only) ---
    /** Current stock. Starts at zero for transmission Firms; may legitimately go negative. */
    this.stock = 0;
    /** Running total of time currently left with this Firm by People. */
    this.contributedTime = 0;
    /** Total output at the current level of contributed time. */
    this.productionLevel = 0;
    /** Output per unit of input — the efficiency plotted in §6.4.1. */
    this.outputPerUnitTime = 0;
    /** Revenue this turn, and the previous turn's, for the location-crawl feedback. */
    this.revenue = 0;
    this.revenueLastTurn = 0;

    // --- Stock targeting state ---
    // Named for what they are: differences between consecutive *transactions*, not days.
    // The original called these "...BetweenDays", which is misleading — the granular
    // adjustment fires on every purchase, so with 300 People that is 300 updates a day per
    // Firm (CODE_ANATOMY §5.6). It governs the oscillation frequency in E1.
    /** Stock before the current transaction. */
    this.stockBeforeTransaction = 0;
    /** First difference of stock across transactions. */
    this.stockFirstDiff = 0;
    /** Previous first difference, kept to form the second. */
    this.stockFirstDiffPrevious = 0;
    /** Second difference of stock across transactions. */
    this.stockSecondDiff = 0;
    /** Per-transaction profit target. Zero means "just clear stock". */
    this.markUp = 0;
    /** Running total the markup ratchets up; the Firm skims the difference (§6.3.2). */
    this.movingMarkUpTarget = 0;
    /** Consecutive price rises. A long run indicates the runaway of §6.3.2. */
    this.consecutivePriceRises = 0;
    /** Last price recorded while price was falling; the fallback if a price goes negative. */
    this.lastFallingPrice = 0;
  }

  /** Total price to a buyer at this distance: P = p + c·d (§4.3.1). @param {number} d */
  priceAtDistance(d) {
    return this.goodCost + this.deliveryCost * d;
  }

  label() {
    return `Firm ${this.id}`;
  }
}

/**
 * A candidate location, with all the economics attached (§4.6.1).
 *
 * A Person does not search the whole map. They assemble a handful of Bundles — a few
 * nearby, a few anywhere, plus one for where they already are — work out what each would be
 * worth, and move to the best. Each Bundle answers, from *this* spot: what is the best wage
 * I could get, what would every good cost me delivered here, what density would I incur,
 * and so what utility could I reach?
 *
 * The whole framework hangs off this. It is why the density cost is computable at all —
 * every agent evaluates proximity from their own position — and why no market-clearing
 * price is needed, since two People at different spots simply face different prices.
 */
export class Bundle {
  /** @param {import('./space.js').Pt} offset @param {import('./space.js').Pt} location */
  constructor(offset, location) {
    /** Offset from the Person's current position. Zero means "stay where I am". */
    this.offset = offset;
    /** Absolute location this Bundle refers to. */
    this.location = location;
    /** Firms whose goods are available, parallel to `prices`. @type {Firm[]} */
    this.sellers = [];
    /** Full delivered price per unit from each seller, at this location. @type {number[]} */
    this.prices = [];
    /** Best employer found from here, or null if none is reachable. @type {Firm | null} */
    this.employer = null;
    /** Budget: the wage from `employer` after commuting, then less density cost. */
    this.budget = 0;
    /** Time left to give an employer after commuting. */
    this.workTime = 0;
    /** Density weight before scaling — the normalised neighbour sum. */
    this.rawDensityCost = 0;
    /** Density cost actually charged: densityCost × raw. */
    this.densityCost = 0;
    /** How many others fall inside the density radius here. */
    this.neighbours = 0;
    /** Utility achievable here, once optimised. */
    this.utility = 0;
    /** Optimal quantity of each good, parallel to `sellers`. @type {number[]} */
    this.quantities = [];
  }

  /** @param {Firm} firm @param {number} price */
  addSeller(firm, price) {
    this.sellers.push(firm);
    this.prices.push(price);
  }

  hasSellers() {
    return this.sellers.length > 0;
  }

  /**
   * Whether this location is worth considering at all.
   *
   * A non-positive budget means the density cost here has swallowed the entire wage, so the
   * Bundle is discarded rather than allowed to go negative. That single rule produces the
   * dispersal of §5.4.7: raise the density cost far enough and every central location fails
   * this test, so People end up spread to the edges — not because they are pushed, but
   * because nowhere crowded is affordable.
   *
   * It is also why C1's utility is not perfectly flat: density cannot go below zero, so
   * beyond the cutoff distance there is nothing left to trade away (CODE_ANATOMY §5.3).
   */
  isViable() {
    return this.budget > 0 && this.hasSellers();
  }
}
