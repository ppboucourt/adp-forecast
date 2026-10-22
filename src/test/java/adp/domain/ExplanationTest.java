package adp.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import adp.domain.Explanation.Constant;
import adp.domain.Explanation.Weighted;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExplanationTest {

  static final ReferenceMonth SEP = ReferenceMonth.of(2026, 9);
  static final ReferenceMonth AUG = ReferenceMonth.of(2026, 8);

  @Test
  void weightedAmountIsWeightTimesInputRoundedToWholeJobs() {
    assertThat(new Weighted(0.45, SEP, new JobsChange(143_001)).amount())
        .isEqualTo(new JobsChange(64_350));
    assertThat(new Weighted(0.5, SEP, new JobsChange(-3)).amount()).isEqualTo(new JobsChange(-1));
  }

  @Test
  void totalIsTheSumOfTermAmounts() {
    var explanation =
        new Explanation(
            List.of(
                new Constant("intercept", new JobsChange(42_000)),
                new Weighted(0.45, SEP, new JobsChange(143_000)),
                new Weighted(0.21, AUG, new JobsChange(54_000))));

    assertThat(explanation.total()).isEqualTo(new JobsChange(42_000 + 64_350 + 11_340));
  }

  @Test
  void rejectsEmptyOrInvalidTerms() {
    assertThatThrownBy(() -> new Explanation(List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Weighted(Double.NaN, SEP, new JobsChange(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Constant(" ", new JobsChange(1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void isImmutable() {
    var terms = new ArrayList<Explanation.Term>();
    terms.add(new Constant("intercept", new JobsChange(1)));
    var explanation = new Explanation(terms);

    terms.add(new Constant("extra", new JobsChange(1)));

    assertThat(explanation.terms()).hasSize(1);
  }
}
