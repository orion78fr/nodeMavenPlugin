package fr.orion78.nodeMavenPlugin;

import fr.orion78.nodeMavenPlugin.execution.Execution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

class MojoExecutionFromCodeIT {
  @Test
  void testCallUglifyFromCode() throws MojoExecutionException {
    NodeMojo mojo = new NodeMojo();
    mojo.setVersion("8.11.3");
    mojo.setInstallDir(System.getProperty("user.home") + "/.nodeMvnPlugin");
    mojo.setDependencies(new String[]{"uglify-js@3.4.2"});
    mojo.setExecutions(new Execution[]{
        new Execution("uglifyjs",
            "-o target/testFromCode.js" +
                " --source-map \"url='./testFromCode.js.map'\"" +
                " -c -- src/test/resources/js/test.js")});

    mojo.setLog(new TestLog());

    mojo.execute();

    Assertions.assertTrue(new File("target", "testFromCode.js").exists());
    Assertions.assertTrue(new File("target", "testFromCode.js.map").exists());
  }

  private static class TestLog implements Log {
    @Override
    public boolean isDebugEnabled() {
      return false;
    }

    @Override
    public void debug(CharSequence charSequence) {
    }

    @Override
    public void debug(CharSequence charSequence, Throwable throwable) {
    }

    @Override
    public void debug(Throwable throwable) {
    }

    @Override
    public boolean isInfoEnabled() {
      return true;
    }

    @Override
    public void info(CharSequence charSequence) {
      System.out.println("[INFO] " + charSequence);
    }

    @Override
    public void info(CharSequence charSequence, Throwable throwable) {
      System.out.println("[INFO] " + charSequence);
      System.out.print("[INFO] ");
      throwable.printStackTrace(System.out);
    }

    @Override
    public void info(Throwable throwable) {
      System.out.print("[INFO] ");
      throwable.printStackTrace(System.out);
    }

    @Override
    public boolean isWarnEnabled() {
      return true;
    }

    @Override
    public void warn(CharSequence charSequence) {
      System.out.println("[WARN] " + charSequence);
    }

    @Override
    public void warn(CharSequence charSequence, Throwable throwable) {
      System.out.println("[WARN] " + charSequence);
      System.out.print("[WARN] ");
      throwable.printStackTrace(System.out);
    }

    @Override
    public void warn(Throwable throwable) {
      System.out.print("[WARN] ");
      throwable.printStackTrace(System.out);
    }

    @Override
    public boolean isErrorEnabled() {
      return true;
    }

    @Override
    public void error(CharSequence charSequence) {
      System.err.println("[ERROR] " + charSequence);
    }

    @Override
    public void error(CharSequence charSequence, Throwable throwable) {
      System.err.println("[ERROR] " + charSequence);
      System.err.print("[ERROR] ");
      throwable.printStackTrace(System.err);
    }

    @Override
    public void error(Throwable throwable) {
      System.err.print("[ERROR] ");
      throwable.printStackTrace(System.err);
    }
  }
}
