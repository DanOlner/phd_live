// @ts-check

/**
 * Every model, registered. Ids match THESIS_DIGEST.md.
 *
 * The whole catalogue: all seven groups, A through G. Several entries here are not agent
 * models at all -- C4 and C5 are closed forms, E4 is a difference equation, F1 and F2 are
 * process demonstrations, and G1 is Krugman's Core Model, a separate program in the original.
 * They are scenarios anyway, so that every model in the thesis is a page with the same
 * controls, notes and chart machinery.
 */

import { register } from '../scenario.js';
import { a1 } from './a1.js';
import { a2 } from './a2.js';
import { a3 } from './a3.js';
import { a4 } from './a4.js';
import { b1 } from './b1.js';
import { b2, b2z } from './b2.js';
import { b3 } from './b3.js';
import { c1 } from './c1.js';
import { c2 } from './c2.js';
import { c6, c6c } from './c6.js';
import { c8 } from './c8.js';
import { d1a, d1b, d1c, d1d } from './d1.js';
import { d2, d2n, d2nn } from './d2.js';
import { e1, e1many, e1raw } from './e1.js';
import { e3 } from './e3.js';
import { e5, e6 } from './e5.js';
import { e7 } from './e7.js';
import { e8 } from './e8.js';
import { c3c, c3d, c3g, c3w } from './c3.js';
import { c4 } from './c4.js';
import { c5 } from './c5.js';
import { c7 } from './c7.js';
import { e2 } from './e2.js';
import { e4 } from './e4.js';
import { f1 } from './f1.js';
import { f2 } from './f2.js';
import { f3 } from './f3.js';
import { f4 } from './f4.js';
import { g1 } from './g1.js';

register(
  a1, a2, a3, a4,
  b1, b2, b2z, b3,
  c1, c2, c3w, c3g, c3c, c3d, c4, c5, c6, c6c, c7, c8,
  d1a, d1b, d1c, d1d,
  d2, d2n, d2nn,
  e1, e1raw, e1many, e2, e3, e4, e5, e6, e7, e8,
  f1, f2, f3, f4,
  g1,
);

export {
  a1, a2, a3, a4,
  b1, b2, b2z, b3,
  c1, c2, c3w, c3g, c3c, c3d, c4, c5, c6, c6c, c7, c8,
  d1a, d1b, d1c, d1d,
  d2, d2n, d2nn,
  e1, e1raw, e1many, e2, e3, e4, e5, e6, e7, e8,
  f1, f2, f3, f4,
  g1,
};
