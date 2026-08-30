Integrity Anchoring API Contract

Purpose

Integrity anchoring is an optional enhancement.

It may create an external timestamped reference for an evidence Merkle root, but it is not required for local evidence integrity and must not block evidence sealing or export.

Raw evidence is never required for anchoring.

Endpoints

Create an asynchronous anchoring request:

POST /api/v1/anchors

Retrieve the result:

GET /api/v1/anchors/{anchor_id}

Both endpoints require authentication.

Request boundary

The minimum anchor request contains:

- Incident identifier.
- Merkle root.
- Device-generated signature.
- Timestamp.

A device integrity signal may optionally be included when supported.

The backend must not require:

- Raw evidence.
- Evidence encryption keys.
- Raw audio.
- Raw video.

Asynchronous behavior

Anchor submission returns an accepted response with:

anchor_id
status: pending

The client later retrieves status using the anchor identifier.

Possible states are:

pending
anchored
failed

Evidence sealing must continue regardless of anchoring outcome.

Modes

The API explicitly identifies the anchoring mode:

testnet
mock

A mock result must never be represented as an actual blockchain transaction.

The client UI must visibly distinguish mock results from testnet results.

Idempotency

Anchor creation supports "Idempotency-Key".

Repeated submission of the same logical anchor request must not create duplicate durable anchor records or multiple unintended transactions.

Failure behavior

Anchoring failure must not:

- Delete local evidence.
- Invalidate local signatures.
- Modify the Merkle root.
- Prevent ordinary evidence export.

The failure must be reported separately from evidence integrity.

A locally verified incident remains integrity-protected even if external anchoring is unavailable.

Ownership and validation

The backend should validate that the authenticated caller is authorized to submit an anchor for the referenced incident.

Conflicting Merkle roots for the same incident must not be silently overwritten.

The backend must return an explicit integrity conflict when conflicting evidence integrity data is encountered.

Privacy

The anchoring service must not expose raw evidence through public blockchain metadata.

Only the minimum integrity representation necessary for the selected anchoring design should be submitted.

Ordinary logs must not contain raw evidence, private keys, authentication tokens, or sensitive location information.