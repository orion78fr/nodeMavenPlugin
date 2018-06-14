package fr.orion78.nodeMavenPlugin;

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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Mojo(
    name = "execute",
    defaultPhase = LifecyclePhase.PROCESS_RESOURCES
)
public class NodeMojo extends AbstractMojo {
  @Parameter
  private String globalScriptToExecute;
  @Parameter(property = "nodePlugin.exec.args", defaultValue = "--help")
  private String args;
  @Parameter(defaultValue = "8.11.2")
  private String version;
  @Parameter
  private String nodeURL;
  @Parameter(property = "nodePlugin.node.install.directory")
  private String installDir;
  @Parameter
  private String[] dependencies;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  public void execute() throws MojoExecutionException {
    getLog().info("Node args " + args);

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
            Files.setPosixFilePermissions(f.toPath(), modeToPermissionSet(entry.getMode()));
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
            Files.setPosixFilePermissions(f.toPath(), modeToPermissionSet(entry.getMode()));
          } else {
            throw new IOException("Unsupported entry type " + entry.getName());
          }
        }
      } catch (IOException | InterruptedException e) {
        throw new MojoExecutionException("Error while decompressing", e);
      }
    }

    // Install check
    File nodeExe = new File(extractDir, "bin/node");
    if (!nodeExe.exists() || !nodeExe.isFile()) {
      throw new MojoExecutionException("Node executable not found : " + nodeExe);
    }
    if (!nodeExe.canExecute()) {
      throw new MojoExecutionException("Cannot execute node executable : " + nodeExe);
    }
    try {
      List<String> command = Arrays.asList(nodeExe.toString(), "--version");
      Process p = new ProcessBuilder(command).start();
      p.waitFor(10, TimeUnit.SECONDS);
      int exitVal = p.exitValue();
      if (exitVal != 0) {
        throw new MojoExecutionException("Execution returned a non zero exit value : " + exitVal);
      }
      try (BufferedReader buffer = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
        String line = buffer.readLine();
        if (!('v' + version).equals(line)) {
          throw new MojoExecutionException("Expected version of node is v" + version + " but got " + line);
        }
      }
    } catch (InterruptedException | IOException e) {
      throw new MojoExecutionException("Error while executing", e);
    }

    getLog().info("Node version " + version);

    // Install deps
    // TODO remove this
    dependencies = new String[]{"uglify-js@3.4.0"};
    if (dependencies != null && dependencies.length != 0) {
      File npmJs = new File(extractDir, "bin/npm");
      if (!npmJs.exists() || !npmJs.isFile()) {
        throw new MojoExecutionException("Npm executable not found : " + npmJs);
      }
      if (!npmJs.canExecute()) {
        throw new MojoExecutionException("Cannot execute npm executable : " + npmJs);
      }
      try {
        List<String> command = new ArrayList<>();
        command.add(nodeExe.toString());
        command.add(npmJs.toString());
        command.add("install");
        command.add("-g");
        command.addAll(Arrays.asList(dependencies));
        Process p = new ProcessBuilder(command).start();
        p.waitFor(10, TimeUnit.SECONDS);
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
          buffer.lines().forEach(l -> getLog().info(l));
        }
        int exitVal = p.exitValue();
        if (exitVal != 0) {
          throw new MojoExecutionException("Execution returned a non zero exit value : " + exitVal);
        }
      } catch (InterruptedException | IOException e) {
        throw new MojoExecutionException("Error while executing", e);
      }
    }

    // Execute
    try {
      // TODO remove this
      globalScriptToExecute = "uglifyjs";
      List<String> command = new ArrayList<>();
      command.add(nodeExe.toString());
      if (globalScriptToExecute != null && !globalScriptToExecute.isEmpty()) {
        command.add(new File(new File(extractDir, "bin"), globalScriptToExecute).toString());
      }
      command.addAll(Arrays.asList(args.split(" "))); // TODO this does not split correctly quoted args
      Process p = new ProcessBuilder(command).start();
      p.waitFor(10, TimeUnit.SECONDS);
      int exitVal = p.exitValue();
      if (exitVal != 0) {
        throw new MojoExecutionException("Execution returned a non zero exit value : " + exitVal);
      }

      getLog().info("Node help");
      getLog().info("");
      try (BufferedReader buffer = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
        buffer.lines().forEach(l -> getLog().info(l));
      }
      getLog().info("");
    } catch (IOException | InterruptedException e) {
      throw new MojoExecutionException("Error while executing", e);
    }
  }

  @NotNull
  private Set<PosixFilePermission> modeToPermissionSet(int mode) {
    Set<PosixFilePermission> permissions = new HashSet<>();

    if ((mode & 1) != 0) {
      permissions.add(PosixFilePermission.OTHERS_EXECUTE);
    }
    if ((mode & (1 << 1)) != 0) {
      permissions.add(PosixFilePermission.OTHERS_WRITE);
    }
    if ((mode & (1 << 2)) != 0) {
      permissions.add(PosixFilePermission.OTHERS_READ);
    }
    if ((mode & (1 << 3)) != 0) {
      permissions.add(PosixFilePermission.GROUP_EXECUTE);
    }
    if ((mode & (1 << 4)) != 0) {
      permissions.add(PosixFilePermission.GROUP_WRITE);
    }
    if ((mode & (1 << 5)) != 0) {
      permissions.add(PosixFilePermission.GROUP_READ);
    }
    if ((mode & (1 << 6)) != 0) {
      permissions.add(PosixFilePermission.OWNER_EXECUTE);
    }
    if ((mode & (1 << 7)) != 0) {
      permissions.add(PosixFilePermission.OWNER_WRITE);
    }
    if ((mode & (1 << 8)) != 0) {
      permissions.add(PosixFilePermission.OWNER_READ);
    }

    return permissions;
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
