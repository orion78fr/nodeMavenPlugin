package fr.orion78.uglifyjsMavenPlugin;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

class FilesUtilsTest {
  @Test
  void testDirCrawl() throws IOException {
    List<File> files = FilesUtils.crawlDir(".",
        new String[]{"src/**/*.java"},
        new String[]{"**/Uglify*.java"});

    Assertions.assertTrue(files.contains(new File("./src/test/java/fr/orion78/uglifyjsMavenPlugin/FilesUtilsTest.java")));
    Assertions.assertFalse(files.contains(new File("./src/main/java/fr/orion78/uglifyjsMavenPlugin/UglifyArgs.java")));
    Assertions.assertFalse(files.contains(new File("./src/main/java/fr/orion78/uglifyjsMavenPlugin/UglifyMojo.java")));
  }
}