package adp.forecast;

import adp.domain.Forecast;
import adp.domain.PrintHistory;

/** A forecasting model. Forecasts the month after the latest month in {@code history}. */
public interface Forecaster {

  /** Display name, e.g. {@code AR(3)}. */
  String name();

  /**
   * @throws InsufficientHistoryException if the history lacks the months this model needs
   */
  Forecast forecast(PrintHistory history);
}
