package adp.domain;

import java.util.Objects;

/** Inclusive range of reference months. */
public record MonthRange(ReferenceMonth from, ReferenceMonth to) {

  public MonthRange {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    if (from.compareTo(to) > 0) {
      throw new IllegalArgumentException("range starts after it ends: " + from + " > " + to);
    }
  }

  public boolean contains(ReferenceMonth month) {
    return from.compareTo(month) <= 0 && month.compareTo(to) <= 0;
  }
}
