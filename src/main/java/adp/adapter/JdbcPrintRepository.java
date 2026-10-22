package adp.adapter;

import adp.domain.Observation;
import adp.domain.ReferenceMonth;
import adp.port.PrintRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/** Stores observations in the {@code observation} table; see {@code schema.sql}. */
public class JdbcPrintRepository implements PrintRepository {

  private static final String INSERT =
      """
      INSERT INTO observation (reference_month, vintage_date, level)
      VALUES (:month, :vintage, :level)
      ON CONFLICT DO NOTHING
      """;

  private static final String SELECT_ALL =
      """
      SELECT reference_month, vintage_date, level
      FROM observation
      ORDER BY reference_month, vintage_date
      """;

  private final JdbcClient jdbc;
  private final TransactionTemplate transactions;

  public JdbcPrintRepository(JdbcClient jdbc, TransactionTemplate transactions) {
    this.jdbc = jdbc;
    this.transactions = transactions;
  }

  @Override
  public int saveAll(Collection<Observation> observations) {
    Integer added =
        transactions.execute(
            status -> {
              int count = 0;
              for (Observation o : observations) {
                count +=
                    jdbc.sql(INSERT)
                        .param("month", o.month().toString())
                        .param("vintage", o.vintageDate().toString())
                        .param("level", o.level())
                        .update();
              }
              return count;
            });
    return added == null ? 0 : added;
  }

  @Override
  public List<Observation> findAll() {
    return jdbc.sql(SELECT_ALL)
        .query(
            (rs, row) ->
                new Observation(
                    ReferenceMonth.parse(rs.getString("reference_month")),
                    LocalDate.parse(rs.getString("vintage_date")),
                    rs.getLong("level")))
        .list();
  }
}
