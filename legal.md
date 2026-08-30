Legal Assistance API Contract

Purpose

The legal assistance API converts user-approved structured incident information into a draft document.

It is an assistance feature, not an autonomous legal filing system.

Every generated document must be clearly presented as a draft requiring human or legal review.

Endpoint

POST /api/v1/legal/drafts

Authentication is required.

The operation may use an "Idempotency-Key" because generation creates a durable draft record or result.

Data boundary

The API must never automatically receive raw audio evidence.

The minimum request data is:

- Structured incident summary prepared by the application.
- Evidence hashes or Merkle integrity information.
- Basic incident timestamps.
- Approximate location only when the user has already approved sharing it.

Optional information includes:

- User-entered narrative.
- Witness statements.

The client must explicitly confirm approval for external processing before the request is sent.

Draft types

The API supports:

incident_summary
fir_style_complaint
generic_police_complaint

The selected type controls the requested structure and presentation of the draft. It does not guarantee that a generated document is legally sufficient or appropriate for filing.

Response

The backend returns structured JSON rather than a mandatory PDF.

This allows the Android application to:

- Render the draft accessibly.
- Let the user review the content.
- Allow later export.
- Keep generated legal content separate from sealed evidence.

The response includes:

- Draft identifier.
- Requested document type.
- Title.
- Summary.
- Structured sections.
- Disclaimers.
- Legal references.
- Mandatory review indicator.
- Generation timestamp.
- Input retention status.

Input retention

Structured incident input is intended to be deleted immediately after generation.

The response must explicitly state the resulting retention state.

The expected successful state is:

deleted_after_generation

If deletion cannot be confirmed, the backend must not silently claim deletion.

The response must contain an explicit warning state or the request must fail according to the implementation's privacy guarantees. The client must visibly surface any retention warning.

Provider failure

If the LLM provider is unavailable, the API must return an explicit "LEGAL_PROVIDER_UNAVAILABLE" error.

The client may offer a locally generated structured incident summary, but must not silently represent it as an AI-generated legal draft.

Validation

Generated output must pass deterministic validation and schema validation before being returned.

The implementation should reject malformed output rather than exposing partially parsed model output as a completed legal draft.

Legal references must be presented as references or suggested context, not as a guarantee that the document has been legally validated.

Evidence relationship

A generated legal draft is a derived artifact.

It is not automatically inserted into the original sealed evidence package.

The application may later export it alongside an incident, but doing so must not modify the original sealed evidence integrity root.