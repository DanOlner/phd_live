# Code anatomy: how the thesis models were actually built

**Companion to** [THESIS_DIGEST.md](THESIS_DIGEST.md). Where the digest catalogues *what* the models show, this document reconstructs *how they were built* from the source, and pins each published model to the code that produced it.

**Sources examined:** `thesis_on_github/FrameworkModelSourceCode/` (~190 Java classes, `src/location2012/`), the 24 `.properties` config files, and the seven compiled jars in `thesisModelsDanOlner2013/FrameworkModelBuilds/` plus their shipped configs.

**Scope note:** the Krugman Core Model reimplementation (`CoreModel/src/coremodelviz/Main.java`, model **G1**) is a completely separate, self-contained program. It is deliberately left out here and should get its own pass.

**Revision, July 2026.** This document was first written before a Java runtime was available on this machine and before any of the models had been reimplemented. Both have since changed: JDK 17 is installed, the jars run, `javap` decompiles them, and 28 of the models have been rebuilt and verified in [`spatialecon-java/`](spatialecon-java/README.md). Everything that reading alone could only guess at has been re-checked against a running model. Sections marked **↻** carry findings that came out of that work; §10 summarises what the reimplementation settled, and what it did not.

---

## 1. The headline finding, and what it means for us

**Scenarios were not selected by configuration. They were selected by editing `Main.java` and `ActorMaker.java`, commenting and uncommenting blocks, and recompiling.**

`Main.java` is 856 lines, of which roughly 400 are commented-out alternatives: different `VariableStore`s to record, different graphs to open, different agent-placement calls, different parameter sweeps. `ActorMaker.java` does the same for which action class each agent gets. The `.properties` files carry the *numbers* (ρ, distance cost, agent counts, density cost) but not the *wiring* (which variables are plotted against which, where agents are placed, which Firm behaviour is active, whether a parameter sweep runs).

**↻ And it is not only `Main`.** `Visualiser`'s constructor is the same switch again, one layer down. Its entire job is to compose the visualisation, and it does so by having every option present and all but one commented out:

```java
} else {
//    drawables.add(new CESTest1DPresentation());
//    drawables.add(new Hotelling1DPresentation());
//    drawables.add(new TransmissionDraw1DPresentation());
//    drawables.add(new TransmissionDraw1D());
    drawables.add(new PeopleDrawToSpace());
    drawables.add(new FirmsDrawToSpace());
//    drawables.add(new PeopleBuyLines());
}
```

So the five 1-D presentation classes — about 3,900 lines, and the source of the D1, E3, E5, E6 and E7 figures — are *all* dormant in the source snapshot, along with `PeopleBuyLines`. Which one was live for which figure is not in the source tree at all. It is recoverable only from the jars, and §3 now does that.

Three consequences:

1. **The source tree is a single snapshot** — whatever state the code was left in around June 2013. It does not represent any one published figure. Right now it is wired for a density-cost correlation plot (`distanceToCentralFirm` vs `maxUtility`) with Firms placed at the centre of a torus.
2. **The seven jars are the frozen states.** Each was built from a different edit of `Main.java`, and each ships its own `configs/` folder with its own `location2012.init` naming the active config. This is a genuine archive — see §3.
3. **For the port, config-driven scenarios are the right target,** and would be a real improvement on the original. Everything the commented blocks do (choose recorded variables, choose graphs, place agents, run sweeps) is declarable.

A related point of style, relevant when reading anything here: the code is candid working code, liberally self-annotated (`//Hackeroo strikes again!`, `//Factory Schmactory`, `//blech, gotta be a better way of doing this`). Those comments are often the most reliable guide to intent, and several of them mark exactly the compromises the thesis argues about. They're worth mining for the website's "under the hood" pop-outs.

---

## 2. Build and runtime shape

- **Language/toolchain:** Java (NetBeans project layout), no build file in the repo beyond `lib/nblibraries.properties`.
- **Rendering:** Processing 1.x (`core.jar`) with ControlP5 for widgets, PeasyCam for 3D. Each visualisation is a Processing applet embedded in a frame by `ProcessingDrawer`/`ProcessingSpaceDrawer`.
- **Other deps:** Guava, OpenCSV, JUnit, iText/PDF, `physics.jar`, JUNG (graphs), QTJava. Most are unused by the model logic.
- **Entry point:** `location2012.Main`. Reads `configs/location2012.init`, which names one `.properties` file; `gl.setUpFromPropertiesFile()` parses it by reflection onto static fields of `gl`.
- **↻ A Java runtime is now installed** — Homebrew `openjdk@17`, keg-only, so it must be called by full path (see `CLAUDE.md`). The jars run, and `javap` decompiles them. The first draft of this document was written without either, from source plus raw string extraction from the class files; §3 has been redone properly.

**`gl` (globals) is the spine.** 1,381 lines of public static fields — every parameter, every agent list (`gl.people`, `gl.firms`, `gl.allActors`, `gl.goodSellers`), the timeline, the datastore. Classes read and write it freely. This is the single biggest obstacle to a clean port and the single easiest thing to fix (see §8).

---

## 3. Recovering scenario provenance from the jars

This is the good news. All seven builds have **different compiled `Main.class`** — confirmed by checksum — and there are **three distinct `ActorMaker.class` variants**:

| Build | `Main.class` | `ActorMaker.class` | active config |
|---|---|---|---|
| ManyAgentsLineTransmissionBelt | `15fd8e75b808` | `f06525b2ecc8` | `1DLineManyAgentsTransmission.properties` |
| MonoCentricDensity | `16267ca8c76e` | `913c2a22f6ae` | `CEUS1.properties` |
| SymmetryBreakParameterSweep | `49867c48222c` | `d1b7c9a6d508` | `TwoRegionTransmission.properties` |
| SymmetryBreakParameterSweepFast | `caac8fa385bc` | `d1b7c9a6d508` | `TwoRegionTransmission.properties` |
| TransmissionBeltPlusDensity | `96a4d3cf9286` | `913c2a22f6ae` | `TransmissionPlusDensity1.properties` |
| TwoFirmsLineMove2000dayAverage | `661ef4958b42` | `913c2a22f6ae` | `Hotelling_Line_DMR.properties` |
| TwoRegionFourFirms | `e2181a6c8c84` | `f06525b2ecc8` | `TwoRegionTransmission.properties` |

**The shipped configs also differ from each other and from the source tree.** SymmetryBreak and TwoRegionFourFirms use the same *filename* but different contents:

| | SymmetryBreak | TwoRegionFourFirms | source tree |
|---|---|---|---|
| `overRideNumFirms` | 2 | 4 | 4 |
| `rho` | 0.65 | 0.5 | 0.5 |
| `timeDelay` | 10 | 5 | 5 |
| `vizWidth` | 600 | 1200 | 1200 |

So the source tree's copy corresponds to the *four-Firm* build, not the two-Firm one. Anyone diffing source against the thesis text without noticing this would get E5 wrong.

**String extraction from each `Main.class`** recovers exactly which variables and graphs each build wired up. Common to all builds: `maxUtility`, `chosenDensityCost`, `stock`, `goodCost`, `currentContributedTime`, `productionLevel`, `output per unit time`, `people_x/y`, `firm_x/y`. The distinctive ones are diagnostic:

