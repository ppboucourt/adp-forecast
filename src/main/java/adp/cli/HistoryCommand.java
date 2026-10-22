package adp.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** Show historical prints, revisions and first prints. */
@Command(
    name = "history",
    description = "Show historical prints, revisions and first prints.",
    mixinStandardHelpOptions = true)
public class HistoryCommand implements Callable<Integer> {

  @Spec CommandSpec spec;

  @Override
  public Integer call() {
    spec.commandLine().getErr().println("history: not implemented yet");
    return ExitCode.SOFTWARE;
  }
}
