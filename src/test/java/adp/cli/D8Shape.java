package adp.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.regex.Pattern;

/** The output shape of {@code forecast --explain} from DESIGN.md D8, for AR(3). */
final class D8Shape {

  private D8Shape() {}

  static void assertMatches(List<String> lines) {
    assertThat(lines.getFirst())
        .matches("[A-Z][a-z]{2} \\d{4} forecast \\(AR\\(3\\), window=exclude-covid\\): [+-]\\d+K");
    assertThat(lines.get(1)).matches("  intercept +[+-]\\d+K");
    for (int i = 2; i <= 4; i++) {
      assertThat(lines.get(i))
          .matches("  -?\\d\\.\\d{2} × [A-Z][a-z]{2} \\([+-] *\\d+K\\) +[+-]\\d+K");
    }
    assertThat(lines.get(5)).matches("  ─+");
    assertThat(lines.get(6)).matches("  forecast +[+-]\\d+K");
    assertThat(lines.get(7))
        .matches("  backtest MAE \\d+K \\| skill vs naive [+-]\\d\\.\\d{2} \\| n=\\d+");
    if (lines.size() > 8) {
      assertThat(lines.get(8)).startsWith("  note: naive has been at least as accurate");
    }

    long rows = lines.subList(1, 5).stream().mapToLong(D8Shape::amount).sum();
    assertThat(rows).isEqualTo(amount(lines.get(6))).isEqualTo(amount(lines.getFirst()));
  }

  private static long amount(String line) {
    var m = Pattern.compile("([+-]\\d+)K$").matcher(line);
    assertThat(m.find()).as(line).isTrue();
    return Long.parseLong(m.group(1));
  }
}
