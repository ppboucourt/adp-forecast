package adp.config;

import adp.cli.CliRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import picocli.CommandLine.IFactory;

/** Wires the command line. Picocli instantiates commands through Spring, so they get injection. */
@Configuration(proxyBeanMethods = false)
public class CliConfig {

  @Bean
  CliRunner cliRunner(IFactory factory) {
    return new CliRunner(factory);
  }
}
