package adp.forecast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import adp.domain.MonthRange;
import adp.domain.PrintHistory;
import adp.domain.ReferenceMonth;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class RollingTest {

  // Levels Dec 2019 - Dec 2024: 61 months.
  static final PrintHistory HISTORY =
      Histories.fromChanges(ReferenceMonth.of(2020, 1), Collections.nCopies(60, 1_000L));

  @Test
  void keepsExactlyTheLastNMonthsOfLevels() {
    var windowed = new Rolling(12).apply(HISTORY);

    assertThat(windowed.latest().firstKey()).isEqualTo(ReferenceMonth.of(2024, 1));
    assertThat(windowed.latest().lastKey()).isEqualTo(ReferenceMonth.of(2024, 12));
    assertThat(windowed.latest()).hasSize(12);
  }

  @Test
  void losesOneChangeAtTheLeftEdge() {
    var changes = new Rolling(12).apply(HISTORY).changes();

    assertThat(changes).hasSize(11).doesNotContainKey(ReferenceMonth.of(2024, 1));
  }

  @Test
  void aShorterHistoryIsReturnedUnchanged() {
    assertThat(new Rolling(100).apply(HISTORY)).isSameAs(HISTORY);
    var empty = new PrintHistory(List.of());
    assertThat(new Rolling(12).apply(empty)).isSameAs(empty);
  }

  @Test
  void slidesWithTheAsOfDate() {
    var asOfMid2022 = HISTORY.asOf(ReferenceMonth.of(2022, 7).firstDay());

    var windowed = new Rolling(12).apply(asOfMid2022);

    assertThat(windowed.latest().firstKey()).isEqualTo(ReferenceMonth.of(2021, 7));
    assertThat(windowed.latest().lastKey()).isEqualTo(ReferenceMonth.of(2022, 6));
  }

  @Test
  void composesWithExcluding() {
    var covid = new MonthRange(ReferenceMonth.of(2024, 3), ReferenceMonth.of(2024, 4));

    var windowed = new Excluding(covid).andThen(new Rolling(12)).apply(HISTORY);

    assertThat(windowed.latest()).hasSize(10).doesNotContainKeys(covid.from(), covid.to());
  }

  @Test
  void monthsMustBePositive() {
    assertThatThrownBy(() -> new Rolling(0)).isInstanceOf(IllegalArgumentException.class);
  }
}
