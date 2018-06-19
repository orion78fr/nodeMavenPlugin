package fr.orion78.nodeMavenPlugin.execution;

import org.apache.maven.plugins.annotations.Parameter;
import org.jetbrains.annotations.NotNull;

public class Execution {
  /**
   * Nodejs executable name (e.g. uglifyjs). <br/>
   * Can be empty (then node with be called directly with {@link #args}).
   */
  @Parameter
  private String executableName;
  /**
   * Arguments for the command execution. <br/>
   */
  @Parameter(required = true)
  private String args;

  public Execution() {
  }

  public Execution(@NotNull String executableName, @NotNull String args) {
    this.executableName = executableName;
    this.args = args;
  }

  @NotNull
  public String getExecutableName() {
    return executableName;
  }

  @NotNull
  public String getArgs() {
    return args;
  }
}
