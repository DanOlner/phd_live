# CLAUDE.md

## Project

Turning Dan Olner's 2013 Geography PhD (*Agent-based modelling and spatial economics*, Leeds) into an accessible website: interactive versions of the thesis models, with pop-out explanations and step-by-step teaching. The original models were interactive Java (Processing); we are translating them to JavaScript. Nothing is computationally heavy.

Dan is the thesis author. Assume deep familiarity with the economics and the original code — explain new findings, not his own work back to him.

## Read these first

Four reference documents live at the repo root. Consult them before answering questions about the thesis or the code; don't re-derive from the PDF or re-read the Java wholesale.

- **`THESIS_DIGEST.md`** — the argument, the maths, and a catalogue of 24 candidate interactives in seven groups (A–G), each with setup, parameters and teaching point. Model IDs (A1, C8, E4…) are used as shorthand everywhere.
- **`CODE_ANATOMY.md`** — how the models were actually built, model↔code mapping, exact algorithms and constants, discrepancies/gotchas, and (§5.9) the original visual language. Sections marked ↻ were revised after the Java refactor; §10 says what it settled.
- **`BUILD_PLANNER.md`** — the plan for the JavaScript port: dependency policy, Java→JS translation rules, the visual fidelity spec, parity harness, staging.
- **`BUILD_LOG.md`** — what was actually built, stage by stage, and what it taught: the decisions, the findings, and the things got wrong and corrected. The other three state current truth; this one keeps the history. Its "Corrections to the record" table is the fastest way to see which earlier claims have moved. **Append to it as stages land** — an entry earns its place by recording a decision, a finding or an error, not a list of files touched.

And `spatialecon-java/README.md` for what the refactor verified, model by model.

## Layout

- `Thesis_on_github/` — the PDF, plus the code. **290-page PDF: `Read` needs a `pages` range, max 20.**
  - `FrameworkModelSourceCode/src/location2012/` — ~190 Java classes, the framework
  - `FrameworkModelSourceCode/configs/*.properties` — parameter sets
  - `thesisModelsDanOlner2013/FrameworkModelBuilds/` — seven runnable jars, each with its own configs
  - `thesisModelsDanOlner2013/CoreModel/` — the Krugman Core Model program (separate, self-contained; model G1)
- `ThesisCorrectionsLatex/` — LaTeX sources. **Easier to read than the PDF.** Chapter text lives in `Chapter*/​*FromWord.tex`.
- `spatialecon-java/` — the reference implementation that verified the models. **Does not ship.** One `Scenario` class per model; see its own `README.md`.
- `spatialecon-js/` — **the deliverable, and where active work happens.** Zero dependencies, no build step. See its own `README.md`. Neither of the trees above is ever modified.
- `.github/workflows/deploy-pages.yml` — publishes `spatialecon-js/` to GitHub Pages as-is. **Don't move the site into `docs/`** or restructure it for hosting; the workflow exists so the folder can stay where it is. Keep every path relative and case-exact (Pages is case-sensitive, macOS isn't).

## Traps

These have already caused, or would cause, wrong conclusions:

1. **Scenarios were not config-driven.** They were selected by commenting/uncommenting blocks in `Main.java` and `ActorMaker.java` and recompiling. The source tree is one arbitrary snapshot, not any published figure.
2. **The jars are the authoritative record for scenario wiring; the source tree for algorithms.** All seven jars have distinct `Main.class`.
3. **Same config filename, different contents across jar folders.** `TwoRegionTransmission.properties` is 2 Firms/ρ=0.65 in `SymmetryBreak` but 4 Firms/ρ=0.5 in `TwoRegionFourFirms`. The source tree matches the *four*-Firm build. Always read the config from the jar folder you mean.
4. **Only `FirmActRespondAverage` was ever wired in.** The other nine `FirmAct*` classes are abandoned attempts. `FirmActMarkup` is an empty shell.
5. **`peopleConsiderAll = true` in every config** short-circuits all radius search: every Person sees every Firm at any distance. The radius arguments in the Person actions are dead.
6. **Constants that are hardcoded, not configured:** price-response divisor `adjustCostAmount = 100`; hill-climber `iterations = 1000`, jiggle 0.01→0.001; Firm crawl rate 0.025; `choose = 2` (Firms can only move, never set price or wage); initial `goodCost = 3`.
7. **"BetweenDays" in `Firm` is a misnomer** — those differences are per-*transaction*, not per-day. Governs oscillation frequency in E1.

## Conventions

