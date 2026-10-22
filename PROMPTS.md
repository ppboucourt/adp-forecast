# PROMPTS — AI workflow log

How this repository was built with an AI assistant (Claude Code). Per DESIGN.md D11, each phase records
the prompt as sent (with any edits), the raw session log, and a note on what was used, edited or rejected
and why. The structured prompts are in [`docs/PROMPTS-PLAN.md`](docs/PROMPTS-PLAN.md); every one starts by
asking the AI to argue its open questions before coding, and the notes below record those decisions.

> **Draft status:** the notes were drafted by the assistant at the end of the build and reviewed by me.
> Raw logs are exported separately (see each phase).

## Phase 1 — Design

- **Raw log:** [`docs/sessions/01-design.txt`](docs/sessions/01-design.txt)
- **Output:** `docs/DESIGN.md` (decisions D1–D11), `CLAUDE.md` (rules and pattern map),
  `docs/PROMPTS-PLAN.md` (P0–P8).

## Build phases P0–P8

All build phases ran in one Claude Code session.
**Raw log:** `docs/sessions/02-build.txt` *(to export and add)*.

---

### P0 — Scaffold

**Prompt as sent:**
> The design for this project is in docs/ (also at https://github.com/ppboucourt/adp-forecast/tree/main/docs).
> Read CLAUDE.md, docs/DESIGN.md, and the P0 section of docs/PROMPTS-PLAN.md, answer P0's open questions
> first, then implement P0. Stop when its acceptance criteria pass: make init builds the image, make run
> ARGS="--help" lists the four subcommands, and make test and make lint pass.

**Note:**
- **Used:** the AI's written trade-off for Spring vs. manual wiring (strongest case for manual wiring:
  ~15 constructors, compile-time errors, ~100 ms startup; kept: `Map<String, Forecaster>` plug-in
  registration, externalised config, conditional adapters, reversibility because Spring is confined to
  the edges). Spotless (google-java-format) over Checkstyle: auto-fixable, no rule file to curate.
- **Edited:** google-java-format pinned to 1.36.1 after 1.37.0 crashed inside Spotless 8.10.4 (found by
  running it, not assumed). `make init` only built the image (no data existed yet).
- **Proposed by the AI, accepted later:** an ArchUnit test for "no Spring in the core" (added in P1).

### P1 — Domain

**Prompt as sent:**
> Read CLAUDE.md, docs/DESIGN.md (D1 and D9), and the "P1 — Domain" section of docs/PROMPTS-PLAN.md.
> Answer P1's open questions first and wait for my decision before coding. Then implement P1 test-first.
>
> Also add the ArchUnit test proposed in P0: the build must fail if domain/, forecast/, evaluation/ or
> port/ import anything from Spring. Add a row for it to the pattern map in CLAUDE.md.
>
> Stop when these pass: [first-print same-vintage rule; asOf; invalid months and null levels rejected;
> the ArchUnit test; make test and make lint]

**Decision message:**
> Q1: agreed — rich, immutable final class. Q2: (a), drop Print. Update D1 and the CLAUDE.md pattern map
> (Observation joins the value objects; vintagesOf replaces the revision-trail type). Same-vintage rule:
> agreed, use asOf(V). Update D1's wording to match, and test both cases: M−1 has no row at V, and M−1 was
> revised exactly at V. Level: primitive long. Replace the null-level criterion with "non-positive levels
> are rejected" and note that in D1. ArchUnit row: fine as proposed. Go ahead.

**Note:**
- **Used:** `PrintHistory` as a rich immutable class; dropped the `Print` entity (it can't own its key
  behavior, the change, which needs M−1 from the same vintage).
- **AI-raised, accepted:** D1's "same vintage" was ambiguous. ALFRED records a value only when it changes,
  so M−1 usually has no row dated exactly V; the rule became "both levels as of V".
- **Edited:** acceptance criterion "null levels rejected" became "non-positive levels rejected" (primitive
  `long` makes null impossible by type).

### P2 — Ports and adapters

**Prompt as sent:**
> Read P2 in PROMPTS-PLAN.md and answer its open questions first

**Decision message:**
> 1. Q1: yes. The headline is the real-time first-print backtest (~49 targets, no look-ahead), and the
> README says the MAE carries about ±15% sampling noise. Add the latest-vintage backtest (2011–2026) as a
> secondary, labelled biased, for the window sensitivity check and the COVID scoring question. Update D2's
> "To verify during build" note now with these findings.
> 2. Q2: sync stays in the CLI. SyncCommand calls the ports directly, with no application/ layer. Define
> the CSV format once in adapter/, shared by CsvSource and --export. Test it with fakes through
> CommandLine.execute, with no Spring context.
> 3. Fix the Observation date rule in P2: the vintage can't precede the first day of the reference month.
> Add regression tests for 2022-08-31 (relaunch) and 2023-08-30 (early release). Keep it as a separate
> commit inside P2 so the P1 fix is easy to see.
> 4. Blocker: (b). Use a small hand-made sample CSV for now so P2's acceptance step can run […]. The Q1
> vintage count stays an estimate (~49) until FredSource runs against the live API with a key.

