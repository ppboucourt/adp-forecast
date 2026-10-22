package adp.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import adp.domain.Observation;
import adp.domain.ReferenceMonth;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Serves a FRED-shaped response from a local server; no network. */
class FredSourceTest {

  HttpServer server;
  final AtomicReference<URI> lastRequest = new AtomicReference<>();
  int status = 200;
  byte[] body;

  @BeforeEach
  void start() throws IOException {
    body =
        FredSourceTest.class.getResourceAsStream("/adapter/fred-observations.json").readAllBytes();
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          lastRequest.set(exchange.getRequestURI());
          exchange.sendResponseHeaders(status, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  FredSource source(String apiKey) {
    var base = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/fred");
    return new FredSource(HttpClient.newHttpClient(), base, "ADPMNUSNERSA", apiKey);
  }

  @Test
  void mapsRealtimeStartToVintageAndSkipsMissingValues() {
    var observations = source("secret").fetch();

    assertThat(observations)
        .containsExactly(
            new Observation(ReferenceMonth.of(2022, 7), LocalDate.of(2022, 8, 31), 131_900_000),
            new Observation(ReferenceMonth.of(2022, 8), LocalDate.of(2022, 8, 31), 132_032_000),
            new Observation(ReferenceMonth.of(2022, 8), LocalDate.of(2022, 10, 5), 132_050_000),
            new Observation(ReferenceMonth.of(2022, 9), LocalDate.of(2022, 10, 5), 132_258_000));
  }

  @Test
  void rejectsAFractionalLevelWithContext() {
    body =
        """
        {"observations": [
          {"realtime_start": "2022-08-31", "date": "2022-07-01", "value": "131900000.5"}
        ]}
        """
            .getBytes(StandardCharsets.UTF_8);

    assertThatThrownBy(() -> source("secret").fetch())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("2022-07-01")
        .hasMessageContaining("131900000.5");
  }

  @Test
  void requestsEveryVintageByRealTimePeriod() {
    source("secret").fetch();

    var request = lastRequest.get();
    assertThat(request.getPath()).isEqualTo("/fred/series/observations");
    assertThat(request.getQuery())
        .contains(
            "series_id=ADPMNUSNERSA",
            "api_key=secret",
            "file_type=json",
            "output_type=1",
            "realtime_start=1776-07-04",
            "realtime_end=9999-12-31");
  }

  @Test
  void refusesToRunWithoutAKey() {
    assertThatThrownBy(() -> source(" ").fetch())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("FRED_API_KEY");
    assertThat(lastRequest.get()).isNull();
  }

  @Test
  void reportsHttpErrorsWithoutLeakingTheKey() {
    status = 400;
    body = "{\"error_message\":\"Bad Request.\"}".getBytes(StandardCharsets.UTF_8);

    assertThatThrownBy(() -> source("secret").fetch())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("400")
        .hasMessageNotContaining("secret");
  }
}
