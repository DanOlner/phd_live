# Green blob, red blob: reconstructing the August 2011 video

**The video:** [youtube.com/watch?v=bb2Q5iXxOV4](https://www.youtube.com/watch?v=bb2Q5iXxOV4), "Green blob red blob", uploaded 13 August 2011. 33 s, 494×480 at 30 fps. Description: *"Green blob likes red blob. A lot. In a slightly smothering way. Red blob is far from sure about this."*

**The question:** what setup produced it? The code that ran it is gone. The scenario was wired by hand in the usual way (CLAUDE.md, trap 1), and the source tree is a 2013 snapshot. What follows reconstructs the setup from the frames, from the 2011 drawing code that survives in the snapshot, and from figures and draft text written in the same fortnight. The best candidate was then tested by re-running it on the `spatialecon-js` engine.

## Bottom line

- **Red and green are not two kinds of agent. They are utility rank.** The 2011 renderer coloured every Person on a min–max ramp, red for the highest utility in the population that frame and green for the lowest. The two settlements show as two solid colours because one workforce is better off than the other throughout.
- **The white ring is nobody.** It marks the mean position of all actors, which is why it always sits in the gap between the settlements, and why it vanishes whenever they overlap.
- **The best reconstruction:** two mobile Firms (the DMR crawl), each with its own workforce tied to it **for work only**. Everyone buys from both Firms (CES, ρ ≈ 0.6), mobile People pay a density cost, and the red Firm pays about twice the wage. This is the von Thünen tie applied to the labour channel and not to goods.
- **Why green likes red:** red's People are richer, so they are the better customers. Green's Firm finds its revenue rising as it moves toward them, and green's commuters follow their Firm. **Why red is far from sure:** green's crowding raises the density cost red's People pay, their budgets shrink, red's Firm loses revenue, and its crawl breaks away and accelerates. **The smothering:** when the two meet, they sort exactly as C6 does. The better-paid take the centre ("the wealthy buy proximity") and the others wrap round them.
- **The re-run reproduces the video's statistics closely, not exactly:** the chase in the right direction at about the right strength, the right share of time apart and engulfed, and the right cloud scale at about one model day per video frame. It cycles about half as often as the video.

That matches your recollection ("workers/commuters locked to each central firm", "green city gains, red loses") on every point except one: the colours never denoted the cities.

## 1. What the video shows

Method: the video was downloaded with `yt-dlp`, and all 993 frames were streamed through `ffmpeg`. Each frame was segmented into red, green and white pixels by colour threshold, giving a centroid and rms spread per colour. Velocities come from 4-frame differences of 5-frame-smoothed centroids. "Separation" is centroid distance divided by the sum of the two rms spreads, so 1 is two clouds just touching. Below 0.35 counts as **engulfed** and above 0.75 as **apart**.

| Measure | Value |
|---|---|
| Red moving away from green (along the line joining them) | 73% of frames |
| Green moving toward red | 75% of frames |
| Green's velocity follows red's | best match at a 6-frame lag (cosine 0.31, against 0.09 at no lag) |
| Speed, red vs green | 1.6 vs 2.5 px/frame; green's sideways component is 2.4× red's (the wrapping) |
| Spread (rms), red vs green | 13.5 vs 17.4 px, ratio **1.29**; when engulfed, green 24.5 px around red 12.6 px |
| Red cloud scale | rms ≈ 0.032 of the model-space width (≈425 px) |
| Separation | median 1.16; apart 72%, **far apart (> 1.5) 35%**, engulfed 11% |
| Meetings | 10 in 33 s (20 apart/engulfed transitions), irregular, 1–6 s apart |
| Cloud sizes when apart | red pixel count steady (sd 7%, no trend), green sd 12%: no wholesale switching between clouds |
| White ring | visible only when apart (248 frames, none while engulfed); sits 0.47 of the way from green's centroid to red's, about 4 px off the axis |
| Density radius | ≈ 13 px on isolated People, so 1/32 of the width |

The cycle runs like this. The settlements sit apart with the white ring in the gap. Green closes in and wraps right round red, with red as a compact core inside a green annulus. Red breaks out sideways in a burst (red's speed spikes to 5–8 px/frame), and green follows. The pair as a whole wanders the top-left of the space. When apart, each settlement packs into concentric rings about one density radius apart around a solid central square, the arrangement C1/C2 produce around a Firm.

## 2. Reading the picture: what the 2011 renderer drew

The video predates the monochrome decision (CODE_ANATOMY §5.9). The colour drawing code it used survives, dormant, in `SpaceTools.draw3D` (the September 2011 UML diagram, `Thesis_on_github/location1_UML.jpeg`, has `Space` as the `Drawable`), with a near-copy in `Visualiser.draw3D`.

**Colour is relative utility.** With `gl.visUtilityRangeOn`, each Person's square is stroked `(colVal, 255 − colVal, 0)`, where `colVal` maps utility onto 0–255 between the previous frame's lowest and highest (`SpaceTools.java:2084`). Red is the best-off Person in the population and green the worst-off. Group colouring existed (`colourByFirm`, `SpaceTools.java:2297`, `Visualiser.java:220`), but it cannot have been on: colouring by Firm cannot change, and the video's colours do. In frames 0–2 **both** settlements are red, and at frame 3 the lower one turns green.

**The opening frames prove it.** `useHighUtil` and `useLowUtil` are bare `double` fields (`SpaceTools.java:80`), so they start at 0. The range is only updated `if (findLowUtil > 0)` (`SpaceTools.java:2605`), meaning once **every** Person has positive utility. In frames 0–1 a few stray People sit outside both settlements; while any of them has zero utility the range stays 0–0, `colVal` divides by zero, and Processing clamps the infinity to pure red. The strays rejoin at frame 2, the range is set, and the worse-off settlement turns green on the next frame.

**So only the ranking is visible.** Min–max normalisation stretches any gap to the full red–green span, so the absolute utility difference between the settlements could be small. What the video does establish is that the ranking never flips in 33 s.

**The white ring is `drawMeanCentrePoint()`** (`SpaceTools.java:2615`). It is a filled 10 px circle plus a 15 px ring at the mean position of every actor in `isInSpace`, People and Firms alike, lifted 20 units in z. Its call is commented out in the snapshot (`:2573`). A population mean naturally sits between two equal settlements, and 0.47 of the way across is what two equal halves give. It is hidden when inside a cloud because, off top view, each Person's utility square is raised by utility × `visFactorUtility` (50 in the 2011 preset). With utility above 0.4 those squares sit above the ring, which suggests the run used the 3-D view rotated to look straight down.

**The Firms are drawn, but invisibly.** A Firm is a 5 px square in its palette colour, the same size as a Person's, so it vanishes into its settlement. It is presumably the solid square at the centre of each ring pattern. The palette that was live is unknown. `gl.nextActorColour()` has red, green, blue and cyan commented out (`gl.java:1356`), and `ActorMaker` resets the palette before making Firms (`ActorMaker.java:137`).

**Scale.** A density radius of 1/32 of the width fits width 2 with radius 1/16, the density models' convention (`CEUS1`).

## 3. What else was on the bench, August–September 2011

- **`DCrealism2/3/4.jpg` and `DensiyCostTwoFirmRealismPoint.psd`, 25 August**, in `ThesisCorrectionsLatex/ChapterConnectedModels/Figures/`. Same renderer, same white ring: two settlements with a solid square at each core and the ring dead centre, the Firms fixed. No `.tex` file includes them. These are the static two-Firm density run, twelve days after the video.
- **`Engelwdensity.jpg`, 31 August,** with the September 2011 draft text (`ChapterConnectedModels/ModelsFromWord.tex`, the Engel section). One necessity Firm, one luxury Firm, People mobile, a wage-distance cost, and "People work for either Firm, though they buy in from the necessity producer". The same white ring sits between the two clouds, nearer the bigger one. The draft also has a commented-out note about watching a run recorded as `RandomFirms_EngelChangingDeliveryCost350days.avi`: recording runs was routine that month.
- **`DensityCost250w_wagedistanceon.jpg`, 29 August.** Its caption in the same draft (line 740): *"White indicates where People are unable to get any utility."* This is the other white in this renderer, and a different glyph from the ring.
- **`vonThunen4firm.jpg` and `vonThunenPrice1.jpg`, 24–29 August.** The von Thünen tie and per-Firm cost ranging were being built that fortnight.
- **`2firmgame_rho_*.jpg` and `FirmGame11/12.jpg`, 29 August.** Two mobile Firms playing the DMR game, so the crawl was live that same week.
- **The one surviving hand-written preset**, `gl.setUp()` (`gl.java:807`). 8 Firms, 800 People, `vonThunenGoods = true`, `vonThunenWage = false`, `colourByFirm = true`, density cost 1, CES ρ 0.5, Firms fixed. Same machinery, different wiring.

So by mid-August every ingredient existed: per-channel tie flags (`vonThunenWage`, `vonThunenGoods`, `vonThunenCommute`, `vonThunenDelivery`), per-Firm cost ranging, Firms that crawl, a density cost and the colour ramp.

**One caveat about the snapshot.** In the 2013 code, setting any of the four tie flags switches on `vonThunenMode` (`gl.java:704`), and the goods restriction is keyed on that (`PersonAction.java:423`). So **the snapshot cannot express "work tied, goods open" from a properties file.** In August 2011 the channels look to have been independent: the flags are separate, `ActorMaker.java:260` has the per-flag condition commented out, and the colour condition still reads `vonThunenGoods || vonThunenWage`. That is an inference, not something the snapshot shows directly.

## 4. The reconstruction

| | Setting | Basis |
|---|---|---|
| Space | bounded plane, width 2, density radius 1/16 | measured ratio; CEUS1 convention |
| Firms | 2, starting 0.3 apart, both crawling (`FirmActRespondAverage` logic) | two cores; the chase needs mobile Firms (§5) |
| People | split round-robin, half per Firm, all mobile | the ring at the midpoint; steady cloud sizes |
| Tie | **work only**: each Person can only work for their own Firm | your recollection; the visual test in §5 |
| Goods | open: everyone buys from both, CES **ρ ≈ 0.6** | fitted; ρ is the dial (§5) |
| Wage gap | red's Firm pays **2×** (`wageRange` convention) | necessary; the ratio is not identifiable |
| Costs | commute 1, delivery 0.75, density cost 5 | fitted to separation and cloud scale |
| People count | 160 used; immaterial to shape | the density divisor makes cloud size head-count-free (tested on the runner-up wiring: 60, 160 and 300 People all give rms ≈ 0.035 W) |

**The mechanism, step by step:**

1. **The tie keeps two workforces apart.** Commuting anchors each workforce to its own Firm, and nobody can change employer, so the two settlements cannot merge into one labour market.
2. **Love of variety pulls them together.** Everyone wants some of both goods, and delivery is costly, so each settlement is drawn toward the other's Firm. The density cost pushes them apart. ρ sets the balance.
3. **The wage gap makes it one-sided.** Red's People are richer. That makes them (a) the better customers and (b) better able to afford density: a higher wage shrinks the density cost in proportion, the C6 result.
4. **Green's Firm courts red's custom.** Revenue is quantity times base price from every buyer, and the rich buyers are red. Green's Firm's crawl finds revenue rising toward red, so it keeps going and accelerates, and green's commuters follow it. Measured over 3 seeds, green's Firm steps toward red in 66–69% of its moves, and its workforce's movement tracks it.
5. **Red's Firm flees the crowd.** When green wraps round, red's People pay more density cost, their budgets drop and red's revenue falls. Its crawl reverses and re-randomises until some direction raises revenue, usually away from green, then accelerates (`adjustStep`). That is the breakout burst, and red's commuters follow. Red's Firm steps away from green in 57–60% of its moves.
6. **Meeting is C6.** With both Firms close, both workforces want the same central space. The better-paid take the core and the rest form the ring: von Thünen sorting, briefly, until step 5 breaks it.

Your summary holds up as economics. Green gains, because its Firm wins red's custom and its People get red's good cheaply. Red loses, because its People pay green's crowding cost and its Firm loses the revenue.

## 5. The test

**Method.** A scratch script imported the `spatialecon-js` engine read-only (`World`, `defineModel`, `firmLocationCrawl`, `ces`, `constrained`) and supplied one Person behaviour of its own. It is `personBundle`'s logic with the tie split into a work channel and a goods channel, which the engine cannot currently express (§6). Each run was 3,000 days with the first 500 discarded, scored with the same statistics measured on the video. "Red" is whichever group holds the higher utility, as in the 2011 renderer. Ranges are over 2–3 seeds. Matching red's cloud speed to the video gives **about one model day per video frame**, which is plausible for an interactive run at `timeDelay = 7` ms.

| | Video | **Work tied, goods open** (the reconstruction) | Goods tied, work open (runner-up) |
|---|---|---|---|
| Better-off group keeps the lead | always | 100% of days | 100% |
| Red moving away / green moving toward | 73% / 75% | 63–66% / 65–71% | 76–78% / 74–77% |
| Spread ratio, green / red | 1.29 | 1.35–1.47 | 1.26–1.29 |
| Red cloud rms / width | 0.032 | 0.036–0.038 | 0.036 |
| Median separation | 1.16 | 0.89–1.14 | 0.89–0.92 |
| Far apart (> 1.5) | 35% | 25–35% | **0%** |
| Engulfed (< 0.35) | 11% | 7–14% | **0%** |
| Transitions per 993 frames | 20 | 8–14 | 0–2 |

**Controls**, at the same settings. Each removes one ingredient, and each kills the chase:

| Variant | Red away / green toward | What happens |
|---|---|---|
| Reconstruction, but equal wages | 50–51% / 51% | settlements sit side by side, no leader, no chase |
| Reconstruction, but Firms fixed | 10–12% / 10–12% | nothing moves |
| Both channels tied (C6-style), 2× wage | 51–52% / 50–51% | settlements repel and stay apart |
| Goods tied, wage premium attached to the *Person* (the commented `wageOffer * me.vonThunenFirmIndex` form) | 51–52% / 48–50% | apart, no chase: green has no reason to seek red's Firm |
| Nothing tied, premium attached to the Person | 30–32% / 22–25% | permanent C6 rings, red core inside a green annulus (spread ratio 2.2), no escape |

**ρ is the dial** (one seed each, indicative). Below about 0.55 the settlements spend long spells merged, and at a higher delivery cost they merge for good. Between 0.55 and 0.65 they cycle, which is the video. Above about 0.65 they part and stay parted (median separation 6–10). Love of variety is what holds the two cities together, which ties neatly to C7.

**Why the runner-up loses.** Goods tied with work open is the wiring of the surviving 2011 preset, and it gives the **strongest** chase: green's People commute to red's better-paying Firm and buy only from their own. But its settlements stay glued edge to edge. It is never far apart and never engulfed, and its frames show one lopsided blob, red on one side and green on the other, never the two clean states the video alternates between. A faster crawl (2–4×, as in `FirmActFeb11`'s 0.05 base rate) makes it touch and part more often, but it still never engulfs.

## 6. What doesn't match, and what's untested

- **Cycling is about half the video's rate.** The chase is a little weaker, green a little more diffuse, and red's cloud slightly larger. Candidates: the 2011 crawl constants (which Firm action was live on 13 August is unrecoverable; `FirmActFeb11` used 0.05), the wage ratio, the commute cost, the greedy optimiser's natural noise, or how many days the screen capture skipped per frame.
- **The Engel necessity/luxury pair with mobile Firms is untested.** It was on the bench that fortnight, and it has the same "one side needs the other more" shape. The JS engine has `stoneGeary`, so it is a cheap next test.
- **A hybrid is possible.** For example, goods partly tied. The two best variants bracket the video: one chases harder, the other alternates better.
- **Left to run, the pair works along the walls.** In 77–94% of days at least one settlement is within a tenth of the width of an edge, against 5% of the video's frames. That is not a different dynamic. A breakaway accelerates for as long as moving pays, so sooner or later one runs into a wall, which stops it, and the chaser pins it there. The video drifted at the same rate as the model (0.42 of the window over its 33 seconds, against 0.25–0.43 per 1,000 days here), starting from mid-field, so it would have reached an edge a few thousand frames later. The original's space had the same walls (`checkInTorus`: "set to the closest edge"). A wider space only delays it, and slowing the breakaways (removing the crawl's acceleration) stops the settlements ever coming apart.

## 7. On the site: model `h1`

Built as **`h1`**, the first entry in a new section, **H · curiosities** (`model.html?m=h1`). It runs the §4 setup at the §4 defaults, on the site's default seed (12). On that seed: chase 61% / 63%, far apart 43%, engulfed 10%.

**Engine.** Two small additions, both backward-compatible:

- **`config.tie`** (`'both'`, the default, as in C6; `'work'`; `'goods'`) says which channels an assigned Firm closes. `personBundle` reads it at decision time, in the two `assignedFirm` checks in `behaviour.js`, so it can be a live dial. That is why it is a config field rather than the `personBundle` option first proposed here.
- **`config.rangeStep`** (default 1) spaces the ranged levels: Firm *i* gets base × (1 + *i* × step), set live through `world.setRangeStep`. At 1 it is the original's ×1, ×2…; here it makes the wage gap a dial.

The page also now draws `boolean` and `choice` dials as a checkbox and a select (`chrome.js`), which is what `Parameter.kind` was always for. F2's bins toggle becomes a checkbox as a side effect, and dragging on the canvas now moves numeric dials only.

**Dials:**

- ρ, 0.05–0.99, capped as the original was;
- the wage gap, Firm 1 over Firm 0, 1–3;
- density cost, 0–20;
- distance cost, 0–3;
- the tie channel: work, goods or both;
- Firms move, on or off.

ρ and the wage gap are on the left and right drag.

**View.** The plane view with the workforces as group shapes and greys: squares at wage 1, circles at wage 2. This follows BUILD_PLANNER §4.2, and "shade People by: utility" gives the 2011 encoding in grey. There are two additions:

- an optional **2011 ring** overlay, the mean of all actors, offered on `h1` only;
- a second panel tracing the **separation** against "touching", in a rolling 1,500-day window.

**Pop-outs and walkthrough.** There is an economics panel, an under-the-hood panel and a what-we-found panel, all in `src/content.js`, plus a five-step walkthrough keyed to separation: set-up, apart, caught, away, caught again. All five fire by day 544 on the default seed.

**Tests.** These are tier 2, with windows set from eight seeds at 2,000 days (the first 400 discarded):

| Claim | Window | Eight seeds |
|---|---|---|
| Better-paid workforce leads on utility | ≥ 95% of days | 100% |
| People moving away / toward | > 53% each | 57–68% / 57–69% |
| Green's Firm steps toward / red's away | > 54% / > 50% | 58–67% / 53–63% |
| Far apart / engulfed / changes | > 10% / > 3% / ≥ 6 | 17–43% / 6–14% / 11–22 |
| Equal wages | chase 40–60%, no stable leader | 45–53% |
| Firms fixed | chase < 30% | 12–24% |
| Both channels tied | far apart > 90%, chase 40–60% | 99–100%, 47–54% |
| Goods tied (the runner-up) | toward > 70%, never apart or engulfed | 74–77%, 0% |
| ρ 0.45 / ρ 0.8 | far < 15% and engulfed > 10% / far > 75% | 1–6% and 11–28% / 82–100% |

There are also engine tests for the tie channels and the ranging step, view tests for the ring, the panel and the legend, and the site's walkthrough-reachability check.

**The catalogue still** is day 2,555 of the default run, chosen as `defaultDays` because that frame is a mid-field engulfment. Nothing settles, so any run length is arbitrary.

## 8. Questions for you

1. Does "workers tied to their own Firm, buying from both" ring true? The surviving 2011 preset ties goods instead, which is the runner-up.
2. What made red's Firm the better one: a wage multiple as assumed here, or something else, such as commute cost, price, or the Engel necessity/luxury pairing?
3. Do you remember recording from the 3-D view rotated to look straight down? It would explain the ring disappearing.
4. Any memory of the People count, ρ or the density cost?
