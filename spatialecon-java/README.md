# spatialecon-java

The models from Dan Olner's 2013 PhD, refactored so each one is a self-describing,
independently runnable thing.

The original (`../Thesis_on_github/FrameworkModelSourceCode/`) is untouched and remains the
authoritative record of what the models do. This is a clean reimplementation of the same
mechanisms, and the specification for the JavaScript port that follows.

See `../THESIS_DIGEST.md` for the model catalogue and `../CODE_ANATOMY.md` for how the
original was built.

## Build and run

Needs JDK 17 at `/opt/homebrew/opt/openjdk@17` — see `../CLAUDE.md` for why not a newer one.

```sh
make core                                # build (no external dependencies)
make list                                # what models exist
make run ARGS="smoke --info"             # one model's setup
make run ARGS="smoke --days 200 --out data"
```

## Layout

```
src/spatialecon/core/
  engine/     World, Timeline, Space, Actor, Person, Firm, Pt, Placement, Rng
  econ/       Utility + Ces, Production + SmoothEoS
  behaviour/  PersonBehaviour, FirmBehaviour
  config/     ModelConfig, Parameter, Quirks
  record/     Recorder, Series, Aggregation
  scenario/   Scenario, ScenarioRegistry, models/
  Runner.java
src/spatialecon/viz/    Processing renderers (needs lib/*.jar)
reference/              output from the original, and how to get more
```

`core` has **no external dependencies** — it compiles, runs headless and tests against the
JDK alone. Only `viz` needs the Processing jars. That is not an accident of this design: an
audit of every non-JDK import in the original found Guava used for exactly one class and
OpenCSV only in a replay path that is not carried over.

## What changed, and why

Four things in the original made it hard to work with. Each collapses.

**Scenarios were not data.** Choosing a model meant editing `Main.java`, `ActorMaker.java`
and `Visualiser.java` and recompiling. No checkout corresponded to any published figure, and
two scenarios were lost outright. Now a model is a `Scenario` class that states its own
parameters, recordings and controls, and any number coexist.

**`gl` held everything.** 131 public static fields, four near-duplicate `setUp` methods.
Now `ModelConfig` (structure final, costs tunable) plus state on the objects that own it.
Notably `rho` is instance state on `Ces` rather than static, so identical tastes is a choice
a scenario makes rather than something the type system imposes.

**29 `*MouseChanger` classes** differed only in which field they poked. Now one
`Parameter` with getter/setter lambdas.

**49 `Bucket` + 14 `VariableStore` classes** differed only in aggregation and which field
they read, pulled by reflection on a name string so a typo gave a silent empty column. Now
`Recorder` with typed accessors and an `Aggregation` enum.

## Quirks

`CODE_ANATOMY.md` §7 lists places where the 2013 code departs from what the thesis text
says — the density-cost divisor, the missing commute round-trip factor, noise amplitude, and
the ρ ≥ 1 demand branch. Rather than quietly picking one, both readings are implemented and
selectable via `Quirks`. `Quirks.ORIGINAL` is the default everywhere, because that is what
produced the published figures. Flipping one is an experiment, not a fix — until a flag is
shown to change a published result, we do not know which reading the figures used.

## Tests

```sh
make test        # 25 assertions, JDK + JUnit only
```

These check the economics against values workable by hand, because everything later is built
on them: even splits at equal prices, budgets exhausted exactly, the closed form
`U = Y·n^(1/ρ−1)`, Cobb–Douglas demand independence and unit elasticity, `SmoothEoS(1,1)` at
t=0.5 giving 0.75, Core Model output going negative below α, and the two optimisers agreeing.

One test pins current behaviour rather than asserting correctness — the ρ ≥ 1 branch, where
the original and the ρ→1⁻ limit disagree (CODE_ANATOMY §7.1). It asserts both readings so
that whichever we later conclude is right, the change is visible.

## Status

**Stage 0** — engine, config, recording, scenario framework, headless runner.
**Stage 1** — econ layer (`Ces`, `StoneGeary`, `SmoothEoS`, `CoreModelEoS`, both optimisers),
25 tests, and models A1–A4.

```sh
make run ARGS="a1 --out data"    # CES and love of variety
make run ARGS="a2 --out data"    # Cobb-Douglas: the Firm with no lever
make run ARGS="a3 --out data"    # two production functions
make run ARGS="a4 --out data"    # hill-climber vs analytic
```

