package io.struxel.sdk;

import java.util.logging.Logger;

public class Guard {
  private static final Logger LOGGER = Logger.getLogger(Guard.class.getName());

  private String enforcementMode;
  private int rateLimit;

  public Guard(String apiKey) {
    this(apiKey, "realtime", 500, LicenseValidator.DEFAULT_URL, 800, null);
  }

  public Guard(String apiKey, String enforcementMode, int rateLimit, String licenseServerUrl, int timeoutMs) {
    this(apiKey, enforcementMode, rateLimit, licenseServerUrl, timeoutMs, null);
  }

  public Guard(
      String apiKey,
      String enforcementMode,
      int rateLimit,
      String licenseServerUrl,
      int timeoutMs,
      LicenseValidator validator) {
    this.enforcementMode = enforcementMode;
    this.rateLimit = rateLimit;

    LicenseValidator validatorToUse =
        validator != null ? validator : new LicenseValidator(apiKey, licenseServerUrl, timeoutMs, "1.0.0", "java", null);

    LicenseValidator.LicenseInfo info = validatorToUse.validate();
    if (info.enforcementMode != null) {
      this.enforcementMode = info.enforcementMode;
    }
    if (info.rateLimit != null) {
      this.rateLimit = info.rateLimit;
    }
  }

  public CheckResult check(String prompt, String model, String userId) {
    return evaluate(prompt);
  }

  public CheckResult checkOutput(String output, String model) {
    return evaluate(output);
  }

  public CheckResult checkOutput(String output, String model, String userId) {
    return evaluate(output);
  }

  private CheckResult evaluate(String content) {
    long startNs = System.nanoTime();
    boolean blocked = content.contains("<struxel:block>");
    double latencyMs = (System.nanoTime() - startNs) / 1_000_000.0;

    CheckResult result = new CheckResult(
        blocked,
        blocked ? "Content violates policy" : null,
        blocked ? "policy.blocked_content" : null,
        enforcementMode,
        latencyMs);

    if (!blocked) {
      return result;
    }

    if ("realtime".equals(enforcementMode)) {
      throw new PolicyViolationException("Content blocked by Struxel Guard.", result);
    }

    if ("batch".equals(enforcementMode)) {
      LOGGER.info("Struxel violation recorded: " + result.getReason());
    } else {
      LOGGER.warning("Struxel advisory warning: " + result.getReason());
    }

    return result;
  }

  public int getRateLimit() {
    return rateLimit;
  }

  public String getEnforcementMode() {
    return enforcementMode;
  }
}
