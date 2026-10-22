package adp.evaluation;

import adp.domain.JobsChange;
import adp.domain.ReferenceMonth;

final class Rows {

  private Rows() {}

  static ScoredMonth row(int month, long firstPrint, long model, long baseline) {
    return new ScoredMonth(
        ReferenceMonth.of(2023, month),
        new JobsChange(firstPrint),
        new JobsChange(model),
        new JobsChange(baseline));
  }
}
