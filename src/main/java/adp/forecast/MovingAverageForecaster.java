package adp.forecast;

import adp.domain.Explanation;
import adp.domain.Explanation.Term;
import adp.domain.Explanation.Weighted;
import adp.domain.Forecast;
import adp.domain.PrintHistory;
import java.util.ArrayList;

/** Next change = equal-weighted mean of the last {@code window} changes; nothing is fitted. */
public class MovingAverageForecaster implements Forecaster {

  private final int window;

  public MovingAverageForecaster(int window) {
    if (window < 1) {
      throw new IllegalArgumentException("moving-average window must be at least 1: " + window);
    }
    this.window = window;
  }

  @Override
  public String name() {
    return "MA(" + window + ")";
  }

  @Override
  public Forecast forecast(PrintHistory history) {
    var levels = history.latest();
    if (levels.isEmpty()) {
      throw new InsufficientHistoryException(name() + " needs history; history is empty");
    }
    var target = levels.lastKey().next();
    var changes = history.changes();
    var terms = new ArrayList<Term>(window);
    var lag = target;
    for (int i = 0; i < window; i++) {
      lag = lag.previous();
      var change = changes.get(lag);
      if (change == null) {
        throw new InsufficientHistoryException(
            name()
                + " needs changes for the "
                + window
                + " months before "
                + target
                + "; missing "
                + lag);
      }
      terms.add(new Weighted(1.0 / window, lag, change));
    }
    return Forecast.of(target, new Explanation(terms));
  }
}