- Capitalised **People** and **Firms** mean the model objects; lowercase means real ones. The thesis is consistent about this; keep it.
- **ρ (rho)**: ρ→1 = perfect substitutes, *low* love of variety. ρ→0 = strong LoV. Easy to state backwards.
- **EoS vs LoV**: increasing returns in *production* vs in *utility*. The thesis deliberately keeps them separate (Krugman's Core Model collapses them); their interaction is the main finding. Don't conflate.
- Distances, widths and budgets are normalised to 1 (width 2 for density-cost models).

## Open items

Not yet resolved — don't assume either way:

- **Still unverified**: the commute round-trip factor of 2 is in the thesis equation but not the code; noise is ±half what the thesis text implies. Both implemented behind `Quirks`; neither has been shown to change a published result.
- **D2 (§5.5.2) discrepancy**: the thesis reports low ρ making the DMR game harder under noise; the refactor shows the opposite, with a coherent mechanism for its own result. Noise amplitude, distance cost and noise character all ruled out. CODE_ANATOMY §7.12.
- **C3 and C7 configs are lost.** Reconstructed in the JS port from thesis text on C1's population and space, and flagged in-file as reconstructed. All four of C3's published directions and C7's core swap reproduce; the exact configs are still not recoverable.
(The website questions that used to sit here — sweeps, modernisation, methodology, how to present the disagreements — are all **decided**. BUILD_PLANNER §9.)

**Resolved, don't relitigate:**

- **ρ ≥ 1 branch.** Wrong (multiplies where the limit divides) but never ran — ρ was clamped to 0.99 everywhere it could be set, so "ρ = 1" figures are really 0.99. Use 0.99 where the thesis says 1. CODE_ANATOMY §7.1.
- **Initial prices are load-bearing.** The transmission belt does not converge from the source's constant 3.0 (3 of 50 Firms run away); the thesis' randomised 0.1–1.5 works. §7.9.
- **The shipped jars cannot batch-export CSV** — tried and confirmed. `CSVWriter` iterates `ds.buckets`; every build populates `ds.variableStores` instead, so the write loop runs zero times while still printing "writing". The original's data path was interactive (`w` key from the graph windows); the thesis figures are screen captures. Salvaged series in `spatialecon-java/reference/`. Patching `CSVWriter` and rebuilding from source would fix it if exact traces ever become necessary.
- **Which visualisation each published build used** — recovered by decompiling `Visualiser.class` from each jar. Table in CODE_ANATOMY §3.1. Not in the source tree at all: `Visualiser`'s constructor is the same comment/uncomment switch as `Main.java`.
- **The three "magnitude shortfalls" were one measurement error.** E8's figure plots `productionLevel`, not `Output_per_unitTime` (which its config caps at 1.3); E5–E7 thread on the `goodCost` ratio, not efficiency. Reading the right variables gives E8 **13.55 at ρ=0.9** vs the thesis' ~13, and E5/E6 thread repeatedly. CODE_ANATOMY §3.2. **A figure label is not a variable name** — only the jars say which field a store read.

## Running the original models

Verified working. Homebrew `openjdk@17` is already installed on this machine but is **keg-only** — not on `PATH`, not registered with `/usr/libexec/java_home`, so `java` alone reports "unable to locate a Java Runtime". Use the full path:

```sh
cd Thesis_on_github/thesisModelsDanOlner2013/FrameworkModelBuilds/<Scenario>
/opt/homebrew/opt/openjdk@17/bin/java -jar <Scenario>.jar
```

- **Must run from inside the jar's own folder** — `Class-Path` in the manifest is relative (`lib/core.jar` etc.), and the jar reads `configs/location2012.init` from the working directory.
- **Do not use the default `openjdk`** (currently 26). Processing's `PApplet` extends `java.applet.Applet`, which is present in 17 and removed in 26 — verified with `javap`.
- `modelRunDays = 0` means run until killed. These are interactive scenarios; expect a GUI window and no natural exit. `pkill -f <Scenario>.jar` to stop (exit 144 = SIGTERM, not a fault).
- **`javap` decompiles the jars**, which is how the per-build wiring, the live visualisation per build (§3.1) and the variable behind each figure label (§3.2) were recovered. Reach for it before assuming the source tree tells you what a figure did.

## Working notes

- **The JavaScript is the live work.** `spatialecon-js/` — zero runtime dependencies, no build step, ES modules, Canvas 2D, `// @ts-check` + JSDoc instead of TypeScript. **All eight stages of `BUILD_PLANNER.md` §8 are done**: 42 models — the whole catalogue, all seven groups — 142 tests, seven precomputed sweeps, pop-outs, walkthroughs, and `methodology.html`. Over half the stage-8 tail are not agent models (C4/C5 closed forms, E4 a difference equation, F1/F2 process demonstrations, G1 Krugman's Core Model in `src/coremodel.js`); they are `Scenario`s anyway so every model is a page with the same machinery. Its §9 decisions are settled — read them before proposing anything they rule out.
- **`data/sweeps/` and `snapshots/thumbs/` are committed output, not scratch.** The site is a static file tree with no build step, so anything a page fetches must already be in the repo. `npm run sweep` and `npm run thumbs` regenerate them; `.gitignore` un-ignores `data/sweeps/` specifically.
- **Walkthrough captions may not read `world.day`** unless the step sets `elapsed: true`, and a test enforces it. A caption keyed to a day number is pinned to one trajectory and breaks the reroll button, which is the whole reason nothing user-facing may depend on a specific run.
- **The Java does not ship.** It stays as the reference implementation, linked background, and the record of what was verified. The site is fully JS and depends on nothing in `spatialecon-java`.
- **No bit-exact parity, and no shared random stream.** The Java keeps `java.util.Random`; the JS picks its own seeded PRNG. Don't propose changing `Rng` — that plan existed and was dropped. Verification is two-tier (BUILD_PLANNER §6): the RNG-free economics diffed numerically at 1e-12, everything stochastic asserted by *claim* in a window ("E3 breaks with ρ in [0.70, 0.80]", not "at 0.764").
- **Seeding still matters** — a fixed run plus a reroll. So nothing user-facing may depend on a specific trajectory: step-through captions fire on model *events*, never on day numbers.
- When porting, **reproduce the original behaviour first and verify against published figures, then decide whether to correct it.** Where we do correct something, that difference is good pop-out material.
- Preserve the seeded RNG so the site can offer both a fixed run and a reroll — matters for E5/E8, where which Firm wins is genuinely chance.
- The Java is candid working code with self-deprecating comments (`//Hackeroo strikes again!`). Several mark exactly the compromises the thesis argues about — good source material for "under the hood" pop-outs.
- **The original visuals are more considered than they look** — monochrome by choice, everything double-encoded as size *and* greyscale, and on the plane the overlapping density-radius circles make the crowding measure and its picture the same object. CODE_ANATOMY §5.9; fidelity decisions in BUILD_PLANNER §4.
