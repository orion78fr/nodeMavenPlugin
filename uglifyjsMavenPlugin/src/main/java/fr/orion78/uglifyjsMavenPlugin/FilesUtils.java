package fr.orion78.uglifyjsMavenPlugin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FilesUtils {
  @NotNull
  public static List<File> crawlDir(@NotNull String sourcesFolder,
                                    @Nullable String[] includes,
                                    @Nullable String[] excludes,
                                    boolean excludesFirst) throws IOException {
    File dir = new File(sourcesFolder);
    if (!dir.exists() || !dir.isDirectory() || !dir.canRead()) {
      throw new IOException("Source directory does not exists or is unreadable");
    }

    List<File> result = new ArrayList<>();

    for (File f : Optional.ofNullable(dir.listFiles()).orElse(new File[0])) {
      // TODO
    }

    return result;
  }
}
