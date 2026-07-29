import { describe, expect, it } from "vitest";

import { PolicyViolationError } from "../src/errors";
import { Guard } from "../src/guard";

const validator = (enforcement_mode: "realtime" | "batch" | "advisory") => ({
  validate: async () => ({
    valid: true,
    enforcement_mode,
    rate_limit: 500,
    tenant_id: "tenant",
    expires_at: "2027-01-01",
  }),
});

describe("Guard", () => {
  it("throws in realtime mode", async () => {
    const guard = new Guard({ apiKey: "sdk_test", validator: validator("realtime") });
    await expect(guard.check("<struxel:block>", "gpt-4", "user-1")).rejects.toBeInstanceOf(PolicyViolationError);
  });

  it("returns blocked result in batch mode", async () => {
    const guard = new Guard({ apiKey: "sdk_test", validator: validator("batch") });
    const result = await guard.check("<struxel:block>", "gpt-4", "user-1");

    expect(result.blocked).toBe(true);
    expect(result.enforcement_mode).toBe("batch");
    expect(result.reason).toBe("Content violates policy");
  });

  it("returns clean result in advisory mode", async () => {
    const guard = new Guard({ apiKey: "sdk_test", validator: validator("advisory") });
    const result = await guard.checkOutput("safe", "gpt-4");

    expect(result.blocked).toBe(false);
    expect(result.reason).toBeNull();
    expect(result.policy_id).toBeNull();
  });
});
