package adp.forecast;

/** The history lacks months a model needs, e.g. lags just after an excluded window. */
public class InsufficientHistoryException extends RuntimeException {

  public InsufficientHistoryException(String message) {
    super(message);
  }
}
