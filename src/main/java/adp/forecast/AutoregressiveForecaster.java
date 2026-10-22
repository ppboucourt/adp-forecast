package adp.forecast;

import adp.domain.Explanation;
import adp.domain.Explanation.Constant;
import adp.domain.Explanation.Term;
import adp.domain.Explanation.Weighted;
import adp.domain.Forecast;
import adp.domain.JobsChange;
import adp.domain.PrintHistory;
import adp.domain.ReferenceMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import org.apache.commons.math3.linear.SingularMatrixException;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

/**
 * AR(p) on monthly changes, fitted by OLS inside every call: {@code y_t = c + Σ φ_i y_{t-i}}. The
 * forecast is explained as the intercept plus one weighted term per lag.
 */
public class AutoregressiveForecaster implements Forecaster {

  private final int order;

  public AutoregressiveForecaster(int order) {
    if (order < 1) {
      throw new IllegalArgumentException("AR order must be at least 1: " + order);
    }
    this.order = order;
  }

  @Override
  public String name() {
    return "AR(" + order + ")";
  }

  @Override
  public Forecast forecast(PrintHistory history) {
    var levels = history.latest();
    if (levels.isEmpty()) {
      throw new InsufficientHistoryException(name() + " needs history; history is empty");
    }
    var target = levels.lastKey().next();
    var changes = history.changes();
    var lags = lagsOf(target);
    for (ReferenceMonth lag : lags) {
      if (!changes.containsKey(lag)) {
        throw new InsufficientHistoryException(
            name()
                + " needs changes for the "
                + order
                + " months before "
                + target
                + "; missing "
                + lag);
      }
    }

    var rows = regressionTargets(changes, order);
    if (rows.size() <= order + 1) {
      throw new InsufficientHistoryException(
          name() + " needs more than " + (order + 1) + " complete rows; found " + rows.size());
    }
    double[] coefficients = fit(changes, rows);

    var terms = new ArrayList<Term>();
    terms.add(new Constant("intercept", new JobsChange(Math.round(coefficients[0]))));
    for (int i = 0; i < order; i++) {
      var lag = lags.get(i);
      terms.add(new Weighted(coefficients[i + 1], lag, changes.get(lag)));
    }
    return Forecast.of(target, new Explanation(terms));
  }

  /**
   * Months usable as regression targets: the month and its {@code order} preceding months all have
   * a change, so no row spans a gap in the series.
   */
  static List<ReferenceMonth> regressionTargets(
      NavigableMap<ReferenceMonth, JobsChange> changes, int order) {
    var targets = new ArrayList<ReferenceMonth>();
    for (ReferenceMonth month : changes.keySet()) {
      boolean complete = true;
      var lag = month;
      for (int i = 0; i < order && complete; i++) {
        lag = lag.previous();
        complete = changes.containsKey(lag);
      }
      if (complete) {
        targets.add(month);
      }
    }
    return targets;
  }

  /** Most recent first: {@code target-1, target-2, ..., target-order}. */
  private List<ReferenceMonth> lagsOf(ReferenceMonth target) {
    var lags = new ArrayList<ReferenceMonth>(order);
    var lag = target;
    for (int i = 0; i < order; i++) {
      lag = lag.previous();
      lags.add(lag);
    }
    return lags;
  }

  /** Returns {@code [c, φ_1, ..., φ_order]}. */
  private double[] fit(
      NavigableMap<ReferenceMonth, JobsChange> changes, List<ReferenceMonth> rows) {
    double[] y = new double[rows.size()];
    double[][] x = new double[rows.size()][order];
    for (int r = 0; r < rows.size(); r++) {
      var month = rows.get(r);
      y[r] = changes.get(month).jobs();
      var lags = lagsOf(month);
      for (int i = 0; i < order; i++) {
        x[r][i] = changes.get(lags.get(i)).jobs();
      }
    }
    var ols = new OLSMultipleLinearRegression();
    ols.newSampleData(y, x);
    try {
      return ols.estimateRegressionParameters();
    } catch (SingularMatrixException e) {
      throw new IllegalStateException(name() + " cannot be fitted: the lags are collinear", e);
    }
  }
}
