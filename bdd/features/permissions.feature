@core
Feature: Permissions and Capability Degradation

Sahara requests permissions in the context of the safety capabilities they
enable. Denial of one permission must not unnecessarily disable unrelated
safety capabilities.

Background:
Given Sahara is installed and opened by the user

@core
Scenario: Permission requests explain their safety purpose
Given the user has not yet granted a required feature permission
When Sahara requests the permission
Then Sahara explains the capability that requires the permission
And Sahara does not describe the permission as mandatory for unrelated features

@core
Scenario: User can complete basic setup without every optional capability
Given one or more optional safety capabilities require permissions
When the user declines an optional permission
Then Sahara allows setup to continue with available capabilities
And Sahara clearly identifies unavailable features
And Sahara does not falsely report full capability availability

@core
Scenario: Microphone denial disables only microphone-dependent detection
Given microphone permission is denied
When the user views Safety Monitoring
Then Sahara identifies microphone-based automatic detection as unavailable
And manual panic remains available where supported
And other independent safety capabilities remain available

@core
Scenario: Location denial does not disable evidence capture
Given location permission is unavailable
When a distress incident occurs
Then Sahara can capture available non-location evidence
And Sahara does not block the incident waiting for location permission

@core @fallback_allowed
Scenario: SMS permission or capability denial does not block other delivery paths
Given SMS delivery is unavailable because required capability or permission is unavailable
When an incident requires notification
Then Sahara marks the SMS path as unavailable
And Sahara continues other configured delivery paths
And Sahara does not report an SMS as sent

@core
Scenario: Permanent permission denial does not cause repeated nagging
Given the user has permanently denied a permission
When the user continues using Sahara
Then Sahara does not repeatedly interrupt the user with the same system permission request
And Sahara clearly shows the affected capability as unavailable
And Sahara provides a route to device settings when the user chooses to restore the capability

@core
Scenario: Restoring permission restores the related capability
Given a feature was unavailable because its required permission was denied
When the user grants the required permission through device settings
And Sahara refreshes its capability state
Then Sahara can make the related feature available again
And Sahara does not require unrelated features to be reconfigured unnecessarily

@core
Scenario: Permission state is reflected honestly in monitoring status
Given one or more configured monitoring capabilities lack required permissions
When the user enables Safety Monitoring
Then Sahara enters degraded monitoring when independent capabilities remain available
And the unavailable capabilities are clearly identified
And Sahara does not report degraded monitoring as fully active

@core
Scenario: Manual panic remains available with minimal permissions
Given automatic detection permissions are unavailable
When the user activates an available manual panic control
Then Sahara begins the panic flow using all capabilities that remain available
And Sahara does not require unrelated automatic detection permissions to activate Panic