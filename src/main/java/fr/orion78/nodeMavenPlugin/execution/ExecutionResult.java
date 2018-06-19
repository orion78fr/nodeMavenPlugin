package fr.orion78.nodeMavenPlugin.execution;

import org.jetbrains.annotations.NotNull;

public class ExecutionResult {
  private final int exitVal;
  private final String out;
  private final String err;

  public ExecutionResult(int exitVal, @NotNull String out, @NotNull String err) {
    this.exitVal = exitVal;
    this.out = out;
    this.err = err;
  }

  public int getExitVal() {
    return exitVal;
  }

  @NotNull
  public String getOut() {
    return out;
  }

  @NotNull
  public String getErr() {
    return err;
  }
}
