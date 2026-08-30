# Sahara Implementation Roadmap

This document outlines the coherent milestones for building Sahara according to `architecture.yaml`, `PRD.md`, `docs/api/`, `bdd/features/`, and `AGENTS.md`.

---

## Hackathon Core Demo Path Overview
The critical path prioritized across the milestones is:
**Monitoring → Keyword Detection → Possible Distress → Confirmation → Evidence Capture → AES-256-GCM Encryption → Keystore Signing → Merkle Sealing → Local Persistence → Delivery / Notify Circle (Mesh & Direct SMS Fallback).**

---

## Milestone 1: Android Project Foundation & Core Infrastructure Architecture
- **Goal:** Establish the native Kotlin Android project structure (`android/`), Clean Architecture directory layers (`core`, `features`, `services`), dependencies, database entities/Room setup, and permission declaration.
- **Relevant Architecture Components:** `repository.directories.android`, `technology.android`, `privacy`.
- **Relevant Specs:** `docs/api/README.md`.
- **Relevant BDD Scenarios:** `bdd/features/permissions.feature` (permission explanation and graceful handling).
- **Protected Modules Touched:** None directly in Milestone 1 (prepares foundations for all).
- **Dependencies Required:** Kotlin Coroutines/Flow, Jetpack Compose, Room (SQLite), Hilt/Koin (DI if needed), AndroidX Core/Lifecycle/WorkManager.
- **Real-Device Requirements:** Physical Android device or Emulator with API level 26+ (Android 8.0+).
- **Main Risks:** Complex Gradle project configuration and multi-module setup overhead.
- **Acceptance Criteria:**
  - Android application compiles successfully with Jetpack Compose support.
  - Room database and basic schemas compile.
  - Clean architecture boundaries (`core/domain`, `core/data`, `core/security`, `services/`, `features/`) established.

---

## Milestone 2: On-Device Deterministic Distress Detection Engine
- **Goal:** Build the on-device detection engine consisting of TensorFlow Lite keyword spotting, hybrid scream detection signal handling, accelerometer motion analysis, and configurable signal fusion.
- **Relevant Architecture Components:** `components.safety_agent`, `detection`, `technology.android.machine_learning`, `technology.android.sensors`.
- **Relevant Specs:** `architecture.yaml` (section `detection`), `docs/specs/detection.yaml` (when created).
- **Relevant BDD Scenarios:** `bdd/features/monitoring.feature`.
- **Protected Modules Touched:** `distress detection` [PROTECTED].
- **Dependencies Required:** TensorFlow Lite runtime (`org.tensorflow:tensorflow-lite`), Android AudioRecord API, SensorManager.
- **Real-Device Requirements:** Real Android device recommended for microphone input and accelerometer hardware sensing.
- **Main Risks:** Audio record foreground permission restrictions on Android 14+; false positives in KWS model.
- **Acceptance Criteria:**
  - Real AudioRecord stream feeds TFLite keyword detection model.
  - Detection events fire deterministically on-device without network calls.
  - Multi-signal fusion rule `keyword OR (scream AND motion)` is evaluated in real-time.

---

## Milestone 3: Incident State Machine, Foreground Service & Panic Controller
- **Goal:** Implement the persistent Android Foreground Service for safety monitoring, the deterministic Incident State Machine (`idle` -> `monitoring` -> `suspicious_signal` -> `candidate_incident` -> `pending_confirmation` -> `active_incident` -> `sealed` -> `archived`), and explicit Panic activation (in-app button & cancellation window).
- **Relevant Architecture Components:** `incident_state_machine`, `components.panic_controller`, `technology.android.background_execution`.
- **Relevant Specs:** `architecture.yaml` (section `incident_state_machine`).
- **Relevant BDD Scenarios:** `bdd/features/incident.feature`, `bdd/features/panic.feature`.
- **Protected Modules Touched:** `incident state machine` [PROTECTED], `panic activation` [PROTECTED].
- **Dependencies Required:** AndroidX Lifecycle, Foreground Service permissions (`FOREGROUND_SERVICE_MICROPHONE`).
- **Real-Device Requirements:** Physical device required to test persistent foreground notification lifecycle and physical volume/gesture trigger.
- **Main Risks:** Android OS background execution limits and OEM battery killer kills of foreground service.
- **Acceptance Criteria:**
  - Android Foreground Service runs continuously with user-visible notification.
  - In-app Panic button triggers cancellation countdown window (3-5s).
  - State machine transitions correctly and logs audit events to SQLite.

---

## Milestone 4: Encrypted Evidence Engine & Cryptographic Integrity Sealing
- **Goal:** Capture pre-roll and active incident evidence (audio chunks, location, accelerometer streams), perform per-incident AES-256-GCM key generation wrapped by Android Keystore, compute SHA-256 chunk hashes, build Merkle tree, and produce signed evidence manifests.
- **Relevant Architecture Components:** `components.evidence_engine`, `components.crypto_engine`, `security`, `evidence`.
- **Relevant Specs:** `architecture.yaml` (sections `evidence`, `security`), `docs/specs/crypto.yaml`, `docs/specs/evidence.yaml`.
- **Relevant BDD Scenarios:** `bdd/features/evidence.feature`, `bdd/features/export.feature`.
- **Protected Modules Touched:** `evidence encryption` [PROTECTED], `Android Keystore integration` [PROTECTED], `evidence signing and hashing` [PROTECTED], `Merkle tree logic` [PROTECTED].
- **Dependencies Required:** `javax.crypto`, Android Keystore API, SHA-256 / AES-GCM primitives.
- **Real-Device Requirements:** Real Android device with hardware TEE/StrongBox for hardware-backed Keystore signing.
- **Main Risks:** Nonce reuse risks, Keystore provider differences across Android vendors.
- **Acceptance Criteria:**
  - Audio pre-roll buffer is maintained in-memory and committed on incident candidate state.
  - Evidence chunks are encrypted with per-incident AES-256-GCM data keys.
  - Merkle root is finalized and signed with Android Keystore key.
  - Unencrypted raw evidence never touches disk or network.

