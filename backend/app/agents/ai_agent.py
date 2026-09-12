import os
from app.models.schemas import (
    LegalDraftRequest, LegalDraftResponse,
    AISummaryRequest, AISummaryResponse,
    AITimelineRequest, AITimelineResponse,
    AIQaRequest, AIQaResponse,
    AIReportRequest, AIReportResponse
)
from app.agents.providers import (
    get_ai_provider, AIProviderUnavailableError, MANDATORY_LEGAL_DISCLAIMER
)

class AIAgent:
    """
    Unified AI Agent entry point that delegates to the configured AIProvider
    (GroqLLMProvider or DeterministicMockAIProvider).
    """

    def _check_availability_and_auth(self, user_authorized: bool):
        if os.getenv("AI_PROVIDER_UNAVAILABLE", "false").lower() == "true":
            raise AIProviderUnavailableError("The AI provider is currently marked unavailable.")
        if not user_authorized:
            raise ValueError("User explicit authorization required before processing incident data with AI.")

    def generate_legal_draft(self, request: LegalDraftRequest) -> LegalDraftResponse:
        self._check_availability_and_auth(request.user_authorized)
        provider = get_ai_provider()
        return provider.generate_legal_draft(request)

    def generate_summary(self, request: AISummaryRequest) -> AISummaryResponse:
        self._check_availability_and_auth(request.user_authorized)
        provider = get_ai_provider()
        return provider.generate_summary(request)

    def explain_timeline(self, request: AITimelineRequest) -> AITimelineResponse:
        self._check_availability_and_auth(request.user_authorized)
        provider = get_ai_provider()
        return provider.explain_timeline(request)

    def answer_question(self, request: AIQaRequest) -> AIQaResponse:
        self._check_availability_and_auth(request.user_authorized)
        provider = get_ai_provider()
        return provider.answer_question(request)

    def generate_report(self, request: AIReportRequest) -> AIReportResponse:
        self._check_availability_and_auth(request.user_authorized)
        provider = get_ai_provider()
        return provider.generate_report(request)
