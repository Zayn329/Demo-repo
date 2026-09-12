@core @ai_assistance
Feature: Post-Incident AI Assistance Features

Sahara provides AI assistance for incident summarization, timeline explanation,
evidence-aware Q&A, FIR complaint drafting, and incident report generation.
AI assistance is purely advisory, post-incident, and never alters or overwrites
sealed evidence or cryptographic integrity records.

Background:
Given a completed or Sealed incident exists with verified structured facts

@core
Scenario: AI Incident Summarization
Given the user approves AI processing for a completed incident
And the AI provider is available
When Sahara requests an incident summary
Then Sahara produces a structured summary of the incident facts
And the summary contains the mandatory AI review disclaimer
And the sealed incident evidence remains unchanged

@core
Scenario: AI Timeline Reconstruction and Explanation
Given the user approves AI processing for a completed incident
And the incident contains verified chronological telemetry events
And the AI provider is available
When Sahara requests a timeline explanation
Then Sahara generates chronological entries explaining each event in plain language
And each event explanation references the verified timestamp and event type
And the output is clearly marked as AI-assisted timeline explanation

@core
Scenario: AI Evidence-Aware Question Answering
Given the user approves AI processing for a completed incident
And verified incident facts are provided
And the AI provider is available
When the user asks "What triggered the incident?"
Then Sahara returns an answer grounded strictly in the verified facts
And the answer references the specific verified detection event
And the response indicates the grounded facts used

@core
Scenario: AI Evidence-Aware Q&A handles missing or unknown information
Given the user approves AI processing for a completed incident
And the verified incident facts do not contain suspect identification or vehicle details
And the AI provider is available
When the user asks "What was the suspect's license plate?"
Then Sahara explicitly states that the information is unknown or missing from verified facts
And Sahara does not fabricate missing incident facts

@core
Scenario: AI Incident Report Generation
Given the user approves AI processing for a completed incident
And the AI provider is available
When the user requests a formal incident report
Then Sahara generates a comprehensive report based on authorized structured facts
And the report includes the mandatory legal and human review disclaimer

@core
Scenario: FIR Drafting Backward Compatibility
Given the user approves legal drafting for a completed incident
And the AI provider is available
When the user requests an FIR complaint draft
Then Sahara generates the complaint draft with required disclaimers and Merkle root references
And existing FIR drafting contracts remain fully backward compatible

@core
Scenario: Explicit failure when AI provider is unavailable
Given the user requests AI assistance
And the backend AI provider is unavailable
When Sahara attempts to process the AI request
Then Sahara explicitly returns an AI provider unavailable error
And Sahara does not silently manufacture fake AI output

@core
Scenario: AI cannot modify or overwrite sealed evidence
Given an incident has been sealed with a cryptographic Merkle root
When any AI capability (summarization, timeline, Q&A, report, or FIR drafting) is executed
Then the original sealed evidence files remain intact
And the Merkle root and manifest signature remain unchanged

@core
Scenario: User authorization is required before AI processing
Given an incident exists
When an AI assistance request is made without explicit user authorization
Then the request is rejected
And no incident data is transmitted to the AI provider
