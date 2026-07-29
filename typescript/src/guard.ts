import { PolicyViolationError } from "./errors";
import { LicenseValidator } from "./license";
import type { CheckResult, GuardOptions } from "./types";

export class Guard {
  private enforcementMode: "realtime" | "batch" | "advisory";
  private rateLimit: number;
  private readonly validator: { validate: () => Promise<any> };
  private readonly initPromise: Promise<void>;

  constructor(options: GuardOptions) {
    this.enforcementMode = options.enforcementMode ?? "realtime";
    this.rateLimit = options.rateLimit ?? 500;
    this.validator =
      options.validator ??
      new LicenseValidator(
        options.apiKey,
        options.licenseServerUrl ?? LicenseValidator.DEFAULT_URL,
        options.timeoutMs ?? 800,
      );

    this.initPromise = this.initialize();
  }

  private async initialize(): Promise<void> {
    const license = await this.validator.validate();
    this.enforcementMode = (license.enforcement_mode as "realtime" | "batch" | "advisory") ?? this.enforcementMode;
    this.rateLimit = license.rate_limit ?? this.rateLimit;
  }

  async check(prompt: string, model: string, userId: string): Promise<CheckResult> {
    await this.initPromise;
    return this.evaluate(prompt);
  }

  async checkOutput(output: string, model: string, userId?: string): Promise<CheckResult> {
    await this.initPromise;
    return this.evaluate(output);
  }

  private evaluate(content: string): CheckResult {
    const start = performance.now();
    const blocked = content.includes("<struxel:block>");
    const latency = performance.now() - start;

    const result: CheckResult = {
      blocked,
      reason: blocked ? "Content violates policy" : null,
      policy_id: blocked ? "policy.blocked_content" : null,
      enforcement_mode: this.enforcementMode,
      latency_ms: latency,
    };

    if (!blocked) {
      return result;
    }

    if (this.enforcementMode === "realtime") {
      throw new PolicyViolationError("Content blocked by Struxel Guard.", result);
    }

    if (this.enforcementMode === "batch") {
      console.info("Struxel violation recorded", result.reason);
    } else {
      console.warn("Struxel advisory warning", result.reason);
    }

    return result;
  }
}
