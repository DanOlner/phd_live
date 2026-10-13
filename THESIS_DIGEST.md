# Thesis digest & model catalogue

**Source:** Dan Olner (2013/14), *Agent-based modelling and spatial economics* — `Thesis_on_github/DanOlnerThesisPrintDraft23_6_13.pdf`, LaTeX in `ThesisCorrectionsLatex/`, Java in `thesis_on_github/`.

**Purpose of this document:** a working digest of the thesis argument plus a catalogue of every model in it, written so that each entry can become an interactive JavaScript piece with pop-out explanation on the website. Section numbers refer to the thesis.

---

## 1. The thesis in one page

**The question.** How do changing *space costs* — the costs of moving people and goods across distance — reshape the economic landscape, and how does the resulting shape feed back into those costs? This is framed as topical (climate, energy transition, relocalisation debates) but pursued as a theoretical/methodological problem.

**The gap.** Agent-based modelling (ABM) looks perfectly suited to this. Space imposes *heterogeneous* costs — from where each agent stands, the costs differ — and rules out a single market-clearing auction. Those are exactly the two things ABM is good at and neoclassical economics has to assume away. And yet ABM has barely touched general spatial economics. Why?

**The method.** Build a spatial economic agent framework from first principles, and use the *development process itself* as the evidence. Every place the model got stuck is a finding. Alongside it, "disassemble" the canonical alternative — Krugman's Core Model of geographical economics (GE) — to compare what each approach can and can't do.

**The two headline obstacles identified:**

1. **Dependencies.** Borrowed from OOP, where dependencies are "the villain of the piece" and loose coupling is the goal. In modelling social systems, dependencies are unavoidable and are *part of the model's meaning*. Choosing a CES demand function forces a particular supply-side structure; choosing constrained optimisation forces demand to be met instantaneously, which forces firms out of instantaneous decision-making; and so on. ABM's mistake is believing it can transcend these.
2. **Year zero.** A widespread belief that computation broke ABM free of modelling history, so nothing can be learned from economics. The thesis argues the opposite via Friedman (1953): the "photographic reproduction vs engine of analysis" distinction is exactly as live today.

**The supporting distinction: descriptive vs functional mapping.** ABM's OOP roots push modellers towards mapping the world *descriptively* onto code objects (one agent = one person). The thesis argues for *functional* mapping: a single static Utility class serving all People is fine; agent number can be small while still "assuming large numbers"; a production function need not be a plausible account of production if its job is to link production to welfare.

**The models.** Three main setups plus a stack of testing models:
- **Mobile People** (partial) — People optimise location against a *density cost*; Firms fixed.
- **Mobile Firms** (partial) — Firms grope for revenue-maximising locations; People fixed. Introduces the **DMR** ("data, meaning, response") problem.
- **Transmission belt** (general) — supply and demand fully linked. People buy goods directly with time; Firms use granular, damped price adjustment to target a stock level; economies of scale in production feed through to consumer welfare. The model finds stable equilibria with heterogeneous locations and can transition catastrophically between *production regimes*.

**What the transmission belt achieves and what it lacks.** Four of five "general" components work: demand side, supply side, firm profit, and a price system tying them together. The fifth — genuine competition between Firms — does not emerge. That failure is reported honestly and analysed as a dependency consequence, not hidden.

---

## 2. Key concepts (the glossary the website needs)

| Term | Meaning in the thesis |
|---|---|
| **Space costs** | Umbrella term. Split into **distance costs** (moving goods/people) and **costs & benefits of proximity** (density, congestion, land, externalities). |
| **Love of variety (LoV)** | Consumers prefer a mix of goods to a narrow range. Controlled by ρ in the CES function. |
| **Elasticity of substitution** | The mirror image of LoV. As ρ → 1 goods become perfect substitutes and LoV vanishes. |
| **Economies of scale (EoS)** | Increasing returns in *production*. Kept deliberately distinct from increasing returns in *utility* (which is what LoV is). Krugman's Core Model collapses the two; the thesis keeps them separate so their interaction can be studied. |
| **Density cost** | The thesis' proxy for land cost + congestion + all proximity costs. Centred on each agent, computed from who else is nearby. Cheap to state, essentially impossible outside ABM. |
| **Bundle** | A candidate location with all the economics attached: good prices incl. delivery, best available wage incl. commute, density cost incurred. People compare Bundles and move to the best. |
| **DMR (data, meaning, response)** | From Jane Jacobs. An agent gets a datum, assigns meaning, responds. The meaning step is where agents go wrong. |
| **Transmission belt** | The linkage from production efficiency through to consumer welfare. |
| **Partial vs general** | Partial = look at supply or demand, hold the rest fixed. General = link them and claim to represent a whole economy. |
| **First vs second nature** | First nature = intrinsic properties of a place. Second nature = properties arising from what other agents do. The density cost is purely second nature. |
| **Core Model (CM)** | Krugman 1991. Two regions, mobile manufacturing workers, immobile agricultural workers, iceberg transport costs, monopolistic competition via Dixit–Stiglitz. |
| **Streetlight effect** | Looking for the keys under the lamp. The thesis argues ABM has a worse case of it than the economics it wants to supersede — and less awareness of it. |

