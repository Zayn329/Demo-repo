Notify Circle API Contract

Purpose

Notify Circle backend functionality complements local safety delivery paths.

The core incident workflow must not depend on backend delivery because Sahara may operate without network connectivity.

Device-side SMS and nearby mesh remain independent of this API.

Circle membership

The authenticated user's Circle can be managed through:

GET /api/v1/notify/circle
PUT /api/v1/notify/circle

The hackathon target supports up to five configured members.

Membership records are lightweight and should contain only information necessary to support the configured delivery channel.

Phone numbers and other contact identifiers are sensitive data and must not appear in ordinary logs.

Backend notification fan-out

The primary backend path is:

POST /api/v1/notifications/incident-alert

The intended flow is:

Android device
    →
Sahara FastAPI backend
    →
FCM
    →
Notify Circle application device

The backend records delivery attempts and may provide partial delivery status.

Acceptance by the backend does not guarantee that the recipient has seen the alert.

Alert information

An alert may contain:

- Incident identifier.
- Incident status.
- Timestamp.
- Approved approximate location.
- Optional integrity reference.
- A concise status message.

Exact evidence coordinates must not be sent through this endpoint unless a future explicitly approved contract adds such behavior.

Incident statuses

The v1 backend notification contract supports:

possible_distress
emergency_confirmed
incident_cancelled
evidence_sealed

These statuses are notification events and must not be treated as authority to modify the device incident state machine.

The device remains authoritative for the actual incident state.

Acknowledgements

Notify Circle members can record acknowledgement through:

POST /api/v1/notifications/acknowledgements

An acknowledgement means only:

«I saw this.»

It does not automatically:

- Cancel an incident.
- Stop evidence capture.
- Stop SMS.
- Change the incident state.
- Release evidence.

Acknowledgements are recorded for transparency.

Offline behavior

If the backend is unavailable:

- The app records the failure locally.
- Local SMS and mesh behavior continues independently.
- The event may later synchronize if synchronization is enabled.

The application must not hide backend delivery failure.

Delivery semantics

Backend acceptance means the server accepted responsibility for the dispatch attempt.

It does not mean:

- The recipient opened the notification.
- The recipient is physically responding.
- The recipient has acknowledged the incident.

These states must remain distinct in user-visible activity history.