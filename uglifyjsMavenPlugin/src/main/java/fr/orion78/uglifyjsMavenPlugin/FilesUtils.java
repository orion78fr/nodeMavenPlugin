package fr.orion78.uglifyjsMavenPlugin;

import org.codehaus.plexus.util.DirectoryScanner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FilesUtils {
  @NotNull
  public static List<File> crawlDir(@NotNull String sourcesFolder,
                                    @Nullable String[] includes,
                                    @Nullable String[] excludes) throws IOException {
    File dir = new File(sourcesFolder);
    if (!dir.exists() || !dir.isDirectory() || !dir.canRead()) {
      throw new IOException("Source directory does not exists or is unreadable");
    }

    DirectoryScanner scanner = new DirectoryScanner();
    scanner.setBasedir(sourcesFolder);
    scanner.setIncludes(includes);
    scanner.setExcludes(excludes);
    scanner.setFollowSymlinks(false);
    scanner.scan();

    return Arrays.stream(scanner.getIncludedFiles())
        .map(pathname -> new File(sourcesFolder, pathname))
        .collect(Collectors.toList());
  }
}
