# Build log

**What this is.** The record of what was actually built, in order, and what it taught — including the things that were got wrong and corrected. The other root documents are all *statements of current truth*: [BUILD_PLANNER.md](BUILD_PLANNER.md) says what the plan is, [CODE_ANATOMY.md](CODE_ANATOMY.md) says how the originals work, [THESIS_DIGEST.md](THESIS_DIGEST.md) says what the models show. None of them keeps the history, and the history is where the judgement calls and the wrong turns live.

**What it is not.** Not a changelog of files touched. An entry earns its place by recording a *decision*, a *finding*, or an *error* — something a reader would otherwise have to re-derive or re-discover.

Findings that changed a published claim are cross-referenced to wherever they now live; this log is not their home, only their provenance.

---

## Phase 0 — the Java reference implementation *(July 2026)*

`spatialecon-java/` built as a clean reimplementation of the 2013 framework: 28 models as self-describing `Scenario` classes, 55 tests, a dozen thesis claims pinned to numbers. It does not ship. Its job was to turn the thesis from prose plus an unreliable source tree into a verified specification, so that the JavaScript port would be a translation of something known rather than a second archaeology.

Four collapses did the work: 131 static fields → an immutable config; 29 `*MouseChanger` classes → one `Parameter`; 63 bucket/store classes → one `Recorder`; ten `FirmAct*` → two behaviours, only one of which was ever wired in.

**Then:** `CODE_ANATOMY.md` revised from what the refactor taught (sections marked ↻), and `BUILD_PLANNER.md` written for the JS port.

**Dan's five decisions** (BUILD_PLANNER §9). Two changed the plan materially:

- **Sweeps are precomputed and ship as data.** Removed in-browser 8,000-day runs, progress bars and worker threads from the plan entirely.
- **No bit-exact parity, no shared random stream.** This deleted a whole planned stage — the one that would have made both languages consume the same PRNG so parity could be a strict numerical diff. Verification became two-tier instead (§6): the RNG-free economics diffed numerically, everything stochastic asserted *by claim in a window* ("E3 breaks with ρ in [0.70, 0.80]", never "at 0.764").

---

## Stages 1–2 — the engine *(31 July 2026)*

`rng`, `space`, `econ`, `agents`, `config`, `record`, `world`, the headless runner, and tier-1 verification against the Java. Zero dependencies, no build step, `// @ts-check` + JSDoc.

**Tier 1 came out exact.** Six deterministic models agree with the Java at 0.00e+0 relative deviation across every column, at the six significant figures both sides write. On a different PRNG, in a different language.

**The performance estimate in BUILD_PLANNER §3.4 was wrong.** It said "a few hundred thousand FLOPs per day… that is nothing". C1 ran at 14 days/sec — 106 seconds for a 1,500-day settlement. The real cost is 500 People × 21 Bundles × 500 People × 2 passes ≈ 10.5M distance calculations per day. Fixed with a uniform grid index over the density radius and by merging the two passes into one; 124 days/sec after. A test asserts the indexed query equals brute force to 1e-12.

**And the index immediately went stale.** A test placing agents with `placeAt` failed the moment the grid existed. Fixed with `world.movePerson()` as the only sanctioned move, plus a `Timeline.onDayStart` hook that reindexes — belt and braces, because the two failure modes are different: one is a caller bypassing the API, the other is legitimate direct placement between days. Two tests, one per path.

---

## Stage 3 — behaviours, the plane, and the first live model *(31 July 2026)*

`behaviour.js`, groups B/C/D, `view/plane.js`, `view/chrome.js`, C1 running on a canvas with working controls. The first point at which "does this feel right?" had an answer.

**The palette in the source cannot have produced figures 5.15 and 5.16.** `gl.nextActorColour()` as left builds its list with a loop yielding two entries — black and mid-grey — then hands every Firm past the second a *random RGB colour*. With four Firms the published figures would show two greys and two arbitrary colours, which they plainly do not. Directly above, commented out, sits a four-entry ramp (220, 190, 160, 130) matching the published light-to-dark exactly. The drawing code carries the same disease as the scenario wiring: the snapshot is not the state any figure was made in. → CODE_ANATOMY §5.9.

**Two bug reports from Dan, both valid:**

