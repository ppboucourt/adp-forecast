package adp.forecast;

import adp.domain.Observation;
import adp.domain.PrintHistory;
import adp.domain.ReferenceMonth;
import java.util.ArrayList;
import java.util.List;

/** Builds single-vintage histories from a series of monthly changes. */
final class Histories {

  static final long BASE_LEVEL = 100_000_000;

  private Histories() {}

  /** Level for {@code first.previous()} is the base; each change adds one month after it. */
  static PrintHistory fromChanges(ReferenceMonth first, List<Long> changes) {
    var observations = new ArrayList<Observation>();
    var month = first.previous();
    long level = BASE_LEVEL;
    observations.add(new Observation(month, month.next().firstDay(), level));
    for (long change : changes) {
      month = month.next();
      level += change;
      observations.add(new Observation(month, month.next().firstDay(), level));
    }
    return new PrintHistory(observations);
  }

  static PrintHistory fromChanges(ReferenceMonth first, long... changes) {
    var list = new ArrayList<Long>();
    for (long c : changes) {
      list.add(c);
    }
    return fromChanges(first, list);
  }
}
