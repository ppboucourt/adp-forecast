package adp.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** Root command; prints usage when no subcommand is given. */
@Command(
    name = "adp",
    description = "Track the ADP National Employment Report and forecast the next print.",
    mixinStandardHelpOptions = true,
    version = "adp-forecast 0.1.0",
    subcommands = {
      HistoryCommand.class,
      ForecastCommand.class,
      BacktestCommand.class,
      SyncCommand.class
    })
public class AdpCommand implements Runnable {

  @Spec CommandSpec spec;

  @Override
  public void run() {
    spec.commandLine().usage(spec.commandLine().getOut());
  }
}
