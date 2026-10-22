package adp.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import adp.domain.Observation;
import adp.domain.ReferenceMonth;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import org.sqlite.SQLiteDataSource;

/** Runs against a real SQLite file with the production schema. */
class JdbcPrintRepositoryTest {

  static final ReferenceMonth AUG = ReferenceMonth.of(2022, 8);
  static final ReferenceMonth SEP = ReferenceMonth.of(2022, 9);

  final List<Observation> rows =
      List.of(
          new Observation(SEP, LocalDate.of(2022, 10, 5), 132_258_000),
          new Observation(AUG, LocalDate.of(2022, 10, 5), 132_050_000),
          new Observation(AUG, LocalDate.of(2022, 8, 31), 132_032_000));

  @TempDir Path dir;
  JdbcPrintRepository repository;

  @BeforeEach
  void openDatabase() {
    var dataSource = new SQLiteDataSource();
    dataSource.setUrl("jdbc:sqlite:" + dir.resolve("adp.db"));
    new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
    repository =
        new JdbcPrintRepository(
            JdbcClient.create(dataSource),
            new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
  }

  @Test
  void roundTripsObservationsInMonthThenVintageOrder() {
    assertThat(repository.saveAll(rows)).isEqualTo(3);

    assertThat(repository.findAll()).containsExactly(rows.get(2), rows.get(1), rows.get(0));
  }

  @Test
  void reinsertingIsANoOp() {
    repository.saveAll(rows);

    assertThat(repository.saveAll(rows)).isZero();
    assertThat(repository.findAll()).hasSize(3);
  }

  @Test
  void aStoredVintageIsNeverOverwritten() {
    repository.saveAll(rows);

    var changed = new Observation(AUG, LocalDate.of(2022, 8, 31), 999_999_999);
    assertThat(repository.saveAll(List.of(changed))).isZero();
    assertThat(repository.findAll()).contains(rows.get(2)).doesNotContain(changed);
  }

  @Test
  void countsOnlyTheNewRowsInAMixedBatch() {
    repository.saveAll(rows.subList(0, 1));

    assertThat(repository.saveAll(rows)).isEqualTo(2);
  }
}
