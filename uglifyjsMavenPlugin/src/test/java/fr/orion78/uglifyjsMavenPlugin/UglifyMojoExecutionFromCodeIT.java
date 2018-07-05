package fr.orion78.uglifyjsMavenPlugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

class UglifyMojoExecutionFromCodeIT {
  @Test
  void testCallFromCode() throws MojoExecutionException {
    UglifyMojo mojo = new UglifyMojo();

    mojo.execute();
  }
}
