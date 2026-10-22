package adp.cli;

import java.io.PrintWriter;
import java.io.StringWriter;
import picocli.CommandLine;

/** Runs one command object through picocli and captures its output. */
final class Cli {

  final StringWriter out = new StringWriter();
  final StringWriter err = new StringWriter();
  final int exitCode;

  Cli(Object command, String... args) {
    var cli = CliRunner.configure(new CommandLine(command));
    cli.setOut(new PrintWriter(out, true));
    cli.setErr(new PrintWriter(err, true));
    exitCode = cli.execute(args);
  }

  String out() {
    return out.toString();
  }

  String err() {
    return err.toString();
  }
}
