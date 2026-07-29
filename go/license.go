package struxelsdk

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"strings"
	"time"
)

const defaultLicenseURL = "https://api.struxel.ai/v1/license/validate"

type licenseResponse struct {
	Valid           bool   `json:"valid"`
	EnforcementMode string `json:"enforcement_mode"`
	RateLimit       int    `json:"rate_limit"`
	TenantID        string `json:"tenant_id"`
	ExpiresAt       string `json:"expires_at"`
}

type LicenseValidator struct {
	apiKey           string
	licenseServerURL string
	timeout          time.Duration
	sdkVersion       string
	language         string
	httpClient       *http.Client
	cache            *licenseResponse
	cacheExpiresAt   time.Time
	now              func() time.Time
}

func NewLicenseValidator(apiKey, licenseServerURL string, timeoutMS int) *LicenseValidator {
	url := licenseServerURL
	if url == "" {
		url = defaultLicenseURL
	}

	return &LicenseValidator{
		apiKey:           apiKey,
		licenseServerURL: url,
		timeout:          time.Duration(timeoutMS) * time.Millisecond,
		sdkVersion:       "1.0.0",
		language:         "go",
		httpClient:       &http.Client{Timeout: time.Duration(timeoutMS) * time.Millisecond},
		now:              time.Now,
	}
}

func (v *LicenseValidator) Validate() (*licenseResponse, error) {
	now := v.now()
	if v.cache != nil && now.Before(v.cacheExpiresAt) {
		return v.cache, nil
	}

	if !strings.HasPrefix(v.apiKey, "sdk_") {
		return nil, &StruxelSDKError{Message: "Invalid API key format. Expected sdk_<uuid>."}
	}

	payload := map[string]string{
		"api_key":     v.apiKey,
		"sdk_version": v.sdkVersion,
		"language":    v.language,
	}
	data, _ := json.Marshal(payload)

	req, err := http.NewRequest(http.MethodPost, v.licenseServerURL, bytes.NewReader(data))
	if err != nil {
		return nil, &StruxelSDKError{Message: "License validation request failed."}
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := v.httpClient.Do(req)
	if err != nil {
		return nil, &StruxelSDKError{Message: "License validation request failed."}
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return nil, &StruxelSDKError{Message: fmt.Sprintf("License validation failed with HTTP %d.", resp.StatusCode)}
	}

	var body licenseResponse
	if err := json.NewDecoder(resp.Body).Decode(&body); err != nil {
		return nil, &StruxelSDKError{Message: "Invalid response from license server."}
	}

	if !body.Valid {
		return nil, &StruxelSDKError{Message: "License validation failed: license is invalid."}
	}

	if body.ExpiresAt != "" {
		expiresAt, err := time.Parse("2006-01-02", body.ExpiresAt)
		todayUTC := time.Date(now.UTC().Year(), now.UTC().Month(), now.UTC().Day(), 0, 0, 0, 0, time.UTC)
		if err == nil && expiresAt.Before(todayUTC) {
			return nil, &StruxelSDKError{Message: "License validation failed: license is expired."}
		}
	}

	v.cache = &body
	v.cacheExpiresAt = now.Add(5 * time.Minute)
	return &body, nil
}