1. An unlabelled slider under the buttons that did nothing. It was the speed control, living in the button row with only a `title`, and being wiped by `renderControls()` on every rebuild. Moved to a separate persistent container with a proper label and live value.
2. The von Thünen group markers were missing. Each wage level should get its own shape *and* grey — 0.5 white square, 1 light-grey circle, 1.5 grey triangle, 2 darker pentagon — with the same split applied to commute cost in the following figure. Added as `GROUP_GREYS` + `GROUP_SHAPES` with a legend that names whichever cost is actually varying, and an `auto` shading mode that picks groups whenever People are tied to Firms.

**`tools/snapshot.mjs` was lying about the fix.** Its `--shade` default was `density`, which overrode `auto`, so the first regenerated C6 looked unchanged and the group markers appeared not to work. Default changed to `auto`.

---

## Stage 4 — the transmission belt *(31 July 2026)*

`view/line.js`, `view/timeseries.js`, E1, E3, E7. The belt is the highest-risk part of the port — granular per-*transaction* stock targeting with damping on the first and second differences — and it landed on arithmetic: clearing price 0.50000 with stock at 4e-15, and 0.16667 for 20 Firms and 100 People, against values deducible by hand.

**A correction to the Java record.** `spatialecon-java/README.md` had E1raw (the undamped variant) as "settles at 0.299, stock never clears". Re-running the Java gave 0.42 at the same day, swinging between 0.16 and 1.12 over the last hundred days. It does not settle at all — which is the *point* of the variant, since the undamped oscillator is what the damping exists to fix. Both READMEs corrected. The JS test now asserts the swing over a window rather than sampling a value, because sampling a value is what produced the wrong claim in the first place.

---

## Stage 5 — E5, E6, E8, and the magnitude shortfalls *(31 July 2026)*

The headline result of the whole port.

**Three apparent failures to reproduce were one measurement error, and it was not in the models.** E8's ratio reaching 1.26 against the thesis' ~13; E6 crossing the regime boundary once in 20,000 days; E7's welfare claim holding only as a trend. All three were one-sided — too weak, never too strong — which is the shape of a systematic error rather than three unrelated weaknesses.

Decompiling the shipped jars one level further, to the arguments each `VariableStore` was constructed with, resolved all three:

- **E8's "Firm efficiency ratio" is not an efficiency ratio.** It reads `productionLevel`, total output `f(T) = vT² + mT`. The obvious reading — output per unit, which the same build also records — is `vT + m`, and with the shipped `EoS_Curve = 0.001, EoS_Magnitude = 1` that is `0.001·T + 1`. With 300 People, `T ≤ 300`, so **the per-unit ratio cannot exceed 1.3 by arithmetic**, whatever the model does. Measuring 1.26 was reaching 97% of a ceiling and recording it as a failure.
- **E5/E6/E7 thread on the ratio of prices**, not efficiency. Price moves from both ends when symmetry breaks — the winner's falls, the loser's climbs — so it is far the more responsive trigger.

After correction: E8 settles at **13.55 at ρ = 0.9**, the thesis value at the thesis parameter, rather than needing ρ = 0.97. E6 threads 3 crossings in 8,000 days against 1 in 20,000. → CODE_ANATOMY §3.2, §10.

**The general lesson, and it is the one worth keeping: a figure label is not a variable name.** Only the jars say which field a store actually read.

**Also found:** the shipped `TransmissionPlusDensity1.properties` says `width = 2`, but the Java E8 used `width(1.0)` — which puts two Firms a distance of 1 apart exactly on the space corners and clips the settlement that forms around the winner. The JS uses the shipped width.

---

## Stage 5b — the views group A never had *(2 August 2026)*

`view/curve.js`, `view/correlate.js`, `view/axis.js`. Stage 5 had been reported complete while these were outstanding, and they were named as outstanding rather than quietly folded in.

**Group A had no view at all.** A1–A4 ran headless and wrote CSV, which is fine for verification and useless as a page. They needed a *function* plot — the x axis is the swept parameter, never the day.

**Two deliberate departures from the original, both recorded in BUILD_PLANNER §4.2:**

