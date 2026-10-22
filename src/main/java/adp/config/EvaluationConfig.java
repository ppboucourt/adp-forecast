package adp.config;

import adp.evaluation.AccuracyMetric;
import adp.evaluation.MeanAbsoluteError;
import adp.evaluation.SkillVsNaive;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/** Chooses the accuracy metrics (in report order) and the clock used for staleness checks. */
@Configuration(proxyBeanMethods = false)
public class EvaluationConfig {

  @Bean
  @Order(1)
  AccuracyMetric meanAbsoluteError() {
    return new MeanAbsoluteError();
  }

  @Bean
  @Order(2)
  AccuracyMetric skillVsNaive() {
    return new SkillVsNaive();
  }

  @Bean
  Clock clock() {
    return Clock.systemDefaultZone();
  }
}