---

## 3. The mathematical toolkit

Everything the JavaScript ports will need. Small, closed-form, nothing computationally heavy.

**Cobb–Douglas utility** (§2.2.2)

    U = F^δ · M^(1−δ),  0 < δ < 1

Constrained optima: `F = δY/p_F`, `M = (1−δ)Y/p_M`. Note the property the thesis exploits: price of one good does *not* appear in demand for the other. Each sector is independently unit elastic.

**CES utility, n goods** (§2.2.3) — the workhorse

    U = ( Σ_{i=1..N} c_i^ρ )^(1/ρ),  0 < ρ < 1

ρ → 1: perfect substitutes, no LoV. ρ → 0: strong LoV.

**CES constrained optimum for a single good** (§2.2.4)

    g_j = ( P_j^(1/(ρ−1)) · Y ) / Σ_{i=1..n} P_i^(ρ/(ρ−1))

where `P` is *full* price (base + distance), `Y` the budget. This one equation drives most of the model behaviour on the demand side — it is where the externality lives, because every good's price appears in every other good's demand.

**Full price of a good with distance** (§4.3.1)

    P_g = p_g + (c · d)

**Cost of "buying money" with time** (§4.3.3) — the reframing that makes commuting parallel to goods delivery

    P_g = p_g / (1 − d/tech)

which for a time budget t=1, tech=1, p_g=1 gives `g = (1−d)·t / p_g`. Infinite cost at the hard time limit, which is what makes commuting different from goods.

**Best wage selection** (§4.6.1)

    l_af = t_a − 2·t_af          (labour left after a round-trip commute)
    bw_p = max { W_f · l_af  |  W_f ∈ F,  t_af < 0.5 }

**Smooth economies of scale** (§2.2.5) — the thesis' production function

    x = m·l + v·l²

`v` = curve parameter (scale of increasing returns), `m` = magnitude of the linear part. Normalised form `x = l + l²`. Contrast with the Core Model's `l = α + βx` (fixed + marginal labour), which produces *negative* output below the fixed labour requirement — fatal in an agent setting.

**Granular production allocation** (§4.7.5) — how EoS work without strategy

    g_a = f(T_p) · (t_a / T_p)

Total input `T_p` goes through the EoS function; the buying agent gets their proportional share of the output. Agents *leave* their time with the Firm until their next turn.

