# 6. Notify Circle Management & Multi-Channel Alert Delivery

* Status: accepted
* Date: 2026-08-30

## Context
Sahara allows primary users to configure a small set of trusted contacts (Notify Circle, max 5) who receive emergency status alerts and location updates during a confirmed incident. Delivery must operate independently for each contact across supported transports (SMS, Mesh, or optional Backend) without blocking local incident preservation. User sharing preferences and contact location permissions must be strictly respected.

## Decision
We implement Notify Circle management in `:android:features:notify-circle`:
- `NotifyCircleManager`: Manages contact configuration, enforces the maximum contact limit (5), filters location sharing according to individual contact permissions, and dispatches alerts across available transports via `EscalationFallbackManager`.
- `ContactDeliveryRecord`: Tracks delivery state per contact (`PENDING`, `DELIVERED`, `FAILED`, `ACKNOWLEDGED`).
- Transport acceptance records delivery success for v1; recipient acknowledgement (`"I saw this"`) is recorded separately for transparency without altering the incident state.

## Consequences
- Delivery failure for one contact does not affect other contacts or invalidate local evidence.
- Location sharing respects user-configured contact permissions.
- Adheres strictly to `architecture.yaml`, `docs/api/notify.md`, `bdd/features/notify.feature`, and `bdd/features/location.feature`.
