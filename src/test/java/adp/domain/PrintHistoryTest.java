package adp.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PrintHistoryTest {

  static final ReferenceMonth JUL = ReferenceMonth.of(2026, 7);
  static final ReferenceMonth AUG = ReferenceMonth.of(2026, 8);
  static final ReferenceMonth SEP = ReferenceMonth.of(2026, 9);

  // Release dates: each publishes a new month and may revise the one before it.
  static final LocalDate AUG_RELEASE = LocalDate.of(2026, 8, 5);
  static final LocalDate SEP_RELEASE = LocalDate.of(2026, 9, 3);
  static final LocalDate OCT_RELEASE = LocalDate.of(2026, 10, 7);

  static Observation obs(ReferenceMonth month, LocalDate vintage, long level) {
    return new Observation(month, vintage, level);
  }

  @Nested
  class FirstPrintChange {

    @Test
    void usesPriorMonthAsRevisedInTheSameVintage() {
      // Aug first printed at 1_100; the Sep release revises Aug to 1_080 and first prints Sep.
      var history =
          new PrintHistory(
              List.of(
                  obs(AUG, SEP_RELEASE, 1_100),
                  obs(AUG, OCT_RELEASE, 1_080),
                  obs(SEP, OCT_RELEASE, 1_200),
                  // Later revision of Sep must not leak into its first print.
                  obs(SEP, LocalDate.of(2026, 11, 4), 1_250)));

      assertThat(history.firstPrintChange(SEP)).contains(new JobsChange(120));
    }

    @Test
    void carriesPriorMonthForwardWhenItHasNoRowAtThatVintage() {
      // Sep release does not revise Aug, so Aug has no row dated OCT_RELEASE.
      var history =
          new PrintHistory(
              List.of(
                  obs(AUG, SEP_RELEASE, 1_100),
                  obs(SEP, OCT_RELEASE, 1_200),
                  // Aug revised after Sep's first print: must be ignored.
                  obs(AUG, LocalDate.of(2026, 11, 4), 1_050)));

      assertThat(history.firstPrintChange(SEP)).contains(new JobsChange(100));
    }

    @Test
    void emptyWhenPriorMonthIsMissing() {
      var history = new PrintHistory(List.of(obs(SEP, OCT_RELEASE, 1_200)));

      assertThat(history.firstPrintChange(SEP)).isEmpty();
    }

    @Test
    void emptyForUnknownMonth() {
      var history = new PrintHistory(List.of(obs(AUG, SEP_RELEASE, 1_100)));

      assertThat(history.firstPrintChange(SEP)).isEmpty();
    }
  }

  @Nested
  class AsOf {

    final PrintHistory history =
        new PrintHistory(
            List.of(
                obs(JUL, AUG_RELEASE, 1_000),
                obs(JUL, SEP_RELEASE, 1_010),
                obs(AUG, SEP_RELEASE, 1_100),
                obs(AUG, OCT_RELEASE, 1_080),
                obs(SEP, OCT_RELEASE, 1_200)));

    @Test
    void latestVintageOnOrBeforeDateForEveryMonth() {
      var view = history.asOf(SEP_RELEASE);

      assertThat(view.latest()).containsExactly(Map.entry(JUL, 1_010L), Map.entry(AUG, 1_100L));
    }

    @Test
    void excludesVintagesPublishedAfterDate() {
      var view = history.asOf(OCT_RELEASE.minusDays(1));

      assertThat(view.vintagesOf(AUG)).containsOnlyKeys(SEP_RELEASE);
      assertThat(view.vintagesOf(SEP)).isEmpty();
      assertThat(view.firstPrintChange(SEP)).isEmpty();
    }

    @Test
    void beforeAnyReleaseIsEmpty() {
      assertThat(history.asOf(AUG_RELEASE.minusDays(1)).latest()).isEmpty();
    }

    @Test
    void doesNotChangeTheOriginal() {
      history.asOf(AUG_RELEASE);

      assertThat(history.latest()).containsKeys(JUL, AUG, SEP);
    }
  }

  @Test
  void changesAreTheHeadlineSeriesFromTheLatestLevels() {
    var history =
        new PrintHistory(
            List.of(
                obs(JUL, AUG_RELEASE, 1_000),
                obs(AUG, SEP_RELEASE, 1_100),
                obs(AUG, OCT_RELEASE, 1_080),
                obs(SEP, OCT_RELEASE, 1_200)));

    assertThat(history.changes())
        .containsExactly(Map.entry(AUG, new JobsChange(80)), Map.entry(SEP, new JobsChange(120)));
  }

  @Test
  void changesSkipMonthsWithoutAPredecessor() {
    var history =
        new PrintHistory(List.of(obs(JUL, AUG_RELEASE, 1_000), obs(SEP, OCT_RELEASE, 1_200)));

    assertThat(history.changes()).isEmpty();
  }

  @Test
  void vintagesOfListsTheRevisionTrailInOrder() {
    var history =
        new PrintHistory(List.of(obs(AUG, OCT_RELEASE, 1_080), obs(AUG, SEP_RELEASE, 1_100)));

    assertThat(history.vintagesOf(AUG))
        .containsExactly(Map.entry(SEP_RELEASE, 1_100L), Map.entry(OCT_RELEASE, 1_080L));
  }

  @Test
  void rejectsTwoLevelsForTheSameMonthAndVintage() {
    assertThatThrownBy(
            () ->
                new PrintHistory(
                    List.of(obs(AUG, SEP_RELEASE, 1_100), obs(AUG, SEP_RELEASE, 1_101))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullInput() {
    assertThatThrownBy(() -> new PrintHistory(null)).isInstanceOf(NullPointerException.class);
  }
}