**Damped stock targeting** (§6.3.1) — the transmission belt's control law

    Δp_g = (s + s' + s'') / x

where `s` is stock, `s'` and `s''` are discrete first and second differences across timesteps, and `x` scales the response. Targeting only `s` produces a permanent, perfectly stable oscillation — a lovely teaching moment. Adding the derivatives damps it. With a markup, the target becomes `s + s' + s'' = mt`.

**Density cost** (§4.4)
1. For each other Person within radius r, normalise distance to [0,1] where 1 = same spot, 0 = at/beyond r.
2. Sum those, divide by total number of People in the run.
3. Multiply by a factor (usually 10; 100 and 500 used to break equilibrium).
4. Subtract the result directly from the Person's wage.

Radius = 1/32 of model width. Note it is independent of wage, so a higher wage shrinks it proportionally — which is why raising the wage is *agglomerating*.

**Core Model region equations** (§2.5) — for the CM comparison piece

Income `Y`, price index `I`, nominal wage `W`, iterated to convergence; then real wage `w_r = W_r / I_r^δ`. Parameters: δ (manufacturing share), λ (share of manufacturing workers in region 1), ε (elasticity of substitution), T (iceberg transport cost). Solved by simple iteration — guess W, compute Y and I, recompute W, repeat.

---

## 4. The framework being ported

**Spaces.** Spaceless point; two discrete regions; continuous line R¹; continuous plane R². Size normalised to 1 (2 for density-cost models). Euclidean distance. Torus/racetrack wrapping available.

**People have:** a location; one unit of time per day; a CES utility function with ρ; a density cost.
**Firms have:** a location; a wage offer; a base good price; a production function; a stock level.

**Timeline.** Each "day", an Action Permuter calls Actors in a random order. Crucially People and Firms are permuted in *separate bins*, so all People decide before any Firm does — this fixes a subtle bug where Firms measuring change between turns get faulty data because the gap between an agent's own actions has a pyramidal distribution, not a constant one day (§4.7.4 — worth its own small interactive).

**Three decision types:**
- *Instant* — gather info now, optimise now. (People choosing Bundles.)
- *Feedback* — act, wait a step, check if you're better off, repeat or revert. (Firms moving. The DMR mechanism.)
- *Reflex* — respond to another agent's request out of turn. (Granular price adjustment on each sale.)

**The Bundle cycle** (§4.6.1), the heart of People's behaviour:
1. Generate N Bundles at random points — some in a small radius nearby, some anywhere in space — plus one at the current location.
2. For each: find the best available wage from that point; find the optimal goods mix given all prices incl. delivery; compute utility; add density cost.
3. Move to the best Bundle, take the wage, buy the goods. Moving is free; only goods movement and commuting cost anything.
4. Repeat until nobody can unilaterally improve — a Nash equilibrium.

**Two optimisers**, both worth exposing on the site side by side (§4.6.1, §5.2.3):
- *Hill-climber*: randomly slice the budget across goods, jiggle the slices, keep the change if utility improved, else revert. Ratchets towards the optimum. Plausible-looking, flexible, can handle stock shortages, noisy, and expensive.
- *Constrained*: plug prices and budget into the CES optimum equation directly. Exact, fast, and causally backwards — the Person gets their optimal quantities and then has to feed them *back through* the utility function to find out how happy it made them.

---

## 5. Model catalogue

Each entry: what it is, what to vary, what you see, and the teaching point. These are the candidate interactives.

### Group A — Foundations (Ch. 2 & 5.2). Pure function explorers, no agents.

**A1. CES / love of variety explorer** — §2.2.3, §5.2.1
Sliders for ρ and number of goods. Show: two-good case with the Dixit–Stiglitz intuition (4 of one good vs 2+2); utility vs ρ; utility vs number of varieties at fixed ρ (rising at a decreasing rate under a budget constraint). *Point:* one parameter captures the whole substitutes↔variety spectrum, and it is the single most important number in the thesis.

**A2. Cobb–Douglas explorer** — §2.2.2, §5.2.2
Contour plot with isoquants, δ slider, budget line. Then the killer demo: two Firms, one fixes its price, the other sweeps its own. Firm 1's demand *does not move at all*. *Point:* Cobb–Douglas imposes strict unit elasticity, which means a price-setting agent has no lever to pull — and, spatially, no reason to move. A model's utility function can silently disable a whole class of agent behaviour. Also the honest note: the *explanation* here comes from the algebra, not from the simulation.

**A3. Increasing returns: two production functions** — §2.2.5
Core Model style `l = α + βx` vs smooth `x = ml + vl²`. Plot total, average and marginal output. *Point:* below α, the Core Model function returns negative output. Analytically fine; in an agent model, fatal.

**A4. Hill-climber vs constrained optimiser** — §4.6.1, §5.2.3
Watch 16 randomised budget slices converge on 0.0625 each; graph marginal utility gain per iteration (nearly all the gain arrives immediately). Slider for iteration count; compare mean and SD against the exact optimum for 2 vs 8 goods. *Point:* the "realistic" method and the "spooky" method agree; the analytic one wins on cost. And the quirk: *more* goods optimise *faster*, because the random budget slices have less variance.

### Group B — Space enters (§5.3)

**B1. Love of variety meets distance** — §5.3.1
One Person at the origin, ten Firms evenly spaced from 0 to 1, same base price. Sweep ρ. With no distance cost, demand is flat regardless of ρ. Add distance cost and the demand profile tilts; at ρ=1 the nearest Firm takes everything. *Point:* this is the CES working as a spatial interaction function.

**B2. The double dip** — §5.3.1
Same setup, now sweep the distance cost c from 0 to 40. Nearby Firms' demand first *rises* (reallocated from distant ones) then falls as the absolute cost dominates. Toggle: put the nearest Firm at d=0 and the dip disappears — it absorbs all reallocated demand forever. *Point:* changing a cost does not move all agents in the same direction.

**B3. The black hole** — §5.3.2
20 People, 4 Firms, random start, everyone mobile, no density cost. Plot mean nearest-neighbour distance over 1000 days. Everything collapses to a point. People converge fast; Firms take far longer because they are groping via feedback. *Point:* without a counterforce, space deletes itself. This motivates everything that follows.

### Group C — Mobile People and the density cost (§5.4). The visual centrepiece.

Standard setup: R² plane, 500 People, Firms fixed, density cost factor 10, radius 1/32.

**C1. The monocentric result** — §5.4.1
People arrange themselves around a central Firm. Two linked views: the plane (square size/shade = density cost incurred) and the correlation plot (distance to centre vs density cost, one dot per Person). Then the payoff plot: **utility vs distance is flat at ~0.9**. *Point:* a spatial equilibrium emerges — everyone equally well off — from nothing but individuals trading density against distance. No market, no landlord, no bidding.

**C2. Density cost internals** — §5.4.2
Show each Person's radius; highlight a few and draw the neighbours entering their calculation. The hexagonal packing that emerges is striking and — the thesis is careful to say — economically uninteresting. Plot: number of neighbours in radius vs distance to centre.

**C3. Spatial vs non-spatial costs** — §5.4.3
Four parameter sweeps, each 0→1→0 in steps of 0.005, taking a datapoint when mean movement < 0.0001 over 10 iterations, plotted against mean distance from centre:
- wage ("cost of money") — **agglomerating**
- base good cost — **dispersive**
- commute cost — **agglomerating**
- goods distance cost — **agglomerating**

*Point:* non-spatial costs (wage, base price) and spatial costs (commute, delivery) push in **opposite directions**. Cheaper transport disperses; a higher real wage concentrates. This equivalence is what later licenses collapsing wage and goods into one exchange in the transmission belt.

**C4. Two regions, two People** — §5.4.4. A hand-computable thought experiment; ideal for step-by-step teaching.
Region 1 has no distance cost; region 2 costs 1 per unit. One Firm in region 1, wage 1, good 1. Density cost 0.5 if both People share a region. Utility is linear in goods. Only two sensible states: both in region 1, or one in each. Plot both states' utility as good cost and distance cost vary; the crossing points are where somebody moves. *Point:* whoever moves *leaves the other better off than themselves*. The equilibrium is driven entirely by externalities. In an ABM, market interaction is nothing *but* externalities.

**C5. Three regions, three People** — §5.4.4
Same idea extended: three states, two crossover points, and you can read off *which* Person is incentivised to move at each. Worth building as an animated walk along the x-axis.

**C6. Von Thünen without landlords** — §5.4.5
Four Firms at the centre; each Person permanently assigned to one; a quarter of People at each of four cost levels. Two runs:
- vary **wage** (0.5–2): the wealthy buy **proximity**.
- vary **commute cost** (0.25–1, plotted inverted so shading is comparable): the effectively-wealthy buy **distance**.

*Point:* clean von Thünen-like rings from a density cost and almost no assumptions — no landlords, no bid-rent curves, no market at all. And the direction of sorting flips depending on whether the cost is spatial.

**C7. Love of variety shapes settlement** — §5.4.6
Two Firms 1 apart. Distance costs differ: 2 per unit vs 4 per unit. 500 People, no commute cost. Sweep ρ from 0.99 to ~0 and back (to show hysteresis), plotting mean distance to the cheap Firm. High LoV → People cluster near the *expensive* Firm and buy the cheap good in from afar. Low LoV → they cluster near the cheap one. *Point:* **changing tastes alone, with no cost change at all, redraws the map.** Two distinct cores appear and swap dominance.

**C8. When spatial equilibrium stops being utility equilibrium** — §5.4.7
Crank the density cost multiplier: 10 → 100 → 500.
- At 10: flat utility, clean trade-off.
- At 100: People on the edge start avoiding density altogether. A second inner band appears; utility slopes down. Path dependence — people already holding good spots don't move until something better appears.
- At 500: nobody accepts any density cost. The settlement fully disperses; utility falls monotonically with distance.

*Point:* still a Nash equilibrium — nobody can unilaterally improve — but no longer a *utility* equilibrium. The spatial-equilibrium assumption that urban economics rests on is shown to hold only inside a parameter range. This is the model surprising its author, and the thesis says so.
*(Ships as a runnable jar: `MonoCentricDensity`.)*

### Group D — Mobile Firms and DMR (§5.5)

**D1. Two Firms on a line** — §5.5.1
R¹ line, 50 People evenly spread, two Firms that can move *only* (price and wage fixed at 1). Each Firm perturbs its position, waits one step, asks "did my revenue rise?", repeats or reverts. Display: mean Firm position with ±1 SD bars, a "barcode" of every position tried over the last 10,000 steps, and the demand profile for each Firm above and below the line.

Four cases:
| ρ | distance cost | outcome |
|---|---|---|
| 0.1 | 1 | both lock onto a tiny central spot |
| 1 | 1 | central on average, but wandering the whole line |
| 0.1 | 20 | centre-ish, much more spread |
| 1 | 20 | **Hotelling-like split to opposite ends** |

Plus a single-timestep snapshot showing demand overlapping (high LoV) vs binary (perfect substitutes) — a cut-off invisible in the averaged data.
*Point:* nothing like Hotelling's clean deduction. Firms are weak optimisers, and averaging hides that.
*(Ships as a jar: `TwoFirmsLineMove2000dayAverage`.)*

**D2. The DMR game** — §5.5.2. The sharpest single result in the partial models.
People occupy only 0.25–0.75 of the line. Both Firms start at opposite edges. The game ends when both get inside the People, or after 10,000 days. Sweep ρ; add a noise term to demand (0, 0.1, 0.5); average 100 random seeds per point. Plot: days-to-success vs ρ.

Result: with **no noise, ρ makes no difference**. With noise, high ρ is still fine, but as LoV rises (ρ → 0), Firms take dramatically longer — an order of magnitude worse at noise 0.5.

*Point:* a single Firm alone finds the centre trivially; the world is static so any change must be its own doing. Add one competitor and the meaning of "my revenue went up" becomes unreliable. Higher LoV means each Firm's move has a bigger external effect on the other's demand, so the signal degrades. Noise in the model arises *naturally* from the hill-climber — this is where A4 pays off.

### Group E — The transmission belt (§6.3–6.4). The main event.

Setup throughout: People buy goods **directly with time** through a CES function, so every Firm gets a slice of every Person's day (avoiding Bertrand winner-takes-all). Distance costs melt away. Firms turn received time into output via smooth EoS and adjust price on every single sale to hit a stock target.

**E1. Oscillation and damping** — §6.3.1
One Firm, one Person, no distance. First run: target zero stock only. You get a *perfectly stable oscillation* — stock hits zero exactly when price is furthest from equilibrium. Then switch on the derivative terms (`s + s' + s''`) and watch it settle to p = 0.5. *Point:* a classic harmonic-oscillator problem sitting inside an economic model, with a control-theory fix. Great step-by-step candidate: let the user add s', then s''.

Extensions in the same interactive:
- 50 Firms × 50 People, prices randomised 0.1–1.5, no distance → all converge to 0.5 (and *faster*, because each Firm gets 50 price responses per day).
- 50 × 50 with random R² locations and distance cost 0.25 → a **spread** of equilibrium prices. Correlate contributed time vs good cost: more input → more efficient → lower clearing price. *Point:* a stable general equilibrium with genuinely heterogeneous costs. No market-clearing price exists and none is needed.
- 20 Firms, 100 People, randomised Firm response magnitudes: all 100 People's utility lines lie exactly on top of each other. Utility varies over time but is identical across People at every instant.

**E2. Markup and the tipping point** — §6.3.2
Firms take profit by targeting a *rising* stock level and skimming the increase. Three demos:
- Sweep the EoS curve parameter 0→1 with Firm 0's markup fixed at 0.1: measure how far Firm 1's equilibrium price is dragged from its no-markup value. Zero effect at zero curve; grows with EoS. *Point:* without EoS, one Firm's markup changes *relative* prices only. With EoS it changes *absolute* prices for everyone — the whole economy's production structure shifts.
- Sweep Firm 0's markup 0→0.15 at EoS curve 0.5 and correlate the two Firms' prices: Firm 0's price rises, Firm 1's **falls** (it picks up the displaced demand, produces more efficiently, clears at a lower price).
- Sweep ρ from 0.1 to 1, raising markup until Firm 0 breaks. Plot the markup achieved at the breaking point against ρ. As goods become more substitutable, the sustainable markup collapses.

*Point:* the honest failure. Two possible endings — implicit collusion squeezing People until they can't buy anything, or one Firm tipping into a runaway feedback where it has lost too much demand to ever hit its target and keeps raising price forever. **No stable competitive outcome emerges.** People have no exogenous alternative and therefore no sanction.

**E3. EoS vs LoV: the regime transition** — §6.3.3
Two Firms, one Person, no space. Nudge ρ from 0.73 to 0.80 and watch four synchronised traces: output per unit input, stock, good cost, utility. One Firm pulls ahead, produces more efficiently, its stock rises then its price collapses, locking it in as *the* site of production. The driver, visible in the fourth trace, is the Person finding more utility by shifting demand.
*Point:* **you do not need space for a catastrophic production shift.** Dixit–Stiglitz rules this out by assuming symmetry.

**E4. Tracking down the transmission belt** — §6.3.4. The "why is this happening?" model. Currently an Excel sheet; a natural JS piece.
Four variable pairs: prices (p₀,p₁), demanded amounts (c₀,c₁) from the CES optimum, produced amounts (f₀,f₁) from smooth EoS, and the gaps (c−f). One law of motion: `p_n' = f_n − c_n`, applied by a small constant each step.

At symmetry: time splits 0.5/0.5, output = t²+t = 0.75, equilibrium price = 0.667. Perturb slightly.
- ρ = 0.73 → converges straight back to symmetry (~60 iterations).
- ρ = 0.80 → diverges to a new pair of equilibria, one efficient producer.

*Point:* the externality is located precisely — it lives *inside the Person's CES function*. Both Firms decide simultaneously, so it isn't timing or interaction. One Firm's price change alters demand for the other because both prices appear in the same constrained optimum, with ρ setting the strength. This is the thesis' answer to "emergence is not explanation": showing something interesting is the *start*, not the end.
*(Ships as `EoS_vs_LoV_ExcelModel_DanOlner2013web.xlsx`.)*

**E5. Two regions** — §6.4
One Person and one Firm per region, discrete distance between them. Start symmetric with ρ and distance cost both low. Raise ρ until efficiency beats variety and symmetry breaks; then raise distance cost until the efficiency gain is lost and symmetry returns; repeat, threading a line back and forth across the transition. The result is a mapped boundary in (ρ, distance cost) space.
*Point:* both parameters can trigger the same regime change from opposite directions.
Sub-result worth its own note: let People move and the winning region **locks in permanently** — a black hole for People. Adding a fixed agricultural-style demand in both regions (the Core Model's escape route) makes no difference; a single mobile Person is enough.
*(Ships as `SymmetryBreakParameterSweep` / `...Fast`.)*

**E6. Two regions, four Firms** — §6.4
Two Firms per region, one Person each, EoS curve and magnitude both 0.5. Vary ρ and distance cost and get four distinct regimes:
| ρ | dist. cost | regime |
|---|---|---|
| 0.5 | 0.1 | production spread evenly everywhere |
| 0.77 | 0.1 | one single efficient producer model-wide |
| 0.77 | 0.35 | regions diverge — one internally symmetric, one split |
| 0.85 | 0.55 | one efficient producer *in each* region |

*Point:* regions develop **internal** structure. The Core Model cannot do this; it has no inside to a region.
*(Ships as `TwoRegionFourFirmsTransmissionBelt`, both parameters on mouse buttons — an obvious 2-slider web piece.)*

**E7. Nineteen agents on a line** — §6.4.1
19 Firms and 19 People at matched, evenly spaced positions on a line of length 1 (odd number, so one pair sits dead centre). Firm efficiency drawn above the line, Person utility below with a mean marker.
- ρ=0.70, dist 4.75: symmetric. Central agents best off — more Firms within reach.
- ρ=0.70, dist 4.50: **the central Firm takes off** (note the y-axis rescale — the efficiency jump is large).
- ρ=0.73, dist 8.5: two efficient producers.
- ρ=0.79, dist 45.5: many efficient producers.

*Point:* distance cost selects the *number and spacing* of production centres. And a clean welfare finding: transitions to more efficient production **never make any individual Person worse off** — the aggregate gain is a positive externality for everyone in reach.
*(Ships as `ManyAgentsLineTransmissionBelt`, mouse-linked to ρ and distance cost. Highly interactive already.)*

**E8. Transmission belt + density cost** — §6.4.2. The closing synthesis.
Two Firms 1 apart in R², 300 People, distance cost 0.25, EoS curve 0.001 (kept low so 300 daily demand signals don't tip Firms over). Raise ρ from 0.5 to 0.9. A more-efficient producer emerges *endogenously* — there is no built-in difference between the Firms — and People physically migrate to it, forming a settlement, buying the other good in from a distance.

Presentation trick worth stealing: the correlation plot uses **arrows** for datapoints, angled along the vector from t−1 to t, so model time is visible in a scatter. Efficiency ratio climbs to ~13 as mean distance to Firm 0 falls.
*Point:* what C7 had to impose exogenously (which Firm is expensive) is now endogenous. Compare directly against C7 on the site — same visual, one assumption removed.
*(Ships as `TransmissionBeltPlusDensity`.)*

### Group F — Framework demonstrations. Small, cheap, high pedagogic value.

**F1. Zero-intelligence money exchange** — §4.7.2
100 agents, 100 units each, 200,000 iterations. Each picks another agent and hands over one unit if they have any. Result: a bounded random walk into severe inequality — a few very wealthy, most in the bottom band, a Gini not far off real countries.
*Point (and the footnote is the best line in the chapter):* this "grows" wealth inequality. Does it *explain* it? "There is not a large gap between claiming it does to making justifications for wealth inequality as a natural, unavoidable consequence of money flow." Also the practical reason the thesis abandons money entirely and uses time as currency.
*(Ships as `exchangetestrandom.m`, trivial to port.)*

**F2. The permutation gap** — §4.7.4
10,000 agents, 1,000 days, order randomly permuted each day. Histogram the gap between an agent's consecutive actions. You get a pyramid centred on exactly one day, not a spike.
*Point:* any agent using a t vs t−1 feedback is quietly working with the wrong denominator. The fix used: permute People and Firms in separate bins. A genuine, concrete ABM gotcha.

**F3. Blind vs rational choice** — §4.7.6
Core Model production with fixed labour requirement α = 3. Agents can each supply 1 unit.
- *Rational*: nobody works, because with no stock there's nothing to buy, and no single agent can get output above zero. Deadlock.
- *Blind*: pick a random employer; if you're better off next turn, do it again. Four agents will eventually stumble onto the same Firm and production starts.

*Point:* the more "rational" agent performs worse. The smooth EoS function works in both cases, which is why the thesis uses it. Beautiful as a two-button side-by-side.

**F4. Bundle sampling, stepped** — §4.6.1
Follow one Person over two iterations. Show all sampled Bundle points sized/shaded by utility, ring the best one, draw lines to where that Bundle sources goods with thickness ∝ spend, show everyone's density radii. Step forward and watch them move and resample.
*Point:* the core loop of the whole framework, made visible. Should probably be the site's first interactive.

### Group G — The comparator

**G1. Core Model: short run and long run** — §2.5.5
Iterate Y, I, W to convergence; compute real wages; plot.
- *Short run*: real wage ratio w₁/w₂ against λ₁ ∈ [0,1] at fixed T (2.25 gives five equilibria). Arrows show which way workers are pulled. Identify stable vs unstable crossings.
- *Long run ("tomahawk")*: sweep T from 1 to 3.5 (500 points × 500 λ values), plot stable equilibria black, unstable grey.

*Point:* the canonical result of geographical economics, and the benchmark the transmission belt is measured against. The existing jar already toggles between the two views with 'a' and steps T with the arrow keys.
*(Ships as `CoreModelViz_1.jar`; source in `thesisModelsDanOlner2013/CoreModel/src/coremodelviz/Main.java` — a single file, easily the simplest port.)*

---

## 6. Comparison table: Core Model vs transmission belt (§7.1)

Worth building as an interactive side-by-side on the site.

| | Core Model | Transmission belt |
|---|---|---|
| Timing | Two equilibrium stages; entry/exit instant, migration slow | One flow of days; granular price adjustment on every sale |
| Firms | Implicit; mirror of workers; no separate decisions | Explicit agents with stock, price and a production function |
| Production | `l = α + βx`; scale fixed by normalisation; EoS show up only as firm *number* | Smooth `ml + vl²`; scale genuinely varies and drives outcomes |
| EoS & LoV | Collapsed into one parameter (ε/ρ) | Kept separate, so their interaction is the main finding |
| Market structure | Dixit–Stiglitz monopolistic competition, zero profit by assumption | Profit possible but no stable competitive dynamic emerges |
| Space | Two regions, iceberg costs, no interior | Continuous or discrete; regions have internal structure |
| Prices | Single symmetric price via markup rule | A spread of prices; no market-clearing price required |
| Regime change | Symmetric production assumed; only λ moves | Catastrophic production shifts, even without space |

---

## 7. Proposed website structure

A spine of four passes at the same material, each one deeper — so a casual reader gets the argument and a determined one gets the mechanism.

1. **The question.** Space costs change; what happens to the map, and to how well off people are? F4 (Bundle sampling) as the opening interactive so the reader sees an agent thinking within thirty seconds.
2. **Building blocks.** A1–A4, B1–B3. Pure functions and single mechanisms, each with a pop-out derivation. Ends with the black hole: space deletes itself unless something stops it.
3. **The density cost thread.** C1 → C2 → C3 → C4/C5 (hand-computable) → C6 → C7 → C8. This is the most visually rewarding sequence and the one with the biggest surprise at the end.
4. **The coordination thread.** D1 → D2 → F3 → F1/F2. Why getting agents to coordinate is hard, told through failures.
5. **The transmission belt.** E1 → E3 → E4 → E2 → E5/E6 → E7 → E8. Each step adds one component. E4 is the "and here's *why*" moment.
6. **The comparison.** G1 plus the table in §6 above.
7. **The argument.** Dependencies, assumptions, descriptive vs functional mapping, the streetlight effect. This is where pop-outs earn their keep — the methodological argument is what makes the models mean something.

**Pop-out categories** worth defining as consistent components: *Equation* (the maths, derivable on demand) · *Why this choice?* (the dependency being threaded) · *What this assumes away* · *In the thesis* (section link + original figure) · *Under the hood* (the code).

---

## 8. Code inventory and porting notes

**`thesis_on_github/thesisModelsDanOlner2013/`** — runnable artefacts:
- `CoreModel/CoreModelViz_1.jar` (+ source, one file)
- `FrameworkModelBuilds/` — six scenario jars matching E5–E8, C8, D1 (see `listOfModels.txt`)
- `EoS_vs_LoV_ExcelModel_DanOlner2013web.xlsx` — model E4
- `exchangetestrandom.m` — model F1

**`thesis_on_github/FrameworkModelSourceCode/src/location2012/`** — ~190 Java classes, Processing-based. The parts that actually matter for a port are a small subset:
- `econs/` — `CESOneTypeUtility`, `SmoothEoS`, `CoreModelEoS`, `OptimiseHillClimb`, `Bundle`, `Good`, `Production`
- `Person`, `PersonAction`, `PersonActionTransmission`, `Firm`, `FirmAct*` (many variants — will need to identify which correspond to published results), `FirmActMarkup`
- `geog/SpaceTools`, `RadiusSearcher` — distance, wrapping, radius queries
- `utils/gl` (globals), `utils/Randoms` (seeded RNG — worth preserving for reproducibility)
- `observe/Timeline`, `actiontools/RandPermuteActorListener` — the day loop and the two-bin permutation
- `configs/*.properties` — **the parameter sets for published runs.** These are the most valuable files in the repo for us; they pin down the exact numbers behind each figure.

Most of the rest is Processing visualisation, CSV output "Buckets", and parameter-sweep plumbing — all of which we'd replace rather than port. Nothing here is computationally demanding: the heaviest thing is 500 agents × a few hundred Bundle evaluations per day, comfortably within a browser frame budget, and the hill-climber's 1000–2000 iterations is trivial.

Suggested port shape: a single small engine module (space, timeline, actors, utility, production, optimisers, density cost) with per-model scenario configs mirroring the `.properties` files, and a thin rendering layer per interactive. That keeps the site's models genuinely *the* models rather than reimplementations that drift.

---

## 9. Notes and open questions

A few things I'd want your steer on before we go further — none of them block starting.

1. **Reproduce or re-present?** Some figures are parameter sweeps that took long runs (e.g. C3's 0.005-increment sweeps to spatial equilibrium, D2's 100 seeds per ρ value, G1's 250,000 points). Options: run them live but slowly with a progress indicator; ship precomputed data and make only the *illustrative* runs live; or both, with a "run it yourself" button. My instinct is precomputed data for sweeps, live for anything under a few thousand agent-steps.

2. **Fidelity vs clarity.** Where the original visualisation is idiosyncratic — the shaded-square relative scaling, the "barcode" of Firm positions, the arrow-datapoint correlation plots — do we reproduce them faithfully (they're distinctive and they're what's in the thesis) or modernise?

3. **The FirmAct variants.** There are ~10 `FirmAct*` classes. I'll need to work out which one produced which published result; the `.properties` files should tell us, but if you remember any of it that'd save time.

4. **Framing.** How much of the *methodological* argument goes on the site? The models are attractive on their own, but the thesis' actual contribution is the dependencies/assumptions argument — the models are evidence for it. I've assumed we want both, with the argument as the spine and the models as the payload. Say if you'd rather it were primarily a model gallery.

5. **The honest failures.** E2 (no competitive equilibrium) and D2 (Firms are poor optimisers) are, to my reading, among the most interesting things in the thesis — negative results with clear diagnoses. I've catalogued them as first-class models rather than footnotes. Flag if you'd rather they were downplayed.

6. **Scope of "the PhD".** The digest covers Chs. 1–7 and the appendix. Chapter 3 (ABM and economic theory — Fowler, ACE, generation vs explanation, virtual worlds vs engines of analysis) is almost entirely argument with no models; I've folded its concepts into §1 and §7 rather than giving it its own section. Let me know if you want it treated more fully.
