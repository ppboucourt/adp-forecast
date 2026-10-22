# Build prompt pack

Structured prompts distilled from the Phase 1 design session (`docs/DESIGN.md`). Run each one in its own
Claude Code session, in order. After each session, add to `PROMPTS.md`: the prompt as sent (including any
edits), the raw session log, and a note on what you used, edited or rejected and why. Commit once per phase.

Every prompt has the same shape:
**Context · Goal · Patterns (and why) · Constraints · Open questions — argue before coding · Acceptance · Out of scope**

The open questions are there on purpose: the AI should challenge the design before building it, and you
record whether you accepted its argument.

---

## P0 — Scaffold

**Context:** Empty repo with `CLAUDE.md` and `docs/DESIGN.md`. Read both.

**Goal:** A buildable Spring Boot 4.1.x / Java 25 / Gradle (Kotlin DSL) project with the package layout from
DESIGN.md D7, a Dockerfile, and a Makefile with `init`, `run`, `test`, `lint`, `refresh-snapshot` targets.
`make run ARGS="--help"` prints picocli help.

**Patterns (and why):**
- Command → picocli root command with empty `history`, `forecast`, `backtest`, `sync` subcommands; each
  user action is one object.
- DI container → Spring Boot, used only for wiring and configuration.

**Constraints:**
- Dependencies: `spring-boot-starter`, `spring-boot-starter-jdbc`, configuration processor,
  `picocli-spring-boot-starter`, `sqlite-jdbc`, `commons-math3`. Test: JUnit 5, AssertJ.
- `spring.main.web-application-type=none`, banner off, logging quiet by default.
- Multi-stage Dockerfile (JDK build stage, JRE runtime stage). DB on a named volume mounted at `/data`.
- One lint tool (Spotless or Checkstyle — pick one and justify).

**Open questions — argue before coding:**
1. Is Spring Boot over-engineering for a CLI of ~15 objects? Give the strongest case for manual
   constructor-injection wiring, then state what we keep by using Spring anyway. (Decision stands; I want the
   trade-off written down.)
2. Spotless or Checkstyle for a solo take-home?

**Acceptance:** `make init` builds the image; `make run ARGS="--help"` lists the four subcommands;
`make test` and `make lint` pass on an empty test suite.

**Out of scope:** any domain logic.

---

## P1 — Domain

**Context:** Scaffold exists. Read CLAUDE.md, DESIGN.md D1 and D9.

**Goal:** `ReferenceMonth`, `JobsChange`, `Vintage`/observation record, `Print`, `PrintHistory` with
`asOf(LocalDate)`, `latest()`, `firstPrintChange(ReferenceMonth)`, and `changes()` (headline series).

**Patterns (and why):**
- Value Object → `ReferenceMonth`, `JobsChange`: immutable, validated, equality by value.
- Entity → `Print`: identity is the reference month; holds its revision trail.

**Constraints:** Pure Java, no Spring, no third-party libraries. Records with compact-constructor validation.

**Open questions — argue before coding:**
1. Should `PrintHistory` be a class with rich behavior or a record plus a separate service? Which keeps
   `asOf` easiest to test?
2. Do we need `Print` as an entity at all, or is `PrintHistory` over raw observations enough? Argue for the
   smaller model if it holds up.

**Acceptance (tests first):**
- First-print change for M uses both levels from the vintage that first published M (test with a revised M−1).
- `asOf(date)` returns the latest vintage on or before `date` for every month and excludes later ones.
- Invalid months / null levels are rejected.

**Out of scope:** persistence, sources, forecasting.

---

## P2 — Ports and adapters

**Context:** Domain exists. Read DESIGN.md D2 and D9.

**Goal:** `PrintSource` and `PrintRepository` ports; `CsvSource`, `FredSource`, `JdbcPrintRepository`;
`schema.sql`; the `sync` command (`--source csv|fred`, `--export <path>`).

**Patterns (and why):**
- Repository → `PrintRepository`: the core never sees SQL; Postgres is a config change, S3 a new class.
- Adapter → `PrintSource` implementations translate external formats into domain observations.

**Constraints:**
- Inserts are `INSERT … ON CONFLICT DO NOTHING` (portable SQLite/Postgres). Vintages are immutable.
- `FredSource` uses `java.net.http.HttpClient`, reads `FRED_API_KEY` from config, fetches all vintages of
  `ADPMNUSNERSA` via the ALFRED real-time parameters. Verify the exact parameters against FRED API docs.
- Source selection by name via Spring (`Map<String, PrintSource>`).

**Open questions — argue before coding:**
1. How far back do ALFRED vintages go for `ADPMNUSNERSA`, and how did ADP's 2022 methodology relaunch affect
   them? If coverage is short, what's the honest fallback for the backtest?
2. Should `sync` live in the CLI layer or behind a small application service? Which is easier to test?

