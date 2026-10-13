# Build planner: moving to JavaScript

**Companion to** [THESIS_DIGEST.md](THESIS_DIGEST.md) (what the models show), [CODE_ANATOMY.md](CODE_ANATOMY.md) (how the originals were built), [BUILD_LOG.md](BUILD_LOG.md) (what was built, and what it taught) and [spatialecon-java/README.md](spatialecon-java/README.md) (what the reimplementation verified).

This is the plan for the thing we are actually shipping: a website where each thesis model runs live in the browser, looks like the figures it came from, and can be taken apart.

---

## 1. Where we are

The Java refactor did its job. 28 models are built as self-describing `Scenario` classes, 55 tests pass, and a dozen thesis claims have been reproduced numerically — including four transmission-belt equilibria that agree with hand arithmetic to five decimal places. Three things the thesis left ambiguous were settled along the way (CODE_ANATOMY §9), and one genuine disagreement was found (§7.12).

**So the port is not a translation of the 2013 code.** It is a translation of a verified specification: every model arrives with its claims already pinned to numbers, which is a materially easier problem than the one the Java refactor faced. Worth not squandering by rushing the engine to get to the pictures.

Two things remain unbuilt in Java and should be built in JavaScript instead, not both:

- **F4 (bundle sampling)** is a picture of one Person deliberating. It has no headless form.
- **The interaction layer.** The Java `Parameter` seam exists and is exercised, but nothing binds it to a mouse. Doing that twice would be waste.

**The Java does not ship** (decision 5). It stays in the repo as the reference implementation, as linked background reading, and as the record of what was verified and how. Nothing on the site runs it or depends on it, and it never needs a UI. From Stage 1 the JavaScript is the live artefact; the Java is provenance.

---

## 2. Constraints, and what follows from them

**Runs from a static file.** No server, no build step, no install. Open `index.html` and the model runs. This keeps deployment trivial, makes the whole thing archivable, and means the site outlives whatever tooling is fashionable — which matters for a thesis from 2013 being revived in 2026.

**Zero runtime dependencies.** Not a preference, a design constraint with consequences worth stating:

| Tempting | Why not | Instead |
|---|---|---|
| React / Svelte | The DOM here is a handful of controls. The model is a canvas. A framework buys nothing and costs a build step. | ~150 lines of DOM wiring |
| D3 | Used for scales and axes; we need neither — the original's graphs are deliberately spare | ~40 lines of tick maths |
| Chart.js / uPlot | Would fight us on the aesthetic, which is the whole point (§4) | write the two graph types, ~350 lines |
| A bundler | ES modules are native everywhere. `<script type="module">` is enough | nothing |
| TypeScript | A compile step for the whole project | `// @ts-check` + JSDoc types: full editor checking, zero build |

That last one is worth doing from line one. JSDoc annotations with `@ts-check` catch the class of error that actually bites in a numerical port — a wrong argument order, a mistyped field name — and cost nothing at runtime.

**One dependency is unavoidable and already present:** the browser's Canvas 2D API. It does everything Processing did for us here.

---

## 3. Porting the engine

### 3.1 What maps how

Java's ceremony is most of its line count and none of its meaning. The collapse:

| Java | JavaScript | Note |
|---|---|---|
| `interface Utility` + `Ces implements` | factory returning `{quantities, utility, rho}` | closures over `rho`; no `implements` |
| `ModelConfig.builder()...build()` | object literal + `defineModel(spec)` filling defaults | ~200 lines → ~40. Biggest single win |
| `record Quirks(...)` | frozen object | |
| `List<Parameter> controls()` | `[{name, get, set, min, max, label}]` | already the shape it wants to be |
| `Scenario` implementations | one exported object per model | `{id, title, thesisRef, config, setUp, record, controls, beforeDay, notes}` unchanged |
| streams | array methods | |
| `Person`, `Firm` | **classes**, fields declared in the constructor | the one place to keep classes: thousands of instances, and stable shapes keep V8's hidden classes stable |

Everything else becomes plain functions over plain data. The scenario objects should port almost verbatim — that is the payoff of having made them declarative in Java.

### 3.2 Determinism

