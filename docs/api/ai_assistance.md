# AI Assistance API Contract

## Purpose

The AI assistance API provides post-incident AI capabilities on user-approved structured incident data:
1. **FIR/Complaint Drafting** (`POST /api/v1/legal/drafts` - documented in `legal.md`)
2. **Incident Summarization** (`POST /api/v1/ai/summaries`)
3. **Timeline Reconstruction/Explanation** (`POST /api/v1/ai/timelines`)
4. **Evidence-Aware Q&A** (`POST /api/v1/ai/qa`)
5. **Incident Report Generation** (`POST /api/v1/ai/reports`)

All output is advisory/draft material requiring human review. AI capabilities do not alter, overwrite, or re-sign original evidence, Merkle roots, or cryptographic manifests.

---

## Data & Trust Boundary

### Permitted Request Inputs
- Incident metadata (incident_id, title, created_at, timestamps).
- Verified telemetry event list (event_id, event_type, timestamp, payload metadata).
- User-provided narrative/claims (explicitly flagged as user statements).
- Evidence references & metadata (type, sha256, chunk_index).
- Merkle root and signature reference metadata.
- Redacted approximate location (only when approved by user).

### Prohibited Inputs
- Raw audio / video / sensor streams.
- Device encryption keys or private Keystore keys.
- User authentication tokens or internal application secrets.

---

## Endpoints

### 1. FIR / Complaint Drafting
**Endpoint:** `POST /api/v1/legal/drafts`
See `docs/api/legal.md`.

---

### 2. Incident Summarization
**Endpoint:** `POST /api/v1/ai/summaries`
**Auth:** Required (`Bearer <token>`)

#### Request Body
```json
{
  "incident_id": "inc_12345",
  "authorized_summary": {
    "title": "Late Evening Distress",
    "date": "2026-08-30 22:15:00",
    "location": "Central Station",
    "trigger_sources": ["KEYWORD_HELP", "SCREAM"],
    "merkle_root": "a1b2c3d4..."
  },
  "user_authorized": true
}
```

#### Response Body (`200 OK`)
```json
{
  "summary_id": "sum_67890",
  "incident_id": "inc_12345",
  "title": "Late Evening Distress",
  "executive_summary": "On-device detection triggered by KEYWORD_HELP and SCREAM...",
  "key_events": ["22:15:00 - Keyword Help detected", "22:15:02 - Scream confirmed"],
  "integrity_reference": "a1b2c3d4...",
  "disclaimer": "AI-GENERATED SUMMARY FOR INFORMATIONAL PURPOSES. REQUIRES HUMAN REVIEW.",
  "created_at": 1700000000
}
```

---

### 3. Timeline Reconstruction & Explanation
**Endpoint:** `POST /api/v1/ai/timelines`
**Auth:** Required (`Bearer <token>`)

#### Request Body
```json
{
  "incident_id": "inc_12345",
  "events": [
    {
      "event_id": "evt_1",
      "event_type": "detection.keyword_detected",
      "timestamp": 1700000000,
      "payload": {"keyword": "HELP", "confidence": 0.92}
    },
    {
      "event_id": "evt_2",
      "event_type": "incident.activated",
      "timestamp": 1700000003,
      "payload": {"trigger": "PANIC_BUTTON"}
    }
  ],
  "user_authorized": true
}
```

#### Response Body (`200 OK`)
```json
{
  "timeline_id": "time_112233",
  "incident_id": "inc_12345",
  "chronological_entries": [
    {
      "timestamp": 1700000000,
      "event_type": "detection.keyword_detected",
      "explanation": "At 1700000000, distress keyword 'HELP' was detected with high confidence (92%)."
    },
    {
      "timestamp": 1700000003,
      "event_type": "incident.activated",
      "explanation": "3 seconds later, emergency panic button was explicitly activated."
    }
  ],
  "disclaimer": "AI-GENERATED TIMELINE EXPLANATION BASED ON VERIFIED EVENT TELEMETRY. REQUIRES HUMAN REVIEW.",
  "created_at": 1700000005
}
```

---

### 4. Evidence-Aware Incident Q&A
**Endpoint:** `POST /api/v1/ai/qa`
**Auth:** Required (`Bearer <token>`)

#### Request Body
```json
{
  "incident_id": "inc_12345",
  "question": "What time was the panic button pressed?",
  "authorized_facts": {
    "title": "Distress Incident",
    "events": [
      {"event_type": "incident.activated", "timestamp": 1700000003, "trigger": "PANIC_BUTTON"}
    ]
  },
  "user_authorized": true
}
```

#### Response Body (`200 OK`)
```json
{
  "qa_id": "qa_998877",
  "incident_id": "inc_12345",
  "question": "What time was the panic button pressed?",
  "answer": "Based on verified incident telemetry, the panic button was activated at timestamp 1700000003.",
  "grounded_facts_used": ["Event incident.activated at 1700000003"],
  "missing_information": [],
  "disclaimer": "AI ASSISTANT ANSWER GROUNDED ONLY IN AUTHORIZED VERIFIED FACTS. MAY NOT BE USED AS SOLE LEGAL PROOF.",
  "created_at": 1700000010
}
```

---

### 5. Incident Report Generation
**Endpoint:** `POST /api/v1/ai/reports`
**Auth:** Required (`Bearer <token>`)

#### Request Body
```json
{
  "incident_id": "inc_12345",
  "report_type": "FORMAL_INCIDENT_REPORT",
  "authorized_summary": {
    "title": "Incident Report Request",
    "date": "2026-08-30 22:15:00",
    "location": "Downtown Metro",
    "merkle_root": "a1b2c3d4..."
  },
  "user_authorized": true
}
```

#### Response Body (`200 OK`)
```json
{
  "report_id": "rep_554433",
  "incident_id": "inc_12345",
  "report_type": "FORMAL_INCIDENT_REPORT",
  "content": "FORMAL INCIDENT REPORT\n...",
  "disclaimer": "AI-GENERATED INCIDENT REPORT. DRAFT FOR HUMAN AND LEGAL REVIEW.",
  "created_at": 1700000020
}
```

---

## Grounding & Unavailability Behavior

- **Grounded Q&A**: If the question asks about facts not present in `authorized_facts`, the AI MUST state in `answer` and `missing_information` that the information is unknown/missing from verified evidence.
- **Provider Failure**: If the AI provider is unavailable, endpoints return HTTP `503` with code `AI_PROVIDER_UNAVAILABLE`. Silent fallbacks presenting fake AI responses are prohibited.
- **Unauthorized Request**: If `user_authorized` is `false`, endpoints return HTTP `400` with code `USER_AUTHORIZATION_REQUIRED`.
