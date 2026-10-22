package adp.domain;

import java.util.List;
import java.util.Objects;

/**
 * How a forecast was built: a list of terms whose amounts add up to the point forecast. Terms come
 * in two shapes, so any linear model can be rendered without knowing which model produced it.
 */
public record Explanation(List<Term> terms) {

  public Explanation {
    terms = List.copyOf(terms);
    if (terms.isEmpty()) {
      throw new IllegalArgumentException("an explanation needs at least one term");
    }
  }

  public JobsChange total() {
    return new JobsChange(terms.stream().mapToLong(t -> t.amount().jobs()).sum());
  }

  /** One additive part of a forecast, in whole jobs. */
  public sealed interface Term permits Constant, Weighted {
    JobsChange amount();
  }

  /** A fixed amount, such as a regression intercept. */
  public record Constant(String name, JobsChange amount) implements Term {

    public Constant {
      Objects.requireNonNull(amount, "amount");
      if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("constant term needs a name");
      }
    }
  }

  /** {@code weight × input}, where input is the change published for {@code month}. */
  public record Weighted(double weight, ReferenceMonth month, JobsChange input) implements Term {

    public Weighted {
      Objects.requireNonNull(month, "month");
      Objects.requireNonNull(input, "input");
      if (!Double.isFinite(weight)) {
        throw new IllegalArgumentException("weight must be finite: " + weight);
      }
    }

    /** Rounded to whole jobs by {@link Math#round(double)}, so halves round up. */
    @Override
    public JobsChange amount() {
      return new JobsChange(Math.round(weight * input.jobs()));
    }
  }
}
