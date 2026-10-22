package adp.adapter;

import adp.domain.Observation;
import adp.port.PrintSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Reads observations from a snapshot file in {@link CsvFormat}. */
public class CsvSource implements PrintSource {

  private final Path path;

  public CsvSource(Path path) {
    this.path = path;
  }

  @Override
  public List<Observation> fetch() {
    try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      return CsvFormat.read(reader);
    } catch (IOException e) {
      throw new UncheckedIOException("cannot read " + path, e);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(path + ", " + e.getMessage(), e);
    }
  }
}
