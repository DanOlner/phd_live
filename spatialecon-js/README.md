# spatialecon-js

The models from Dan Olner's 2013 PhD, in the browser.

This is the deliverable. `../spatialecon-java` is the reference implementation that verified
the models and does not ship; the original 2013 code in `../Thesis_on_github` is untouched.

See [`../BUILD_PLANNER.md`](../BUILD_PLANNER.md) for the plan, [`../BUILD_LOG.md`](../BUILD_LOG.md)
for what was built and what it taught, [`../THESIS_DIGEST.md`](../THESIS_DIGEST.md) for the
model catalogue, and [`../CODE_ANATOMY.md`](../CODE_ANATOMY.md) for how the original was
built.

## Run it

No install, no build step, no dependencies.

```sh
python3 -m http.server 8000           # then open localhost:8000 — the models, live
npm test                              # 154 assertions, ~5min
node tools/run.mjs --list             # what models exist
node tools/run.mjs --info c1          # one model's setup
node tools/run.mjs c1 --out data      # run headless, write CSV
node tools/check.mjs                  # numerical agreement with the Java
node tools/snapshot.mjs c1            # render a model to SVG, no browser
node tools/snapshot.mjs --sweep e3-rho # render a precomputed sweep the same way
npm run sweep                         # regenerate data/sweeps/ (~2min)
npm run thumbs                        # regenerate the catalogue stills
```

`data/sweeps/` and `snapshots/thumbs/` are **committed output**. The site is a static file
tree with no build step, so anything a page loads has to already be there.

Needs Node 18+ for the runner and tests. The engine itself is plain ES modules with no Node
APIs, so it loads unchanged in a browser via `<script type="module">`. A static server is
needed only because browsers refuse ES modules over `file://`.

## Deploy

GitHub Pages, from this folder as-is — no `docs/` folder, no `gh-pages` branch.
`../.github/workflows/deploy-pages.yml` uploads `spatialecon-js/` as the Pages artifact on
every push that touches it. One-time setup: Settings → Pages → Source: **GitHub Actions**.

The workflow does not regenerate anything, so regenerate the sweeps and stills locally and
commit them before pushing. Every path in the site is relative and matches its file's case
exactly, which matters: macOS forgives a case mismatch and Pages does not.

## Layout

```
index.html       the model catalogue, with a still per model
model.html       one model, live: ?m=c1 — controls, pop-outs, walkthrough, sweeps
methodology.html the argument: decision types, agent number, what a model can claim
data/sweeps/     precomputed sweep results, committed (decision 1)
snapshots/       SVG stills; thumbs/ are the catalogue cards
src/
  content.js     per-model pop-outs and walkthroughs
  rng.js         seeded PRNG — fixed run and reroll
  space.js       the four space kinds, distance, placements
  econ.js        CES, Stone-Geary, both production functions, both optimisers
  agents.js      Person, Firm, Bundle
  world.js       World and Timeline: agent construction, day loop, density index
  config.js      defineModel, Parameter, Quirks
  record.js      Recorder, Series, aggregations
  behaviour.js   the Bundle cycle, the DMR crawl, the transmission belt
  scenario.js    the Scenario shape and registry
  coremodel.js   Krugman's Core Model — solved, not simulated (G1)
  scenarios/     one file per model family
  view/
    plane.js     People and Firms in 2-D               (B3 C1 C2 C6 C8 E8 H1)
    line.js      mirrored profiles either side of a rule  (D1 D2 E7)
    timeseries.js  auto-scaling series vs a deduced value (E1 E3 E5 E6; H1's 2nd panel)
    curve.js     a quantity against a swept parameter    (A1 A2 A3 A4)
    correlate.js two quantities against each other, joined in time order (E8)
    sampler.js   one Person's decision, drawn                       (F4)
    optimiser.js the hill-climber in the space it searches   (A4, 2nd panel)
    buylines.js  overlay: the trades themselves, weight ~ quantity
    equalise.js  overlay: C1's equalising cutoff and its fringe
    axis.js      ticks, log scales and label spreading, shared by the charts
    profiles.js  what the line view plots, per model family
    choose.js    which view a model gets, and what it plots
    chrome.js    run loop, controls, keys, capture
    sampler.js   one Person's decision, drawn                       (F4)
    distribution.js  a shape over a population — wealth, gaps    (F1 F2)
    coremodel.js the Core Model's short-run figure                   (G1)
    sweep.js     render a precomputed sweep, curve or scatter
tools/
  run.mjs        headless runner, same flags as the Java
  check.mjs      tier 1 verification against the Java
  snapshot.mjs   render a model or a sweep to SVG, via the real view code
  sweep.mjs      run the parameter sweeps, write data/sweeps/*.json
  thumbs.mjs     regenerate the catalogue stills
test/
  tiny.js        ~40-line harness; runs in Node and in a browser
```

