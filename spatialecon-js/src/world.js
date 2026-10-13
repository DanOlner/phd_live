// @ts-check

/**
 * One model run: the config, the agents, the geography and the clock.
 *
 * This is what the original scattered across `gl`'s statics, `ActorMaker` and `Main`.
 * Constructing a World builds the agents, applies per-Firm cost ranging, lays everyone out
 * and wires the day loop. Nothing here is global, so runs are independent.
 */

import { Firm, Person } from './agents.js';
import { parameter } from './config.js';
import { Rng } from './rng.js';
import { distance } from './space.js';

/**
 * The day loop.
 *
 * A day is a sequence of *phases*. Within a phase the actors are permuted and each acts
 * once; phases run in the order they were added. Nothing acts simultaneously — when an
 * agent decides, everyone else is momentarily static (§4.5.2).
 *
 * The phase split is not cosmetic. Permuting People and Firms in one pot leaves a Firm
 * comparing its revenue against "yesterday" when a variable number of People have acted in
 * between; the thesis found the fix was to permute them separately, so every Person has
 * acted before any Firm does (§4.7.4). In the original that was two adjacent lines in
 * `Main.java` and easy to miss. Here it is the structure of the loop.
 */
export class Timeline {
  /** @param {Rng} rng */
  constructor(rng) {
    this.rng = rng;
    this.day = 0;
    /** @type {{name: string, actors: any[], action: (a: any) => void}[]} */
    this.phases = [];
    /** @type {(() => void)[]} */
    this.dayStartHooks = [];
  }

  /** Run before any phase, each day. For engine housekeeping, not agent behaviour.
   * @param {() => void} fn */
  onDayStart(fn) {
    this.dayStartHooks.push(fn);
    return this;
  }

  /**
   * Add a phase. Order of addition is the order phases run in.
   * @param {string} name @param {any[]} actors @param {(a: any) => void} action
   */
  phase(name, actors, action) {
    this.phases.push({ name, actors, action });
    return this;
  }

  /** Run one day: every phase, in order, each internally permuted. */
  nextDay() {
    for (const hook of this.dayStartHooks) hook();
    for (const p of this.phases) {
      const order = p.actors.slice();
      this.rng.shuffle(order);
      for (const a of order) p.action(a);
    }
    this.day++;
  }

  phaseNames() {
    return this.phases.map((p) => p.name);
  }
}

export class World {
  /** @param {import('./config.js').ModelConfig} config */
  constructor(config) {
    this.config = config;
    this.space = config.space;
    this.rng = new Rng(config.seed);

    /** @type {Person[]} */
    this.people = [];
    /** @type {Firm[]} */
    this.firms = [];

    let id = 0;
    for (let i = 0; i < config.peopleCount; i++) this.people.push(new Person(id++));
    for (let i = 0; i < config.firmCount; i++) this.firms.push(this._makeFirm(id++, i));

    config.placePeople(this.people, this.space, this.rng);
    config.placeFirms(this.firms, this.space, this.rng);

    this.reindexPeople();

    this.timeline = new Timeline(this.rng)
      // Rebuilt each day so that any out-of-band `placeAt` — a scenario's setUp, a test, a
      // future view that drags an agent — self-heals within a day rather than leaving a
      // silently wrong index. O(People) against ~50,000 queries a day: about 1%.
      .onDayStart(() => this.reindexPeople())
      .phase('people', this.people, (p) => config.personBehaviour?.act(p, this))
      .phase('firms', this.firms, (f) => config.firmBehaviour?.act(f, this));
  }

  // --- Spatial index for the density query -------------------------------------------
  //
  // The density cost is the only genuine radius query in the model (the goods search is
  // not — every published config set `peopleConsiderAll`, so every Person sees every Firm).
  // Done naively it is also the whole cost of the model: each Person evaluates ~21 candidate
  // Bundles per turn and each one scans the entire population, so C1 at 500 People runs
  // 10.5 million distance calculations a day. That is 14 days/sec, which is fine headless
  // and hopeless on a canvas.
  //
  // A uniform grid of cells one radius across reduces each query to the nine cells that
  // could possibly contain a neighbour. **It changes no result** — the same People are
  // found, the arithmetic on them is identical — it just stops asking about the ones that
  // cannot qualify.

  /**
   * Rebuild the index from scratch. Call after moving People by any route other than
   * {@link movePerson} — a scenario's `setUp`, say.
   */
  reindexPeople() {
    this._cell = Math.max(this.config.densityRadius, 1e-9);
    /** @type {Map<number, import('./agents.js').Person[]>} */
    this._grid = new Map();
    for (const p of this.people) this._gridAdd(p);
  }

  /** Integer cell key. The offset keeps negative coordinates (TWO_REGION) well-behaved.
   * @param {number} x @param {number} y */
  _key(x, y) {
    return (Math.floor(x / this._cell) + 4096) * 65536 + (Math.floor(y / this._cell) + 4096);
  }

  /** @param {import('./agents.js').Person} p */
  _gridAdd(p) {
    const key = this._key(p.position.x, p.position.y);
    p._cellKey = key;
    const cell = this._grid.get(key);
    if (cell) cell.push(p);
    else this._grid.set(key, [p]);
  }

