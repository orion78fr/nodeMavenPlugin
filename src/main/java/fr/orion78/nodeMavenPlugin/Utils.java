package fr.orion78.nodeMavenPlugin;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

class Utils {
  private static final int NORMAL = 0;
  private static final int IN_QUOTE = 1;
  private static final int IN_DOUBLE_QUOTE = 2;

  /**
   * From CommandLineUtils.translateCommandline
   */
  @NotNull
  static List<String> translateCommandline(@NotNull String toProcess) throws IOException {
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

  /**
   * Transforms unix fs mode (e.g int 0755) to the corresponding set of PosixFilePermission
   */
  @NotNull
  static Set<PosixFilePermission> modeToPermissionSet(int mode) {
    Set<PosixFilePermission> permissions = new HashSet<>();

    if ((mode & 1) != 0) {
      permissions.add(PosixFilePermission.OTHERS_EXECUTE);
    }
    if ((mode & (1 << 1)) != 0) {
      permissions.add(PosixFilePermission.OTHERS_WRITE);
    }
    if ((mode & (1 << 2)) != 0) {
      permissions.add(PosixFilePermission.OTHERS_READ);
    }
    if ((mode & (1 << 3)) != 0) {
      permissions.add(PosixFilePermission.GROUP_EXECUTE);
    }
    if ((mode & (1 << 4)) != 0) {
      permissions.add(PosixFilePermission.GROUP_WRITE);
    }
    if ((mode & (1 << 5)) != 0) {
      permissions.add(PosixFilePermission.GROUP_READ);
    }
    if ((mode & (1 << 6)) != 0) {
      permissions.add(PosixFilePermission.OWNER_EXECUTE);
    }
    if ((mode & (1 << 7)) != 0) {
      permissions.add(PosixFilePermission.OWNER_WRITE);
    }
    if ((mode & (1 << 8)) != 0) {
      permissions.add(PosixFilePermission.OWNER_READ);
    }

    return permissions;
  }
}