## Dependencies: none

Not a preference, a constraint. Canvas 2D replaces Processing; the two graph types get
written rather than pulled in, because the aesthetic is the point (BUILD_PLANNER §4).
Type safety comes from `// @ts-check` plus JSDoc — full editor checking, zero build step.

## Verification

Two tiers, because the model has two kinds of content (BUILD_PLANNER §6).

**Tier 1 — the deterministic core.** The economics has no randomness in it: given prices and
a budget, CES optimal quantities are a formula. So these have exactly right answers, and are
checked both against closed forms and against the Java column by column.

```
model  columns  rows   max rel. dev   verdict
a1     6        99     0.00e+0        ok
a2     7        100    0.00e+0        ok
a3     9        120    0.00e+0        ok
```

Both sides write six significant figures, so that is the precision compared — a zero means
"agrees to six figures", not "bit-identical". Ample for catching a transcription error,
which is what it is for.

**Tier 2 — everything stochastic.** Checked by *claim*, not by trace. The two
implementations use different generators deliberately, and `Math.pow` is not correctly
rounded in either runtime, so trajectories diverge by design. What is asserted is the
published result: the clearing price reaches 1/(T+1), the Hotelling split happens, symmetry
breaks with ρ somewhere in [0.70, 0.80]. Windows where the Java records points, so the suite
survives a reseed.

That the 25 ported economics tests passed first time on a completely different generator is
the evidence this split is the right one — none of those claims depended on the stream.

## Status

**All eight stages done.** Forty-two models — the whole catalogue, all seven groups — plus
one curiosity, and 154 tests pass. Every model has a view, pop-outs in three kinds, and a
place in the index.

Seven precomputed sweeps, rendered through the same chart code as the live models; two of
them are clickable and set the model above. Walkthroughs fire on model state rather than day
numbers, so they survive a reroll, and a test enforces it. A methodology page carries the
§7.4 argument in prose, cross-linked to the models that demonstrate each part of it.

Not every entry is an agent model, and that is the catalogue rather than a shortfall: C4 and
C5 are closed forms you could do on paper, E4 is a difference equation, F1 and F2 are process
demonstrations, and G1 is Krugman's Core Model — solved, not simulated.

| | |
|---|---|
| engine + econ + behaviours | ported, 55 tests |
| group A (a1–a4) | tier 1 exact vs Java; curve view |
| group B (b1, b2, b2z, b3) | b1/b2/b2z tier 1 exact; b3 by claim |
| group C (c1, c2, c6, c6c, c8) | by claim; live on canvas |
| group D (d1a–d, d2/d2n/d2nn) | by claim; line view |
| group E (e1, e1raw, e1many, e3, e5, e6, e7, e8) | **by arithmetic** — see below |
| group F (f1, f2, f3, f4) | the framework's own machinery, made visible |
| group G (g1) | the Core Model, ported from its own program |
| group H (h1) | a curiosity: a 2011 video, reconstructed; by claim, against statistics measured on the video ([`../GREEN_BLOB_RED_BLOB.md`](../GREEN_BLOB_RED_BLOB.md)) |


