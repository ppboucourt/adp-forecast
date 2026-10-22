package adp.cli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.IFactory;

/** Runs the picocli command tree with Spring as the factory and reports its exit code. */
public class CliRunner implements CommandLineRunner, ExitCodeGenerator {

  private final IFactory factory;
  private int exitCode;

  public CliRunner(IFactory factory) {
    this.factory = factory;
  }

  /** Reports a failing command as one {@code error:} line instead of a stack trace. */
  public static CommandLine configure(CommandLine commandLine) {
    return commandLine.setExecutionExceptionHandler(
        (e, cl, parseResult) -> {
          cl.getErr().println("error: " + e.getMessage());
          return ExitCode.SOFTWARE;
        });
  }

  @Override
  public void run(String... args) {
    exitCode = configure(new CommandLine(AdpCommand.class, factory)).execute(args);
  }

  @Override
  public int getExitCode() {
    return exitCode;
  }
}
