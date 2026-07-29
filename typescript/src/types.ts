export interface CheckResult {
  blocked: boolean;
  reason: string | null;
  policy_id: string | null;
  enforcement_mode: string;
  latency_ms: number;
}

export interface GuardOptions {
  apiKey: string;
  enforcementMode?: "realtime" | "batch" | "advisory";
  rateLimit?: number;
  licenseServerUrl?: string;
  timeoutMs?: number;
  validator?: {
    validate: () => Promise<LicenseResponse>;
  };
}

export interface LicenseResponse {
  valid: boolean;
  enforcement_mode?: string;
  rate_limit?: number;
  tenant_id?: string;
  expires_at?: string;
}
