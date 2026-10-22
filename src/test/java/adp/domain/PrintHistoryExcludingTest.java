package adp.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrintHistoryExcludingTest {

  @Test
  void dropsEveryVintageOfTheMonthsInRangeAndNothingElse() {
    var observations = new ArrayList<Observation>();
    for (var m = ReferenceMonth.of(2020, 1);
        m.compareTo(ReferenceMonth.of(2020, 6)) <= 0;
        m = m.next()) {
      observations.add(new Observation(m, m.next().firstDay(), 1_000));
      observations.add(new Observation(m, m.next().next().firstDay(), 1_001));
    }
    var history = new PrintHistory(observations);
    var range = new MonthRange(ReferenceMonth.of(2020, 3), ReferenceMonth.of(2020, 4));

    var trimmed = history.excluding(range);

    assertThat(trimmed.latest().keySet())
        .containsExactly(
            ReferenceMonth.of(2020, 1),
            ReferenceMonth.of(2020, 2),
            ReferenceMonth.of(2020, 5),
            ReferenceMonth.of(2020, 6));
    assertThat(trimmed.vintagesOf(ReferenceMonth.of(2020, 5)))
        .isEqualTo(history.vintagesOf(ReferenceMonth.of(2020, 5)));
    assertThat(history.latest()).hasSize(6);
  }

  @Test
  void anEmptyHistoryStaysEmpty() {
    var range = new MonthRange(ReferenceMonth.of(2020, 3), ReferenceMonth.of(2020, 4));

    assertThat(new PrintHistory(List.of()).excluding(range).latest()).isEmpty();
  }
}
