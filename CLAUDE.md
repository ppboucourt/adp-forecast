# ADP Forecast — rules for AI sessions

Read `docs/DESIGN.md` before changing anything. Decisions there are settled; if a change would contradict
one, stop and say so instead of working around it.

## Architecture rules

- Ports and adapters. Dependencies point inward: `cli`, `adapter`, `config` → `evaluation`, `forecast` → `domain`.
- **No Spring annotations or imports in `domain/`, `forecast/`, `evaluation/`, `port/`.** Plain Java, constructor
  injection only. Spring lives in `config/`, `adapter/`, `cli/`.
- Concrete classes are chosen only in `config/` (`@Bean` methods).
- `domain/` has no I/O and no third-party libraries.
- Value objects are Java records, validated in the compact constructor.
- Stored data is raw vintages (levels). Changes, first prints and revisions are derived in the domain.
- Nothing in `forecast/` or `evaluation/` may read data published after the as-of date.

## Pattern map

| Area | Pattern | Where | Deliberately not used |
|---|---|---|---|
| Domain | Value Object | `ReferenceMonth`, `JobsChange`, `Observation`; `PrintHistory.vintagesOf()` for revision trails | `Print` entity |
| Data access | Repository (port) | `PrintRepository` → `JdbcPrintRepository` | Spring Data JPA |
| Data sources | Adapter | `PrintSource` → `CsvSource`, `FredSource`; `SnapshotWriter` → `CsvSnapshotWriter` (one shared `CsvFormat`) | Caching decorator |
| Forecasting | Strategy | `Forecaster` → `NaiveForecaster`, `AutoregressiveForecaster` | Ensemble / Composite |
| Windows | Strategy + composition | `HistoryWindow.andThen()` | Specification pattern |
| Evaluation | Strategy | `AccuracyMetric` → `MeanAbsoluteError`, `SkillVsNaive` | Template Method backtester |
| CLI | Command | picocli subcommands | Renderer interface (one format) |
| Wiring | DI container | Spring `@Configuration` at the edges | Annotations in the core |
| Architecture rules | Fitness function (test) | `ArchitectureTest` (ArchUnit) | Checkstyle import rules |

Don't add a pattern that isn't in this table without arguing for it first.

## Commands

All tooling runs in Docker through the Makefile:

```
make init                 # build image + seed DB from data/adp_vintages.csv
make run ARGS="forecast --explain"
make test
make lint
make refresh-snapshot     # needs FRED_API_KEY; rewrites data/adp_vintages.csv
```

A change is done only when `make test` and `make lint` both pass.

## Style

- Write tests first for domain, forecast and evaluation code.
- Comments: brief and technical, explain what the code does now. No ticket references.
- Keep the scope to the current phase prompt; list anything else as a follow-up instead of building it.
