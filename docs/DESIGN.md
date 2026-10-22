# ADP Forecast — Design

Decision log from the design session (Phase 1). Each entry: what we decided, why, and what we cut.

## Problem

A CLI that tracks the monthly ADP National Employment Report and forecasts the next print. A user can:

1. See historical numbers
2. See the prediction for next month
3. Understand why the tool predicted what it did

Plus: measure forecast accuracy, justify the method, show results. Graded on structure, composition and
defending choices — not on model sophistication.

**Non-goals:** low latency (batch CLI), web UI, model sophistication beyond what is explainable.

## Decisions

### D1 — A print is versioned data, not one number per month

ADP revises the prior month on every release. We store every published **vintage** and derive the rest.

- `ReferenceMonth`, `JobsChange`, `Observation` — value objects (records, validated, immutable).
  `Observation` is one stored row: reference month, vintage date, level. The level is a primitive `long`, so
  null is impossible by type; non-positive levels are rejected.
- `PrintHistory` — immutable class over observations. A month's level at date D is its latest vintage ≤ D.
  `asOf(date)` drops vintages published after `date`; `vintagesOf(month)` returns one month's revision trail.

**Why:** "September's number" is ambiguous (first print vs. revised). Forecasts should be judged against the
first print (what the market saw), and backtests must not see revisions published later (look-ahead bias).

**Subtlety:** the first-print change for month M is `level(M) − level(M−1)` with **both levels read as of the
same vintage date V** — the one that first published M, i.e. `asOf(V)`. M−1's level is its latest vintage
≤ V: a row dated exactly V if that release revised M−1, otherwise the earlier value carried forward (ALFRED
records a value only when it changes). Mixing vintages produces a number ADP never published.

**Cut (P1):** a `Print` entity per month. Its key behavior, the change, needs M−1 from the same vintage, so it
can't live on a single month; `PrintHistory` owns it and `vintagesOf` covers the revision trail.

### D2 — Data source: FRED/ALFRED behind an adapter, bundled CSV by default

- FRED series `ADPMNUSNERSA` — ADP total private payroll *level*, monthly, seasonally adjusted.
- ALFRED (same API, `realtime_*` / `vintage_dates` params) keeps every historical vintage.
- Headline change (+143K) is derived from the level in the domain, not in the adapter.
- `PrintSource` port → `CsvSource` (default, offline) and `FredSource` (opt-in, needs `FRED_API_KEY`).

**Why:** clone-and-run must work without an API key. Vintages give us first prints for free.

**Cut:** extra predictors (BLS NFP, jobless claims). Listed under "next week".

**Verified during build (P2):**

- ALFRED vintages for `ADPMNUSNERSA` start **2022-08-31**, the relaunch of the NER (new methodology built with
  the Stanford Digital Economy Lab). Levels for 2010 – Jul 2022 first appear in that vintage as a backcast;
  they never had a real-time first print. The pre-2022 ADP report was a different product (a forecast of
  BLS) and is not spliced on.
- Every February the annual QCEW reweighting restates the whole history back to 2010 (Feb 2026: level about
  −2.5M). `asOf(V)` handles it: a January's first print reads the benchmarked December from the same vintage.
- API: `fred/series/observations` with `realtime_start=1776-07-04`, `realtime_end=9999-12-31` (both default
  to today), `output_type=1`. Each row's `realtime_start` is our `vintage_date`; rows appear only when a
  value changes.

**Backtest consequence:**

- **Headline:** real-time first-print backtest, targets Sep 2022 onward (~49 to Sep 2026; an estimate until
  `FredSource` runs against the live API), no look-ahead. The README states that its MAE carries about ±15%
  sampling noise.
- **Secondary, labelled biased:** latest-vintage backtest over 2011–2026 (revised, benchmarked data is both
  the fit and the truth). Used for the window sensitivity check and the COVID scoring question, since the
  real-time test window contains no COVID months.

### D3 — Java 25, Gradle, Spring Boot for DI only, Docker

- Java 25 (records, sealed interfaces, pattern matching), Gradle Kotlin DSL, Spring Boot 4.1.x.
- Spring is used **for dependency injection and configuration only**, and only at the edges
  (`config/`, `adapter/`, `cli/`). `domain/`, `forecast/`, `evaluation/` are plain Java.
- picocli (with `picocli-spring-boot-starter`) for subcommands, `JdbcClient` + `sqlite-jdbc`,
  `java.net.http.HttpClient` for FRED, `commons-math3` for OLS.
- Docker multi-stage build; Makefile wraps everything.

**Dead end, kept for the record:** we went back and forth between Spring Boot and manual "pure DI" with a
composition root. Manual wiring is simpler and starts faster; Spring was chosen so wiring isn't maintained by
hand and plug-ins register themselves (`Map<String, Forecaster>` injection). The core stays framework-free, so
the choice is reversible by rewriting only `config/`.

