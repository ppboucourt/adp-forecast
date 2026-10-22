package adp.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** Measure forecast accuracy with a walk-forward backtest. */
@Command(
    name = "backtest",
    description = "Measure forecast accuracy with a walk-forward backtest.",
    mixinStandardHelpOptions = true)
public class BacktestCommand implements Callable<Integer> {

  @Spec CommandSpec spec;

  @Override
  public Integer call() {
    spec.commandLine().getErr().println("backtest: not implemented yet");
    return ExitCode.SOFTWARE;
  }
}
