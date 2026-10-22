package adp.cli;

import static org.assertj.core.api.Assertions.assertThat;

import adp.domain.Explanation;
import adp.domain.Explanation.Constant;
import adp.domain.Explanation.Weighted;
import adp.domain.Forecast;
import adp.domain.JobsChange;
import adp.domain.ReferenceMonth;
import adp.evaluation.BacktestReport;
import adp.evaluation.Basis;
import adp.evaluation.ScoredMonth;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

class TextRendererTest {

  static final ReferenceMonth OCT = ReferenceMonth.of(2026, 10);

  static BacktestReport report(String model, int n, double mae, OptionalDouble skill) {
    var metrics = new LinkedHashMap<String, OptionalDouble>();
    metrics.put("MAE", OptionalDouble.of(mae));
    metrics.put("skill", skill);
    var rows =
        Collections.nCopies(
            n, new ScoredMonth(OCT, new JobsChange(0), new JobsChange(0), new JobsChange(0)));
    return new BacktestReport(Basis.REAL_TIME, model, "naive", "all", "all", rows, 0, metrics);
  }

  @Test
  void largestRemainderRowsAddUpToTheRoundedTotal() {
    // Rounded independently these show 11 + 11 + 11 = 33 against a total of 32.
    assertThat(TextRenderer.largestRemainderK(new long[] {10_500, 10_500, 10_500}))
        .containsExactly(11, 11, 10);
    assertThat(TextRenderer.largestRemainderK(new long[] {-10_500, 5_200})).containsExactly(-10, 5);
    assertThat(TextRenderer.largestRemainderK(new long[] {41_600, 64_350, 11_340, 1_200}))
        .containsExactly(42, 64, 11, 1);
  }

  @Test
  void formatsSignedAndPaddedThousands() {
    assertThat(TextRenderer.signedK(143)).isEqualTo("+143K");
    assertThat(TextRenderer.signedK(-12)).isEqualTo("-12K");
    assertThat(TextRenderer.paddedK(54)).isEqualTo("+ 54K");
    assertThat(TextRenderer.paddedK(-7)).isEqualTo("-  7K");
  }

  @Test
  void explanationRowsAddUpToTheShownForecast() {
    var forecast =
        Forecast.of(
            OCT,
            new Explanation(
                List.of(
                    new Constant("intercept", new JobsChange(10_500)),
                    new Weighted(0.5, OCT.previous(), new JobsChange(21_000)),
                    new Weighted(0.5, OCT.previous().previous(), new JobsChange(21_000)))));

    var lines = TextRenderer.explanation(forecast);

    assertThat(lines)
        .containsExactly(
            "  intercept                     +11K",
            "  0.50 × Sep (+ 21K)            +11K",
            "  0.50 × Aug (+ 21K)            +10K",
            "  ──────────────────────────────────",
            "  forecast                      +32K");
    assertThat(TextRenderer.headline(forecast, "AR(2)", "all"))
        .isEqualTo("Oct 2026 forecast (AR(2), window=all): +32K");
  }

  @Test
  void trustLineShowsMaeSkillAndN() {
    assertThat(TextRenderer.trust(report("AR(3)", 49, 61_400, OptionalDouble.of(0.18))))
        .containsExactly("  backtest MAE 61K | skill vs naive +0.18 | n=49");
  }

  @Test
  void trustLineAddsANoteWhenTheModelDoesNotBeatTheBaseline() {
    assertThat(TextRenderer.trust(report("AR(3)", 49, 70_000, OptionalDouble.of(-0.05))))
        .containsExactly(
            "  backtest MAE 70K | skill vs naive -0.05 | n=49",
            "  note: naive has been at least as accurate over these 49 months");
  }

  @Test
  void trustLineOmitsSkillForTheBaselineItselfAndHandlesNoTargets() {
    assertThat(TextRenderer.trust(report("naive", 49, 70_000, OptionalDouble.of(0))))
        .containsExactly("  backtest MAE 70K | n=49");
    assertThat(TextRenderer.trust(report("AR(3)", 0, 0, OptionalDouble.empty())))
        .containsExactly("  backtest: not enough history");
  }

  @Test
  void backtestTableShowsOneRowPerFitWindowAndTheSkippedCount() {
    var all = report("AR(3)", 49, 61_400, OptionalDouble.of(0.18));
    var lines = TextRenderer.backtestTable(List.of(all));

    assertThat(lines)
        .containsExactly(
            "AR(3) vs naive · real-time first prints · scored on all",
            "fit window          n    MAE   skill",
            "all                49    61K   +0.18",
            "skipped: 0 of 49 targets");
  }
}
