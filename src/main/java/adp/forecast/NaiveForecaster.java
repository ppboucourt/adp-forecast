package adp.forecast;

import adp.domain.Explanation;
import adp.domain.Explanation.Weighted;
import adp.domain.Forecast;
import adp.domain.JobsChange;
import adp.domain.PrintHistory;
import java.util.List;

/** Next change = latest change. The baseline every other model is scored against. */
public class NaiveForecaster implements Forecaster {

  @Override
  public String name() {
    return "naive";
  }

  @Override
  public Forecast forecast(PrintHistory history) {
    var levels = history.latest();
    if (levels.isEmpty()) {
      throw new InsufficientHistoryException("naive needs at least two months; history is empty");
    }
    var latest = levels.lastKey();
    JobsChange last = history.changes().get(latest);
    if (last == null) {
      throw new InsufficientHistoryException("naive needs a change for " + latest);
    }
    return Forecast.of(latest.next(), new Explanation(List.of(new Weighted(1.0, latest, last))));
  }
}
