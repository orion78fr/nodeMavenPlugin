package fr.orion78.nodeMavenPlugin.execution;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Execution {
  /**
   * Nodejs executable name (e.g. uglifyjs). <br/>
   * Can be empty (then node with be called directly with {@link #args}).
   */
  private String executableName;
  /**
   * Arguments for the command execution.
   */
  private String args;

  /**
   * Mandatory empty constructor for maven reactor injection
   */
  @SuppressWarnings("unused")
  public Execution() {
  }

  public Execution(@Nullable String executableName, @NotNull String args) {
    this.executableName = executableName;
    this.args = args;
  }

  @Nullable
  public String getExecutableName() {
    return executableName;
  }

  @Nullable
  public String getArgs() {
    return args;
  }

  @Override
  public String toString() {
    return executableName + " " + args;
  }
}
