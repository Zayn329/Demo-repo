# 1. Modular Clean Architecture for Sahara Android

* Status: accepted
* Date: 2026-08-30

## Context
Sahara requires a deterministic, offline-first Android application adhering to high-security and clean architectural boundaries as mandated by `architecture.yaml`. Core capabilities (distress detection, panic trigger, encrypted evidence storage, mesh relay) must remain functional without internet connectivity or external AI backends.

## Decision
We structure the Android codebase (`android/`) using a modular Clean Architecture pattern:
- `:android:app` — Jetpack Compose presentation, MainActivity, and dependency composition.
- `:android:core:domain` — Pure Kotlin domain models (`Incident`, `EvidenceEntry`, `DetectionEvent`, `NotifyContact`, `AuditEvent`), state machine interfaces, and repository definitions with zero platform dependencies.
- `:android:core:data` — Room persistence (`SaharaDatabase`), SQLite DAOs, and repository implementations.
- `:android:core:security` — KeyStorageManager & EncryptedFileStorage security contracts backed by Android Keystore.
- `:android:core:testing` — Shared test utilities and fakes.

## Consequences
- Strict separation of concerns ensuring core safety logic remains untangled from UI framework details.
- Unit and integration testing can run deterministically without hardware dependencies.
- Clear module boundaries align directly with the protected modules defined in `architecture.yaml`.
