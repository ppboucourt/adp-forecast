package adp.cli;

import adp.domain.Observation;
import adp.port.PrintRepository;
import adp.port.PrintSource;
import adp.port.SnapshotWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/** Loads every vintage a source publishes into the repository; existing vintages are kept. */
@Command(
    name = "sync",
    description = "Load published vintages into the local database.",
    mixinStandardHelpOptions = true)
public class SyncCommand implements Callable<Integer> {

  @Spec CommandSpec spec;

  @Option(
      names = "--source",
      paramLabel = "<name>",
      defaultValue = "csv",
      description = "Where to read vintages from: csv (bundled snapshot) or fred. Default: csv.")
  String source;

  @Option(
      names = "--export",
      paramLabel = "<path>",
      description = "Also write the fetched observations to this CSV snapshot file.")
  Path export;

  private final Map<String, PrintSource> sources;
  private final PrintRepository repository;
  private final SnapshotWriter snapshotWriter;

  public SyncCommand(
      Map<String, PrintSource> sources, PrintRepository repository, SnapshotWriter snapshotWriter) {
    this.sources = sources;
    this.repository = repository;
    this.snapshotWriter = snapshotWriter;
  }

  @Override
  public Integer call() {
    PrintSource selected = sources.get(source);
    if (selected == null) {
      spec.commandLine()
          .getErr()
          .println(
              "unknown source '"
                  + source
                  + "'; valid sources: "
                  + String.join(", ", new TreeSet<>(sources.keySet())));
      return ExitCode.USAGE;
    }
    List<Observation> observations = selected.fetch();
    int added = repository.saveAll(observations);
    var out = spec.commandLine().getOut();
    out.println("sync: " + observations.size() + " read from " + source + ", " + added + " new");
    if (export != null) {
      snapshotWriter.write(observations, export);
      out.println("exported " + observations.size() + " observations to " + export);
    }
    return ExitCode.OK;
  }
}
