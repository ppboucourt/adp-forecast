package adp.adapter;

import adp.domain.Observation;
import adp.domain.ReferenceMonth;
import adp.port.PrintSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Fetches every vintage of a series from the FRED/ALFRED API. Asks for observations by real-time
 * period over all time, so each row is a value with the date it was first published ({@code
 * realtime_start}), which becomes the vintage date.
 */
public class FredSource implements PrintSource {

  private static final String MISSING = ".";

  private final HttpClient http;
  private final URI baseUrl;
  private final String seriesId;
  private final String apiKey;
  private final JsonMapper json = JsonMapper.builder().build();

  public FredSource(HttpClient http, URI baseUrl, String seriesId, String apiKey) {
    this.http = http;
    this.baseUrl = baseUrl;
    this.seriesId = seriesId;
    this.apiKey = apiKey;
  }

  @Override
  public List<Observation> fetch() {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("FRED_API_KEY is not set");
    }
    var request =
        HttpRequest.newBuilder(requestUri()).timeout(Duration.ofSeconds(60)).GET().build();
    HttpResponse<String> response;
    try {
      response = http.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      throw new UncheckedIOException("FRED request for " + seriesId + " failed", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("FRED request for " + seriesId + " interrupted", e);
    }
    if (response.statusCode() != 200) {
      throw new IllegalStateException(
          "FRED returned HTTP " + response.statusCode() + " for " + seriesId);
    }
    return parse(response.body());
  }

  private URI requestUri() {
    Map<String, String> query = new LinkedHashMap<>();
    query.put("series_id", seriesId);
    query.put("api_key", apiKey);
    query.put("file_type", "json");
    query.put("output_type", "1");
    // Both default to today, which would return only the current vintage.
    query.put("realtime_start", "1776-07-04");
    query.put("realtime_end", "9999-12-31");
    String encoded =
        query.entrySet().stream()
            .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
    return URI.create(baseUrl + "/series/observations?" + encoded);
  }

  private List<Observation> parse(String body) {
    JsonNode observations = json.readTree(body).path("observations");
    var result = new ArrayList<Observation>();
    for (JsonNode node : observations) {
      String value = node.path("value").asString();
      if (MISSING.equals(value)) {
        continue;
      }
      result.add(
          new Observation(
              new ReferenceMonth(YearMonth.from(LocalDate.parse(node.path("date").asString()))),
              LocalDate.parse(node.path("realtime_start").asString()),
              Long.parseLong(value)));
    }
    return result;
  }
}
