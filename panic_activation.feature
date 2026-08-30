@core @protected @real_device_required @demo
Feature: Panic Activation

Sahara provides a direct manual panic path that remains available
independently of automatic distress detection where technically possible.

Background:
Given Sahara is available to the user

@core @demo
Scenario: In-app panic activation bypasses automatic confirmation rules
Given the user can access the panic control
When the user intentionally activates Panic
Then Sahara immediately begins the panic incident flow
And Sahara does not require the normal multi-signal detection rule
And Sahara begins required evidence capture
And Sahara presents the configured cancellation countdown before notifications where applicable

@core
Scenario: User cancels panic during the cancellation countdown
Given a panic incident has been activated
And notifications have not yet started
When the user clearly confirms that they are safe
Then Sahara stops the pending notification escalation
And Sahara preserves the evidence captured during the panic activation
And the incident is marked as cancelled
And the preserved incident follows the required sealing process

@core
Scenario: User cancels after notification delivery has started
Given a panic incident is active
And at least one notification delivery attempt has started
When the user clearly confirms that they are safe
Then Sahara stops future alert escalation where technically possible
And Sahara stops future evidence capture according to the incident rule
And already captured evidence remains preserved
And the incident is marked as cancelled
And the incident is eventually sealed

@core @real_device_required
Scenario: Physical panic gesture activates the panic flow when available
Given the user has configured an available physical panic gesture
When the user performs the configured gesture correctly
Then Sahara starts the panic incident flow
And the gesture follows the same protected incident rules as the in-app panic control

@core @fallback_allowed
Scenario: Unavailable physical panic gesture is disclosed during setup
Given the device or Android configuration does not support the configured physical panic gesture
When the user configures panic activation
Then Sahara clearly identifies the physical gesture as unavailable
And Sahara keeps the in-app panic control available
And Sahara may offer another supported gesture when available

@core
Scenario: Panic remains available when microphone detection is unavailable
Given microphone-based automatic detection is unavailable
When the user activates Panic
Then Sahara starts the panic incident flow without requiring microphone access