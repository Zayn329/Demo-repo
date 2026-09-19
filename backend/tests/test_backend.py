import pytest
from fastapi.testclient import TestClient
from app.main import app, MANDATORY_LEGAL_DISCLAIMER
from app.db.database import SessionLocal
from app.db.models import DBOtpRequest

def get_otp_code_from_db(request_id: str) -> str:
    db = SessionLocal()
    try:
        otp_rec = db.query(DBOtpRequest).filter(DBOtpRequest.request_id == request_id).first()
        return otp_rec.otp_code if otp_rec else "000000"
    finally:
        db.close()

client = TestClient(app)

def get_authenticated_headers(phone="+1234567890"):
    req_res = client.post("/api/v1/auth/request-otp", json={"phone_number": phone})
    req_data = req_res.json()
    otp_code = get_otp_code_from_db(req_data["request_id"])
    verify_res = client.post(
        "/api/v1/auth/verify-otp",
        json={"request_id": req_data["request_id"], "otp_code": otp_code}
    )
    token = verify_res.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}

def test_auth_otp_flow():
    req_res = client.post("/api/v1/auth/request-otp", json={"phone_number": "+1234567890"})
    assert req_res.status_code == 200
    req_data = req_res.json()
    assert "request_id" in req_data

    otp_code = get_otp_code_from_db(req_data["request_id"])
    verify_res = client.post(
        "/api/v1/auth/verify-otp",
        json={"request_id": req_data["request_id"], "otp_code": otp_code}
    )
    assert verify_res.status_code == 200
    verify_data = verify_res.json()
    assert "access_token" in verify_data
    assert "refresh_token" in verify_data

def test_unauthenticated_request_rejected():
    res = client.get("/api/v1/notify/circle")
    assert res.status_code == 401

