package org.sahara.core.domain.models

import java.util.UUID

enum class IncidentState {
    IDLE,
    MONITORING,
    SUSPICIOUS_SIGNAL,
    CANDIDATE_INCIDENT,
    PENDING_CONFIRMATION,
    ACTIVE_INCIDENT,
    CANCELLED,
    SEALED,
    EXPORT_PENDING,
    EXPORTED,
    ARCHIVED
}

data class Incident(
    val incidentId: UUID = UUID.randomUUID(),
    val state: IncidentState,
    val createdAt: Long = System.currentTimeMillis(),
    val activatedAt: Long? = null,
    val sealedAt: Long? = null,
    val triggerSources: List<String> = emptyList(),
    val configurationSnapshotJson: String = "{}",
    val finalMerkleRoot: String? = null
)

enum class EvidenceType {
    AUDIO,
    LOCATION,
    ACCELEROMETER,
    VIDEO
}

data class EvidenceEntry(
    val evidenceId: UUID = UUID.randomUUID(),
    val incidentId: UUID,
    val type: EvidenceType,
    val createdAt: Long = System.currentTimeMillis(),
    val encryptedPath: String,
    val sha256: String,
    val signatureReference: String,
    val chunkIndex: Int? = null
)

enum class DetectorType {
    KEYWORD,
    SCREAM,
    MOTION,
    PANIC_BUTTON,
    GESTURE
}

data class DetectionEvent(
    val eventId: UUID = UUID.randomUUID(),
    val incidentId: UUID? = null,
    val detectorType: DetectorType,
    val confidence: Float,
    val occurredAt: Long = System.currentTimeMillis(),
    val modelVersion: String? = null
)

enum class ContactType {
    APP_USER,
    SMS_ONLY
}

data class NotifyContact(
    val contactId: UUID = UUID.randomUUID(),
    val displayName: String,
    val type: ContactType,
    val phoneNumber: String? = null,
    val appUserId: UUID? = null,
    val locationPermission: Boolean = false,
    val notificationPermission: Boolean = true
)

enum class AuditResult {
    SUCCESS,
    FAILURE,
    DENIED,
    DEGRADED
}

data class AuditEvent(
    val auditId: UUID = UUID.randomUUID(),
    val occurredAt: Long = System.currentTimeMillis(),
    val component: String,
    val action: String,
    val result: AuditResult,
    val incidentId: UUID? = null,
    val metadataJson: String = "{}"
)
