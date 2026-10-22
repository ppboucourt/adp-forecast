package adp.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Every published vintage of the payroll level, indexed by reference month. The level of a month at
 * date D is its latest vintage on or before D, so {@link #latest()} is the series as it stood at
 * the newest vintage held, and {@link #asOf(LocalDate)} is the series as it stood at D.
 */
public final class PrintHistory {

  private final NavigableMap<ReferenceMonth, NavigableMap<LocalDate, Long>> trails;
  private final boolean latestOnly;

  public PrintHistory(Collection<Observation> observations) {
    this(observations, false);
  }

  private PrintHistory(Collection<Observation> observations, boolean latestOnly) {
    Objects.requireNonNull(observations, "observations");
    this.latestOnly = latestOnly;
    var byMonth = new TreeMap<ReferenceMonth, NavigableMap<LocalDate, Long>>();
    for (Observation o : observations) {
      var trail = byMonth.computeIfAbsent(o.month(), m -> new TreeMap<>());
      Long existing = trail.putIfAbsent(o.vintageDate(), o.level());
      if (existing != null && existing != o.level()) {
        throw new IllegalArgumentException(
            "conflicting levels for " + o.month() + " at vintage " + o.vintageDate());
      }
    }
    byMonth.replaceAll((month, trail) -> Collections.unmodifiableNavigableMap(trail));
    this.trails = Collections.unmodifiableNavigableMap(byMonth);
  }

  /** The history as it was known on {@code date}: vintages published after it are dropped. */
  public PrintHistory asOf(LocalDate date) {
    Objects.requireNonNull(date, "date");
    return new PrintHistory(
        trails.entrySet().stream()
            .flatMap(
                e ->
                    e.getValue().headMap(date, true).entrySet().stream()
                        .map(v -> new Observation(e.getKey(), v.getKey(), v.getValue())))
            .toList(),
        latestOnly);
  }

  /** The history without any vintage of the months in {@code range}. */
  public PrintHistory excluding(MonthRange range) {
    Objects.requireNonNull(range, "range");
    return new PrintHistory(
        trails.entrySet().stream()
            .filter(e -> !range.contains(e.getKey()))
            .flatMap(
                e ->
                    e.getValue().entrySet().stream()
                        .map(v -> new Observation(e.getKey(), v.getKey(), v.getValue())))
            .toList(),
        latestOnly);
  }

  /**
   * Every month's latest level as its only vintage, stamped with a <b>synthetic</b> release date:
   * the first day of the following month. Not real publication dates (backcast months share one
   * real vintage); used only for the latest-vintage backtest, whose fit and truth are both revised
   * data and therefore biased. The result reports {@link #isLatestOnly()}, as do views derived from
   * it.
   */
  public PrintHistory latestOnly() {
    return new PrintHistory(
        latest().entrySet().stream()
            .map(e -> new Observation(e.getKey(), e.getKey().next().firstDay(), e.getValue()))
            .toList(),
        true);
  }

  /** True for {@link #latestOnly()} and views derived from it. */
  public boolean isLatestOnly() {
    return latestOnly;
  }

  /** Latest level of every month, in month order. */
  public NavigableMap<ReferenceMonth, Long> latest() {
    var levels = new TreeMap<ReferenceMonth, Long>();
    trails.forEach((month, trail) -> levels.put(month, trail.lastEntry().getValue()));
    return Collections.unmodifiableNavigableMap(levels);
  }

  /** Headline series: change of each month's latest level over the prior month's latest level. */
  public NavigableMap<ReferenceMonth, JobsChange> changes() {
    var levels = latest();
    var changes = new TreeMap<ReferenceMonth, JobsChange>();
    levels.forEach(
        (month, level) -> {
          Long prior = levels.get(month.previous());
          if (prior != null) {
            changes.put(month, JobsChange.between(prior, level));
          }
        });
    return Collections.unmodifiableNavigableMap(changes);
  }

  /**
   * Change for {@code month} as first published: both levels are read as of the vintage that first
   * published the month. Empty if the month, or the prior month at that vintage, is unknown.
   */
  public Optional<JobsChange> firstPrintChange(ReferenceMonth month) {
    var trail = trails.get(month);
    var priorTrail = trails.get(month.previous());
    if (trail == null || priorTrail == null) {
      return Optional.empty();
    }
    Map.Entry<LocalDate, Long> first = trail.firstEntry();
    Map.Entry<LocalDate, Long> prior = priorTrail.floorEntry(first.getKey());
    if (prior == null) {
      return Optional.empty();
    }
    return Optional.of(JobsChange.between(prior.getValue(), first.getValue()));
  }

  /** Revision trail of one month: level by vintage date, oldest first. Empty if unknown. */
  public NavigableMap<LocalDate, Long> vintagesOf(ReferenceMonth month) {
    return trails.getOrDefault(month, Collections.emptyNavigableMap());
  }
}