- **TwoFirmsLineMove2000dayAverage** → `firm0demand`, `firm1demand`, `demand0followmean`, `demand1followmean`, `firm follow x`, `firm follow x sd`. That is precisely the D1 display: per-Firm demand profiles plus the running-mean position with standard-deviation bars.
- **TransmissionBeltPlusDensity** → `mean distance to Firm 0`, `Firm efficiency ratio`, `distanceToCentralFirm`. Exactly the two axes of E8's arrow-datapoint correlation plot.
- **SymmetryBreak (both)** → `Firm good cost ratio`, `delivery cost`, `trigger`, `triggerData`. The threaded parameter sweep of E5, driven by a trigger-based `DataStore`.
- **ManyAgentsLineTransmissionBelt / TwoRegionFourFirms** → `Firm good cost ratio`, `distance cost`, `trigger`, `triggerData`, `output per unit input`.
- **MonoCentricDensity** → `distance to centre`, `chosenDensityCost`, plus *leftover* markup machinery (`Firm 0 good cost (markup)`, `priceDiff`, `economies of scale curve`). The markup strings are dead code carried along from whatever state the file was in — a useful warning that string presence proves availability, not use.

### ↻ 3.1 Decompiled, not guessed

`javap -p -c` on each jar replaces the string extraction above with the actual call sequence. Two things follow that string presence could not establish.

**First, the leftover strings really are leftover *windows*.** In `TwoFirmsLineMove2000dayAverage` (the D1/D2 build), the recovered construction order is:

```
VariableStoreCoords x, y ×4        VariableStore maxUtility "utility"
VariableStore chosenDensityCost    VariableStoreMeanSD maxUtility "utility mean"
VariableStoreMeanFollow x "firm follow x"      VariableStoreSDFollow x "firm follow x sd"
VariableStore firm0demand "firm 0 demand"      VariableStore firm1demand "firm 1 demand"
VariableStoreMeanFollow firm0demand "demand0followmean"  … firm1demand "demand1followmean"
VariableStore currentContributedTime / goodCost / Output_per_unitTime / stock
TimeSeriesGraph currentContributedTime | TimeSeriesGraph stock | TimeSeriesGraph goodCost
CorrelateArraysGraph Output_per_unitTime vs goodCost
```

The first two-thirds is exactly the D1 display. The last four are transmission-belt quantities, which for a partial model never move — and they are genuinely `new`ed, so four dead windows opened on screen every run. The MonoCentricDensity markup strings noted above are the same thing. **Every build carries windows from whatever the previous build was.** Worth knowing before treating any recovered window list as an intended design.

**Second, and more useful: which visualisation each build actually used.** `Main` never names one — `Visualiser`'s constructor picks it (see §1), so the answer is in `Visualiser.class`, one per jar. Decompiling the constructor and reading the branch order (`singleActor` → first, `transmissionFirms` → second, else → third) gives an unambiguous map:

| Build | Models | `transmissionFirms` | **Live visualisation** |
|---|---|---|---|
| MonoCentricDensity | C1, C2, C8 | false | `PeopleDrawToSpace` + `FirmsDrawToSpace` |
| TwoFirmsLineMove2000dayAverage | D1, D2 | false | **`Hotelling1DPresentation`** |
| SymmetryBreakParameterSweep(Fast) | E5 | true | **`TransmissionDraw1D`** |
| TwoRegionFourFirmsTransmissionBelt | E6 | true | **`TransmissionDraw1DPresentation`** |
| ManyAgentsLineTransmissionBelt | E7 | true | **`TransmissionDraw1DPresentationForManyAgents`** |
| TransmissionBeltPlusDensity | E8 | true | `PeopleDrawToSpace` + `FirmsDrawToSpace` |

That is the piece §1 said was missing from the source tree entirely, and it is what a faithful port needs. `CESTest1DPresentation` and `PeopleBuyLines` appear in no shipped build; they are dormant in every jar as well as in the source.

(Minor correction to the table above: the seven jars live in **six** folders — `SymmetryBreak/` holds two.)

### ↻ 3.2 Which *variable* each figure plotted — and why it matters more than it sounds

Decompiling one level further, to the arguments each `VariableStore` was constructed with, resolves what looked like three separate failures to reproduce. They were one failure, and it was not in the models.

| Build | Store | **Reads** | Label on the figure |
|---|---|---|---|
| TransmissionBeltPlusDensity (E8) | `VariableStoreTwoFirmRatio` | **`productionLevel`** | "Firm efficiency ratio" |
| SymmetryBreak (E5), TwoRegionFourFirms (E6), ManyAgentsLine (E7) | `VariableStoreTwoFirmPriceRatioTrigger` | **`goodCost`** | "Firm good cost ratio" |

**E8's "efficiency ratio" is not an efficiency ratio.** It is the ratio of `productionLevel`, i.e. total output `f(T) = vT² + mT`. The obvious reading — `Output_per_unitTime`, which the same build also records — gives `vT + m`, and with the shipped `EoS_Curve = 0.001, EoS_Magnitude = 1` that is `0.001·T + 1`. Total time in the model is 300 People × 1 day, so `T ≤ 300` and **the per-unit ratio cannot exceed 1.3 by arithmetic**, whatever the model does. A reimplementation measuring it reached 1.26 — 97% of the ceiling — and recorded a failure to reproduce the thesis' ~13. Measuring `productionLevel` instead gives **13.55**, at ρ = 0.9 as the thesis states rather than the 0.97 that reading the capped statistic seemed to require.

**E5–E7 thread on price, not efficiency.** The regime-boundary trigger is the ratio of `goodCost`. Price is much the more responsive signal: at symmetry both prices sit together, and when symmetry breaks the winner's falls *while* the loser's climbs, so the ratio moves from both ends at once. Threading on efficiency crosses the boundary once in 20,000 days; threading on price crosses repeatedly.

Two general lessons, both cheap to state and expensive to learn:

1. **A label is not a variable.** "Firm efficiency ratio" is a display string chosen by the author for a figure; the quantity behind it is a field name in a constructor argument. Only the second is recoverable, and only from the jars.
2. **Check the arithmetic ceiling of a statistic before concluding a model is weak.** `vT + m` over `m` is bounded by the configuration, not by the model's behaviour. That bound could have been computed in a minute at any point and would have saved treating a measurement artefact as a substantive discrepancy about economies of scale.

**Recommendation:** treat the jar folders as the authoritative record for their scenarios (config + wiring + view), and the source tree as the authoritative record for *algorithms*. For the two published scenarios with no matching jar (C3, C7 — see §4), reconstruct from the thesis text and the parameter conventions in §6.

---

## 4. Model ↔ code mapping, verified

Checking each entry in the digest's catalogue against the code. **Verified** = config and/or code path directly confirms it. **Reconstructable** = the mechanism exists, the exact scenario wiring doesn't survive.

