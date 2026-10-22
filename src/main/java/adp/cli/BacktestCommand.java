package adp.cli;

import adp.evaluation.AccuracyMetric;
import adp.evaluation.Backtester;
import adp.evaluation.Basis;
import adp.evaluation.NamedWindow;
import adp.forecast.Forecaster;
import adp.forecast.HistoryWindow;
import adp.port.PrintRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.springframework.beans.factory.annotation.Value;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/** Walk-forward backtest; one row per fit window, all scored on the same months. */
@Command(
    name = "backtest",
    description = "Measure forecast accuracy with a walk-forward backtest.",
    mixinStandardHelpOptions = true)
public class BacktestCommand implements Callable<Integer> {

  private static final Map<String, Basis> BASES =
      Map.of("real-time", Basis.REAL_TIME, "latest-vintage", Basis.LATEST_VINTAGE);

  @Spec CommandSpec spec;

  @Option(
      names = "--model",
      paramLabel = "<name>",
      defaultValue = "ar",
      description = "Model to evaluate against the baseline. Default: ar.")
  String model;

  @Option(
      names = "--window",
      paramLabel = "<name>",
      description = "Fit window; repeat to compare. Default: all and exclude-covid.")
  List<String> fitWindows;

  @Option(
      names = "--score",
      paramLabel = "<name>",
      defaultValue = "all",
      description = "Scoring window, fixed for the whole table. Default: all.")
  String scoringWindow;

  @Option(
      names = "--basis",
      paramLabel = "<name>",
      defaultValue = "real-time",
      description =
          "real-time (first prints, the headline) or latest-vintage (biased). Default: real-time.")
  String basis;

  private final PrintRepository repository;
  private final Map<String, Forecaster> forecasters;
  private final Map<String, HistoryWindow> windows;
  private final List<AccuracyMetric> metrics;
  private final Clock clock;
  private final String baselineName;

  public BacktestCommand(
      PrintRepository repository,
      Map<String, Forecaster> forecasters,
      Map<String, HistoryWindow> windows,
      List<AccuracyMetric> metrics,
      Clock clock,
      @Value("${adp.baseline:naive}") String baselineName) {
    this.repository = repository;
    this.forecasters = forecasters;
    this.windows = windows;
    this.metrics = metrics;
    this.clock = clock;
    this.baselineName = baselineName;
  }

  @Override
  public Integer call() {
    var cli = spec.commandLine();
    Forecaster forecaster = Names.pick(forecasters, model, "model", cli);
    Forecaster baseline = Names.pick(forecasters, baselineName, "model", cli);
    Basis selectedBasis = Names.pick(BASES, basis, "basis", "bases", cli);
    var scoring = new NamedWindow(scoringWindow, Names.pick(windows, scoringWindow, "window", cli));
    var names = fitWindows == null ? List.of("all", "exclude-covid") : fitWindows;
    var cells =
        names.stream()
            .map(
                name ->
                    new Backtester(
                        forecaster,
                        baseline,
                        new NamedWindow(name, Names.pick(windows, name, "window", cli)),
                        metrics))
            .toList();

    var history = StoredHistory.load(repository, clock, cli.getErr());
    var reports = Backtester.compare(cells, history, selectedBasis, scoring);
    TextRenderer.backtestTable(reports).forEach(cli.getOut()::println);
    return ExitCode.OK;
  }
}