- **Log y on A1 and A4.** A1's utility is `Y·n^(1/ρ−1)`, which at ρ = 0.01 with three goods is 3⁹⁹ ≈ 1.7e47. On a linear axis that single value flattens the other ninety-eight days of the sweep onto the x rule, so the chart shows a model doing nothing and then exploding — the opposite of the claim. A4 spans 0.2 down to 1e-8 and the part that matters (the spread never quite reaching zero, which is where D2's demand noise comes from) is at the small end. **A3 stays linear**: its point is a curve crossing *below* zero, which a log axis cannot draw.
- E8's second panel is an **arrow scatter**, not a plain scatter. A scatter shows that production concentration and mean distance are correlated. A track joined in time order shows something much stronger — that the pair sits still for two thousand days and then turns a corner. Correlation is what a scatter shows; direction of travel is what E8 is about.

**The group A sweeps were unclamped.** `t = day / (DAYS - 1)` with no ceiling — harmless headless, where the run stops at `defaultDays`, but a page keeps going. A1 walked ρ through zero into the negative, where CES is not merely wrong but meaningless; A4 kept raising the hill-climber's iteration budget, so the page got slower the longer it was left open. All four clamped, and later B1, B2, D2 and E3 for the same reason.

**Two fixes that came out of the refactor rather than the feature:**

- `snapshot.mjs` was deciding for itself which view a model got, duplicating `chooseView`. Pointed at `chooseView` instead — and that made **time-series models render for the first time**, because the tool had only ever drawn the final frame and an accumulating view has nothing in it at that point.
- Which immediately exposed that `timeseries.js` reserved a fixed 11% of width for its right-hand labels, so "clearing price" was drawn off the edge of every belt model; and that converging series wrote their labels on top of each other, which is precisely what a *settling* model looks like. Both fixed with a shared `spread()` helper. E3's chart is the payoff: the symmetry break, the two prices separating, and the efficiency ratio leaving 1.0 at the same instant, against the deduced 2/3.
- `chrome.js` never reset accumulating views on reset/reroll, so the discarded run stayed on the chart with the new one drawn over it.

---

## Stage 6 — the views the original never published *(2 August 2026)*

`view/buylines.js`, `view/equalise.js`, `view/sampler.js` + the F4 scenario, `view/optimiser.js` as A4's second panel.

**The C1 fringe reproduces a third time, and the estimator is the whole game.** Drawing the equalising cutoff forced a commitment to how U\* is estimated from a run. The utility distribution is a spike at U\* — everyone in the interior, who all reach the same level by construction — plus a left tail of fringe People, so for any fringe under half the population the **median** is U\*, and it does not move as the tail lengthens. Estimating U\* as the *maximum* is the obvious choice and is wrong: it picks the luckiest draw.

| People | Java, measured | JS, median U\* | JS, max U\* |
|---|---|---|---|
| 125 | 43.2% | 44.0% | 93.6% |
| 250 | 20.4% | 20.0% | 48.0% |
| 500 | 7.2% | 7.6% | 18.6% |
| 1000 | 1.9% | 2.0% | 4.9% |

The median column tracks the Java to within a point at every population, from code sharing no line with it. The max column matches nothing. → CODE_ANATOMY §5.3.

**F4 needed tuning to teach, and the tuning is a finding.** At density radius 1/8 the circles blanket a unit space, everyone pays crowding everywhere, and the settlement flees to the boundary where there are fewer neighbours — true, and exactly C8's mechanism, but it drags the followed Person off the edge of the frame. At 1/12 with a cost of 60, about a fifth of sampled Bundles come back unaffordable while the population stays interior and legible.

**And F4 gave away something the prose does not say.** The affordable Bundles split into a large group all worth within a few per cent of each other and a handful much worse. So the decision is nearly always between near-equivalents settled by a small margin — which is why the spatial equilibrium is loose rather than sharp, and why a little demand noise is enough to make a Firm misread it. Written into the model's notes and its *what we found* panel.

**A design error caught by looking:** the chosen Bundle was drawn as the largest, darkest disc *and then* covered with a filled marker, which made it look smaller than its near-equals — reading as though the Person had picked wrongly. The ring alone does the identification.

**`index.html` was stale** — still listing only groups A–D and marking everything outside the plane models "needs its own view", which had not been true since 5b.

---

## Stage 7 — the site *(2 August 2026)*

`tools/sweep.mjs` and five committed sweeps, `view/sweep.js`, `src/content.js`, walkthroughs, the catalogue, capture, mobile.

**`paintCurves` was split out of `curve.js`** so a precomputed sweep and a live chart are the same renderer rather than two that resemble each other — which is what BUILD_PLANNER §7.3 asks for and would not have survived being two code paths.

