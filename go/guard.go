package struxelsdk

import (
	"log"
	"strings"
	"time"
)

type Guard struct {
	enforcementMode string
	rateLimit       int
	validator       *LicenseValidator
}

func NewGuard(apiKey, enforcementMode string, rateLimit int, licenseServerURL string, timeoutMS int) (*Guard, error) {
	if enforcementMode == "" {
		enforcementMode = "realtime"
	}
	if rateLimit == 0 {
		rateLimit = 500
	}
	if timeoutMS == 0 {
		timeoutMS = 800
	}

	validator := NewLicenseValidator(apiKey, licenseServerURL, timeoutMS)
	license, err := validator.Validate()
	if err != nil {
		return nil, err
	}

	if license.EnforcementMode != "" {
		enforcementMode = license.EnforcementMode
	}
	if license.RateLimit != 0 {
		rateLimit = license.RateLimit
	}

	return &Guard{enforcementMode: enforcementMode, rateLimit: rateLimit, validator: validator}, nil
}

func (g *Guard) Check(prompt, model, userID string) (CheckResult, error) {
	return g.evaluate(prompt)
}

func (g *Guard) CheckOutput(output, model string, userID ...string) (CheckResult, error) {
	return g.evaluate(output)
}

func (g *Guard) evaluate(content string) (CheckResult, error) {
	start := time.Now()
	blocked := strings.Contains(content, "<struxel:block>")
	latencyMS := float64(time.Since(start).Nanoseconds()) / 1_000_000

	var reason *string
	var policyID *string
	if blocked {
		r := "Content violates policy"
		p := "policy.blocked_content"
		reason = &r
		policyID = &p
	}

	result := CheckResult{
		Blocked:         blocked,
		Reason:          reason,
		PolicyID:        policyID,
		EnforcementMode: g.enforcementMode,
		LatencyMS:       latencyMS,
	}

	if !blocked {
		return result, nil
	}

	if g.enforcementMode == "realtime" {
		return result, &PolicyViolationError{Message: "Content blocked by Struxel Guard.", Result: result}
	}

	if g.enforcementMode == "batch" {
		log.Printf("Struxel violation recorded: %s", *reason)
	} else {
		log.Printf("Struxel advisory warning: %s", *reason)
	}

	return result, nil
}
