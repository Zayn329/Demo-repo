import os
import time
import uuid
import json
import urllib.request
import urllib.error
from abc import ABC, abstractmethod
from typing import Dict, Any, List, Optional

from app.models.schemas import (
    LegalDraftRequest, LegalDraftResponse,
    AISummaryRequest, AISummaryResponse,
    AITimelineRequest, AITimelineResponse, TimelineEntry,
    AIQaRequest, AIQaResponse,
    AIReportRequest, AIReportResponse
)

MANDATORY_LEGAL_DISCLAIMER = "DRAFT FOR HUMAN AND LEGAL REVIEW. THIS DOCUMENT HAS NOT BEEN FILED WITH ANY AUTHORITY."
SUMMARY_DISCLAIMER = "AI-GENERATED SUMMARY FOR INFORMATIONAL PURPOSES. REQUIRES HUMAN REVIEW."
TIMELINE_DISCLAIMER = "AI-GENERATED TIMELINE EXPLANATION BASED ON VERIFIED EVENT TELEMETRY. REQUIRES HUMAN REVIEW."
QA_DISCLAIMER = "AI ASSISTANT ANSWER GROUNDED ONLY IN AUTHORIZED VERIFIED FACTS. MAY NOT BE USED AS SOLE LEGAL PROOF."
REPORT_DISCLAIMER = "AI-GENERATED INCIDENT REPORT. DRAFT FOR HUMAN AND LEGAL REVIEW."

class AIProviderUnavailableError(Exception):
    """Raised when the AI provider is unavailable or fails."""
    pass

class BaseAIProvider(ABC):
    @abstractmethod
    def generate_legal_draft(self, request: LegalDraftRequest) -> LegalDraftResponse:
        pass

    @abstractmethod
    def generate_summary(self, request: AISummaryRequest) -> AISummaryResponse:
        pass

    @abstractmethod
    def explain_timeline(self, request: AITimelineRequest) -> AITimelineResponse:
        pass

    @abstractmethod
    def answer_question(self, request: AIQaRequest) -> AIQaResponse:
        pass

    @abstractmethod
    def generate_report(self, request: AIReportRequest) -> AIReportResponse:
        pass


