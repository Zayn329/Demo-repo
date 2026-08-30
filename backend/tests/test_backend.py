import pytest
from fastapi.testclient import TestClient
from app.main import app, MANDATORY_LEGAL_DISCLAIMER

client = TestClient(app)

def test_auth_otp_flow():
    # 1. Request OTP
    req_res = client.post("/api/v1/auth/request-otp", json={"phone_number": "+1234567890"})
    assert req_res.status_code == 200
    req_data = req_res.json()
    assert "request_id" in req_data

    # 2. Verify OTP
    verify_res = client.post(
        "/api/v1/auth/verify-otp",
        json={"request_id": req_data["request_id"], "otp_code": "123456"}
    )
    assert verify_res.status_code == 200
    verify_data = verify_res.json()
    assert "access_token" in verify_data
    assert "refresh_token" in verify_data

def test_batch_sync_raw_audio_rejection():
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
    res = client.post("/api/v1/sync/batch", json=sync_data)
    assert res.status_code == 200
    data = res.json()
    assert "e1" in data["accepted_event_ids"]
    assert "e2" not in data["accepted_event_ids"]
    assert len(data["rejected_events"]) == 1
    assert data["rejected_events"][0]["event_id"] == "e2"

def test_legal_draft_generation_and_mandatory_disclaimer():
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
    res = client.post("/api/v1/legal/drafts", json=draft_request)
    assert res.status_code == 200
    data = res.json()
    assert data["incident_id"] == "inc_999"
    assert data["disclaimer"] == MANDATORY_LEGAL_DISCLAIMER
    assert "FORMAL INCIDENT COMPLAINT DRAFT (FIR STYLE)" in data["content"]
    assert "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855" in data["content"]

def test_unauthorized_legal_request_rejection():
    draft_request = {
        "incident_id": "inc_999",
        "draft_type": "FIR_COMPLAINT",
        "authorized_summary": {},
        "user_authorized": False
    }
    res = client.post("/api/v1/legal/drafts", json=draft_request)
    assert res.status_code == 400
