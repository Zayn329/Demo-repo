@core @offline @demo
Feature: Notify Circle Delivery

Sahara attempts to notify configured trusted contacts without making
local incident protection dependent on successful delivery.

Background:
Given an incident has been confirmed

@core
Scenario: Confirmed incident proceeds without a Notify Circle
Given the user has not configured any Notify Circle members
When an incident is confirmed
Then Sahara continues evidence protection and incident handling
And Sahara does not fail the incident because no contacts exist
And contact delivery attempts are skipped

@core @demo
Scenario: All eligible Notify Circle members are attempted independently
Given multiple Notify Circle members are configured
And different members may support different delivery paths
When an incident requires notification
Then Sahara independently attempts each eligible configured delivery path
And failure for one member does not prevent attempts for another eligible member

@core
Scenario: Transport acceptance records successful v1 delivery
Given Sahara sends an alert through an available supported transport
When the transport accepts the alert for delivery
Then Sahara records the alert as delivered for the v1 delivery definition
And Sahara does not require recipient acknowledgement to record transport delivery

@core
Scenario: Recipient acknowledgement is recorded separately
Given a Notify Circle member receives an alert
When the member acknowledges the alert
Then Sahara records the acknowledgement
And acknowledgement does not automatically cancel or otherwise alter the incident workflow

@core
Scenario: Partial delivery is visible
Given an incident notification is attempted for multiple members
And at least one delivery succeeds
And at least one delivery fails or remains pending
Then Sahara records partial delivery
And the incident activity view clearly distinguishes successful and unsuccessful paths

@core
Scenario: Alert delivery reports only information permitted by the user
Given an incident requires Notify Circle delivery
When Sahara prepares an alert
Then Sahara includes only information permitted by the user's configured sharing preferences
And Sahara does not silently enable live location sharing

@core
Scenario: Delivery failure does not invalidate local evidence
Given one or more Notify Circle delivery attempts fail
When local evidence protection remains available
Then Sahara preserves and protects the local incident evidence
And the delivery failure is recorded visibly