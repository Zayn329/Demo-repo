package org.sahara.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.sahara.app.export.EvidenceExporter
import org.sahara.app.help.OfflineHelpDirectory
import org.sahara.core.domain.models.EvidenceEntry
import org.sahara.core.domain.models.EvidenceType
import org.sahara.core.domain.models.Incident
import org.sahara.core.domain.models.IncidentState
import org.sahara.services.evidence.manifest.EvidenceManifest
import java.io.File
import java.util.UUID

class AppMilestone8UnitTest {

    private lateinit var tempDir: File

    @Before
    fun setup() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "app_test_" + System.currentTimeMillis())
        tempDir.mkdirs()
    }

    @Test
    fun testOfflineHelpDirectoryLookup() {
        val mumbaiContacts = OfflineHelpDirectory.getContactsForCity("Mumbai")
        assertTrue("Mumbai should return contacts including national emergency", mumbaiContacts.size >= 3)
        assertTrue(mumbaiContacts.any { it.phone == "100" })

        val allContacts = OfflineHelpDirectory.getAllContacts()
        assertEquals(5, allContacts.size)
    }

    @Test
    fun testEvidenceExporterVerifiedPackageCreation() {
        val incident = Incident(
            incidentId = UUID.randomUUID(),
            state = IncidentState.SEALED,
            sealedAt = System.currentTimeMillis(),
            triggerSources = listOf("KEYWORD_HELP")
        )
        val manifest = EvidenceManifest(
            incidentId = incident.incidentId.toString(),
            createdAt = incident.createdAt,
            sealedAt = incident.sealedAt!!,
            chunkHashes = listOf("hash_123"),
            merkleRoot = "root_merkle_123",
            signingKeyMetadata = "KeyStore/manifest_key",
            signature = "sig_bytes_hex"
        )
        val entry = EvidenceEntry(
            incidentId = incident.incidentId,
            type = EvidenceType.AUDIO,
            encryptedPath = "/path/to/encrypted/file.bin",
            sha256 = "hash_123",
            signatureReference = "sig_123",
            chunkIndex = 0
        )

        val pkg = EvidenceExporter.createExportPackage(
            incident, manifest, listOf(entry), tempDir, isIntegrityVerified = true
        )

        assertTrue(pkg.isIntegrityVerified)
        assertNull(pkg.warningDisclaimer)
        assertNotNull(pkg.exportPath)
        assertTrue(pkg.summaryText.contains("Technical integrity protection"))
        assertTrue(pkg.summaryText.contains("root_merkle_123"))
    }

    @Test
    fun testEvidenceExporterTamperedPackageWarning() {
        val incident = Incident(incidentId = UUID.randomUUID(), state = IncidentState.SEALED)
        val manifest = EvidenceManifest(
            incidentId = incident.incidentId.toString(),
            createdAt = incident.createdAt,
            sealedAt = System.currentTimeMillis(),
            chunkHashes = listOf("tampered_hash"),
            merkleRoot = "root_tampered",
            signingKeyMetadata = "KeyStore/manifest_key",
            signature = "sig_bytes_hex"
        )

        val entry = EvidenceEntry(
            incidentId = incident.incidentId,
            type = EvidenceType.AUDIO,
            encryptedPath = "/path/to/encrypted/file.bin",
            sha256 = "tampered_hash",
            signatureReference = "sig_123",
            chunkIndex = 0
        )

        val pkg = EvidenceExporter.createExportPackage(
            incident, manifest, listOf(entry), tempDir, isIntegrityVerified = false
        )

        assertFalse(pkg.isIntegrityVerified)
        assertNotNull(pkg.warningDisclaimer)
        assertTrue(pkg.warningDisclaimer!!.contains("Integrity verification failed"))
    }
}
