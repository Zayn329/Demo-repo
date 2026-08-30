Sahara API Contracts

Purpose

This directory defines the public backend API contracts for Sahara, an offline-first safety companion.

The API layer supports backend-dependent capabilities without becoming a dependency of the core safety path. Distress detection, panic activation, evidence capture, encryption, signing, Merkle integrity generation, and local incident persistence must continue to function without backend connectivity.

The normative public API contract is:

docs/api/openapi.yaml

The remaining Markdown files explain the intended behavior, privacy constraints, and edge cases for individual API domains.

Authority

API contracts have the same authority level as the technical specifications.

The repository-wide precedence order is:

1. "architecture.yaml"
2. Technical specifications under "docs/specs/" and API contracts under "docs/api/"
3. BDD scenarios under "bdd/"
4. "PRD.md"
5. "AGENTS.md"

If a public API contract conflicts with a broader technical specification, the more specific API contract governs external interface behavior. Internal implementation must still comply with the applicable architecture and security specifications.

API versioning

All public endpoints use the "/api/v1" prefix.

Breaking changes must not silently modify existing endpoint behavior. Future breaking changes require a new API version or an explicitly approved migration strategy.

Design principles

The API is designed around several constraints:

- The Android application remains usable without backend authentication.
- Backend-dependent features require authentication.
- Raw evidence must not be automatically uploaded.
- The device is authoritative for evidence-related integrity data.
- Synchronization should tolerate offline queues and repeated delivery attempts.
- Durable create operations should support idempotency.
- Sensitive data must not appear in ordinary application logs.
- External service failures must produce explicit degraded behavior rather than silent substitution.

Contract domains

Authentication

Defined by "auth.md".

The backend abstracts the underlying phone OTP provider. Android clients communicate only with Sahara API endpoints and do not directly depend on Firebase, Twilio, or another OTP provider.

Authentication provides short-lived access tokens and refresh tokens.

Legal assistance

Defined by "legal.md".

Legal drafting receives only user-approved structured incident information. Raw audio and other raw evidence are not automatically sent to the backend or LLM provider.

Generated documents are drafts and require human or legal review.

Incident synchronization

Defined by "incident-sync.md".

The primary synchronization path is batch-oriented to support offline operation. Evidence metadata and integrity information may synchronize when permitted. Raw evidence blobs must never synchronize automatically.

Notify Circle

Defined by "notify.md".

The backend may store Circle membership, delivery records, acknowledgement history, and optionally fan out notifications through FCM. Device-side SMS and offline mesh remain independent paths.

Anchoring

Defined by "anchoring.md".

Anchoring is optional and asynchronous. The API must clearly distinguish real testnet results from mock results.

Authentication

Authenticated endpoints require:

Authorization: Bearer <access-token>

Access tokens and refresh tokens must not be logged.

The Android client should store session credentials only using platform-protected storage. Exact Android implementation details are governed by the Android security implementation and applicable specifications.

Correlation IDs

Clients may send:

X-Request-ID: <client-generated-id>

The server must associate the request with a correlation identifier and return:

X-Request-ID: <request-id>

The identifier is intended for troubleshooting and must not contain phone numbers, location coordinates, authentication tokens, or other sensitive user data.

Idempotency

Endpoints that create durable records support:

Idempotency-Key: <unique-key>

The key represents one logical client operation.

Repeated requests using the same authenticated principal, endpoint, and idempotency key must not create duplicate durable records. Implementations may return the original successful response for a duplicate request.

Idempotency does not permit clients to reuse a key for different request payloads.

Standard errors

All API errors use the common envelope:

{
  "error": {
    "code": "MACHINE_READABLE_CODE",
    "message": "Human readable message",
    "retryable": true
  }
}

The OpenAPI contract defines the relevant error codes and response status codes.

Clients must not infer success from transport availability alone. Explicit response status and body semantics govern operation results.

Rate limiting

OTP, legal drafting, synchronization, and anchoring operations may be rate limited.

Exact limits are configuration-driven and may differ between development, demo, and deployment environments. A rate-limited response should use HTTP "429" and include a standard error envelope.

Where available, the server should provide:

Retry-After

Clients must treat rate limiting as a temporary service condition unless the response explicitly states otherwise.

Privacy and observability

Backend implementations must use privacy-safe observability.

The following must not be written to ordinary application logs:

- Raw audio or other raw evidence.
- Exact latitude and longitude.
- Phone numbers.
- Encryption keys.
- Private keys.
- Access tokens.
- Refresh tokens.
- OTP values.
- Unredacted authorization headers.

Audit and operational logs must use redacted identifiers where possible.

The backend may record operational metadata such as request IDs, endpoint names, response classes, durations, and non-sensitive error codes.

External service degradation

Failure of an external provider must not silently change the meaning of an operation.

Examples include:

- An unavailable LLM provider returns an explicit legal-provider failure.
- An unavailable anchoring network returns an explicit pending or failed state.
- A development mock must visibly identify itself as a mock.
- A backend outage does not invalidate local evidence capture.

The core safety path must never depend on these APIs to preserve evidence locally.

Contract implementation rules

Coding agents implementing these contracts may design internal database schemas, service layers, provider adapters, and repository patterns where those decisions do not alter the public contract.

Changes to endpoint paths, schemas, authentication requirements, or public error semantics require explicit approval.