def test_batch_sync_raw_audio_rejection():
    headers = get_authenticated_headers("+1999888777")
    sync_data = {
        "events": [
            {
                "event_id": "e1",
                "incident_id": "inc1",
                "event_type": "incident.sealed",
                "timestamp": 1700000000,
                "payload": {"merkle_root": "0x123"}
            },
            {
                "event_id": "e2",
                "incident_id": "inc1",
                "event_type": "evidence.raw",
                "timestamp": 1700000001,
                "payload": {"raw_audio": "audio_bytes_data"}
            }
        ]
    }
    res = client.post("/api/v1/sync/batch", json=sync_data, headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert "e1" in data["accepted_event_ids"]
    assert "e2" not in data["accepted_event_ids"]
    assert len(data["rejected_events"]) == 1
    assert data["rejected_events"][0]["event_id"] == "e2"

def test_legal_draft_generation_and_mandatory_disclaimer():
    headers = get_authenticated_headers("+1999888776")
    draft_request = {
        "incident_id": "inc_999",
        "draft_type": "FIR_COMPLAINT",
        "authorized_summary": {
            "title": "Evening Commute Distress",
            "date": "2026-08-30 22:00:00",
            "location": "Mumbai Suburbs",
            "trigger_sources": ["KEYWORD_HELP", "MOTION_IMPACT"],
            "merkle_root": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        },
        "user_authorized": True
    }
    res = client.post("/api/v1/legal/drafts", json=draft_request, headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert data["incident_id"] == "inc_999"
    assert data["disclaimer"] == MANDATORY_LEGAL_DISCLAIMER
    assert "FORMAL INCIDENT COMPLAINT DRAFT (FIR STYLE)" in data["content"]
    assert "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855" in data["content"]

def test_unauthorized_legal_request_rejection():
    headers = get_authenticated_headers("+1999888775")
    draft_request = {
        "incident_id": "inc_999",
        "draft_type": "FIR_COMPLAINT",
        "authorized_summary": {},
        "user_authorized": False
    }
    res = client.post("/api/v1/legal/drafts", json=draft_request, headers=headers)
    assert res.status_code == 400

def test_ai_summary_endpoint():
    headers = get_authenticated_headers("+1999888774")
    body = {
        "incident_id": "inc_sum_100",
        "authorized_summary": {
            "title": "Late Night Walk Alert",
            "date": "2026-08-30 23:30:00",
            "location": "Central Park West",
            "trigger_sources": ["KEYWORD_HELP", "SCREAM"],
            "merkle_root": "0xabc123merkle"
        },
        "user_authorized": True
    }
    res = client.post("/api/v1/ai/summaries", json=body, headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert data["incident_id"] == "inc_sum_100"
    assert "Central Park West" in data["executive_summary"]
    assert data["integrity_reference"] == "0xabc123merkle"
    assert "AI-GENERATED SUMMARY" in data["disclaimer"]

def test_ai_timeline_endpoint_and_db_event_tracing():
    headers = get_authenticated_headers("+1999888773")
    # First sync events to DB
    sync_data = {
        "events": [
            {
                "event_id": "evt_trace_1",
                "incident_id": "inc_trace_101",
                "event_type": "detection.keyword_detected",
                "timestamp": 1700000010,
                "payload": {"keyword": "EMERGENCY", "confidence": 0.98}
            },
            {
                "event_id": "evt_trace_2",
                "incident_id": "inc_trace_101",
                "event_type": "incident.sealed",
                "timestamp": 1700000020,
                "payload": {"merkle_root": "0xtrace999"}
            }
        ]
    }
    sync_res = client.post("/api/v1/sync/batch", json=sync_data, headers=headers)
    assert sync_res.status_code == 200

    # Call AI timeline endpoint with empty events list to trigger DB event trace lookup
    timeline_req = {
        "incident_id": "inc_trace_101",
        "events": [],
        "user_authorized": True
    }
    res = client.post("/api/v1/ai/timelines", json=timeline_req, headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert data["incident_id"] == "inc_trace_101"
    assert len(data["chronological_entries"]) == 2
    assert "EMERGENCY" in data["chronological_entries"][0]["explanation"]
    assert "0xtrace999" in data["chronological_entries"][1]["explanation"]

def test_ai_qa_endpoint():
    headers = get_authenticated_headers("+1999888772")
    body = {
        "incident_id": "inc_qa_102",
        "question": "What time did the incident occur?",
        "authorized_facts": {
            "date": "2026-08-30 22:15:00",
            "location": "South Station"
        },
        "user_authorized": True
    }
    res = client.post("/api/v1/ai/qa", json=body, headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert "2026-08-30 22:15:00" in data["answer"]
    assert len(data["grounded_facts_used"]) > 0

def test_ai_report_endpoint():
    headers = get_authenticated_headers("+1999888771")
    body = {
        "incident_id": "inc_rep_103",
        "report_type": "FORMAL_INCIDENT_REPORT",
        "authorized_summary": {
            "title": "Formal Audit Report",
            "date": "2026-08-30",
            "location": "Airport Terminal 2",
            "trigger_sources": ["PANIC_BUTTON"],
            "merkle_root": "0xrep888"
        },
        "user_authorized": True
    }
    res = client.post("/api/v1/ai/reports", json=body, headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert "SAHARA INCIDENT COMPREHENSIVE REPORT" in data["content"]
    assert "0xrep888" in data["content"]

def test_ai_provider_unavailability_503(monkeypatch):
    headers = get_authenticated_headers("+1999888770")
    monkeypatch.setenv("AI_PROVIDER_UNAVAILABLE", "true")

    body = {
        "incident_id": "inc_err_503",
        "authorized_summary": {"title": "Test Error"},
        "user_authorized": True
    }
    res = client.post("/api/v1/ai/summaries", json=body, headers=headers)
    assert res.status_code == 503
    data = res.json()
    assert data["detail"]["error"]["code"] == "AI_PROVIDER_UNAVAILABLE"

def test_prohibited_sensitive_data_rejection():
    headers = get_authenticated_headers("+1999888769")
    body = {
        "incident_id": "inc_sens_104",
        "authorized_summary": {
            "title": "Malicious Request",
            "raw_audio": "base64_raw_bytes_stream"
        },
        "user_authorized": True
    }
    res = client.post("/api/v1/ai/summaries", json=body, headers=headers)
    assert res.status_code == 400
    data = res.json()
    assert data["detail"]["error"]["code"] == "PROHIBITED_SENSITIVE_DATA"
