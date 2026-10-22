package adp.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PrintHistoryLatestOnlyTest {

  static final ReferenceMonth JUL = ReferenceMonth.of(2022, 7);
  static final ReferenceMonth AUG = ReferenceMonth.of(2022, 8);
  static final ReferenceMonth SEP = ReferenceMonth.of(2022, 9);

  // Jul and Aug share the relaunch vintage; Aug is revised when Sep is first published.
  static final PrintHistory HISTORY =
      new PrintHistory(
          List.of(
              new Observation(JUL, LocalDate.of(2022, 8, 31), 1_000),
              new Observation(AUG, LocalDate.of(2022, 8, 31), 1_100),
              new Observation(AUG, LocalDate.of(2022, 10, 5), 1_080),
              new Observation(SEP, LocalDate.of(2022, 10, 5), 1_200)));

  @Test
  void keepsOneVintagePerMonthWithItsLatestLevelOnTheFirstOfTheNextMonth() {
    var latest = HISTORY.latestOnly();

    assertThat(latest.vintagesOf(JUL)).containsExactly(Map.entry(AUG.firstDay(), 1_000L));
    assertThat(latest.vintagesOf(AUG)).containsExactly(Map.entry(SEP.firstDay(), 1_080L));
    assertThat(latest.vintagesOf(SEP)).containsExactly(Map.entry(SEP.next().firstDay(), 1_200L));
  }

  @Test
  void firstPrintsBecomeLatestVintageChanges() {
    var latest = HISTORY.latestOnly();

    assertThat(latest.firstPrintChange(AUG)).contains(new JobsChange(80));
    assertThat(latest.firstPrintChange(SEP)).contains(new JobsChange(120));
    assertThat(HISTORY.firstPrintChange(SEP)).contains(new JobsChange(120));
    assertThat(HISTORY.firstPrintChange(AUG)).contains(new JobsChange(100));
  }

  @Test
  void isMarkedAndTheMarkSurvivesAsOfAndExcluding() {
    var latest = HISTORY.latestOnly();

    assertThat(HISTORY.isLatestOnly()).isFalse();
    assertThat(latest.isLatestOnly()).isTrue();
    assertThat(latest.asOf(LocalDate.of(2022, 9, 15)).isLatestOnly()).isTrue();
    assertThat(latest.excluding(new MonthRange(JUL, JUL)).isLatestOnly()).isTrue();
    assertThat(HISTORY.asOf(LocalDate.of(2022, 9, 15)).isLatestOnly()).isFalse();
  }
}
