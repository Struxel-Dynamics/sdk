# Struxel SDK

Struxel SDK provides in-process guardrails for AI pipelines so teams can enforce policy checks directly inside their own applications. The SDK is designed for a fast path under 1ms overhead while supporting license-backed enforcement controls.

## Install

```bash
pip install struxel-sdk==1.0.0
npm install @struxel/sdk@1.0.0
# java: io.struxel:sdk:1.0.0 (Maven)
go get github.com/struxel-dynamics/sdk@v1.0.0
```

## Quickstart

```python
guard = Guard(
    api_key="sdk_<uuid>",                    # required — validated against license server
    enforcement_mode="realtime",             # "realtime" | "batch" | "advisory"
    rate_limit=500,                          # inferences/sec (from tier: 100/500/1000)
    license_server_url="https://api.struxel.ai/v1/license/validate",  # default, overridable
    timeout_ms=800,                          # default 800ms — keeps total overhead <1ms in fast path
)
result = guard.check(prompt="...", model="gpt-4", user_id="user-123")
# result.blocked: bool
# result.reason: str | None
# result.policy_id: str | None
# result.latency_ms: float
result = guard.check_output(output="...", model="gpt-4")
```

## API Key

Use an API key in `sdk_<uuid>` format. Generate it from the Struxel CCC dashboard after tenant provisioning.

## Enforcement Modes

| Mode | Behavior |
| --- | --- |
| realtime | Raises/throws `PolicyViolationError` when blocked |
| batch | Never raises; logs violations and returns blocked result |
| advisory | Never raises; logs warnings and returns blocked result |

## Rate Limits

| Tier | Limit |
| --- | --- |
| Starter | 100/s |
| Professional | 500/s |
| Enterprise | 1000/s |

## Full Integration Guide

See https://docs.struxel.ai/sdk.

## Release Distribution

For embedded SDK packaging, publish a GitHub release in `struxel-dynamics/sdk` with the four versioned assets for the release tag (for example, `v1.0.0`):

- `struxel-sdk-python-1.0.0.tar.gz`
- `struxel-sdk-typescript-1.0.0.tgz`
- `struxel-sdk-java-1.0.0.jar`
- `struxel-sdk-go-1.0.0.tar.gz`

True air-gapped customers do not fetch from GitHub releases. Deliver their artifacts through the Self-Hosted/Air-Gap bundle flow instead.

## Security

Do not commit API keys into this repository. The SDK validates every license with Struxel's license server, and customer model data remains in the customer's environment.

## Package Metadata

| Language | Package name | Version | Registry |
| --- | --- | --- | --- |
| Python | struxel-sdk | 1.0.0 | PyPI |
| TypeScript | @struxel/sdk | 1.0.0 | npm |
| Java | io.struxel:sdk | 1.0.0 | Maven Central |
| Go | github.com/struxel-dynamics/sdk | v1.0.0 | Go modules (via GitHub tag) |