  /**
   * Move a Person and keep the index correct. Behaviours must use this rather than
   * `person.moveTo`, or the grid silently goes stale — the same class of trap as writing a
   * cost to the config instead of the Firms.
   * @param {import('./agents.js').Person} person @param {import('./space.js').Pt} to
   */
  movePerson(person, to) {
    const cell = this._grid.get(person._cellKey ?? -1);
    if (cell) {
      const i = cell.indexOf(person);
      if (i >= 0) cell.splice(i, 1);
    }
    person.moveTo(to, this.space);
    this._gridAdd(person);
  }

  /**
   * People whose cell could hold something within `radius` of this point. A superset of the
   * true answer — callers still test the actual distance.
   * @param {import('./space.js').Pt} point @param {number} radius
   * @returns {import('./agents.js').Person[][]}
   */
  _cellsNear(point, radius) {
    // The density radius can be changed live, so notice if the cell size no longer suits it.
    if (this._cell !== Math.max(this.config.densityRadius, 1e-9)) this._buildGrid();

    const span = Math.ceil(radius / this._cell);
    const cx = Math.floor(point.x / this._cell);
    const cy = Math.floor(point.y / this._cell);
    /** @type {import('./agents.js').Person[][]} */
    const out = [];
    for (let i = cx - span; i <= cx + span; i++) {
      for (let j = cy - span; j <= cy + span; j++) {
        const cell = this._grid.get((i + 4096) * 65536 + (j + 4096));
        if (cell && cell.length > 0) out.push(cell);
      }
    }
    return out;
  }

  get day() {
    return this.timeline.day;
  }

  /** Build one Firm, then apply the ranging rules to it. @param {number} id @param {number} index */
  _makeFirm(id, index) {
    const f = new Firm(id);
    f.stock = 0;
    this._applyCosts(f, index);
    const range = this.config.initialPriceRange;
    if (range) f.goodCost = this.rng.between(range[0], range[1]);
    f.lastFallingPrice = f.goodCost;
    return f;
  }

  /**
   * Push the config's costs onto a Firm, multiplied by its index if ranging is on.
   *
   * Ranging is how the two-Firm scenario of §5.4.6 gets delivery costs of 2 and 4 from a
   * single base of 2, and how the four wage bands of §5.4.5 come from a base of 1. Easy to
   * miss, and it means the headline parameter is a base, not the value any Firm sees.
   *
   * The multiplier is 1 + index × `rangeStep`. The original's step was always 1, giving
   * ×1, ×2, ×3…; H1 varies it to make the gap between two Firms' wages a live control.
   *
   * @param {Firm} f @param {number} index position in the Firm list
   */
  _applyCosts(f, index) {
    const c = this.config;
    const m = 1 + index * c.rangeStep;
    // Leave prices alone when they were randomised at setup, or when a Firm is setting its
    // own price dynamically -- clobbering those would undo the whole mechanism.
    if (!c.initialPriceRange) {
      f.goodCost = c.baseGoodCost * (c.rangeGoodCost ? m : 1);
    }
    f.deliveryCost = c.deliveryCost * (c.rangeDelivery ? m : 1);
    f.wage = c.wage * (c.rangeWage ? m : 1);
    f.commuteCost = c.commuteCost * (c.rangeCommute ? m : 1);
  }

  /**
   * Re-apply the config's costs to every Firm.
   *
   * Needed because a Firm carries its own copy of each cost — it has to, since ranging
   * makes them differ. Changing a cost on the config alone therefore does nothing until it
   * is pushed out here, silently: the model runs, nothing errors, and the parameter you
   * thought you were sweeping is inert. This flattened B2's double dip into a straight line
   * in the Java for two build stages before anyone noticed (CODE_ANATOMY §7.11). The
   * original had the same split and solved it the same way, with each mouse control
   * iterating `gl.firms`.
   *
   * Note this overwrites any per-Firm cost a scenario set directly in `setUp`. Ranging is
   * currently the only supported way to give Firms different costs.
   */
  refreshFirmCosts() {
    this.firms.forEach((f, i) => this._applyCosts(f, i));
  }

  // --- Live cost changes. Always go through these, never the config directly. ---

  /** @param {number} v */
  setDeliveryCost(v) { this.config.deliveryCost = v; this.refreshFirmCosts(); }
  /** @param {number} v */
  setCommuteCost(v) { this.config.commuteCost = v; this.refreshFirmCosts(); }
  /** @param {number} v */
  setBaseGoodCost(v) { this.config.baseGoodCost = v; this.refreshFirmCosts(); }
  /** @param {number} v */
  setWage(v) { this.config.wage = v; this.refreshFirmCosts(); }
  /** Spacing of the ranged levels: Firm i gets base × (1 + i × v). @param {number} v */
  setRangeStep(v) { this.config.rangeStep = v; this.refreshFirmCosts(); }

  // --- Ready-made parameter bindings, bound so changes actually reach the Firms ---

