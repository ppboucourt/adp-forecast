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
  void rejectsVintageBeforeTheMonthHasEnded() {
    assertThatThrownBy(() -> new Observation(SEP, LocalDate.of(2026, 9, 30), 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(new Observation(SEP, LocalDate.of(2026, 10, 1), 1).vintageDate())
        .isEqualTo(LocalDate.of(2026, 10, 1));
  }
}
