package adp.forecast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import adp.domain.Explanation.Weighted;
import adp.domain.JobsChange;
import adp.domain.ReferenceMonth;
import org.junit.jupiter.api.Test;

class NaiveForecasterTest {

  final NaiveForecaster naive = new NaiveForecaster();

  @Test
  void forecastsTheMonthAfterTheLatestAsTheLastChange() {
    var history = Histories.fromChanges(ReferenceMonth.of(2026, 7), 23_000, 54_000, 143_000);

    var forecast = naive.forecast(history);

    assertThat(forecast.target()).isEqualTo(ReferenceMonth.of(2026, 10));
    assertThat(forecast.point()).isEqualTo(new JobsChange(143_000));
    assertThat(forecast.explanation().terms())
        .containsExactly(new Weighted(1.0, ReferenceMonth.of(2026, 9), new JobsChange(143_000)));
  }

  @Test
  void failsWhenTheLatestMonthHasNoChange() {
    var history = Histories.fromChanges(ReferenceMonth.of(2026, 7));

    assertThatThrownBy(() -> naive.forecast(history))
        .isInstanceOf(InsufficientHistoryException.class);
  }

  @Test
  void isNamedNaive() {
    assertThat(naive.name()).isEqualTo("naive");
  }
}
