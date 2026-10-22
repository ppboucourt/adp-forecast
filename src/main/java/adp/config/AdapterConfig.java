package adp.config;

import adp.adapter.CsvSnapshotWriter;
import adp.adapter.CsvSource;
import adp.adapter.FredSource;
import adp.adapter.JdbcPrintRepository;
import adp.port.PrintRepository;
import adp.port.PrintSource;
import adp.port.SnapshotWriter;
import java.net.http.HttpClient;
import java.time.Duration;
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

  @Bean
  SnapshotWriter snapshotWriter() {
    return new CsvSnapshotWriter();
  }
}
