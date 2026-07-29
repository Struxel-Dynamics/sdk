import { StruxelSDKError } from "./errors";
import type { LicenseResponse } from "./types";

const CACHE_TTL_MS = 5 * 60 * 1000;

export class LicenseValidator {
  static readonly DEFAULT_URL = "https://api.struxel.ai/v1/license/validate";

  private cache: { expiresAt: number; value: LicenseResponse } | null = null;

  constructor(
    private readonly apiKey: string,
    private readonly licenseServerUrl: string = LicenseValidator.DEFAULT_URL,
    private readonly timeoutMs: number = 800,
    private readonly sdkVersion: string = "1.0.0",
    private readonly language: string = "typescript",
  ) {}

  async validate(): Promise<LicenseResponse> {
    const now = Date.now();
    if (this.cache && now < this.cache.expiresAt) {
      return this.cache.value;
    }

    if (!this.apiKey.startsWith("sdk_")) {
      throw new StruxelSDKError("Invalid API key format. Expected sdk_<uuid>.");
    }

    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), this.timeoutMs);

    let body: LicenseResponse;
    try {
      const response = await fetch(this.licenseServerUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          api_key: this.apiKey,
          sdk_version: this.sdkVersion,
          language: this.language,
        }),
        signal: controller.signal,
      });

      if (!response.ok) {
        throw new StruxelSDKError(`License validation failed with HTTP ${response.status}.`);
      }
      body = (await response.json()) as LicenseResponse;
    } catch (error) {
      if (error instanceof StruxelSDKError) {
        throw error;
      }
      throw new StruxelSDKError("License validation request failed.");
    } finally {
      clearTimeout(timer);
    }

    if (!body.valid) {
      throw new StruxelSDKError("License validation failed: license is invalid.");
    }

    if (body.expires_at) {
      const expiry = new Date(`${body.expires_at}T00:00:00.000Z`);
      const now = new Date();
      const todayUtc = Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate());
      if (expiry.getTime() < todayUtc) {
        throw new StruxelSDKError("License validation failed: license is expired.");
      }
    }

    this.cache = { expiresAt: now + CACHE_TTL_MS, value: body };
    return body;
  }
}
