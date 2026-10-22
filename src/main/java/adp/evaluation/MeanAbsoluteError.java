package adp.evaluation;

import java.util.List;
import java.util.OptionalDouble;

/** Mean absolute error of the model against first prints, in jobs. */
public class MeanAbsoluteError implements AccuracyMetric {

  @Override
  public String name() {
    return "MAE";
  }

  @Override
  public OptionalDouble compute(List<ScoredMonth> rows) {
    return rows.stream().mapToLong(r -> Math.abs(r.modelError())).average();
  }
}
