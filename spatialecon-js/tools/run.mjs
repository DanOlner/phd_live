// @ts-check

/**
 * Headless runner. Node only — no DOM, no canvas.
 *
 * Deliberately mirrors `spatialecon-java`'s Runner, flag for flag, so the same command runs
 * either implementation and the outputs can be put side by side.
 *
 *   node tools/run.mjs --list
 *   node tools/run.mjs --info a1
 *   node tools/run.mjs a1
 *   node tools/run.mjs a1 --days 500 --out data
 *   node tools/run.mjs a1 --seed 7
 */

import { mkdir, writeFile } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';

import { Recorder } from '../src/record.js';
import { all, get } from '../src/scenario.js';
import { World } from '../src/world.js';
import '../src/scenarios/index.js';

const args = process.argv.slice(2);

const has = (/** @type {string} */ flag) => args.includes(flag);

/** @param {string} flag @param {string | null} fallback */
function stringArg(flag, fallback) {
  const i = args.indexOf(flag);
  return i >= 0 && i + 1 < args.length ? args[i + 1] : fallback;
}

/** @param {string} flag @param {number} fallback */
function intArg(flag, fallback) {
  const v = stringArg(flag, null);
  return v === null ? fallback : Number.parseInt(v, 10);
}

/** The first bare word: flags that take a value swallow the next token. */
function firstNonFlag() {
  const valueless = new Set(['--list', '--info', '--help', '-h']);
  for (let i = 0; i < args.length; i++) {
    if (args[i].startsWith('--') || args[i] === '-h') {
      if (!valueless.has(args[i]) && i + 1 < args.length) i++;
      continue;
    }
    return args[i];
  }
  return null;
}

function usage() {
  console.log(`spatialecon — thesis models, headless runner

  run --list                        list scenarios
  run --info <id>                   show one scenario's setup
  run <id> [options]                run it

options:
  --days N          days to run (default: scenario's own)
  --out DIR         write DIR/<id>.csv
  --seed N          override the random seed
  --sample-every N  record every nth day (default 1)`);
}

function list() {
  console.log('Scenarios (ids match THESIS_DIGEST.md):\n');
  for (const s of all()) {
    console.log(`  ${s.id.padEnd(8)} ${s.title.padEnd(46)} ${s.thesisRef}`);
  }
  console.log('\n--info <id> for detail.');
}

/** @param {import('../src/scenario.js').Scenario} s */
function info(s) {
  console.log(`== ${s.id}: ${s.title}  (${s.thesisRef})`);
  if (s.notes) console.log(`\n${s.notes}`);
  const w = new World(s.config());
  s.setUp?.(w);
  console.log(`\n${w.config.summary()}`);
  const controls = s.controls?.(w) ?? [];
  if (controls.length > 0) {
    console.log('\ncontrols:');
    for (const p of controls) {
      console.log(`  ${p.name.padEnd(10)} ${p.label.padEnd(28)} ${sig(p.get())}  [${sig(p.min)} .. ${sig(p.max)}]`);
    }
  }
  console.log(`\ndefault days: ${s.defaultDays ?? 1000}`);
}

/**
 * @param {import('../src/scenario.js').Scenario} scenario
 * @param {number} days @param {number} sampleEvery
 * @param {number | null} seedOverride @param {string | null} outDir
 */
async function run(scenario, days, sampleEvery, seedOverride, outDir) {
  const config = scenario.config();
  if (seedOverride !== null) config.seed = seedOverride;

  const world = new World(config);
  scenario.setUp?.(world);
  const recorder = new Recorder().sampleEvery(sampleEvery);
  scenario.record?.(recorder, world);

  console.log(`== ${scenario.id}: ${scenario.title}  (${scenario.thesisRef})`);
  console.log(config.summary());
  console.log(`phases: ${world.timeline.phaseNames().join(' -> ')}`);
  console.log(`running ${days} days${sampleEvery > 1 ? `, sampling every ${sampleEvery}` : ''}\n`);

  const start = performance.now();
  for (let d = 0; d < days; d++) {
    scenario.beforeDay?.(world, d);
    world.timeline.nextDay();
    recorder.sample(world.day);
  }
  const seconds = (performance.now() - start) / 1000;

  console.log(`done: ${days} days in ${seconds.toFixed(2)}s (${Math.round(days / Math.max(seconds, 1e-9))} days/sec)`);

  if (!recorder.isEmpty) {
    console.log(`series: ${recorder.names().join(', ')}`);
    console.log(`mean People movement last day: ${world.meanPersonMovement().toFixed(6)}`);
  }

  if (outDir) {
    const file = resolve(join(outDir, `${scenario.id}.csv`));
    await mkdir(dirname(file), { recursive: true });
    await writeFile(file, recorder.toCsv());
    console.log(`wrote ${file}`);
  }
}

/** @param {number} v */
function sig(v) {
  return Number(v.toPrecision(4)).toString();
}

// --- main ----------------------------------------------------------------------------

if (args.length === 0 || has('--help') || has('-h')) {
  usage();
} else if (has('--list')) {
  list();
} else {
  const id = firstNonFlag();
  if (!id) {
    console.error('No scenario given. Try --list.');
    process.exit(2);
  }
  const scenario = get(id);
  if (has('--info')) {
    info(scenario);
  } else {
    await run(
      scenario,
      intArg('--days', scenario.defaultDays ?? 1000),
      intArg('--sample-every', 1),
      has('--seed') ? intArg('--seed', 12) : null,
      stringArg('--out', null),
    );
  }
}
