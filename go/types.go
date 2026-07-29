package struxelsdk

type CheckResult struct {
	Blocked         bool
	Reason          *string
	PolicyID        *string
	EnforcementMode string
	LatencyMS       float64
}
