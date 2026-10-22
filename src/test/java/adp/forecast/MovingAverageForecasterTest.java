package adp.forecast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import adp.domain.Explanation.Weighted;
import adp.domain.JobsChange;
import adp.domain.MonthRange;
import adp.domain.ReferenceMonth;
import org.junit.jupiter.api.Test;

class MovingAverageForecasterTest {

  final MovingAverageForecaster ma3 = new MovingAverageForecaster(3);

  @Test
  void forecastsTheEqualWeightedMeanOfTheLastKChanges() {
    var history =
        Histories.fromChanges(ReferenceMonth.of(2026, 5), 500_000, 90_000, 30_000, 60_000);

    var forecast = ma3.forecast(history);

    assertThat(forecast.target()).isEqualTo(ReferenceMonth.of(2026, 9));
    assertThat(forecast.point()).isEqualTo(new JobsChange(60_000));
    assertThat(forecast.explanation().terms())
        .containsExactly(
            new Weighted(1.0 / 3, ReferenceMonth.of(2026, 8), new JobsChange(60_000)),
            new Weighted(1.0 / 3, ReferenceMonth.of(2026, 7), new JobsChange(30_000)),
            new Weighted(1.0 / 3, ReferenceMonth.of(2026, 6), new JobsChange(90_000)));
  }

  @Test
  void pointIsTheSumOfRoundedTermsSoTheExplanationAddsUp() {
    var history = Histories.fromChanges(ReferenceMonth.of(2026, 6), 10_001, 10_001, 10_001);

    var forecast = ma3.forecast(history);

    long sum = forecast.explanation().terms().stream().mapToLong(t -> t.amount().jobs()).sum();
    assertThat(forecast.point().jobs()).isEqualTo(sum).isEqualTo(10_002);
  }

  @Test
  void needsKConsecutiveChangesBeforeTheTarget() {
    var tooShort = Histories.fromChanges(ReferenceMonth.of(2026, 7), 1_000, 2_000);
    assertThatThrownBy(() -> ma3.forecast(tooShort))
        .isInstanceOf(InsufficientHistoryException.class)
        .hasMessageContaining("MA(3)");

    var gap =
        Histories.fromChanges(ReferenceMonth.of(2026, 1), 1, 2, 3, 4, 5, 6, 7, 8)
            .excluding(new MonthRange(ReferenceMonth.of(2026, 6), ReferenceMonth.of(2026, 6)));
    assertThatThrownBy(() -> ma3.forecast(gap))
        .isInstanceOf(InsufficientHistoryException.class)
        .hasMessageContaining("2026-07");
  }

  @Test
  void nameShowsTheWindowWhichMustBePositive() {
    assertThat(ma3.name()).isEqualTo("MA(3)");
    assertThatThrownBy(() -> new MovingAverageForecaster(0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
