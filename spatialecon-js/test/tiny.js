// @ts-check

/**
 * A test harness, in about forty lines.
 *
 * No framework, because none is needed and the whole project holds to zero dependencies.
 * Runs in Node and unmodified in a browser, which matters later: some of what wants
 * checking (the views) has no headless form.
 */

let passed = 0;
/** @type {string[]} */
const failures = [];
let currentSuite = '';

/** @param {string} name */
export function suite(name) {
  currentSuite = name;
  console.log(`\n${name}`);
}

/** @param {string} name @param {() => void} fn */
export function test(name, fn) {
  try {
    fn();
    passed++;
    console.log(`  ✓ ${name}`);
  } catch (e) {
    const message = e instanceof Error ? e.message : String(e);
    failures.push(`${currentSuite} > ${name}\n      ${message}`);
    console.log(`  ✗ ${name}\n      ${message}`);
  }
}

/** @param {string} message @param {number} expected @param {number} actual @param {number} [eps] */
export function assertClose(message, expected, actual, eps = 1e-9) {
  if (!Number.isFinite(actual) || Math.abs(expected - actual) > eps) {
    throw new Error(`${message}: expected ${expected}, got ${actual} (tolerance ${eps})`);
  }
}

/** @param {string} message @param {boolean} condition */
export function assertTrue(message, condition) {
  if (!condition) throw new Error(message);
}

/** @param {string} message @param {any} expected @param {any} actual */
export function assertEqual(message, expected, actual) {
  if (expected !== actual) {
    throw new Error(`${message}: expected ${expected}, got ${actual}`);
  }
}

/**
 * Print the tally and exit non-zero if anything failed, then reset — so a file that runs
 * several suites reports each on its own rather than cumulatively.
 */
export function report() {
  console.log(`\n${passed} passed, ${failures.length} failed`);
  const ok = failures.length === 0;
  if (!ok) {
    console.log('\nFailures:');
    for (const f of failures) console.log(`  ${f}`);
    process.exitCode = 1;
  }
  passed = 0;
  failures.length = 0;
  return ok;
}
