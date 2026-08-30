@core @offline @protected @real_device_required
Feature: Distress Detection

Sahara performs automatic distress detection locally without requiring
normal network connectivity. Detection must be resistant to isolated
noisy signals and must not rely on an LLM for safety triggering.

Background:
Given Safety Monitoring is active
And the relevant detector is available

@core @demo
Scenario: Repeated configured keyword detections create possible distress
Given a configured distress keyword is detected repeatedly or sustained above the configured threshold
When Sahara determines that the configured keyword detection rule is satisfied
Then Sahara starts an evidence capture candidate
And Sahara enters the Possible Distress state
And Sahara begins evaluating the configured confirmation rule
And normal internet connectivity is not required

@core
Scenario: A single noisy detection does not create an incident
Given an isolated detector spike does not satisfy the configured detection rule
When no additional qualifying detection occurs
Then Sahara does not confirm an incident
And Sahara remains in or returns to the Monitoring state

@core @protected
Scenario: Possible distress expires without confirmation
Given Sahara is in the Possible Distress state
And evidence capture is active for the candidate
When the configured confirmation window expires without satisfying the confirmation rule
Then the incident candidate expires
And the candidate evidence is discarded
And Sahara returns to Monitoring
And Sahara does not notify the Notify Circle for the expired candidate

@core @protected
Scenario: A strong second signal confirms possible distress
Given Sahara is in the Possible Distress state
And the confirmation window is active
When a valid second signal satisfies the configured confirmation rule
Then Sahara confirms the incident without waiting for an unnecessary additional delay
And the incident enters the Active Incident state

@core @protected @fallback_allowed
Scenario: Signal fusion follows the configured rule
Given Sahara is in the Possible Distress state
And the configured confirmation rule allows multiple signal combinations
When qualifying signals occur within the configured confirmation window
Then Sahara evaluates the signals according to the configured rule
And Sahara confirms the incident only when the configured rule is satisfied

@core @offline
Scenario: Detection continues without network connectivity
Given the device has no normal internet connectivity
When a configured distress detection rule is satisfied
Then Sahara performs local detection
And Sahara can enter Possible Distress
And Sahara does not wait for a backend response before evaluating the incident