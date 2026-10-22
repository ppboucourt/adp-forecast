package adp.cli;

import adp.port.PrintRepository;
import java.time.Clock;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

/** Prints recent months as currently published, optionally with first prints and revisions. */
@Command(
    name = "history",
    description = "Show historical prints, revisions and first prints.",
    mixinStandardHelpOptions = true)
public class HistoryCommand implements Callable<Integer> {

  @Spec CommandSpec spec;

  @Option(
      names = "--last",
      paramLabel = "<N>",
      defaultValue = "12",
      description = "Number of most recent months to show. Default: 12.")
  int last;

  @Option(
      names = "--first-print",
      description = "Also show each month's first-print change and its revision since.")
  boolean firstPrint;

  private final PrintRepository repository;
  private final Clock clock;

  public HistoryCommand(PrintRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Override
  public Integer call() {
    if (last < 1) {
      throw new ParameterException(spec.commandLine(), "--last must be at least 1");
    }
    var history = StoredHistory.load(repository, clock, spec.commandLine().getErr());
    TextRenderer.historyTable(history, last, firstPrint)
        .forEach(spec.commandLine().getOut()::println);
    return ExitCode.OK;
  }
}
