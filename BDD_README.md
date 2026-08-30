Sahara BDD Specification

This directory contains the Behavior-Driven Development (BDD) specifications for Sahara's hackathon-critical product behavior.

These scenarios define the expected observable behavior of the product. They are derived from the PRD and must remain consistent with "architecture.yaml". They intentionally describe what the system must do, rather than prescribing internal classes, APIs, databases, or implementation details.

"architecture.yaml" remains the highest technical authority for architecture, module boundaries, technology choices, security boundaries, and protected modules. Where a BDD scenario and "architecture.yaml" conflict, implementation must stop and the conflict must be reported rather than silently resolved.

Purpose

The BDD suite exists to provide a stable behavioral contract for:

- Coding agents implementing Sahara.
- Automated test development.
- Manual real-device verification.
- Hackathon demo readiness.
- Review of safety-critical behavior.

The scenarios in this directory are normative. A coding agent may choose reasonable implementation details when they are not specified, but may not silently change behavior defined by a scenario.

Scope

The initial BDD suite focuses on the hackathon-critical safety path:

Monitoring → Distress Detection → Incident → Evidence Protection → Delivery Attempt → Recovery or Sealing.

Future or optional functionality should be represented separately and tagged "@future" where necessary.

Tags

"@core"

Required for Sahara to be considered demo-ready.

"@protected"

Touches a protected safety or security module and requires the additional review and testing rules defined in "AGENTS.md".

"@offline"

The scenario must remain functional without normal internet connectivity.

"@demo"

Part of the intended hackathon demonstration.

"@real_device_required"

Full validation requires documented real-device or hardware verification. Emulator-only validation is insufficient.

"@emulator_allowed"

The scenario may be validated through automated tests or an emulator where appropriate.

"@fallback_allowed"

An explicitly permitted degraded or fallback implementation may satisfy the scenario. If used, the implementation report must explicitly state:

PASSED USING FALLBACK

Core Behavioral Principles

Local safety behavior must not depend on connectivity

Loss of internet connectivity, backend access, mesh availability, SMS capability, or Notify Circle configuration must not prevent Sahara from performing independent local safety actions that remain technically available.

No insecure downgrade

Sahara must never silently downgrade evidence security.

If required encryption, Keystore access, signing, or integrity protection fails, the system must enter a protected error state rather than intentionally storing new evidence insecurely.

Cancellation does not silently destroy captured evidence

Once evidence has been intentionally captured for a panic event or confirmed incident, cancellation may stop future actions but must not silently destroy evidence that the product behavior requires to be preserved.

Capability degradation should be isolated

Loss of one capability must not unnecessarily disable unrelated capabilities.

For example, microphone denial may disable automatic keyword detection while manual panic remains available.

Delivery status must be honest

Sahara must not claim that an alert was delivered unless the product's defined delivery condition has been reached.

For v1, transport acceptance is sufficient to record delivery. Recipient acknowledgement is a separate state and must not be represented as a delivery requirement.

Protected-module verification

Scenarios tagged "@protected" require:

- Automated tests where technically possible.
- Updated tests when behavior changes.
- Test execution.
- A documented real-device/manual verification when hardware behavior cannot be fully automated.
- Final security/privacy review according to "AGENTS.md".

Scenario Interpretation

BDD scenarios define behavioral outcomes.

When a scenario permits configurable behavior, implementation may use a reasonable configuration provided that:

- It remains consistent with "architecture.yaml".
- It remains consistent with the PRD.
- It does not weaken a protected security boundary.
- The chosen behavior is documented.

Exact timings, thresholds, transport internals, worker implementations, API schemas, and class structures remain implementation decisions unless explicitly constrained by a scenario or specification.

Demo Readiness

The "@core" scenarios represent the behavioral contract for hackathon demo readiness.

A demo is not considered ready merely because the UI appears functional. Core scenarios must be implemented with the required real or explicitly allowed fallback behavior.

Where a fallback is used for a scenario tagged "@fallback_allowed", the completion report must explicitly state:

PASSED USING FALLBACK

The fallback must be visible, documented, and must not silently replace a required real implementation.

Relationship to Other Harness Files

The intended hierarchy is:

PRD.md
Product requirements and user outcomes.

BDD specifications
Observable behavioral contracts.

Focused specifications
Precise domain rules where behavior requires additional implementation detail.

architecture.yaml
Technical architecture, module boundaries, security boundaries, and implementation constraints.

AGENTS.md
Rules governing coding-agent behavior and implementation discipline.

Tests
Executable verification of the required behavior.

A coding agent must read the relevant BDD scenarios before implementing a feature and must not silently modify these scenarios to make an incomplete implementation pass.