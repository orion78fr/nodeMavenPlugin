package fr.orion78.nodeMavenPlugin.utils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

public class CommandLineUtils {
  private static final int NORMAL = 0;
  private static final int IN_QUOTE = 1;
  private static final int IN_DOUBLE_QUOTE = 2;

  /**
   * From CommandLineUtils.translateCommandline of plexus
   */
  @NotNull
  public static List<String> translateCommandline(@NotNull String toProcess) throws IOException {
    if (toProcess.isEmpty()) {
      return Collections.emptyList();
    }

    // Parse with a simple finite state machine
    int state = NORMAL;

    StringTokenizer tok = new StringTokenizer(toProcess, "\"\' \t\n\r", true);
    List<String> l = new ArrayList<>();
    StringBuilder current = new StringBuilder();

    while (tok.hasMoreTokens()) {
      String nextTok = tok.nextToken();
      switch (state) {
        case IN_QUOTE:
          if ("\'".equals(nextTok)) {
            state = NORMAL;
          } else {
            current.append(nextTok);
          }
          break;
        case IN_DOUBLE_QUOTE:
          if ("\"".equals(nextTok)) {
            state = NORMAL;
          } else {
            current.append(nextTok);
          }
          break;
        default:
          if ("\'".equals(nextTok)) {
            state = IN_QUOTE;
          } else if ("\"".equals(nextTok)) {
            state = IN_DOUBLE_QUOTE;
          } else if (" ".equals(nextTok)
              || "\t".equals(nextTok)
              || "\n".equals(nextTok)
              || "\r".equals(nextTok)) {
            if (current.length() != 0) {
              l.add(current.toString());
              current.setLength(0);
            }
          } else {
            current.append(nextTok);
          }
          break;
      }
    }

    if (current.length() != 0) {
      l.add(current.toString());
    }

    if (state == IN_QUOTE || state == IN_DOUBLE_QUOTE) {
      throw new IOException("Unbalanced quotes in " + toProcess);
    }

    return l;
  }
}
