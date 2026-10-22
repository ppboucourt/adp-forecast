package adp.domain;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

/** The calendar month a print measures, e.g. 2026-09. */
public record ReferenceMonth(YearMonth value) implements Comparable<ReferenceMonth> {

  public ReferenceMonth {
    Objects.requireNonNull(value, "value");
  }

  public static ReferenceMonth of(int year, int month) {
    try {
      return new ReferenceMonth(YearMonth.of(year, month));
    } catch (DateTimeException e) {
      throw new IllegalArgumentException("invalid reference month: " + year + "-" + month, e);
    }
  }

  /** Parses ISO {@code yyyy-MM}. */
  public static ReferenceMonth parse(String text) {
    try {
      return new ReferenceMonth(YearMonth.parse(text));
    } catch (DateTimeException e) {
      throw new IllegalArgumentException("invalid reference month: " + text, e);
    }
  }

  public ReferenceMonth previous() {
    return new ReferenceMonth(value.minusMonths(1));
  }

  public ReferenceMonth next() {
    return new ReferenceMonth(value.plusMonths(1));
  }

  public LocalDate endOfMonth() {
    return value.atEndOfMonth();
  }

  @Override
  public int compareTo(ReferenceMonth other) {
    return value.compareTo(other.value);
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
