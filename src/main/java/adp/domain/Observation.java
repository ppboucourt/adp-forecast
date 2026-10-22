package adp.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * One published payroll level: the value for {@code month} as published on {@code vintageDate}. A
 * level stays in force until a later vintage of the same month replaces it. ADP can publish a month
 * before it ends (2023-08 on 2023-08-30), so a vintage only has to fall on or after the month's
 * first day.
 */
public record Observation(ReferenceMonth month, LocalDate vintageDate, long level) {

  public Observation {
    Objects.requireNonNull(month, "month");
    Objects.requireNonNull(vintageDate, "vintageDate");
    if (level <= 0) {
      throw new IllegalArgumentException("level must be positive: " + level);
    }
    if (vintageDate.isBefore(month.firstDay())) {
      throw new IllegalArgumentException(
          "vintage " + vintageDate + " precedes reference month " + month);
    }
  }
}
