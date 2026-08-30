PRD.md

Sahara — Product Requirements Document

1. Product Overview

Working name: Sahara

One-line product description:

«An offline-first safety companion that detects distress on-device, quietly alerts the people you trust, and keeps a tamper-evident record — even when you have no signal.»

Sahara is an offline-first personal safety companion designed primarily around situations where a person may be unable to actively operate their phone or may not have reliable network connectivity.

The product focuses on four connected capabilities:

1. Detecting potential distress on the device.
2. Capturing and protecting evidence locally.
3. Attempting to reach trusted contacts even when normal connectivity is unavailable.
4. Helping the user organize information after an incident.

The product architecture is gender-inclusive. The initial hackathon problem framing and primary demonstration focus on women's safety in India.

Sahara is not designed to guarantee prevention of harm or replace emergency services. Its purpose is to reduce dependence on immediate user interaction and network availability while preserving useful, integrity-protected information during and after a potential incident.

---

2. Problem

Personal safety applications often assume two things that may not be true during an emergency:

- The user can actively operate their phone.
- A reliable internet or mobile network connection is available.

In a real distress situation, a person may be unable to unlock their phone, navigate an interface, press a panic button, make a call, or maintain connectivity.

The main problems Sahara addresses, in priority order, are:

Trusted contacts may not know something is wrong in time

A person may be in distress without being able to actively communicate. Existing safety workflows often depend on the person manually initiating every emergency action.

Network connectivity may be unavailable

Internet and mobile connectivity cannot be assumed in every location or situation. A safety system that depends entirely on cloud services may fail precisely when it is needed.

Evidence may be lost, deleted, or disputed

Information captured during an incident may later be difficult to organize or verify. Users need a way to preserve incident data locally and demonstrate that an exported package has not been silently altered.

A single panic button is not always enough

A panic button is useful but assumes the user can consciously access and operate their device. Safety support should not depend on a single interaction mechanism.

Reporting an incident afterward can be difficult

After an incident, organizing evidence, timelines, and relevant information can be stressful and intimidating.

---

3. Product Vision

Sahara aims to provide a safety layer that remains useful when the user has limited ability to interact with their phone and when connectivity is unreliable.

The central product promise is:

«Your safety data stays on your device unless you choose to share it.»

The product should remain calm rather than fear-based. It should avoid creating the impression that the user is constantly under surveillance or that an emergency is inevitable.

Sahara should feel quietly available in the background, with clear user control over whether monitoring is active.

The strongest product differentiator is offline resilience.

Sahara must be able to:

- Detect configured distress signals without relying on the cloud.
- Capture and preserve evidence locally.
- Protect evidence integrity on the device.
- Attempt nearby peer-to-peer relay when conventional connectivity is unavailable.
- Use available delivery mechanisms when connectivity becomes possible.

The product should prefer real local capability over cloud dependence.

---

4. Target Users

Primary User

The primary user for the first version is a woman in India who regularly moves around alone.

Typical situations include:

- Daily commuting.
- Returning home at night.
- Travelling alone.
- Moving through unfamiliar areas.
- Passing through areas with unreliable connectivity.

The product should be usable by people with minimal technical knowledge.

The initial geographic focus is India, with Mumbai as the primary demonstration location.

Secondary Users

Notify Circle Members

Trusted family members, friends, guardians, or other selected contacts who may receive safety notifications.

Their primary role in v1 is to:

- Receive a possible distress or emergency notification.
- View the information the primary user has authorized for sharing.
- Acknowledge that they have seen the alert.

Acknowledgement in v1 means:

«"I saw this."»

It does not automatically change the incident workflow.

A future version may support a separate response acknowledgement such as:

«"I am responding."»

Evidence Consumers

Police, lawyers, NGOs, or legal aid organizations may later receive user-exported incident packages.

These parties are not direct active users of the first version of the mobile product.

---

5. Product Principles

Offline First

Core detection and evidence capture must not depend on backend availability or internet connectivity.

User Control

The user controls whether monitoring is active and what information may be shared.

Calm by Default

The product should communicate safety status clearly without using unnecessarily alarming or fear-driven interactions.

Evidence Stays Local by Default

Raw evidence remains on the user's device unless the user explicitly authorizes sharing through an available product flow.

Integrity Over Claims

The product may describe evidence as integrity-protected or tamper-evident.

