import json
from datetime import date
from time import monotonic
from typing import Any, Callable, Dict, Optional
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from .exceptions import StruxelSDKError


class LicenseValidator:
    DEFAULT_URL = "https://api.struxel.ai/v1/license/validate"
    CACHE_TTL_SECONDS = 300

    def __init__(
        self,
        api_key: str,
        license_server_url: str = DEFAULT_URL,
        timeout_ms: int = 800,
        sdk_version: str = "1.0.0",
        language: str = "python",
        http_client: Optional[Callable[[str, bytes, int], Dict[str, Any]]] = None,
    ) -> None:
        self.api_key = api_key
        self.license_server_url = license_server_url
        self.timeout_ms = timeout_ms
        self.sdk_version = sdk_version
        self.language = language
        self.http_client = http_client
        self._cache: Optional[Dict[str, Any]] = None
        self._cache_expires_at = 0.0

    def validate(self) -> Dict[str, Any]:
        now = monotonic()
        if self._cache and now < self._cache_expires_at:
            return self._cache

        if not self.api_key.startswith("sdk_"):
            raise StruxelSDKError("Invalid API key format. Expected sdk_<uuid>.")

        payload = {
            "api_key": self.api_key,
            "sdk_version": self.sdk_version,
            "language": self.language,
        }

        if self.http_client:
            response = self.http_client(
                self.license_server_url,
                json.dumps(payload).encode("utf-8"),
                self.timeout_ms,
            )
        else:
            response = self._post(payload)

        self._validate_response(response)
        self._cache = response
        self._cache_expires_at = now + self.CACHE_TTL_SECONDS
        return response

    def _post(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        request = Request(
            self.license_server_url,
            data=json.dumps(payload).encode("utf-8"),
            method="POST",
            headers={"Content-Type": "application/json"},
        )
        try:
            with urlopen(request, timeout=self.timeout_ms / 1000) as response:
                body = response.read().decode("utf-8")
                return json.loads(body)
        except HTTPError as err:
            raise StruxelSDKError(f"License validation failed with HTTP {err.code}.") from err
        except URLError as err:
            raise StruxelSDKError(f"License validation request failed: {err.reason}.") from err
        except (TimeoutError, json.JSONDecodeError) as err:
            raise StruxelSDKError("Invalid response from license server.") from err

    def _validate_response(self, response: Dict[str, Any]) -> None:
        if not response.get("valid"):
            raise StruxelSDKError("License validation failed: license is invalid.")

        expires_at = response.get("expires_at")
        if expires_at and date.fromisoformat(expires_at) < date.today():
            raise StruxelSDKError("License validation failed: license is expired.")
