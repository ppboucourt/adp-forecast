package adp;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** The architecture rules from CLAUDE.md, enforced on every build. */
@AnalyzeClasses(packages = "adp", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  @ArchTest
  static final ArchRule coreDoesNotDependOnSpring =
      noClasses()
          .that()
          .resideInAnyPackage("adp.domain..", "adp.forecast..", "adp.evaluation..", "adp.port..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("org.springframework..")
          .because("Spring lives only in config/, adapter/ and cli/");

  @ArchTest
  static final ArchRule dependenciesPointInward =
      layeredArchitecture()
          .consideringOnlyDependenciesInLayers()
          .layer("Domain")
          .definedBy("adp.domain..")
          .layer("Port")
          .definedBy("adp.port..")
          .layer("Forecast")
          .definedBy("adp.forecast..")
          .layer("Evaluation")
          .definedBy("adp.evaluation..")
          .layer("Adapter")
          .definedBy("adp.adapter..")
          .layer("Cli")
          .definedBy("adp.cli..")
          .layer("Config")
          .definedBy("adp.config..")
          .whereLayer("Config")
          .mayNotBeAccessedByAnyLayer()
          .whereLayer("Cli")
          .mayOnlyBeAccessedByLayers("Config")
          .whereLayer("Adapter")
          .mayOnlyBeAccessedByLayers("Config")
          .whereLayer("Evaluation")
          .mayOnlyBeAccessedByLayers("Cli", "Config")
          .whereLayer("Forecast")
          .mayOnlyBeAccessedByLayers("Evaluation", "Cli", "Config")
          .whereLayer("Port")
          .mayOnlyBeAccessedByLayers("Adapter", "Cli", "Config")
          .whereLayer("Domain")
          .mayNotAccessAnyLayer();

  @ArchTest
  static final ArchRule domainUsesOnlyTheJdk =
      classes()
          .that()
          .resideInAPackage("adp.domain..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage("adp.domain..", "java..")
          .because("domain/ has no I/O and no third-party libraries");
}
