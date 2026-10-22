package adp.cli;

import adp.domain.Observation;
import adp.domain.PrintHistory;
import adp.port.PrintRepository;
import java.io.PrintWriter;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

/** Loads everything stored and warns on stderr when the newest vintage is stale. */
final class StoredHistory {

  static final long STALE_AFTER_DAYS = 35;

  private StoredHistory() {}

  static PrintHistory load(PrintRepository repository, Clock clock, PrintWriter err) {
    var observations = repository.findAll();
    if (observations.isEmpty()) {
      throw new IllegalStateException("no data stored; run make init or adp sync");
    }
    LocalDate newest =
        observations.stream().map(Observation::vintageDate).max(Comparator.naturalOrder()).get();
    long age = ChronoUnit.DAYS.between(newest, LocalDate.now(clock));
    if (age > STALE_AFTER_DAYS) {
      err.println("warning: newest vintage is " + newest + " (" + age + " days old); run adp sync");
    }
    return new PrintHistory(observations);
  }
}
