package adp.cli;

import static org.assertj.core.api.Assertions.assertThat;

import adp.domain.Observation;
import adp.domain.ReferenceMonth;
import adp.port.PrintRepository;
import adp.port.PrintSource;
import adp.port.SnapshotWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/** Plain picocli execution with fakes; no Spring context. */
class SyncCommandTest {

  static final List<Observation> ROWS =
      List.of(
          new Observation(ReferenceMonth.of(2022, 8), LocalDate.of(2022, 8, 31), 132_032_000),
          new Observation(ReferenceMonth.of(2022, 9), LocalDate.of(2022, 10, 5), 132_258_000));

  static class InMemoryRepository implements PrintRepository {
    final Set<Observation> stored = new LinkedHashSet<>();

    @Override
    public int saveAll(Collection<Observation> observations) {
      int added = 0;
      for (Observation o : observations) {
        added += stored.add(o) ? 1 : 0;
      }
      return added;
    }

    @Override
    public List<Observation> findAll() {
      return List.copyOf(stored);
    }
  }

  static class RecordingWriter implements SnapshotWriter {
    final List<Object> calls = new ArrayList<>();

    @Override
    public void write(Collection<Observation> observations, Path target) {
      calls.add(List.of(List.copyOf(observations), target));
    }
  }

  final InMemoryRepository repository = new InMemoryRepository();
  final RecordingWriter writer = new RecordingWriter();
  final Map<String, PrintSource> sources =
      Map.of(
          "csv",
          () -> ROWS,
          "fred",
          () -> {
            throw new IllegalStateException("FRED_API_KEY is not set");
          });
  final StringWriter out = new StringWriter();
  final StringWriter err = new StringWriter();

  int run(String... args) {
    var cli = CliRunner.configure(new CommandLine(new SyncCommand(sources, repository, writer)));
    cli.setOut(new PrintWriter(out, true));
    cli.setErr(new PrintWriter(err, true));
    return cli.execute(args);
  }

  @Test
  void reportsNewObservationsThenZeroOnTheSecondRun() {
    assertThat(run("--source", "csv")).isZero();
    assertThat(out.toString()).contains("2 read from csv, 2 new");

    assertThat(run("--source", "csv")).isZero();
    assertThat(out.toString()).contains("2 read from csv, 0 new");
    assertThat(repository.stored).containsExactlyElementsOf(ROWS);
  }

  @Test
  void exportsWhatTheSourceReturned() {
    assertThat(run("--source", "csv", "--export", "out.csv")).isZero();

    assertThat(writer.calls).containsExactly(List.of(ROWS, Path.of("out.csv")));
    assertThat(out.toString()).contains("exported 2 observations to out.csv");
  }

  @Test
  void listsValidSourcesForAnUnknownName() {
    assertThat(run("--source", "bls")).isEqualTo(CommandLine.ExitCode.USAGE);

    assertThat(err.toString()).contains("unknown source 'bls'", "csv, fred");
    assertThat(repository.stored).isEmpty();
  }

  @Test
  void reportsSourceFailuresAsOneLine() {
    assertThat(run("--source", "fred")).isEqualTo(CommandLine.ExitCode.SOFTWARE);

    assertThat(err.toString().strip()).isEqualTo("error: FRED_API_KEY is not set");
    assertThat(repository.stored).isEmpty();
  }

  @Test
  void defaultsToTheOfflineCsvSource() {
    assertThat(run()).isZero();

    assertThat(out.toString()).contains("read from csv");
  }
}
