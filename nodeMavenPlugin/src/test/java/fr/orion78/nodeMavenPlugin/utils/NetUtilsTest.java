package fr.orion78.nodeMavenPlugin.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

class NetUtilsTest {
  @Test
  void testDownload() throws IOException {
    UUID uuid = UUID.randomUUID();
    File downloadedFile = new File("target", uuid.toString());
    Assertions.assertFalse(downloadedFile.exists());

    NetUtils.localize("https://nodejs.org/dist/v8.11.3/node-v8.11.3-linux-x64.tar.xz", downloadedFile);
  }
}