Tier-2 results, against the independently-built Java:

| Claim | Java | Here |
|---|---|---|
| C1 mean utility ("close to 0.9") | 0.898 | **0.8985** |
| C1 corr(distance, density) | −0.986 | **−0.985** |
| C1 spatial equilibrium reached | yes | movement < 1e−4 |
| C8 utility–distance corr at density 500 | −0.97 | **< −0.9** |
| C6 a non-spatial advantage buys proximity | yes | yes |
| C6c a spatial advantage buys distance | yes | yes |
| D1 Hotelling split at ρ=0.99, cost 20 | 0.343 | **> 0.25** |
| B3 People collapse faster than Firms | 7× | yes |
| D2 noise makes the game harder | yes | yes |
| D2 low ρ is harder under noise (thesis) | **not reproduced** | **not reproduced** |
| E1 clearing price, 1 Firm 1 Person | 0.50000 | **0.50000**, stock 4e−15 |
| E1many 50 Firms 50 People | 0.50000 | **0.50000** |
| 20 Firms 100 People (T=5) | 0.16667 | **0.16667** |
| E3 symmetric two-Firm equilibrium | 0.66670 | **0.6666 / 0.6668** |
| E3 symmetry breaks at ρ | 0.764 | **0.766** |
| E7 efficiency rises, nobody worse off | ×3.4 | ratio 3.58 → 7.56, min utility 0.371 → 0.523 |
| E8 production ratio (thesis ~13) | 1.26 ✗ | **passes 13, settles 33** — see below |
| E8 transition at ρ | 0.97 needed | **0.9**, the thesis value |
| E6 threads the regime boundary | 1 crossing / 20,000 days | **3 / 8,000** |

The belt rows are the strongest evidence in the project. With x = l² + l a Firm receiving T
units of time clears its stock at price 1/(T+1), so these are checks against arithmetic
rather than against a correlation — and E3's threshold landing at 0.766 against the Java's
0.764, from independent code with a different generator, is about as good as agreement gets
in an emergent model.

### Notes from the port

**Seeding is for the reroll, not for parity.** Same seed, same run — so a visitor can be
shown a specific outcome and then press reroll and watch a different Firm win. E5 and E8
need this, because which Firm wins is genuinely chance and that is the thing being taught.
The consequence to design around: **nothing user-facing may key off a day number.**
Step-through captions fire on model events, never "on day 412", or the first reroll breaks
them.

**a4 is the shape of every tier-2 check.** Individual points differ between the two
implementations — different generator, 200-run batches — but both converge on the analytic
optimum (0.500000 and ~0.12501 against 0.5 and 0.125) and both reproduce the finding that
8 goods lead early and 2 goods win once converged. Right shape, different trace, exactly as
intended.

**The cost-propagation trap has a regression test.** Firms hold their own copy of every
cost, so writing to the config alone changes nothing — silently. This flattened B2's double
dip into a straight line in the Java for two build stages (CODE_ANATOMY §7.11). Live changes
go through `world.setDeliveryCost` and friends, and a test asserts that poking the config
directly does *not* reach the Firms.

**The Java's builder collapsed to almost nothing.** `ModelConfig` plus its builder is 256
lines of Java; `defineModel` is an object literal with defaults. That was the single biggest
predicted win of the port and it held up.

**The three "magnitude shortfalls" were one measurement error, and not in the models.**
E8's ratio reaching 1.26 against ~13, E6 crossing the boundary once in 20,000 days, and E7's
welfare claim holding only in trend looked like three separate weaknesses — all too weak,
never too strong, which is what suggested a shared cause.

Decompiling the shipped jars one level further, to the arguments each `VariableStore` was
constructed with, found it. E8's figure — labelled "Firm efficiency ratio" — plots
`productionLevel`, total output f(T). The obvious reading, `Output_per_unitTime`, is
`0.001·T + 1` with the shipped config, and since total time is 300 People × 1 day it
**cannot exceed 1.3 whatever the model does**. The Java measured it, got 1.26 (97% of the
arithmetic ceiling) and recorded a failure to reproduce. E5–E7 likewise thread on the ratio
of `goodCost`, not efficiency; price moves from both ends at once when symmetry breaks,
which is why it threads and efficiency does not.

