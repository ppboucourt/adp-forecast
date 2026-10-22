# adp-forecast

A command-line tool that tracks the monthly ADP National Employment Report (total private payrolls) and
forecasts the next print. It answers three questions:

1. **What were the numbers?** `history` shows each month as currently published, and with `--first-print`
   what the market first saw and how much it has been revised since.
2. **What comes next?** `forecast` predicts next month's change.
3. **Why, and how far to trust it?** `forecast --explain` breaks the forecast into its terms and shows the
   model's measured accuracy against first prints.

Design decisions and their reasons are in [`docs/DESIGN.md`](docs/DESIGN.md). The AI-assisted build is
documented in [`docs/PROMPTS-PLAN.md`](docs/PROMPTS-PLAN.md) and [`CLAUDE.md`](CLAUDE.md).

## Quick start

Requires Docker and `make`. No API key needed: the repository ships a snapshot of every published vintage.

```
make init                                    # build the image, seed the database from the snapshot
make run ARGS="forecast --explain"
```

```
Oct 2026 forecast (AR(3), window=exclude-covid): +112K
  intercept                     +48K
  0.71 × Sep (+ 90K)            +64K
  -0.14 × Aug (+ 36K)            -5K
  0.10 × Jul (+ 44K)             +5K
  ──────────────────────────────────
  forecast                     +112K
  backtest MAE 62K | skill vs naive +0.07 | n=49
```

