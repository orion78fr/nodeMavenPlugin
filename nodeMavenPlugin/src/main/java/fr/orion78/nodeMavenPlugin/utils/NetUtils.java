package fr.orion78.nodeMavenPlugin.utils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class NetUtils {
  public static void localize(@NotNull String urlString, @NotNull File file) throws IOException {
    URL url;
    try {
      url = new URL(urlString);
    } catch (MalformedURLException e) {
      throw new IOException("Problem in node url " + urlString, e);
    }

    localize(url, file);
  }

  public static void localize(@NotNull URL url, @NotNull File file) throws IOException {
    if (!file.getParentFile().mkdirs()) {
      throw new IOException("Cannot create folder " + file.getParent());
    }

    try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
         FileOutputStream fos = new FileOutputStream(file)) {
      fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    }
  }
}