It must not guarantee legal admissibility or make unsupported claims about court acceptance.

Real Behavior Over Simulated Features

Core functionality should work genuinely during the hackathon demonstration. A visually convincing but non-functional safety feature is not acceptable as a completed product capability.

Contract Stability

The agent may create new internal APIs, schemas, and event contracts when required by a clearly defined architecture and feature specification. New contracts must be documented. Once consumed by another module, service, or external client, a contract is considered established and cannot be changed incompatibly without approval.

---

6. Core Product Experience

First-Time Setup

The first experience should be short and understandable:

Install → Value introduction → Permission explanation → Notify Circle setup → Detection preferences → Ready

Permissions must explain why they are requested.

The user may skip Notify Circle setup.

The core product remains usable without Notify Circle because local detection and evidence capture can still operate.

Setup is considered complete when:

- Required permissions for the selected features have been granted.
- The user has reached and understood the primary monitoring state.

The user should clearly see whether Safety Monitoring is:

- ON
- OFF
- Paused
- Degraded because a required capability is unavailable

Daily Use

Sahara should remain mostly quiet during normal use.

The user should not need to repeatedly open the application.

Monitoring begins only when the user explicitly enables it.

If the user has explicitly allowed it, monitoring may resume after a device reboot according to platform capabilities and product configuration.

The user must always have an obvious way to:

- Stop monitoring.
- Pause monitoring for privacy.
- Review what the application is currently capable of detecting or sharing.

---

7. Incident Experience

Possible Distress

When Sahara first detects a possible distress event, the product should:

- Begin or continue evidence capture.
- Provide discreet vibration feedback.
- Continue monitoring according to the configured confirmation rules.
- Show a short control or cancellation interface when the screen is available.

Evidence capture may begin before the incident is fully confirmed in order to preserve relevant context, including supported pre-roll behavior.

A possible distress state should not immediately be represented as a confirmed emergency unless the configured confirmation conditions are met.

Confirmed Incident

During the first moments of a confirmed incident, Sahara prioritizes actions in the following order:

1. Begin or continue evidence capture.
2. Show the user a cancellation and control interface.
3. Notify the Notify Circle.
4. Attempt nearby mesh relay.
5. Attempt SMS fallback.
6. Start authorized live-location sharing.
7. Seal the first evidence package.

Exact transport timing and concurrency are implementation concerns defined by the architecture, but this order represents product priority.

Cancellation

The user may cancel an automatically triggered incident.

Cancellation must require a clear confirmation action.

Cancelling an incident:

- Stops future emergency notifications associated with the active incident.
- Does not silently destroy captured evidence.
- Preserves evidence already captured according to the evidence retention policy.

Explicit Panic Activation

Explicit panic activation is intended to be faster and more direct than automatic detection.

The panic path may use a shorter confirmation period or no confirmation period according to the configured implementation.

The product must not interfere with device-level Emergency SOS functionality.

---

8. Distress Detection

The first version must support real on-device detection.

The minimum required automatic detection capability is configurable keyword detection.

Examples may include configured phrases such as:

- "Help"
- "Leave me alone"

Additional detection signals may include:

- Scream detection.
- Motion analysis.
- Configurable multi-signal confirmation.

The user may configure supported trigger preferences and sensitivity.

The product must clearly distinguish between:

- Available detection capability.
- Disabled detection capability.
- Degraded detection capability.

Sahara must not imply that automatic detection guarantees recognition of every emergency or distress situation.

---

9. Evidence Protection

Evidence protection is a major visible product feature.

The user should be able to understand that an incident has moved through states such as:

- Recording.
- Captured.
- Protected.
- Sealed.
- Exported.

The product should visibly communicate evidence integrity protection without overwhelming the user with cryptographic terminology.

For advanced users or exported evidence consumers, the system may expose:

- Integrity hashes.
- Signatures.
- Evidence manifest information.
- Verification results.

The user may review evidence after an incident.

For v1, the user should primarily manage evidence at the incident level.

Deleting an entire incident is supported according to retention and user-confirmation rules.

Fine-grained deletion of individual evidence items is not a primary v1 requirement.

Evidence should be described as:

«Integrity-protected and tamper-evident»

The product must not claim guaranteed legal admissibility.

---

10. Notify Circle

Notify Circle consists of trusted contacts selected by the primary user.