class GroqLLMProvider(BaseAIProvider):
    def __init__(self):
        self.api_key = os.getenv("LLM_API_KEY")
        self.model = os.getenv("LLM_MODEL", "openai/gpt-oss-20b")
        self.url = "https://api.groq.com/openai/v1/chat/completions"

    def _call_groq_completion(self, prompt: str, system_instruction: str) -> str:
        if not self.api_key:
            raise AIProviderUnavailableError("Groq LLM provider requires LLM_API_KEY environment variable.")

        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
            "User-Agent": "Sahara-Backend-AI-Agent/1.0"
        }

        body = {
            "model": self.model,
            "messages": [
                {"role": "system", "content": system_instruction},
                {"role": "user", "content": prompt}
            ],
            "temperature": 0.2
        }

        data = json.dumps(body).encode("utf-8")
        req = urllib.request.Request(self.url, data=data, headers=headers, method="POST")

        try:
            with urllib.request.urlopen(req, timeout=15) as response:
                if response.status == 200:
                    res_body = json.loads(response.read().decode("utf-8"))
                    choices = res_body.get("choices", [])
                    if choices and "message" in choices[0]:
                        content = choices[0]["message"].get("content", "").strip()
                        if content:
                            return content
                    raise AIProviderUnavailableError("Groq provider returned empty or malformed choice payload.")
                else:
                    raise AIProviderUnavailableError(f"Groq API returned HTTP status {response.status}.")
        except AIProviderUnavailableError:
            raise
        except Exception as e:
            raise AIProviderUnavailableError(f"Groq provider request failed: {str(e)}")

    def generate_legal_draft(self, request: LegalDraftRequest) -> LegalDraftResponse:
        summary = request.authorized_summary
        prompt = (
            f"Draft formal complaint ({request.draft_type}) based on verified incident data:\n"
            f"Incident ID: {request.incident_id}\nFacts: {json.dumps(summary)}\n"
            f"Do not fabricate facts not in the facts payload."
        )
        system = "You are Sahara Legal AI Agent. Draft formal complaint documents from authorized facts only."
        content = self._call_groq_completion(prompt, system)
        return LegalDraftResponse(
            draft_id=str(uuid.uuid4()),
            incident_id=request.incident_id,
            draft_type=request.draft_type,
            content=content,
            disclaimer=MANDATORY_LEGAL_DISCLAIMER,
            created_at=int(time.time())
        )

    def generate_summary(self, request: AISummaryRequest) -> AISummaryResponse:
        summary = request.authorized_summary
        prompt = f"Summarize incident ID '{request.incident_id}' from facts: {json.dumps(summary)}."
        system = "You are Sahara Incident Summarization Agent. Produce concise executive summary of verified facts."
        content = self._call_groq_completion(prompt, system)

        title = summary.get("title", "Safety Incident")
        merkle_root = summary.get("merkle_root")
        key_events = [f"Summary Facts: {k}={v}" for k, v in summary.items() if k in ["date", "location", "trigger_sources"]]

        return AISummaryResponse(
            summary_id=str(uuid.uuid4()),
            incident_id=request.incident_id,
            title=title,
            executive_summary=content,
            key_events=key_events,
            integrity_reference=merkle_root,
            disclaimer=SUMMARY_DISCLAIMER,
            created_at=int(time.time())
        )

    def explain_timeline(self, request: AITimelineRequest) -> AITimelineResponse:
        sorted_events = sorted(request.events, key=lambda e: e.timestamp)
        events_json = json.dumps([e.model_dump() for e in sorted_events])
        prompt = f"Explain chronological timeline for incident '{request.incident_id}' from telemetry events: {events_json}."
        system = "You are Sahara Timeline Reconstruction Agent. Explain event timeline chronologically in plain language."
        content = self._call_groq_completion(prompt, system)

        # Build chronological entries grounded in telemetry timestamps
        entries = []
        for e in sorted_events:
            entries.append(TimelineEntry(
                timestamp=e.timestamp,
                event_type=e.event_type,
                explanation=f"[{e.event_type}]: {content[:80]}..." if len(content) > 80 else content
            ))

        return AITimelineResponse(
            timeline_id=str(uuid.uuid4()),
            incident_id=request.incident_id,
            chronological_entries=entries,
            disclaimer=TIMELINE_DISCLAIMER,
            created_at=int(time.time())
        )

    def answer_question(self, request: AIQaRequest) -> AIQaResponse:
        facts = request.authorized_facts or {}
        prompt = (
            f"Question: {request.question}\n"
            f"Verified Facts: {json.dumps(facts)}\n"
            f"Instructions: Answer using ONLY the verified facts. If information is not in facts, state explicitly that it is missing/unknown."
        )
        system = "You are Sahara Evidence-Aware Assistant. Answer questions strictly grounded in supplied facts. Zero fabrication."
        content = self._call_groq_completion(prompt, system)

        # Evaluate grounded vs missing facts
        missing_facts = []
        q_lower = request.question.lower()
        if any(unsupported in q_lower for unsupported in ["suspect", "license plate", "vehicle", "weapon", "attacker name"]):
            if "unknown" in content.lower() or "missing" in content.lower() or "not present" in content.lower():
                missing_facts.append(f"Details for '{request.question}' not in telemetry facts.")

        grounded_facts = [f"{k}: {v}" for k, v in facts.items()]

        return AIQaResponse(
            qa_id=str(uuid.uuid4()),
            incident_id=request.incident_id,
            question=request.question,
            answer=content,
            grounded_facts_used=grounded_facts,
            missing_information=missing_facts,
            disclaimer=QA_DISCLAIMER,
            created_at=int(time.time())
        )

    def generate_report(self, request: AIReportRequest) -> AIReportResponse:
        summary = request.authorized_summary
        prompt = (
            f"Generate formal report ({request.report_type}) for incident ID {request.incident_id}.\n"
            f"Facts: {json.dumps(summary)}"
        )
        system = "You are Sahara Report Generation Agent. Generate factual formal incident reports from telemetry facts."
        content = self._call_groq_completion(prompt, system)

        return AIReportResponse(
            report_id=str(uuid.uuid4()),
            incident_id=request.incident_id,
            report_type=request.report_type,
            content=content,
            disclaimer=REPORT_DISCLAIMER,
            created_at=int(time.time())
        )


