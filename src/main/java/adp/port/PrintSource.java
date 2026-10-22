package adp.port;

import adp.domain.Observation;
import java.util.List;

/** An external publication of ADP payroll vintages. */
public interface PrintSource {

  /** Every observation the source holds, across all vintages. */
  List<Observation> fetch();
}
