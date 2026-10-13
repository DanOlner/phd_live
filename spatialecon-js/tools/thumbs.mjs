// @ts-check

/**
 * Regenerate the catalogue stills — one per registered model, into `snapshots/thumbs/`.
 *
 * A thin loop over `snapshot.mjs --thumb` rather than a second renderer, so the cards on the
 * index page are drawn by the same view code as the live models. Run it after adding a model
 * or changing a view; the output is committed, because the site has no build step.
 *
 *   npm run thumbs
 */

import { spawnSync } from 'node:child_process';
import { ids } from '../src/scenario.js';
import '../src/scenarios/index.js';

let failed = 0;
for (const id of ids()) {
  const result = spawnSync(process.execPath, ['tools/snapshot.mjs', id, '--thumb'], {
    encoding: 'utf8',
  });
  if (result.status !== 0) {
    failed++;
    console.error(`${id}: FAILED\n${result.stderr}`);
  } else {
    process.stdout.write(`${id} `);
  }
}
console.log(`\n${ids().length - failed} stills written to snapshots/thumbs/`
  + (failed ? `, ${failed} failed` : ''));
