@core @fallback_allowed
Feature: Post-Incident Legal Assistance

Sahara provides optional AI-assisted organization and complaint drafting
after an incident. Legal assistance is an enhancement and must not alter
the protected original evidence package.

Background:
Given a completed or Sealed incident exists

@core
Scenario: Legal assistance is available only from a completed incident
Given no completed or Sealed incident is selected
When the user views general safety monitoring features
Then Sahara does not automatically start legal drafting
And legal assistance remains an optional post-incident action

@core
Scenario: User explicitly approves data before legal drafting
Given the user selects legal assistance for a completed incident
When Sahara prepares to send incident information to a backend or LLM provider
Then Sahara clearly identifies the information intended for processing
And Sahara requires explicit user approval before sending it
And Sahara does not silently transmit incident data for legal drafting

@core
Scenario: Raw audio is not automatically sent to the LLM
Given the user approves legal drafting
When Sahara prepares information for the legal drafting provider
Then Sahara sends only the explicitly prepared structured metadata or incident summary
And Sahara does not automatically send raw incident audio

@core
Scenario: Legal drafting can organize incident information
Given the user approves legal assistance
And the legal drafting provider is available
When Sahara requests legal assistance
Then Sahara can generate a structured incident summary
And the result is clearly identified as AI-assisted output requiring human review

@core
Scenario: Legal drafting can generate a complaint-style draft
Given the user approves legal assistance
And the configured legal drafting provider is available
When the user requests a supported complaint or FIR-style draft
Then Sahara generates a draft based on the approved structured incident information
And the draft is explicitly labelled as requiring human and legal review
And Sahara does not automatically submit the draft to police, a court, an employer, or another authority

@core
Scenario: Backend or LLM unavailability is reported honestly
Given the user requests legal assistance
And the backend or LLM provider is unavailable
When Sahara cannot obtain a legal draft
Then Sahara clearly reports that legal drafting is unavailable
And Sahara provides the locally available structured incident summary when possible
And Sahara does not silently claim that a draft was generated

@core @fallback_allowed
Scenario: Allowed legal drafting fallback is clearly identified
Given the real legal drafting provider is unavailable
And an explicitly permitted local or simplified fallback exists
When Sahara provides the fallback result
Then Sahara clearly identifies the result as a fallback or degraded capability
And the completion report states "PASSED USING FALLBACK"

@core
Scenario: Generated legal draft remains separate from sealed evidence
Given Sahara generates a legal draft from a completed incident
When the draft is stored or displayed
Then the draft remains a separate derived artifact
And Sahara does not silently modify the original sealed evidence package
And the original evidence integrity state remains unchanged

@core
Scenario: User controls whether a generated legal draft is exported or shared
Given a legal draft has been generated
When the user views the draft
Then Sahara allows the user to review it before sharing
And Sahara does not automatically send the draft to an external authority
And Sahara clearly distinguishes the AI-generated draft from original incident evidence