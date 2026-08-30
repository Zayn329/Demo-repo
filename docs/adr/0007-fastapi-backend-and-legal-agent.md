# 7. FastAPI Backend & AI Legal Agent Provider Abstraction

* Status: accepted
* Date: 2026-08-30

## Context
Sahara's primary safety and evidence capture functions are local-first and on-device. However, post-incident workflows (legal drafting, incident metadata sync, circle management, and integrity anchoring) benefit from optional online backend services. To maintain safety boundaries, the backend must never require or automatically receive raw audio evidence, and all AI-generated legal outputs must carry clear disclaimers.

## Decision
We implement the FastAPI backend service in `backend/`:
- Phone OTP authentication endpoints with provider abstraction (`/api/v1/auth/*`).
- Incident Sync batch endpoint (`/api/v1/sync/batch`) accepting structured metadata while explicitly rejecting raw audio payloads.
- Notify Circle API endpoints (`/api/v1/notify/circle`, `/api/v1/notifications/*`).
- Integrity Anchoring API endpoints (`/api/v1/anchors`).
- Legal Assistance Agent (`LegalAgent` & `/api/v1/legal/drafts`) converting user-approved structured incident metadata into FIR complaint drafts. Every draft output enforces the mandatory disclaimer: `"DRAFT FOR HUMAN AND LEGAL REVIEW. THIS DOCUMENT HAS NOT BEEN FILED WITH ANY AUTHORITY."`.

## Consequences
- Core offline Android safety companion operates independently without backend reliance.
- Raw evidence never transfers to the backend or LLMs.
- All legal outputs are explicitly disclaimed as unfiled drafts requiring human review.
- Adheres strictly to `architecture.yaml`, `docs/api/`, and `bdd/features/legal_assistance.feature`.