| Digest | Config | Key classes | Status |
|---|---|---|---|
| **A1** CES explorer | `CESTest1Person9Firms` (2 firms, 1 person, δcost 0) | `CESOneTypeUtility` | ✅ verified |
| **A2** Cobb–Douglas | — | `NecessityLuxuryUtility` | ✅ present — see note below |
| **A3** Production functions | — | `SmoothEoS`, `CoreModelEoS` | ✅ verified |
| **A4** Hill-climber vs constrained | any, via `optimiser=Greedy` | `OptimiseHillClimb`, `HillClimbOptBucket`, `TestCESOptimiseHillClimb` | ✅ verified |
| **B1/B2** LoV × distance, double dip | `spatialimpact_of_CES` — *1 person at (0,0), 10 firms on a line 0→1* | `CESOneTypeUtility` | ✅ exact match to thesis setup |
| **B3** Black hole | — | `NearestNeighBucketAllActors`, `NearestNeighMetaBucket`, `NearestNeighBucketMeansSDsOnly` | ✅ mechanism verified (multi-run meta-bucket = the 20-run average) |
| **C1/C2** Monocentric, density internals | `CEUS1` (500 people, 1 firm, width 2, radius 1/16) | `PersonAction`, `SpaceTools.findDensityCost` | ✅ verified |
| **C3** Spatial vs non-spatial sweeps | — | `ActorParamSweeper`, `VariableStoreMeansOnlyTrigger` | ⚠️ reconstructable — sweep wiring was in `Main` |
| **C4/C5** Two/three region thought experiment | — | — | ➖ not agent code; hand/spreadsheet calculation |
| **C6** Von Thünen | `vonThunen1`, `CEUS2vonThunen1` | `ActorMaker` vonThünen block, `Person.vonThunenFirm`, `gl.wageRange`/`commuteRange` | ✅ verified |
| **C7** LoV shapes settlement | — | `ActorMaker` `deliveryRange` block | ⚠️ reconstructable — needs `deliveryRange=true`, `deliveryCost=2`, 2 Firms (gives 2 and 4 — see §6) |
| **C8** Spatial ≠ utility equilibrium | `CEUS1` (`DENSITYCOST=100`) | as C1 | ✅ verified, jar shipped |
| **D1** Two Firms on a line | `Hotelling_Line_DMR` (Line, 2 mobile Firms, 50 People) | `FirmActRespondAverage`, `Hotelling1DPresentation` | ✅ verified, jar shipped |
| **D2** DMR game | `Hotelling_Line_DMR` + uncomment `splitActorsBetweenTwo1DPoints = people,0.25,0.75` | `FirmsCircleGameBucket`, `FirmsCircleGameMetaBucket`, `gl.noiseUtil` | ✅ verified — the split line is present but commented |
| **E1** Damping | `ConnectedModels/DampingFigure` (50/50, δcost 0, EoS 1/1); `Transmission_singleFirm1` | `Firm.transmissionFirmsAction` | ✅ verified |
| **E2** Markup & tipping point | `EoS_LoV_noSpace` (`markUpSingleFirmOnly=true`) | `Firm.transmissionFirmsAction` markup sweep hack (commented) | ⚠️ mechanism verified, sweep is commented — see §7 |
| **E3** EoS × LoV regime transition | `ConnectedModels/EoS_LoV_noSpace` (2 Firms, 2 People, EoS 1/1) | as E1 | ✅ verified (config says 2 People; thesis text says 1) |
| **E4** Tracking down the belt | — | — | ➖ Excel only, no Java |
| **E5** Two regions | `TwoRegionTransmission` **as shipped in SymmetryBreak** (2 Firms, ρ 0.65) | `ShouterParamSweeper`, `VariableStoreCrossesZero` | ✅ verified, 2 jars |
| **E6** Two regions, four Firms | `TwoRegionTransmission` **as shipped in TwoRegionFourFirms** (4 Firms, ρ 0.5) | as E5 | ✅ verified, jar shipped |
| **E7** Nineteen on a line | `1DLineManyAgentsTransmission` (19/19, EoS 0.1/0.1) | `TransmissionDraw1DPresentationForManyAgents` | ✅ verified, jar shipped |
| **E8** Belt + density | `TransmissionPlusDensity1` (2 Firms, 300 People, δcost 0.25, `EoS_Curve` 0.001, `DENSITYCOST` 10) | `PersonActionTransmission` + density | ✅ verified — config matches thesis text exactly |
| **F1** Money exchange | — | `GiniBucket` (Java); `exchangetestrandom.m` (Matlab) | ✅ verified |
| **F2** Permutation gap | — | `RandPermuteActorListener` (`count`, `pos`, `yesterdayPos`), `PermuteSpaceBucket` | ✅ verified — the instrumentation is still in the class |
| **F3** Blind vs rational | — | `CoreModelEoS`, `BackyardSingleActorAction` | ⚠️ reconstructable |
| **F4** Bundle sampling | any, `writePog=true` | `OptimiseHillClimb` POG output, `ProcessingOptimiserGrapher` | ✅ verified |

**Two corrections to the digest, and one confirmation:**

- **A2 (Cobb–Douglas) — I was wrong to expect a `CobbDouglas` class.** It's `NecessityLuxuryUtility`, which implements the Stone–Geary form `U = F^δ · (M+γ)^(1−δ)`. Setting γ = 0 reduces it to plain Cobb–Douglas. This matches the thesis' commented-out "Engel function" equations, which it notes "reduce to the Cobb Douglas when γ is zero". So A2 is buildable, and there's a free bonus: the γ parameter gives a non-homothetic variant (luxury goods with an entry threshold) that the thesis derived but cut. Might be worth a pop-out.
- **C3 and C7 have no surviving config.** Both are reconstructable, but their exact sweep settings are gone. These are the two catalogue entries where "run it live" is genuinely harder than the others.
- **E8's config confirms the digest's numbers to the decimal** — 2 Firms, 300 People, distance cost 0.25, `EoS_Curve` 0.001. Good sign for the rest.

---

## 5. The engine, class by class

What follows is what the code actually does, with the constants that were live in the shipped state.

### 5.1 Timeline and turn order

`Timeline` is an `Audible`; everything that needs to act each day is a `Listener` holding a `Shouter`. `Main` registers, **in this order**:

```java
new RandPermuteActorListener(gl.time, 1, gl.people);
if (!gl.singleActor) new RandPermuteActorListener(gl.time, 1, gl.firms);
```

Listeners are sorted by weight before the run (`sortListenersByWeight()`); both register at default weight, so registration order holds. **This is the two-bin fix from §4.7.4 of the thesis, and it is exactly two lines.** All People are permuted and act, then all Firms are permuted and act. `RandPermuteActorListener` still carries the instrumentation used to produce the pyramid histogram (`count`, `pos`, `yesterdayPos`).

`gl.killSwitch` stops a run; `gl.modelRunning` drives `while (gl.modelRunning) gl.time.nextDay();`.

### 5.2 Space

`SpaceTools` (2,839 lines, much of it commented alternatives). Four `SpaceType`s: `Point`, `Line`, `TwoRegion`, `Torus`. Despite the name, `Torus` is used as a plain bounded plane in all published runs — `wrapSpace=false` everywhere.

Two-region geometry: regions sit at `centrePoint ± width/2`, so `gl.width` *is* the inter-region distance.

**Critical for the port:** every config sets `peopleConsiderAll = true`. In `whosInRangeOfArray`, that short-circuits all the geometry and returns **every** actor with a relative offset, ignoring the `range` argument entirely:

```java
if (gl.peopleConsiderAll) {
    for (Actor a : actors)
        lm.addActor(a, new Point2D.Double(a.getPoint().x - loc.x, a.getPoint().y - loc.y));
    return lm;
}
```

So the radius arguments (`0.2` in the Person actions) are dead in every published run. Every Person sees every Firm at every distance. This matches the thesis' own admission that CES buyers "face no uncertainty in their space search: prices of all goods are known, regardless of distance" — but it's worth knowing that the search machinery exists and is simply bypassed.

Placement helpers, all static: `putActorsInAHorizontalLineStartEnd`, `putActorsInACircle`, `putActorsOnThisPoint`, `putActorsInsideCircleAreaRandomly`, `splitActorsBetweenTwo1DPoints`. Configs can invoke these by reflection (`SpaceTools.method = people,0,1`), which is the one piece of genuine scenario configurability in the original — and a good model for the port.