A user should be able to configure a small set of trusted contacts without complex setup.

During an incident, an authorized Notify Circle member may receive:

- Possible distress or emergency status.
- Approximate location.
- Timestamp.
- Incident status.
- An acknowledgement action.

Evidence integrity information may be exposed as an advanced capability.

Live location is shared only when the user has pre-configured authorization for that behavior.

The user must be able to review:

- Which contacts received an alert.
- What information was shared.
- Which delivery path was used.

Different permission levels for different Circle members are not required for v1 but may be introduced later.

---

11. Offline Delivery and Mesh

Offline resilience is a central Sahara product requirement.

When conventional connectivity is unavailable, the product should continue functioning locally.

Core local capabilities include:

- Distress detection.
- Evidence capture.
- Evidence protection.
- Local incident persistence.

When possible, Sahara should attempt nearby peer-to-peer delivery through opted-in participants.

Mesh networking should remain mostly invisible during ordinary use.

When relevant, the user should see a simple status such as:

- Relaying nearby.
- Alert delivered.
- Delivery pending.
- No delivery path currently available.

If no delivery mechanism is available, Sahara should clearly communicate that:

«Evidence is safely stored on the device and delivery can be attempted when a supported path becomes available.»

The user should never be misled into believing an alert was delivered when it was not.

---

12. Local Help Directory

Sahara should provide a lightweight local help directory.

The first version should focus on Mumbai and major Maharashtra cities, with a structure that can later expand across India.

The directory may include:

- Police contact information.
- Relevant emergency helplines.
- NGOs.
- Legal aid organizations.

The directory should remain useful offline.

The directory is primarily surfaced as support after or around an incident rather than replacing emergency services.

---

13. Post-Incident Legal Assistance

Legal assistance is a post-incident enhancement.

It should be available from a completed or sealed incident rather than interrupting the immediate safety workflow.

The feature may help the user:

- Organize incident information.
- Build a structured timeline.
- Summarize available evidence.
- Prepare an FIR-style complaint draft.
- Prepare other supported complaint drafts.

All generated legal content must be clearly labelled as:

«Draft — requires human and/or legal review before submission.»

The system must not automatically submit complaints or legal documents.

AI assistance should organize and draft information, not make legal guarantees.

---

14. Privacy and User Control

Sahara's central privacy promise is:

«Your safety data stays on your device unless you choose to share it.»

The product should provide meaningful transparency around data sharing.

The user must be able to understand:

- Whether monitoring is active.
- Which detection capabilities are active.
- Whether location sharing is enabled.
- Who received alerts.
- What information was shared.
- Which delivery or fallback path was used.

Live location may be automatically shared during a confirmed incident only when the user has explicitly pre-configured permission for that incident behavior.

Raw evidence must not leave the device automatically.

The user must have an obvious privacy pause or stop-monitoring control.

---

15. Product Scope

Must Have

The first hackathon version must prioritize:

- Clear monitoring ON/OFF state.
- Manual panic activation.
- Real on-device keyword detection.
- Evidence capture.
- Integrity protection.
- Evidence sealing and export.
- Notify Circle.
- SMS fallback capability.
- Basic nearby mesh relay.
- Calm incident control interface.
- Local Help Directory.

Should Have

Features that significantly improve the experience but may have controlled limitations include:

- Scream detection.
- Motion detection.
- Multi-signal confirmation.
- Live location updates.
- Real SMS delivery on supported physical devices.
- Post-incident legal drafting.
- Enhanced evidence verification views.

Could Have

If time permits:

- Advanced Notify Circle permissions.
- Advanced acknowledgement states.
- Additional evidence export formats.
- Optional blockchain anchoring.
- Optional privacy-preserving confirmation mechanisms.
- Encrypted evidence storage through optional external storage systems where explicitly authorized.

Future

Future versions may explore:

- Predictive safe routing.
- Risk-aware route recommendations.
- Broader offline map support.
- Additional geographic safety datasets.
- Expanded Indian help directories.
- More advanced Circle collaboration.
- Optional legal aid workflows.
- Additional detection models.

---

16. Explicitly Out of Scope for the Hackathon

The hackathon version does not attempt to implement:

- Predictive safe routing.
- Automated financial or escrow mechanisms.
- Direct live police integration.
- Heavy continuous video recording.
- A complex multi-role permission matrix.
- Guaranteed emergency response.
- Guaranteed court admissibility.
- Guaranteed detection in every Android background state.

