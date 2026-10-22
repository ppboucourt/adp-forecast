package adp.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JobsChangeTest {

  @Test
  void betweenIsToMinusFrom() {
    assertThat(JobsChange.between(1_000, 1_143)).isEqualTo(new JobsChange(143));
    assertThat(JobsChange.between(1_143, 1_000)).isEqualTo(new JobsChange(-143));
  }
}