**Acceptance:**
- `JdbcPrintRepository` round-trips observations on a temp SQLite file; re-inserting is a no-op.
- `CsvSource` parses a fixture; `FredSource` parses a recorded JSON response (no network in tests).
- `make run ARGS="sync --source csv"` reports the number of new vintages; running it twice reports 0.

**Out of scope:** caching, Postgres, S3.

---

## P3 — Forecast layer

**Context:** Domain, ports and adapters exist. Read DESIGN.md D4 and D6.

**Goal:** `Forecaster` strategy with `NaiveForecaster` and `AutoregressiveForecaster(p)`; `Forecast` and
`Explanation`; `HistoryWindow` with `AllHistory`, `Excluding(MonthRange)` and `andThen`.

**Patterns (and why):**
- Strategy → `Forecaster`: models are interchangeable; the CLI selects by name.
- Strategy + composition → `HistoryWindow.andThen()`: windows combine without new classes.
- Value Object → `Forecast`, `Explanation`.

**Constraints:** No Spring in this package. OLS via `commons-math3`. `p` comes from configuration (default 3).

**Open questions — argue before coding:**
1. With only two models, is a `Forecaster` interface over-engineering? Give the strongest case against, then decide.
2. Should `Explanation` be model-specific (sealed interface) or one generic list of named contributions?

**Acceptance (tests first):**
- AR recovers known coefficients (± tolerance) on a synthetic AR(3) series.
- Explanation contributions sum exactly to the point forecast.
- `Excluding(COVID)` removes exactly Mar 2020 – Dec 2021; `andThen` composes in order.

**Out of scope:** moving average, ensembles, prediction intervals.

---

## P4 — Evaluation

**Context:** Forecasters exist. Read DESIGN.md D5 and D6.

**Goal:** `Backtester` (walk-forward, point-in-time, scored against first prints), `BacktestReport`,
`AccuracyMetric` with `MeanAbsoluteError` and `SkillVsNaive`; multi-window comparison.

**Patterns (and why):** Strategy → `AccuracyMetric`; `Backtester` receives forecaster, window and metrics
by constructor.

**Constraints:** For each test month, the forecaster receives only `history.asOf(dayBeforeRelease)` after
the window is applied. `p` is never tuned here.

**Open questions — argue before coding:**
1. Template Method backtester vs. composition — which is simpler here? (Pattern map says composition; push
   back if you disagree.)
2. Should the scoring window always match the fit window, or be independent?

**Acceptance (tests first):**
- **Look-ahead guard:** a spy forecaster asserts it never receives an observation with a vintage date on or
  after the release it's predicting.
- Naive skill vs. naive = 0.
- MAE on a hand-computed example matches.

**Out of scope:** model selection, intervals.

---

## P5 — CLI and wiring

**Context:** All layers exist. Read DESIGN.md D7 and D8.

**Goal:** Implement `history` (`--last N`, `--first-print`), `forecast` (`--model`, `--window`,
`--explain`), `backtest` (`--model`, repeatable `--window`, comparison table). `@Configuration` classes
register all beans.

**Patterns (and why):** Command → one picocli class per subcommand; DI → commands receive
`Map<String, Forecaster>` / `Map<String, HistoryWindow>`, so a new plug-in is one `@Bean`.

**Constraints:** Output exactly the shape in D8. Warn when the newest vintage is older than ~35 days.
Unknown model/window names list the valid ones.

**Open questions — argue before coding:**
1. Is a `Renderer` interface warranted yet? (Pattern map says no.)
2. Should `forecast` default to AR or naive, and why?

**Acceptance:** One `@SpringBootTest` runs `forecast --explain` against the bundled snapshot and checks the
output shape. `make test` and `make lint` pass.

**Out of scope:** JSON output, web UI.

---

## P6 — Data snapshot and README

**Context:** Working tool. Read DESIGN.md.

**Goal:** Run `make refresh-snapshot`, commit `data/adp_vintages.csv`, run the backtest comparison, and write
`README.md`: how to run, approach and trade-offs, accuracy method and **actual results**, next week.

**Open questions — argue before coding:**
1. Do the results support the `exclude-covid` default? Report honestly if they don't.
2. Does AR(3) actually beat naive? If not, say so and explain why that's still a useful finding.

**Acceptance:** A fresh clone runs `make init && make run ARGS="forecast --explain"` with no API key.

---

## P7 — Extension demo

**Goal:** Add `MovingAverageForecaster(k)` and `Rolling(months)` window, each as one new class plus one
`@Bean` method.

**Acceptance:** `git diff` shows no edits to existing classes outside `config/`. Both appear in
`backtest` output.

---

## P8 — Self-review

**Prompt:** Review the repository against `CLAUDE.md` (architecture rules and pattern map), `docs/DESIGN.md`
and the original brief. List violations, gaps and over-engineering, most important first. Don't fix anything yet.

Record which findings you fixed and which you rejected, and why.
