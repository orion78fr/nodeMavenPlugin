package fr.orion78.uglifyjsMavenPlugin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

@SuppressWarnings("unused")
public class UglifyArgs {
  private boolean parse;
  private String parseOptions;

  private boolean compress;
  private String compressOptions;

  private boolean mangle;
  private String mangleOptions;
  private String mangleProps;

  private boolean beautify;
  private String beautifyOptions;

  private String commentFilter;
  private String[] defines;
  private String enclosing;
  private boolean ie8;
  private boolean keepFnames;
  private String nameCacheFile;
  private boolean forceRename;
  private String sourceMapOptions;
  private boolean topLevel;
  private String wrapper;

  @NotNull
  public String createArgsForFile(@NotNull File file) {
    StringBuilder sb = new StringBuilder();

    if (notEmpty(parseOptions)) {
      sb.append("--parse \"")
          .append(escapeQuote(parseOptions))
          .append("\" ");
    } else if (parse) {
      sb.append("-p ");
    }

    if (notEmpty(compressOptions)) {
      sb.append("--compress \"")
          .append(escapeQuote(compressOptions))
          .append("\" ");
    } else if (compress) {
      sb.append("-c ");
    }

    if (notEmpty(mangleOptions)) {
      sb.append("--mangle \"")
          .append(escapeQuote(mangleOptions))
          .append("\" ");
    } else if (mangle) {
      sb.append("-m ");
    }
    if (notEmpty(mangleProps)) {
      sb.append("-- mangle-props \"")
          .append(escapeQuote(mangleProps))
          .append("\" ");
    }

    if (notEmpty(beautifyOptions)) {

    } else if (beautify) {

    }

    if (notEmpty(commentFilter)) {

    }

    if (defines != null && defines.length > 0) {

    }

    sb.append("-- ").append(file.getPath());

    return sb.toString();
  }

  private boolean notEmpty(@Nullable String str) {
    return str != null && !str.isEmpty();
  }

  @NotNull
  private String escapeQuote(@NotNull String parseOptions) {
    return parseOptions.replace("\"", "\\\"");
  }
}
