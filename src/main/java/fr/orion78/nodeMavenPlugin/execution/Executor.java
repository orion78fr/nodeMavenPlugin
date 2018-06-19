package fr.orion78.nodeMavenPlugin.execution;

import fr.orion78.nodeMavenPlugin.utils.CommandLineUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Executor {
  private final File nodeBinDir;
  private final File nodeExe;

  public Executor(@NotNull File nodeExtractDir) {
    this.nodeBinDir = new File(nodeExtractDir, "bin");
    this.nodeExe = new File(nodeBinDir, "node");
  }

  @NotNull
  public ExecutionResult execute(@NotNull Execution execution,
                                 long timeoutInSeconds) throws InterruptedException, IOException {
    List<String> command = new ArrayList<>();
    command.add(nodeExe.toString());

    String executableName = execution.getExecutableName();
    if (!executableName.isEmpty()) {
      command.add(new File(nodeBinDir, executableName).toString());
    }

    command.addAll(CommandLineUtils.translateCommandline(execution.getArgs()));

    Process p = new ProcessBuilder(command).start();
    p.waitFor(timeoutInSeconds, TimeUnit.SECONDS);
    int exitVal = p.exitValue();

    try (BufferedReader buffer = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
      String line = buffer.readLine();
      if (!('v' + version).equals(line)) {
        throw new MojoExecutionException("Expected version of node is v" + version + " but got " + line);
      }
    }
  }
}
