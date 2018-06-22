package fr.orion78.uglifyjsMavenPlugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(
    name = "execute",
    defaultPhase = LifecyclePhase.PROCESS_RESOURCES
)
public class UglifyMojo extends AbstractMojo {
  /*
   * Node versions
   */
  @Parameter
  private String nodeVersion;
  @Parameter
  private String uglifyjsVersion;
  @Parameter
  private String nodeURL;
  @Parameter
  private String installDir;

  /*
   * File lookup
   */
  @Parameter
  private String repository;
  @Parameter
  private String[] includes;
  @Parameter
  private String[] excludes;
  @Parameter
  private boolean excludesFirst;

  @Override
  public void execute() {
    // TODO
  }
}
