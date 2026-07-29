class StruxelSDKError(Exception):
    """Base exception for Struxel SDK errors."""


class PolicyViolationError(StruxelSDKError):
    """Raised when content violates a policy in realtime mode."""

    def __init__(self, message: str, result=None):
        super().__init__(message)
        self.result = result
