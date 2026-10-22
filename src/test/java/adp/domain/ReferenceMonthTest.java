package adp.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class ReferenceMonthTest {

  @Test
  void rejectsNull() {
    assertThatThrownBy(() -> new ReferenceMonth(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsInvalidMonthNumbers() {
    assertThatThrownBy(() -> ReferenceMonth.of(2026, 13))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ReferenceMonth.of(2026, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsUnparseableText() {
    assertThatThrownBy(() -> ReferenceMonth.parse("2026-13"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ReferenceMonth.parse("Sep 2026"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void equalByValueAndOrdered() {
    assertThat(ReferenceMonth.parse("2026-09")).isEqualTo(ReferenceMonth.of(2026, 9));
    assertThat(ReferenceMonth.of(2026, 9)).isLessThan(ReferenceMonth.of(2026, 10));
  }

  @Test
  void navigatesAcrossYearBoundary() {
    assertThat(ReferenceMonth.of(2026, 1).previous()).isEqualTo(ReferenceMonth.of(2025, 12));
    assertThat(ReferenceMonth.of(2025, 12).next()).isEqualTo(ReferenceMonth.of(2026, 1));
  }

  @Test
  void endOfMonthIsLastDay() {
    assertThat(new ReferenceMonth(YearMonth.of(2024, 2)).endOfMonth())
        .isEqualTo(LocalDate.of(2024, 2, 29));
  }

  @Test
  void printsAsIsoYearMonth() {
    assertThat(ReferenceMonth.of(2026, 9)).hasToString("2026-09");
  }
}
