package adp.evaluation;

/** What the backtest fits on and scores against. */
public enum Basis {
  /** Point-in-time vintages, scored against first prints. The headline. */
  REAL_TIME("real-time first prints"),
  /** Latest revised levels as if published monthly. Biased: revisions inform fit and truth. */
  LATEST_VINTAGE("latest vintage (biased)");

  private final String label;

  Basis(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
