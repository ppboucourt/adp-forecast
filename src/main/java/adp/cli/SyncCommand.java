package adp.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** Load published vintages into the local database. */
@Command(
    name = "sync",
    description = "Load published vintages into the local database.",
    mixinStandardHelpOptions = true)
public class SyncCommand implements Callable<Integer> {

  @Spec CommandSpec spec;

  @Override
  public Integer call() {
    spec.commandLine().getErr().println("sync: not implemented yet");
    return ExitCode.SOFTWARE;
  }
}
