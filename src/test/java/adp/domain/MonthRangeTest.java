package adp.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MonthRangeTest {

  static final MonthRange COVID =
      new MonthRange(ReferenceMonth.of(2020, 3), ReferenceMonth.of(2021, 12));

  @Test
  void containsBothEnds() {
    assertThat(COVID.contains(ReferenceMonth.of(2020, 3))).isTrue();
    assertThat(COVID.contains(ReferenceMonth.of(2021, 12))).isTrue();
    assertThat(COVID.contains(ReferenceMonth.of(2020, 2))).isFalse();
    assertThat(COVID.contains(ReferenceMonth.of(2022, 1))).isFalse();
  }

  @Test
  void aSingleMonthRangeIsValid() {
    var month = ReferenceMonth.of(2020, 4);

    assertThat(new MonthRange(month, month).contains(month)).isTrue();
  }

  @Test
  void rejectsReversedOrMissingEnds() {
    assertThatThrownBy(
            () -> new MonthRange(ReferenceMonth.of(2021, 12), ReferenceMonth.of(2020, 3)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new MonthRange(null, ReferenceMonth.of(2020, 3)))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new MonthRange(ReferenceMonth.of(2020, 3), null))
        .isInstanceOf(NullPointerException.class);
  }
}
