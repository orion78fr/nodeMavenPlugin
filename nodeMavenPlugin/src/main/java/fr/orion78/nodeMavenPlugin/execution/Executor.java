package fr.orion78.nodeMavenPlugin.execution;

import fr.orion78.nodeMavenPlugin.utils.CommandLineUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    if (executableName != null && !executableName.isEmpty()) {
      File executable = new File(nodeBinDir, executableName);
      if (!executable.exists() || !executable.isFile()) {
        throw new IOException("Executable not found : " + executable);
      }
      if (!executable.canExecute()) {
        throw new IOException("Command is not executable : " + executable);
      }

      command.add(executable.toString());
    }

    command.addAll(CommandLineUtils.translateCommandline(execution.getArgs()));

    Process p = new ProcessBuilder(command).start();
    p.waitFor(timeoutInSeconds, TimeUnit.SECONDS);
    int exitVal = p.exitValue();

    String out = new String(IOUtils.toByteArray(p.getInputStream()), StandardCharsets.UTF_8);
    String err = new String(IOUtils.toByteArray(p.getErrorStream()), StandardCharsets.UTF_8);

    return new ExecutionResult(exitVal, out, err);
  }
}
