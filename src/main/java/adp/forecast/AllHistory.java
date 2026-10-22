package adp.forecast;

import adp.domain.PrintHistory;

/** Keeps the whole history. */
public record AllHistory() implements HistoryWindow {

  @Override
  public PrintHistory apply(PrintHistory history) {
    return history;
  }
}
