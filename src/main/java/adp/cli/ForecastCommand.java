package adp.cli;

import adp.domain.PrintHistory;
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

/**
 * Forecasts the month after the latest one stored. With {@code --explain}, shows the terms and a
 * live real-time backtest of the same model and fit window.
 */
@Command(
    name = "forecast",
    description = "Forecast the next month's print.",
    mixinStandardHelpOptions = true)
public class ForecastCommand implements Callable<Integer> {

  @Spec CommandSpec spec;

  @Option(
      names = "--model",
      paramLabel = "<name>",
      defaultValue = "ar",
      description = "Model to use. Default: ar.")
  String model;

  @Option(
      names = "--window",
      paramLabel = "<name>",
      defaultValue = "exclude-covid",
      description = "Fit window. Default: exclude-covid.")
  String window;

  @Option(names = "--explain", description = "Show how the forecast was built and its accuracy.")
  boolean explain;

  private final PrintRepository repository;
  private final Map<String, Forecaster> forecasters;
  private final Map<String, HistoryWindow> windows;
  private final List<AccuracyMetric> metrics;
  private final Clock clock;
  private final String baselineName;

  public ForecastCommand(
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
    HistoryWindow fitWindow = Names.pick(windows, window, "window", cli);
    Forecaster baseline = Names.pick(forecasters, baselineName, "model", cli);
    PrintHistory history = StoredHistory.load(repository, clock, cli.getErr());

    var forecast = forecaster.forecast(fitWindow.apply(history));
    var out = cli.getOut();
    out.println(TextRenderer.headline(forecast, forecaster.name(), window));
    if (explain) {
      TextRenderer.explanation(forecast).forEach(out::println);
      var report =
          new Backtester(forecaster, baseline, new NamedWindow(window, fitWindow), metrics)
              .run(history, Basis.REAL_TIME, new NamedWindow("all", h -> h));
      TextRenderer.trust(report).forEach(out::println);
    }
    return ExitCode.OK;
  }
}
