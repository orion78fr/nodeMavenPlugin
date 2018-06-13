package fr.orion78.nodeMavenPlugin;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
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
  @Parameter(defaultValue = "--version")
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
           CompressorInputStream uncompressed = new CompressorStreamFactory().createCompressorInputStream(bis);
           BufferedInputStream uncompressedBuffered = new BufferedInputStream(uncompressed);
           ArchiveInputStream archive = new ArchiveStreamFactory().createArchiveInputStream(uncompressedBuffered)) {
        ArchiveEntry entry;
        while ((entry = archive.getNextEntry()) != null) {
          String name = entry.getName();
          if (!archive.canReadEntryData(entry)) {
            throw new IOException("Cannot read data " + name);
          }

          File f = new File(extractDir, name.substring(name.indexOf('/'))); // Remove first subfolder
          if (entry.isDirectory()) {
            if (!f.isDirectory() && !f.mkdirs()) {
              throw new IOException("Failed to create directory " + f);
            }
          } else {
            File parent = f.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) {
              throw new IOException("Failed to create directory " + parent);
            }

            try (OutputStream o = Files.newOutputStream(f.toPath())) {
              IOUtils.copy(archive, o);
            }
          }
        }
      } catch (CompressorException | ArchiveException | IOException e) {
        throw new MojoExecutionException("Error while decompressing", e);
      }
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
    return new File(getNodeExtractDir(), "/node-v" + version + "-linux-x64.tar.xz");
  }

  @NotNull
  private File getNodeExtractDir() {
    return new File(project.getBasedir(), "target/node/");
  }
}
