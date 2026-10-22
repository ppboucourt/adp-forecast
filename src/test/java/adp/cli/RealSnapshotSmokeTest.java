package adp.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

/**
 * Full Spring wiring on the committed snapshot data/adp_vintages.csv. Checks shape only, so a
 * snapshot refresh does not break it.
 */
@SpringBootTest(args = "--help")
class RealSnapshotSmokeTest {

  @DynamicPropertySource
  static void testDatabaseAndSeries(DynamicPropertyRegistry registry) throws IOException {
    var dir = Files.createTempDirectory("adp-real-smoke");
    registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dir.resolve("adp.db"));
    registry.add(
        "adp.csv.path", () -> Path.of("data/adp_vintages.csv").toAbsolutePath().toString());
  }

  @Autowired IFactory factory;

  String run(String... args) {
    var out = new StringWriter();
    var cli = CliRunner.configure(new CommandLine(AdpCommand.class, factory));
    cli.setOut(new PrintWriter(out, true));
    cli.setErr(new PrintWriter(new StringWriter(), true));
    assertThat(cli.execute(args)).as(String.join(" ", args)).isZero();
    return out.toString();
  }

  @Test
  void forecastExplainOnTheRealSnapshotHasTheD8Shape() {
    run("sync", "--source", "csv");

    var lines = run("forecast", "--explain").lines().toList();

    D8Shape.assertMatches(lines);
  }
}
