package io.struxel.sdk;

public class PolicyViolationException extends RuntimeException {
  private final CheckResult result;

  public PolicyViolationException(String message, CheckResult result) {
    super(message);
    this.result = result;
  }

  public CheckResult getResult() {
    return result;
  }
}
