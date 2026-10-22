package adp.config;

import adp.domain.MonthRange;
import adp.domain.ReferenceMonth;
import adp.forecast.AllHistory;
import adp.forecast.AutoregressiveForecaster;
import adp.forecast.Excluding;
import adp.forecast.Forecaster;
import adp.forecast.HistoryWindow;
import adp.forecast.NaiveForecaster;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Chooses the models and windows. Bean names are the names the CLI accepts. */
@Configuration(proxyBeanMethods = false)
public class ForecastConfig {

  /** COVID swings: a structural break, excluded on domain grounds. */
  static final MonthRange COVID =
      new MonthRange(ReferenceMonth.of(2020, 3), ReferenceMonth.of(2021, 12));

  @Bean("naive")
  Forecaster naiveForecaster() {
    return new NaiveForecaster();
  }

  @Bean("ar")
  Forecaster autoregressiveForecaster(AdpProperties properties) {
    return new AutoregressiveForecaster(properties.ar().order());
  }

  @Bean("all")
  HistoryWindow allHistory() {
    return new AllHistory();
  }

  @Bean("exclude-covid")
  HistoryWindow excludeCovid() {
    return new Excluding(COVID);
  }
}
