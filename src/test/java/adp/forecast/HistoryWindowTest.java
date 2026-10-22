package adp.forecast;

import static org.assertj.core.api.Assertions.assertThat;

import adp.domain.MonthRange;
import adp.domain.PrintHistory;
import adp.domain.ReferenceMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class HistoryWindowTest {

  static final MonthRange COVID =
      new MonthRange(ReferenceMonth.of(2020, 3), ReferenceMonth.of(2021, 12));

  static final PrintHistory HISTORY =
      Histories.fromChanges(ReferenceMonth.of(2019, 1), Collections.nCopies(60, 1_000L));

  @Test
  void allHistoryReturnsTheHistoryUnchanged() {
    assertThat(new AllHistory().apply(HISTORY)).isSameAs(HISTORY);
  }

  @Test
  void excludingCovidRemovesExactlyMarch2020ThroughDecember2021() {
    var windowed = new Excluding(COVID).apply(HISTORY);

    var removed = new ArrayList<>(HISTORY.latest().keySet());
    removed.removeAll(windowed.latest().keySet());
    assertThat(removed).hasSize(22).first().isEqualTo(ReferenceMonth.of(2020, 3));
    assertThat(removed.getLast()).isEqualTo(ReferenceMonth.of(2021, 12));
    assertThat(windowed.latest())
        .containsKeys(ReferenceMonth.of(2020, 2), ReferenceMonth.of(2022, 1));
  }

  @Test
  void excludingCovidAlsoLosesTheJanuary2022Change() {
    var changes = new Excluding(COVID).apply(HISTORY).changes();

    assertThat(changes)
        .containsKey(ReferenceMonth.of(2020, 2))
        .containsKey(ReferenceMonth.of(2022, 2));
    assertThat(changes).doesNotContainKey(ReferenceMonth.of(2022, 1));
  }

  @Test
  void andThenAppliesThisWindowFirst() {
    var calls = new ArrayList<String>();
    HistoryWindow first =
        h -> {
          calls.add("first");
          return h;
        };
    HistoryWindow second =
        h -> {
          calls.add("second");
          return h;
        };

    first.andThen(second).apply(HISTORY);

    assertThat(calls).containsExactly("first", "second");
  }

  @Test
  void andThenFeedsTheFirstResultIntoTheSecond() {
    var early = new MonthRange(ReferenceMonth.of(2019, 1), ReferenceMonth.of(2019, 6));
    HistoryWindow keepOnlyIfEarlyGone =
        h -> h.latest().containsKey(ReferenceMonth.of(2019, 1)) ? new PrintHistory(List.of()) : h;

    var result = new Excluding(early).andThen(keepOnlyIfEarlyGone).apply(HISTORY);

    assertThat(result.latest()).isNotEmpty().doesNotContainKey(ReferenceMonth.of(2019, 1));
  }
}
