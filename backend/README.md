# Backend

This directory reserves the service boundary for provider integration.

Before implementation, define:
- Server-side provider configuration (never commit provider host credentials or secrets).
- HTTPS-only API endpoints.
- Client authentication and short-lived, scoped tokens.
- Input validation, rate limits, audit-safe logging and credential redaction.
- Explicit authorization checks for every account and playlist.

The mobile app currently contains UI only; it does not connect to a provider or backend yet.
