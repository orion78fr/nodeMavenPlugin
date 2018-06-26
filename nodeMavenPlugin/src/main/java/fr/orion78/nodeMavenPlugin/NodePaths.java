package fr.orion78.nodeMavenPlugin;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

class NodePaths {
  private final String nodeURLString;
  private final File nodeInstallDir;
  private final File nodeDownloadFile;

  private NodePaths(@Nullable String nodeURL,
                    @Nullable String installDir,
                    @NotNull String version,
                    @NotNull String projectBuildDir) {
    this.nodeURLString = nodeURL != null && !nodeURL.isEmpty() ?
        nodeURL
        : "https://nodejs.org/dist/v" + version + "/node-v" + version + "-linux-x64.tar.xz";

    this.nodeInstallDir = installDir != null && !installDir.isEmpty() ?
        new File(installDir)
        : new File(projectBuildDir, "node/");

    this.nodeDownloadFile = new File(this.nodeInstallDir, "node-v" + version + "-linux-x64.tar.xz");
  }

  @NotNull
  @Contract("_,_,_,_ -> new")
  static NodePaths of(@Nullable String nodeURL,
                      @Nullable String installDir,
                      @NotNull String version,
                      @NotNull String projectBuildDir) {
    return new NodePaths(nodeURL, installDir, version, projectBuildDir);
  }

  @NotNull
  String getNodeURLString() {
    return nodeURLString;
  }

  @NotNull
  File getNodeInstallDir() {
    return nodeInstallDir;
  }

  @NotNull
  File getNodeDownloadFile() {
    return nodeDownloadFile;
  }
}
