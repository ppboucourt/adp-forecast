package adp.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** Forecast the next month's print. */
@Command(
    name = "forecast",
    description = "Forecast the next month's print.",
    mixinStandardHelpOptions = true)
public class ForecastCommand implements Callable<Integer> {

  @Spec CommandSpec spec;

  @Override
  public Integer call() {
    spec.commandLine().getErr().println("forecast: not implemented yet");
    return ExitCode.SOFTWARE;
  }
}