**Note:**
- **Used:** the AI's research on ALFRED coverage (vintages start at the 2022-08-31 relaunch; annual QCEW
  restatements) and the resulting two-backtest plan, recorded in D2.
- **AI-found bug in P1, fixed in its own commit:** the `Observation` rule rejected vintages dated on or
  before month end, which would have rejected the very first real row (2022-08-31).
- **Added by the AI, flagged:** a `SnapshotWriter` port so `--export` doesn't make the CLI depend on an
  adapter; Jackson 3 for FRED JSON.
- **Edited:** the FRED fixture was hand-written to FRED's documented format (no key yet). This later
  missed the `.0` value format (P6) and was replaced by a real recording in P8.

### P3 — Forecast layer

**Prompt as sent:**
> Read P3 in PROMPTS-PLAN.md and answer its open questions first

**Decision messages:**
> 1. Q1: keep the interface — agreed for the three reasons you gave.

> 2. Q2: term-shape Explanation with point = Σ term amounts enforced in Forecast — agreed. For the P5
> display issue, plan largest-remainder rounding in the renderer so displayed rows add up to the displayed
> total; note it as a P5 follow-up.
> 3. COVID window: keep D6's level-based window, document the lost Jan 2022 change, and require consecutive
> lags — agreed. Add a test that the AR never builds a row spanning the gap. Also record for P4: AR can't
> forecast the first p months after the gap while naive can. The backtest must score only months where
> every compared model produced a forecast, and report how many were skipped.
> Other details: all fine. Go ahead.

**Note:**
- **Used:** keep `Forecaster` (spy for the look-ahead test, shared code path for model and baseline,
  plug-in map). `Explanation` as a list of two sealed term shapes (`Constant`, `Weighted`) rather than one
  type per model, so new linear models need no renderer change (proven in P7).
- **AI-raised, accepted:** level-based windows lose one extra change (Jan 2022); AR must not bridge gaps.
- **Added by me:** the gap test, and the "score only months every model forecast" rule for P4.

### P4 — Evaluation

**Prompt as sent:**
> Read P4 in PROMPTS-PLAN.md and answer its open questions first

**Decision message:**
> 1. Q1: composition with PrintHistory.latestOnly() — agreed. Document in its Javadoc that its release
> dates are synthetic (1st of the following month), and add a test that the real-time backtest never
> receives a latestOnly() history.
> 2. Q2: agreed — independent windows, scoring window fixed per table, every cell scored on the same
> months, skipped count reported, 2×2 for the secondary.
> 3. Target set: agreed, including the target == M assertion.
> 4. Metric shape: agreed with one change — name the column baselinePoint, and inject the baseline
> forecaster (default naive) instead of hard-wiring it.
> Every report line should show n (number of scored months) next to MAE and skill. Go ahead.

**Note:**
- **Used:** one composed `Backtester`; the latest-vintage backtest is the same loop on
  `PrintHistory.latestOnly()`, not a Template Method subclass.
- **Edited by me:** `baselinePoint` naming, injectable baseline, `n` on every line.
- **AI-found consequence, recorded in D5:** with `fit=exclude-covid` in a table, COVID-period targets drop
  out for every cell, so the COVID scoring question is answered by the `fit=all` row alone.
- **Verified:** three planted look-ahead bugs (including a same-day revision leak) each failed the guard.

### P5 — CLI and wiring

**Prompt as sent:**
> Read P5 in PROMPTS-PLAN.md and answer its open questions first

> speed things if possible

**Decision message (arrived mid-implementation):**
> 1. Q1: agreed — one concrete TextRenderer, no interface. 2. Q2: agreed — default AR, with the skill ≤ 0
> note. 3. Sample data: (b). I don't want fabricated numbers in data/ that could be mistaken for real ADP
> data, and that file is replaced by the real snapshot in P6. Make the fixture a seeded synthetic series
> (~60 months, a few revisions) in src/test/resources, clearly labelled as test data. Update P5's
> acceptance wording accordingly, and add a P6 follow-up: a second smoke test that runs forecast
> --explain against the real snapshot. 4. --basis and --score: agreed, with the output as proposed.
> 5. history columns: agreed. Your other details are fine. Go ahead.

**Note:**
- **Rejected:** after "speed things if possible" the AI went ahead with its own recommendation (a larger
  sample CSV in `data/`); I chose a labelled synthetic test fixture instead, and the AI reverted.
- **Edited:** P5's acceptance wording in PROMPTS-PLAN.md (synthetic series instead of the bundled
  snapshot); a real-snapshot smoke test was added to P6.
- **Caught in self-check:** a "valid basiss" error message that the AI's own test had enshrined.

### P6 — Data snapshot and README

