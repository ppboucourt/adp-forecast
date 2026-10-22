package adp.adapter;

import adp.domain.Observation;
import adp.port.SnapshotWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Writes observations in {@link CsvFormat}, replacing the target file. Header comments (e.g. the
 * data citation) are written on every write, so a refreshed snapshot keeps them.
 */
public class CsvSnapshotWriter implements SnapshotWriter {

  private final List<String> headerComments;

  public CsvSnapshotWriter() {
    this(List.of());
  }

  public CsvSnapshotWriter(List<String> headerComments) {
    this.headerComments = List.copyOf(headerComments);
  }

  @Override
  public void write(Collection<Observation> observations, Path target) {
    try (var writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
      CsvFormat.write(observations, writer, headerComments);
    } catch (IOException e) {
      throw new UncheckedIOException("cannot write " + target, e);
    }
  }
}
