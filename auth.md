Authentication API Contract

Purpose

Authentication is required only for Sahara features that depend on the backend.

The core offline safety application must remain usable without authentication. A user must be able to enable monitoring, trigger panic activation, capture evidence, seal an incident, and use local delivery mechanisms without creating or authenticating a backend account.

Phone OTP is the primary backend authentication mechanism.

Provider abstraction

The Android application communicates only with Sahara API endpoints:

POST /api/v1/auth/request-otp
POST /api/v1/auth/verify-otp
POST /api/v1/auth/refresh
POST /api/v1/auth/logout

The backend owns integration with the underlying OTP provider.

Android code must not become coupled to Firebase, Twilio, or another provider's client API merely to authenticate with Sahara.

Changing OTP providers must not require changing the public Android-to-Sahara API contract.

Session model

Successful OTP verification returns:

- A JWT access token.
- A refresh token.
- The access token expiry duration.

The access token is sent using the HTTP "Authorization" header.

Refresh tokens must never be placed into ordinary logs, analytics events, crash reports, or URLs.

Android clients must use platform-protected storage for credentials. Session storage must be encrypted or protected by Android security facilities appropriate to the supported API range.

OTP request

"POST /api/v1/auth/request-otp" does not require prior authentication.

The backend may rate-limit requests to prevent abuse.

The client must not assume that repeated OTP requests are independent. Provider and backend policies may invalidate previous OTPs.

Phone numbers are sensitive data and must not appear in ordinary backend logs.

OTP verification

"POST /api/v1/auth/verify-otp" exchanges a valid request identifier and OTP for a session.

Invalid, expired, or exhausted OTP attempts return explicit authentication errors.

The application must never log the OTP.

Refresh

"POST /api/v1/auth/refresh" accepts a refresh token and returns a new token pair according to the backend session policy.

If refresh fails, backend-dependent functionality becomes unavailable until authentication is restored. The application must not disable offline safety functionality because backend authentication expires.

Logout

Logout revokes or invalidates the backend session according to the provider-independent backend session model.

Logging out removes backend access. It does not delete local incidents, evidence, cryptographic keys, or offline Notify Circle configuration.

Development authentication

Local development authentication may exist only when explicitly enabled by the development environment.

A development bypass must:

- Be impossible to activate accidentally in a release/demo backend.
- Be visibly identifiable in logs and developer-facing diagnostics.
- Not silently impersonate production authentication.

The public contract remains unchanged even when a local development authentication adapter is used.