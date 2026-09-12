import os
import pytest
from app.agents.ai_agent import AIAgent, AIProviderUnavailableError
from app.models.schemas import (
    LegalDraftRequest, AISummaryRequest, AITimelineRequest,
    TimelineEventInput, AIQaRequest, AIReportRequest
)

@pytest.fixture
def agent():
    return AIAgent()

def test_legal_draft_generation(agent):
    req = LegalDraftRequest(
        incident_id="inc_001",
        draft_type="FIR_COMPLAINT",
        authorized_summary={
            "title": "Night Commute Safety Incident",
            "date": "2026-08-30 23:00:00",
            "location": "Station Road",
            "trigger_sources": ["KEYWORD_HELP", "SCREAM"],
            "merkle_root": "0x123abc"
        },
        user_authorized=True
    )
    res = agent.generate_legal_draft(req)
    assert res.incident_id == "inc_001"
    assert "DRAFT FOR HUMAN AND LEGAL REVIEW" in res.disclaimer
    assert "0x123abc" in res.content
    assert "FORMAL INCIDENT COMPLAINT DRAFT" in res.content

def test_summary_generation(agent):
    req = AISummaryRequest(
        incident_id="inc_002",
        authorized_summary={
            "title": "Distress Signal Activated",
            "date": "2026-08-31 10:00:00",
            "location": "North Campus",
            "trigger_sources": ["PANIC_BUTTON"],
            "merkle_root": "0xdef456"
        },
        user_authorized=True
    )
    res = agent.generate_summary(req)
    assert res.incident_id == "inc_002"
    assert "North Campus" in res.executive_summary
    assert res.integrity_reference == "0xdef456"
    assert "AI-GENERATED SUMMARY" in res.disclaimer

def test_timeline_explanation(agent):
    req = AITimelineRequest(
        incident_id="inc_003",
        events=[
            TimelineEventInput(event_id="e1", event_type="detection.keyword_detected", timestamp=1000, payload={"keyword": "HELP", "confidence": 0.95}),
            TimelineEventInput(event_id="e2", event_type="detection.scream_detected", timestamp=1002, payload={}),
            TimelineEventInput(event_id="e3", event_type="incident.sealed", timestamp=1010, payload={"merkle_root": "0x999"})
        ],
        user_authorized=True
    )
    res = agent.explain_timeline(req)
    assert len(res.chronological_entries) == 3
    assert res.chronological_entries[0].timestamp == 1000
    assert "HELP" in res.chronological_entries[0].explanation
    assert "scream" in res.chronological_entries[1].explanation.lower()

def test_qa_grounded_answer(agent):
    req = AIQaRequest(
        incident_id="inc_004",
        question="What triggered the distress alert?",
        authorized_facts={
            "trigger_sources": ["KEYWORD_HELP", "MOTION_IMPACT"],
            "date": "2026-08-30 22:00:00"
        },
        user_authorized=True
    )
    res = agent.answer_question(req)
    assert "KEYWORD_HELP, MOTION_IMPACT" in res.answer
    assert len(res.grounded_facts_used) > 0
    assert len(res.missing_information) == 0

def test_qa_missing_fact_handling_no_fabrication(agent):
    req = AIQaRequest(
        incident_id="inc_005",
        question="What was the suspect's vehicle license plate number?",
        authorized_facts={
            "trigger_sources": ["KEYWORD_HELP"],
            "date": "2026-08-30 22:00:00"
        },
        user_authorized=True
    )
    res = agent.answer_question(req)
    assert "unknown or missing" in res.answer.lower()
    assert len(res.missing_information) > 0
    assert "license plate" in res.missing_information[0].lower()

def test_report_generation(agent):
    req = AIReportRequest(
        incident_id="inc_006",
        report_type="FORMAL_INCIDENT_REPORT",
        authorized_summary={
            "title": "Comprehensive Safety Incident",
            "date": "2026-08-30",
            "location": "Metro Line 1",
            "trigger_sources": ["SCREAM_CLASSIFIER"],
            "merkle_root": "0xmerkle777"
        },
        user_authorized=True
    )
    res = agent.generate_report(req)
    assert res.incident_id == "inc_006"
    assert "SAHARA INCIDENT COMPREHENSIVE REPORT" in res.content
    assert "0xmerkle777" in res.content

def test_provider_unavailability_error(agent, monkeypatch):
    monkeypatch.setenv("AI_PROVIDER_UNAVAILABLE", "true")
    req = AISummaryRequest(
        incident_id="inc_err",
        authorized_summary={"title": "Error Test"},
        user_authorized=True
    )
    with pytest.raises(AIProviderUnavailableError):
        agent.generate_summary(req)

def test_unauthorized_user_rejection(agent):
    req = AISummaryRequest(
        incident_id="inc_unauth",
        authorized_summary={"title": "Unauth Test"},
        user_authorized=False
    )
    with pytest.raises(ValueError) as exc:
        agent.generate_summary(req)
    assert "authorization required" in str(exc.value)
