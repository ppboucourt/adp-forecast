package adp.adapter;

import adp.domain.Observation;
import adp.port.SnapshotWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

/** Writes observations in {@link CsvFormat}, replacing the target file. */
public class CsvSnapshotWriter implements SnapshotWriter {

  @Override
  public void write(Collection<Observation> observations, Path target) {
    try (var writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
      CsvFormat.write(observations, writer);
    } catch (IOException e) {
      throw new UncheckedIOException("cannot write " + target, e);
    }
  }
}