Group A are function explorers rather than simulations, so each *day* is a sweep step and
`beforeDay` walks the parameter. Verified against closed forms: A1 matches
`Y·n^(1/ρ−1)` to zero error across the sweep; A2 shows demand for the untouched good moving
by exactly 0 across a 15× price change in the other; A3 puts the Core Model form negative for
exactly the first half of its range; A4's two optimisers converge to an identical utility.

**One refinement to the thesis.** §5.2.3 says more goods optimise faster. Measured here, that
holds in *absolute* error at low iteration counts — 8 goods runs at about a third the error
of 2 goods between 5 and 200 iterations — but it reverses once both converge: by 1000
iterations the 2-good case is essentially exact (~1e-6) while 8 goods sits two orders worse.
The thesis illustrates the early part of the curve, so its claim stands; the scope is now
pinned by two tests. Worth knowing for the website, since a plot stopping at 200 iterations
tells the opposite story to one running to 2000.

**Stage 2** — the `Bundle` cycle, the density cost, `PersonBundle` and `FirmLocationCrawl`;
models B1–B3, C1, C2, C6, C8. Headless; the interaction layer is its own later stage.

| Model | Claim | Result |
|---|---|---|
| B1 | at ρ→1 the nearest Firm takes everything | **100%** of demand at ρ=0.99 |
| B2 | nearby demand rises before it falls | dip confirmed: 0.100 → **0.140** @ cost 4.0 → 0.062 |
| B2z | a Firm at distance 0 never turns over | monotonic 0.100 → **0.637** |
| B3 | People converge fast, Firms slowly | halving times **3** vs **21** days — a 7× gap |
| C1 | utility equalises across distance | mean **0.898** — §5.4.1 says "close to 0.9" |
| C1 | density falls linearly with distance | corr **−0.986** |
| C6 | non-spatial cost sorts one way… | best-off group settles **closest** (0.10 vs 0.47) |
| C6c | …spatial cost the other | best-off group settles **furthest** (0.25 vs 0.07) |
| C8 | equilibrium breaks at high density | corr(distance, utility) **−0.39 → −0.96 → −0.97** |

C1's mean of 0.898 is the closest thing to a numerical match with a published figure so far,
reached by an independent reimplementation.

### Why C1's utility is not perfectly flat

The thesis assumes `du/dd = 0` — utility equal regardless of distance. C1 gets close but shows
a mild gradient (corr −0.22 to −0.39 across seeds). Investigated, because it looked like a bug.
It is not. It is a boundary effect of a finite settlement, and it is worth understanding
because it is the same mechanism that produces C8.

With one Firm the CES collapses to a single good and utility has a closed form:

```
U = (wage − D(d)) / (p + c·d) = (1 − D(d)) / (1 + 0.25d)
```

Setting `dU/dd = 0` requires the density profile to be exactly linear:

```
D(d) = (1 − k) − 0.25k·d        which reaches ZERO at d = (1 − k) / 0.25k
```

Two consequences. The model does produce that linear profile — corr(distance, density) is
−0.986, and the band means sit within ±0.003 of the equalising line with no systematic trend.
But **density cannot go below zero.** Beyond the cutoff there is nothing left to trade away,
so those People pay pure distance cost. Their utility tracks `1/(1 + 0.25d)` to four decimals
and falls with corr **−0.966**. They are not in disequilibrium — no move improves them — they
are simply outside the range over which equalisation is possible.

Ruled out along the way:

- **Staleness** — density is recorded when a Person acts, so later movers could invalidate it.
  Measured drift between action-time and end-of-day density: exactly **zero**, because at
  equilibrium nobody moves.
- **A missing deadband.** The original has none: the current-location Bundle is evaluated
  first and the comparison is a strict `>`, so ties keep the incumbent. This implementation
  matches. A deadband would in any case widen the inequality, not close it.
- **Sampling granularity** — the first guess, and wrong.

The decisive test was population, since a finite-population boundary effect must wash out as
the population grows:

| People | corr ALL | fringe % | cutoff *d* |
|---|---|---|---|
| 125 | −0.823 | 43.2 | 0.278 |
| 250 | −0.485 | 20.4 | 0.386 |
| 500 | −0.423 | 7.2 | 0.456 |
| 1000 | −0.238 | 1.9 | 0.496 |

