package io.struxel.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class GuardTest {
  private Guard createGuard(String mode) {
    LicenseValidator validator = new LicenseValidator(
        "sdk_test",
        LicenseValidator.DEFAULT_URL,
        800,
        "1.0.0",
        "java",
        (url, body, timeout) ->
            "{\"valid\":true,\"enforcement_mode\":\"" + mode + "\",\"rate_limit\":500,\"expires_at\":\"2027-01-01\"}");
    return new Guard("sdk_test", mode, 500, LicenseValidator.DEFAULT_URL, 800, validator);
  }

  @Test
  void throwsInRealtimeMode() {
    Guard guard = createGuard("realtime");
    assertThrows(PolicyViolationException.class, () -> guard.check("hello <struxel:block>", "gpt-4", "user-1"));
  }

  @Test
  void returnsBlockedInBatchMode() {
    Guard guard = createGuard("batch");
    CheckResult result = guard.check("hello <struxel:block>", "gpt-4", "user-1");

    assertTrue(result.isBlocked());
    assertEquals("batch", result.getEnforcementMode());
    assertEquals("Content violates policy", result.getReason());
  }

  @Test
  void returnsCleanResult() {
    Guard guard = createGuard("advisory");
    CheckResult result = guard.checkOutput("safe output", "gpt-4");

    assertFalse(result.isBlocked());
  }
}
