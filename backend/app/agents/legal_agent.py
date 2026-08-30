import time
import uuid
from typing import Dict, Any, List
from app.models.schemas import LegalDraftRequest, LegalDraftResponse

MANDATORY_LEGAL_DISCLAIMER = "DRAFT FOR HUMAN AND LEGAL REVIEW. THIS DOCUMENT HAS NOT BEEN FILED WITH ANY AUTHORITY."

class LegalAgent:

    def generate_draft(self, request: LegalDraftRequest) -> LegalDraftResponse:
        if not request.user_authorized:
            raise ValueError("User authorization required before legal draft generation.")

        summary = request.authorized_summary
        incident_id = request.incident_id
        draft_type = request.draft_type

        # Extract structured details safely
        title = summary.get("title", "Safety Incident Summary")
        date_str = summary.get("date", "Unknown Date")
        location_str = summary.get("location", "Location Not Provided")
        trigger_sources = ", ".join(summary.get("trigger_sources", ["Automatic Detection"]))
        merkle_root = summary.get("merkle_root", "N/A")

        if draft_type.upper() == "FIR_COMPLAINT":
            draft_text = (
                f"FORMAL INCIDENT COMPLAINT DRAFT (FIR STYLE)\n"
                f"-----------------------------------------\n"
                f"Incident Reference ID: {incident_id}\n"
                f"Date & Time: {date_str}\n"
                f"Location: {location_str}\n"
                f"Triggering Signals: {trigger_sources}\n"
                f"Integrity Proof (Merkle Root): {merkle_root}\n\n"
                f"STATEMENT OF INCIDENT:\n"
                f"On {date_str}, an automated on-device safety detection event occurred at location {location_str}. "
                f"The device recorded distress signals including [{trigger_sources}]. Encrypted evidence packages were captured "
                f"and cryptographically sealed locally on the primary device.\n\n"
                f"EVIDENCE & INTEGRITY:\n"
                f"All raw audio and sensor logs remain preserved on-device under tamper-evident SHA-256 Merkle root {merkle_root}.\n\n"
                f"REQUESTED ACTION:\n"
                f"This document is submitted for human and legal review to assist in organizing the incident timeline and filing formal reports."
            )
        else:
            draft_text = (
                f"INCIDENT SUMMARY REPORT\n"
                f"-----------------------\n"
                f"Title: {title}\n"
                f"Incident ID: {incident_id}\n"
                f"Date: {date_str}\n"
                f"Location: {location_str}\n"
                f"Evidence Root: {merkle_root}\n"
            )

        return LegalDraftResponse(
            draft_id=str(uuid.uuid4()),
            incident_id=incident_id,
            draft_type=draft_type,
            content=draft_text,
            disclaimer=MANDATORY_LEGAL_DISCLAIMER,
            created_at=int(time.time())
        )