The fringe collapses from 43% of the population to 2%, and the gradient goes with it. The
interior correlation has no monotone trend (−0.41, −0.12, −0.32, −0.20), which is what
stochastic scatter looks like rather than a systematic slope.

**So `du/dd = 0` holds in the interior, and holds better the larger the population** — the
idealisation is a large-numbers one, exactly as §4.7.8 argues about agent number generally.

The pleasing part: this is C8's mechanism in miniature. Raising the density cost pushes the
cutoff inward until the fringe swallows the settlement. C1 and C8 are not two regimes but one
continuum, and the fringe is already present at density 10 — just small enough to overlook.
Good website material: the equalising line and the point where it hits zero can both be drawn.

**A bug worth recording.** Firms carry their own copy of each cost — they must, since ranging
makes them differ — so changing a cost on the config alone never reached them. That silently
disabled every distance-cost control and flattened B2 into a straight line. Live cost changes
now go through `World.setDeliveryCost` and friends, which re-apply ranging; there is a
regression test. The original had the same split and solved it the same way, with each mouse
control iterating `gl.firms`.

**Stage 3** — demand noise, and models D1 (two Firms on a line) and D2 (the DMR game).

| Model | Claim | Result |
|---|---|---|
| D1a | ρ=0.1, dist 1: tight central spot | separation **0.065** |
| D1b | ρ=0.99, dist 1: central, wandering | separation **0.221** |
| D1c | ρ=0.1, dist 20: centre-ish, spread | separation **0.164** |
| D1d | ρ=0.99, dist 20: **Hotelling split** | separation **0.343** ✓ |
| D2 | a lone Firm finds its market easily | yes; a second Firm slows it, noise slows it further |
| D2 | noise raises days-to-succeed | 15 → 17 → 32 at noise 0 / 0.1 / 0.5 |
| D2 | under noise, low ρ is much harder | **not reproduced** — see below |

### The ρ ≥ 1 question, resolved

CODE_ANATOMY §7.1 flagged the ρ ≥ 1 demand branch as possibly wrong and unresolved. It is
wrong, and it never mattered.

D1d was the test. At ρ = 1.0 the Firms co-locate (separation 0.05); at ρ = 0.99 they split
(0.34), which is what the thesis reports. The original's branch computes `price × budget`,
so moving *away* from a buyer *increases* what they buy — an inverted signal. The correct
ρ→1⁻ limit is `budget / price`, and substituting it at ρ = 1.0 restores the split (0.348),
matching ρ = 0.99 and ρ = 0.999 exactly as continuity requires.

It never affected a published result because **ρ = 1 was unreachable**. Both places ρ could
be changed clamp it:

```java
gl.rho = (gl.rho > 0.99 ? 0.99 : gl.rho);          // UtilityMouseChanger
} else if (gl.rho > 0.99 && !printed) { gl.rho = 0.99;   // CES_Sweeper
```

So every figure captioned "ρ = 1" was run at 0.99, through the ordinary branch. The bug sat
in dead code for twelve years. D1's "substitutes" variants therefore use 0.99, not 1.0 —
using 1.0 would take a path the thesis never ran and give a qualitatively different answer.

### An unresolved discrepancy in D2

§5.5.2's headline is that under noise, games get *harder* as ρ falls, because higher love of
variety means each Firm's move has a larger external effect on the other's demand. **This
implementation shows the opposite**: ρ makes little difference, and low ρ is if anything
easier. Tried and ruled out: noise amplitude (both `Quirks` readings), distance cost 1 vs the
shipped 20, and replacing the additive noise with the hill-climber's natural budget-conserving
noise — that last makes games much harder overall (up to 139 days) but does not change the ρ
direction.

There is a coherent mechanism for what is observed. At high ρ demand is nearly
winner-take-all, so a Firm out at the edge captures almost nothing; its revenue is tiny and
the change it is trying to read is swamped. At low ρ every Person buys some of everything, so
even a distant Firm has a substantial revenue base and a readable signal.

What *is* reproduced is the mechanism the model exists to demonstrate: a lone Firm optimises
easily, competition degrades it, noise degrades it further. A test asserts the observed
direction so that any later correction fails loudly. Flagged for Dan — the setup may differ
from what the section describes.

