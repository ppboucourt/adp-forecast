package adp.cli;

import java.util.Map;
import java.util.TreeSet;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;

/** Looks up a plug-in by name; an unknown name lists the valid ones. */
final class Names {

  private Names() {}

  static <T> T pick(Map<String, T> options, String name, String kind, CommandLine commandLine) {
    return pick(options, name, kind, kind + "s", commandLine);
  }

  static <T> T pick(
      Map<String, T> options, String name, String kind, String plural, CommandLine commandLine) {
    T found = options.get(name);
    if (found == null) {
      throw new ParameterException(
          commandLine,
          "unknown "
              + kind
              + " '"
              + name
              + "'; valid "
              + plural
              + ": "
              + String.join(", ", new TreeSet<>(options.keySet())));
    }
    return found;
  }
}
