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
  @Parameter(defaultValue = "--help")
  private String args;
  @Parameter(defaultValue = "8.11.2")
  private String version;
  @Parameter
  private String nodeURL;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  public void execute() throws MojoExecutionException {
    getLog().info("Node args " + args);

    File extractDir = getNodeExtractDir();
    if (extractDir.exists()) {
      getLog().info("Node already downloaded to " + extractDir);
    } else {
      // Download node
      File downloadedFile = getNodeDownloadFile();
      getLog().info("Node version " + version);
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

    // Execute
    List<String> command = new ArrayList<>();
    command.add(new File(extractDir, "bin/node").toString());
    command.addAll(Arrays.asList(args.split(" "))); // TODO this does not split correctly quoted args
    try {
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
      permissions.add(PosixFilePermission.OTHERS_READ);
    }
    if ((mode & (1 << 1)) != 0) {
      permissions.add(PosixFilePermission.OTHERS_WRITE);
    }
    if ((mode & (1 << 2)) != 0) {
      permissions.add(PosixFilePermission.OTHERS_EXECUTE);
    }
    if ((mode & (1 << 3)) != 0) {
      permissions.add(PosixFilePermission.GROUP_READ);
    }
    if ((mode & (1 << 4)) != 0) {
      permissions.add(PosixFilePermission.GROUP_WRITE);
    }
    if ((mode & (1 << 5)) != 0) {
      permissions.add(PosixFilePermission.GROUP_EXECUTE);
    }
    if ((mode & (1 << 6)) != 0) {
      permissions.add(PosixFilePermission.OWNER_READ);
    }
    if ((mode & (1 << 7)) != 0) {
      permissions.add(PosixFilePermission.OWNER_WRITE);
    }
    if ((mode & (1 << 8)) != 0) {
      permissions.add(PosixFilePermission.OWNER_EXECUTE);
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
    return new File(project.getBasedir(), "target/node/");
  }
}
