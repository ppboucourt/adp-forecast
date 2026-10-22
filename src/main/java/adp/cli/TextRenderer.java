package adp.cli;

import adp.domain.Explanation.Constant;
import adp.domain.Explanation.Term;
import adp.domain.Explanation.Weighted;
import adp.domain.Forecast;
import adp.domain.JobsChange;
import adp.domain.PrintHistory;
import adp.domain.ReferenceMonth;
import adp.evaluation.BacktestReport;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.stream.IntStream;

/** Plain-text output for every command. Jobs are shown in thousands (K). */
final class TextRenderer {

  private static final DateTimeFormatter MONTH_YEAR =
      DateTimeFormatter.ofPattern("MMM yyyy", Locale.US);
  private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM", Locale.US);
  private static final String RULE = "─".repeat(34);

  private TextRenderer() {}

  static String monthYear(ReferenceMonth month) {
    return month.value().format(MONTH_YEAR);
  }

  static String month(ReferenceMonth month) {
    return month.value().format(MONTH);
  }

  static long roundK(long jobs) {
    return Math.round(jobs / 1000.0);
  }

  /** Signed thousands, e.g. {@code +143K}. */
  static String signedK(long thousands) {
    return (thousands < 0 ? "-" : "+") + Math.abs(thousands) + "K";
  }

  /** Signed thousands with the digits padded to three, e.g. {@code + 54K}. */
  static String paddedK(long thousands) {
    return String.format(Locale.US, "%s%3dK", thousands < 0 ? "-" : "+", Math.abs(thousands));
  }

  /**
   * Rounds each amount to thousands so the rounded rows add up to the rounded total: floor every
   * row, then give the remaining thousands to the rows with the largest remainders.
   */
  static long[] largestRemainderK(long[] jobs) {
    long total = 0;
    long[] shown = new long[jobs.length];
    long floorSum = 0;
    for (int i = 0; i < jobs.length; i++) {
      total += jobs[i];
      shown[i] = Math.floorDiv(jobs[i], 1000);
      floorSum += shown[i];
    }
    long remaining = roundK(total) - floorSum;
    var byRemainder =
        IntStream.range(0, jobs.length)
            .boxed()
            .sorted(
                Comparator.comparingLong((Integer i) -> Math.floorMod(jobs[i], 1000)).reversed())
            .toList();
    for (int r = 0; r < remaining; r++) {
      shown[byRemainder.get(r)]++;
    }
    return shown;
  }

  static String headline(Forecast forecast, String model, String window) {
    return monthYear(forecast.target())
        + " forecast ("
        + model
        + ", window="
        + window
        + "): "
        + signedK(roundK(forecast.point().jobs()));
  }

  /** D8's explanation block, without the headline. */
  static List<String> explanation(Forecast forecast) {
    List<Term> terms = forecast.explanation().terms();
    long[] shown = largestRemainderK(terms.stream().mapToLong(t -> t.amount().jobs()).toArray());
    var lines = new ArrayList<String>();
    for (int i = 0; i < terms.size(); i++) {
      lines.add(row(label(terms.get(i)), signedK(shown[i])));
    }
    lines.add("  " + RULE);
    lines.add(row("forecast", signedK(roundK(forecast.point().jobs()))));
    return lines;
  }

  private static String label(Term term) {
    return switch (term) {
      case Constant c -> c.name();
      case Weighted w ->
          String.format(
              Locale.US,
              "%.2f × %s (%s)",
              w.weight(),
              month(w.month()),
              paddedK(roundK(w.input().jobs())));
    };
  }

  private static String row(String label, String amount) {
    return String.format(Locale.US, "  %-28s%6s", label, amount);
  }

  /** How far to trust the forecast; a note follows when the model hasn't beaten the baseline. */
  static List<String> trust(BacktestReport report) {
    if (report.n() == 0) {
      return List.of("  backtest: not enough history");
    }
    var line = new StringBuilder("  backtest MAE ").append(maeK(report.metrics().get("MAE")));
    OptionalDouble skill = report.metrics().getOrDefault("skill", OptionalDouble.empty());
    boolean modelIsBaseline = report.model().equals(report.baseline());
    if (!modelIsBaseline) {
      line.append(" | skill vs ").append(report.baseline()).append(' ').append(skill(skill));
    }
    line.append(" | n=").append(report.n());
    var lines = new ArrayList<String>(List.of(line.toString()));
    if (!modelIsBaseline && skill.isPresent() && skill.getAsDouble() <= 0) {
      lines.add(
          "  note: "
              + report.baseline()
              + " has been at least as accurate over these "
              + report.n()
              + " months");
    }
    return lines;
  }

  static String maeK(OptionalDouble mae) {
    return mae == null || mae.isEmpty() ? "n/a" : Math.round(mae.getAsDouble() / 1000) + "K";
  }

  static String skill(OptionalDouble skill) {
    return skill == null || skill.isEmpty()
        ? "n/a"
        : String.format(Locale.US, "%+.2f", skill.getAsDouble());
  }

  /** One comparison table: fixed scoring window, one row per fit window. */
  static List<String> backtestTable(List<BacktestReport> reports) {
    var first = reports.getFirst();
    var lines = new ArrayList<String>();
    lines.add(
        first.model()
            + " vs "
            + first.baseline()
            + " · "
            + first.basis().label()
            + " · scored on "
            + first.scoringWindow());
    lines.add(String.format(Locale.US, "%-16s%5s%7s%8s", "fit window", "n", "MAE", "skill"));
    for (BacktestReport r : reports) {
      lines.add(
          String.format(
              Locale.US,
              "%-16s%5d%7s%8s",
              r.fitWindow(),
              r.n(),
              maeK(r.metrics().get("MAE")),
              skill(r.metrics().get("skill"))));
    }
    lines.add("skipped: " + first.skipped() + " of " + (first.n() + first.skipped()) + " targets");
    return lines;
  }

  /** Latest level and change per month; optionally first print and revision. */
  static List<String> historyTable(PrintHistory history, int last, boolean firstPrint) {
    var levels = history.latest();
    var changes = history.changes();
    var months = new ArrayList<>(levels.keySet());
    var shown = months.subList(Math.max(0, months.size() - last), months.size());
    var lines = new ArrayList<String>();
    String header = String.format(Locale.US, "%-10s%12s%9s", "month", "level", "change");
    lines.add(firstPrint ? header + String.format("%13s%10s", "first print", "revision") : header);
    for (ReferenceMonth month : shown) {
      JobsChange change = changes.get(month);
      String line =
          String.format(
              Locale.US,
              "%-10s%11sK%9s",
              monthYear(month),
              String.format(Locale.US, "%,d", roundK(levels.get(month))),
              change == null ? "—" : signedK(roundK(change.jobs())));
      if (firstPrint) {
        var first = history.firstPrintChange(month);
        String revision =
            first.isPresent() && change != null
                ? signedK(roundK(change.jobs() - first.get().jobs()))
                : "—";
        line +=
            String.format(
                Locale.US,
                "%13s%10s",
                first.map(f -> signedK(roundK(f.jobs()))).orElse("—"),
                revision);
      }
      lines.add(line);
    }
    return lines;
  }
}