**Prompt as sent:**
> Read P6 in PROMPTS-PLAN.md and answer its open questions first

**Decision messages:**
> FRED_API_KEY is now loaded from a gitignored .env via the Makefile — never echo it, and don't print it
> in any output.
> 1. Citation approach: agreed — citation in the CSV header and the README, snapshot committed.
> 2. Pre-registered rules: agreed, all three. Add a short "Pre-registered interpretation" section to the
> README stating them before the results. List the Diebold–Mariano test under "next week" as the proper
> replacement for the noise-band heuristic.
> 3. AR vs. naive interpretation: agreed. Since the baseline is injectable, plan the README so that after
> P7 it also reports AR's skill vs. the moving-average baseline — that answers "is AR more than its
> intercept?" directly. Proceed with the snapshot and P6.

**Note:**
- **AI-raised, accepted:** FRED tags the series "Copyrighted: Citation Required"; the snapshot writer
  emits the citation on every refresh.
- **Pre-registered before results** (agreed in the session before any backtest ran on real data): default
  window stays regardless; differences inside the noise band are "no measurable difference"; how to read
  each AR-vs-naive outcome. The git history alone can't show this order, which is why it is recorded here.
- **Found by running it:** the first live FRED call failed because FRED formats levels as `X.0`; fixed
  with a test.
- **Results:** AR(3) 61–62K vs naive 66K (n=49): no measurable difference; fit windows indistinguishable.

### Makefile comment fix (between P6 and P7)

> Fix: your Makefile comment still points to .env.example, which doesn't exist. […]

The AI's attempt to create `.env.example` had been denied, so the comment was rewritten instead.

### P7 — Extension demo

**Prompt as sent:**
> Read P7 in PROMPTS-PLAN.md and answer its open questions first

**Decision message:**
> 1. No env-var workaround. The no-edits rule is about plug-ins, not features. P7 = MA + Rolling only,
> zero edits outside config/. Then a separate commit adds --baseline to backtest (default naive), and the
> README's AR-vs-MA result uses that option. 2. k = 3. Constant-mean baseline goes under "next week", as you
> said. 3. N = 60, registered as rolling-60, with the left-edge loss documented. 4. Makefile comment fix:
> its own commit, before P7. Order: comment fix → P7 → --baseline. Go ahead.

**Note:**
- **Rejected:** the AI's `ADP_BASELINE` environment-variable workaround (it read P7's acceptance rule as
  forbidding a `--baseline` option; the rule is about plug-ins, not features).
- **Used:** `MovingAverageForecaster(3)` and `Rolling(60)`, each one class + one `@Bean`; existing classes
  edited only in `config/`. Lengths fixed before any backtest ran.
- **Results:** AR vs MA(3): no measurable difference (MA nominally ahead); MA vs naive +0.11, at the edge
  of the band.

### P8 — Self-review

**Prompt as sent:**
> Push changes. Read P8 in PROMPTS-PLAN.md and answer its open questions first

> Fix 1–6, 9 and forecast --baseline; draft PROMPTS.md notes

**Findings (most important first) and outcome:**

| # | Finding | Outcome |
|---|---|---|
| 1 | PROMPTS.md missing (D11); the pre-registration order is only provable from the session log | **Fixed:** this file |
| 2 | README's "±15% sampling noise" was an unmeasured estimate | **Fixed:** measured one SE = 10–13% of MAE (95% ≈ ±21–25%); README and D2 corrected; rule 2 kept as registered |
| 3 | "Dependencies point inward" was not enforced (ArchUnit only checked Spring) | **Fixed:** layered-architecture rule and "domain uses only the JDK" rule; each verified against a planted violation |
| 4 | `cli/` builds `Backtester`/`NamedWindow` and used an inline identity window | **Fixed:** uses the registered `all` window; the runtime-assembly exception is documented in CLAUDE.md |
| 5 | FRED fixture was hand-written, not recorded (D10) | **Fixed:** real response recorded 2026-10-22, trimmed to 13 rows, checked to contain no key |
| 6 | `rolling-60` bean name could drift from its property | **Fixed:** length is a constant beside the bean name; property removed |
| 7 | `PrintHistory.isLatestOnly()` puts an evaluation concern in the domain | **Rejected:** small, tested, and it gives the P4 guarantee; accepted as debt |
| 8 | Renderer looks up metrics by name (`"MAE"`, `"skill"`) | **Rejected:** generalising for two metrics is over-engineering |
| 9 | `SkillVsNaive` misnamed once the baseline became injectable | **Fixed:** renamed `SkillVsBaseline` |
| 10 | Spring Boot cost for a CLI (startup, SQLite opened even for `--help`) | **Rejected:** trade-off already recorded at P0 |
| — | `forecast` had no `--baseline`, unlike `backtest` | **Fixed** |
| — | Container runs as root; D8's "milliseconds" claim unmeasured | **Rejected / not done:** local CLI; noted only |