The forecast is the sum of the rows: an intercept plus a weight times each of the last three published
changes. The last line is a live backtest of exactly this model and window: mean absolute error against
first prints over `n` months, and skill relative to the naive "same as last month" forecast. See
[Results](#results) for what those numbers do and don't support.

### Commands

| Command | What it does |
|---|---|
| `history [--last N] [--first-print]` | Recent months: level and change; optionally first print and revision |
| `forecast [--model ar\|naive\|ma] [--window all\|exclude-covid\|rolling-60] [--explain]` | Next month's forecast; defaults `ar`, `exclude-covid` |
| `backtest [--model …] [--baseline …] [--window …]… [--score …] [--basis real-time\|latest-vintage]` | Accuracy table, one row per fit window, all scored on the same months; skill is vs. `--baseline` (default `naive`) |
| `sync [--source csv\|fred] [--export path]` | Load vintages into the local database (never overwrites a stored vintage) |

Run any of them with `make run ARGS="…"`. Unknown model or window names list the valid ones. A warning
appears when the newest stored vintage is more than 35 days old.

### Live data (optional)

Put a free [FRED API key](https://fred.stlouisfed.org/docs/api/api_key.html) in a gitignored `.env` file
(`FRED_API_KEY=…`, no quotes). `make init` then also syncs from FRED, and `make refresh-snapshot` rewrites
`data/adp_vintages.csv` from the API.

## Data

- **Series:** FRED `ADPMNUSNERSA`, total nonfarm private payroll employment, persons, seasonally adjusted.
  The tool stores levels exactly as published and derives changes, first prints and revisions itself.
- **Every vintage:** the snapshot holds all 1,302 values ALFRED has recorded across 50 publication dates,
  each with the date it was first published. "September's number" is ambiguous (first print or revised?),
  so forecasts are judged against the **first print**, and backtests never see a value published later
  than the moment being simulated.
- **Coverage starts 2022-08-31.** ADP relaunched the report with a new methodology that day. The 2010 – Jul
  2022 history first appears in that vintage as a backcast, so those months never had a real-time first
  print. The full history has since been restated several times (QCEW reweighting): 2023-02-01,
  2024-01-31, 2025-02-05, 2025-03-05, 2025-04-02 and 2026-02-04.
- **Citation (required by FRED for this copyrighted series):** Automatic Data Processing, Inc., Total
  Nonfarm Private Payroll Employment [ADPMNUSNERSA], retrieved from FRED, Federal Reserve Bank of St.
  Louis. The same citation heads `data/adp_vintages.csv`.

## Approach and trade-offs

- **Adding a plug-in is one class plus one `@Bean`.** `ma` (MA(3), equal-weighted mean of the last
  three changes) and the `rolling-60` fit window were added this way, with no edits outside `config/`.
- **Two models, one interface.** `naive` (next change = last change) is the baseline every score is
  measured against. `ar` is AR(3) on monthly changes, fitted by ordinary least squares on every call. Its
  forecast decomposes exactly into `intercept + Σ weight × lag`, which is the explanation. Richer models
  were cut on purpose: they would be harder to explain, and the accuracy method below would be needed to
  justify them anyway.
- **Explanations sum exactly.** Each term is rounded to whole jobs and the forecast is defined as their
  sum, so the rows always add up; display rounding to thousands uses largest-remainder rounding to keep it
  that way.
- **COVID is excluded from the fit by default** (Mar 2020 – Dec 2021, a structural break). This was decided
  on domain grounds before seeing any results; the backtest shows the sensitivity.
- **The AR order is fixed at 3** and never tuned on the backtest; tuning it there would turn the test set
  into a training set.
- **Architecture:** ports and adapters. The domain, forecasting and evaluation code is plain Java with no
  framework; Spring Boot does dependency injection at the edges only (enforced by an ArchUnit test).
  Adding a model or window is one class plus one `@Bean`. Storage is SQLite behind a repository interface.
  The pattern map in `CLAUDE.md` lists each pattern used and the ones deliberately not used.

## How accuracy is measured

A **walk-forward backtest**. For each target month M:

1. Take the history exactly as it stood the day before M was first published (`asOf`).
2. Apply the fit window, let each model fit itself, and forecast M.
3. Compare with M's **first print**.

- **Targets** are months whose predecessor was the newest month available at that moment: Sep 2022 – Sep
  2026, **49 months**. Months published together in the relaunch backcast are not targets.
- **Like for like:** every model and every fit window in a table is scored on the same months; months any
  of them couldn't forecast are counted as skipped.
- **Metrics:** mean absolute error (MAE, thousands of jobs) and skill = `1 − MAE_model / MAE_naive`
  (positive = better than naive).
- **Sampling noise:** with 49 months, an MAE carries roughly ±15% sampling noise, so small differences
  between models or windows are not evidence of anything.
- **Secondary backtest (biased):** the same loop on the latest revised values, Feb 2010 – Sep 2026. It has
  ~4× more months and includes COVID, but both the fit and the "truth" are revised data, so it is used
  only for sensitivity checks, never as the headline.

## Pre-registered interpretation

Fixed before the backtest was run, so the results can't be read to fit them:

1. **The default window stays `exclude-covid` whatever the results show.** It was chosen on domain grounds;
   switching to whichever window scored better would be model selection on the test set.
2. **Differences inside the noise band count as no difference:** an MAE gap under ~15% or a skill gap under
   ~0.1 is reported as "no measurable difference", not as a win.
3. **AR vs. naive:** if AR wins with near-zero weights, it is only exploiting the mean; if it wins with
   meaningful weights, report it with `n` and the noise caveat; if it doesn't win, say so — the tool then
   tells users itself (`--explain` prints a note when skill ≤ 0).

## Results

Snapshot retrieved 2026-10-22; newest vintage 2026-09-30.

### Headline: real-time first prints

| fit window | n | MAE | skill vs naive |
|---|---|---|---|
| all | 49 | 61K | +0.08 |
| exclude-covid (default) | 49 | 62K | +0.07 |
| rolling-60 | 49 | 66K | −0.00 |
| *naive baseline* | 49 | 66K | — |

No targets were skipped. `rolling-60` (the last five years only, added in P7 with its length fixed before
this run) is the weakest fit window, 4–5K behind the others: still inside the noise band, and consistent
with the bias–variance trade-off in DESIGN.md D6 (fewer rows, noisier coefficients).

**Does AR(3) beat naive?** Nominally, by 7–8% of MAE; under rule 2 that is **no measurable difference**.
AR is not merely its intercept: it puts a weight of 0.71 on last month's change, so it behaves like a
damped naive forecast pulled toward a long-run level. That is still a useful finding: over these 49 months,
ADP's own short history adds little beyond the last print, which points to other information (BLS
payrolls, jobless claims) rather than more elaborate models of the same series.

**Do the results support the `exclude-covid` default?** They neither support nor contradict it: 61K vs.
62K, skill +0.08 vs. +0.07, well inside the noise band. Per rule 1 the default stays. The window does
change the model, though: fitted on all history, AR's weights are 1.16 / −0.35 / −0.01 instead of
0.71 / −0.14 / 0.10, and this month's forecast is +120K instead of +112K. The COVID swings reshape the
coefficients without making the forecasts measurably better or worse over 2022–2026.

### Secondary: latest vintage (biased)

Scoring window fixed, both fit windows:

| scored on | fit window | n | MAE | skill |
|---|---|---|---|---|
| all | all | 167 | 56K | −0.01 |
| all | exclude-covid | 167 | 55K | +0.01 |
| exclude-covid | all | 166 | 56K | +0.00 |
| exclude-covid | exclude-covid | 166 | 55K | +0.02 |

With `fit=exclude-covid` in the table, COVID-period months can't be forecast and drop out for every row
(33 and 12 skipped), so these rows say nothing about scoring COVID. Fit windows again make no measurable
difference, and AR vs. naive is a wash.

**Should COVID months be scored?** Answered by the `fit=all` row alone:

| scored on | n | MAE | skill |
|---|---|---|---|
| all | 192 | 85K | −0.07 |
| exclude-covid | 170 | 60K | −0.03 |

The 22 COVID-era months raise MAE from 60K to 85K. Scoring them would let one extraordinary episode
dominate any accuracy number, which is why the headline (2022 onward) is the right yardstick and COVID is
best reported separately.

### AR vs. a moving-average baseline

`ma` is MA(3): the same three lags AR uses, equally weighted, nothing fitted. Measuring AR against it asks
whether fitting the weights and an intercept beats plain averaging of the same information.

```
make run ARGS="backtest --baseline ma"     # AR(3) scored against MA(3)
make run ARGS="backtest --model ma"        # MA(3) scored against naive
```

| comparison (real-time, n=49) | fit window | MAE | skill |
|---|---|---|---|
| AR(3) vs MA(3) | all | 61K vs 59K | −0.03 |
| AR(3) vs MA(3) | exclude-covid | 62K vs 59K | −0.04 |
| MA(3) vs naive | either | 59K vs 66K | +0.11 |

**AR vs. MA: no measurable difference**, and nominally MA is slightly better. Fitting the weights and the
intercept buys nothing over averaging the last three prints. This sharpens the AR-vs-naive finding: what
helps against naive is smoothing the last few prints, not the regression. AR stays the default because it
was chosen before these results (rule 1's logic applies to models too) and because its explanation shows
*why* (the weights and the pull toward a long-run level); this table is the evidence a user needs to
discount it.

**MA vs. naive** sits at +0.11, right at the edge of the ~0.1 band: too close to call with the heuristic,
which is exactly the case the Diebold–Mariano test under "Next week" would settle. Whether AR is more than
its intercept specifically needs the constant-mean baseline, also listed there.

## Next week

- **Diebold–Mariano test** for forecast accuracy, replacing the ±15% / 0.1 noise-band heuristic with a
  proper test of whether two models' errors differ.
- **Exogenous predictors** (BLS nonfarm payrolls, jobless claims) behind a feature provider — the results
  suggest this is where accuracy would come from.
- **Prediction intervals**, with their coverage evaluated in the same backtest.
- **Ensemble** weighted by recent error.
- **Constant-mean baseline**, to answer directly whether AR is more than its intercept (MA(3) answers a
  narrower question: fitted weights vs. equal weights on the same lags).
- **Selecting `p`** on a held-out earlier window, never on the reported one.
- Postgres / S3 repositories, Flyway migrations, JSON output, a small web UI.

## Development

```
make test                 # all tests, in Docker
make lint                 # Spotless (google-java-format) check
make format               # apply formatting
make refresh-snapshot     # maintainer: rewrite data/adp_vintages.csv from FRED (needs FRED_API_KEY)
```

Layout: `domain/` (values, point-in-time history), `forecast/` (models, windows), `evaluation/`
(backtester, metrics), `port/` + `adapter/` (CSV, FRED, SQLite), `cli/` (commands, rendering), `config/`
(the only place concrete classes are chosen).
