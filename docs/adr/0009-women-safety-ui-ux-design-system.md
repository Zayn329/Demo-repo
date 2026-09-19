# 0009. Women's Personal Safety Companion UI/UX Design System

## Status
Accepted

## Context
Emergency and safety applications often default to intimidating, high-stress visual metaphors: dark interfaces, cybersecurity graphs, pulsing sirens, giant red SOS buttons, and surveillance tracking terminology. For a women's personal safety companion operating in stressful moments, this induces cognitive overload and makes the user feel monitored rather than supported.

The application required a design language that feels like a premium wellness and personal care mobile application, conveying calm, care, privacy, and confidence ("You're not alone.") while maintaining deterministic offline-first execution.

## Decision
1. **Color Palette & Visual Tokens**:
   - Primary: Brand Pink (`#F34B86`), Soft Pink (`#FDE7EF`), Very Soft Pink (`#FFF4F8`).
   - Secondary Accents: Soft Blue (`#6F9BEF`), Soft Yellow (`#F7C94B`).
   - Surfaces: Warm White (`#FFFCFA`), Pure White (`#FFFFFF`).
   - Typography & Hierarchy: Deep Navy/Charcoal (`#1B1D2A`) for high readability, Muted Slate (`#6C727F`) for secondary guidance.
   - Status Semantics: Muted Sage Green (`#4E9F76`), Warm Coral (`#E05353`) reserved exclusively for confirmed danger.
2. **Component Architecture**:
   - Reusable Jetpack Compose components (`SaharaTheme`, `SaharaPrimaryButton`, `SaharaSecondaryButton`, `SaharaStatusBadge`, `SaharaToggleCard`, `SaharaContactCard`, `SaharaSafetyStatusCard`, `SaharaTimelineItem`, `SaharaHoldToActivateButton`, `SaharaBottomNav`, `BreathingSafetyVisual`).
   - Large rounded cards (20–28px) with subtle ambient shadows instead of sharp borders.
   - Press-and-hold gestures with animated circular progress feedback to prevent accidental triggers while remaining quickly accessible.
3. **Cohesive 11-Screen Journey**:
   - Screen 1: Welcome & Value Proposition
   - Screen 2: Permissions & Consent with plain-language rationales
   - Screen 3: Notify Circle Setup (1–3 trusted contacts, location privacy guarantee)
   - Screen 4: Quick Preferences (Always-on Agent, Check-ins, Motion Assist)
   - Screen 5: Home Dashboard (Star screen with central breathing calming pearl)
   - Screen 6: Safety Watch (Proactive attentive monitoring prior to confirmed incident)
   - Screen 7: Active Incident (Autonomous evidence capture and multi-channel delivery)
   - Screen 8: Incident Sealed (Closure with abstract soft vault sealing feedback)
   - Screen 9: Incident Record / Timeline (Chronological timeline with Merkle proof integrity)
   - Screen 10: Trusted Contact Incident Alert (Receiving party actionable view)
   - Screen 11: Notify Circle Management

## Consequences
- The user interface is approachable, accessible, and reassuring.
- Critical safety actions remain fully functional and autonomous without relying on cloud backends.
- All 11 screens are realized in native Jetpack Compose code (`org.sahara.app.ui`) and as an interactive high-fidelity prototype artifact for preview.
