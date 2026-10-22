package adp.forecast;

import adp.domain.MonthRange;
import adp.domain.PrintHistory;
import java.util.Objects;

/**
 * Drops every vintage of the months in {@code range}. Because a change needs the prior month's
 * level, the change for the first month after the range is lost too.
 */
public record Excluding(MonthRange range) implements HistoryWindow {

  public Excluding {
    Objects.requireNonNull(range, "range");
  }

  @Override
  public PrintHistory apply(PrintHistory history) {
    return history.excluding(range);
  }
}
