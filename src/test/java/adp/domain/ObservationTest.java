package adp.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ObservationTest {

  private static final ReferenceMonth SEP = ReferenceMonth.of(2026, 9);
  private static final LocalDate RELEASE = LocalDate.of(2026, 10, 7);

  @Test
  void acceptsAValidRow() {
    var obs = new Observation(SEP, RELEASE, 132_000_000);
    assertThat(obs.level()).isEqualTo(132_000_000);
  }

  @Test
  void rejectsNullMonthOrVintage() {
    assertThatThrownBy(() -> new Observation(null, RELEASE, 1))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Observation(SEP, null, 1))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsNonPositiveLevels() {
    assertThatThrownBy(() -> new Observation(SEP, RELEASE, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Observation(SEP, RELEASE, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsVintageBeforeTheReferenceMonthStarts() {
    assertThatThrownBy(() -> new Observation(SEP, LocalDate.of(2026, 8, 31), 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(new Observation(SEP, LocalDate.of(2026, 9, 1), 1).vintageDate())
        .isEqualTo(LocalDate.of(2026, 9, 1));
  }

  @Test
  void acceptsTheRelaunchReleaseOnTheLastDayOfTheMonth() {
    var aug2022 = ReferenceMonth.of(2022, 8);

    assertThat(new Observation(aug2022, LocalDate.of(2022, 8, 31), 1).month()).isEqualTo(aug2022);
  }

  @Test
  void acceptsAReleaseBeforeTheMonthHasEnded() {
    var aug2023 = ReferenceMonth.of(2023, 8);

    assertThat(new Observation(aug2023, LocalDate.of(2023, 8, 30), 1).month()).isEqualTo(aug2023);
  }
}
