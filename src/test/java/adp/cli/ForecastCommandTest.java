package adp.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import picocli.CommandLine.ExitCode;

class ForecastCommandTest {

  Cli run(String... args) {
    return new Cli(
        new ForecastCommand(
            InMemoryRepository.withTestSeries(),
            Fixtures.FORECASTERS,
            Fixtures.WINDOWS,
            Fixtures.METRICS,
            Fixtures.FRESH,
            "naive"),
        args);
  }

  @Test
  void defaultsToArWithTheCovidExclusionAndPrintsOneLine() {
    var cli = run();

    assertThat(cli.exitCode).isZero();
    assertThat(cli.out().lines().toList())
        .singleElement()
        .asString()
        .matches("Jan 2035 forecast \\(AR\\(3\\), window=exclude-covid\\): [+-]\\d+K");
  }

  @Test
  void naiveForecastIsTheLastChange() {
    var cli = run("--model", "naive", "--window", "all", "--explain");

    var lines = cli.out().lines().toList();
    assertThat(lines.getFirst()).startsWith("Jan 2035 forecast (naive, window=all): ");
    assertThat(lines.get(1)).startsWith("  1.00 × Dec (");
    assertThat(lines.getLast()).matches("  backtest MAE \\d+K \\| n=\\d+");
  }

  @Test
  void baselineOptionChangesWhatTheExplainSkillIsMeasuredAgainst() {
    var lines = run("--explain", "--baseline", "ma").out().lines().toList();

    assertThat(lines)
        .anyMatch(
            l ->
                l.matches(
                    "  backtest MAE \\d+K \\| skill vs MA\\(3\\) [+-]\\d\\.\\d{2} \\| n=\\d+"))
        .noneMatch(l -> l.contains("skill vs naive"));
  }

  @Test
  void unknownNamesListTheValidOnes() {
    var model = run("--model", "prophet");
    var window = run("--window", "rolling");

    assertThat(model.exitCode).isEqualTo(ExitCode.USAGE);
    assertThat(model.err()).contains("unknown model 'prophet'; valid models: ar, ma, naive");
    assertThat(window.exitCode).isEqualTo(ExitCode.USAGE);
    assertThat(window.err())
        .contains("unknown window 'rolling'; valid windows: all, exclude-covid");
  }
}
