import logging
import time
from typing import Optional

from .exceptions import PolicyViolationError
from .license import LicenseValidator
from .models import CheckResult


class Guard:
    def __init__(
        self,
        api_key: str,
        enforcement_mode: str = "realtime",
        rate_limit: int = 500,
        license_server_url: str = LicenseValidator.DEFAULT_URL,
        timeout_ms: int = 800,
        validator: Optional[LicenseValidator] = None,
    ) -> None:
        self._validator = validator or LicenseValidator(
            api_key=api_key,
            license_server_url=license_server_url,
            timeout_ms=timeout_ms,
        )
        license_data = self._validator.validate()
        self.enforcement_mode = license_data.get("enforcement_mode", enforcement_mode)
        self.rate_limit = int(license_data.get("rate_limit", rate_limit))

    def check(self, prompt: str, model: str, user_id: str) -> CheckResult:
        return self._evaluate(prompt)

    def check_output(self, output: str, model: str, user_id: Optional[str] = None) -> CheckResult:
        return self._evaluate(output)

    def _evaluate(self, content: str) -> CheckResult:
        start = time.perf_counter()
        blocked = "<struxel:block>" in content
        latency_ms = (time.perf_counter() - start) * 1000

        result = CheckResult(
            blocked=blocked,
            reason="Content violates policy" if blocked else None,
            policy_id="policy.blocked_content" if blocked else None,
            enforcement_mode=self.enforcement_mode,
            latency_ms=latency_ms,
        )

        if not blocked:
            return result

        if self.enforcement_mode == "realtime":
            raise PolicyViolationError("Content blocked by Struxel Guard.", result=result)
        if self.enforcement_mode == "batch":
            logging.info("Struxel violation recorded: %s", result.reason)
        else:
            logging.warning("Struxel advisory warning: %s", result.reason)

        return result
