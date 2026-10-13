// @ts-check

/**
 * One model from the thesis, as a self-describing unit — and the registry of them.
 *
 * This is the whole point of the refactor. In the original, choosing a model meant editing
 * `Main.java`, `ActorMaker.java` and `Visualiser.java`, then recompiling — so no checkout
 * corresponded to any published figure, and two scenarios were lost entirely. Here a model
 * states its own parameters, what it records and what it exposes for interaction, and any
 * number of them coexist.
 *
 * @typedef {object} Scenario
 * @property {string} id           short identifier matching THESIS_DIGEST: 'c8', 'e7'
 * @property {string} title        human-readable name
 * @property {string} thesisRef    section reproduced, e.g. '§5.4.7'
 * @property {() => import('./config.js').ModelConfig} config  built once per run
 * @property {number} [defaultDays]
 * @property {string} [notes]      caveats, provenance, what to watch
 * @property {(world: import('./world.js').World) => void} [setUp]
 *   Adjust the world after construction, before the run starts — tying People to Firms,
 *   seeding individual prices, imposing a starting asymmetry.
 * @property {(recorder: import('./record.js').Recorder, world: import('./world.js').World) => void} [record]
 * @property {(world: import('./world.js').World) => import('./config.js').Parameter[]} [controls]
 * @property {(world: import('./world.js').World, day: number) => void} [beforeDay]
 *   Called before each day, 0-based. The hook parameter sweeps use: step a value here and
 *   the day index becomes the sweep index, so a sweep is just a run and needs no driver.
 * @property {{when: (world: import('./world.js').World) => boolean, caption: string}[]} [steps]
 *   Optional step-through. `when` is a predicate on model state, never a day number —
 *   captions must survive a reroll (BUILD_PLANNER §3.2).
 */

/** @type {Map<string, Scenario>} */
const registry = new Map();

/** @param {Scenario[]} scenarios */
export function register(...scenarios) {
  for (const s of scenarios) {
    if (registry.has(s.id)) throw new Error(`duplicate scenario id: ${s.id}`);
    registry.set(s.id, s);
  }
}

/** @param {string} id */
export function get(id) {
  const s = registry.get(id);
  if (!s) {
    throw new Error(`unknown scenario '${id}'. Known: ${[...registry.keys()].join(', ')}`);
  }
  return s;
}

export function all() {
  return [...registry.values()];
}

export function ids() {
  return [...registry.keys()];
}
