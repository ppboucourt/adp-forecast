package adp.evaluation;

import static adp.evaluation.Rows.row;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class MeanAbsoluteErrorTest {

  final MeanAbsoluteError mae = new MeanAbsoluteError();

  @Test
  void matchesAHandComputedExample() {
    // Model errors: +10K, -20K, +30K -> mean of absolute values = 20K.
    var rows =
        List.of(
            row(1, 100_000, 110_000, 0), row(2, 150_000, 130_000, 0), row(3, 50_000, 80_000, 0));

    assertThat(mae.compute(rows)).hasValue(20_000.0);
  }

  @Test
  void ignoresTheBaselineColumn() {
    assertThat(mae.compute(List.of(row(1, 100, 100, 999_999)))).hasValue(0.0);
  }

  @Test
  void isUndefinedWithoutRows() {
    assertThat(mae.compute(List.of())).isEmpty();
  }
}
