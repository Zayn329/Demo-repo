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

def test_auth_otp_flow():
    # 1. Request OTP
    req_res = client.post("/api/v1/auth/request-otp", json={"phone_number": "+1234567890"})
    assert req_res.status_code == 200
    req_data = req_res.json()
    assert "request_id" in req_data

    # 2. Verify OTP
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
    # Helper auth
    req_res = client.post("/api/v1/auth/request-otp", json={"phone_number": "+1999888777"})
    req_data = req_res.json()
    otp_code = get_otp_code_from_db(req_data["request_id"])
    verify_res = client.post("/api/v1/auth/verify-otp", json={"request_id": req_data["request_id"], "otp_code": otp_code})
    token = verify_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

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
    # Helper auth
    req_res = client.post("/api/v1/auth/request-otp", json={"phone_number": "+1999888776"})
    req_data = req_res.json()
    otp_code = get_otp_code_from_db(req_data["request_id"])
    verify_res = client.post("/api/v1/auth/verify-otp", json={"request_id": req_data["request_id"], "otp_code": otp_code})
    token = verify_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

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
    # Helper auth
    req_res = client.post("/api/v1/auth/request-otp", json={"phone_number": "+1999888775"})
    req_data = req_res.json()
    otp_code = get_otp_code_from_db(req_data["request_id"])
    verify_res = client.post("/api/v1/auth/verify-otp", json={"request_id": req_data["request_id"], "otp_code": otp_code})
    token = verify_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    draft_request = {
        "incident_id": "inc_999",
        "draft_type": "FIR_COMPLAINT",
        "authorized_summary": {},
        "user_authorized": False
    }
    res = client.post("/api/v1/legal/drafts", json=draft_request, headers=headers)
    assert res.status_code == 400
