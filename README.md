# "An agent-based modelling approach to spatial economic theory" live version

> *This README was drafted by Claude Code.*

Dan Olner's 2013 Geography PhD at the University of Leeds, *An agent-based modelling approach to spatial economic theory*, built a spatial economic agent framework from first principles. It asked how the costs of moving people and goods across distance shape the economic landscape, and why agent-based modelling had barely touched spatial economics. Its models were interactive Java (Processing) programs. This project rebuilds every one of them to run in the browser, with explanations and walkthroughs alongside.

**[Open the models → danolner.github.io/phd_live](https://danolner.github.io/phd_live/)**

If you want the argument before the models, start with the [methodology page](https://danolner.github.io/phd_live/methodology.html). It covers why an agent's budget is a day rather than money, which kind of decision quietly breaks, and why showing something is the start of an explanation rather than the end.

## The models

There are forty-two models in seven groups, following the thesis, plus a curiosity from the years of building it. Each one runs live, with its own controls and with a fixed run plus a reroll. Pop-out notes say what to watch, explain the economics behind the model and how it is built, and, where there is one, describe what the rebuild found that the thesis did not say.

| Group | What it is for | Models |
|---|---|---|
| **A** · the economics on its own | The utility and production functions the rest of the thesis is built from, with no space and barely any agents. | [A1](https://danolner.github.io/phd_live/model.html?m=a1) · [A2](https://danolner.github.io/phd_live/model.html?m=a2) · [A3](https://danolner.github.io/phd_live/model.html?m=a3) · [A4](https://danolner.github.io/phd_live/model.html?m=a4) |
| **B** · space meets love of variety | Distance enters through a delivered price. What variety is worth starts to depend on what reaching it costs. | [B1](https://danolner.github.io/phd_live/model.html?m=b1) · [B2](https://danolner.github.io/phd_live/model.html?m=b2) · [B2z](https://danolner.github.io/phd_live/model.html?m=b2z) · [B3](https://danolner.github.io/phd_live/model.html?m=b3) |
| **C** · the density cost | Crowding is made expensive and a city appears, with no landlords, no rent and no market for space. | [C1](https://danolner.github.io/phd_live/model.html?m=c1) · [C2](https://danolner.github.io/phd_live/model.html?m=c2) · [C3w](https://danolner.github.io/phd_live/model.html?m=c3w) · [C3g](https://danolner.github.io/phd_live/model.html?m=c3g) · [C3c](https://danolner.github.io/phd_live/model.html?m=c3c) · [C3d](https://danolner.github.io/phd_live/model.html?m=c3d) · [C4](https://danolner.github.io/phd_live/model.html?m=c4) · [C5](https://danolner.github.io/phd_live/model.html?m=c5) · [C6](https://danolner.github.io/phd_live/model.html?m=c6) · [C6c](https://danolner.github.io/phd_live/model.html?m=c6c) · [C7](https://danolner.github.io/phd_live/model.html?m=c7) · [C8](https://danolner.github.io/phd_live/model.html?m=c8) |
| **D** · Firms groping for a location | Firms that know nothing except whether their own revenue went up. How poorly they do under noise is the finding. | [D1a](https://danolner.github.io/phd_live/model.html?m=d1a) · [D1b](https://danolner.github.io/phd_live/model.html?m=d1b) · [D1c](https://danolner.github.io/phd_live/model.html?m=d1c) · [D1d](https://danolner.github.io/phd_live/model.html?m=d1d) · [D2](https://danolner.github.io/phd_live/model.html?m=d2) · [D2n](https://danolner.github.io/phd_live/model.html?m=d2n) · [D2nn](https://danolner.github.io/phd_live/model.html?m=d2nn) |
| **E** · the transmission belt | Money is removed and People buy goods directly with time. Production reorganises abruptly, and the gains reach consumers as welfare. | [E1](https://danolner.github.io/phd_live/model.html?m=e1) · [E1raw](https://danolner.github.io/phd_live/model.html?m=e1raw) · [E1many](https://danolner.github.io/phd_live/model.html?m=e1many) · [E2](https://danolner.github.io/phd_live/model.html?m=e2) · [E3](https://danolner.github.io/phd_live/model.html?m=e3) · [E4](https://danolner.github.io/phd_live/model.html?m=e4) · [E5](https://danolner.github.io/phd_live/model.html?m=e5) · [E6](https://danolner.github.io/phd_live/model.html?m=e6) · [E7](https://danolner.github.io/phd_live/model.html?m=e7) · [E8](https://danolner.github.io/phd_live/model.html?m=e8) |
| **F** · how the framework works | The machinery itself made visible: what a Bundle is, and what an agent can actually be assumed to do. | [F1](https://danolner.github.io/phd_live/model.html?m=f1) · [F2](https://danolner.github.io/phd_live/model.html?m=f2) · [F3](https://danolner.github.io/phd_live/model.html?m=f3) · [F4](https://danolner.github.io/phd_live/model.html?m=f4) |
| **G** · the comparator | Krugman's Core Model, the benchmark everything else is measured against. | [G1](https://danolner.github.io/phd_live/model.html?m=g1) |
| **H** · curiosities | Not in the thesis: runs from the years of building the framework that survive only as a picture or a video, reconstructed and tested against what is left. | [H1](https://danolner.github.io/phd_live/model.html?m=h1) |

Good places to start:

- **[C1, monocentric settlement](https://danolner.github.io/phd_live/model.html?m=c1).** People trade crowding against distance and settle around a central Firm, ending up equally well off, with no land market anywhere in the model.
- **[E3, the regime transition](https://danolner.github.io/phd_live/model.html?m=e3).** Two Firms, one Person and no space. Raise ρ past about 0.75 and one Firm pulls ahead and takes over production, so a sudden shift in where production happens does not need space at all.
- **[E8, transmission belt plus density cost](https://danolner.github.io/phd_live/model.html?m=e8).** The closing synthesis. A more efficient producer emerges from two identical Firms, and People migrate to it and form a settlement.
- **[D2, the DMR game](https://danolner.github.io/phd_live/model.html?m=d2).** The one place where the rebuild still disagrees with the thesis (see below).
- **[G1, Krugman's Core Model](https://danolner.github.io/phd_live/model.html?m=g1).** The short-run wiggle and the long-run tomahawk, as the comparator.

## What was done

**1. Reading the original.** This covered the thesis, about 190 Java classes and seven runnable jars. The main discovery was that scenarios were never selected by configuration. They were selected by commenting blocks of code in and out and then recompiling, so the source tree is one arbitrary snapshot and not the state behind any particular figure. What each published figure actually ran was recovered by decompiling the jars.

**2. A Java reference implementation** ([`spatialecon-java/`](spatialecon-java/)). This is a clean rebuild of 28 models as self-describing scenarios, with 55 tests that pin a dozen thesis claims to numbers. It turned the thesis from prose plus an unreliable source tree into a verified specification, so the port that followed was a translation of something known. It is not published.

**3. The JavaScript site** ([`spatialecon-js/`](spatialecon-js/)). It covers all 42 models and the curiosity, has no dependencies and no build step, and is plain ES modules drawing to Canvas 2D in the original's monochrome visual style. It has 169 tests, seven precomputed parameter sweeps, step-through walkthroughs and the methodology page. It is published to GitHub Pages directly from that folder.

**Verification** works in two tiers. The deterministic economics agrees with the Java reference to six significant figures. The two implementations deliberately use different random generators, so everything stochastic is checked against the published claim within a window rather than against a trace. For example, symmetry between two Firms breaks at ρ = 0.766 here and at 0.764 in the Java, and transmission-belt clearing prices match values worked out by hand to five decimal places.

## What the rebuild found

- **Three apparent failures to reproduce turned out to be one measurement error.** The figure labelled "Firm efficiency ratio" plots total output, not output per unit. With the shipped parameters, output per unit cannot exceed 1.3 whatever the model does. Reading the right variable gives a ratio of 13.55 at ρ = 0.9, against the thesis' ~13. A figure label is not a variable name.
- **One result still does not reproduce.** The thesis reports that strong love of variety makes the Firms' location game harder under noise. Both rebuilds show the opposite, and each has a coherent mechanism for its own result. It is on the site, in D2's notes.
- **The undamped transmission belt never settles**, which corrects an earlier record. It is an undamped oscillator, and showing that is the point of the variant.
- **Two scenario configs (C3 and C7) are lost.** Both are reconstructed from the thesis text, and every published direction they show reproduces.
- **A 2011 video, reconstructed as H1.** In "Green blob red blob", one settlement chases another, smothers it and is shaken off, over and over. Its red and green turn out to be utility rank, not two cities. The setup that reproduces it is two Firms that crawl for revenue, each with a workforce tied to it for work only, and one paying double. It is scored against statistics measured on the video's own frames. Charting what each side acts on showed the chase is the Firms' doing. Both workforces are better off together, but the better-paid Firm earns more apart, and its crawl cannot tell its rival's moves from its own: the thesis' own argument about groping Firms.

The full list is the "Corrections to the record" table in [BUILD_LOG.md](BUILD_LOG.md).

## Documents

The planning and reference documents at the root:

- **[THESIS_DIGEST.md](THESIS_DIGEST.md)**: the thesis' argument and maths in digest form, plus the catalogue of models (groups A–G) whose IDs name everything on the site.
- **[CODE_ANATOMY.md](CODE_ANATOMY.md)**: how the original Java models were actually built, including which code and config produced each published figure, the exact algorithms and constants, and the traps.
- **[BUILD_PLANNER.md](BUILD_PLANNER.md)**: the plan for the JavaScript port, covering dependency policy, translation rules, visual fidelity, verification, staging and the design decisions now settled.
- **[BUILD_LOG.md](BUILD_LOG.md)**: what was built stage by stage and what each stage taught, including the claims that later had to be corrected.
- **[GREEN_BLOB_RED_BLOB.md](GREEN_BLOB_RED_BLOB.md)**: the case for H1's reconstruction of the 2011 video, covering what the frames show, how the 2011 renderer drew them, and the variants tested.
- **[CLAUDE.md](CLAUDE.md)**: working notes for Claude Code, covering project conventions, known traps in the original code and how to run the 2013 jars.

The two implementations have their own READMEs too:

- **[spatialecon-js/README.md](spatialecon-js/README.md)**: how to run, test and deploy the site, its module layout, and the verification results.
- **[spatialecon-java/README.md](spatialecon-java/README.md)**: what the Java reference implementation verified, model by model.

## Repository layout

| Path | What it is |
|---|---|
| [`spatialecon-js/`](spatialecon-js/) | The site, which is the deliverable. |
| [`spatialecon-java/`](spatialecon-java/) | The Java reference implementation that verified the models. Not published. |
| [`Thesis_on_github/`](Thesis_on_github/) | The [thesis PDF](Thesis_on_github/DanOlnerThesisPrintDraft23_6_13.pdf), the original framework source and the seven runnable jars, all untouched. |
| [`ThesisCorrectionsLatex/`](ThesisCorrectionsLatex/) | The thesis' LaTeX sources. |
| [`.github/workflows/deploy-pages.yml`](.github/workflows/deploy-pages.yml) | Publishes `spatialecon-js/` to GitHub Pages on every push that changes it. |

## Running it locally

```sh
cd spatialecon-js
python3 -m http.server 8000    # then open http://localhost:8000
npm test                       # needs Node 18+
```

There is nothing to install and nothing to build. The static server is needed only because browsers will not load ES modules from `file://`. The original 2013 jars still run, but they need Java 17. [CLAUDE.md](CLAUDE.md) explains how.
