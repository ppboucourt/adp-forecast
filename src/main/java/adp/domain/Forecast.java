package adp.domain;

import java.util.Objects;

/** A point forecast for {@code target}; the point always equals the sum of its explanation. */
public record Forecast(ReferenceMonth target, JobsChange point, Explanation explanation) {

  public Forecast {
    Objects.requireNonNull(target, "target");
    Objects.requireNonNull(point, "point");
    Objects.requireNonNull(explanation, "explanation");
    if (!point.equals(explanation.total())) {
      throw new IllegalArgumentException(
          "point " + point + " differs from explanation total " + explanation.total());
    }
  }

  public static Forecast of(ReferenceMonth target, Explanation explanation) {
    return new Forecast(target, explanation.total(), explanation);
  }
}
