package org.sahara.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey val incidentId: String,
    val state: String,
    val createdAt: Long,
    val activatedAt: Long?,
    val sealedAt: Long?,
    val triggerSourcesJson: String,
    val configurationSnapshotJson: String,
    val finalMerkleRoot: String?
)

@Entity(tableName = "evidence_entries")
data class EvidenceEntryEntity(
    @PrimaryKey val evidenceId: String,
    val incidentId: String,
    val type: String,
    val createdAt: Long,
    val encryptedPath: String,
    val sha256: String,
    val signatureReference: String,
    val chunkIndex: Int?
)

@Entity(tableName = "detection_events")
data class DetectionEventEntity(
    @PrimaryKey val eventId: String,
    val incidentId: String?,
    val detectorType: String,
    val confidence: Float,
    val occurredAt: Long,
    val modelVersion: String?
)

@Entity(tableName = "notify_contacts")
data class NotifyContactEntity(
    @PrimaryKey val contactId: String,
    val displayName: String,
    val type: String,
    val phoneNumber: String?,
    val appUserId: String?,
    val locationPermission: Boolean,
    val notificationPermission: Boolean
)

@Entity(tableName = "audit_events")
data class AuditEventEntity(
    @PrimaryKey val auditId: String,
    val occurredAt: Long,
    val component: String,
    val action: String,
    val result: String,
    val incidentId: String?,
    val metadataJson: String
)
