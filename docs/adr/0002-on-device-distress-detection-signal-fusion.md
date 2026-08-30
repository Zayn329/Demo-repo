# 2. On-Device Deterministic Distress Detection & Signal Fusion Engine

* Status: accepted
* Date: 2026-08-30

## Context
Sahara requires real, deterministic on-device distress detection without internet connectivity or LLMs. Detection signals include keyword spotting, audio scream detection, and motion anomalies. Signals must be combined deterministically to avoid false positives and isolated noise spikes.

## Decision
We implement an on-device detection engine in `:android:services:detection`:
- `KeywordDetector`: Analyzes audio PCM buffers against configured keyword threshold.
- `ScreamDetector`: Evaluates audio zero-crossing rates (ZCR) and peak amplitude ratios.
- `MotionDetector`: Processes accelerometer G-force calculations for impact anomalies.
- `SignalFusionEngine`: Evaluates incoming signals against configurable combination rules (`KEYWORD OR (SCREAM AND MOTION)`).
- Incident Candidates expire after the configurable confirmation window (default 6 seconds) unless a valid confirming signal is received.

## Consequences
- Core detection executes deterministically on-device without cloud dependencies.
- Signals are clean and isolated, avoiding false positive escalations.
- Strict adherence to `architecture.yaml` and `bdd/features/monitoring.feature`.
