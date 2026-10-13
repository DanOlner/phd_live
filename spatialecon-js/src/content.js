// @ts-check

/**
 * What each model page says, beyond the model itself. BUILD_PLANNER §7.2.
 *
 * Three kinds of pop-out, deliberately kept apart because they answer different questions and
 * a reader wants them at different moments:
 *
 * - **`economics`** — the argument. Why this model exists and what it is for. From
 *   THESIS_DIGEST.
 * - **`hood`** — how it is actually built, including where the original code is candid about
 *   its own compromises. From CODE_ANATOMY. The 2013 source is liberally self-annotated
 *   (`//Hackeroo strikes again!`) and several of those comments sit directly on top of things
 *   the thesis argues about, which makes them better documentation than a tidy paraphrase.
 * - **`found`** — what rebuilding it turned up, *including where that complicates the thesis*.
 *   Decision 4: the disagreements go in. A model with nothing to report here has no `found`
 *   entry rather than a reassuring one.
 *
 * And `steps`: an ordered walkthrough. Each step is a **predicate on model state**, never a
 * day number (BUILD_PLANNER §3.2) — "when the efficiency ratio first exceeds 1.15" survives a
 * reroll and "on day 412" does not. That constraint is the whole reason the site can offer a
 * reroll button at all.
 *
 * The exception, and it has to be declared: a caption whose subject *is* elapsed time — "two
 * thousand days in and the Firms are still moving" — sets `elapsed: true`. That claim is true
 * under every seed, because the Firms never settle under any of them; it is not a day number
 * standing in for a state. Anything reading `world.day` without the flag is a test failure,
 * which is how the distinction stays a decision rather than an oversight.
 *
 * Narrative lives here rather than on the `Scenario` so that the scenarios stay about the
 * models: a scenario is the thing that would be verified against the Java, and prose about
 * what a visitor should notice is not part of that.
 *
 * Coverage is honest rather than complete. Variants inherit from their family (`d1b` reads
 * `d1`, `e6` reads `e5`), and a model with no entry simply shows its own `notes`.
 */

/**
 * @typedef {object} Entry
 * @property {string} [economics]
 * @property {string} [hood]
 * @property {string} [found]
 * @property {{when: (world: import('./world.js').World) => boolean, caption: string, elapsed?: boolean}[]} [steps]
 */

/** Mean over People. @param {import('./world.js').World} w @param {(p: any) => number} f */
const peopleMean = (w, f) => w.people.reduce((a, p) => a + f(p), 0) / Math.max(w.people.length, 1);

/**
 * Has anyone acted yet?
 *
 * Utility is zero until a Person takes their first turn, which makes this the state-based way
 * to say "the run has started" — needed because `meanPersonMovement()` is also zero before
 * anyone has moved, so a settling test would otherwise fire on the very first frame. The
 * obvious guard is `world.day > 100`, and it works, but it puts a day number back into a
 * predicate that has no business knowing one.
 * @param {import('./world.js').World} w
 */
const hasRun = (w) => w.people.some((p) => p.utility > 0);

/** Highest over lowest, always ≥ 1. @param {number[]} vs */
const ratio = (vs) => {
  const lo = Math.min(...vs);
  return lo <= 0 ? 1 : Math.max(...vs) / lo;
};

