package io.struxel.sdk;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LicenseValidator {
  public static final String DEFAULT_URL = "https://api.struxel.ai/v1/license/validate";
  private static final long CACHE_TTL_MS = 300_000;

  private final String apiKey;
  private final String licenseServerUrl;
  private final int timeoutMs;
  private final String sdkVersion;
  private final String language;
  private final Transport transport;

  private LicenseInfo cached;
  private long cacheExpiresAtMs;

  @FunctionalInterface
  public interface Transport {
    String post(String url, String body, int timeoutMs) throws IOException, InterruptedException;
  }

  public static class LicenseInfo {
    public final boolean valid;
    public final String enforcementMode;
    public final Integer rateLimit;
    public final String expiresAt;

    public LicenseInfo(boolean valid, String enforcementMode, Integer rateLimit, String expiresAt) {
      this.valid = valid;
      this.enforcementMode = enforcementMode;
      this.rateLimit = rateLimit;
      this.expiresAt = expiresAt;
    }
  }

  public LicenseValidator(String apiKey) {
    this(apiKey, DEFAULT_URL, 800, "1.0.0", "java", null);
  }

  public LicenseValidator(
      String apiKey,
      String licenseServerUrl,
      int timeoutMs,
      String sdkVersion,
      String language,
      Transport transport) {
    this.apiKey = apiKey;
    this.licenseServerUrl = licenseServerUrl;
    this.timeoutMs = timeoutMs;
    this.sdkVersion = sdkVersion;
    this.language = language;
    this.transport = transport;
  }

  public synchronized LicenseInfo validate() {
    long now = System.currentTimeMillis();
    if (cached != null && now < cacheExpiresAtMs) {
      return cached;
    }

    if (!apiKey.startsWith("sdk_")) {
      throw new StruxelSDKError("Invalid API key format. Expected sdk_<uuid>.");
    }

    String payload = String.format(
        "{\"api_key\":\"%s\",\"sdk_version\":\"%s\",\"language\":\"%s\"}",
        escapeJson(apiKey),
        escapeJson(sdkVersion),
        escapeJson(language));

    String body;
    try {
      body = transport != null ? transport.post(licenseServerUrl, payload, timeoutMs) : defaultPost(licenseServerUrl, payload, timeoutMs);
    } catch (IOException | InterruptedException e) {
      throw new StruxelSDKError("License validation request failed.", e);
    }

    LicenseInfo info = parseLicenseResponse(body);
    if (!info.valid) {
      throw new StruxelSDKError("License validation failed: license is invalid.");
    }
    if (info.expiresAt != null && LocalDate.parse(info.expiresAt).isBefore(LocalDate.now(ZoneOffset.UTC))) {
      throw new StruxelSDKError("License validation failed: license is expired.");
    }

    cached = info;
    cacheExpiresAtMs = now + CACHE_TTL_MS;
    return info;
  }

  private String defaultPost(String url, String body, int timeoutMs) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .timeout(java.time.Duration.ofMillis(timeoutMs))
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException("HTTP " + response.statusCode());
    }

    return response.body();
  }

  private LicenseInfo parseLicenseResponse(String json) {
    boolean valid = extractBoolean(json, "valid");
    String enforcementMode = extractString(json, "enforcement_mode");
    Integer rateLimit = extractInteger(json, "rate_limit");
    String expiresAt = extractString(json, "expires_at");
    return new LicenseInfo(valid, enforcementMode, rateLimit, expiresAt);
  }

  private String extractString(String json, String key) {
    Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
    Matcher m = p.matcher(json);
    return m.find() ? m.group(1) : null;
  }

  private Integer extractInteger(String json, String key) {
    Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
    Matcher m = p.matcher(json);
    return m.find() ? Integer.parseInt(m.group(1)) : null;
  }

  private boolean extractBoolean(String json, String key) {
    Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*(true|false)");
    Matcher m = p.matcher(json);
    return m.find() && Boolean.parseBoolean(m.group(1));
  }

  private String escapeJson(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\b", "\\b")
        .replace("\f", "\\f")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}