  deliveryCostParam() {
    return parameter({
      name: 'deliv', label: 'distance cost', min: 0, max: 50,
      get: () => this.config.deliveryCost, set: (v) => this.setDeliveryCost(v),
    });
  }

  commuteCostParam() {
    return parameter({
      name: 'commute', label: 'commute cost', min: 0, max: 5,
      get: () => this.config.commuteCost, set: (v) => this.setCommuteCost(v),
    });
  }

  goodCostParam() {
    return parameter({
      name: 'price', label: 'base price', min: 0, max: 10,
      get: () => this.config.baseGoodCost, set: (v) => this.setBaseGoodCost(v),
    });
  }

  wageParam() {
    return parameter({
      name: 'wage', label: 'wage', min: 0, max: 5,
      get: () => this.config.wage, set: (v) => this.setWage(v),
    });
  }

  /** Density cost is not per-Firm, so it needs no propagation. */
  densityCostParam() {
    return parameter({
      name: 'density', label: 'density cost', min: 0, max: 500,
      get: () => this.config.densityCost, set: (v) => { this.config.densityCost = v; },
    });
  }

  /** @param {number} i */
  person(i) { return this.people[i]; }
  /** @param {number} i */
  firm(i) { return this.firms[i]; }

  /**
   * Distance between two actors, or from a point to an actor.
   * @param {any} a @param {any} b
   */
  distance(a, b) {
    const from = a.position ?? a;
    const to = b.position ?? b;
    return distance(this.space, from, to);
  }

  /**
   * People within a radius of a point, excluding one.
   * @param {import('./space.js').Pt} centrePt @param {number} radius @param {Person | null} exclude
   */
  peopleWithin(centrePt, radius, exclude) {
    return this.people.filter(
      (p) => p !== exclude && distance(this.space, centrePt, p.position) < radius,
    );
  }

  /**
   * The normalised density weight at a point, before scaling (§4.4).
   *
   * Each neighbour inside the radius contributes a weight tapering linearly from 1
   * (co-located) to 0 (at the radius). The sum is divided by the divisor — which is the
   * subject of Quirks, since the original used Firms plus People where the thesis text says
   * People alone.
   *
   * Note this is genuinely a radius query. The goods search is not: every published config
   * set `peopleConsiderAll`, so every Person saw every Firm at any distance (CODE_ANATOMY
   * §5.2). Proximity is local; the market is not.
   *
   * @param {import('./space.js').Pt} point @param {Person | null} exclude
   */
  rawDensityAt(point, exclude) {
    if (this.config.densityCost <= 0) return 0;
    return this.densityAndNeighboursAt(point, exclude).raw;
  }

  /**
   * How many others fall inside a radius of this point. Recorded for §5.4.2.
   * @param {import('./space.js').Pt} point @param {Person | null} exclude @param {number} radius
   */
  countNeighbours(point, exclude, radius) {
    let count = 0;
    for (const cell of this._cellsNear(point, radius)) {
      for (const other of cell) {
        if (other === exclude) continue;
        if (distance(this.space, point, other.position) < radius) count++;
      }
    }
    return count;
  }

  /**
   * Both density measures in one pass.
   *
   * They walk the same neighbours over the same radius, so computing them separately does
   * exactly twice the work for no benefit — and the Bundle cycle needs both on every
   * candidate location.
   *
   * @param {import('./space.js').Pt} point @param {Person | null} exclude
   * @returns {{raw: number, neighbours: number}}
   */
  densityAndNeighboursAt(point, exclude) {
    const radius = this.config.densityRadius;
    let weight = 0;
    let neighbours = 0;
    for (const cell of this._cellsNear(point, radius)) {
      for (const other of cell) {
        if (other === exclude) continue;
        const d = distance(this.space, point, other.position);
        if (d < radius) {
          weight += 1 - d / radius;
          neighbours++;
        }
      }
    }
    return { raw: weight / this.config.densityDivisor(), neighbours };
  }

  /** Density cost actually charged at a point: the raw weight, scaled.
   * @param {import('./space.js').Pt} point @param {Person | null} forPerson */
  densityCostAt(point, forPerson) {
    return this.config.densityCost * this.rawDensityAt(point, forPerson);
  }

  /**
   * Mean distance from each actor to its nearest neighbour of the same kind.
   *
   * The measure behind the "black hole" result of §5.3.2: with nothing to counteract
   * distance costs, this collapses towards zero as everyone piles onto one spot.
   *
   * @param {import('./agents.js').Actor[]} group
   */
  meanNearestNeighbour(group) {
    if (group.length < 2) return 0;
    let total = 0;
    for (const a of group) {
      let nearest = Infinity;
      for (const b of group) {
        if (a === b) continue;
        nearest = Math.min(nearest, distance(this.space, a.position, b.position));
      }
      total += nearest;
    }
    return total / group.length;
  }

  /** Mean distance moved by People last turn — the spatial-equilibrium test of §5.4.3. */
  meanPersonMovement() {
    if (this.people.length === 0) return 0;
    let total = 0;
    for (const p of this.people) total += p.distanceMovedLastTurn;
    return total / this.people.length;
  }
}
