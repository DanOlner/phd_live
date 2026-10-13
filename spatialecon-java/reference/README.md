# Reference output

Numbers from the original 2013 implementation, for checking the refactor against.

## The batch-export problem

**The shipped jars cannot write CSV.** This was tried and it does not work — worth recording so nobody spends the afternoon on it again.

`io/CSVWriter.java:85` iterates `ds.buckets`. Every jar build populates the *other* collection, `ds.variableStores` (`io/DataStore.java:26–27` declares both). The two are disjoint, so the write loop runs zero times. The run still prints `writing` and `Ping!` on the way out, which makes it look like it worked.

Setting `modelRunDays` to a finite value and creating a `data/` directory — both genuinely required, and neither sufficient — changes nothing.

So the original's data path is **interactive**: series were exported from the on-screen graph windows by keypress, and the thesis figures themselves are screen captures. That is what the `RR_`, `RV_`, `JK_` prefixes on the figure filenames are — the `PersistentAlphabeticNumberer` "keyPrint" labels.

The specific key is **`w`**, with focus on a graph window: it sets `saveVariableStoreClone`, which writes that graph's series (`CorrelateArraysGraph.java:876`). `p` screenshots the window. Note that `k` on the main window — commented "kill the model, write the data, shut it down" — goes through the broken `CSVWriter` path above and writes nothing.

That is exactly how the five files below were produced, and it means **more reference data is obtainable right now**: run a jar, let it settle, press `w` on the graph you care about. Manual and per-figure, but it needs no code changes.

Three ways forward, in order of cost:

1. Use the captured series already in the tree (below).
2. Run a jar interactively and export from the graph windows by hand.
3. Patch `CSVWriter` to iterate `variableStores` and rebuild the original from source. Probably a small change, and it would make every scenario batch-capturable — worth doing if we end up wanting exact per-scenario traces.

Given the agreed fidelity target is behavioural equivalence, (1) plus visual comparison against the thesis figures is proportionate for now.

## `original-csv/`

Five series exported from `CEUS1` runs in December 2012 — the monocentric density model, i.e. digest models **C1** and **C8**. Copied unmodified from `Thesis_on_github/FrameworkModelSourceCode/data/`.

Single column, no header; one row per model day.

| File | Days | Min | Max | Mean |
|---|---|---|---|---|
| `RR_..._maxUtility_timeSeries.csv` | 999 | 0.74364 | 0.75927 | 0.75236 |
| `RV_..._maxUtility_timeSeries.csv` | 4999 | 0.74364 | 0.75927 | 0.75284 |
| `RS_..._distanceMovedToday_timeSeries.csv` | 999 | 0 | 0.67243 | 0.00103 |
| `RW_..._distanceMovedToday_timeSeries.csv` | 4999 | 0 | 0.67243 | 0.00021 |
| `RX_..._distanceToCentralFirm_timeSeries.csv` | 4999 | 0.17780 | 0.78718 | 0.17827 |

**What they are good for.** Two things the refactor should reproduce:

- **Utility equilibriates and stays put.** It sits in a band of about 0.0156 (roughly 2% of its value) across 5,000 days. The claim of §5.4.1 is that utility equalises across People regardless of distance from the centre; these show it is also steady over time.
- **Movement decays towards spatial equilibrium.** Mean distance moved falls five-fold between the 1,000- and 5,000-day runs. §5.4.3 treats mean movement below 0.0001 as equilibrium; the 5,000-day run at 0.00021 is approaching but has not crossed it.

**What they are not good for.** The exact parameters behind them are not recorded. `CEUS1.properties` as it stands today has `DENSITYCOST = 100`, which is the §5.4.7 setting where equilibrium starts breaking down — not the §5.4.1 setting of 10. The absolute level (~0.752) should therefore not be treated as a target until we know which configuration produced it. The *shapes* — tight utility band, decaying movement — are the safe comparisons.

Whether the file is a mean across People or a single Person's trace is also not recorded. The `_timeSeries` suffix and the graph classes suggest a mean.