**The E3 sweep is sharper than the ramped run, and says why.** Holding ρ still instead of ramping it, the efficiency ratio is 1.000 to three decimals up to 0.73, first departs at 0.74, and has clearly gone by 0.75 — where the thesis puts the break. The ramped runs break at 0.766 (here) and 0.764 (Java). Both ramps lag, which is what a ramp through a threshold does. **So the ramp tells you the model shifts and the sweep tells you where**, and that is the argument for precomputing sweeps rather than showing a ramp and calling it the same thing.

**The D1 sweep contradicted the note written for it, and the note was wrong.** The assumption was that the Firms wander most where the incentive is strongest. It is the reverse: at ρ = 0.99 the wander is 0.155 at a distance cost of 1 and falls to 0.07 by 20. When goods are near-substitutes and distance is cheap, a Firm's position barely affects its revenue, the crawl has almost no signal to follow, and the Firms roam the whole line. **The weak-incentive corner is the unreliable one.** Corrected before it shipped.

**And the first version of that sweep measured nothing.** One run per point, separation read off the final day — of a process the thesis explicitly says never settles, because the crawl always perturbs. It gave a non-monotone scatter with no trend. Rewritten to average over the second half of each run and over several seeds, and to plot the *wandering* alongside the separation, since the wandering is the §5.5.2 result rather than error in the measurement. Distance cost 0 was dropped: with no distance cost a Firm's location cannot affect its revenue at all, so nothing moves and the "result" is just where the Firms were placed.

**A test caught seven walkthrough predicates breaking the project's own rule.** BUILD_PLANNER §3.2 says captions fire on model *state*, never day numbers, because a day number is pinned to one trajectory and that breaks the reroll button. Of the seven: four were openers (now `() => true`); two were C1 startup guards, replaced with a state test — utility is zero until a Person acts, which says "the run has started" without a day; three are genuinely *about* elapsed time ("two thousand days in and the Firms are still moving"), which holds under every seed because they never settle under any of them. Those declare `elapsed: true`, and the test now fails on any undeclared `world.day` read. **The rule went from a convention to something enforced.**

**`.gitignore` would have silently excluded the sweeps.** A blanket `data/` — correct for headless CSV output, fatal for the JSON the site fetches. Now `data/*` with `!data/sweeps/`, and the reason is in the file, because a static site with no build step means anything a page loads has to already be committed.

**Catalogue stills cost a paragraph of thought.** These are vector, so file size follows shape count and coordinate length, not pixel dimensions: a settled 500-Person plane is a thousand shapes however small it is drawn, and E3's time series is one path with six thousand points. A naïve set was several megabytes on one page. Integer precision at thumbnail size, dropping repeated identical path commands, and omitting the density rings (grey mush at that scale) took E3 from 136K to 12K and the whole set to 724K, lazily loaded.

---

## Stage 8 — the rest of the catalogue *(2 August 2026)*

C3's four costs, C4, C5, C7, E2, E4, F1–F3 and G1 (`src/coremodel.js`); `view/distribution.js` and `view/coremodel.js` for the shapes no existing view could carry; two more sweeps, `c3-costs` and `g1-tomahawk`; and `methodology.html`, written rather than built, as decision 3 asked. Thirteen models, which completes the catalogue: 42 registered against its 24 entries, because several entries ship as families.

**Over half the tail is not agent models, and they are `Scenario`s anyway.** C4 and C5 are closed forms, E4 is a difference equation, F1 and F2 demonstrate a process, F3 runs two populations of its own, and G1 is solved rather than simulated. Making each a `Scenario` costs a placeholder config and buys a page, controls, pop-outs and a catalogue card from the same machinery as everything else. None has a Java counterpart to diff against, so `test/catalogue.test.js` asserts the claim each model exists to make, in the thesis' own terms. That is tier 2 without the second implementation: "the more rational agent performs worse" becomes "the rational population's output stays at zero".

**C3 and C7 are reconstructed, and everything they publish reproduces.** Both configs are lost (CODE_ANATOMY §4). Each is built on C1's population, space and density cost, on the grounds that each is C1 with one thing moved. C7's two distance costs come from a base of 2 with `rangeDelivery` on, which is what that flag exists for. All four of C3's signs come out as the thesis states, measured as mean distance from the centre over the rising pass:

| Cost | Swept | Mean distance | Direction |
|---|---|---|---|
| wage | 0.05 → 1.5 | 0.550 → 0.238 | agglomerating |
| base good cost | 0.05 → 2 | 0.268 → 0.313 | **dispersive** |
| commute | 0 → 1 | 0.307 → 0.156 | agglomerating |
| delivery | 0 → 1 | 0.733 → 0.181 | agglomerating |

C7's core swap reproduces too. The population sits 0.37 nearer the cheap Firm at ρ = 0.99, is down to 0.10 by ρ = 0.52, and sits 0.49 nearer the dear one at ρ = 0.05. The return sweep crosses at about the same place, with modest hysteresis. Both models are flagged in-file as reconstructed. Reproducing the directions does not recover the configs, so they stay under *Still open*.

**G1: the original's tomahawk has no blades.** The original finds equilibria by walking each short-run curve for sign changes in the real wage ratio minus one. The corners λ = 0 and λ = 1 are equilibria by construction rather than by crossing: with every worker already in one region there is nobody left to move. So the original cannot see them, and its long-run figure is drawn without the two stable branches that give it its name. The port adds them, applying the same stability test at the ends, and flags this as a deliberate departure. Two smaller choices go opposite ways:

- **Kept:** the original reads region 2's income and price index on the first pass before assigning them, so Java's zero-initialised arrays supply zero. The fixed point does not depend on it, and tidying is how a port stops matching.
- **Added:** an iteration cap, which the original lacks, because a frozen tab is a worse failure than a slightly wrong point.

At the shipped T = 2.25 the port finds the five equilibria the thesis states: 0.092 unstable, 0.500 stable, 0.910 unstable, and the two stable corners. The break point is near T = 1.9 and the sustain point near 2.5. The tomahawk sweep is a scatter, not a curve: at one transport cost there may be one equilibrium or three, so there is no function to draw, and joining the points in order would invent branches.

**F3: "drop α to 1 and it frees up" was wrong.** At α = 1 a lone worker produces (1 − 1)/1 = 0, which is no better than not working, so a population of correct reasoners still never starts. Only α = 0 breaks the deadlock. That sharpens the model's point rather than weakening it: the hole is the existence of the threshold, not its size, and any positive fixed labour requirement stops correct reasoners from ever starting. Corrected in the model's notes and pinned by a test that sets α = 1 and asserts zero output.

**E2: an unfinished run read as an effect.** The coupling between one Firm's markup and the other's price *is* the economies of scale, so it should vanish at zero EoS curve. At 2,000 days it read 0.002, which is most of what the real coupling is worth. But at zero curve the equilibrium price is 1.0 and these runs start near 0.66, so they were still travelling. At 6,000 days the drag is 0.0000003, against 0.0026 at curve 0.5 and 0.0047 at curve 1. This is the same error as E1raw's "settles at 0.299": a sample read as a resting point. The test now runs 6,000 days and its comment says why.

**E2's two endings depend on how the markup arrives.** The scenario both drives the markup (a slow ramp to 0.15) and exposes it as a slider. That needs a latch, or the ramp overwrites the slider the next day and the control appears to work while doing nothing. With the latch, the two paths reach the thesis' two endings:

- **Left to ramp:** Firm 0 adapts as its target rises under it, and you get the squeeze. Its price climbs to 1.27, Firm 1's falls to 0.76, and utility slides from 2.5 to 2.1.
- **Dropped straight onto 0.15:** you get the runaway. Firm 0's price reaches about five thousand and it sells nothing, Firm 1 becomes the sole producer at the one-Firm clearing price of 0.667, and utility falls to 1.50.

**E4 does what E3 does, at the same threshold, with the agents taken away.** E4 is four pairs of numbers and one law of motion, with both Firms updated simultaneously from the same state so that turn order cannot be blamed. Starting from a perturbation of 0.002 and running 4,000 steps:

- **ρ = 0.73:** the run decays back to symmetry, with asymmetry 5.5e-7 and both prices at 0.6667.
- **ρ = 0.80:** asymmetry grows to 0.38 and holds. Prices settle at 0.527 and 0.907, and one Firm produces fifteen times the other.

So the instability leads to a new pair of equilibria, not a runaway, and the threshold lies between 0.73 and 0.76. That agrees with the E3 sweep's first departure at 0.74. Convergence takes thousands of steps against the thesis' sixty, because the constant in the law of motion is a free choice and this one is smaller. It changes the speed and not the threshold.