/** @type {Record<string, Entry>} */
export const CONTENT = {

  // --- Group A: the economics on its own ------------------------------------------------

  a1: {
    economics: `CES utility is the engine of the whole thesis, and rho is the single number
that carries it. At rho close to 1 the goods are near-perfect substitutes: having three of
them is worth no more than having three units of any one. As rho falls, variety itself
becomes valuable, and the same budget spread over the same goods buys steeply more utility.

That is "love of variety", and it is an increasing return — but in *consumption*, not in
production. Keeping those two apart is the thesis' central methodological move, because
Krugman's Core Model collapses them into one parameter and can then never ask how they
interact.`,
    hood: `The quantities are found by the constrained optimiser, which solves the
Dixit-Stiglitz first-order conditions directly. With equal prices they come out even for
every rho, which is why the quantity line is flat.

The direction of rho is the single easiest thing to state backwards, in conversation and in
code. High rho means LOW love of variety.`,
    found: `The closed form, drawn dashed over the computed curve, is Y·n^(1/rho - 1). At
rho = 0.01 with three goods that is 3^99, about 1.7e47 — which is why this chart has a
logarithmic vertical axis where the original had a linear one. It is not a large number
because something has gone wrong; love of variety with almost no substitutability really is
unbounded, and a linear axis simply hides ninety-eight days of the sweep.`,
  },

  a2: {
    economics: `Cobb-Douglas is the obvious alternative to CES, and this model is here to
show why the thesis does not use it. Firm 1 sweeps its price across a factor of fifteen.
Demand for Firm 0's good does not move at all — not approximately, exactly — because Firm
1's price does not appear in it. And Firm 1's own revenue does not move either: the demand
is unit elastic, so every price cut is met by precisely enough extra demand to leave takings
unchanged.

A Firm facing this demand has no lever. It cannot improve its position by pricing, and since
space enters this framework only through price, it has no reason to move either. The choice
of utility function has quietly disabled a whole class of agent behaviour before any agent
has done anything.`,
    hood: `The thesis draws a further point out of this that is worth keeping. An agent model
would eventually *reveal* the independence — run it, watch the line stay flat — but could
never explain it. The explanation is one line of algebra: p_M does not appear in demand for
F. Simulation is not always the tool with the most explanatory power, and knowing which
questions it is the wrong tool for is part of using it well.`,
  },

  a3: {
    economics: `Two ways of writing increasing returns to scale, and the difference decides
what kind of model you can build.

The smooth form x = vl² + ml produces something for any positive input, and both average and
marginal output rise with scale. The Core Model form x = (l - alpha)/beta has a constant
marginal by construction and — the point of the exercise — returns *negative* output for any
input below alpha.`,
    hood: `The negative region is not clamped here. It is the whole thing worth seeing.

Perfectly fine as algebra, where l is an aggregate. Fatal in an agent model, where a Person
supplies one unit of labour and alpha is 3: no individual can ever reach positive output
alone, so a Person who reasons correctly about it never works at all. Production never
starts. That is model F3, and it is why the thesis uses the smooth function throughout.

Both marginals are computed numerically rather than symbolically, so the same code applies to
any production function added later.`,
  },

  a4: {
    economics: `The hill-climber is not an implementation detail, it is a claim about what an
agent can plausibly do. A Person is not assumed to solve a constrained optimisation. They try
a basket, try a nearby basket, keep whichever is better, and get tired. The rest of the
framework is built to work with an agent that reasons like that.

With n equally priced goods and a budget of 1 the analytic optimum is 1/n of each, so every
run can be scored against a known answer.`,
    hood: `The second panel draws the search in the space it searches. Every candidate is
normalised back onto the budget line before being scored, so the constraint is not enforced
by rejecting candidates — it is built into how a candidate is generated. That is why the
climb never leaves the diagonal.

The original's iteration count is hardcoded at 1000, with the jiggle annealing from 0.01 to
0.001 across the run.`,
    found: `Two things the numbers show and the prose tends not to.

Most of the gain arrives in the first few dozen iterations and the remaining nine hundred buy
very little — visible as the accepted steps bunching at the start of the path.

And the spread never reaches zero. That residue is not a defect to be tuned away: it is
exactly where the demand noise of §5.5.2 comes from, and that noise is what makes Firms
misread their own revenue signals in D2. The imprecision of the agent and the difficulty of
the DMR game are the same fact.`,
  },

  // --- Group B: space meets love of variety ---------------------------------------------

  b1: {
    economics: `The first model with distance in it. One Person, several Firms at different
distances, and a delivered price p + cd that rises with how far away the seller is.

Sweeping rho shows the interaction the thesis is after: how much love of variety is worth
depends on what the variety costs to reach. Strong love of variety will pay a real distance
premium to keep buying from everyone; weak love of variety will not.`,
  },

  b2: {
    economics: `Distance cost rising, with the Person fixed. As it rises the distant Firms'
delivered prices climb and demand concentrates on the near ones — but never to zero, because
CES with rho below 1 always buys some of everything.

That last property matters far more than it looks. It is what keeps the transmission belt of
chapter 6 away from Bertrand competition, where the cheapest seller would take the entire
market on a marginal price difference.`,
  },

  b3: {
    economics: `Both People and Firms mobile, with nothing to hold them apart. They collapse
onto a point, which is the null result the density cost exists to fix: with increasing
returns and no cost to crowding, everyone should be in one place.

The interesting part is the asymmetry in the collapse.`,
    found: `People collapse markedly faster than Firms — about seven times faster in the
Java. Not because they want it more, but because they are better searchers: a Person
evaluates twenty-one candidate locations a turn and takes the best, while a Firm gropes one
step at a time and only finds out afterwards whether it helped. The same spatial pressure
acting on two different decision procedures produces two different speeds.`,
  },

  // --- Group C: the density cost --------------------------------------------------------

  c1: {
    economics: `Five hundred People, one Firm at the centre, and a cost to being crowded.
People arrange themselves until nobody can improve by moving — which means utility ends up
equal everywhere, with density traded off exactly against distance. A monocentric city, with
no landlords, no rent and no market for space. The crowding cost is paid by the crowd itself.

This is the thesis' answer to von Thünen without the machinery von Thünen needed.`,
    hood: `Density is genuinely local: the neighbour set comes from a real radius query. The
goods search is not — every config sets peopleConsiderAll = true, so every Person sees every
Firm at any distance and the radius arguments in the Person action are dead code.

The density cost is subtracted from the *wage*, before anything is bought, and a Bundle whose
density cost exceeds the wage is discarded rather than scored negative. That one rule turns
out to explain a lot; see below, and see C8.

Turn on "buy lines" to see the trades themselves rather than their consequences — the only
view here that draws transactions rather than outcomes. It was written in 2013 and never
published.`,
    found: `Utility is *not* perfectly flat across distance, and working out why was the most
useful thing the rebuild did.

With one Firm there is a closed form: U = (1 - D(d)) / (1 + 0.25d). For dU/dd = 0 the density
profile has to be exactly linear in distance and reach zero at a cutoff d*. The model
produces that line — the correlation between distance and density is -0.985 here and -0.986
in the Java. But density cannot go below zero, so past d* there is nothing left to trade
away. Those People pay pure distance cost and their utility falls.

They are not in disequilibrium. No move improves them; they are outside the range over which
equalisation is *possible*. It is a finite-population boundary effect, and it thins with
numbers exactly as that implies — 44%, 20%, 7.6%, 2.0% and 0.45% of the population at 125,
250, 500, 1000 and 2000 People. The swept figure below is that measurement.

Switch on "equalising cutoff" to see d* drawn as a ring with the fringe People marked. Then
turn the density cost up and watch the ring close: C1 and C8 are one continuum, not two
regimes, and the fringe is already present at density 10 — just small enough to overlook.`,
    steps: [
      {
        when: () => true,
        caption: `Five hundred People, scattered at random, and one Firm at the centre. Each
faint circle is one Person's density radius, so where the page goes dark is where the
crowding cost is high — the measure and the picture of it are the same object.`,
      },
      {
        when: (w) => peopleMean(w, (p) => p.densityCost) > 0.02,
        caption: `People are moving inward. Proximity to the Firm is worth having, because a
delivered good costs p + cd and being close makes everything cheaper.`,
      },
      {
        when: (w) => hasRun(w) && w.meanPersonMovement() < 5e-3,
        caption: `The settlement has formed. Nobody is coordinating this: each Person only
ever evaluates a handful of spots and takes the best, and every move changes the choice set
of whoever decides next. The externality is the entire mechanism.`,
      },
      {
        when: (w) => hasRun(w) && w.meanPersonMovement() < 1e-4,
        caption: `Spatial equilibrium — mean movement below 1e-4. Mean utility settles near
0.9, and it is very nearly the same for everyone. Switch on "equalising cutoff" to see where
"very nearly" stops being good enough, and why.`,
      },
    ],
  },

  c2: {
    economics: `C1 with the Firm free to move as well. It has nothing to gain — the People
are already arranged around it — but watching it not move is the point: the equilibrium is
stable against the Firm's own search, not merely a consequence of pinning it down.`,
  },

  c6: {
    economics: `Four Firms with different wages, and each Person permanently tied to one of
them. They may neither work nor buy anywhere else. What emerges is von Thünen's rings — the
Firms offering the highest wage draw their People closest in, because those People can afford
to outbid the others for central space.

There is no land market here and no landlord. The bidding is done entirely through the
density cost, which each Person pays to the crowd rather than to an owner.`,
    hood: `Each group gets its own shape *and* its own grey — square, circle, triangle,
pentagon at 220, 190, 160, 130 — matching figures 5.15 and 5.16.

Those greys are not the ones in the source tree. `+"`gl.nextActorColour()`"+` as left builds its
list with a loop that yields two entries, black and mid-grey, and then hands every Firm past
the second a *random RGB colour*. With four Firms the published figures would have had two
greys and two arbitrary colours, which they plainly do not. Directly above that line, commented
out, sits the four-entry ramp used here. The drawing code carries the same disease as the
scenario wiring: the snapshot is not the state any figure was made in.`,
  },

  c6c: {
    economics: `The same picture keyed to a *spatial* cost — commuting — instead of a
non-spatial one, and the sorting reverses.

A high wage buys proximity, because it is worth the same wherever you stand and can be spent
outbidding others for central space. A low commute cost buys *distance*, because its whole
value is in making distance cheap, and spending it on being central would waste it.

That distinction — between costs that vary with location and costs that do not — is the one
the thesis argues matters, and it matters more than the distinction between goods and wages.`,
  },

  c8: {
    economics: `C1 with the density cost raised fifty-fold. The settlement disperses; at high
enough cost the middle empties out entirely and People spread to the edges.

Not because they are pushed. Because nowhere crowded is affordable: a Bundle whose density
cost exceeds the wage has no budget left to buy anything with and is discarded, so the
central locations simply stop being options.`,
    found: `The swept figure below puts C1 and C8 on one axis, and there is no kink in it.
They are the same model at two points on a continuum.

Note what does *not* move: mean utility is 0.898 at a density cost of 10 and 0.882 at 500,
across a fiftyfold rise. The People absorb all of it by moving apart — mean distance from the
Firm doubles instead. Nobody ends up worse off for crowding because nobody stays crowded,
which is what makes this an equilibrium rather than a burden.`,
  },

  // --- Group D: Firms groping for a location --------------------------------------------

  d1: {
    economics: `Two Firms that may change only their position, and have no idea what they are
doing. Move, wait a turn, ask whether revenue rose; if it did keep going, if it did not go
back and try a fresh direction. That is the entire decision procedure.

It is "data, meaning, response", the thesis' borrowing from Jane Jacobs: the datum is a
revenue change, the assumed meaning is "my last action caused this", and the response
follows. The four variants cover both values of rho and both distance costs, and the outcomes
differ in kind rather than degree.`,
    hood: `Only the location branch of this behaviour was ever reachable. The original's
`+"`FirmActRespondAverage`"+` contains fully written branches for perturbing wage and price too,
sitting behind a hardcoded choose = 2; the thesis notes those "faced problems". Nine other
FirmAct classes exist in the source and none was ever wired in — one is an empty shell.

The step size matters more than it looks. A new search draws a *signed magnitude* uniformly
from ±0.0125, so most steps are well under the maximum and some are nearly zero. It is not a
unit direction times a fixed rate; getting that wrong makes Firms far better searchers than
they are, converges the games in a fraction of the days, and quietly flatters the result.`,
    found: `The Firms never settle, and that is the finding rather than noise in the
measurement. The crawl always perturbs, so a Firm that has found a good spot keeps testing
and keeps giving ground.

Which means reading a separation off the final day tells you almost nothing. The swept figure
below averages over the second half of each run and over several seeds, and plots the
*wandering* alongside the separation. The wandering is worst exactly where the incentive is
weakest: with near-substitute goods and cheap distance, a Firm's position barely affects its
revenue, so the crawl has almost no signal to follow and the Firms roam the whole line.`,
    steps: [
      {
        when: () => true,
        caption: `Fifty People fixed and evenly spread. Demand for Firm 0 is drawn above the
rule, Firm 1 mirrored below, one bar per Person at their position. The white pennants are the
Firms; the box above each is where it has been recently, mean and spread.`,
      },
      {
        when: (w) => Math.abs(w.firm(0).position.x - w.firm(1).position.x) > 0.15,
        caption: `They have separated. Neither Firm knows the other exists — the only thing
either has ever observed is its own revenue going up or down.`,
      },
      {
        elapsed: true,
        when: (w) => w.day > 2000,
        caption: `Two thousand days in, and still moving. Compare the spread box with the
pennant: the average is much tidier than the behaviour underneath it, which is why the
original drew both.`,
      },
    ],
  },

  d2: {
    economics: `D1 with demand noise, which makes the Firms' game genuinely hard. A Firm
observes its revenue changed and infers its own move caused it. Under noise that inference is
often simply wrong, and the Firm confidently walks the wrong way.

This is the sharpest form of the DMR argument. The problem is not that the Firms are stupid;
it is that the signal they are reading does not carry the information they are reading out of
it.`,
    hood: `Noise is applied to the quantities *bought*, not to the utility scored. A Person's
utility is computed on the clean optimum, and the noisy quantity is what the Firm then sees
as demand.

That asymmetry is deliberate and is the model: the noise represents imprecision in carrying
out a purchase, not in knowing what you wanted. So People are not confused — only the Firms
watching them are.`,
    found: `This is the one place the rebuild disagrees with the thesis and cannot yet
explain it away.

The thesis reports that *low* rho makes the DMR game harder under noise. Both rebuilds — the
Java and this one, independently — show the opposite, with a coherent mechanism for their own
result. Noise amplitude, distance cost and the character of the noise have all been ruled out
as explanations. It is written up in CODE_ANATOMY §7.12 and it is open.

It is here rather than in a footnote because a rebuild that quietly reproduced everything
would be less informative than one that does not.`,
  },

  // --- Group E: the transmission belt ---------------------------------------------------

  e1: {
    economics: `The transmission belt begins by throwing money away. People buy goods
directly with time: a day is the budget, and the goods it buys are the wage. No money, no
separate labour market.

The thesis arrives here after finding that treating money as a real conserved quantity
produces runaway inequality dynamics that swamp everything else (model F1), and after
establishing that the distinction that matters is between spatial and non-spatial costs
rather than between goods and wages.

A Firm receiving T units of time and producing x = l² + l clears its stock at a price of
1/(T+1). That is deducible by hand, which makes this model a check against arithmetic rather
than against a correlation.`,
    hood: `The price mechanism is a *reflex*, the thesis' third decision type (§4.5.2): it
fires on the sale itself rather than being scheduled on the timeline. So with 300 People a
Firm adjusts its price 300 times a day, not once.

The damping is the interesting part. Targeting stock alone does not work, and the way it
fails is instructive — when stock reaches zero the price is at its furthest from equilibrium,
so the system sails through and comes back the other way, giving a perfectly stable
oscillation that never settles. A textbook undamped harmonic oscillator, sitting in the
middle of an economic model. Targeting the first and second differences as well damps it.

The original names those differences "...BetweenDays", which is a misnomer: they are
per-transaction. Run the undamped variant to see what the damping is for.`,
    found: `The clearing price arrives at 0.50000 with stock at 4e-15, and at 0.16667 for 20
Firms and 100 People. Exact, against a value deduced by hand.

One correction to the record: the undamped variant does not settle at all. It was written up
as settling at 0.299; re-running it shows the price swinging between 0.16 and 1.12 over the
last hundred days. Both READMEs have been corrected.`,
  },

  e3: {
    economics: `Two Firms, one Person, and **no space at all**. Rho is raised slowly. Below a
threshold the Firms produce symmetrically; above it symmetry breaks and one becomes the
efficient producer, charging a much lower price and taking most of the day.

The claim this establishes is the chapter's sharpest: you do not need space for a
catastrophic shift in production structure. Dixit-Stiglitz cannot show it, because it assumes
all varieties are produced symmetrically at the same scale. Here the scale is real and free
to move, so it moves.`,
    hood: `Where the instability comes from is worth being precise about, because "emergence"
explains nothing. Both Firms decide simultaneously and never observe each other. The coupling
is entirely inside the Person's CES demand: every price appears in every good's demand, with
rho setting how strongly. Below the threshold a price perturbation is self-correcting; above
it the demand shift outruns the price correction, the favoured Firm gets more input, produces
more efficiently, can charge less, and attracts more still.

A slight initial asymmetry is required. From exactly equal prices there is nothing for the
feedback to amplify and the model stays symmetric forever.`,
    found: `The symmetric equilibrium price is 0.6667, deducible by hand and hit exactly.

The break: the thesis puts it near 0.75, the Java's ramped run at 0.764, and this ramped run
at 0.766. The swept figure below holds rho still instead of ramping it, and finds the
efficiency ratio is 1.000 to three decimals up to 0.73 and has departed by 0.75. Both ramps
lag, which is what a ramp through a threshold does — so the ramp tells you the model shifts
and the sweep tells you where.

Read the utility line carefully. It declines throughout as a background trend, because raising
rho reduces love of variety and that is worth less regardless. What matters is the local turn
at the transition: it stops falling and starts rising. The Person is not a victim of the
shift, they cause it.`,
    steps: [
      {
        when: () => true,
        caption: `Two Firms, one Person, no space. Both prices sit at the symmetric
equilibrium of 2/3 — the dashed line — and the efficiency ratio is exactly 1.`,
      },
      {
        when: (w) => ratio(w.firms.map((f) => f.outputPerUnitTime)) > 1.05,
        caption: `Symmetry is breaking. Nothing was added: rho crossed a threshold and a tiny
price difference that used to correct itself now amplifies instead.`,
      },
      {
        when: (w) => ratio(w.firms.map((f) => f.outputPerUnitTime)) > 1.8,
        caption: `One Firm has taken over. Its price is heading for 0.5 — the one-Firm
clearing price — while the loser's climbs away. No space was involved anywhere in this.`,
      },
    ],
  },

  e5: {
    economics: `Two discrete regions, and both rho and distance cost able to trigger the same
regime change from opposite directions. Raising rho makes goods substitutable enough that
efficiency beats variety and one producer takes over; raising distance cost undoes it,
because reaching the distant efficient Firm costs more than the efficiency is worth.

The run threads back and forth across the boundary by alternately raising each, so each
crossing is a point on the line dividing the two regimes.

The four-Firm variant is the one the Core Model cannot match: production structure varies
*within* a region as well as between them. A model whose regions are dimensionless points has
no inside for structure to live in.`,
    hood: `The crossing is detected on the ratio of *prices*, which is what the shipped jars
thread on — recovered by decompiling them, since the source tree does not say.

That detail cost real time. Price is the more responsive signal: at symmetry both sit at the
same value, and when symmetry breaks the winner's price falls while the loser's climbs, so
the ratio moves from both ends at once. An earlier build threaded on efficiency instead and
crossed the boundary once in twenty thousand days rather than repeatedly.`,
  },

  e7: {
    economics: `Nineteen Firms and nineteen People on a line, with the central Firm gradually
advantaged. It takes off, production concentrates — and nobody is made worse off.

That is what "transmission belt" names: efficiency gains in production reaching consumers as
welfare, through nothing but prices and each agent's own choices.`,
    found: `The efficiency ratio rises from 3.58 to 7.56 while the *minimum* utility across
all People rises monotonically from 0.371 to 0.523. Not the mean — the worst-off. Nobody is
traded off against the concentration.`,
  },

  e8: {
    economics: `The closing result. Two Firms a distance of 1 apart, three hundred mobile
People, rho raised from 0.5. A more efficient producer emerges, People migrate to it, and a
settlement forms around it — while they go on buying the other good in from a distance.

What makes it the closing result is what it removes. In C7 the same two-core shift happens,
but only because the two Firms were *given* different distance costs by the modeller. Here
the Firms are identical in every respect. Which one wins is decided by the model, from an
arbitrarily small difference in starting price amplified by the feedback between demand and
production scale. An assumption has been replaced by a mechanism.`,
    hood: `The second panel plots the production ratio against mean distance to Firm 0, with
consecutive samples joined in time order. That is deliberate: a scatter would show the two
are correlated, which is a much weaker claim than a track that sits still for two thousand
days and then turns a corner. Correlation is what a scatter shows; direction of travel is
what this model is about.

The width is 2, as the shipped config says. The Java build used width 1, which puts two Firms
1 apart exactly on the space corners and clips the settlement that forms around the winner.`,
    found: `This model appeared not to reproduce, and the failure was in the measurement.

The published "Firm efficiency ratio" reads the Firms' *productionLevel* — total output
f(T) = vT² + mT — which was recovered by decompiling the shipped jar. The obvious reading is
output per unit, which the same build also records; but with the shipped EoS curve of 0.001
and magnitude 1 that is 0.001·T + 1, and with 300 People T cannot exceed 300, so **the
per-unit ratio cannot exceed 1.3 whatever the model does**. A rebuild measuring it reached
1.26 — 97 per cent of an arithmetic ceiling — and recorded a failure to reproduce the thesis'
value of about 13.

Measuring total output instead gives 13.55, at rho = 0.9 as the thesis states, rather than
the 0.97 the capped statistic seemed to require. Both ratios are recorded here, because the
difference between them is the whole reason this looked weaker than it is.

The general lesson is worth keeping: a figure label is not a variable name. Only the jars say
which field a store actually read.`,
    steps: [
      {
        when: () => true,
        caption: `Two identical Firms and three hundred People scattered at random. Nothing
distinguishes the Firms but an arbitrarily small difference in starting price.`,
      },
      {
        when: (w) => /** @type {any} */ (w.config.utility).rho > 0.7,
        caption: `Rho is rising and almost nothing is happening. The production ratio is still
about 1.0 — the two Firms are producing at the same scale.`,
      },
      {
        when: (w) => ratio(w.firms.map((f) => f.productionLevel)) > 3,
        caption: `The shift has started, and it is fast. Watch the second panel turn its
corner: the production ratio and the mean distance to Firm 0 are moving together, because
People migrating and production concentrating are one event, not two.`,
      },
      {
        when: (w) => ratio(w.firms.map((f) => f.productionLevel)) > 20,
        caption: `A settlement has formed around the winner, and the People in it still buy
the other good in from a distance. Press reroll: sometimes the other Firm wins. The shape is
what reproduces, not the winner.`,
      },
    ],
  },


  c3: {
    economics: `Four sweeps of one settlement, one per cost, each against how far from the
centre People end up. The result is a set of SIGNS: wage agglomerating, base good cost
dispersive, commute cost agglomerating, goods distance cost agglomerating.

The mechanism is in C1's closed form, U = (w - D(d)) / (p + cd). A non-spatial cost enters as
a constant -- the wage on top, the base price in the denominator -- and changes what the
DISTANCE term is worth relative to the crowding term. Raise the wage and crowding is cheap in
proportion, so People accept more of it and pack in. Raise the base price and the cd term
matters proportionally less, so distance stops being worth paying to avoid. A spatial cost is
multiplied by distance and simply makes distance dearer, which pulls inward every time.

This is the model that licenses the whole of chapter 6. If a wage and a goods price reach
location through the same channel, differing only in whether distance multiplies them, there
is no need to model a labour market and a goods market as separate things -- which is exactly
the collapse the transmission belt performs when it throws money away.`,
    hood: `C3's configuration is one of the two that are LOST. The shape of the figure is from
the thesis text; the population, the space and the density cost are C1's, on the grounds that
C3 is C1 with a cost being moved.

The sweep runs up and back down, as the original's did, so hysteresis shows as the two passes
failing to lie on top of each other.`,
    found: `All four directions reproduce. Measured over the rising pass:

  wage 0.05 -> 1.5      mean distance 0.550 -> 0.238   agglomerating
  good cost 0.05 -> 2   mean distance 0.268 -> 0.313   dispersive
  commute 0 -> 1        mean distance 0.307 -> 0.156   agglomerating
  delivery 0 -> 1       mean distance 0.733 -> 0.181   agglomerating

The interesting one is the good cost, because it is the only reversal and the easiest to get
backwards. Goods getting dearer does not push People outward by making them poorer -- it does
it by making the part of the price that varies with distance a smaller share of the total.`,
  },

  c4: {
    economics: `The smallest model in the thesis and the only one that can be done on paper.
Two regions, two People, one Firm. Three closed forms and where they cross.

Look at which curve the mover lands on. Moving out is worth doing when 1/(g+d) beats
(1-D)/g -- and whenever it is, the person who moves ends up BELOW the person who stayed. The
mover pays the distance cost; the stayer keeps the central spot and is relieved of the
crowding. The one who acts confers the larger benefit on the one who does not.`,
    hood: `No agents are simulated. The curves are the algebra, and that is deliberate: this
is the case where a closed form explains and a simulation would only reveal. Compare a2,
where the thesis makes the same point about Cobb-Douglas demand.`,
    found: `The externality is the whole mechanism, and here it is visible in three lines.
Nobody is being altruistic and nobody is coordinating; each is only asking whether they are
better off elsewhere.

That generalises further than it looks. In an agent model, market interaction is nothing BUT
externalities -- every agent's choice set is made of what the others have already done. C1's
five hundred People arranging themselves into a city is this same fact, run five hundred
times over and no longer legible as anything so simple.`,
  },

  c5: {
    economics: `C4 with one more of everything, which is enough to turn a single crossing into
a sequence. Two crossings, at different distance costs, so the settlement dissolves in a
definite ORDER -- one Person leaves, then a second -- and each crossing is a specific decision
by a specific Person.`,
    found: `The C4 asymmetry compounds. The first to leave pays d and relieves the two who
stay of a unit of crowding each; the second pays 2d and relieves the last one entirely. Each
mover ends up worse off than everyone they left behind.

And the Person who never moves finishes on 1/g -- the highest utility available anywhere in
the model -- reached by doing nothing at all.`,
  },

  c7: {
    economics: `Two Firms 1 apart, identical except that one costs 2 per unit distance to
reach and the other 4. Five hundred People. rho is swept down and back up.

No cost changes at any point in the run. The only thing that moves is taste, and it relocates
the entire population from one Firm to the other.

The direction is backwards from the obvious guess. With STRONG love of variety a Person must
have some of both goods whatever they cost, so the sensible place to stand is next to the good
that is expensive to ship, importing the cheap one from a distance -- People cluster around
the DEARER Firm. With weak love of variety the goods are near-substitutes, nobody needs both,
and everyone crowds the cheap one.`,
    hood: `The two distance costs come from a single base of 2 with the ranging flag on, which
multiplies each Firm's cost by its index. That flag exists for this model.

C7's configuration is the second of the two that are LOST. The Firms, their separation, the
two distance costs and the rho sweep are all in the thesis text; the density cost and radius
are C1's.`,
    found: `The core swap reproduces cleanly. Measured as mean distance to the cheap Firm minus
mean distance to the dear one -- negative means the population sits nearer the cheap Firm:

  rho 0.99   -0.37     nearer the cheap Firm
  rho 0.52    -0.10     crossing
  rho 0.05   +0.49     nearer the dear Firm

and the return sweep crosses at about the same place, with modest hysteresis.

What e8 removes: this model is HANDED its asymmetry by the modeller, who decided one Firm
would be dearer to reach. E8 produces the same two-core outcome from Firms identical in every
respect, deciding which wins from an arbitrarily small difference in starting price. That is
the difference between an assumption and a mechanism, and it is why e8 closes the thesis and
this does not.`,
  },

  e2: {
    economics: `The honest failure, and the thesis says so. Every Firm up to here clears its
stock and takes no profit. Give one a markup -- have it target a stock level that rises on
every sale, and skim the increase -- and nothing stable happens.

Two endings, neither a competitive equilibrium. Implicit collusion, where both Firms raise
price together and squeeze People until they can barely buy anything. Or runaway, where one
Firm raises price past the point where it can attract the demand it needs, and compounds.

Nothing stops either, because a Person here has no exogenous alternative -- no savings, no
outside option, no way to withhold their day -- and therefore no sanction to apply.`,
    hood: `The markup is per transaction and it RATCHETS: the Firm's stock target rises by
that much on every sale. So a constant markup is a linearly rising target, and a Firm taking
profit here is not charging a margin, it is chasing a receding goal.

Which ending you get depends on how the markup arrives. Left alone the run ramps it slowly and
you get the squeeze; drag the slider straight to 0.15 -- which stops the ramp -- and you get
the runaway.`,
    found: `The finding that is not about failure: watch the OTHER Firm. Its price falls. It
picks up the demand Firm 0 has priced itself out of, and more input means more efficient
production under increasing returns, so it clears at a lower price.

That coupling is the economies of scale, and it can be switched off. At a fixed markup of 0.02
after 3000 days, the amount Firm 1's price is dragged from its no-markup value:

  EoS curve 0     -0.0003   (nothing)
  EoS curve 0.25  -0.0015
  EoS curve 0.5   -0.0030
  EoS curve 1     -0.0046

Zero effect at zero curve, growing with EoS, as the thesis says. So one Firm's markup does not
merely redistribute -- with increasing returns it changes the ABSOLUTE price level for
everyone, because it changes where production happens and therefore at what scale.`,
  },

  e4: {
    economics: `The "why is this happening?" model, and the thesis' answer to its own
methodological complaint. E3 shows a catastrophic shift in production structure. Showing it is
the START of an explanation, not the end -- and "emergence" is not a mechanism.

So this model takes the agents away. What is left is four pairs of numbers and one line of
arithmetic: prices, the demanded amounts from the CES optimum, the produced amounts, and
p' = p + k(c - f). No People, no Firms, no turns, no sampling, no noise, no space.

It does the same thing at the same threshold.`,
    hood: `Both Firms are updated SIMULTANEOUSLY from the same state. That is the point of the
construction: if they were updated in sequence the second would see the first's new price, and
any instability could be blamed on the ordering -- which is exactly the objection this model
exists to rule out.

The symmetric fixed point is deducible by hand: each Firm gets half a budget of one, so t =
0.5 and f = 0.75, and demand at price 2/3 is 0.75. Supply equals demand and nothing moves.`,
    found: `Measured over 4000 steps from a starting perturbation of 0.002:

  rho 0.73   asymmetry decays to 5.5e-7; both prices return to 0.6667, both produce 0.750
  rho 0.80   asymmetry grows to 0.38 and holds; prices 0.527 and 0.907, production 1.703
             and 0.113 -- one efficient producer, fifteen times the other

So it diverges to a NEW PAIR OF EQUILIBRIA rather than running away, and the threshold sits
between 0.73 and 0.76 as the thesis reports.

That locates the externality exactly. The only channel between the two Firms is that both
prices appear in the same constrained optimum -- which is what CES demand IS -- and rho sets
how strongly one price moves demand for the other good. The instability lives inside the
Person's utility function.

Convergence here takes a few thousand steps against the thesis' sixty, because the constant
the law of motion is applied by is a free choice and this one is smaller. It changes the speed
and not the threshold.`,
  },

  f1: {
    economics: `A hundred agents, a hundred units each, and one rule: pick an agent, and if
they have anything, give one unit to someone else. No preferences, no strategy, no production,
no prices. After two hundred thousand exchanges the distribution is severely unequal.

It is a bounded random walk, and this is what bounded random walks look like.`,
    hood: `This is one of the reasons money is abandoned entirely from chapter 6 onwards in
favour of time. A conserved quantity that agents pass around develops dynamics of its own, and
those dynamics swamp whatever the model was built to study. A day cannot be hoarded.`,
    found: `The thesis is here to WARN about this model, not to offer it. The footnote is the
best line in chapter 4: "There is not a large gap between claiming it does to making
justifications for wealth inequality as a natural, unavoidable consequence of money flow."

The model grows inequality. It does not explain it. Nothing in it corresponds to anything
about how wealth is actually distributed, and the resemblance of its output to real data is
exactly what makes it dangerous -- a good fit reached by a mechanism with no claim to being
the right one.

Two numbers from the rebuild. At the thesis' 200,000 exchanges the Gini is 0.33 and still
climbing; run it five times longer and it saturates at 0.48, which is the exponential limit
this process converges to and, again, "not far off real countries".

Compare e4, which is the same situation handled the other way: a striking output, and then the
simplest possible reduction saying exactly what it was.`,
  },

  f2: {
    economics: `Agents act one at a time and the order is reshuffled every day, because
otherwise whoever goes first has a permanent advantage. Reshuffling is right. This is what it
costs.

The gap between one agent's consecutive actions averages exactly one day and is almost never
one day for anybody in particular -- go last today and first tomorrow and almost no time has
passed; go first today and last tomorrow and nearly two days have. The distribution is a
pyramid, not a spike.`,
    hood: `The gap in turns is (N - i) + j: the turns left after you yesterday, plus the turns
before you today. Divided by N that is a number of days, averaging 1 and ranging over (0, 2).

The thesis' fix is to permute People and Firms in separate bins, so all the People act and
then all the Firms. The checkbox turns it on. It does not remove the spread, because there is
still an order within each bin; what it removes is a Firm's observation window straddling an
unknown number of Person actions.`,
    found: `Only 19 per cent of gaps land within a tenth of a day of the mean. If the
distribution were the spike everyone assumes when writing a t versus t-1 comparison, it would
be 100.

Which matters because that comparison is exactly what every Firm in group D makes: move, wait
a turn, ask whether revenue rose. It is a real source of the noise those Firms are misreading,
and it is invisible unless you go looking for it.

This is why the thesis distinguishes three decision types. A FEEDBACK decision is the one this
bites. A REFLEX -- the transmission belt's price response, which fires on the sale itself --
never asks how long ago anything happened, and is immune.`,
  },

  f3: {
    economics: `Core Model production with a fixed labour requirement of 3, and agents who
each supply one unit. Two populations run side by side, identical except in how they decide.

The rational ones work out that joining alone yields (1 - 3) = -2, worse than not working, and
decline. Every one of them reasons correctly, so nobody ever joins and production never
starts. The blind ones pick an employer at random and ask only whether they are better off
than last turn; four eventually stumble onto the same Firm and production starts.

The more rational agent performs worse, and not marginally -- it never produces anything at
all.`,
    hood: `The deadlock is a coordination failure that correct individual reasoning cannot
escape, because the payoff to the first mover is strictly negative and only turns positive
once three others have already made the same mistake.

Drag alpha down to 1 and the rational population starts producing immediately. The hole is the
threshold, not the reasoning.`,
    found: `This is the argument for the smooth production function used everywhere else in
the thesis. x = vl^2 + ml produces something for any positive input, so there is no threshold
to coordinate across and both kinds of agent work under it.

The Core Model form is perfectly sound as algebra, where l is an aggregate and nobody has to
be first to move. Put it in an agent model and it has a hole in it exactly where the agents
are. Model a3 shows that function's negative region as a curve; this shows what the region
does to a population.`,
  },

  g1: {
    economics: `Krugman's Core Model -- not an agent model, and a separate program in the
original. It is here because it is the benchmark the whole thesis is measured against and the
thing the transmission belt is offered as an alternative to.

Two regions, mobile manufacturing workers, immobile farmers, iceberg transport costs. The
chart solves for wages at every possible split of the workforce and plots the real wage ratio;
where it crosses 1, neither region pays better and nobody moves.

At the shipped T = 2.25 there are five equilibria. Lower the transport cost and the symmetric
one loses its stability while the corners keep theirs -- the economy tips into one region and
stays there. That is the tomahawk, and it is the canonical result of geographical economics.`,
    hood: `The three equations are iterated to a fixed point at each lambda. The port keeps one
oddity of the original rather than tidying it: on the first pass the second region's income and
price index are read before being assigned, so Java's zero-initialised arrays supply zero for
both. It converges regardless and the fixed point does not depend on it, but "tidied" is how a
port stops matching.

Two departures are deliberate. There is an iteration cap, because a browser tab that stops
responding is a worse failure than a slightly wrong point. And the tomahawk includes the
CORNER equilibria at lambda 0 and 1, which the original's crossing-detection could not see --
they are equilibria by construction rather than by crossing, since with every worker already
in one region there is nobody left to move. Without them the figure is drawn without the two
stable branches that give it its name.`,
    found: `Five equilibria at T = 2.25, as the thesis states: three interior crossings at
0.092 unstable, 0.500 stable and 0.910 unstable, plus the two stable corners. The tomahawk's
break point is at T around 1.9 and its sustain point at about 2.5.

What the Core Model cannot do is the reason for the rest of the catalogue. The elasticity e is
love of variety AND economies of scale at once, inseparably. And a region here is a
dimensionless point, so production structure cannot vary within one.

Compare e3, which gets a catastrophic production shift with two Firms, one Person and no space
at all. Compare e5's four-Firm variant, where structure varies inside a region. Compare e8,
which produces the two-core outcome from Firms identical in every respect.

And note what this model assumes that none of those do: every variety is produced at the same
scale by construction, there is no individual Firm anywhere in it, and lambda moves because
the modeller says workers follow real wages -- not because anybody decides anything.`,
  },

  // --- Group F: how the framework works -------------------------------------------------

  f4: {
    economics: `This is the core loop of the whole framework, and everything else in the
catalogue is built out of it. A Person does not optimise over space. They assemble
twenty-one candidate locations — ten nearby, ten anywhere, and one for where they already
are — work out what each would be worth, and move to the best.

C1's settlement, C6's rings, D1's split and E8's migration are all *this*, run five hundred
times a day for two thousand days.`,
    hood: `Each candidate is a Bundle, and a Bundle answers, from one spot: what is the best
wage I could get, what would every good cost me delivered here, what density would I incur,
and so what utility could I reach?

The whole framework hangs off that object. It is why the density cost is computable at all —
every agent evaluates proximity from its own position — and why no market-clearing price is
needed, since two People standing in different places simply face different prices.

Bundles are normally discarded the instant a decision is made. This model publishes one
Person's set through a read-only hook rather than retaining them, because keeping 500 × 21
Bundles alive to serve one visualisation would be an odd tax on every other model.`,
    found: `Two things this makes obvious that are easy to miss in the prose.

A Bundle marked with a cross was not scored badly — it was not scored at all. Its density cost
exceeded the wage, leaving no budget to buy anything with, so it was discarded. That single
rule is the whole of C8's dispersal. Turn the density cost up and watch crosses spread inward
from the crowded middle.

And the affordable Bundles usually split into a large group all worth within a few per cent of
each other and a handful much worse. So the decision is nearly always between near-equivalents
settled by a small margin — which is why the spatial equilibrium is loose rather than sharp,
and why a little demand noise is enough to make a Firm misread it.`,
    steps: [
      {
        when: () => true,
        caption: `The hollow square is where this Person stood when they decided. The discs
are the twenty-one locations they considered, sized and shaded by the utility each would
yield; the ring marks the one they took.`,
      },
      {
        elapsed: true,
        when: (w) => w.day > 2,
        caption: `The lines run from the chosen spot to each Firm the basket sources from,
weighted by spend. Note it buys from all three — CES with rho below 1 always does, which is
what keeps this framework away from winner-takes-all pricing.`,
      },
      {
        elapsed: true,
        when: (w) => w.day > 4,
        caption: `Crosses are Bundles that were discarded rather than scored: the density cost
there exceeded the whole wage. Every one of those density terms was computed from where the
other People are standing right now — which is the entire coordinating mechanism.`,
      },
    ],
  },
};

/**
 * Content for a model, falling back to its family.
 *
 * `d1b` reads `d1`, `e6` reads `e5`, `b2z` reads `b2`. The variants differ in parameters, not
 * in what they are about, and duplicating the prose four times is how it drifts.
 *
 * @param {string} id
 */
export function contentFor(id) {
  if (CONTENT[id]) return CONTENT[id];
  /** @type {Record<string, string>} */
  const family = {
    d1a: 'd1', d1b: 'd1', d1c: 'd1', d1d: 'd1',
    d2n: 'd2', d2nn: 'd2',
    e1raw: 'e1', e1many: 'e1',
    e6: 'e5',
    b2z: 'b2',
    c3w: 'c3', c3g: 'c3', c3c: 'c3', c3d: 'c3',
  };
  return CONTENT[family[id]] ?? {};
}