---

## Milestone 5: Offline Mesh Relay & Direct SMS Emergency Escalation Fallback
- **Goal:** Implement offline peer-to-peer distress packet relay using Google Nearby Connections (BLE / Wi-Fi Direct) with hop limits & deduplication, and fallback to direct device SMS via `SmsManager`.
- **Relevant Architecture Components:** `components.mesh_relay`, `components.fallback_manager`, `networking.mesh`, `networking.fallback`.
- **Relevant Specs:** `docs/specs/mesh.yaml`, `bdd/features/mesh.feature`, `bdd/features/sms.feature`.
- **Relevant BDD Scenarios:** `bdd/features/mesh.feature`, `bdd/features/sms.feature`.
- **Protected Modules Touched:** `SMS escalation` [PROTECTED], `mesh relay` [PROTECTED].
- **Dependencies Required:** Google Play Services Nearby (`com.google.android.gms:play-services-nearby`), Android `SmsManager`.
- **Real-Device Requirements:** Two physical Android devices in close proximity to test Nearby Connections BLE/Wi-Fi Direct peer discovery and packet forwarding; SIM card/cellular network for SMS.
- **Main Risks:** Nearby Connections permission complexity (location + bluetooth scan/connect) and radio discovery latency.
- **Acceptance Criteria:**
  - Device discovers nearby opted-in peer and relays compact distress packet.
  - Hop count limits (max 12) and duplicate packet cache prevent relay loops.
  - SMS fallback fires if primary transport fails/times out, sending concise alert with location and incident reference.

---

## Milestone 6: Notify Circle Management & Multi-Channel Delivery
- **Goal:** Provide trusted contacts management (up to 5 contacts), permission controls (location sharing authorization), alert dispatching, and delivery receipt tracking.
- **Relevant Architecture Components:** `components.notify_circle`, `policy_engine`, `data_models.NotifyContact`.
- **Relevant Specs:** `docs/api/notify.md`, `bdd/features/notify.feature`.
- **Relevant BDD Scenarios:** `bdd/features/notify.feature`, `bdd/features/location.feature`.
- **Protected Modules Touched:** Policy Engine & Contact Sharing permissions.
- **Dependencies Required:** Room persistence for contacts, Android Contacts Picker (optional integration).
- **Real-Device Requirements:** Physical device for SMS sending or app notification testing.
- **Main Risks:** Permission revocation during active incident.
- **Acceptance Criteria:**
  - User can add/edit trusted contacts and set explicit location sharing permissions.
  - Notification dispatch attempts eligible paths independently.
  - Delivery state (Delivered/Pending/Failed) is clearly displayed in UI.

---

## Milestone 7: Optional FastAPI Backend & AI Legal Drafting Agent
- **Goal:** Create the minimal FastAPI Python backend service (`backend/`) providing phone OTP auth abstraction, incident metadata sync batch API, optional Merkle root anchoring API, and LLM-assisted FIR/incident summary draft generation.
- **Relevant Architecture Components:** `backend`, `components.backend_api`, `components.legal_agent`, `docs/api/`.
- **Relevant Specs:** `docs/api/auth.md`, `docs/api/legal.md`, `docs/api/incident-sync.md`, `docs/api/anchoring.md`, `docs/api/openapi.yaml`.
- **Relevant BDD Scenarios:** `bdd/features/legal_assistance.feature`.
- **Protected Modules Touched:** None (Backend is non-safety-critical optional layer).
- **Dependencies Required:** Python 3.11+, FastAPI, Pydantic v2, Uvicorn, pytest, `uv`.
- **Real-Device Requirements:** Server environment / local dev server accessible from Android client via network.
- **Main Risks:** LLM provider API failures or rate limits.
- **Acceptance Criteria:**
  - FastAPI service implements `/api/v1/` OpenAPI contract.
  - Structured incident metadata can be converted to an FIR complaint draft.
  - Every legal draft contains the required mandatory disclaimer: `"DRAFT FOR HUMAN AND LEGAL REVIEW. THIS DOCUMENT HAS NOT BEEN FILED WITH ANY AUTHORITY."`
  - Core offline Android app functions completely when backend is unreachable.

---

## Milestone 8: End-to-End Golden Demo Integration & Verification
- **Goal:** Wire the full golden demo path together with Jetpack Compose UI (Monitoring ON/OFF, Panic button, Incident Active view, Offline Help Directory, Evidence Export & Verification viewer).
- **Relevant Architecture Components:** All system components, `components.help_directory`, `verifier`.
- **Relevant Specs:** `PRD.md` (Golden Demo Scenario), `bdd/features/export.feature`, `bdd/features/failure_and_degraded_behavior.feature`.
- **Relevant BDD Scenarios:** All core scenarios (`@core`, `@demo`).
- **Protected Modules Touched:** Verification of all protected modules.
- **Dependencies Required:** Android UI libraries, baseline verification scripts.
- **Real-Device Requirements:** 2 Physical Android devices for golden demo flow.
- **Main Risks:** Edge case UI state mismatch or unhandled background permission state.
- **Acceptance Criteria:**
  - Complete golden demo path runs smoothly offline: Keyword -> Distress -> Confirm -> Capture -> Encrypt -> Sign -> Seal -> Relay -> SMS.
  - Evidence package export verification tool verifies valid package integrity and rejects tampered packages.
