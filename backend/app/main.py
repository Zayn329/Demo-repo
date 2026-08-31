import time
import uuid
from fastapi import FastAPI, HTTPException, Header, Response, Depends, status
from typing import Optional
from sqlalchemy.orm import Session

from app.db.database import Base, engine, get_db
from app.db.models import DBCircleMember, DBSyncEvent, DBAnchor, DBUser
from app.models.schemas import (
    RequestOtpRequest, RequestOtpResponse, VerifyOtpRequest, VerifyOtpResponse,
    RefreshTokenRequest, CircleUpdateRequest, CircleMember, IncidentAlertRequest,
    IncidentAlertResponse, AcknowledgementRequest, BatchSyncRequest, BatchSyncResponse,
    AnchorRequest, AnchorResponse, LegalDraftRequest, LegalDraftResponse, ErrorEnvelope, ErrorDetail
)
from app.agents.legal_agent import LegalAgent, MANDATORY_LEGAL_DISCLAIMER

# Initialize database tables
try:
    Base.metadata.create_all(bind=engine)
except Exception:
    pass

app = FastAPI(
    title="Sahara Safety Companion API",
    version="1.0.0",
    docs_url="/docs",
    openapi_url="/api/v1/openapi.json"
)

legal_agent = LegalAgent()

# In-memory storage fallback for OTP requests
otp_requests: dict = {}

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
def get_circle(authorization: Optional[str] = Header(None), db: Session = Depends(get_db)):
    members = db.query(DBCircleMember).all()
    return [
        CircleMember(
            contact_id=m.contact_id,
            display_name=m.display_name,
            type=m.type,
            phone_number=m.phone_number,
            app_user_id=m.app_user_id,
            location_permission=m.location_permission,
            notification_permission=m.notification_permission
        ) for m in members
    ]

@app.put("/api/v1/notify/circle", response_model=list[CircleMember])
def update_circle(req: CircleUpdateRequest, authorization: Optional[str] = Header(None), db: Session = Depends(get_db)):
    if len(req.members) > 5:
        raise HTTPException(
            status_code=400,
            detail=ErrorEnvelope(error=ErrorDetail(code="CIRCLE_LIMIT_EXCEEDED", message="Circle cannot exceed 5 members")).model_dump()
        )
    db.query(DBCircleMember).delete()
    for m in req.members:
        db_m = DBCircleMember(
            contact_id=m.contact_id,
            display_name=m.display_name,
            type=m.type,
            phone_number=m.phone_number,
            app_user_id=m.app_user_id,
            location_permission=m.location_permission,
            notification_permission=m.notification_permission
        )
        db.add(db_m)
    db.commit()
    members = db.query(DBCircleMember).all()
    return [
        CircleMember(
            contact_id=m.contact_id,
            display_name=m.display_name,
            type=m.type,
            phone_number=m.phone_number,
            app_user_id=m.app_user_id,
            location_permission=m.location_permission,
            notification_permission=m.notification_permission
        ) for m in members
    ]

@app.post("/api/v1/notifications/incident-alert", response_model=IncidentAlertResponse)
def send_incident_alert(req: IncidentAlertRequest, authorization: Optional[str] = Header(None), db: Session = Depends(get_db)):
    alert_id = str(uuid.uuid4())
    count = db.query(DBCircleMember).count()
    return IncidentAlertResponse(alert_id=alert_id, dispatched_count=count, status="ACCEPTED")

@app.post("/api/v1/notifications/acknowledgements", status_code=200)
def record_acknowledgement(req: AcknowledgementRequest, authorization: Optional[str] = Header(None)):
    return {"status": "SUCCESS", "message": "Acknowledgement recorded"}

@app.post("/api/v1/sync/batch", response_model=BatchSyncResponse)
def batch_sync(req: BatchSyncRequest, authorization: Optional[str] = Header(None), db: Session = Depends(get_db)):
    accepted_ids = []
    rejected = []
    for event in req.events:
        # Security check: verify raw audio is not uploaded
        payload_str = str(event.payload).lower()
        if "raw_audio" in payload_str or "audio_bytes" in payload_str:
            rejected.append({"event_id": event.event_id, "reason": "Raw evidence upload prohibited"})
        else:
            existing = db.query(DBSyncEvent).filter(DBSyncEvent.event_id == event.event_id).first()
            if not existing:
                db_event = DBSyncEvent(
                    event_id=event.event_id,
                    incident_id=event.incident_id,
                    event_type=event.event_type,
                    occurred_at=getattr(event, "occurred_at", getattr(event, "timestamp", int(time.time()))),
                    payload=event.payload
                )
                db.add(db_event)
            accepted_ids.append(event.event_id)
    db.commit()
    return BatchSyncResponse(accepted_event_ids=accepted_ids, rejected_events=rejected)

@app.post("/api/v1/anchors", response_model=AnchorResponse)
def create_anchor(req: AnchorRequest, authorization: Optional[str] = Header(None), db: Session = Depends(get_db)):
    anchor_id = str(uuid.uuid4())
    tx_hash = f"0x{uuid.uuid4().hex}"
    created_at = int(time.time())
    db_anchor = DBAnchor(
        anchor_id=anchor_id,
        status="PENDING",
        transaction_hash=tx_hash,
        created_at=created_at
    )
    db.add(db_anchor)
    db.commit()
    return AnchorResponse(
        anchor_id=anchor_id,
        status="PENDING",
        transaction_hash=tx_hash,
        created_at=created_at
    )

@app.get("/api/v1/anchors/{anchor_id}", response_model=AnchorResponse)
def get_anchor(anchor_id: str, authorization: Optional[str] = Header(None), db: Session = Depends(get_db)):
    db_anchor = db.query(DBAnchor).filter(DBAnchor.anchor_id == anchor_id).first()
    if not db_anchor:
        raise HTTPException(
            status_code=404,
            detail=ErrorEnvelope(error=ErrorDetail(code="ANCHOR_NOT_FOUND", message="Anchor request not found")).model_dump()
        )
    return AnchorResponse(
        anchor_id=db_anchor.anchor_id,
        status=db_anchor.status,
        transaction_hash=db_anchor.transaction_hash,
        created_at=db_anchor.created_at
    )

@app.post("/api/v1/legal/drafts", response_model=LegalDraftResponse)
def generate_legal_draft(req: LegalDraftRequest, authorization: Optional[str] = Header(None)):
    try:
        return legal_agent.generate_draft(req)
    except ValueError as e:
        raise HTTPException(
            status_code=400,
            detail=ErrorEnvelope(error=ErrorDetail(code="UNAUTHORIZED_LEGAL_REQUEST", message=str(e))).model_dump()
        )
