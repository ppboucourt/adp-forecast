package adp.domain;

/** Month-over-month change in private payroll employment, in jobs (persons). */
public record JobsChange(long jobs) {

  public static JobsChange between(long fromLevel, long toLevel) {
    return new JobsChange(toLevel - fromLevel);
  }
}
