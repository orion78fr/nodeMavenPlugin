package fr.orion78.nodeMavenPlugin;

import fr.orion78.nodeMavenPlugin.execution.Execution;
import fr.orion78.nodeMavenPlugin.execution.ExecutionResult;
import fr.orion78.nodeMavenPlugin.execution.Executor;
import fr.orion78.nodeMavenPlugin.utils.ArchiveUtils;
import fr.orion78.nodeMavenPlugin.utils.NetUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

@Mojo(
    name = "execute",
    defaultPhase = LifecyclePhase.PROCESS_RESOURCES
)
public class NodeMojo extends AbstractMojo {
  // https://nodejs.org/en/
  @Parameter(property = "nodePlugin.node.version", defaultValue = "8.11.3")
  private String version;
  @Parameter(property = "nodePlugin.node.download.url")
  private String nodeURL;
  @Parameter(property = "nodePlugin.node.install.directory")
  private String installDir;
  @Parameter
  private String[] dependencies;
  @Parameter
  private Execution[] executions;

  @Parameter(defaultValue = "${project.build.directory}", readonly = true)
  private String projectBuildDir;

  public void execute() throws MojoExecutionException {
    NodePaths paths = NodePaths.of(nodeURL, installDir, version, projectBuildDir);

    File extractDir = paths.getNodeInstallDir();
    if (extractDir.exists()) {
      if (getLog().isDebugEnabled()) {
        getLog().debug("Node already downloaded to " + extractDir);
      }
    } else {
      // Download node
      File downloadedFile = paths.getNodeDownloadFile();
      String nodeUrlString = paths.getNodeURLString();
      getLog().info("Node url " + nodeUrlString);

      File localizedFile;
      if (nodeUrlString.startsWith("file://")) {
        // File is already local
        localizedFile = new File(nodeUrlString.substring("file://".length()));
      } else {
        getLog().info("Downloading node to " + downloadedFile);

        try {
          NetUtils.localize(nodeUrlString, downloadedFile);
        } catch (IOException e) {
          throw new MojoExecutionException("Problem while downloading node", e);
        }

        localizedFile = downloadedFile;
      }

      // Extract
      getLog().info("Extracting " + localizedFile + " to " + extractDir);

      try {
        ArchiveUtils.extractTar(localizedFile, extractDir, 1, getLog());
      } catch (IOException e) {
        throw new MojoExecutionException("Error during extract", e);
      }
    }

    Executor executor = new Executor(extractDir);

    try {
      ExecutionResult res = executor.execute(new Execution(null, "--version"), 10);
      if (res.getExitVal() != 0) {
        throw new MojoExecutionException("Execution returned a non zero exit value : " + res.getExitVal());
      }
      if (!('v' + version).equals(res.getOut().trim())) {
        throw new MojoExecutionException("Expected version of node is v" + version + " but got " + res.getOut());
      }
    } catch (InterruptedException | IOException e) {
      throw new MojoExecutionException("Error while checking version", e);
    }

    getLog().info("Node version " + version);

    // Install deps
    if (dependencies != null && dependencies.length != 0) {
      try {
        getLog().info("Installing dependencies");
        ExecutionResult res = executor.execute(new Execution("npm",
            "install -g " + String.join(" ", dependencies)), 10);

        logOrFail(res, getLog());
      } catch (InterruptedException | IOException e) {
        throw new MojoExecutionException("Error while executing", e);
      }
    }

    // Execute
    try {
      for (Execution execution : executions) {
        getLog().info("Executing " + execution);
        ExecutionResult res = executor.execute(execution, 10);

        logOrFail(res, getLog());
      }
    } catch (IOException | InterruptedException e) {
      throw new MojoExecutionException("Error while executing", e);
    }
  }

  private void logOrFail(@NotNull ExecutionResult res,
                         @NotNull Log log) throws MojoExecutionException {
    if (!res.getOut().trim().isEmpty()) {
      log.info(res.getOut());
    }
    if (!res.getErr().trim().isEmpty()) {
      if (res.getExitVal() == 0) {
        log.warn(res.getErr());
      } else {
        log.error(res.getErr());
        throw new MojoExecutionException("Execution returned a non zero exit value : " + res.getExitVal());
      }
    }
  }

  public void setVersion(@NotNull String version) {
    this.version = version;
  }

  public void setNodeURL(@NotNull String nodeURL) {
    this.nodeURL = nodeURL;
  }

  public void setInstallDir(@NotNull String installDir) {
    this.installDir = installDir;
  }

  public void setDependencies(@NotNull String[] dependencies) {
    this.dependencies = dependencies;
  }

  public void setExecutions(@NotNull Execution[] executions) {
    this.executions = executions;
  }

  public void setProjectBuildDir(@NotNull String projectBuildDir) {
    this.projectBuildDir = projectBuildDir;
  }
}
