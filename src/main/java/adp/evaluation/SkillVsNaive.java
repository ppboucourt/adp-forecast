package adp.evaluation;

import java.util.List;
import java.util.OptionalDouble;

/**
 * {@code 1 − MAE_model / MAE_baseline}: positive means the model beats the baseline. The baseline
 * is whatever the backtester was given (naive by default). Undefined when the baseline is perfect.
 */
public class SkillVsNaive implements AccuracyMetric {

  @Override
  public String name() {
    return "skill";
  }

  @Override
  public OptionalDouble compute(List<ScoredMonth> rows) {
    if (rows.isEmpty()) {
      return OptionalDouble.empty();
    }
    double model = rows.stream().mapToLong(r -> Math.abs(r.modelError())).sum();
    double baseline = rows.stream().mapToLong(r -> Math.abs(r.baselineError())).sum();
    if (baseline == 0) {
      return OptionalDouble.empty();
    }
    return OptionalDouble.of(1 - model / baseline);
  }
}
