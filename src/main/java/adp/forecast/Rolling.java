package adp.forecast;

import adp.domain.MonthRange;
import adp.domain.PrintHistory;
import adp.domain.ReferenceMonth;

/**
 * Keeps the last {@code months} reference months, counted back from the latest month held, so the
 * window slides with the as-of date. The first kept month has no prior level, so the change series
 * holds {@code months - 1} changes.
 */
public record Rolling(int months) implements HistoryWindow {

  public Rolling {
    if (months < 1) {
      throw new IllegalArgumentException("rolling window must be at least 1 month: " + months);
    }
  }

  @Override
  public PrintHistory apply(PrintHistory history) {
    var levels = history.latest();
    if (levels.isEmpty()) {
      return history;
    }
    var firstKept = new ReferenceMonth(levels.lastKey().value().minusMonths(months - 1));
    if (levels.firstKey().compareTo(firstKept) >= 0) {
      return history;
    }
    return history.excluding(new MonthRange(levels.firstKey(), firstKept.previous()));
  }
}
