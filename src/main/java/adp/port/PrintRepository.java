package adp.port;

import adp.domain.Observation;
import java.util.Collection;
import java.util.List;

/** Local store of observations. Vintages are immutable: storing one twice is a no-op. */
public interface PrintRepository {

  /** Stores observations not already held and returns how many were new. */
  int saveAll(Collection<Observation> observations);

  /** All stored observations, ordered by reference month then vintage date. */
  List<Observation> findAll();
}
