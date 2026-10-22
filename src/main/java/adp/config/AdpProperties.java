package adp.config;

import java.net.URI;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings under {@code adp.*}. */
@ConfigurationProperties("adp")
public record AdpProperties(Csv csv, Fred fred, Ar ar, Ma ma, Rolling rolling) {

  /**
   * @param path snapshot file read by the csv source
   */
  public record Csv(Path path) {}

  /**
   * @param baseUrl FRED API root
   * @param seriesId series to fetch
   * @param apiKey API key; the fred source refuses to run without one
   */
  public record Fred(URI baseUrl, String seriesId, String apiKey) {}

  /**
   * @param order number of lags p; fixed up front, never tuned on the backtest
   */
  public record Ar(int order) {}

  /**
   * @param window number of recent changes averaged; fixed up front, never tuned on the backtest
   */
  public record Ma(int window) {}

  /**
   * @param months length of the rolling fit window; fixed up front, never tuned on the backtest
   */
  public record Rolling(int months) {}
}
