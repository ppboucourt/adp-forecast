package adp.evaluation;

import adp.forecast.HistoryWindow;
import java.util.Objects;

/** A history window with the name it is reported under. */
public record NamedWindow(String name, HistoryWindow window) {

  public NamedWindow {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(window, "window");
  }
}
