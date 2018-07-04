package fr.orion78.uglifyjsMavenPlugin;

import fr.orion78.nodeMavenPlugin.NodeMojo;
import fr.orion78.nodeMavenPlugin.execution.Execution;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Mojo(
    name = "execute",
    defaultPhase = LifecyclePhase.PROCESS_RESOURCES
)
public class UglifyMojo extends AbstractMojo {
  /*
   * Node versions
   */
  @Parameter(property = "uglifyPlugin.node.version", defaultValue = "8.11.2")
  private String nodeVersion;
  @Parameter(property = "uglifyPlugin.node.download.url")
  private String nodeURL;
  @Parameter(property = "uglifyPlugin.node.install.directory")
  private String installDir;
  @Parameter(property = "uglifyPlugin.uglify.version", defaultValue = "3.4.0")
  private String uglifyjsVersion;

  /*
   * File lookup
   */
  @Parameter(defaultValue = "src/main/js")
  private String sourcesFolder;
  @Parameter
  private String[] includes;
  @Parameter
  private String[] excludes;

  /*
   * Uglify args (taken from uglify command-line args)
   */
  @Parameter
  private UglifyArgs uglifyArgs;

  @Override
  public void execute() throws MojoExecutionException {
    NodeMojo nodeMojo = new NodeMojo();
    nodeMojo.setVersion(nodeVersion);
    nodeMojo.setNodeURL(nodeURL);
    nodeMojo.setInstallDir(installDir);
    nodeMojo.setDependencies(new String[]{"uglify-js@" + uglifyjsVersion});

    List<File> files;
    try {
      files = FilesUtils.crawlDir(sourcesFolder, includes, excludes);
    } catch (IOException e) {
      throw new MojoExecutionException("Error while crawling the directory " + sourcesFolder);
    }
    Execution[] executions = new Execution[files.size()];

    for (int i = 0; i < files.size(); i++) {
      executions[i] = new Execution("uglifyjs", uglifyArgs.createArgsForFile(files.get(i)));
    }
    nodeMojo.setExecutions(executions);

    nodeMojo.execute();
  }
}