**Stage 4** — the transmission belt: `PersonTransmission`, `FirmStockTarget`, and models
E1, E3, E5, E6, E7, E8. The highest-risk stage, and the one with the strongest checks
available, because several equilibria are deducible by hand.

With `x = l² + l`, a Firm receiving `T` units of time clears stock at price `1/(T+1)`:

| Model | Deduced | Measured |
|---|---|---|
| E1 — 1 Firm, 1 Person | 0.5 | **0.50000**, stock 5e-15 |
| E1many — 50 Firms, 50 People | 0.5 | **0.50000**, stock 4e-15 |
| — 20 Firms, 100 People (T=5) | 1/6 | **0.16667** |
| E3 — symmetric two-Firm | 2/3 | **0.66670** |

Other results:

| Model | Claim | Result |
|---|---|---|
| E1raw | undamped targeting fails | never settles — see the correction below |
| E1many | People's utility "umbilically linked" | identical to 9 decimal places |
| E3 | symmetry breaks near ρ=0.75, **no space needed** | breaks at **ρ=0.764**; winner's price → 0.5002 |
| E3 | the Person drives the shift | utility turns from falling to rising at the transition |
| E5 | ρ and distance cost both trigger the same change | 26 boundary crossings threaded, ρ 0.65→0.73 |
| E7 | efficiency rises, nobody made worse off | efficiency ×3.4; **minimum** utility 0.165 → 0.491 |
| E8 | a winner emerges from **identical** Firms | ratio → 1.26 from ρ≈0.9; People migrate (0.558 → 0.306) |
| — | more input → lower clearing price | corr(contributed time, price) = **−0.97** |

### Correction: the undamped run does not settle

This README previously recorded E1raw as "settles at 0.299, stock never clears (7.31)". That
was a single end-of-run sample read as a resting point. It is wrong, and the JavaScript port
caught it: re-running the same build gives 0.42 at the same day, and sampling the last 100
days shows price swinging 0.16–1.12 with stock at ±9.35. It is still oscillating, and always
was.

Which is what the thesis says it should do — §6.3.1 calls it an undamped harmonic oscillator,
and an undamped oscillator is precisely the thing that *never* settles. The original claim
accidentally contradicted the mechanism it was meant to demonstrate.

Both implementations oscillate forever; they differ only in amplitude (Java ±9.35 stock,
JS ±0.068), and that follows from the randomised starting price, since an undamped
oscillator keeps whatever displacement it began with. The damped run settles at exactly
0.50000 with stock at 4e−15 in both.

The lesson for the rest of the verification: **assert the behaviour, not a sampled value.**
A test asserting "price ≠ 0.5 and stock > 0.1" passes for the wrong reason and would have
gone on doing so. The JS test asserts that the swing over a window is non-zero for the
undamped case and zero for the damped one, which is the actual claim.

### Starting prices are load-bearing

The thesis mentions in passing that one figure randomised initial prices to 0.1–1.5. That
turns out to decide whether spatial runs converge at all. From a uniform 3.0, **3 of 50 Firms
overshoot into the runaway of §6.3.2** and never recover; randomised near equilibrium, none
do. The first few price corrections are proportional to a stock error that is enormous when
the price starts far out, and the Firm never gets back. Hence `randomiseInitialPrices`, and a
test asserting both behaviours. This answers the open question flagged at Stage 1 about E1's
initial-price distribution.

### Where Stage 4 falls short

- **E7's welfare claim holds in trend, not instant by instant.** Minimum utility roughly
  triples across the run, so nobody ends worse off, but individual samples show local dips
  while efficiency is rising. The strong reading — *never* worse off at any moment — is not
  verified here.
- **E8 transitions, but weakly.** Efficiency ratio reaches 1.26 against the thesis' ~13, and
  needs ρ pushed to about 0.97 rather than 0.9. The direction, the migration and the
  endogeneity are all reproduced; the magnitude is not. Likely a matter of running further
  past the transition, which is worth revisiting.
- **E6 crosses the boundary once** in 20,000 days rather than threading repeatedly. The
  restore condition is hard to reach with four Firms.

Remaining stages: sweeps and markup (C3, C5, C7, E2, E4), the demonstrations (F), and the
interaction layer.
