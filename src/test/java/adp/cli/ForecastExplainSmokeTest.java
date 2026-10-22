package adp.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

/**
 * Full Spring wiring: seeds a temporary database from the synthetic test series with {@code sync},
 * then checks that {@code forecast --explain} has the shape from DESIGN.md D8.
 */
@SpringBootTest(args = "--help")
class ForecastExplainSmokeTest {

  @DynamicPropertySource
  static void testDatabaseAndSeries(DynamicPropertyRegistry registry) throws IOException {
    var dir = Files.createTempDirectory("adp-smoke");
    registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dir.resolve("adp.db"));
    registry.add("adp.csv.path", () -> InMemoryRepository.testSeries().toString());
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
  void forecastExplainHasTheD8Shape() {
    run("sync", "--source", "csv");

    var lines = run("forecast", "--explain").lines().toList();

    D8Shape.assertMatches(lines);
  }
}