Nothing was tuned and no model behaviour changed. CODE_ANATOMY §3.2. Two lessons worth the
trouble: **a figure label is not a variable name**, and **check a statistic's arithmetic
ceiling before concluding a model is weak** — that bound was a minute's work at any point.

E8 also revealed a config error inherited from the Java: the shipped
`TransmissionPlusDensity1.properties` says `width = 2`, but the Java build used width 1,
which puts the two Firms exactly on the space corners and clips the settlement that forms
around the winner. At the shipped width the ratio passes through 13 during the transition
and settles near 33; the transition itself is near-vertical, flat at ~1.0 until ρ≈0.88.

**The undamped belt run never settles — and the Java README said it did.** That README
recorded E1raw as "settles at 0.299, stock never clears". Re-running the same Java build
gives 0.42 at the same day, and the last 100 days swing 0.16–1.12. It was a single sample
read as a resting point. The thesis calls this an undamped harmonic oscillator, and an
undamped oscillator is exactly the thing that never settles — so the recorded claim
accidentally contradicted the mechanism it was demonstrating. Corrected in
`../spatialecon-java/README.md`.

Both implementations oscillate forever and differ only in amplitude, which follows from the
randomised starting price since an undamped oscillator keeps its initial displacement. The
test here asserts the *swing over a window* is non-zero undamped and zero damped, rather
than any sampled value — assert the behaviour, not a number you happened to read.

**The performance estimate in BUILD_PLANNER §3.4 was wrong, and it mattered.** It called C1
"a few hundred thousand floating-point operations per day… that is nothing". In fact each of
500 People evaluates ~21 candidate Bundles, and each Bundle scanned the whole population
twice — **10.5 million distance calculations a day**, or 14 days/sec. Fine headless,
hopeless on a canvas. The Java has the same complexity and managed 41 days/sec, so this was
never a language problem.

Fixed with a uniform grid over People, cells one density-radius across, so a query visits
the nine cells that could contain a neighbour instead of all 500 — plus folding the two
density passes into one. **124 days/sec, ~9× faster and 3× quicker than the Java.** It
changes no result: a test asserts the indexed query returns exactly what a brute-force scan
does, to 1e-12, over random probes.

The cost is that an index needs maintaining. `world.movePerson` keeps it correct; anything
going round it is repaired at the start of the next day. Two tests cover both paths, because
a stale index would not error — it would just quietly return the wrong neighbours, which is
the same failure mode as the cost-propagation trap above.

**The von Thünen figures needed a palette the source cannot produce.** C6 and C6c draw each
cost group as its own shape and grey — square, circle, triangle, pentagon, light to dark
(figures 5.15, 5.16) — because four greys alone do not separate in print and the *group* is
the result in these models. But `gl.nextActorColour` as left in the source builds only two
greys and gives every Firm past the second a **random RGB colour**, which cannot be what the
figures show. The commented-out four-entry list just above it (220, 190, 160, 130) matches
them exactly, so that is what is used. CODE_ANATOMY §5.9 — the drawing code turns out to
carry the same comment-and-recompile disease as the scenario wiring.

The view picks this mode automatically wherever People are tied to Firms, since a density
ramp would hide the rings; `shade People by` overrides it.

**Views are tested headlessly.** `test/view.test.js` runs the real `planeView` against a
recording canvas stub, checking that every Person gets a circle and a marker, that the Firm
glyph has its twelve vertices, that no coordinate is NaN or off-canvas, and that
zero-utility People get the white circle. `tools/snapshot.mjs` reuses the same trick to
replay those calls as SVG, so a model can be rendered without a browser. Neither can say the
picture *looks* right — that still needs eyes on `model.html` — but they catch the failures
that actually happen.
