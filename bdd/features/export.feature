@core @protected @demo @emulator_allowed
Feature: Evidence Export

Sahara allows users to export an incident as a human-readable and
integrity-protected package without overstating the legal meaning of
technical integrity verification.

Background:
Given an incident is available locally

@core @demo
Scenario: Verified sealed incident can be exported as a complete package
Given an incident is Sealed
And integrity verification succeeds
When the user chooses to export the incident
Then Sahara creates an export package
And the package contains the required protected evidence
And the package contains integrity information required for verification
And Sahara creates a human-readable incident summary
And Sahara makes the export available through the supported Android sharing flow

@core
Scenario: Cancelled incident can be exported
Given an incident is Cancelled and Sealed
When the user chooses to export the incident
Then Sahara allows the export
And the human-readable summary clearly identifies the incident as cancelled
And Sahara does not remove the captured evidence merely because the incident was cancelled

@core
Scenario: Export proceeds when optional blockchain anchoring is unavailable
Given an incident is ready for export
And optional blockchain anchoring is unavailable or disabled
When the user exports the incident
Then Sahara completes the local export without requiring blockchain anchoring
And Sahara does not falsely claim that an on-chain proof exists

@core @protected
Scenario: Failed integrity verification blocks verified claims
Given an incident evidence package fails integrity verification
When the user attempts export
Then Sahara clearly warns that integrity verification failed
And Sahara does not label the export as verified
And Sahara does not suppress the verification failure from the export information

@core @protected
Scenario: User may export failed-verification evidence with warning
Given an incident evidence package failed integrity verification
When the user explicitly continues with export
Then Sahara allows export with a strong visible warning
And the exported package indicates that integrity verification failed

@core
Scenario: Export includes an integrity limitation disclaimer
Given Sahara creates an evidence export
When the human-readable summary is generated
Then the export clearly explains that technical integrity protection does not guarantee legal admissibility
And Sahara does not claim that the package is automatically accepted by police or courts

@core
Scenario: Export does not automatically upload raw evidence
Given an incident is ready for export
When the user initiates local export
Then Sahara does not automatically upload raw evidence to a backend or third-party storage service
And sharing occurs only through the user's explicit export action

@core
Scenario: Export preserves the original local incident
Given an incident is successfully exported
When the export completes
Then Sahara retains the local incident according to retention settings
And export does not automatically delete or modify the original protected evidence package