**The two implementations do not share a random stream, and are not meant to** (§9.1). The Java keeps `java.util.Random`; the JS uses a small seeded PRNG of its own — mulberry32, eight lines, fast and reproducible within JavaScript. Nothing about `spatialecon-java` changes.

What seeding is still for: **a fixed run and a reroll**. Same seed, same run, every time — so a visitor can be shown a specific outcome, then press reroll and watch a different Firm win. E5 and E8 need this, because which Firm wins is genuinely chance and that is the point being taught.

Two consequences to design around from the start:

- **Never let a view depend on a specific trajectory.** No hardcoded "on day 412 the left Firm takes off". Captions attach to *events* the model detects (a threshold crossing, a symmetry break), not to day numbers. This is what makes the step-through of §7.2 survive a reroll.
- **Chaotic models diverge and that is correct behaviour.** The DMR crawl and anything threshold-driven will take a different path each seed. What is reproducible is the shape: the split happens, the transition occurs, the ratio climbs.

Even with a shared stream this would mostly have been true — `Math.pow` is not required to be correctly rounded by either the JVM or ECMAScript, and CES leans on it hard — so chasing exact traces was never going to work. Now it is not even attempted.

### 3.3 Module map

```
spatialecon-js/
  index.html                  model index; no build step
  model.html                  one page, ?m=<id> selects the model
  src/
    rng.js            ~30     mulberry32, seeded; fixed run + reroll (§3.2)
    space.js         ~130     Pt, the four Space kinds, distance, Placement
    econ.js          ~230     Ces, StoneGeary, SmoothEoS, CoreModelEoS, both optimisers
    agents.js        ~150     Person, Firm, Bundle, Good
    world.js         ~220     World, Timeline, density cost, the cost choke point (§7.11)
    behaviour.js     ~300     personBundle, personTransmission, firmLocationCrawl, firmStockTarget
    config.js        ~120     defineModel, Parameter, Quirks
    record.js        ~120     Recorder, Series, Aggregation
    scenarios/               one file per model, ~40 lines each
    view/
      plane.js       ~200     People + Firms on the plane            (C1 C2 C6 C8 E8 B3)
      line.js        ~260     the mirrored demand-profile view       (D1 D2 E5 E6 E7)
      buylines.js     ~90     trade flows                            (never published)
      timeseries.js  ~180     auto-scaling series, rolling window    (E1 E2 E3)
      correlate.js   ~180     scatter, time-ordered joins            (C8 E8)
      optimiser.js   ~130     hill-climb trace                       (A4 F4)
      curve.js       ~110     pure function plots                    (A1 A2 A3 B1 B2)
      chrome.js      ~170     controls, readouts, keys, capture
  tools/
    run.mjs                   headless in Node → CSV, same flags as the Java Runner
    check.mjs                 both verification tiers (§6), one table
    sweep.mjs                 generate the precomputed sweep data (§7.3)
```

Engine ~1,310, views ~1,320, scenarios ~1,100. Call it **3,700 lines**, against the original's 40,000. No minifier, no bundler; ES modules load fine over `file://` in every current browser when served, and `python3 -m http.server` covers local work.

### 3.4 Performance

The heaviest published model is C1 at 500 People × 21 bundles per day, each bundle an analytic CES solve over one good — a few hundred thousand floating-point operations per day. That is nothing. The hill-climber (1,000 iterations × goods) is heavy but only appears in A4 and F4, which are single-agent demonstrations.

So: run the model in a `requestAnimationFrame` loop with a days-per-frame control, exactly as the original's `timeDelay` did. **Do not reach for a Web Worker up front.** If one model turns out to need it, a worker is still zero-dependency and the engine's lack of DOM references makes the move mechanical.

---

## 4. The visualisations

The brief is "as close to the originals as they can be", and the originals are more considered than they first look. CODE_ANATOMY §5.9 documents the vocabulary; this is what to do with it.

Reference figures live in `ThesisCorrectionsLatex/ChapterTestingModels/Figures/`. The two that matter most:

- `IB_CEUS1_Mon-Dec-03-15_55_58-GMT-2012__MainVizDensity.png` — the plane view
- `MQ_modelday_98111_Hotelling_Line_DMR_…__MainVizrho1del20.png` — the line view

