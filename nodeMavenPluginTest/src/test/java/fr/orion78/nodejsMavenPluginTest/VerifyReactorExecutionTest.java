package fr.orion78.nodejsMavenPluginTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

class VerifyReactorExecutionTest {
  @Test
  void testFileUglifiedInReactor() {
    Assertions.assertTrue(new File("target", "test.js").exists());
    Assertions.assertTrue(new File("target", "test.js.map").exists());
  }
}
