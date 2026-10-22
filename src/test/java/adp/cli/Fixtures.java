package adp.cli;

import adp.domain.MonthRange;
import adp.domain.ReferenceMonth;
import adp.evaluation.AccuracyMetric;
import adp.evaluation.MeanAbsoluteError;
import adp.evaluation.SkillVsNaive;
import adp.forecast.AllHistory;
import adp.forecast.AutoregressiveForecaster;
import adp.forecast.Excluding;
import adp.forecast.Forecaster;
import adp.forecast.HistoryWindow;
import adp.forecast.NaiveForecaster;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/** Plug-ins as config/ wires them, for commands built without Spring. */
final class Fixtures {

  /** Ten days after the test series' newest vintage (2035-01-05). */
  static final Clock FRESH = at(LocalDate.of(2035, 1, 15));

  static final Map<String, Forecaster> FORECASTERS =
      Map.of("ar", new AutoregressiveForecaster(3), "naive", new NaiveForecaster());
  static final Map<String, HistoryWindow> WINDOWS =
      Map.of(
          "all",
          new AllHistory(),
          "exclude-covid",
          new Excluding(new MonthRange(ReferenceMonth.of(2020, 3), ReferenceMonth.of(2021, 12))));
  static final List<AccuracyMetric> METRICS = List.of(new MeanAbsoluteError(), new SkillVsNaive());

  private Fixtures() {}

  static Clock at(LocalDate date) {
    return Clock.fixed(date.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
  }
}