**F2's pyramid was first drawn as a ramp.** The model bins ten million gaps into counts as they happen. Handing those counts to the distribution view's `sorted` mode ranked them, which turned a pyramid centred on one day into a descending ramp: a picture of the opposite of the finding. A `bars` mode now plots already-binned values in the order given. Only 19% of gaps land within a tenth of a day of the mean, where a spike would put all of them.

**F1 is still climbing at the thesis' horizon.** The Gini coefficient is 0.33 at the thesis' 200,000 exchanges. Run five times longer and it saturates at 0.48, close to the limit of the exponential distribution this process tends towards.

---

## Publishing *(13 October 2026)*

The site went live at [danolner.github.io/phd_live](https://danolner.github.io/phd_live/), and a root `README.md` now points the repo at it. Four decisions:

- **Pages serves `spatialecon-js/` as it sits.** `.github/workflows/deploy-pages.yml` uploads that one folder as the Pages artifact. There is no move into `docs/` and no `gh-pages` branch, and nothing else in the repo is published. Edits outside the folder do not trigger a redeploy.
- **The workflow regenerates nothing.** The sweeps take minutes and the site is meant to be the committed files, so `npm run sweep` and `npm run thumbs` run locally before a push.
- **It listens on both `main` and `master`.** Git with no `init.defaultBranch` creates `master` while GitHub suggests `main`. A workflow listening on the wrong branch never runs and never says so.
- **Every path is relative and case-exact.** macOS forgives a case mismatch; Pages does not.

---

## Stage 9 — a curiosity: H1, green blob, red blob *(13 October 2026)*

A new group, **H · curiosities**, for runs from the years of building the framework that never reached the thesis. Its first entry reconstructs a 33-second video Dan recorded on 13 August 2011 and uploaded as "Green blob red blob". [GREEN_BLOB_RED_BLOB.md](GREEN_BLOB_RED_BLOB.md) has the full case: the frames measured, the 2011 drawing code read, figures from the same fortnight, and the variants tested.

**The video's colours were not its two cities.** The 2011 renderer, still dormant in `SpaceTools.draw3D`, stroked every Person on a min–max utility ramp: red for the highest utility in the population that frame, green for the lowest. The proof is in the first three frames, which are all red. The range starts at 0/0 and is updated only once every Person's utility is positive. The white ring between the settlements is `drawMeanCentrePoint()`, the mean position of every actor. → GREEN_BLOB_RED_BLOB.md §2.

**The reconstruction: work tied, goods open.** Two crawling Firms, each with a workforce tied to it for work only. Everyone buys both goods (ρ 0.6), everyone pays a density cost, and Firm 1 pays twice the wage. Scored with the statistics measured on the video, it matches four things at about one model day per frame: the chase direction, the share of time apart, the share of time engulfed, and the cloud scale. Each ingredient is necessary. Equal wages, fixed Firms, both channels tied, or a premium attached to the Person rather than the Firm each stop the chase. The runner-up has goods tied and work open, which is the wiring of the one surviving 2011 preset. It chases harder but never parts or engulfs. → GREEN_BLOB_RED_BLOB.md §4–5.

**Two engine additions, and a UI seam promised since stage 2:**

- **`config.tie`** closes the work and goods channels independently, and C6 is unchanged at `'both'`. The 2013 snapshot could not express a work-only tie from a properties file, because any tie flag switched on `vonThunenMode`, which tied goods. Its separate per-channel flags suggest the 2011 code could.
- **`config.rangeStep`** spaces the ranged costs; a step of 1 is the original's.
- **`chrome.js` now draws boolean and choice Parameters** as a checkbox and a select. `Parameter.kind` had carried the distinction since stage 2 without the page using it, and F2's toggle became a checkbox as a side effect.

**An error caught: walls read as speed.** Left to run, at least one settlement sits near a wall on 77–94% of days, against 5% of the video's frames. The first reading was that the model wandered about three times faster than the video, which was wrong. Measured properly, as net drift rather than displacement between snapshots, the rates agree: 0.42 of the window over the video's 33 seconds, and 0.25–0.43 per 1,000 days here. The video is a clip from mid-field. Walls are where any long run of this dynamic ends up, because an accelerating breakaway keeps going until something stops it, and the original's space was bounded the same way. Two alternatives were tried and rejected:

- a wider space, which only delays the walls;
- removing the crawl's acceleration, which stops the settlements ever coming apart.

So the walls are kept, and the page says so.

**The catalogue still is chosen, not settled.** Nothing in H1 settles, so `defaultDays` is set to 2,555, where the default seed's frame is a mid-field engulfment. All five walkthrough captions fire by day 544.

Not reproduced: the pace. The model cycles about half as often as the video. → *Still open*.

---

## Corrections to the record

Places where building something changed a claim in an existing document. Listed together because each is the kind of thing that gets silently re-derived otherwise.

| Correction | Was | Now | Lives in |
|---|---|---|---|
| E1raw, the undamped belt | "settles at 0.299" | never settles; swings 0.16–1.12 | both READMEs |
| The three magnitude shortfalls | three weak reproductions | one measurement error — wrong variable | CODE_ANATOMY §3.2, §10 |
| C1's fringe | a puzzle in the reimplementation | a finite-population boundary effect, measured three times | CODE_ANATOMY §5.3 |
| The estimator for U\* | (unstated) | median, not maximum; the difference is 7.6% vs 18.6% | CODE_ANATOMY §5.3 |
| The source's actor palette | assumed to be what the figures used | cannot be; use the commented-out ramp | CODE_ANATOMY §5.9 |
| Port performance | "that is nothing" | 10.5M distance calcs/day; needs a spatial index | this log, stages 1–2 |
| E8's space width | Java used 1 | shipped config says 2; 1 clips the settlement | CODE_ANATOMY §3.2 |
| The tomahawk's blades | not drawn — the original detects crossings only | corner equilibria computed and added | this log, stage 8 |
| F3's threshold | "drop α to 1 and it frees up" | α = 1 still deadlocks; only 0 breaks it | this log, stage 8 |
| E2's EoS coupling at zero curve | 0.002 at 2,000 days | 0.0000003 once settled | this log, stage 8 |

---

## Still open

Not resolved by anything built so far, and none of it blocks the site:

- **D2's ρ dependence.** The thesis reports low ρ making the DMR game harder under noise; both independent rebuilds show the opposite, each with a coherent mechanism for its own result. Noise amplitude, distance cost and noise character all ruled out. CODE_ANATOMY §7.12. It is on the site, in D2's *what we found* panel, because a rebuild that quietly reproduced everything would be less informative than one that does not.
- **The commute round-trip factor of 2** (in the thesis equation, not the code) and **noise amplitude** (±half what the thesis text implies). Both switchable behind `Quirks`; neither has been shown to change a published result.
- **C3 and C7 configs are lost.** They are reconstructed from the thesis text on C1's population and space, with the `*Range` conventions, and every published direction reproduces (stage 8). The exact configs are still not recoverable.
- **H1's 2011 configuration is lost too.** The reconstruction reproduces the chase, the shares of time apart and engulfed, and the cloud scale, but it cycles at about half the video's rate. Three things are not recoverable: which Firm crawl was live on 13 August 2011, the wage ratio, and the People count. GREEN_BLOB_RED_BLOB.md §6 and §8 list the candidates, and the questions only Dan can answer.

Two content errors on the live site, found on 13 October 2026 while writing up stage 8. In both, a pop-out still carries a claim that stage 8 corrected everywhere else:

- **F3's *under the hood* pop-out reverses the α correction.** `src/content.js` says "Drag alpha down to 1 and the rational population starts producing immediately." The model's notes and a passing test both say α = 1 still deadlocks and only α = 0 breaks it (stage 8). A visitor to the F3 page is shown both claims.
- **E2's *what we found* table is measured on unsettled runs.** It gives the drag after 3,000 days, including −0.0003 at zero EoS curve. That is the same unfinished-run error stage 8 records. The scenario notes have the settled 6,000-day values (0.0000003, 0.0026, 0.0047), and the pop-out should match them.

---

## Where it stands

**All eight stages of BUILD_PLANNER §8 are done, and the site is live.** There are forty-two models covering the whole catalogue and all seven groups, plus one curiosity in group H, and every one has a view and a page. 154 tests pass, and tier 1 is exact on six deterministic models. The site has:

- a catalogue with a still per model;
- per-model pop-outs in three kinds;
- walkthroughs that survive a reroll;
- seven precomputed sweeps rendered through the live charts' own code;
- a methodology page carrying the §7.4 argument.

What remains is the *Still open* list above.
