# 5. Offline Mesh Relay & Direct SMS Emergency Fallback

* Status: accepted
* Date: 2026-08-30

## Context
Sahara requires offline emergency delivery when internet or cellular network connectivity is unavailable. Local peer-to-peer mesh transport using Google Nearby Connections (BLE / Wi-Fi Direct) must relay compact emergency packets to opted-in nearby devices without causing infinite loops or user interruptions. When mesh transport is unavailable or times out, direct device SMS serves as the emergency fallback.

## Decision
We implement mesh relay and escalation fallback in `:android:services:mesh`:
- `MeshPacket`: Structured emergency packet schema containing packet ID, incident ID, packet type, hop count (max 12), payload hash, and payload content.
- `MeshDeduplicationCache`: Concurrent deduplication cache preventing duplicate packet loops across peer devices.
- `NearbyConnectionsMeshRelay`: Discovers opted-in peers, validates hop counts, and relays emergency packets.
- `EmergencyAlertPayload`: Prepares concise emergency SMS text containing incident ID, timestamp, approximate location (with age info), and evidence integrity hash.
- `EscalationFallbackManager`: Drives escalation order (`Mesh` -> `Local Storage` -> `Direct SMS` -> `Backend Sync`).
- `DemoMockSmsProvider`: Clearly labeled simulated SMS provider for development and demo environments returning explicit `"PASSED USING FALLBACK"` status.

## Consequences
- Peer-to-peer mesh transport operates completely offline between opted-in devices.
- Direct SMS fallback fires if primary transport fails or times out.
- Adheres strictly to `architecture.yaml`, `bdd/features/mesh.feature`, and `bdd/features/sms.feature`.
