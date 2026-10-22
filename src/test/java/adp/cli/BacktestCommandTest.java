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
  void baselineOptionReplacesNaiveInTheHeaderAndTheSkill() {
    var vsNaive = run("--window", "all").out().lines().toList();
    var vsMa = run("--window", "all", "--baseline", "ma").out().lines().toList();

    assertThat(vsMa.getFirst()).startsWith("AR(3) vs MA(3) · ");
    // Same model, same months: MAE matches, skill differs because the baseline changed.
    assertThat(vsMa.get(2).split(" +")[1]).isEqualTo(vsNaive.get(2).split(" +")[1]);
    assertThat(vsMa.get(2).split(" +")[2]).isEqualTo(vsNaive.get(2).split(" +")[2]);
    assertThat(vsMa.get(2).split(" +")[3]).isNotEqualTo(vsNaive.get(2).split(" +")[3]);
  }

  @Test
  void baselineDefaultsToTheConfiguredOne() {
    assertThat(run().out().lines().findFirst())
        .hasValue("AR(3) vs naive · real-time first prints · scored on all");
  }

  @Test
  void unknownBaselineListsTheValidModels() {
    var cli = run("--baseline", "mean");

    assertThat(cli.exitCode).isEqualTo(ExitCode.USAGE);
    assertThat(cli.err()).contains("unknown model 'mean'; valid models: ar, ma, naive");
  }

  @Test
  void unknownBasisListsTheValidOnes() {
    var cli = run("--basis", "vintage");

    assertThat(cli.exitCode).isEqualTo(ExitCode.USAGE);
    assertThat(cli.err())
        .contains("unknown basis 'vintage'; valid bases: latest-vintage, real-time");
  }
}
