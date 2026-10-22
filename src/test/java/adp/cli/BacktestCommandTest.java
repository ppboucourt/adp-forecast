package adp.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import picocli.CommandLine.ExitCode;

class BacktestCommandTest {

  Cli run(String... args) {
    return new Cli(
        new BacktestCommand(
            InMemoryRepository.withTestSeries(),
            Fixtures.FORECASTERS,
            Fixtures.WINDOWS,
            Fixtures.METRICS,
            Fixtures.FRESH,
            "naive"),
        args);
  }

  @Test
  void comparesTheDefaultFitWindowsOnTheSameMonths() {
    var cli = run();

    var lines = cli.out().lines().toList();
    assertThat(cli.exitCode).isZero();
    assertThat(lines).first().isEqualTo("AR(3) vs naive · real-time first prints · scored on all");
    assertThat(lines.get(1)).isEqualTo("fit window          n    MAE   skill");
    assertThat(lines.get(2)).matches("all +(\\d+) +\\d+K +[+-]\\d\\.\\d{2}");
    assertThat(lines.get(3)).matches("exclude-covid +(\\d+) +\\d+K +[+-]\\d\\.\\d{2}");
    assertThat(lines.get(2).split(" +")[1]).isEqualTo(lines.get(3).split(" +")[1]);
    assertThat(lines.getLast()).matches("skipped: \\d+ of \\d+ targets");
  }

  @Test
  void latestVintageBasisIsLabelledBiased() {
    var lines = run("--basis", "latest-vintage", "--window", "all").out().lines().toList();

    assertThat(lines.getFirst()).contains("latest vintage (biased)");
    assertThat(lines).hasSize(4);
  }

  @Test
  void scoringWindowIsShownInTheHeader() {
    assertThat(run("--score", "exclude-covid").out()).contains("scored on exclude-covid");
  }

  @Test
  void unknownBasisListsTheValidOnes() {
    var cli = run("--basis", "vintage");

    assertThat(cli.exitCode).isEqualTo(ExitCode.USAGE);
    assertThat(cli.err())
        .contains("unknown basis 'vintage'; valid bases: latest-vintage, real-time");
  }
}
