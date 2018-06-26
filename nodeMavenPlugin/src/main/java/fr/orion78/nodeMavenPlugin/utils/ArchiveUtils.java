package fr.orion78.nodeMavenPlugin.utils;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.maven.plugin.logging.Log;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

public class ArchiveUtils {
  public static void extractTar(@NotNull File fileToExtract,
                                @NotNull File folder,
                                int foldersToRemove,
                                @NotNull Log log) throws IOException {
    try (FileInputStream fis = new FileInputStream(fileToExtract);
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

        String finalName = name;
        for (int i = 0; i < foldersToRemove; i++) {
          int idx = finalName.indexOf('/');
          if (idx == -1) {
            throw new IOException("Cannot remove " + foldersToRemove + " folders from " + name);
          }
          finalName = finalName.substring(idx);
        }

        File f = new File(folder, finalName);
        if (entry.isDirectory()) {
          log.debug("Creating directory " + f);
          if (!f.isDirectory() && !f.mkdirs()) {
            throw new IOException("Failed to create directory " + f);
          }
          Files.setPosixFilePermissions(f.toPath(), PermissionUtils.modeToPermissionSet(entry.getMode()));
        } else if (entry.isSymbolicLink()) {
          log.debug("Creating symbolic link " + f + " --> " + entry.getLinkName());
          File parent = f.getParentFile();
          if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Failed to create directory " + parent);
          }

          Files.createSymbolicLink(f.toPath(), new File(entry.getLinkName()).toPath());
        } else if (entry.isFile()) {
          log.debug("Extracting file " + f);
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
    }
  }
}
