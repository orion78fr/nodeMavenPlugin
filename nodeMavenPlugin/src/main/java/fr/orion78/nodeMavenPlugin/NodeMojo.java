package fr.orion78.nodeMavenPlugin;

import fr.orion78.nodeMavenPlugin.execution.Execution;
import fr.orion78.nodeMavenPlugin.execution.ExecutionResult;
import fr.orion78.nodeMavenPlugin.execution.Executor;
import fr.orion78.nodeMavenPlugin.utils.PermissionUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;

@Mojo(
    name = "execute",
    defaultPhase = LifecyclePhase.PROCESS_RESOURCES
)
public class NodeMojo extends AbstractMojo {
  @Parameter(property = "nodePlugin.node.version", defaultValue = "8.11.2")
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
      getLog().debug("Node already downloaded to " + extractDir);
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
        URL nodeUrl;
        try {
          nodeUrl = new URL(nodeUrlString);
        } catch (MalformedURLException e) {
          throw new MojoExecutionException("Problem in node url " + nodeUrlString, e);
        }

        if (!downloadedFile.getParentFile().mkdirs()) {
          throw new MojoExecutionException("Cannot create folder " + downloadedFile.getParent());
        }

        try {
          try (ReadableByteChannel rbc = Channels.newChannel(nodeUrl.openStream());
               FileOutputStream fos = new FileOutputStream(downloadedFile)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
          }
        } catch (IOException e) {
          throw new MojoExecutionException("Problem while downloading node", e);
        }

        localizedFile = downloadedFile;
      }

      // Extract
      getLog().info("Extracting " + localizedFile + " to " + extractDir);

      try (FileInputStream fis = new FileInputStream(localizedFile);
           BufferedInputStream bis = new BufferedInputStream(fis);
           XZCompressorInputStream uncompressed = new XZCompressorInputStream(bis);
           BufferedInputStream uncompressedBuffered = new BufferedInputStream(uncompressed);
           TarArchiveInputStream archive = new TarArchiveInputStream(uncompressedBuffered)) {
        TarArchiveEntry entry;
        while ((entry = archive.getNextTarEntry()) != null) {
          String name = entry.getName();
          if (!archive.canReadEntryData(entry)) {
            throw new IOException("Cannot read data " + name);
          }

          File f = new File(extractDir, name.substring(name.indexOf('/'))); // Remove first subfolder
          if (entry.isDirectory()) {
            getLog().debug("Creating directory " + f);
            if (!f.isDirectory() && !f.mkdirs()) {
              throw new IOException("Failed to create directory " + f);
            }
            Files.setPosixFilePermissions(f.toPath(), PermissionUtils.modeToPermissionSet(entry.getMode()));
          } else if (entry.isSymbolicLink()) {
            getLog().debug("Creating symbolic link " + f + " --> " + entry.getLinkName());
            File parent = f.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) {
              throw new IOException("Failed to create directory " + parent);
            }

            Files.createSymbolicLink(f.toPath(), new File(entry.getLinkName()).toPath());
            Thread.sleep(1);
          } else if (entry.isFile()) {
            getLog().debug("Extracting file " + f);
            File parent = f.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) {
              throw new IOException("Failed to create directory " + parent);
            }

            try (OutputStream o = Files.newOutputStream(f.toPath())) {
              IOUtils.copy(archive, o);
            }
            Files.setPosixFilePermissions(f.toPath(), PermissionUtils.modeToPermissionSet(entry.getMode()));
          } else {
            throw new IOException("Unsupported entry type " + entry.getName());
          }
        }
      } catch (IOException | InterruptedException e) {
        throw new MojoExecutionException("Error while decompressing", e);
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

  public void setVersion(String version) {
    this.version = version;
  }

  public void setNodeURL(String nodeURL) {
    this.nodeURL = nodeURL;
  }

  public void setInstallDir(String installDir) {
    this.installDir = installDir;
  }

  public void setDependencies(String[] dependencies) {
    this.dependencies = dependencies;
  }

  public void setExecutions(Execution[] executions) {
    this.executions = executions;
  }

  public void setProjectBuildDir(String projectBuildDir) {
    this.projectBuildDir = projectBuildDir;
  }
}
