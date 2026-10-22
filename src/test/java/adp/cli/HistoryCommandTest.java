package adp.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import picocli.CommandLine.ExitCode;

class HistoryCommandTest {

  final InMemoryRepository repository = InMemoryRepository.withTestSeries();

  Cli run(String... args) {
    return new Cli(new HistoryCommand(repository, Fixtures.FRESH), args);
  }

  @Test
  void showsTheLastNMonthsWithLevelAndChange() {
    var cli = run("--last", "3");

    var lines = cli.out().lines().toList();
    assertThat(cli.exitCode).isZero();
    assertThat(lines).hasSize(4);
    assertThat(lines.getFirst()).isEqualTo("month            level   change");
    assertThat(lines.get(1)).startsWith("Oct 2034").matches(".*\\d{3},\\d{3}K +[+-]\\d+K");
    assertThat(lines.getLast()).startsWith("Dec 2034");
    assertThat(cli.err()).isEmpty();
  }

  @Test
  void firstPrintAddsFirstPrintAndRevisionColumns() {
    var lines = run("--last", "60", "--first-print").out().lines().toList();

    assertThat(lines.getFirst()).endsWith("first print  revision");
    // Some months in the test series were revised at the next release.
    assertThat(lines.stream().skip(1).filter(l -> !l.endsWith("+0K") && !l.endsWith("—")))
        .isNotEmpty();
    assertThat(lines.get(1)).startsWith("Jan 2030").contains("—");
  }

  @Test
  void warnsWhenTheNewestVintageIsStale() {
    var cli = new Cli(new HistoryCommand(repository, Fixtures.at(LocalDate.of(2035, 3, 1))));

    assertThat(cli.exitCode).isZero();
    assertThat(cli.err())
        .contains("warning: newest vintage is 2035-01-05 (55 days old); run adp sync");
  }

  @Test
  void rejectsANonPositiveLast() {
    assertThat(run("--last", "0").exitCode).isEqualTo(ExitCode.USAGE);
  }

  @Test
  void failsClearlyWithAnEmptyDatabase() {
    var cli = new Cli(new HistoryCommand(new InMemoryRepository(), Fixtures.FRESH));

    assertThat(cli.exitCode).isEqualTo(ExitCode.SOFTWARE);
    assertThat(cli.err().strip()).isEqualTo("error: no data stored; run make init or adp sync");
  }
}
