// @ts-check

/**
 * Tier 1 of the verification scheme (BUILD_PLANNER §6): the deterministic core, diffed
 * numerically against the Java.
 *
 * The economics contains no randomness — given prices and a budget, CES optimal quantities
 * are a formula — so for models with no agent behaviour the two implementations should
 * agree to the last few bits, and any disagreement is a transcription error: a swapped
 * argument, a sign, `rho/(rho-1)` written the other way up.
 *
 * What this deliberately does NOT do is compare stochastic models trace by trace. The two
 * implementations use different generators on purpose, and `Math.pow` is not correctly
 * rounded in either runtime, so trajectories diverge by design. Those are checked by claim
 * instead — see the test suites.
 *
 * **Precision limit, worth knowing before over-reading a zero.** Both sides write CSV at
 * six significant figures, so that is the precision actually compared; a reported deviation
 * of 0 means "agree to six figures", not "bit-identical". Six figures is far more than
 * enough to catch a transcription error, which is what this is for, but if exact agreement
 * ever matters both writers would need widening first.
 *
 *   node tools/check.mjs                      # run every registered tier-1 model
 *   node tools/check.mjs --java-dir /tmp/jd   # where the Java CSVs live
 *
 * Regenerate the Java side with, from `spatialecon-java`:
 *   for m in a1 a2 a3; do make run ARGS="$m --out /tmp/javadata"; done
 */

import { readFile } from 'node:fs/promises';
import { join } from 'node:path';

import { Recorder } from '../src/record.js';
import { get } from '../src/scenario.js';
import { World } from '../src/world.js';
import '../src/scenarios/index.js';

/**
 * Models with no randomness anywhere: exact agreement is expected.
 *
 * Group B qualifies because People are immobile and the constrained optimiser is analytic —
 * no RNG call happens on any code path these take, so the two implementations must produce
 * identical numbers despite using different generators.
 */
const DETERMINISTIC = ['a1', 'a2', 'a3', 'b1', 'b2', 'b2z'];

/** Relative tolerance. Generous next to double precision, tight next to a real error. */
const TOLERANCE = 1e-9;

const args = process.argv.slice(2);
const javaDirIndex = args.indexOf('--java-dir');
const javaDir = javaDirIndex >= 0 ? args[javaDirIndex + 1] : '/tmp/javadata';

/** @param {string} csv */
function parse(csv) {
  const lines = csv.trim().split('\n');
  const header = lines[0].split(',');
  const rows = lines.slice(1).map((l) => l.split(',').map((c) => (c === '' ? NaN : Number(c))));
  return { header, rows };
}

/** @param {import('../src/scenario.js').Scenario} scenario */
function runJs(scenario) {
  const world = new World(scenario.config());
  scenario.setUp?.(world);
  const recorder = new Recorder();
  scenario.record?.(recorder, world);
  for (let d = 0; d < (scenario.defaultDays ?? 1000); d++) {
    scenario.beforeDay?.(world, d);
    world.timeline.nextDay();
    recorder.sample(world.day);
  }
  return parse(recorder.toCsv());
}

/** @param {number} a @param {number} b */
function relativeDeviation(a, b) {
  if (Number.isNaN(a) && Number.isNaN(b)) return 0;
  if (Number.isNaN(a) || Number.isNaN(b)) return Infinity;
  const scale = Math.max(Math.abs(a), Math.abs(b), 1e-12);
  return Math.abs(a - b) / scale;
}

let failed = 0;
console.log(`tier 1: deterministic core, vs Java CSVs in ${javaDir}\n`);
console.log(`${'model'.padEnd(7)}${'columns'.padEnd(9)}${'rows'.padEnd(7)}${'max rel. dev'.padEnd(15)}verdict`);
console.log('-'.repeat(52));

for (const id of DETERMINISTIC) {
  const scenario = get(id);
  /** @type {ReturnType<typeof parse>} */
  let java;
  try {
    java = parse(await readFile(join(javaDir, `${id}.csv`), 'utf8'));
  } catch {
    console.log(`${id.padEnd(7)}${'-'.padEnd(9)}${'-'.padEnd(7)}${'-'.padEnd(15)}NO JAVA CSV`);
    failed++;
    continue;
  }
  const js = runJs(scenario);

  /** @type {string[]} */
  const problems = [];
  if (js.header.join(',') !== java.header.join(',')) {
    problems.push(`headers differ:\n    java: ${java.header.join(',')}\n    js:   ${js.header.join(',')}`);
  }
  if (js.rows.length !== java.rows.length) {
    problems.push(`row count: java ${java.rows.length}, js ${js.rows.length}`);
  }

  let worst = 0;
  let worstAt = '';
  const rows = Math.min(js.rows.length, java.rows.length);
  const cols = Math.min(js.header.length, java.header.length);
  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      const dev = relativeDeviation(js.rows[r][c], java.rows[r][c]);
      if (dev > worst) {
        worst = dev;
        worstAt = `${java.header[c]} row ${r}: java ${java.rows[r][c]} vs js ${js.rows[r][c]}`;
      }
    }
  }

  const ok = problems.length === 0 && worst <= TOLERANCE;
  if (!ok) failed++;
  console.log(
    `${id.padEnd(7)}${String(cols).padEnd(9)}${String(rows).padEnd(7)}${worst.toExponential(2).padEnd(15)}${ok ? 'ok' : 'FAIL'}`,
  );
  for (const p of problems) console.log(`  ${p}`);
  if (!ok && worst > TOLERANCE) console.log(`  worst: ${worstAt}`);
}

console.log();
if (failed > 0) {
  console.log(`${failed} model(s) failed tier 1.`);
  process.exitCode = 1;
} else {
  console.log(`all ${DETERMINISTIC.length} deterministic models agree with the Java to ${TOLERANCE}.`);
  console.log('(Both sides write 6 significant figures, so that is the precision compared.)');
  console.log('\nStochastic models (a4, and everything from group B on) are checked by claim,');
  console.log('not by trace — different generators, on purpose. See test/.');
}
