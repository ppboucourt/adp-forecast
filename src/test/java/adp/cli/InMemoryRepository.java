package adp.cli;

import adp.adapter.CsvSource;
import adp.domain.Observation;
import adp.port.PrintRepository;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Repository fake for CLI tests. */
class InMemoryRepository implements PrintRepository {

  final Set<Observation> stored = new LinkedHashSet<>();

  /** Loaded with the synthetic test series in {@code cli/test_vintages.csv}. */
  static InMemoryRepository withTestSeries() {
    var repository = new InMemoryRepository();
    repository.saveAll(new CsvSource(testSeries()).fetch());
    return repository;
  }

  static Path testSeries() {
    try {
      return Path.of(InMemoryRepository.class.getResource("/cli/test_vintages.csv").toURI());
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public int saveAll(Collection<Observation> observations) {
    int added = 0;
    for (Observation o : observations) {
      added += stored.add(o) ? 1 : 0;
    }
    return added;
  }

  @Override
  public List<Observation> findAll() {
    return List.copyOf(stored);
  }
}
