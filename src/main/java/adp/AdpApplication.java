package adp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point. Component scanning is limited to {@code adp.config}; everything else is a bean. */
@SpringBootApplication(scanBasePackages = "adp.config")
public class AdpApplication {

  public static void main(String[] args) {
    System.exit(SpringApplication.exit(SpringApplication.run(AdpApplication.class, args)));
  }
}