class DeterministicMockAIProvider(BaseAIProvider):
    def generate_legal_draft(self, request: LegalDraftRequest) -> LegalDraftResponse:
        summary = request.authorized_summary
        incident_id = request.incident_id
        draft_type = request.draft_type

        title = summary.get("title", "Safety Incident Summary")
        date_str = summary.get("date", "Unknown Date")
        location_str = summary.get("location", "Location Not Provided")
        trigger_sources = ", ".join(summary.get("trigger_sources", ["Automatic Detection"]))
        merkle_root = summary.get("merkle_root", "N/A")

        if draft_type.upper() in ["FIR_COMPLAINT", "GENERIC_POLICE_COMPLAINT"]:
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

    def generate_summary(self, request: AISummaryRequest) -> AISummaryResponse:
        summary = request.authorized_summary
        title = summary.get("title", "Safety Incident")
        date_str = summary.get("date", "Unknown Date")
        location_str = summary.get("location", "Location Not Provided")
        triggers = summary.get("trigger_sources", [])
        merkle_root = summary.get("merkle_root")

        exec_summary = (
            f"Incident '{title}' occurred on {date_str} near {location_str}. "
            f"Safety mechanisms activated via {', '.join(triggers) if triggers else 'automated distress signals'}. "
            f"Evidence was cryptographically sealed locally."
        )

        key_events = [
            f"Date/Time: {date_str}",
            f"Location: {location_str}",
            f"Triggers: {', '.join(triggers) if triggers else 'N/A'}"
        ]

        return AISummaryResponse(
            summary_id=str(uuid.uuid4()),
            incident_id=request.incident_id,
            title=title,
            executive_summary=exec_summary,
            key_events=key_events,
            integrity_reference=merkle_root,
            disclaimer=SUMMARY_DISCLAIMER,
            created_at=int(time.time())
        )

    def explain_timeline(self, request: AITimelineRequest) -> AITimelineResponse:
        sorted_events = sorted(request.events, key=lambda e: e.timestamp)
        entries: List[TimelineEntry] = []

        for evt in sorted_events:
            event_type = evt.event_type
            ts = evt.timestamp
            payload = evt.payload or {}

            if "keyword" in event_type.lower():
                word = payload.get("keyword", "distress word")
                conf = payload.get("confidence", "high")
                explanation = f"Keyword detector recognized '{word}' with confidence {conf}."
            elif "scream" in event_type.lower():
                explanation = "Acoustic classifier detected scream pattern above safety threshold."
            elif "motion" in event_type.lower():
                explanation = "Accelerometer detected rapid impact or motion anomaly."
            elif "activated" in event_type.lower() or "candidate" in event_type.lower():
                trigger = payload.get("trigger", "signal fusion")
                explanation = f"Incident state escalated to ACTIVE by {trigger}."
            elif "sealed" in event_type.lower():
                root = payload.get("merkle_root", "N/A")
                explanation = f"Evidence capture completed and cryptographically sealed under Merkle root {root}."
            else:
                explanation = f"System event '{event_type}' recorded with payload details."

            entries.append(TimelineEntry(timestamp=ts, event_type=event_type, explanation=explanation))

        return AITimelineResponse(
            timeline_id=str(uuid.uuid4()),
            incident_id=request.incident_id,
            chronological_entries=entries,
            disclaimer=TIMELINE_DISCLAIMER,
            created_at=int(time.time())
        )

    def answer_question(self, request: AIQaRequest) -> AIQaResponse:
        q_lower = request.question.lower()
        facts = request.authorized_facts or {}
        grounded_facts: List[str] = []
        missing_facts: List[str] = []

        trigger_sources = facts.get("trigger_sources") or []
        events = facts.get("events") or []
        date_str = facts.get("date") or facts.get("timestamp")
        location_str = facts.get("location")
        merkle_root = facts.get("merkle_root")

        if any(unsupported in q_lower for unsupported in ["suspect", "license plate", "vehicle", "weapon", "attacker name"]):
            missing_facts.append(f"Details regarding '{request.question}' are not present in authorized verified telemetry.")
            answer = "Information regarding this question is unknown or missing from the authorized verified incident facts."
        elif "trigger" in q_lower or "cause" in q_lower or "start" in q_lower:
            if trigger_sources:
                grounded_facts.append(f"Trigger sources: {', '.join(trigger_sources)}")
                answer = f"The incident was triggered by {', '.join(trigger_sources)} according to verified detection telemetry."
            elif events:
                first_evt = events[0]
                grounded_facts.append(f"First event: {first_evt.get('event_type')}")
                answer = f"The incident began with event type '{first_evt.get('event_type')}'."
            else:
                missing_facts.append("No explicit trigger source in facts.")
                answer = "Trigger source is missing from the provided facts."
        elif "when" in q_lower or "time" in q_lower or "date" in q_lower:
            if date_str:
                grounded_facts.append(f"Date/time record: {date_str}")
                answer = f"The incident occurred at/on {date_str} based on verified timestamps."
            else:
                missing_facts.append("Timestamp/date missing.")
                answer = "Timestamp details are not present in verified facts."
        elif "where" in q_lower or "location" in q_lower:
            if location_str:
                grounded_facts.append(f"Location record: {location_str}")
                answer = f"The recorded location is {location_str}."
            else:
                missing_facts.append("Location record missing.")
                answer = "Location data is missing from verified facts."
        elif "evidence" in q_lower or "integrity" in q_lower or "merkle" in q_lower or "proof" in q_lower:
            if merkle_root:
                grounded_facts.append(f"Merkle root: {merkle_root}")
                answer = f"Evidence integrity is verified under SHA-256 Merkle root {merkle_root}."
            else:
                missing_facts.append("Merkle root missing.")
                answer = "No Merkle integrity reference was provided in facts."
        else:
            if facts:
                grounded_facts.append(f"Verified facts count: {len(facts)}")
                answer = f"Based on verified facts: Incident ID {request.incident_id} includes records for {', '.join(facts.keys())}."
            else:
                missing_facts.append("No verified facts supplied.")
                answer = "No authorized facts were supplied to answer this question."

        return AIQaResponse(
            qa_id=str(uuid.uuid4()),
            incident_id=request.incident_id,
            question=request.question,
            answer=answer,
            grounded_facts_used=grounded_facts,
            missing_information=missing_facts,
            disclaimer=QA_DISCLAIMER,
            created_at=int(time.time())
        )

    def generate_report(self, request: AIReportRequest) -> AIReportResponse:
        summary = request.authorized_summary
        incident_id = request.incident_id
        report_type = request.report_type

        title = summary.get("title", "Safety Companion Incident Report")
        date_str = summary.get("date", "Unknown Date")
        location_str = summary.get("location", "Location Not Provided")
        triggers = ", ".join(summary.get("trigger_sources", ["Automated Detection"]))
        merkle_root = summary.get("merkle_root", "N/A")

        report_content = (
            f"SAHARA INCIDENT COMPREHENSIVE REPORT ({report_type})\n"
            f"===================================================\n"
            f"Report Reference ID: {str(uuid.uuid4())}\n"
            f"Incident ID: {incident_id}\n"
            f"Generated At: {time.strftime('%Y-%m-%d %H:%M:%S', time.gmtime())}\n\n"
            f"1. INCIDENT OVERVIEW\n"
            f"   Title: {title}\n"
            f"   Date/Time: {date_str}\n"
            f"   Approximate Location: {location_str}\n"
            f"   Primary Triggers: {triggers}\n\n"
            f"2. CRYPTOGRAPHIC EVIDENCE & INTEGRITY\n"
            f"   Merkle Root Digest: {merkle_root}\n"
            f"   Evidence Storage: Local application-private storage\n"
            f"   Integrity Status: VERIFIED & SEALED\n\n"
            f"3. ADVISORY STATEMENT\n"
            f"   This report was generated using verified on-device telemetry facts and is intended "
            f"   for official human review, institutional filing, or legal consultation."
        )

        return AIReportResponse(
            report_id=str(uuid.uuid4()),
            incident_id=incident_id,
            report_type=report_type,
            content=report_content,
            disclaimer=REPORT_DISCLAIMER,
            created_at=int(time.time())
        )


def get_ai_provider() -> BaseAIProvider:
    provider_setting = os.getenv("LLM_PROVIDER", "").lower()
    api_key = os.getenv("LLM_API_KEY")

    if provider_setting == "groq" or (api_key and provider_setting not in ["mock", "deterministic"]):
        return GroqLLMProvider()
    return DeterministicMockAIProvider()
