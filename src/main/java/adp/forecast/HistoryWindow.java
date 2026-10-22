package adp.forecast;

import adp.domain.PrintHistory;

/** Selects which part of a history a model is fitted on (or a backtest is scored on). */
@FunctionalInterface
public interface HistoryWindow {

  PrintHistory apply(PrintHistory history);

  /** This window, then {@code next} on its result. */
  default HistoryWindow andThen(HistoryWindow next) {
    return history -> next.apply(apply(history));
  }
}