§3.1 of CODE_ANATOMY records which view each published build used. That table is the spec for which model gets which renderer.

### 4.1 Reproduce exactly

**Monochrome.** Black on white, greyscale between. This was not a limitation the author worked around — `Colours.java` is an empty stub, the colour ramps are commented out in favour of lines marked `//b&w version`, and even `Actor.colour` resolves to `new Color(i,i,i)`. The figures read well because of it. Default to it.

**Double encoding.** Marker size *and* greyscale both carry the same variable, over a 10× size range and a full black-to-white ramp. It is the reason the figures survive being printed small, and it is good information design. Keep the exact mapping:

```js
size = map(value, low, high, 0.005, 0.05)      // in model units, not pixels
grey = map(value, low, high, 0, 255)
```

**Density as ink.** On the plane, each Person is drawn twice: a faint circle at the density-cost radius, and a marker at their position. Where People crowd, the circles overlap and the page goes dark — so the crowding measure and the picture of it are the same object, with no legend needed. This is the best idea in the original visualisation and the one most worth being careful with. It is also, conveniently, exactly what makes C1's fringe visible: the ring where the ink runs out is the cutoff distance of CODE_ANATOMY §5.3.

**The line view's mirrored profiles.** Firm 0's demand as bars above the rule, Firm 1's inverted below, one bar per Person at that Person's position, greyscale by distance. Firms sit on the rule as a white pennant with a rectangle above showing rolling mean position ± SD — so a wandering DMR Firm is visibly distinct from a settled one, which is the entire content of D1. Keep the pennant, keep the rolling marker.

**Time-ordered scatter.** The correlation graphs join consecutive datapoints so direction of travel is legible in a scatter. E8's headline figure depends on it.

**Auto-scale with manual reset.** Graphs grow their bounds to fit and only shrink when asked. This is right for a live model — an axis that rescales continuously makes a settling series look like it never settles.

### 4.2 Change deliberately, and say so

| Original | Web | Why |
|---|---|---|
| 4–6 separate OS windows | one page, panels in a grid | The `a` key existed to capture all windows at one instant — i.e. the multi-window layout *was* the figure. A responsive panel grid is the same idea, natively |
| `p` → jpeg to `C://saveframes/` | `canvas.toBlob` → download; one button captures all panels | same behaviour, no hardcoded path |
| Aliased screen capture | `devicePixelRatio` scaling, `imageSmoothingEnabled` off for markers | sharper without altering the design |
| `ThreeDCorrelationGraph` + PeasyCam | drop | one figure, and a dependency for it |
| Fixed 600/1200px windows | fluid canvas, model coordinates normalised to 1 | the model was always in normalised units; only the viewport was fixed |
| b&w only | b&w default, **optional colour toggle** | screens are not print. Offer it; do not default to it |
| Linear axes throughout | **log y on A1 and A4** | A1's utility is 3^(1/ρ−1) and reaches 1.7e47 at ρ = 0.01. On a linear axis that one value flattens the other ninety-eight days of the sweep onto the x rule, so the chart shows a model doing nothing and then exploding — which is the opposite of the claim. A4 spans 0.2 down to 1e-8, and the part that matters (that the spread never quite reaches zero, which is where D2's demand noise comes from) is the small end. A3 stays linear: its point is a curve crossing *below* zero, which a log axis cannot draw at all |

The colour toggle deserves a note. The temptation is to "improve" these figures with colour and it should mostly be resisted: the greyscale ramps carry quantitative information, and colour scales that carry it as well are harder to design than they look. Where colour genuinely helps is *identity* — which Firm is which — and that is what the original's two-grey palette was straining to do. So: greyscale for quantities always, optional hue for Firm identity only.

### 4.3 What to add

Three things the original had but never published, which the web is better at. **All three are built** (stage 6); what follows is why each earns its place.

