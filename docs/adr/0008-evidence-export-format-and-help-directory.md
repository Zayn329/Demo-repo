# 8. Evidence Export Package Format & Offline Help Directory

* Status: accepted
* Date: 2026-08-30

## Context
Sahara requires users to be able to review, verify, and export sealed incident evidence packages locally for sharing with authorities or legal counsel. Additionally, the application must bundle an offline help directory focusing on Mumbai and major Maharashtra cities (police, women's helplines, NGOs, legal aid) that remains accessible without network connectivity. All exported packages must include explicit disclaimers regarding legal admissibility.

## Decision
We implement export packaging and help directory features in `:android:app`:
- `OfflineHelpDirectory`: Bundles curated emergency contacts for Mumbai and Maharashtra (`100` Police, `103` Women Helpline, `112` ERSS, Majlis Legal Centre, Pune Women's Cell) accessible offline.
- `EvidenceExporter`: Produces JSON/summary export packages containing incident metadata, Merkle root, chunk hashes, and verification status. Every export attaches the mandatory disclaimer: `"LEGAL DISCLAIMER: Technical integrity protection (SHA-256 Merkle root and ECDSA signature) verifies that this exported safety evidence package has not been tampered with since sealing. This technical verification does not guarantee court admissibility or police acceptance. Consult legal counsel before formal submission."`.
- Jetpack Compose UI Screens (`MainDashboardScreen`, `HelpDirectoryScreen`, `ExportVerifierScreen`): Provide user-facing controls for monitoring state, panic activation, offline help directory lookup, and evidence package integrity verification.

## Consequences
- Exported packages clearly state technical integrity while avoiding unsupported claims of guaranteed court admissibility.
- Emergency help directory is 100% accessible offline.
- Adheres strictly to `architecture.yaml`, `PRD.md`, `bdd/features/export.feature`, and `bdd/features/failure_and_degraded_behavior.feature`.
