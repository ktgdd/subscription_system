# Changelog

## [Unreleased]

### Added
- Dev bypass flag (`app.jwt.dev-bypass-enabled`) to skip JWT validation for testing. When enabled, accepts `X-User-Id`, `X-Request-Id`, and `X-Role` headers instead of JWT tokens.
- Redis idempotency tests for concurrent request handling. Added service-level tests and API integration tests to verify idempotency keys prevent duplicate requests under concurrent load.

