package org.sahara.core.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class ModelsUnitTest {

    @Test
    fun testIncidentCreation() {
        val incident = Incident(
            state = IncidentState.MONITORING,
            triggerSources = listOf("KEYWORD", "MOTION")
        )
        assertNotNull(incident.incidentId)
        assertEquals(IncidentState.MONITORING, incident.state)
        assertEquals(2, incident.triggerSources.size)
        assertNull(incident.activatedAt)
        assertNull(incident.sealedAt)
        assertNull(incident.finalMerkleRoot)
    }

    @Test
    fun testEvidenceEntryCreation() {
        val incidentId = UUID.randomUUID()
        val entry = EvidenceEntry(
            incidentId = incidentId,
            type = EvidenceType.AUDIO,
            encryptedPath = "/storage/encrypted/sample.bin",
            sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            signatureReference = "keystore://alias/sig1",
            chunkIndex = 0
        )
        assertEquals(incidentId, entry.incidentId)
        assertEquals(EvidenceType.AUDIO, entry.type)
        assertEquals(0, entry.chunkIndex)
    }

    @Test
    fun testDetectionEventCreation() {
        val event = DetectionEvent(
            detectorType = DetectorType.KEYWORD,
            confidence = 0.95f,
            modelVersion = "kws_v1.0"
        )
        assertEquals(DetectorType.KEYWORD, event.detectorType)
        assertEquals(0.95f, event.confidence, 0.001f)
        assertEquals("kws_v1.0", event.modelVersion)
    }

    @Test
    fun testNotifyContactCreation() {
        val contact = NotifyContact(
            displayName = "Alice",
            type = ContactType.SMS_ONLY,
            phoneNumber = "+1234567890",
            locationPermission = true
        )
        assertEquals("Alice", contact.displayName)
        assertEquals(ContactType.SMS_ONLY, contact.type)
        assertEquals(true, contact.locationPermission)
    }

    @Test
    fun testAuditEventCreation() {
        val audit = AuditEvent(
            component = "SAFETY_AGENT",
            action = "KEYWORD_DETECTED",
            result = AuditResult.SUCCESS
        )
        assertEquals("SAFETY_AGENT", audit.component)
        assertEquals("KEYWORD_DETECTED", audit.action)
        assertEquals(AuditResult.SUCCESS, audit.result)
    }
}