**Cut:** Spring Web, Spring Data JPA, Flyway, Spring Shell.

### D4 — Forecasting: Strategy, two models, explainability in the return type

```java
interface Forecaster { String name(); Forecast forecast(PrintHistory asOf); }
record Forecast(ReferenceMonth target, JobsChange point, Explanation explanation) {}
```

- `NaiveForecaster` — next = last print. The baseline; accuracy numbers are meaningless without it.
- `AutoregressiveForecaster(p=3)` — AR(p) by OLS. Forecast decomposes as `intercept + Σ coef × lag`, which is
  the explanation.

**Why only two:** build exactly what was asked; prove extensibility with a final small commit instead.

**Explanation (P3):** a list of terms in two shapes, not one type per model: `Constant(name, amount)` (the
intercept) and `Weighted(weight, month, input)` (amount = `round(weight × input)` in whole jobs). Naive is one
`Weighted(1.0, …)`; AR is the intercept plus p `Weighted` lags. The renderer handles two shapes once, so a new
linear model needs no CLI change. The point forecast is defined as the sum of the term amounts, and
`Forecast` rejects any point that differs, so contributions sum exactly by construction.

**Missing months (P3):** a model that lacks the months it needs throws `InsufficientHistoryException` (e.g. AR
needs changes for all p months before the target). D4's signature is unchanged; callers decide whether it is
an error (CLI) or a skipped month (backtest).

**Cut:** moving average, ensemble, prediction intervals (an interval would also need its coverage evaluated).

### D5 — Accuracy: walk-forward backtest against first prints

For each month M in the test window: fit on the point-in-time history before M's release, forecast, compare
with M's **first print**.

- **MAE** in thousands of jobs — readable by anyone.
- **Skill vs. naive** = `1 − MAE_model / MAE_naive` — positive means the model beats the baseline.
- `AccuracyMetric` is a strategy; only these two ship.
- AR order `p` is fixed up front (not tuned on the backtest), so the reported score stays honest.
- **Compare like with like (P4):** score only months where every compared model produced a forecast, and
  report how many were skipped. After an excluded window, AR cannot forecast the first p months while naive
  can; scoring them for naive alone would make the two MAEs incomparable.

**Backtest mechanics (P4):**

- One `Backtester(model, baseline, fitWindow, metrics)`; the baseline is injected (naive by default). Each
  scored month is a row `(target, firstPrint, modelPoint, baselinePoint)`; `AccuracyMetric` computes over rows.
- **Targets:** M is a target if, the day before M's first release, the latest month held is M−1. Months first
  published together (the 2022 relaunch backcast) are never targets, so they don't count as skipped. A
  forecast for any month other than M is an error.
- **Basis:** `REAL_TIME` (headline) runs on real vintages and rejects a `latestOnly()` history.
  `LATEST_VINTAGE` (secondary, biased) runs the same loop on `PrintHistory.latestOnly()`: each month's latest
  level stamped with a *synthetic* release date (1st of the following month).
- **Windows are independent.** In a comparison table the scoring window is fixed and only the fit window
  varies, and every cell is scored on the same months: those every cell's model and baseline forecast. A
  cell whose fit window removed the month before the target can't forecast it. Each report line shows `n`
  (scored months) next to MAE and skill, plus the skipped count.
- **Consequence for the 2×2:** `fit=exclude-covid` cannot forecast COVID-period targets, so in a table that
  includes it those months drop out for every cell (visible in the skipped count). The COVID scoring question
  is answered by the `fit=all` row under both scoring windows.

**Correction recorded:** the backtest *evaluates*; it does not train. The model fits itself (OLS) inside each
`forecast()` call. Using backtest scores to pick `p` or a window would be model selection and would bias the
reported score.

### D6 — History window is a pluggable, composable strategy

```java
interface HistoryWindow { PrintHistory apply(PrintHistory h); default HistoryWindow andThen(HistoryWindow n) {...} }
```

- Ships `AllHistory` and `Excluding(MonthRange)`. Default: `exclude-covid` (Mar 2020 – Dec 2021).
- Two knobs: the **fit window** changes the forecast; the **scoring window** changes only the reported accuracy.
- `adp backtest --window all --window exclude-covid` prints a comparison table.

**Why:** COVID swings (millions of jobs) dominate both the OLS fit and MAE. Default chosen on domain grounds
(structural break), not because it scored best; the comparison table shows the sensitivity.

**Bias–variance:** long windows give stable coefficients but mix old regimes; short windows adapt but are noisy.

**Gaps (P3):** windows remove *levels*, so `exclude-covid` drops Mar 2020 – Dec 2021 and the change series
loses Mar 2020 – **Jan 2022** (Jan 2022's change needs Dec 2021's level): one ordinary month lost, accepted to
keep `HistoryWindow` on `PrintHistory`. AR builds a regression row only when the target and all p lag months
have a change, so it never pairs months across the gap; Feb – Apr 2022 drop out as targets.

