package adp.config;

import adp.adapter.CsvSnapshotWriter;
import adp.adapter.CsvSource;
import adp.adapter.FredSource;
import adp.adapter.JdbcPrintRepository;
import adp.port.PrintRepository;
import adp.port.PrintSource;
import adp.port.SnapshotWriter;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/** Chooses the adapters. Source bean names are the values accepted by {@code sync --source}. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AdpProperties.class)
public class AdapterConfig {

  @Bean("csv")
  PrintSource csvSource(AdpProperties properties) {
    return new CsvSource(properties.csv().path());
  }

  @Bean("fred")
  PrintSource fredSource(AdpProperties properties) {
    var fred = properties.fred();
    var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    return new FredSource(http, fred.baseUrl(), fred.seriesId(), fred.apiKey());
  }

  @Bean
  PrintRepository printRepository(JdbcClient jdbc, TransactionTemplate transactions) {
    return new JdbcPrintRepository(jdbc, transactions);
  }

  /** Every exported snapshot carries the citation FRED requires for this copyrighted series. */
  @Bean
  SnapshotWriter snapshotWriter(AdpProperties properties, Clock clock) {
    var fred = properties.fred();
    return new CsvSnapshotWriter(
        List.of(
            "Automatic Data Processing, Inc., Total Nonfarm Private Payroll Employment ["
                + fred.seriesId()
                + "],",
            "retrieved from FRED, Federal Reserve Bank of St. Louis; all ALFRED vintages, "
                + LocalDate.now(clock)
                + ".",
            "Copyrighted: citation required. Levels in persons, seasonally adjusted.",
            "vintage_date is the ALFRED realtime_start: the date each value was first published."));
  }
}
