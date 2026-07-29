package io.struxel.sdk;

public class CheckResult {
  private final boolean blocked;
  private final String reason;
  private final String policyId;
  private final String enforcementMode;
  private final double latencyMs;

  public CheckResult(boolean blocked, String reason, String policyId, String enforcementMode, double latencyMs) {
    this.blocked = blocked;
    this.reason = reason;
    this.policyId = policyId;
    this.enforcementMode = enforcementMode;
    this.latencyMs = latencyMs;
  }

  public boolean isBlocked() {
    return blocked;
  }

  public String getReason() {
    return reason;
  }

  public String getPolicyId() {
    return policyId;
  }

  public String getEnforcementMode() {
    return enforcementMode;
  }

  public double getLatencyMs() {
    return latencyMs;
  }
}
