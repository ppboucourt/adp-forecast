package adp.evaluation;

import static adp.evaluation.Rows.row;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.junit.jupiter.api.Test;

class SkillVsNaiveTest {

  final SkillVsNaive skill = new SkillVsNaive();

  @Test
  void isOneMinusModelMaeOverBaselineMae() {
    // Model MAE 15, baseline MAE 20 -> 1 - 15/20 = 0.25.
    var rows = List.of(row(1, 100, 110, 120), row(2, 100, 80, 80));

    assertThat(skill.compute(rows).getAsDouble()).isCloseTo(0.25, within(1e-12));
  }

  @Test
  void isZeroWhenTheModelIsTheBaseline() {
    var rows = List.of(row(1, 100, 130, 130), row(2, 100, 60, 60));

    assertThat(skill.compute(rows)).hasValue(0.0);
  }

  @Test
  void isNegativeWhenTheModelIsWorse() {
    assertThat(skill.compute(List.of(row(1, 100, 140, 120))).getAsDouble()).isNegative();
  }

  @Test
  void isUndefinedWhenTheBaselineIsPerfectOrThereAreNoRows() {
    assertThat(skill.compute(List.of(row(1, 100, 110, 100)))).isEmpty();
    assertThat(skill.compute(List.of())).isEmpty();
  }
}