### 5.3 Density cost

`SpaceTools.findDensityCost(LocMemory)`:

```java
for (ActorNLocation a : dlm.actors)
    weight += (1 - (zp.distance(a.p) / gl.DENSITYCOSTRADIUS));
weight /= gl.numTotalActors;
```

Linear taper from 1 (co-located) to 0 (at the radius), summed, then normalised. Applied in the Person action as:

```java
adjustedDensityCost = gl.DENSITYCOST * SpaceTools.findDensityCost(dlm);
b.bestWage -= adjustedDensityCost;
```

Subtracted from the wage, exactly as the thesis says. Note the neighbour set comes from `peopleInRange` — a genuine radius query, *not* short-circuited by `peopleConsiderAll`. Density is properly local; goods search is not.

⚠️ **`gl.numTotalActors = overRideNumFirms + overRideNumPeople`** — Firms *plus* People. The thesis (§4.4) says "divide by the total number of People in the model run". For `CEUS1` (1 Firm, 500 People) the difference is 0.2%; for `TransmissionPlusDensity1` (2 Firms, 300 People) it's 0.7%. Immaterial to results, but the port should pick one and say which. (Now a `Quirks` flag; neither setting changes a published result.)

**↻ The density cost has a floor, and the floor is the interesting part.** A Bundle is only considered if `b.bestWage > 0`, and density is subtracted from the wage, so density cannot effectively go below zero. That turns out to explain why C1's utility is *not* perfectly flat across distance, which looked like a bug in the reimplementation and is not. With one Firm, utility has a closed form `U = (1 − D(d)) / (1 + 0.25d)`, and `dU/dd = 0` requires a density profile that is exactly linear *and reaches zero* at `d = (1 − k)/0.25k`. The model produces that line — corr(distance, density) = −0.986 — but past the cutoff there is nothing left to trade away, so those People pay pure distance cost and their utility tracks `1/(1 + 0.25d)` with corr −0.966.

They are not in disequilibrium; no move improves them. They are outside the range over which equalisation is possible. It is a finite-population boundary effect, and it washes out with numbers exactly as that implies: over 125 / 250 / 500 / 1000 People the fringe falls 43.2% → 20.4% → 7.2% → 1.9% and the overall gradient goes with it (−0.823 → −0.238), while the *interior* correlation shows no monotone trend. Staleness (measured drift: exactly zero), a missing deadband (the original has none — current location is evaluated first and the comparison is a strict `>`) and sampling granularity were all ruled out first.

The pleasing part is that this is C8's mechanism in miniature: raising the density cost pushes the cutoff inward until the fringe swallows the settlement. C1 and C8 are one continuum, not two regimes, and the fringe is already present at density 10 — just small enough to overlook. Full working in `spatialecon-java/README.md`.

**↻ Confirmed a third time, and the estimator matters.** The JS port draws this as an overlay (`spatialecon-js/src/view/equalise.js`), which required committing to a way of estimating U* from a run. The utility distribution is a spike at U* — everyone in the interior, who all reach the same level by construction — plus a left tail of fringe People, so for any fringe under half the population the **median** is U*, and it does not move as the tail lengthens. Estimating U* as the *maximum* is the obvious choice and is wrong: it picks the luckiest draw, overstates U*, understates d*, and counts interior People as fringe.

The difference is checkable against the series above, and the check is worth recording because it is a third independent measurement of the same effect:

| People | Java, measured | JS, median U* | JS, max U* |
|---|---|---|---|
| 125 | 43.2% | 44.0% | 93.6% |
| 250 | 20.4% | 20.0% | 48.0% |
| 500 | 7.2% | 7.6% | 18.6% |
| 1000 | 1.9% | 2.0% | 4.9% |

The median column reproduces the Java to within a point at every population, from code that shares no line with it and runs a different generator. The max column matches nothing. The fringe is real, its size is right, and it thins with numbers exactly as a boundary effect must.

### 5.4 Person: the Bundle cycle

Two action classes, and the difference matters:

| | `PersonAction` (partial models, C/D) | `PersonActionTransmission` (E) |
|---|---|---|
| Bundles | `addToBundles(b, 10, 0.2, 10)` → 10 near + 10 model-wide + 1 current = **21** | `addToBundles(b, 3, 0.2, 5)` → 3 + 5 + 1 = **9** |
| Budget | best wage from a Firm, net of commute | `b.bestWage = 1` — the day itself |
| Wage search | `remainingTime = 1 − (distance × commuteCost)`; picks max `wage × remainingTime` | none |
| Sequence | find best wage → price goods → optimise → move → work → buy | remove time from Firms → price goods → optimise → move → buy |

`Bundle.addToBundles` also handles the two-region case specially: instead of random sampling, it adds bundles at `−width` and `+width` (one redundant, "because that's easier than hacking the code to make Bundle know which region to pick").

The transmission action's opening move is the mechanism that makes economies of scale work without strategy:

```java
bestBundle.removeTimeFromOthers();   // withdraw last turn's input from every Firm
bestBundle = new Bundle();
```

Pricing, in both actions, is the flat form:

```java
price = sap.getGoodCost() + (sap.getDeliveryCost() * distance);
```

The hard-limit commute form `p/(1 − d/tech)` from thesis §4.3.3 is present but **commented out**. It was derivation, not implementation.

A Bundle is only considered if `b.bestWage > 0` — so once density cost exceeds the wage, that location drops out of consideration entirely rather than going negative. That is the mechanism behind C8's dispersal at high density cost.

### 5.5 Utility

`UtilityShell` is a static holder for one `Utility` instance shared by every Person — the thesis' flagship example of functional over descriptive mapping, and it is literally one static field. `CESOneTypeUtility.rho` is likewise `static`: all agents share tastes by construction.

Two entry points, distinguished by argument type:

**Constrained** (`giveUtility(Bundle)`) — for ρ < 1:

```java
top    = Math.pow(price_j, 1/(rho-1)) * b.bestWage;
bottom = Σ Math.pow(price_i, rho/(rho-1));
goods[i] = top / bottom;
b.maxUtility += Math.pow(goods[i], rho);
// ... then
b.maxUtility = Math.pow(b.maxUtility, 1/rho);
```

Exactly the digest's equation. Noise is applied *after*:

```java
b.GoodsList.get(i).optimalChosenAmount = goods[i] + (goods[i] * ((Randoms.nextDouble()-0.5) * gl.noiseUtil));
```

**Hill-climber** (`OptimiseHillClimb.optimise(Bundle)`), live constants:
- `iterations = 1000`
- `startJiggleSize = 0.01`, `endJiggleSize = 0.001`, linearly annealed: `jiggleSize += (end-start)/iterations`
- Slices initialised `Randoms.nextDouble()` per good, then normalised to sum 1
- Each pass: `g.jiggledSlice = g.slice + ((0.5 - Randoms.nextDouble()) * jiggleSize)`, negatives bounced back positive, renormalised, test-spent, utility compared, kept or reverted
- Budget-splitting loop (`while (budget > 0 && anyGoodsLeft)`) reallocates leftover money when a good runs out of stock — this is the flexibility the thesis credits it with, and the constrained version genuinely cannot do it

`gl.writePog` dumps the hill-climb trace for one Person on day 3 into `optimalPogData` — "POG" is *person optimisation graph*, and this is the data behind the digest's A4 and F4 figures. (The thesis appendix admits the author had forgotten what the acronym stood for.)