- **`PeopleBuyLines`** — a line from each Person to each Firm they bought from, weight ∝ quantity. The only view showing trade flows rather than their consequences. Dormant in every jar. Obvious material for explaining what a Bundle *is*.
- **F4, the bundle sampler** — one Person, their sampled bundles, and the utility of each. This is the model's decision procedure made visible and it has no headless form, which is why it is unbuilt.
- **The equalising line on C1** — `U = (1−D(d))/(1+0.25d)` and the point where density hits zero, drawn over the settlement. Turns the fringe finding from a paragraph into a picture.

---

## 5. Interaction

The original's scheme is better than its reputation and should mostly survive. Left and right mouse buttons each bind to a parameter; dragging changes it; `←`/`→` change a sensitivity multiplier in powers of two so one binding covers coarse and fine. The live value prints bottom-left.

**Keep:** drag-to-change on the canvas, the power-of-two multiplier, the always-visible readout, and the single-key actions (CODE_ANATOMY §5.9 has the full table — `k` stop, `m`/`r` rescale, `f` rolling window, `p` capture, `w` export, `↑`/`↓` select agent).

**Add, because a website has visitors who have never used it:** a labelled slider per exposed parameter alongside the drag binding. Same `Parameter` object, two affordances. The sliders make the model self-explanatory; the drag keeps it fast for someone who knows what they are doing.

**Add:** pause/step, reseed, and reset — the original had `modelRunDays = 0` and a kill switch, which suited an experiment station and does not suit a page someone lands on.

**Touch:** drag maps to one finger, the multiplier to pinch. Sliders cover the rest. Worth doing at the start, not retrofitted.

`Scenario.controls(world)` already returns exactly what this needs. The one gap noted in the Java work stands: **`Parameter` is double-only, and several controls are booleans** (view toggles, quirk flags, damping on/off). Widen it to a tagged union — `{kind: 'number'|'boolean'|'choice'}` — in the JS, and the same seam covers sliders, checkboxes and the b&w/colour switch.

---

## 6. Verification

Without a shared random stream (§9.1), "is the JavaScript the same model?" is answered in two tiers rather than by one diff. The split is not a compromise — it is the distinction between the parts of the model that *have* an exactly right answer and the parts that only have a right shape.

**Tier 1 — the deterministic core, checked numerically.** The economics contains no randomness at all: given prices and a budget, CES optimal quantities are a formula; `SmoothEoS(1,1)` at t=0.5 is 0.75; the density taper is arithmetic; `U = Y·n^(1/ρ−1)` is a closed form. These are checked two ways, both cheap:

- against the closed forms directly, which is what the Java's tests already do — port them
- against the Java, by running both headless on an RNG-free path and diffing at `1e-12`

The second catches transcription errors the first cannot: a swapped argument, a sign, `rho/(rho-1)` written the other way up. Group A models are entirely in this tier, which is why they come first in the staging.

**Tier 2 — the stochastic models, checked by claim.** For everything with agents in it, the assertion is the published claim, not the trace. `spatialecon-java/README.md` already holds these as verified numbers, and they port directly into JS tests:

| Claim | Assertion |
|---|---|
| transmission belt, T units of time in | clearing price → `1/(T+1)`, four setups, tolerance 1e-4 |
| B1: ρ→1 gives the nearest Firm everything | demand share = 100% |
| B2: the double dip | rises 0.100 → 0.140, then falls to 0.062 |
| B3: People converge faster than Firms | halving times differ by ~7× |
| C1: utility equalises | mean ≈ 0.9; corr(distance, density) < −0.95 |
| C8: equilibrium breaks under density cost | corr(distance, utility) goes −0.39 → −0.97 |
| D1d: Hotelling split at ρ=0.99, cost 20 | separation > 0.3 |
| E3: symmetry breaks with no space | a break occurs, ρ in [0.70, 0.80] |
| E7: efficiency rises, nobody worse off | minimum utility does not end lower |
| undamped targeting fails | price ≠ clearing, stock never clears |

Note the shape of these: a *window* where the Java records a point (E3 breaks "in [0.70, 0.80]", not "at 0.764"). That is the discipline §3.2 asks for, and it is what makes the suite survive a reseed.

`tools/check.mjs` runs both tiers over all 28 scenarios and prints one table. No framework needed — a 30-line `assert`/`test` pair, or `node --test`.

