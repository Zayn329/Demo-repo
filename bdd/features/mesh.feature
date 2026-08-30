@core @offline @real_device_required @demo
Feature: Offline Mesh Relay

Sahara attempts local peer-to-peer relay for emergency packets when
normal network connectivity is unavailable or unreliable.

Background:
Given an incident requires delivery
And the primary device has no reliable normal network connectivity

@core @demo
Scenario: Emergency packet reaches an opted-in nearby Sahara device
Given another nearby device is running Sahara
And that device has previously opted in to relay participation
And a supported peer-to-peer connection is available
When Sahara attempts mesh delivery
Then the emergency packet can be transferred to the nearby opted-in device
And the primary device records successful mesh transport acceptance

@core
Scenario: Nearby users are never prompted to opt in during an emergency
Given a nearby Sahara user has not opted in to relay participation
When another user's emergency requires mesh delivery
Then Sahara does not interrupt the nearby user with an emergency opt-in request
And the non-opted-in device is not used as an automatic relay

@core
Scenario: Mesh failure does not block other delivery paths
Given mesh delivery is unavailable or fails
When another configured delivery path is available
Then Sahara continues according to the configured delivery fallback strategy
And local evidence protection continues independently

@core
Scenario: Mesh attempts do not block evidence sealing
Given mesh delivery is being attempted
When evidence capture reaches its sealing boundary
Then Sahara can continue evidence protection and sealing
And Sahara does not wait indefinitely for mesh delivery before protecting evidence

@core @fallback_allowed
Scenario: Packet reaching configured hop limits is handled safely
Given an emergency packet is being relayed
When the configured maximum hop count is reached
Then Sahara handles the packet according to the configured hop-limit policy
And Sahara does not create an unbounded relay loop
And the resulting delivery state is recorded

@core
Scenario: Successful mesh delivery does not falsely imply recipient acknowledgement
Given a mesh relay accepts an emergency packet
When the primary device records mesh success
Then Sahara reports successful mesh transport
And Sahara does not claim that a Notify Circle member acknowledged the emergency unless an acknowledgement is actually received