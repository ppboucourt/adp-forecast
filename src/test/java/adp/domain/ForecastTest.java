package adp.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import adp.domain.Explanation.Constant;
import adp.domain.Explanation.Weighted;
import java.util.List;
import org.junit.jupiter.api.Test;

class ForecastTest {

  static final ReferenceMonth OCT = ReferenceMonth.of(2026, 10);
  static final Explanation EXPLANATION =
      new Explanation(
          List.of(
              new Constant("intercept", new JobsChange(42_000)),
              new Weighted(0.45, OCT.previous(), new JobsChange(143_000))));

  @Test
  void acceptsAPointEqualToTheSumOfItsTerms() {
    var forecast = new Forecast(OCT, new JobsChange(106_350), EXPLANATION);

    assertThat(forecast.point()).isEqualTo(EXPLANATION.total());
  }

  @Test
  void rejectsAPointThatDiffersFromItsExplanation() {
    assertThatThrownBy(() -> new Forecast(OCT, new JobsChange(106_351), EXPLANATION))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void ofComputesThePointFromTheExplanation() {
    assertThat(Forecast.of(OCT, EXPLANATION).point()).isEqualTo(new JobsChange(106_350));
  }
}
