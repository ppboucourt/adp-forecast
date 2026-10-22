package adp.evaluation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Result of one backtest cell. {@code rows} are the scored months; {@code skipped} counts months in
 * the scoring window that some cell of the same comparison could not forecast.
 */
public record BacktestReport(
    Basis basis,
    String model,
    String baseline,
    String fitWindow,
    String scoringWindow,
    List<ScoredMonth> rows,
    int skipped,
    Map<String, OptionalDouble> metrics) {

  public BacktestReport {
    rows = List.copyOf(rows);
    metrics = Collections.unmodifiableMap(new LinkedHashMap<>(metrics));
  }

  /** Number of scored months. */
  public int n() {
    return rows.size();
  }
}
