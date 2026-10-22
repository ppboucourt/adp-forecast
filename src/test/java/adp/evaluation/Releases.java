package adp.evaluation;

import adp.domain.Observation;
import adp.domain.PrintHistory;
import adp.domain.ReferenceMonth;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a release history: month M is first published on the 5th of M+1, and that release also
 * revises M-1 by {@code revision} jobs.
 */
final class Releases {

  private Releases() {}

  static LocalDate releaseOf(ReferenceMonth month) {
    return month.next().firstDay().plusDays(4);
  }

  /** First-print changes, starting at {@code first}; a base level for {@code first-1}. */
  static PrintHistory monthly(ReferenceMonth first, List<Long> firstPrints, long revision) {
    var observations = new ArrayList<Observation>();
    var base = first.previous();
    long level = 100_000_000;
    observations.add(new Observation(base, releaseOf(base), level));
    var month = base;
    for (long change : firstPrints) {
      var previous = month;
      month = month.next();
      // The release of `month` revises `previous` and prints `month` on top of the revision.
      long revised = level + revision;
      if (revision != 0) {
        observations.add(new Observation(previous, releaseOf(month), revised));
      }
      level = revised + change;
      observations.add(new Observation(month, releaseOf(month), level));
    }
    return new PrintHistory(observations);
  }
}
