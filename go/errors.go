package struxelsdk

type StruxelSDKError struct {
	Message string
}

func (e *StruxelSDKError) Error() string {
	return e.Message
}

type PolicyViolationError struct {
	Message string
	Result  CheckResult
}

func (e *PolicyViolationError) Error() string {
	return e.Message
}
