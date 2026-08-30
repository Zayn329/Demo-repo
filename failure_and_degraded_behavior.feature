@core
Feature: Safety-Critical Failure and Degraded Behavior

Sahara must fail honestly and preserve available safety capabilities.
A failure of connectivity, hardware, or an external service must not
silently weaken evidence integrity or misrepresent system behavior.

@core @offline
Scenario: No network does not stop local detection and evidence handling
Given the device has no normal internet connectivity
When a qualifying distress event occurs
Then Sahara can perform available on-device detection
And Sahara can create and protect the incident locally
And Sahara does not wait for network connectivity before starting local safety actions

@core
Scenario: Missing Notify Circle does not block an incident
Given the user has no Notify Circle configured
When an incident is confirmed
Then Sahara continues local evidence and incident handling
And Sahara clearly records that contact delivery was not configured where relevant

@core @fallback_allowed
Scenario: Microphone permission denial produces feature-level degradation
Given microphone permission is denied
When the user enables Safety Monitoring
Then microphone-based automatic detection is unavailable
And Sahara remains usable for supported manual safety features
And Sahara clearly explains the unavailable capability
And Sahara provides a route to device settings when appropriate
And Sahara does not repeatedly nag after permanent denial

@core
Scenario: Location unavailability does not block emergency delivery
Given an incident requires an alert
And current location is unavailable
When Sahara prepares an emergency notification
Then Sahara uses last-known location with its age when available
Or sends the alert without location when no usable location exists
And Sahara does not block the alert indefinitely waiting for location

@core @offline
Scenario: Mesh unavailability does not stop the incident workflow
Given mesh delivery is unavailable
When an incident requires notification
Then Sahara records mesh as unavailable or failed
And Sahara continues with other configured delivery paths
And local evidence protection continues

@core @fallback_allowed
Scenario: SMS unavailability is reported honestly
Given the device cannot send SMS through the configured path
When Sahara reaches the SMS delivery stage
Then Sahara marks SMS delivery as unavailable or failed
And Sahara visibly reports failure or delivery pending
And Sahara continues other available paths
And Sahara does not claim that an SMS was sent

@core @fallback_allowed
Scenario: Backend unavailability does not block offline core safety behavior
Given the backend is unavailable
When a local incident occurs
Then Sahara continues local detection, incident handling, and evidence protection
And backend-dependent features clearly report degraded or unavailable status
And Sahara does not silently substitute a successful backend result

@core @protected @real_device_required
Scenario: Application restart during active incident preserves the incident
Given an Active Incident exists
When the application is interrupted or restarted
Then Sahara marks the incident as interrupted
And preserves available incident data
And attempts required protected sealing or recovery

@core @protected
Scenario: Integrity verification failure is visible
Given a sealed evidence package exists
When integrity verification fails
Then Sahara clearly reports verification failure
And Sahara does not claim that the package is integrity verified
And verified export claims are blocked

@core @protected
Scenario: Critically low storage stops unsafe continuation
Given an incident is actively capturing evidence
When storage reaches the configured critical threshold
Then Sahara stops further capture
And attempts to seal existing evidence
And records the storage interruption

@core @protected
Scenario: Encryption failure prevents insecure downgrade
Given an incident requires evidence encryption
When encryption fails
Then Sahara enters a protected error state
And Sahara does not intentionally store new evidence as plaintext
And the user is informed of the protected failure

@core @protected
Scenario: Android Keystore failure prevents fake signing
Given an incident requires evidence signing
When the required Keystore operation fails
Then Sahara does not generate a fake or substitute signature
And Sahara enters a protected error state
And the signing failure is recorded

@core
Scenario: Degraded state never reports unavailable capabilities as active
Given one or more Sahara capabilities are unavailable
When the user views the safety status
Then Sahara clearly identifies the degraded capability
And Sahara does not represent the unavailable capability as operational
And available independent capabilities remain usable where technically possible