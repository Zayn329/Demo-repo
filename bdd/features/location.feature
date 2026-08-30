@core @offline @real_device_required
Feature: Location Capture and Sharing

Sahara treats location as a user-controlled safety capability. Location
unavailability must not prevent incident handling, and live sharing must
stop when the user's configured incident controls require it.

Background:
Given Sahara is capable of handling an incident

@core
Scenario: Confirmed incident captures available location evidence
Given location access is available
When an incident is confirmed
Then Sahara captures location information according to the user's configured permissions
And location information can be included in the incident evidence package

@core
Scenario: Current location unavailable uses last-known location when available
Given an incident requires an alert
And current location cannot be obtained
And a usable last-known location exists
When Sahara prepares the alert
Then Sahara may include the last-known location
And Sahara records or communicates that the location is not necessarily current

@core
Scenario: Location unavailability does not block alert delivery
Given an incident requires an alert
And neither current nor usable last-known location is available
When Sahara sends the alert
Then Sahara sends the alert without location
And Sahara does not indefinitely wait for location availability

@core
Scenario: Live location starts only when pre-approved
Given an incident is confirmed
And the user has pre-configured live location sharing for the applicable incident type
When Sahara begins incident delivery
Then Sahara may begin live location updates according to the configured update policy

@core
Scenario: Live location is not silently enabled
Given an incident is confirmed
And live location sharing was not pre-approved
When Sahara processes the incident
Then Sahara does not automatically begin continuous live location sharing
And Sahara may still include separately permitted one-time location information

@core
Scenario: Cancelling an incident stops future live location updates
Given an incident is actively sharing live location
When the user cancels the incident
Then Sahara stops future live location updates immediately
And already captured or previously shared location remains part of the existing incident record according to retention rules

@core
Scenario: Revoking location permission stops future location capture
Given an active incident is collecting location updates
When the user revokes location permission
Then Sahara stops collecting future location updates as soon as the permission change is recognized
And already captured location evidence remains associated with the incident
And Sahara records that future location updates became unavailable

@core
Scenario: Location sharing status is visible
Given an incident is active
When Sahara is sharing or attempting to share location
Then the user can see the relevant location-sharing status
And Sahara does not represent unavailable location updates as active

@core @offline
Scenario: Location evidence can be retained without network connectivity
Given the device has no normal internet connectivity
And location access is available
When an incident captures location information
Then Sahara stores available location evidence locally
And normal network connectivity is not required for local location evidence retention