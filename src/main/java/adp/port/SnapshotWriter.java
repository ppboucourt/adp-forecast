package adp.port;

import adp.domain.Observation;
import java.nio.file.Path;
import java.util.Collection;

/** Writes observations to a file that a {@link PrintSource} can read back. */
public interface SnapshotWriter {

  void write(Collection<Observation> observations, Path target);
}
