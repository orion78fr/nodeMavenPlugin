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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.Contract;
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
  @Parameter(defaultValue = "8.11.2")
  private String version;
  @Parameter
  private String nodeURL;
  @Parameter(property = "nodePlugin.node.install.directory")
  private String installDir;
  @Parameter
  private String[] dependencies;
  @Parameter
  private Execution[] executions;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  public void execute() throws MojoExecutionException {
    File extractDir = getNodeExtractDir();
    if (extractDir.exists()) {
      getLog().debug("Node already downloaded to " + extractDir);
    } else {
      // Download node
      File downloadedFile = getNodeDownloadFile();
      getLog().info("Node url " + getNodeUrl());
      getLog().info("Downloading node to " + downloadedFile);
      URL nodeUrl;
      try {
        nodeUrl = new URL(getNodeUrl());
      } catch (MalformedURLException e) {
        throw new MojoExecutionException("Problem in node url " + getNodeUrl(), e);
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

      // Extract
      getLog().info("Extracting " + downloadedFile + " to " + extractDir);

      try (FileInputStream fis = new FileInputStream(downloadedFile);
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
    getLog().info("Installing dependencies");

    // Install deps
    // TODO remove this
    dependencies = new String[]{"uglify-js@3.4.0"};
    if (dependencies != null && dependencies.length != 0) {
      try {
        ExecutionResult res = executor.execute(new Execution("npm",
            "install -g " + String.join(" ", dependencies)), 10);
        getLog().debug(res.getOut());
        getLog().error(res.getErr());
        if (res.getExitVal() != 0) {
          throw new MojoExecutionException("Execution returned a non zero exit value : " + res.getExitVal());
        }
      } catch (InterruptedException | IOException e) {
        throw new MojoExecutionException("Error while executing", e);
      }
    }

    // Execute
    try {
      // TODO remove this
      executions = new Execution[]{new Execution("uglifyjs", "--help")};
      for (Execution execution : executions) {
        getLog().info("Executing " + execution);
        ExecutionResult res = executor.execute(execution, 10);
        getLog().info(res.getOut());
        getLog().error(res.getErr());
      }
    } catch (IOException | InterruptedException e) {
      throw new MojoExecutionException("Error while executing", e);
    }
  }

  @Contract(pure = true)
  @NotNull
  private String getNodeUrl() {
    return nodeURL != null ? nodeURL
        : "https://nodejs.org/dist/v" + version + "/node-v" + version + "-linux-x64.tar.xz";
  }

  @NotNull
  private File getNodeDownloadFile() {
    return new File(getNodeExtractDir(), "node-v" + version + "-linux-x64.tar.xz");
  }

  @NotNull
  private File getNodeExtractDir() {
    return installDir != null ? new File(installDir) : new File(project.getBasedir(), "target/node/");
  }
}
