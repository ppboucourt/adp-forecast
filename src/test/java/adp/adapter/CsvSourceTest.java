package adp.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import adp.domain.Observation;
import adp.domain.ReferenceMonth;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvSourceTest {

  static Path fixture() throws URISyntaxException {
    return Path.of(CsvSourceTest.class.getResource("/adapter/vintages.csv").toURI());
  }

  @Test
  void parsesTheFixture() throws Exception {
    var observations = new CsvSource(fixture()).fetch();

    assertThat(observations)
        .hasSize(4)
        .contains(
            new Observation(ReferenceMonth.of(2022, 8), LocalDate.of(2022, 10, 5), 132_050_000));
  }

  @Test
  void readsBackWhatTheSnapshotWriterWrote(@TempDir Path dir) throws Exception {
    var original = new CsvSource(fixture()).fetch();
    var copy = dir.resolve("copy.csv");

    new CsvSnapshotWriter().write(original, copy);

    assertThat(new CsvSource(copy).fetch()).isEqualTo(original);
  }

  @Test
  void failsClearlyWhenTheFileIsMissing(@TempDir Path dir) {
    var missing = dir.resolve("nope.csv");

    assertThatThrownBy(() -> new CsvSource(missing).fetch())
        .isInstanceOf(UncheckedIOException.class)
        .hasMessageContaining(missing.toString());
  }

  @Test
  void snapshotWriterReplacesAnExistingFile(@TempDir Path dir) throws Exception {
    var target = dir.resolve("out.csv");
    var writer = new CsvSnapshotWriter();
    writer.write(new CsvSource(fixture()).fetch(), target);

    writer.write(List.of(), target);

    assertThat(new CsvSource(target).fetch()).isEmpty();
  }
}
