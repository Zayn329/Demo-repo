@core @protected
Feature: Incident Lifecycle

Sahara manages distress events as a controlled incident lifecycle.
Once an incident is confirmed, its recorded history must remain
preserved and must not silently return to an earlier safety state.

Background:
Given Sahara is operational

@core
Scenario: Only one incident may be active at a time
Given an Active Incident already exists
When another distress trigger occurs
Then Sahara does not create a competing simultaneous active incident
And Sahara handles the additional signal according to the active incident's configured evidence behavior

@core
Scenario: Possible distress transitions to confirmed incident
Given Sahara is in Possible Distress
When the configured confirmation rule is satisfied
Then Sahara creates or confirms one incident
And the incident enters Active Incident
And required evidence protection and delivery workflows begin independently

@core
Scenario: Confirmed incident cannot return to a lower unconfirmed state
Given an incident has entered Active Incident
When the user cancels or stops further escalation
Then Sahara does not erase the fact that the incident was confirmed
And Sahara preserves the incident history
And the incident is marked with its resulting status

@core
Scenario: Cancelled incident remains preserved
Given an incident was confirmed
When the user cancels the incident
Then the incident is marked Cancelled
And already captured evidence remains associated with the incident
And the incident is sealed according to the evidence lifecycle

@core
Scenario: Sealed incident cannot accept arbitrary new evidence
Given an incident is Sealed
When additional safety-relevant evidence needs to be captured
Then Sahara does not silently append it to the sealed evidence package
And Sahara creates a new incident or a separately defined sequential evidence segment according to the configured design

@core
Scenario: Exported status does not remove the sealed incident
Given an incident is Sealed
When the user successfully exports the incident package
Then the incident is marked Exported
And the protected local incident remains available according to retention settings

@core
Scenario: User-visible incident states remain understandable
Given an incident changes lifecycle state
Then Sahara presents the relevant user-visible state as applicable
And Sahara does not represent a cancelled incident as an active emergency
And Sahara does not represent an unverified package as integrity-verified