**Where the two implementations disagree, that is a finding, not a failure.** It was exactly this kind of disagreement between the Java and the thesis that produced CODE_ANATOMY §7.12 and the C1 fringe result. A third implementation disagreeing with the second is worth the same attention — and per decision 4, worth putting on the site.

---

## 7. From models to a website

### 7.1 Pages

Each `Scenario` already carries `id`, `title`, `thesisRef`, `notes` and `controls`. That is most of a page.

- **`index.html`** — the 24 catalogue entries in their seven groups, each a card with title, thesis reference and a still.
- **`model.html?m=c1`** — the model, its canvas panels, its controls, its notes. One page, driven by data.

### 7.2 Pop-outs and step-through

Short expandable panels beside the canvas. Three kinds, and the material for all three already exists:

- *the economics* — from THESIS_DIGEST
- *under the hood* — from CODE_ANATOMY, including the original's own candid comments (`//Hackeroo strikes again!` sits directly on top of a compromise the thesis argues about)
- *what we found* — per decision 4, the reimplementation's results where they complicate the thesis: C1's fringe and why `du/dd = 0` is a large-numbers idealisation; the ρ ≥ 1 branch that was wrong for twelve years and never ran; initial prices being load-bearing; E8's magnitude shortfall; D2's unreproduced ρ direction

**Step-through**: a scenario exposes an ordered list of `{when, caption, focus}` and the page walks it. Note `when` is a *predicate on model state*, not a day number — see §3.2. "When the efficiency ratio first exceeds 1.15" survives a reroll; "on day 412" does not.

### 7.3 Precomputed sweeps

Per decision 1, sweeps ship as data. Several published figures come from parameter sweeps over thousands of days — E5 threads a regime boundary over 8,000 — and re-running those in a visitor's browser buys nothing that a chart does not.

- `tools/sweep.mjs` runs each sweep headless and writes a small JSON per figure (`data/sweeps/e5-boundary.json` and so on). Committed, so the site stays a static file tree.
- The page renders that JSON with the same `correlate.js` / `timeseries.js` used for live output, so a sweep chart and a live chart look like siblings.
- Each sweep chart links to the code that produced it and to the raw JSON. **The generator is the JS runner, not the Java** — a visitor who follows the link should land in the same language the site is written in.
- Where it is cheap, a point on a swept curve can seed the live model: click the curve at ρ = 0.75, get that model running below it. Worth doing for E3 and E5; not worth forcing everywhere.

### 7.4 The methodological argument

Per decision 3, this is written, not built. A short standalone section — decision types (instant / feedback / reflex), the two-bin day and the permutation gap, agent number as an idealisation, functional versus descriptive mapping — cross-linked from the models that illustrate each. No interactives and no engine support required, so it costs nothing structurally and can be written last.

---

## 8. Staging

Each stage is independently reviewable and leaves something runnable.

| Stage | Delivers | Proves |
|---|---|---|
| **1** | `rng`, `space`, `econ`, `agents`, `config`, `record`, `world`; ported unit tests; `tools/run.mjs` | the economics is right, headless, no pictures |
| **2** | `check.mjs` tier 1 green; A1–A4, B1–B3 | the deterministic core is exact before anything stochastic is built on it |
| **3** | `behaviour.js`; tier 2 on C and D groups; **`view/plane.js` + `chrome.js`; C1 live on a canvas with working controls** | the whole chain, end to end, on one model |
| **4** | `view/line.js`, `timeseries.js`; D1, D2, E1, E3, E7 | the second view family, and the belt |
| **5** | E5–E8; both tiers green across all 28 | feature-complete against the Java |
| **5b** | `curve.js`, `correlate.js`, `axis.js`; group A gets a view at last | every model has a picture |
| **6** | `buylines.js`, `optimiser.js`, `sampler.js`, `equalise.js`; F4 | the views the original never published |
| **7** | `sweep.mjs` + precomputed sweep data; `index.html`, pop-outs, step-through, capture, mobile | a website |

