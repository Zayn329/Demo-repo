package org.sahara.services.mesh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.sahara.core.domain.models.ContactType
import org.sahara.core.domain.models.NotifyContact
import org.sahara.services.mesh.fallback.DemoMockSmsProvider
import org.sahara.services.mesh.fallback.EmergencyAlertPayload
import org.sahara.services.mesh.fallback.EscalationFallbackManager
import org.sahara.services.mesh.fallback.SmsDeliveryStatus
import org.sahara.services.mesh.models.MeshPacket
import org.sahara.services.mesh.models.MeshPacketType
import org.sahara.services.mesh.relay.MeshDeduplicationCache
import org.sahara.services.mesh.relay.MeshRelayResult
import org.sahara.services.mesh.relay.NearbyConnectionsMeshRelay
import java.util.UUID

class MeshRelayUnitTest {

    private lateinit var cache: MeshDeduplicationCache
    private lateinit var meshRelay: NearbyConnectionsMeshRelay
    private lateinit var mockSmsProvider: DemoMockSmsProvider
    private lateinit var fallbackManager: EscalationFallbackManager

    @Before
    fun setup() {
        cache = MeshDeduplicationCache(100)
        meshRelay = NearbyConnectionsMeshRelay(cache)
        mockSmsProvider = DemoMockSmsProvider()
        fallbackManager = EscalationFallbackManager(meshRelay, mockSmsProvider)
    }

    @Test
    fun testDeduplicationAndHopLimits() {
        val packet = MeshPacket(
            incidentId = UUID.randomUUID().toString(),
            packetType = MeshPacketType.DISTRESS_ALERT,
            hopCount = 0,
            maxHops = 12,
            senderIntegrityMetadata = "device_A",
            payloadHash = "sha256_hash",
            payloadText = "HELP"
        )

        val firstResult = meshRelay.processIncomingPacket(packet)
        assertTrue("First packet must be accepted for relay", firstResult is MeshRelayResult.ACCEPTED_FOR_RELAY)

        val duplicateResult = meshRelay.processIncomingPacket(packet)
        assertEquals(MeshRelayResult.DUPLICATE_IGNORED, duplicateResult)

        val maxHopPacket = packet.copy(packetId = UUID.randomUUID().toString(), hopCount = 12, maxHops = 12)
        val hopLimitResult = meshRelay.processIncomingPacket(maxHopPacket)
        assertEquals(MeshRelayResult.HOP_LIMIT_EXCEEDED, hopLimitResult)
    }

    @Test
    fun testEmergencySmsPayloadFormatting() {
        val payload = EmergencyAlertPayload(
            incidentId = "12345678-90ab-cdef-1234-567890abcdef",
            timestamp = 1700000000000L,
            locationText = "19.0760, 72.8777",
            locationAgeSeconds = 12L,
            evidenceIntegrityHash = "a1b2c3d4e5f67890",
            referenceCode = "SAHARA-99"
        )

        val smsText = payload.formatSmsMessage()
        assertTrue(smsText.contains("[SAHARA EMERGENCY ALERT]"))
        assertTrue(smsText.contains("Ref: SAHARA-99"))
        assertTrue(smsText.contains("Loc: 19.0760, 72.8777 (12s ago)"))
        assertTrue(smsText.contains("Integrity: a1b2c3d4"))
    }

    @Test
    fun testEscalationFallbackToSms() {
        val payload = EmergencyAlertPayload(
            incidentId = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            locationText = "Mumbai",
            evidenceIntegrityHash = "e3b0c442",
            referenceCode = "REF123"
        )

        val contact1 = NotifyContact(displayName = "Alice", type = ContactType.SMS_ONLY, phoneNumber = "+11111111")
        val contact2 = NotifyContact(displayName = "Bob", type = ContactType.SMS_ONLY, phoneNumber = "+22222222")

        val results = fallbackManager.executeEscalation(payload, listOf(contact1, contact2), null)

        assertEquals(2, results.size)
        assertTrue(results["Alice"] is SmsDeliveryStatus.SIMULATED_DEMO)
        assertTrue(results["Bob"] is SmsDeliveryStatus.SIMULATED_DEMO)
    }
}