### 5.6 Firm: two completely separate behaviours

**(a) Partial models — DMR location search.** `FirmActRespondAverage`, called once per day:

```java
incomeDifference = me.revenue - me.yesterdayRevenue;
// ... perturb, then after numDays (=1):
if (meanIncomeDifference > 0) { crawlRate += adjustStep; }   // keep going, accelerate
else { setVals(true); changeMode = true; crawlRate = startCrawlRate; }  // reverse, re-randomise
// then
me.yesterdayRevenue = me.revenue;  me.revenue = 0;
```

Constants: `startCrawlRate = 0.025`, `startAdjustStep = 0.0025`, `numDays = 1`. A new direction is `crawlRate = (0.5 − rand) × startCrawlRate`, i.e. ±0.0125; on a torus, paired with a uniform random angle.

**↻ That signed draw is load-bearing, and easy to get wrong.** The obvious reading — step a fixed 0.025 in a randomly chosen direction — makes the crawler far too good. `crawlRate` is a *signed magnitude* drawn uniformly from ±0.0125, so a fresh direction is on average half the nominal rate and is sometimes near zero; `adjustStep` is then applied with the sign of the current step (`step < 0 ? −0.0025 : +0.0025`) so that acceleration means "more of whatever I was doing", not "more positive". Reimplementing it as a unit-direction step made the D2 games converge in a fraction of the days and quietly flattered the DMR result. Reproduce the draw, not the description.

`pickRandomVars()` contains a randomiser over {wage, price, location}, then three lines that override it:

```java
if (choose == 0) { choose = 2; }
choose = 2;
```

**Firms can only ever move.** This is the code behind the thesis' footnote that price and wage perturbation "faced problems" — the switch cases for wage (`case 0`) and price (`case 1`) are fully written and permanently unreachable.

**(b) Transmission belt — granular stock targeting.** Not an `Action` at all. It lives in `Firm.buyGood()`, so it fires **on every purchase**, out of turn — the "reflex" decision type:

```java
yesterdayStock = stock;
stock -= units;
currentContributedTime += (units * goodCost);
stock += ProductionShell.giveOutput(currentContributedTime)
         * ((units * goodCost) / currentContributedTime);     // g_a = f(T_p)·(t_a/T_p)

productionLevel     = ProductionShell.giveOutput(currentContributedTime);
Output_per_unitTime = productionLevel / currentContributedTime;

yesterDayStockDiffBetweenDays = stockDiffBetweenDays;
stockDiffBetweenDays = stock - yesterdayStock;
stockSecondDeriv     = stockDiffBetweenDays - yesterDayStockDiffBetweenDays;

movingMarkUpTarget += markUp;
stockTarget = stock + stockDiffBetweenDays + stockSecondDeriv - movingMarkUpTarget;

adjustCostAmount = 100;
costChangeAmount = Math.abs(stockTarget) / adjustCostAmount;
costChangeAmount = (stock < movingMarkUpTarget ? costChangeAmount : -costChangeAmount);
goodCost += costChangeAmount;
```

Points worth having straight before porting:

- **`x` = 100.** The `adjustCostAmount` in `Δp = (s + s' + s'')/x` is hardcoded to 100, overriding several commented experiments including an EoS-linked version.
- **"BetweenDays" is a misnomer.** `yesterdayStock` is reassigned at the top of *every transaction*, so `stockDiffBetweenDays` and `stockSecondDeriv` are per-*purchase* differences, not per-day. With 300 People that's 300 updates per Firm per day. This matters a lot for reproducing the oscillation frequencies in E1.
- **Magnitude and polarity use different quantities.** Magnitude comes from the damped `stockTarget`; polarity comes from raw `stock` vs `movingMarkUpTarget`. A literal reading of the thesis equation would use the damped sum for both. Reproduce the code, not the equation, if the goal is matching the published traces.
- **Markup is a ratchet:** `movingMarkUpTarget += markUp` on every purchase, so the target climbs without limit and the Firm skims the difference.
- **`goodCost` floors at zero** by reverting to `goodCostAtLastKnownPeriod`, and `countPositiveGoodCostChange` counts consecutive price rises — the runaway detector. Its corrective block is disabled (`if (false)`), which is *why* the tipping point in E2 runs away instead of self-correcting.

**↻ This loop can be checked against arithmetic, and it passes.** Unusually for an agent model, several transmission-belt equilibria are deducible by hand, which makes this the strongest available test of the whole mechanism. With `x = l² + l`, a Firm receiving `T` units of time clears its stock at price `1/(T+1)`. Reimplemented from the code above:

| Setup | Deduced | Measured |
|---|---|---|
| 1 Firm, 1 Person (T=1) | 0.5 | 0.50000, residual stock 5e−15 |
| 50 Firms, 50 People (T=1) | 0.5 | 0.50000, residual stock 4e−15 |
| 20 Firms, 100 People (T=5) | 1/6 | 0.16667 |
| symmetric two-Firm | 2/3 | 0.66670 |

Dropping the two derivative terms — targeting the stock *level* alone — leaves the price stationary at 0.299 with stock never clearing (7.31). That is the argument for damping, demonstrated rather than asserted.

**Initial state:** `goodCost` is randomised `(rand × 5) + 1` and then immediately overwritten by `goodCost = 3;`. Stock starts at `0` for transmission Firms and `100000` for partial ones — the "unlimited stock" of the C and D models is a literal large number.

**↻ And the initial price is not cosmetic.** §7.9 below flagged the three-way disagreement between the thesis (0.1–1.5), the dead randomiser (1–6) and the live constant (3) as an open question about which figure used what. It is now answerable on grounds other than memory: **the constant 3 does not converge.** With distance costs and 50 Firms, 3 of 50 overshoot into the runaway of §6.3.2 and never recover, because the first price corrections are proportional to a stock error that is enormous when the price starts far from clearing. Randomised 0.1–1.5, none do. The thesis' range is the one that works, and any port needs it.

⚠️ Stock targeting is skipped for the actor whose ID equals `gl.LOOKATME`. Since `LOOKATME` indexes a Person in every shipped config (Firm IDs start at `gl.people.size()`), this never bites in practice, but it's a landmine.

### 5.7 Production

```java
// SmoothEoS
return (time * time * gl.EoS_Curve) + (time * gl.EoS_Magnitude);

// CoreModelEoS
return (time - 3) / gl.EoS_Magnitude;
```

`SmoothEoS` is `x = v·l² + m·l`, matching the thesis. `CoreModelEoS` hardcodes α = 3 and reuses `EoS_Magnitude` as β — and returns negative output for `time < 3`, which is precisely the F3 "blind vs rational" demonstration. `ProductionShell` is a static holder, same pattern as `UtilityShell`; the swap is a one-line edit in `gl`.

### 5.8 Output plumbing

