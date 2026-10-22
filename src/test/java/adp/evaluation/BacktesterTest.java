package adp.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import adp.domain.Forecast;
import adp.domain.MonthRange;
import adp.domain.Observation;
import adp.domain.PrintHistory;
import adp.domain.ReferenceMonth;
import adp.forecast.AllHistory;
import adp.forecast.AutoregressiveForecaster;
import adp.forecast.Excluding;
import adp.forecast.Forecaster;
import adp.forecast.NaiveForecaster;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class BacktesterTest {

  static final ReferenceMonth START = ReferenceMonth.of(2015, 1);
  static final NamedWindow ALL = new NamedWindow("all", new AllHistory());
  static final List<AccuracyMetric> METRICS =
      List.of(new MeanAbsoluteError(), new SkillVsBaseline());

  static List<Long> noisySeries(int length, long seed) {
    var random = new Random(seed);
    return LongStream.range(0, length)
        .map(i -> 100_000 + random.nextLong(-50_000, 50_000))
        .boxed()
        .toList();
  }

  /** Delegates to a model and records every history and target it is asked about. */
  static final class Spy implements Forecaster {
    final Forecaster delegate;
    final List<PrintHistory> received = new ArrayList<>();

    Spy(Forecaster delegate) {
      this.delegate = delegate;
    }

    @Override
    public String name() {
      return "spy(" + delegate.name() + ")";
    }

    @Override
    public Forecast forecast(PrintHistory history) {
      received.add(history);
      return delegate.forecast(history);
    }
  }

  static Backtester backtester(Forecaster model, Forecaster baseline, NamedWindow fit) {
    return new Backtester(model, baseline, fit, METRICS);
  }

  @Test
  void lookAheadGuardNoForecasterSeesAVintageOnOrAfterTheReleaseItPredicts() {
    // Every release revises the prior month, so later vintages exist for every origin.
    var history = Releases.monthly(START, noisySeries(60, 1), 7_000);
    var model = new Spy(new AutoregressiveForecaster(3));
    var baseline = new Spy(new NaiveForecaster());

    backtester(model, baseline, ALL).run(history, Basis.REAL_TIME, ALL);

    assertThat(model.received).hasSizeGreaterThan(40);
    assertThat(baseline.received).hasSameSizeAs(model.received);
    for (var spy : List.of(model, baseline)) {
      for (PrintHistory seen : spy.received) {
        var target = seen.latest().lastKey().next();
        var release = history.vintagesOf(target).firstKey();
        for (ReferenceMonth month : seen.latest().keySet()) {
          assertThat(seen.vintagesOf(month).lastKey())
              .as("%s forecasting %s saw %s", spy.name(), target, month)
              .isBefore(release);
        }
        assertThat(seen.isLatestOnly()).isFalse();
      }
    }
  }

  @Test
  void realTimeBacktestRejectsALatestOnlyHistory() {
    var history = Releases.monthly(START, noisySeries(30, 2), 7_000);

    assertThatThrownBy(
            () ->
                backtester(new NaiveForecaster(), new NaiveForecaster(), ALL)
                    .run(history.latestOnly(), Basis.REAL_TIME, ALL))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void latestVintageBacktestFitsAndScoresOnTheLatestOnlyView() {
    var history = Releases.monthly(START, noisySeries(30, 3), 7_000);
    var model = new Spy(new NaiveForecaster());

    var report =
        backtester(model, new NaiveForecaster(), ALL).run(history, Basis.LATEST_VINTAGE, ALL);

    assertThat(model.received).isNotEmpty().allMatch(PrintHistory::isLatestOnly);
    var latestChanges = history.changes();
    assertThat(report.rows())
        .allSatisfy(r -> assertThat(r.firstPrint()).isEqualTo(latestChanges.get(r.target())));
    assertThat(report.basis()).isEqualTo(Basis.LATEST_VINTAGE);
  }

  @Test
  void naiveSkillVsBaselineIsZero() {
    var history = Releases.monthly(START, noisySeries(40, 4), 7_000);

    var report =
        backtester(new NaiveForecaster(), new NaiveForecaster(), ALL)
            .run(history, Basis.REAL_TIME, ALL);

    assertThat(report.metrics().get("skill")).hasValue(0.0);
    assertThat(report.n()).isEqualTo(39);
  }

  @Test
  void maeMatchesAHandComputedExample() {
    // First prints Jan-May: 100, 150, 120, 180, 160 (K). Naive forecasts M as M-1's first print,
    // so targets Feb-May have errors 50, 30, 60, 20 -> MAE 40K. Revisions don't change that.
    var history =
        Releases.monthly(START, List.of(100_000L, 150_000L, 120_000L, 180_000L, 160_000L), 9_000);

    var report =
        backtester(new NaiveForecaster(), new NaiveForecaster(), ALL)
            .run(history, Basis.REAL_TIME, ALL);

    assertThat(report.n()).isEqualTo(4);
    assertThat(report.metrics().get("MAE")).hasValue(40_000.0);
    assertThat(report.rows())
        .extracting(ScoredMonth::target)
        .containsExactly(
            ReferenceMonth.of(2015, 2),
            ReferenceMonth.of(2015, 3),
            ReferenceMonth.of(2015, 4),
            ReferenceMonth.of(2015, 5));
  }

  @Test
  void monthsFirstPublishedTogetherAreNotTargetsAndNotSkips() {
    // A relaunch-style batch: Jan-Dec 2015 all first appear on 2016-01-05, then monthly releases.
    var observations = new ArrayList<Observation>();
    var batch = LocalDate.of(2016, 1, 5);
    long level = 100_000_000;
    var month = ReferenceMonth.of(2014, 12);
    observations.add(new Observation(month, batch, level));
    for (int i = 0; i < 12; i++) {
      month = month.next();
      level += 100_000;
      observations.add(new Observation(month, batch, level));
    }
    for (int i = 0; i < 6; i++) {
      month = month.next();
      level += 90_000 + 1_000L * i;
      observations.add(new Observation(month, Releases.releaseOf(month), level));
    }
    var history = new PrintHistory(observations);

    var report =
        backtester(new NaiveForecaster(), new NaiveForecaster(), ALL)
            .run(history, Basis.REAL_TIME, ALL);

    assertThat(report.rows())
        .extracting(ScoredMonth::target)
        .containsExactly(
            ReferenceMonth.of(2016, 1),
            ReferenceMonth.of(2016, 2),
            ReferenceMonth.of(2016, 3),
            ReferenceMonth.of(2016, 4),
            ReferenceMonth.of(2016, 5),
            ReferenceMonth.of(2016, 6));
    assertThat(report.skipped()).isZero();
  }

  @Test
  void everyCellIsScoredOnTheSameMonthsAndSkipsAreCounted() {
    var history = Releases.monthly(START, noisySeries(72, 5), 7_000);
    var gap =
        new NamedWindow(
            "exclude-2017",
            new Excluding(new MonthRange(ReferenceMonth.of(2017, 1), ReferenceMonth.of(2017, 12))));
    var ar = new AutoregressiveForecaster(3);
    var naive = new NaiveForecaster();

    var reports =
        Backtester.compare(
            List.of(backtester(ar, naive, ALL), backtester(ar, naive, gap)),
            history,
            Basis.REAL_TIME,
            ALL);

    assertThat(reports).hasSize(2);
    var allRows = reports.get(0).rows().stream().map(ScoredMonth::target).toList();
    var gapRows = reports.get(1).rows().stream().map(ScoredMonth::target).toList();
    assertThat(allRows).isEqualTo(gapRows);
    // After the gap AR lacks lags for Jan-Apr 2018 (Jan 2018's change is gone with Dec 2017).
    assertThat(allRows)
        .doesNotContain(
            ReferenceMonth.of(2018, 1),
            ReferenceMonth.of(2018, 2),
            ReferenceMonth.of(2018, 3),
            ReferenceMonth.of(2018, 4))
        .contains(ReferenceMonth.of(2018, 5));
    assertThat(reports).allSatisfy(r -> assertThat(r.n()).isEqualTo(allRows.size()));
    assertThat(reports.get(0).skipped()).isEqualTo(reports.get(1).skipped()).isPositive();
    assertThat(reports.get(0).n() + reports.get(0).skipped())
        .isEqualTo(history.latest().size() - 1);
  }

  @Test
  void theScoringWindowChangesOnlyWhichMonthsAreScored() {
    var history = Releases.monthly(START, noisySeries(48, 6), 7_000);
    var scoreWithout2016 =
        new NamedWindow(
            "exclude-2016",
            new Excluding(new MonthRange(ReferenceMonth.of(2016, 1), ReferenceMonth.of(2016, 12))));
    var bt = backtester(new NaiveForecaster(), new NaiveForecaster(), ALL);

    var full = bt.run(history, Basis.REAL_TIME, ALL);
    var trimmed = bt.run(history, Basis.REAL_TIME, scoreWithout2016);

    assertThat(trimmed.n()).isEqualTo(full.n() - 12);
    assertThat(trimmed.rows()).isSubsetOf(full.rows());
    assertThat(trimmed.scoringWindow()).isEqualTo("exclude-2016");
    assertThat(trimmed.fitWindow()).isEqualTo("all");
  }

  @Test
  void aForecastForTheWrongMonthIsAnError() {
    var history = Releases.monthly(START, noisySeries(12, 7), 0);
    Forecaster wrongMonth =
        new Forecaster() {
          @Override
          public String name() {
            return "wrong";
          }

          @Override
          public Forecast forecast(PrintHistory h) {
            var real = new NaiveForecaster().forecast(h);
            return new Forecast(real.target().next(), real.point(), real.explanation());
          }
        };

    assertThatThrownBy(
            () ->
                backtester(wrongMonth, new NaiveForecaster(), ALL)
                    .run(history, Basis.REAL_TIME, ALL))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("wrong");
  }

  @Test
  void reportNamesModelBaselineWindowsAndBasis() {
    var history = Releases.monthly(START, noisySeries(30, 8), 7_000);

    var report =
        backtester(new AutoregressiveForecaster(3), new NaiveForecaster(), ALL)
            .run(history, Basis.REAL_TIME, ALL);

    assertThat(report.model()).isEqualTo("AR(3)");
    assertThat(report.baseline()).isEqualTo("naive");
    assertThat(report.basis()).isEqualTo(Basis.REAL_TIME);
    assertThat(report.metrics()).containsOnlyKeys("MAE", "skill");
  }
}
