package fr.orion78.nodejsMavenPluginTest;

import fr.orion78.nodeMavenPlugin.NodeMojo;
import fr.orion78.nodeMavenPlugin.execution.Execution;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

class MojoExecutionFromCodeTest {
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
                " -c -- src/main/js/test.js")});

    mojo.execute();

    Assertions.assertTrue(new File("target", "testFromCode.js").exists());
    Assertions.assertTrue(new File("target", "testFromCode.js.map").exists());
  }
}