These exclusions are intentional.

The product should prefer a smaller number of genuinely working capabilities over a large number of simulated features.

---

17. Core Demonstration Requirements

The following capabilities must be genuinely demonstrable and must not silently degrade into mocks:

- Panic activation.
- On-device keyword detection.
- Evidence audio capture.
- Evidence encryption.
- Keystore-backed signing where supported.
- Merkle integrity verification.
- Local evidence persistence.
- Notify Circle delivery through at least one genuine path.
- Nearby mesh relay between physical devices.

The following may use clearly disclosed controlled fallbacks during the hackathon:

- Scream detection.
- Motion detection.
- Advanced signal fusion.
- SMS provider integration.
- Legal LLM provider integration.
- Blockchain anchoring.

Any fallback must be explicitly visible in the implementation state and accurately reported.

---

18. Golden Demo Scenario

The primary demonstration should tell the following story.

A woman is walking through an area with poor or unavailable connectivity.

Sahara monitoring is active.

A configured distress keyword is detected.

Evidence capture begins with supported pre-roll context.

The incident is confirmed either by the user or through the configured confirmation rules.

Sahara begins the incident workflow.

A nearby opted-in device participates in relaying the compact alert.

A Notify Circle member receives a quiet alert containing authorized information, including approximate location where available.

Evidence is protected and sealed locally on the primary device.

Later, when connectivity is available, the user can review and export the evidence package.

Optionally, the user can request an AI-assisted incident summary or legal draft from the sealed incident.

The primary emphasis of the demonstration is:

1. Offline resilience and nearby mesh relay.
2. Evidence integrity.
3. On-device detection.
4. Calm user experience.

---

19. Success Criteria

Success should be evaluated through both product and technical outcomes.

Product Success

The hackathon build is successful if it demonstrates:

- A genuinely working detection-to-incident path on a real device.
- Evidence capture and local sealing.
- A functioning path from incident to Notify Circle.
- Calm and understandable safety controls.
- Honest communication about limitations and degraded states.
- Minimal user actions during the primary emergency path.

Initial product targets include:

- Basic setup in approximately two minutes or less.
- Clear monitoring status without requiring technical understanding.
- A user can understand whether an alert was delivered or remains pending.
- Evidence remains accessible after expected application lifecycle interruptions.

Technical Success

The demonstration should show:

- Real on-device keyword detection.
- Evidence protection and verification.
- Successful local persistence.
- Successful integrity verification after export.
- Mesh communication between two physical devices in the same room.
- Evidence sealing within an implementation-defined short and reasonable period.
- At least one genuine Notify Circle delivery path.

Exact performance thresholds should be measured and documented during implementation rather than invented without device testing.

---

20. Non-Guarantees and Responsible Product Claims

Sahara must not claim that it:

- Prevents harm.
- Guarantees emergency response.
- Guarantees police involvement.
- Guarantees network availability.
- Guarantees background detection in every Android state.
- Guarantees recognition of every distress event.
- Guarantees legal admissibility of exported evidence.

Sahara is not a replacement for emergency services.

Users should contact local emergency services when it is safe and possible to do so.

AI-generated incident summaries and legal drafts require human review and, where appropriate, legal review before use or submission.

---

21. Product Decision Summary

Sahara is an offline-first safety companion built around a simple principle:

«Safety support should not disappear simply because the user cannot operate their phone or because the network disappears.»

For the first version, the product prioritizes a real, demonstrable path from on-device distress detection to protected evidence and trusted-contact notification.

The system remains local-first, user-controlled, and honest about its limitations.

The hackathon version should demonstrate fewer capabilities deeply and genuinely rather than presenting a large number of features that cannot operate outside a scripted demo.

---

22. Relationship to the Repository Harness

This document defines the product requirements and user-facing intent.

"architecture.yaml" remains the authority for technical architecture, system boundaries, components, data flows, and implementation constraints.

"AGENTS.md" defines how coding agents must reason, implement, test, review, document, and report changes.

BDD specifications define the expected behavior of the most important product flows.

When a product requirement requires a technical decision not already covered by "architecture.yaml", the decision must be proposed and resolved according to the repository's agent governance rules rather than silently modifying the architecture.