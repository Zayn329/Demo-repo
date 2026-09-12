from app.agents.ai_agent import AIAgent, MANDATORY_LEGAL_DISCLAIMER
from app.models.schemas import LegalDraftRequest, LegalDraftResponse

class LegalAgent(AIAgent):
    """
    Backward-compatible LegalAgent extending AIAgent.
    """

    def generate_draft(self, request: LegalDraftRequest) -> LegalDraftResponse:
        return self.generate_legal_draft(request)
