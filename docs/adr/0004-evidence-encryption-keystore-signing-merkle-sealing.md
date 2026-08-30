# 4. Evidence Encryption, Keystore Signing & Merkle Integrity Sealing

* Status: accepted
* Date: 2026-08-30

## Context
Sahara requires evidence integrity protection at rest without relying on cloud backends. Captured audio chunks, location logs, and sensor streams must be encrypted locally using per-incident AES-256-GCM data keys wrapped by Android Keystore, hashed using SHA-256, organized into a Merkle tree, and sealed with a signed evidence manifest. Tampered evidence packages must fail verification.

## Decision
We implement evidence protection across `:android:core:security` and `:android:services:evidence`:
- `KeyStorageManagerImpl`: Generates per-incident AES-256 data keys, handles key wrapping/unwrapping via Android Keystore master keys, and signs/verifies manifests.
- `AesGcmFileStorage`: Performs streaming authenticated AES-256-GCM encryption with unique random IVs/nonces.
- `MerkleTree`: Canonicalizes chunk SHA-256 hashes and computes binary Merkle tree roots.
- `BoundedAudioPreRollBuffer`: Manages in-memory PCM audio pre-roll buffer prior to incident candidate confirmation.
- `EvidenceCaptureEngine`: Encrypts audio chunks, writes to local disk storage, computes hashes, and saves evidence metadata to Room DB.
- `EvidenceManifestManager`: Finalizes Merkle roots and produces signed `EvidenceManifest` objects.
- `EvidenceVerifier`: Re-computes chunk hashes, Merkle root, and manifest signature to verify package integrity and detect tampered evidence.

## Consequences
- Raw evidence never exists in plaintext on disk or network.
- Merkle integrity sealing allows tamper-evident verification.
- Adheres strictly to `architecture.yaml`, `bdd/features/evidence.feature`, and `bdd/features/export.feature`.
