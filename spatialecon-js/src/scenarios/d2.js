// @ts-check

/**
 * **D2 — the "data, meaning, response" game.** Thesis §5.5.2.
 *
 * People occupy only the middle half of the line, from 0.25 to 0.75. Two Firms start at
 * opposite edges. A game ends when *both* have got inside the occupied band, or when patience
 * runs out. The question: how long does it take, and what makes it harder?
 *
 * A single Firm has no difficulty at all. The world is static, so any change in its revenue
 * really is its own doing, and it walks straight to the middle. Add one competitor and the
 * datum stops meaning what the Firm assumes it means: revenue now moves for reasons the Firm
 * did not cause and cannot observe. The response follows the misreading.
 *
 * **Verified**: competition and noise both make this harder.
 *
 * **Not reproduced**: the thesis reports that under noise, low ρ (high love of variety) is
 * markedly harder, because each Firm's move then has a bigger external effect on the other's
 * demand. An independent reimplementation shows ρ making little difference, and low ρ if
 * anything easier. Ruled out: noise amplitude under both Quirks readings, distance cost 1 vs
 * the shipped 20, and substituting the hill-climber's natural noise for the additive kind.
 * There is a coherent mechanism for the observed direction — at high ρ demand is nearly
 * winner-take-all, so an edge Firm captures almost nothing and the change it is trying to
 * read is swamped — but that is not an explanation of the discrepancy. CODE_ANATOMY §7.12.
 *
 * The test asserts the *observed* direction, so that if this is ever resolved the change
 * fails loudly rather than passing quietly.
 *
 * Each recorded point plays a batch of complete games. Scaled down from the thesis' 100
 * randomisations and 10,000-day cap to keep a run to about a minute; the shape is the same.
 */

import { firmLocationCrawl, personBundle } from '../behaviour.js';
import { LINE, defineModel, parameter } from '../config.js';
import { ces, smoothEoS } from '../econ.js';
import { evenlyAlongX } from '../space.js';
import { World } from '../world.js';

const GAMES_PER_POINT = 24;
const PATIENCE = 4000;
const PEOPLE = 50;
const BAND_FROM = 0.25;
const BAND_TO = 0.75;
const DAYS = 20;

/** @param {number} rho @param {number} seed @param {number} noise */
function gameConfig(rho, seed, noise) {
  return defineModel({
    spaceKind: LINE, width: 1.0,
    peopleCount: PEOPLE, firmCount: 2,
    placePeople: evenlyAlongX(BAND_FROM, BAND_TO),
    placeFirms: evenlyAlongX(0, 1), // the two edges
    utility: ces(rho),
    production: smoothEoS(),
    baseGoodCost: 1.0,
    wage: 1.0,
    commuteCost: 0,
    deliveryCost: 20.0, // as shipped in Hotelling_Line_DMR.properties
    densityCost: 0,
    demandNoise: noise,
    personBehaviour: personBundle(),
    firmBehaviour: firmLocationCrawl(),
    peopleMobile: false,
    firmsMobile: true,
    seed,
  });
}

/** @param {World} w */
function bothInsideBand(w) {
  return w.firms.every((f) => f.position.x >= BAND_FROM && f.position.x <= BAND_TO);
}

/**
 * One game. Returns days taken, or PATIENCE if both never got inside.
 * @param {number} rho @param {number} seed @param {number} noise
 */
export function playGame(rho, seed, noise) {
  const w = new World(gameConfig(rho, seed, noise));
  for (let day = 1; day <= PATIENCE; day++) {
    w.timeline.nextDay();
    if (bothInsideBand(w)) return day;
  }
  return PATIENCE;
}

/**
 * Mean days to succeed, and the share of games that succeeded at all.
 * @param {number} rho @param {number} noise @param {number} [games]
 */
export function playBatch(rho, noise, games = GAMES_PER_POINT) {
  let totalDays = 0;
  let succeeded = 0;
  for (let game = 0; game < games; game++) {
    const days = playGame(rho, 1000 + game * 37, noise);
    totalDays += days;
    if (days < PATIENCE) succeeded++;
  }
  return { meanDays: totalDays / games, successRate: succeeded / games };
}

/** @param {string} id @param {number} noise */
function make(id, noise) {
  let rho = 0.05;
  let result = { meanDays: 0, successRate: 0 };

  /** @type {import('../scenario.js').Scenario} */
  return {
    id,
    title: `DMR game: two Firms find their market (noise=${noise.toFixed(1)})`,
    thesisRef: '§5.5.2',
    defaultDays: DAYS,

    notes: `50 People occupy only 0.25-0.75 of the line. Two Firms start at the edges. A game
ends when both are inside the band, or after ${PATIENCE} days. Distance cost 20. Demand
noise = ${noise.toFixed(1)}. Each of the ${DAYS} recorded points sweeps rho and plays
${GAMES_PER_POINT} complete games, reporting the mean days to succeed.

VERIFIED: competition and noise both make this harder. A lone Firm walks straight in; a
second Firm slows it; noise slows it further.

NOT REPRODUCED: the thesis reports that under noise, LOW rho (high love of variety) is
markedly harder. Here rho makes little difference, and if anything low rho is easier. Treat
the rho axis of this model as an open question, not a result. CODE_ANATOMY §7.12.

successRate below 1 means some games hit the patience limit -- those are counted at the cap,
so the mean understates the difficulty.`,

    /** The outer scenario is a shell; the real work happens in the sub-games. */
    config: () => gameConfig(rho, 12, noise),

    /** Step rho across its range, then play a batch of games at that value. */
    beforeDay(world, day) {
      // Clamped: the run stops at defaultDays headless, but a page keeps going, and an
      // unclamped ramp walks the swept value straight out of its meaningful range.
      const t = Math.min(day, DAYS - 1) / Math.max(DAYS - 1, 1);
      rho = 0.05 + t * 0.94;
      result = playBatch(rho, noise);
    },

    record(r) {
      r.global('rho', () => rho);
      r.global('daysToSucceed', () => result.meanDays);
      r.global('successRate', () => result.successRate);
      r.global('noise', () => noise);
    },

    controls: () => [
      parameter({
        name: 'noise', label: 'demand noise (fixed per variant)', min: 0, max: 1,
        get: () => noise, set: () => {},
      }),
    ],
  };
}

export const d2 = make('d2', 0);
export const d2n = make('d2n', 0.1);
export const d2nn = make('d2nn', 0.5);
