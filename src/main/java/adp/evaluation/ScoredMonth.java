package adp.evaluation;

import adp.domain.JobsChange;
import adp.domain.ReferenceMonth;
import java.util.Objects;

/** One scored backtest month: the first print and what the model and the baseline forecast. */
public record ScoredMonth(
    ReferenceMonth target, JobsChange firstPrint, JobsChange modelPoint, JobsChange baselinePoint) {

  public ScoredMonth {
    Objects.requireNonNull(target, "target");
    Objects.requireNonNull(firstPrint, "firstPrint");
    Objects.requireNonNull(modelPoint, "modelPoint");
    Objects.requireNonNull(baselinePoint, "baselinePoint");
  }

  public long modelError() {
    return modelPoint.jobs() - firstPrint.jobs();
  }

  public long baselineError() {
    return baselinePoint.jobs() - firstPrint.jobs();
  }
}
