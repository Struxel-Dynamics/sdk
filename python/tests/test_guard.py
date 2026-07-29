import pytest

from struxel_sdk.exceptions import PolicyViolationError
from struxel_sdk.guard import Guard


class FakeValidator:
    def __init__(self, enforcement_mode: str = "realtime") -> None:
        self.enforcement_mode = enforcement_mode

    def validate(self):
        return {
            "valid": True,
            "enforcement_mode": self.enforcement_mode,
            "rate_limit": 500,
            "tenant_id": "tenant",
            "expires_at": "2027-01-01",
        }


def test_realtime_raises_on_blocked_prompt():
    guard = Guard(api_key="sdk_test", validator=FakeValidator("realtime"))

    with pytest.raises(PolicyViolationError):
        guard.check(prompt="hello <struxel:block>", model="gpt-4", user_id="user-1")


def test_batch_returns_blocked_result_without_raise():
    guard = Guard(api_key="sdk_test", validator=FakeValidator("batch"))
    result = guard.check(prompt="hello <struxel:block>", model="gpt-4", user_id="user-1")

    assert result.blocked is True
    assert result.enforcement_mode == "batch"
    assert result.reason == "Content violates policy"


def test_advisory_allows_clean_output():
    guard = Guard(api_key="sdk_test", validator=FakeValidator("advisory"))
    result = guard.check_output(output="clean output", model="gpt-4")

    assert result.blocked is False
    assert result.reason is None
    assert result.policy_id is None
