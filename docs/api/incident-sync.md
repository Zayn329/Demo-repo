Incident Synchronization API Contract

Purpose

Sahara is offline-first.

Synchronization exists to upload approved metadata and operational events when connectivity becomes available. It must not become a dependency of incident creation, evidence protection, or local safety behavior.

The primary synchronization endpoint is:

POST /api/v1/sync/batch

Batch-first design

Offline devices may accumulate multiple pending events before connectivity returns.

A batch endpoint allows the application to:

- Preserve local ordering.
- Retry interrupted synchronization.
- Avoid one network request per event.
- Process partial success explicitly.

Each event has its own identifier and incident identifier.

The server returns accepted event identifiers and explicit rejection information.

Automatically synchronizable information

Subject to user permissions and product policy, synchronization may include:

- Incident metadata.
- Evidence hashes.
- Merkle roots.
- Delivery events.
- Acknowledgement events.
- Approximate location that has already been shared.

Information that must never automatically synchronize

The following must never be automatically uploaded through synchronization:

- Raw audio evidence.
- Raw video evidence.
- Encrypted evidence blobs.
- Evidence encryption keys.
- Android Keystore material.
- Private signing keys.

Evidence remains on the device unless the user explicitly initiates an export or another separately approved transfer mechanism.

Source of truth

For evidence-related integrity information, the device is authoritative.

The backend must not silently overwrite:

- Merkle roots.
- Evidence hashes.
- Manifest integrity data.
- Device-generated integrity signatures.

Conflicting integrity information must be rejected or explicitly flagged as an "INTEGRITY_CONFLICT".

The backend may remain authoritative for backend-originated records such as notification delivery results and acknowledgement records.

Append-only events

Synchronization should use an append-oriented model wherever practical.

The backend should preserve the history of meaningful incident events rather than rewriting a single mutable incident record.

This is particularly important for:

- Incident cancellation.
- Evidence sealing.
- Delivery attempts.
- Circle acknowledgements.

Idempotency

The batch endpoint supports "Idempotency-Key".

Repeated delivery of the same logical batch must not create duplicate durable events.

Individual event identifiers also provide event-level deduplication.

Partial success

A successful HTTP response does not necessarily mean every event was accepted.

The client must inspect:

- "accepted_event_ids"
- "rejected_events"

Rejected events must remain distinguishable from unsent events in the local synchronization queue.

The application may retry only retryable failures according to the returned error code and local retry policy.

Conflict handling

Integrity conflicts are not automatically resolved.

The server must not select one conflicting Merkle root and discard another.

A conflict requires explicit preservation and reporting so that evidence integrity is not silently altered.

Privacy

Synchronization requests must not be logged in a form that exposes exact locations, raw evidence, phone numbers, keys, or tokens.

Request IDs and privacy-safe incident identifiers may be used for troubleshooting.