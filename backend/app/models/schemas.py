from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any

class ErrorDetail(BaseModel):
    code: str
    message: str
    retryable: bool = True

class ErrorEnvelope(BaseModel):
    error: ErrorDetail

class RequestOtpRequest(BaseModel):
    phone_number: str

class RequestOtpResponse(BaseModel):
    request_id: str
    status: str = "PENDING"
    expires_in_seconds: int = 300

class VerifyOtpRequest(BaseModel):
    request_id: str
    otp_code: str

class VerifyOtpResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "Bearer"
    expires_in_seconds: int = 3600
    user_id: str

class RefreshTokenRequest(BaseModel):
    refresh_token: str

class CircleMember(BaseModel):
    contact_id: str
    display_name: str
    phone_number: Optional[str] = None
    app_user_id: Optional[str] = None
    location_permission: bool = False
    notification_permission: bool = True

class CircleUpdateRequest(BaseModel):
    members: List[CircleMember]

class IncidentAlertRequest(BaseModel):
    incident_id: str
    incident_status: str
    timestamp: int
    approximate_location: Optional[str] = None
    integrity_reference: Optional[str] = None
    message: Optional[str] = None

class IncidentAlertResponse(BaseModel):
    alert_id: str
    dispatched_count: int
    status: str = "ACCEPTED"

class AcknowledgementRequest(BaseModel):
    alert_id: str
    contact_id: str
    acknowledged_at: int

class SyncEvent(BaseModel):
    event_id: str
    incident_id: str
    event_type: str
    timestamp: int
    payload: Dict[str, Any]

class BatchSyncRequest(BaseModel):
    events: List[SyncEvent]

class BatchSyncResponse(BaseModel):
    accepted_event_ids: List[str]
    rejected_events: List[Dict[str, Any]] = Field(default_factory=list)

class AnchorRequest(BaseModel):
    incident_id: str
    merkle_root: str
    device_signature: str
    timestamp: int

class AnchorResponse(BaseModel):
    anchor_id: str
    status: str = "PENDING"
    transaction_hash: Optional[str] = None
    created_at: int

class LegalDraftRequest(BaseModel):
    incident_id: str
    draft_type: str = "FIR_COMPLAINT"
    authorized_summary: Dict[str, Any]
    user_authorized: bool = True

class LegalDraftResponse(BaseModel):
    draft_id: str
    incident_id: str
    draft_type: str
    content: str
    disclaimer: str = "DRAFT FOR HUMAN AND LEGAL REVIEW. THIS DOCUMENT HAS NOT BEEN FILED WITH ANY AUTHORITY."
    created_at: int