`DataStore` holds `VariableStore`s; each grabs a named field off every actor in a list **by reflection** on a timer. Variants: `VariableStoreMeanSD`, `MeanFollow`/`SDFollow` (rolling window, `gl.followAverage`), `TwoFirmRatio` (with a flag to keep the ratio ≥ 1 — this is E8's "Firm efficiency ratio"), `CrossesZero`, and several `...Trigger` classes that fire a sweep step when a condition is met. `ActorParamSweeper` and `ShouterParamSweeper` step a parameter on each trigger — that pair is the machinery behind E5's threaded boundary and C3's equilibrium-detection sweeps.

This subsystem is about a third of the codebase and should be replaced wholesale rather than ported.

**↻ The shipped jars cannot batch-export CSV.** This was the plan for validating the port — run each original, save its output, diff. It does not work, and the reason is a two-line bug. `CSVWriter` iterates `ds.buckets`; every shipped build populates `ds.variableStores` instead. The two collections are disjoint, so the write loop runs zero times — while still printing "writing" and creating the file. Tried and confirmed on all seven jars.

The original's data path was never batch. It was interactive: **`w` saves a clone of the on-screen variable store**, and the published figures are screen captures. That is consistent with `modelRunDays = 0` everywhere (§6) and with the thesis' framing of the model as an experiment station rather than a batch job.

Consequences worth being clear about. Automated numerical parity against the original is not available; verification is against published figures, deducible equilibria (§5.6) and qualitative claims. Patching `CSVWriter` to iterate `variableStores` and rebuilding from source would fix it, if exact per-scenario traces ever become necessary — the source tree compiles. Five CSVs salvaged from the figure directories are in `spatialecon-java/reference/`.

### ↻ 5.9 What the models actually looked like

The first draft of this document skipped the 11,339 lines in `graphics/` as not load-bearing for the port. That was right about the *model* and wrong about the *deliverable*: reproducing the visualisations is now an explicit goal, so here is the visual language, read from the drawing code and checked against the published figures in `ThesisCorrectionsLatex/ChapterTestingModels/Figures/`.

**It is monochrome by design.** `Colours.java` and `ActorShape.java` are empty stubs — colour was started and abandoned. Every shading call is a black↔white ramp:

```java
col1 = Map.mapLowHighToColour_DoublesColorsInColorOut(
        pe.maxUtility, minMax1.lowVal, minMax1.highVal, Color.black, Color.white);
```

with the commented-out green↔red version still above it, marked `//b&w version`. Even the per-actor `Actor.colour` field is grey — `gl.nextActorColour()` builds its palette as `new Color(i, i, i)` for `i = 0, 150`, so agents alternate black and mid-grey, with red/green/blue entries commented out just below. The name is vestigial. The thesis printed in black and white, and the figures were captured directly from screen, so the model was made to render for the page.

**Everything is double-encoded.** The same variable drives both the greyscale *and* the size of a marker:

```java
rectSize = Map.mapLowToHigh_DoublesInFloatOut(pe.maxUtility, low, high, 0.005, 0.05);
```

A 10× size range plus a full black-to-white ramp, both carrying one number. This is what makes the figures legible in print at low resolution, and it is worth keeping.

**Two view families, and they look nothing alike.**

*The plane* (`PeopleDrawToSpace` + `FirmsDrawToSpace`; C1, C2, C6, C8, E8). Each Person is drawn twice: a faint circle at the **density-cost radius** (`stroke(0, 80)`), and a marker at their position — a greyscale square sized as above, or a white circle if utility is exactly zero. Firms are a 12-vertex plus/cross glyph plus an unfilled radius circle. There are no axes, no frame and no chrome. The result, in `IB_CEUS1_…_MainVizDensity.png`, is a page of overlapping circles whose ink density *is* the density cost — the crowding measure and its picture are the same object. That is the single best visual idea in the original and the one most worth carrying over.

*The von Thünen variant* (C6, figures 5.15–5.16) replaces the greyscale ramp entirely: `col1 = pe.vonThunenFirm.colour`, and a `switch` on the Person's assigned Firm draws **a different shape per cost group** — square, circle (1.2× size), triangle (0.8×), pentagon (0.7×) — with a legend keyed to the varying cost. Shape *and* grey, because four greys alone do not separate in print. Sensible, and worth reproducing: in these models the group is the result, so shading by density would bury it.

⚠️ **↻ The palette in the source cannot have produced those figures.** `gl.nextActorColour()` as left builds its list with `for (int i = 0; i < 255; i += 150)` — two entries, black and mid-grey — and then hands every Firm past the second a **random RGB colour**. With four Firms, figures 5.15 and 5.16 would have two greys and two arbitrary colours. Directly above that line, commented out, sits a four-entry list — 220, 190, 160, 130 — which matches the published light-to-dark ramp exactly. Another instance of §1: the snapshot is not the state any figure was made in, and here the *drawing* code carries the same disease as the scenario wiring. Use the commented values.

*The line* (`Hotelling1DPresentation`, `TransmissionDraw1D*`; D1, D2, E5, E6, E7). A horizontal rule with Firm 0's demand profile as bars rising above it and Firm 1's mirrored below, one bar per Person at that Person's position, greyscale by distance. Firms sit on the rule as a white pennant, with a rectangle above showing the **rolling mean position ± SD** (`firm follow x`, `firm follow x sd`) — so the figure shows both where a Firm is and where it has been, which is what makes a wandering DMR Firm distinguishable from a settled one. Between the panels, a rug of per-Person ticks. Gridlines and numeric labels on the right; rotated axis labels on the left. `MQ_…_MainVizrho1del20.png` is the reference.

**`PeopleBuyLines`** (dormant in every build, §3.1) draws a line from each Person to each Firm they bought from, with `strokeWeight` proportional to the quantity. It is the only view that shows the trade flows themselves rather than their consequences. Never published; obvious website material.

**Graph windows** are separate frames: `TimeSeriesGraph`, `CorrelateArraysGraph`, `CorrelateSingleVarsGraph`, `ThreeDCorrelationGraph`. Auto-scaling with a manual reset key, optional rolling-window mode, and — in the correlation graphs — datapoints joined in time order so direction of travel is visible in a scatter (E8's arrow plot).

**Interaction is mouse-drag plus single keys.** Left and right mouse buttons are each bound to a `VarChanger`; dragging changes that parameter, and the live value is printed bottom-left. `←`/`→` change a sensitivity `multiplier` in powers of two, so one binding covers coarse and fine adjustment. Keys, uniform across windows where they apply:

| Key | Effect |
|---|---|
| `↑` `↓` | move the `LOOKATME` agent selection |
| `←` `→` | mouse sensitivity, ×2 per press |
| `k` | kill switch — stop the run |
| `space` | toggle top/3-D view |
| `m` / `r` | reset graph max / min-max, i.e. rescale |
| `f` | toggle rolling-window ("follow") mode |
| `p` | save this window as a jpeg |
| `a` | save **every** window, tagged with the next alphabetic sequence |
| `w` | export the on-screen variable store (§5.8 — the only working data export) |

`a` explains the figure filenames throughout the LaTeX: `IB_CEUS1_Mon-Dec-03…__MainVizDensity.png`, where `IB` is `PersistentAlphabeticNumberer`'s counter. Every published figure is one keypress, and the prefix ties a figure to the run that produced it and to its sibling windows from the same instant. Crude, and more traceable than most plotting pipelines.

---

## 6. Parameter conventions that aren't obvious

**Ranged costs across Firms.** `ActorMaker` multiplies a base cost by Firm index:

```java
if (gl.deliveryRange) f.deliverycost = gl.deliveryCost * (j + 1);
if (gl.goodsRange)    f.goodCost     = gl.good        * (j + 1);
if (gl.wageRange)     f.wage         = gl.wage        * (j + 1);
if (gl.commuteRange)  f.commuteCost  = gl.commuteCost * (j + 1);
```

This is how **C7** gets its 2-and-4 delivery costs: `deliveryRange=true`, `deliveryCost=2`, two Firms → 2 and 4. And how **C6** gets its four wage bands: `wageRange=true`, `wage=1`, four Firms → 1, 2, 3, 4. (`vonThunen1.properties` has `wageRange=true`, `wage=1`, `overRideNumFirms=4` — confirming C6 directly.)

**Von Thünen assignment.** When `gl.vonThunenMode`, `ActorMaker` round-robins People across Firms (`p.vonThunenFirm = f`), and `PersonAction` then restricts both employment and buying to that one Firm. That is the "each Person permanently assigned to a single Firm" of C6.

**Agent counts.** `overRideNumFirms` / `overRideNumPeople` (default −1) override a space-dependent formula. Every published config sets both explicitly, so the formula is dead weight.

**`width`** is 1 for line/transmission models and 2 for density models (matching the thesis: "set to 2 to allow People a more variable space to disperse into"). `DENSITYCOSTRADIUS` is correspondingly 1/32 at width 1 and 1/16 at width 2 — i.e. **the same absolute radius**, which reads as 1/32 of width only at width 1. The thesis' "1/32 of the width of model space" holds for the transmission models; the density models use 1/16 of a doubled width. Same physical radius either way.

**`modelRunDays = 0`** in every config means "run until killed" — these are interactive scenarios, not batch runs. Consistent with the thesis' emphasis on the model as an experiment station.

---

## 7. Discrepancies, dead code and gotchas

Ordered by how much they'd cost us if missed.

1. **~~ρ ≥ 1 branch looks wrong.~~ RESOLVED — it is wrong, and it never mattered.** When `rho >= 1`, `CESOneTypeUtility` computes `optimalChosenAmount = price * budget`, where the ρ→1⁻ limit is `budget / price`. Confirmed by experiment in the refactor: at ρ=1.0 two Firms co-locate (separation 0.05), at ρ=0.99 they split (0.34), and substituting the limit formula at ρ=1.0 restores the split (0.348) in line with ρ=0.99 and 0.999.

   **But ρ = 1 was never reachable.** Both places ρ can change clamp it to 0.99 — `UtilityMouseChanger` (`gl.rho = (gl.rho > 0.99 ? 0.99 : gl.rho)`) and `CES_Sweeper`. Every figure captioned "ρ = 1", including the two in §5.5.1, was run at 0.99 through the ordinary branch. The bug sits in code the thesis never executed. Any port should use 0.99 where the thesis says 1.

2. **Commute is not doubled.** `PersonAction` computes `remainingTime = 1 - (distance * f.commuteCost)`. The thesis equation is `l_af = t_a − 2·t_af`, and the code's own comment three lines above says `//2*distance = commute cost`. The factor of 2 isn't there. Either `commuteCost` absorbs it by convention, or the half-day cutoff is effectively a full-day cutoff. Affects C3 and C6 quantitatively.

3. **Noise magnitude.** Code gives `±noiseUtil/2` of the optimal amount; the thesis says `noise=0.5` means "plus or minus half its total value", which would need `±noiseUtil`. Factor of two. Relevant to D2's noise levels (0, 0.1, 0.5).

4. **Noise doesn't reach utility.** `optimalChosenAmount` gets noise; `b.maxUtility` accumulates the *clean* `goods[i]`. So People buy noisily but report utility as if they hadn't. Defensible (the noise represents execution error, not preference error) but it must be a deliberate choice in the port, and it means Firms see noisy demand while the utility plots stay clean.

5. **Switch fall-through in `Bundle.addToBundles`.** `case Line:` has no `break`, so it falls into `case Torus:`. On a line, each requested short-range bundle adds *two* bundles — one 1-D and one 2-D-in-a-circle. Same for the model-wide loop. So D1/D2/E7 line models actually get ~2× the intended bundles, with half of them holding a spurious y-offset. Since `peopleMobile=false` in all the line configs, `addToBundles` returns early and this never fires in the published runs — but it would fire immediately if we let People move on a line.

6. **`FirmActMarkup` is empty.** Both `actMode()` and `responseMode()` have no body. The markup DMR search was never implemented — matching the thesis' statement that "angular frequency makes DMR unmanageable" and that E2 explores markup "through blind Firm behaviour". The actual sweep is a commented block in `Firm.transmissionFirmsAction`:
   ```java
   // if (gl.day % 200 == 0 && ID == gl.people.size()) { lastGoodCostAtMarkupChange = goodCost; markUp += 0.002; }
   ```
   That is E2's parameter sweep: +0.002 markup every 200 days on Firm 0. Recoverable, and worth uncommenting into a config flag.

7. **`ActorMaker` unconditionally attaches a markup action to Firm 0** (`actors.get(numPeople).addAction(new FirmActMarkup(...))`, not commented). Harmless while `markUp=0`, and it's what `actions.get(1).heard(null)` in `Firm` invokes when price flux drops below 1e-8. But it means Firm 0 is structurally different from every other Firm in every run.

8. **`getWageOffer()` vs `wage`.** `PersonAction` *tests* with `getWageOffer()` (which returns `gl.peopleFixWage` when set) but *assigns* `bestWage = this.f.wage * remainingTime`. Divergent when `peopleFixWage > 0`. Every shipped config has it at 0.

9. **~~`goodCost = 3` overwrite.~~ ↻ RESOLVED — use the thesis' range, because the constant does not converge.** The randomised initial price is immediately overwritten by 3, and the thesis' E1 damping figure describes "initial good costs randomised between 0.1 and 1.5" — matching *neither* the random line (`rand*5 + 1` → 1 to 6) nor the constant. This was flagged as a free parameter needing a decision. It is not free: from a uniform 3.0 with distance costs, 3 of 50 Firms run away permanently, and from 0.1–1.5 none do (§5.6). Initial price is a load-bearing parameter of the transmission belt, not a cosmetic one.

10. **Ten `FirmAct*` variants, one live.** `FirmActRespondAverage` is the only one wired in `ActorMaker`; `FirmActMarkup` is attached as a secondary. The rest — `FirmAct`, `FirmActDummy`, `FirmActFeb11`, `FirmActFeb11_v2`, `FirmActRespondAvMCMR`, `FirmActRespondAverageBackup`, `FirmActRespondAverageMCMRtest`, `FirmActRespondAverageOneMove`, `xxFirmActFeb11_v3` — are history. **This answers open question 3 in the digest: only one variant produced published results.** The MCMR ones are abandoned attempts at marginal-cost/marginal-revenue Firms and are interesting only as evidence for the thesis' argument about what didn't work.

11. **↻ Costs are copied onto Firms, so changing the global does nothing.** `ActorMaker` writes `deliveryCost`, `goodCost`, `wage` and `commuteCost` onto each Firm at construction — it has to, since the `*Range` conventions of §6 make them differ per Firm. Every subsequent read is from the Firm's copy. So setting the global alone has no effect whatever, silently: the model runs, nothing errors, and the parameter you thought you were sweeping is inert. In the original this is handled by each `*MouseChanger` iterating `gl.firms` and writing to every Firm; it is easy to miss that this is *why* they do that. Reproducing it wrongly in the reimplementation disabled every distance-cost control across two build stages and flattened B2's double dip into a straight line before anyone noticed. Any port needs a single choke point for cost changes that re-applies ranging.

12. **↻ A discrepancy in the other direction: D2's ρ dependence does not reproduce.** §5.5.2's headline is that under noise the DMR game gets *harder* as ρ falls, because stronger love of variety means each Firm's move has a larger external effect on the other's demand. An independent reimplementation shows ρ making little difference, and low ρ if anything *easier*. Ruled out: noise amplitude (both readings, per item 3), distance cost 1 vs the shipped 20, and substituting the hill-climber's budget-conserving noise for the additive noise — that last makes every game much harder (up to 139 days) without changing the direction. What *does* reproduce is the mechanism the model exists to show: a lone Firm optimises easily, a competitor degrades it, noise degrades it further. There is a coherent mechanism for the observed direction — at high ρ demand is nearly winner-take-all, so a Firm at the edge captures almost nothing and the revenue change it is trying to read is swamped — but a coherent mechanism for the opposite result is not the same as an explanation of the discrepancy. Unlike items 1–10, this one is not a code-vs-thesis mismatch that can be read off the source. Either the published run differs from the setup described, or something in the reimplementation does. **Open.**

13. **Other dead branches worth knowing:** `LandLord`/`LandLordAction` (an explicit land-market alternative to the density cost, `landCostsOn=0` everywhere); `OptimiseGenetic` and `Chromosome` (an evolutionary optimiser, never wired); `CESPlusNecessityGoodUtility`; `firmsMoneyFlowOn`/`firmsGoodsFlowOn` (false everywhere — this is the thesis' decision to abandon money flow, visible as two switches); `wrapSpace` (false everywhere, and it throws `System.exit(0)` if combined with `peopleConsiderAll`); `threeDview`, `reliefLines`, `singleActor` + the whole `Backyard*` family.

---

## 8. What I'd recommend for the port

The engine is genuinely small. Stripping visualisation, CSV plumbing, dead Firm variants and unused libraries, the load-bearing logic is roughly **1,500 lines**: space and distance, the bundle cycle, CES (two optimisers), smooth EoS, the granular stock-target loop, the DMR crawler, density cost, and the two-bin day loop. That is a comfortable single-module JavaScript port.

**↻ Measured, that estimate was about right.** The reimplementation's engine — `engine` + `econ` + `behaviour` + `config` + `record` — is **2,824 lines of Java**, of which a substantial fraction is javadoc explaining *why* rather than code. The 28 scenario classes add 2,506 more, but those are declarations, not mechanism. Against roughly 29,000 non-graphics lines in the original, the collapse is about 10:1, and no model behaviour was lost getting there.

Four suggestions:

1. **Make scenarios data, not code.** Everything `Main.java` does by commenting — recorded variables, chart pairings, agent placement, sweeps — should be declared in a scenario object. The reflection-based `SpaceTools.method = people,0,1` lines in the configs show the author was already moving this way.
2. **Kill `gl`.** Pass an explicit config/state object. This is the change that makes the models testable and makes two scenarios runnable on one page.
3. **Keep the seeded RNG.** `Randoms` wraps a single seeded generator with `incrementSeed()` for Monte Carlo. Preserving seeded reproducibility means the site can offer "same run every time" *and* "reroll", which is pedagogically useful — especially for E5/E8, where which Firm wins is genuinely down to chance.
4. **Port the algorithm as written, then fix separately.** For each item in §7, reproduce the original behaviour first and verify against the published figures; only then decide whether to correct it. Where we do correct something, that's a good pop-out: "the thesis equation says X, the code did Y, here's the difference" is exactly the kind of thing this site can show that a PDF can't.

---

## 9. Open questions

**Closed since the first draft:**

- ~~**The ρ ≥ 1 demand branch.**~~ Wrong, and never executed — ρ was clamped to 0.99 in both places it could be set, so every figure captioned "ρ = 1" ran at 0.99 through the ordinary branch. §7.1.
- ~~**E1's initial price distribution.**~~ Not a matter of memory: the constant 3 does not converge, the thesis' 0.1–1.5 does. §7.9.
- ~~**Java runtime.**~~ Installed. The jars run and `javap` decompiles them (§3.1) — but the hoped-for payoff, capturing reference CSV to diff against, is unavailable for a different reason: the shipped builds' CSV export is broken (§5.8). Verification is against published figures, hand-deducible equilibria and qualitative claims instead.

**Still open, for Dan:**

1. **The commute factor of 2 (§7.2).** Was `commuteCost` meant to absorb the round trip, or is the doubling genuinely missing? Affects C3 and C6 quantitatively. Implemented both ways behind a `Quirks` flag; neither has yet been shown to change a published result.
2. **Noise amplitude (§7.3).** Same status — code gives ±`noiseUtil`/2, the thesis text implies ±`noiseUtil`. Both implemented, neither distinguishable from a figure so far.
3. **D2's ρ dependence (§7.12).** New, and the one real disagreement between the thesis and an independent reimplementation. Does the §5.5.2 setup match what is described there?
4. **C3 and C7 configs are gone (§4).** Reconstructable from the thesis text plus the `*Range` conventions in §6, but if older configs or a backup of `Main.java` exist outside this repo, they would be worth recovering. (`configs/backups/` holds one file, from Oct 2012.)
5. **`E3` agent count.** `ConnectedModels/EoS_LoV_noSpace.properties` sets 2 People; the thesis describes the regime transition with 1 Person "to make it possible to compare to a very simple deductive version". Both may be published; the markup section notes "two People are used in this run to indicate the basic dynamic functions the same as for one".

---

## ↻ 10. What the reimplementation settled

[`spatialecon-java/`](spatialecon-java/README.md) rebuilds the models from this document rather than from the original code, which makes agreement evidence and disagreement interesting. 28 of the catalogue's models are built and 55 tests pass. Full workings are in that README; this is what it means for the account above.

**Confirmed by an independent build.** The mapping in §4 holds everywhere it was checked. C1's mean utility comes out at 0.898 against the thesis' "close to 0.9". B1 puts 100% of demand at the nearest Firm as ρ→1; B2's double dip appears where predicted (0.100 → 0.140 → 0.062); B3's People converge 7× faster than its Firms; C8's utility–distance correlation goes −0.39 → −0.96 → −0.97 as density cost rises; D1d produces the Hotelling split; E3 breaks symmetry at ρ = 0.764 with no space involved; E7 triples efficiency without lowering anyone's utility. The transmission belt hits four hand-deducible equilibria to five decimal places (§5.6).

**Corrected here as a result:** §3.1 (what each build actually displayed), §5.6 (the crawl draw, the analytic checks, initial prices), §5.8 (CSV export is broken), §5.9 (the visual language), §7.9 and §7.11, §8's line estimate.

**~~Not reproduced: the magnitude shortfalls.~~ ↻ RESOLVED — they were one cause, and it was measurement.** E8's ratio reaching 1.26 against ~13, E6 crossing the boundary once in 20,000 days, and E7's welfare claim holding only in trend looked like three separate weaknesses. The suspicion that a one-sided failure implies a shared cause was right, and §3.2 has it: the published figures plot `productionLevel` and `goodCost` where the reimplementation measured `Output_per_unitTime`. Reading the right variables gives E8 a ratio of **13.55 at ρ = 0.9** and E5/E6 threading the boundary repeatedly. No model behaviour changed; nothing was tuned.

**Still not reproduced:** D2's ρ dependence (§7.12), which remains the one genuine disagreement.

**A note on method.** The `Quirks` flags exist so that the four known code-vs-thesis divergences (§7.1–7.3, §5.3) are switchable rather than silently decided. Their default is **original behaviour**, on the grounds that the original produced the figures. So far, flipping any of them changes no published result — which is itself worth recording, since it means those discrepancies are real but immaterial, and the thesis' conclusions do not rest on them.
