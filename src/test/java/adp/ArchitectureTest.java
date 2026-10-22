package adp;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

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
}
