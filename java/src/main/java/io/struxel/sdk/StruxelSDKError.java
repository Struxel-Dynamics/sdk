package io.struxel.sdk;

public class StruxelSDKError extends RuntimeException {
  public StruxelSDKError(String message) {
    super(message);
  }

  public StruxelSDKError(String message, Throwable cause) {
    super(message, cause);
  }
}
