export class StruxelSDKError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "StruxelSDKError";
  }
}

export class PolicyViolationError extends StruxelSDKError {
  result?: unknown;

  constructor(message: string, result?: unknown) {
    super(message);
    this.name = "PolicyViolationError";
    this.result = result;
  }
}
