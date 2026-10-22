package adp.adapter;

import adp.domain.Observation;
import adp.domain.ReferenceMonth;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * The snapshot file format: one observation per row, {@code reference_month,vintage_date,level}
 * with ISO dates. Lines starting with {@code #} and blank lines are ignored.
 */
final class CsvFormat {

  static final String HEADER = "reference_month,vintage_date,level";

  private CsvFormat() {}

  static List<Observation> read(BufferedReader reader) {
    var observations = new ArrayList<Observation>();
    boolean headerSeen = false;
    int lineNumber = 0;
    try {
      for (String line; (line = reader.readLine()) != null; ) {
        lineNumber++;
        String trimmed = line.strip();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
          continue;
        }
        if (!headerSeen) {
          if (!trimmed.equals(HEADER)) {
            throw new IllegalArgumentException(
                "line " + lineNumber + ": expected header '" + HEADER + "'");
          }
          headerSeen = true;
          continue;
        }
        observations.add(parseRow(trimmed, lineNumber));
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return observations;
  }

  private static Observation parseRow(String line, int lineNumber) {
    String[] fields = line.split(",", -1);
    if (fields.length != 3) {
      throw new IllegalArgumentException(
          "line " + lineNumber + ": expected 3 fields, found " + fields.length);
    }
    try {
      return new Observation(
          ReferenceMonth.parse(fields[0].strip()),
          LocalDate.parse(fields[1].strip()),
          Long.parseLong(fields[2].strip()));
    } catch (IllegalArgumentException | DateTimeParseException e) {
      throw new IllegalArgumentException("line " + lineNumber + ": " + e.getMessage(), e);
    }
  }

  static void write(Collection<Observation> observations, Writer writer) {
    try {
      writer.write(HEADER + "\n");
      var sorted =
          observations.stream()
              .sorted(
                  Comparator.comparing(Observation::month).thenComparing(Observation::vintageDate))
              .toList();
      for (Observation o : sorted) {
        writer.write(o.month() + "," + o.vintageDate() + "," + o.level() + "\n");
      }
      writer.flush();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
