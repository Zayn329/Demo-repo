import time
import uuid
from fastapi import FastAPI, HTTPException, Header, Response, status
from typing import Optional
from app.models.schemas import (
    RequestOtpRequest, RequestOtpResponse, VerifyOtpRequest, VerifyOtpResponse,
    RefreshTokenRequest, CircleUpdateRequest, CircleMember, IncidentAlertRequest,
    IncidentAlertResponse, AcknowledgementRequest, BatchSyncRequest, BatchSyncResponse,
    AnchorRequest, AnchorResponse, LegalDraftRequest, LegalDraftResponse, ErrorEnvelope, ErrorDetail
)
from app.agents.legal_agent import LegalAgent, MANDATORY_LEGAL_DISCLAIMER

app = FastAPI(
    title="Sahara Safety Companion API",
    version="1.0.0",
    docs_url="/docs",
    openapi_url="/api/v1/openapi.json"
)

legal_agent = LegalAgent()

# In-memory storage for demo/development
otp_requests: dict = {}
circle_store: dict = {}
sync_events_store: list = []
anchors_store: dict = {}

@app.post("/api/v1/auth/request-otp", response_model=RequestOtpResponse)
def request_otp(req: RequestOtpRequest):
    req_id = str(uuid.uuid4())
    otp_requests[req_id] = req.phone_number
    return RequestOtpResponse(request_id=req_id, status="PENDING", expires_in_seconds=300)

@app.post("/api/v1/auth/verify-otp", response_model=VerifyOtpResponse)
def verify_otp(req: VerifyOtpRequest):
    if req.request_id not in otp_requests:
        raise HTTPException(
            status_code=400,
            detail=ErrorEnvelope(error=ErrorDetail(code="INVALID_OTP", message="Invalid or expired OTP request ID")).model_dump()
        )
    user_id = str(uuid.uuid4())
    return VerifyOtpResponse(
        access_token=f"access_token_{user_id}",
        refresh_token=f"refresh_token_{user_id}",
        user_id=user_id
    )

@app.post("/api/v1/auth/refresh", response_model=VerifyOtpResponse)
def refresh_token(req: RefreshTokenRequest):
    user_id = str(uuid.uuid4())
    return VerifyOtpResponse(
        access_token=f"access_token_{user_id}",
        refresh_token=req.refresh_token,
        user_id=user_id
    )

@app.post("/api/v1/auth/logout", status_code=204)
def logout():
    return Response(status_code=status.HTTP_204_NO_CONTENT)

@app.get("/api/v1/notify/circle", response_model=list[CircleMember])
def get_circle(authorization: Optional[str] = Header(None)):
    return list(circle_store.values())

@app.put("/api/v1/notify/circle", response_model=list[CircleMember])
def update_circle(req: CircleUpdateRequest, authorization: Optional[str] = Header(None)):
    if len(req.members) > 5:
        raise HTTPException(
            status_code=400,
            detail=ErrorEnvelope(error=ErrorDetail(code="CIRCLE_LIMIT_EXCEEDED", message="Circle cannot exceed 5 members")).model_dump()
        )
    circle_store.clear()
    for m in req.members:
        circle_store[m.contact_id] = m
    return list(circle_store.values())

@app.post("/api/v1/notifications/incident-alert", response_model=IncidentAlertResponse)
def send_incident_alert(req: IncidentAlertRequest, authorization: Optional[str] = Header(None)):
    alert_id = str(uuid.uuid4())
    return IncidentAlertResponse(alert_id=alert_id, dispatched_count=len(circle_store), status="ACCEPTED")

@app.post("/api/v1/notifications/acknowledgements", status_code=200)
def record_acknowledgement(req: AcknowledgementRequest, authorization: Optional[str] = Header(None)):
    return {"status": "SUCCESS", "message": "Acknowledgement recorded"}

@app.post("/api/v1/sync/batch", response_model=BatchSyncResponse)
def batch_sync(req: BatchSyncRequest, authorization: Optional[str] = Header(None)):
    accepted_ids = []
    rejected = []
    for event in req.events:
        # Security check: verify raw audio is not uploaded
        payload_str = str(event.payload).lower()
        if "raw_audio" in payload_str or "audio_bytes" in payload_str:
            rejected.append({"event_id": event.event_id, "reason": "Raw evidence upload prohibited"})
        else:
            sync_events_store.append(event)
            accepted_ids.append(event.event_id)
    return BatchSyncResponse(accepted_event_ids=accepted_ids, rejected_events=rejected)

@app.post("/api/v1/anchors", response_model=AnchorResponse)
def create_anchor(req: AnchorRequest, authorization: Optional[str] = Header(None)):
    anchor_id = str(uuid.uuid4())
    resp = AnchorResponse(
        anchor_id=anchor_id,
        status="PENDING",
        transaction_hash=f"0x{uuid.uuid4().hex}",
        created_at=int(time.time())
    )
    anchors_store[anchor_id] = resp
    return resp

@app.get("/api/v1/anchors/{anchor_id}", response_model=AnchorResponse)
def get_anchor(anchor_id: str, authorization: Optional[str] = Header(None)):
    if anchor_id not in anchors_store:
        raise HTTPException(
            status_code=404,
            detail=ErrorEnvelope(error=ErrorDetail(code="ANCHOR_NOT_FOUND", message="Anchor request not found")).model_dump()
        )
    return anchors_store[anchor_id]

@app.post("/api/v1/legal/drafts", response_model=LegalDraftResponse)
def generate_legal_draft(req: LegalDraftRequest, authorization: Optional[str] = Header(None)):
    try:
        return legal_agent.generate_draft(req)
    except ValueError as e:
        raise HTTPException(
            status_code=400,
            detail=ErrorEnvelope(error=ErrorDetail(code="UNAUTHORIZED_LEGAL_REQUEST", message=str(e))).model_dump()
        )
