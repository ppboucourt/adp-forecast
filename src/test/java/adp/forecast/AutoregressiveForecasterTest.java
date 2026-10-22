package adp.forecast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

import adp.domain.Explanation.Constant;
import adp.domain.Explanation.Term;
import adp.domain.Explanation.Weighted;
import adp.domain.MonthRange;
import adp.domain.PrintHistory;
import adp.domain.ReferenceMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class AutoregressiveForecasterTest {

  static final double INTERCEPT = 20_000;
  static final double[] PHI = {0.5, 0.2, 0.1};

  /** y_t = 20K + 0.5 y_{t-1} + 0.2 y_{t-2} + 0.1 y_{t-3} + N(0, 10K), seeded. */
  static List<Long> syntheticAr3(int length, long seed) {
    var random = new Random(seed);
    double mean = INTERCEPT / (1 - PHI[0] - PHI[1] - PHI[2]);
    double[] y = new double[length + 200];
    y[0] = y[1] = y[2] = mean;
    for (int t = 3; t < y.length; t++) {
      y[t] =
          INTERCEPT
              + PHI[0] * y[t - 1]
              + PHI[1] * y[t - 2]
              + PHI[2] * y[t - 3]
              + 10_000 * random.nextGaussian();
    }
    var series = new ArrayList<Long>();
    // Drop a burn-in so the start values don't matter.
    for (int t = 200; t < y.length; t++) {
      series.add(Math.round(y[t]));
    }
    return series;
  }

  static List<Weighted> weighted(List<Term> terms) {
    return terms.stream().filter(Weighted.class::isInstance).map(Weighted.class::cast).toList();
  }

  final AutoregressiveForecaster ar3 = new AutoregressiveForecaster(3);

  @Test
  void recoversKnownCoefficientsOfASyntheticAr3Series() {
    var history = Histories.fromChanges(ReferenceMonth.of(1850, 1), syntheticAr3(2_000, 42));

    var terms = ar3.forecast(history).explanation().terms();

    var lags = weighted(terms);
    assertThat(lags).hasSize(3);
    for (int i = 0; i < 3; i++) {
      assertThat(lags.get(i).weight()).as("phi_%d", i + 1).isCloseTo(PHI[i], within(0.05));
    }
    var intercept = (Constant) terms.getFirst();
    assertThat((double) intercept.amount().jobs()).isCloseTo(INTERCEPT, within(3_000.0));
  }

  @Test
  void contributionsSumExactlyToThePointForecast() {
    for (long seed = 1; seed <= 20; seed++) {
      var history = Histories.fromChanges(ReferenceMonth.of(2000, 1), syntheticAr3(150, seed));

      var forecast = ar3.forecast(history);

      long sum = forecast.explanation().terms().stream().mapToLong(t -> t.amount().jobs()).sum();
      assertThat(forecast.point().jobs()).as("seed %d", seed).isEqualTo(sum);
    }
  }

  @Test
  void explainsWithInterceptThenTheMostRecentLagsFirst() {
    var history = Histories.fromChanges(ReferenceMonth.of(2000, 1), syntheticAr3(150, 7));
    var changes = history.changes();
    var last = changes.lastKey();

    var forecast = ar3.forecast(history);

    assertThat(forecast.target()).isEqualTo(last.next());
    assertThat(forecast.explanation().terms().getFirst()).isInstanceOf(Constant.class);
    assertThat(weighted(forecast.explanation().terms()))
        .extracting(Weighted::month, Weighted::input)
        .containsExactly(
            tuple(last, changes.get(last)),
            tuple(last.previous(), changes.get(last.previous())),
            tuple(last.previous().previous(), changes.get(last.previous().previous())));
  }

  @Test
  void neverBuildsARegressionRowAcrossAGap() {
    var history =
        Histories.fromChanges(ReferenceMonth.of(2019, 1), syntheticAr3(60, 3))
            .excluding(new MonthRange(ReferenceMonth.of(2020, 3), ReferenceMonth.of(2021, 12)));

    var targets = AutoregressiveForecaster.regressionTargets(history.changes(), 3);

    // Changes for Mar 2020 - Jan 2022 are gone; Feb - Apr 2022 lack a full set of lags.
    var gapStart = ReferenceMonth.of(2020, 3);
    var firstFullRowAfter = ReferenceMonth.of(2022, 5);
    assertThat(targets)
        .allSatisfy(
            t ->
                assertThat(t.compareTo(gapStart) < 0 || t.compareTo(firstFullRowAfter) >= 0)
                    .as("row for %s", t)
                    .isTrue())
        .contains(ReferenceMonth.of(2020, 2), firstFullRowAfter);
    assertThat(targets).doesNotContain(ReferenceMonth.of(2019, 1), ReferenceMonth.of(2019, 3));
    assertThat(targets.getFirst()).isEqualTo(ReferenceMonth.of(2019, 4));
  }

  @Test
  void cannotForecastUntilPLagsFollowTheGapWhileNaiveCan() {
    // History ends Feb 2022: its change exists, but Jan 2022's does not.
    var history =
        Histories.fromChanges(ReferenceMonth.of(2015, 1), syntheticAr3(86, 5))
            .excluding(new MonthRange(ReferenceMonth.of(2020, 3), ReferenceMonth.of(2021, 12)));
    assertThat(history.latest().lastKey()).isEqualTo(ReferenceMonth.of(2022, 2));

    assertThatThrownBy(() -> ar3.forecast(history))
        .isInstanceOf(InsufficientHistoryException.class)
        .hasMessageContaining("2022-01");
    assertThat(new NaiveForecaster().forecast(history).target())
        .isEqualTo(ReferenceMonth.of(2022, 3));
  }

  @Test
  void failsClearlyWithTooFewRows() {
    PrintHistory history = Histories.fromChanges(ReferenceMonth.of(2026, 1), 1, 2, 3, 4);

    assertThatThrownBy(() -> ar3.forecast(history))
        .isInstanceOf(InsufficientHistoryException.class)
        .hasMessageContaining("AR(3)");
  }

  @Test
  void nameShowsTheOrderAndOrderMustBePositive() {
    assertThat(ar3.name()).isEqualTo("AR(3)");
    assertThatThrownBy(() -> new AutoregressiveForecaster(0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
