from dataclasses import dataclass
from typing import Optional


@dataclass
class PolicyViolation:
    reason: str
    policy_id: str


@dataclass
class CheckResult:
    blocked: bool
    reason: Optional[str]
    policy_id: Optional[str]
    enforcement_mode: str
    latency_ms: float