### D7 — Architecture: ports and adapters

```
domain/      ReferenceMonth, MonthRange, JobsChange, Observation, PrintHistory, Forecast, Explanation   (no I/O, no libs)
forecast/    Forecaster, NaiveForecaster, AutoregressiveForecaster, HistoryWindow + impls
evaluation/  Backtester, BacktestReport, ScoredMonth, Basis, NamedWindow, AccuracyMetric, MeanAbsoluteError, SkillVsNaive
port/        PrintSource, PrintRepository, SnapshotWriter       (interfaces owned by the core)
adapter/     CsvSource, CsvSnapshotWriter, FredSource, JdbcPrintRepository   (all I/O)
cli/         picocli commands: history, forecast, backtest, sync + text rendering
config/      Spring @Configuration — the only place concrete classes are chosen
```

Dependencies point inward. **Not abstracted on purpose:** rendering (one text format; no `Renderer` interface
until a second format exists).

### D8 — Explainability UX: `adp forecast --explain`

```
Oct 2026 forecast (AR(3), window=exclude-covid): +118K
  intercept                    +42K
  0.45 × Sep (+143K)           +64K
  0.21 × Aug (+ 54K)           +11K
  0.04 × Jul (+ 23K)            +1K
  ─────────────────────────────────
  forecast                    +118K
  backtest MAE 61K | skill vs naive +0.18
```

(Illustrative numbers.) The last line answers "how much should I trust it". The backtest runs live —
milliseconds on ~15 years of monthly data, nothing cached.

**Next month** = month after the latest reference month stored. Warn if the newest vintage is > ~35 days old.

**Rounding (P5 follow-up):** terms sum exactly in whole jobs, but rounding each row to K independently can
make the displayed rows miss the displayed total by a few K. The renderer uses largest-remainder rounding so
the displayed rows add up to the displayed total.

### D9 — Storage and data lifecycle

```sql
CREATE TABLE observation (
  reference_month TEXT    NOT NULL,
  vintage_date    TEXT    NOT NULL,
  level           INTEGER NOT NULL,
  PRIMARY KEY (reference_month, vintage_date)
);
```

- Store levels exactly as published; derive changes, first prints and revisions in the domain.
- Vintages are immutable → inserts use `ON CONFLICT DO NOTHING` (portable across SQLite and Postgres).
- `sync` is explicit; `history`, `forecast`, `backtest` are read-only and reproducible offline.
- `PrintRepository` is **our** port, not Spring Data. Postgres = change `spring.datasource.url`.
  S3 = one new `S3PrintRepository` selected by `@ConditionalOnProperty(adp.storage=s3)`. Only SQLite ships.

| Make target | Behavior |
|---|---|
| `make init` | Build image, seed DB from committed `data/adp_vintages.csv`; also sync from FRED if `FRED_API_KEY` is set |
| `make refresh-snapshot` | Maintainer only: pull from FRED and rewrite the committed CSV |
| `make run ARGS="…"` | Run any subcommand |
| `make test`, `make lint` | Run inside Docker |

### D10 — Testing: pyramid aligned with layers

| Layer | Tests |
|---|---|
| domain | Same-vintage first-print change; `asOf` vintage selection; value-object validation |
| forecast | AR recovers known coefficients on a synthetic series; **explanation contributions sum to the forecast**; `Excluding` drops the right months |
| evaluation | **Look-ahead guard: the model never receives data published after the as-of date**; naive skill = 0 |
| adapter | `JdbcPrintRepository` on a temp SQLite file; `CsvSource` fixture; `FredSource` on recorded JSON (no network) |
| cli | Commands run through picocli with fakes; one `@SpringBootTest` smoke test of `forecast --explain` on a synthetic test series (P6 adds one on the real snapshot) |

JUnit 5 + AssertJ. **Cut:** mutation testing, property-based tests, Testcontainers Postgres.

### D11 — AI workflow

- Phase 1 (this design) is logged raw in PROMPTS.md.
- Build phases are driven by the structured prompts in `docs/PROMPTS-PLAN.md`; each prompt, its raw session
  log, and a note (used / edited / rejected and why) go into PROMPTS.md.
- `CLAUDE.md` holds the architecture rules and pattern map every session must follow.
- One commit per phase, referencing the prompt.

## Extension demo (final commits)

- `MovingAverageForecaster` — one class + one `@Bean`, no edits elsewhere.
- `Rolling(months)` window — same.

## Next week (README material)

Ensemble weighted by recent error · prediction intervals with coverage evaluation · exogenous predictors (BLS,
claims) via a feature provider · Postgres / S3 repositories · Flyway · JSON output / web UI · selecting `p`
on a held-out earlier window.
