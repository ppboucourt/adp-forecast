package adp.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import adp.domain.Observation;
import adp.domain.ReferenceMonth;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvFormatTest {

  static final ReferenceMonth AUG = ReferenceMonth.of(2022, 8);

  static List<Observation> read(String text) {
    return CsvFormat.read(new BufferedReader(new StringReader(text)));
  }

  @Test
  void writesHeaderThenRowsSortedByMonthAndVintage() {
    var out = new StringWriter();
    CsvFormat.write(
        List.of(
            new Observation(AUG, LocalDate.of(2022, 10, 5), 132_050_000),
            new Observation(AUG, LocalDate.of(2022, 8, 31), 132_032_000)),
        out);

    assertThat(out.toString())
        .isEqualTo(
            """
            reference_month,vintage_date,level
            2022-08,2022-08-31,132032000
            2022-08,2022-10-05,132050000
            """);
  }

  @Test
  void writesCommentLinesBeforeTheHeaderAndReadsPastThem() {
    var rows = List.of(new Observation(AUG, LocalDate.of(2022, 8, 31), 132_032_000));
    var out = new StringWriter();

    CsvFormat.write(rows, out, List.of("Source: example", "Retrieved 2026-10-22"));

    assertThat(out.toString())
        .startsWith(
            """
            # Source: example
            # Retrieved 2026-10-22
            reference_month,vintage_date,level
            """);
    assertThat(read(out.toString())).isEqualTo(rows);
  }

  @Test
  void readsWhatItWrites() {
    var rows =
        List.of(
            new Observation(AUG, LocalDate.of(2022, 8, 31), 132_032_000),
            new Observation(AUG.next(), LocalDate.of(2022, 10, 5), 132_258_000));
    var out = new StringWriter();
    CsvFormat.write(rows, out);

    assertThat(read(out.toString())).isEqualTo(rows);
  }

  @Test
  void skipsCommentsAndBlankLines() {
    assertThat(
            read(
                """
                # sample
                reference_month,vintage_date,level

                # trailing note
                2022-08,2022-08-31,132032000
                """))
        .containsExactly(new Observation(AUG, LocalDate.of(2022, 8, 31), 132_032_000));
  }

  @Test
  void rejectsAMissingOrWrongHeader() {
    assertThatThrownBy(() -> read("2022-08,2022-08-31,132032000\n"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("header");
  }

  @Test
  void reportsTheLineOfAMalformedRow() {
    assertThatThrownBy(() -> read("reference_month,vintage_date,level\n2022-08,2022-08-31,abc\n"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("line 2");
    assertThatThrownBy(() -> read("reference_month,vintage_date,level\n2022-08,2022-08-31\n"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("line 2");
  }
}