**Stage 7, as built.** Five sweeps in `data/sweeps/`, committed and rendered through `paintCurves` — the same function the live charts use, split out of `curve.js` for exactly that. Pop-outs live in `src/content.js` in the three kinds §7.2 asks for, with variants inheriting their family. Walkthroughs are predicates on model state; a test asserts that none reads `world.day` unless it declares `elapsed: true`, which is the enforceable form of the §3.2 rule. Capture composes both panels into one image, as the original's `a` key did. Two sweeps are clickable and set the running model rather than restarting it.
| **8** | remaining catalogue: C3, C5, C7, E2, E4, C4, F1–F3, G1; the methodology write-up | the full 24 |

**All stages complete.** Forty-two registered models against the catalogue's 24 entries, because several entries ship as families — C3's four costs, D1's four cases, E1's three variants. The tail turned out to be over half non-agent models (C4 and C5 closed forms, E4 a difference equation, F1 and F2 process demonstrations, G1 a separate program), which is the catalogue rather than a shortfall; they are `Scenario`s anyway so that every model is a page with the same machinery. §7.4's methodology prose is `methodology.html`. See [BUILD_LOG.md](BUILD_LOG.md) for what each stage taught. One model outside the plan followed: H1, a 2011 video reconstructed as the first of a group of curiosities (BUILD_LOG stage 9).

Stage 3 is the one to reach quickly and then stop and look at, because it is the first point where the answer to "does this feel right?" exists.

*(The original Stage 0 — a shared PRNG across both languages — was deleted by decision 5. §9.1.)*

The stage-8 tail was deliberate, and the reasons held up: C3 and C7 needed reconstruction (CODE_ANATOMY §4) and are flagged in-file as reconstructed; C4, C5, E4, F1 and F2 are not agent models; G1 is a separate program. None of them blocked the site, and all four of C3's published directions and C7's core swap reproduced when they were built.

---

## 9. Decisions, settled

Dan's answers, July 2026. Two of them change the plan materially; the consequences are worked through in the sections named.

1. **Parameter sweeps: precompute.** The point is to *illustrate what the sweeps did*, not to re-run them live. Ship the sweep results as data, with a link to the code that produced them. → §7.3. This removes the hybrid option and most of the work: no in-browser 8,000-day runs, no progress bars, no worker threads for E5.

2. **Modernisation: the §4.2 line stands.** Keep the information design, replace the plumbing. Unchanged.

3. **The methodological argument: discuss it, don't build it.** Prose on the site is enough — decision types, the two-bin day, agent number as idealisation. No interactives, no engine support needed. It becomes a written section plus "under the hood" pop-outs on the models that illustrate it.

4. **The disagreements go in.** Where the reimplementation turned up differences, say so. → the *what we found* pop-out in §7.2, and the honest framing in §6.

5. **Fully JavaScript. The Java does not ship.** It stays in the repo as linked background reading and as the thing that verified the models, but nothing on the site runs it or depends on it.

   **And — no bit-exact parity, no matching randomness.** This is the one that reshapes the build. See below.

### 9.1 What dropping bit-exact parity changes

The plan as first written spent a whole stage on making the two languages consume the same random stream, so that parity could be a strict numerical diff. That is no longer wanted, and the honest consequence is that a chunk of planned work simply disappears:

- **Stage 0 is deleted.** `spatialecon-java`'s `Rng` keeps `java.util.Random`. Nothing about the Java changes, its tests keep passing on the stream they already pass on, and its numbers never move. The former Stage 1 becomes Stage 1 (§8).
- **The JS picks its own generator** on its own merits — a small seeded PRNG (mulberry32 is 8 lines and adequate for this) chosen for being fast and reproducible *within* JavaScript, which is all the site needs. Seeded reproducibility still matters: it is what lets the site offer a fixed run and a reroll, which E5 and E8 need because which Firm wins is genuinely chance.
- **The parity harness changes character**, from a diff to a two-tier check. → §6.

What is *not* lost is the verification itself, and it is worth being clear why. The Java refactor never had trace-level parity with the 2013 original either — CODE_ANATOMY §5.8 explains that the shipped jars cannot export data at all. It was verified against published figures, hand-deducible equilibria and qualitative claims, and that was sufficient to reproduce a dozen thesis results and turn up several real findings. **The JS port gets verified the same way, one level down.** The method is already proven on the harder version of this problem.
