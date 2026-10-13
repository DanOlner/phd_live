// @ts-check

/**
 * Every test suite. `npm test`.
 *
 * Tier 1 first (fast, exact), then the views (fast), then the models and the
 * transmission belt (tier 2, ~50s — they run real settlements, which is the only way to
 * check an emergent claim). The site layer last: walkthrough captions, pop-out content and
 * the precomputed sweeps, all of which fail silently rather than loudly if left unchecked.
 */

import './econ.test.js';
import './engine.test.js';
import './view.test.js';
import './models.test.js';
import './belt.test.js';
import './catalogue.test.js';
import './site.test.js';
