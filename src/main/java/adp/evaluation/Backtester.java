package adp.evaluation;

import adp.domain.JobsChange;
import adp.domain.PrintHistory;
import adp.domain.ReferenceMonth;
import adp.forecast.Forecaster;
import adp.forecast.InsufficientHistoryException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * Walk-forward backtest. For each target month M, the model and the baseline see only the history
 * as it stood the day before M was first published, with the fit window applied, and are scored
 * against M's first print. Targets are the months whose predecessor was the latest month held at
 * that moment, so months first published together (the relaunch backcast) are never targets.
 */
public final class Backtester {

  private final Forecaster model;
  private final Forecaster baseline;
  private final NamedWindow fitWindow;
  private final List<AccuracyMetric> metrics;

  public Backtester(
      Forecaster model, Forecaster baseline, NamedWindow fitWindow, List<AccuracyMetric> metrics) {
    this.model = Objects.requireNonNull(model, "model");
    this.baseline = Objects.requireNonNull(baseline, "baseline");
    this.fitWindow = Objects.requireNonNull(fitWindow, "fitWindow");
    this.metrics = List.copyOf(metrics);
  }

  public BacktestReport run(PrintHistory history, Basis basis, NamedWindow scoringWindow) {
    return compare(List.of(this), history, basis, scoringWindow).getFirst();
  }

  /**
   * Runs every cell on the same history and scores them all on the same months: those in the
   * scoring window that every cell's model and baseline could forecast.
   */
  public static List<BacktestReport> compare(
      List<Backtester> cells, PrintHistory history, Basis basis, NamedWindow scoringWindow) {
    PrintHistory base =
        switch (basis) {
          case REAL_TIME -> {
            if (history.isLatestOnly()) {
              throw new IllegalArgumentException(
                  "a real-time backtest needs real vintages, not a latestOnly() history");
            }
            yield history;
          }
          case LATEST_VINTAGE -> history.latestOnly();
        };

    Set<ReferenceMonth> inScoringWindow = scoringWindow.window().apply(base).latest().keySet();
    List<Target> targets =
        targets(base).stream().filter(t -> inScoringWindow.contains(t.month())).toList();

    List<Map<ReferenceMonth, ScoredMonth>> forecasts =
        cells.stream().map(cell -> cell.forecastAll(targets)).toList();
    List<ReferenceMonth> common =
        targets.stream()
            .map(Target::month)
            .filter(m -> forecasts.stream().allMatch(f -> f.containsKey(m)))
            .toList();
    int skipped = targets.size() - common.size();

    var reports = new ArrayList<BacktestReport>();
    for (int i = 0; i < cells.size(); i++) {
      var byMonth = forecasts.get(i);
      var rows = common.stream().map(byMonth::get).toList();
      reports.add(cells.get(i).report(basis, scoringWindow, rows, skipped));
    }
    return reports;
  }

  private record Target(ReferenceMonth month, PrintHistory origin, JobsChange firstPrint) {}

  private static List<Target> targets(PrintHistory history) {
    var targets = new ArrayList<Target>();
    for (ReferenceMonth month : history.latest().keySet()) {
      var release = history.vintagesOf(month).firstKey();
      var origin = history.asOf(release.minusDays(1));
      var held = origin.latest();
      if (held.isEmpty() || !held.lastKey().equals(month.previous())) {
        continue;
      }
      history
          .firstPrintChange(month)
          .ifPresent(firstPrint -> targets.add(new Target(month, origin, firstPrint)));
    }
    return targets;
  }

  private Map<ReferenceMonth, ScoredMonth> forecastAll(List<Target> targets) {
    var scored = new LinkedHashMap<ReferenceMonth, ScoredMonth>();
    for (Target target : targets) {
      var fitted = fitWindow.window().apply(target.origin());
      var held = fitted.latest();
      // The fit window removed the origin month, so this cell cannot forecast the target.
      if (held.isEmpty() || !held.lastKey().equals(target.month().previous())) {
        continue;
      }
      var modelPoint = attempt(model, fitted, target.month());
      var baselinePoint = attempt(baseline, fitted, target.month());
      if (modelPoint.isPresent() && baselinePoint.isPresent()) {
        scored.put(
            target.month(),
            new ScoredMonth(
                target.month(), target.firstPrint(), modelPoint.get(), baselinePoint.get()));
      }
    }
    return scored;
  }

  private static Optional<JobsChange> attempt(
      Forecaster forecaster, PrintHistory fitted, ReferenceMonth target) {
    try {
      var forecast = forecaster.forecast(fitted);
      if (!forecast.target().equals(target)) {
        throw new IllegalStateException(
            forecaster.name() + " forecast " + forecast.target() + " when asked for " + target);
      }
      return Optional.of(forecast.point());
    } catch (InsufficientHistoryException e) {
      return Optional.empty();
    }
  }

  private BacktestReport report(
      Basis basis, NamedWindow scoringWindow, List<ScoredMonth> rows, int skipped) {
    var scores = new LinkedHashMap<String, OptionalDouble>();
    for (AccuracyMetric metric : metrics) {
      scores.put(metric.name(), metric.compute(rows));
    }
    return new BacktestReport(
        basis,
        model.name(),
        baseline.name(),
        fitWindow.name(),
        scoringWindow.name(),
        rows,
        skipped,
        scores);
  }
}
