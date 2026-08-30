@core @fallback_allowed @real_device_required
Feature: SMS Emergency Fallback

Sahara uses SMS as an emergency delivery fallback when supported and
configured. SMS delivery must be represented honestly and must not block
local evidence protection.

Background:
Given an incident has been confirmed
And at least one eligible Notify Circle member can receive SMS

@core
Scenario: SMS is attempted when configured fallback conditions are reached
Given the configured primary delivery path is unavailable or unsuccessful
And SMS capability is available
When Sahara reaches the configured SMS fallback point
Then Sahara attempts SMS delivery without blocking local evidence protection

@core
Scenario: Emergency SMS contains the minimum required information
Given Sahara prepares an emergency SMS
When the SMS is sent
Then the message contains a concise emergency alert
And includes an approximate location when available
And includes a timestamp
And includes a simple incident identifier or reference code

@core
Scenario: Emergency SMS uses last-known location when current location is unavailable
Given current location is unavailable
And a usable last-known location exists
When Sahara prepares an emergency SMS
Then Sahara includes the last-known location
And includes or preserves the age of that location where supported
And Sahara does not represent it as a guaranteed current location

@core
Scenario: Emergency SMS can be sent without location
Given no current or usable last-known location exists
When Sahara prepares an emergency SMS
Then Sahara sends the alert without location when SMS delivery is otherwise available
And Sahara does not indefinitely delay the emergency alert waiting for location

@core
Scenario: SMS transport acceptance is recorded as v1 delivery
Given Sahara submits an emergency SMS through a supported delivery mechanism
When the mechanism confirms that the SMS was accepted for sending
Then Sahara records SMS transport delivery according to the v1 delivery definition
And Sahara does not claim recipient acknowledgement unless acknowledgement is actually received

@core
Scenario: SMS failure is visible to the user
Given Sahara attempts to send an emergency SMS
When the SMS delivery attempt fails
Then Sahara records the failed delivery attempt
And Sahara shows failure or delivery pending in the incident status
And Sahara does not claim that the SMS was delivered

@core
Scenario: Missing SMS capability is handled as a degraded delivery path
Given the device cannot send SMS through any supported configured path
When an incident requires emergency delivery
Then Sahara marks SMS as unavailable
And Sahara continues with other available delivery paths
And local evidence protection continues

@core @fallback_allowed
Scenario: A demo-only SMS transport is clearly identified
Given Sahara is running in an explicitly permitted development or demo environment
And a clearly labelled SMS mock transport is enabled
When Sahara sends an emergency SMS through the mock transport
Then the delivery result is clearly identified as simulated
And the completion report states "PASSED USING FALLBACK"
And the mock transport cannot silently operate in a release build

@core
Scenario: One failed SMS recipient does not block other recipients
Given multiple Notify Circle members are eligible for SMS delivery
When delivery to one recipient fails
Then Sahara continues attempting delivery to other eligible recipients
And the incident records the resulting partial delivery state