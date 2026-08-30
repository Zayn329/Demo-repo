@core @protected @offline @real_device_required @demo
Feature: Evidence Capture and Integrity Protection

Sahara captures safety evidence locally and protects its integrity using
encryption, hashing, signing, and a finalized Merkle root.

Background:
Given Sahara is capable of creating an incident
And required secure evidence capabilities are available

@core @demo
Scenario: Evidence capture begins when a qualifying distress event occurs
Given Safety Monitoring is active
And temporary pre-roll evidence is available
When Sahara enters a qualifying Possible Distress or Panic flow
Then Sahara preserves the configured recent pre-roll evidence for the incident candidate where applicable
And Sahara continues evidence capture according to the incident rules

@core
Scenario: Confirmed incident captures evidence without network connectivity
Given a confirmed incident is active
And the device has no normal internet connectivity
When evidence capture continues
Then Sahara stores and protects the evidence locally
And Sahara does not require backend availability to preserve the incident

@core
Scenario: Evidence capture stops at configured boundaries
Given a confirmed incident is actively capturing evidence
When the configured maximum duration is reached
Then Sahara stops further capture
And the captured evidence proceeds toward sealing

@core
Scenario: User can stop active evidence capture where the incident controls allow it
Given a confirmed incident is actively capturing evidence
When the user uses the available stop capture control
Then Sahara stops future evidence capture
And already captured evidence remains protected
And the incident continues toward sealing

@core @protected
Scenario: Critically low storage protects existing evidence
Given an active incident is capturing evidence
When available storage reaches the configured critical threshold
Then Sahara stops further evidence capture
And Sahara attempts to protect and seal evidence already captured
And Sahara clearly records that capture was interrupted due to low storage

@core @protected
Scenario: Sealing creates a complete integrity-protected package
Given evidence capture for an incident is complete
When Sahara seals the incident
Then all evidence is encrypted at rest
And required evidence hashes are generated
And required signatures are generated using the configured Keystore-backed design
And the final Merkle root is generated
And the incident is marked Sealed only after the required sealing operations succeed

@core @protected
Scenario: Encryption or signing failure does not cause insecure evidence storage
Given an incident is actively capturing or protecting evidence
When required encryption, Keystore access, or signing fails
Then Sahara enters a protected error state
And Sahara does not intentionally continue storing new evidence through an insecure downgrade
And the failure is visible to the user
And the failure is recorded in the incident activity history where technically possible

@core @protected
Scenario: Integrity verification detects a modified package
Given a sealed evidence package exists
When integrity verification detects that required evidence integrity does not match
Then Sahara clearly reports that verification failed
And Sahara does not represent the package as verified
And Sahara blocks verified-integrity claims for the package

@core
Scenario: Export remains possible after verification failure with a strong warning
Given an evidence package failed integrity verification
When the user chooses to export it
Then Sahara allows export only with a clear warning that integrity verification failed
And Sahara does not label the export as verified