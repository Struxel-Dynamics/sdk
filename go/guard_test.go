package struxelsdk

import (
	"io"
	"net/http"
	"strings"
	"testing"
)

type roundTripFunc func(*http.Request) (*http.Response, error)

func (f roundTripFunc) RoundTrip(r *http.Request) (*http.Response, error) {
	return f(r)
}

func createGuard(t *testing.T, mode string) *Guard {
	t.Helper()
	validator := NewLicenseValidator("sdk_test", "http://example.com", 800)
	validator.httpClient = &http.Client{Transport: roundTripFunc(func(req *http.Request) (*http.Response, error) {
		body := `{"valid":true,"enforcement_mode":"` + mode + `","rate_limit":500,"expires_at":"2027-01-01"}`
		return &http.Response{
			StatusCode: 200,
			Header:     make(http.Header),
			Body:       io.NopCloser(strings.NewReader(body)),
		}, nil
	})}

	license, err := validator.Validate()
	if err != nil {
		t.Fatalf("validate: %v", err)
	}

	return &Guard{enforcementMode: license.EnforcementMode, rateLimit: license.RateLimit, validator: validator}
}

func TestRealtimeThrows(t *testing.T) {
	guard := createGuard(t, "realtime")
	_, err := guard.Check("hello <struxel:block>", "gpt-4", "user-1")
	if err == nil {
		t.Fatalf("expected policy violation error")
	}
}

func TestBatchReturnsBlocked(t *testing.T) {
	guard := createGuard(t, "batch")
	result, err := guard.Check("hello <struxel:block>", "gpt-4", "user-1")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if !result.Blocked {
		t.Fatalf("expected blocked result")
	}
	if result.EnforcementMode != "batch" {
		t.Fatalf("expected batch mode, got %s", result.EnforcementMode)
	}
}

func TestAdvisoryAllowsCleanOutput(t *testing.T) {
	guard := createGuard(t, "advisory")
	result, err := guard.CheckOutput("safe output", "gpt-4")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if result.Blocked {
		t.Fatalf("expected non-blocked result")
	}
}
