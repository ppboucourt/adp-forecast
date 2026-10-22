package adp.evaluation;

import java.util.List;
import java.util.OptionalDouble;

/** A score over the months a backtest compared. */
public interface AccuracyMetric {

  /** Short label used in reports, e.g. {@code MAE}. */
  String name();

  /** Empty when the score is undefined for these rows. */
  OptionalDouble compute(List<ScoredMonth> rows);
}
