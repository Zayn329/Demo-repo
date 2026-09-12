package org.sahara.app

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.sahara.app.export.EvidenceExporter
import org.sahara.core.data.repository.AuditRepositoryImpl
import org.sahara.core.data.repository.EvidenceRepositoryImpl
import org.sahara.core.data.repository.IncidentRepositoryImpl
import org.sahara.core.domain.models.IncidentState
import org.sahara.core.security.crypto.AesGcmFileStorage
import org.sahara.core.security.crypto.KeyStorageManagerImpl
import org.sahara.core.testing.fakes.FakeAuditEventDao
import org.sahara.core.testing.fakes.FakeEvidenceDao
import org.sahara.core.testing.fakes.FakeIncidentDao
import org.sahara.features.incident.statemachine.IncidentStateMachine
import org.sahara.features.panic.controller.PanicController
import org.sahara.services.evidence.engine.EvidenceCaptureEngine
import org.sahara.services.evidence.manifest.EvidenceManifestManager
import org.sahara.services.evidence.manifest.EvidenceVerifier
import org.sahara.services.evidence.preroll.AudioChunk
import org.sahara.services.evidence.preroll.BoundedAudioPreRollBuffer
import java.io.File

class CriticalEvidenceIntegrationTest {

    private lateinit var tempDir: File
    private lateinit var incidentRepository: IncidentRepositoryImpl
    private lateinit var evidenceRepository: EvidenceRepositoryImpl
    private lateinit var auditRepository: AuditRepositoryImpl
    private lateinit var stateMachine: IncidentStateMachine
    private lateinit var panicController: PanicController
    private lateinit var keyManager: KeyStorageManagerImpl
    private lateinit var captureEngine: EvidenceCaptureEngine
    private lateinit var manifestManager: EvidenceManifestManager
    private lateinit var preRollBuffer: BoundedAudioPreRollBuffer

    @Before
    fun setup() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "p0_integration_test_" + System.currentTimeMillis())
        tempDir.mkdirs()

        incidentRepository = IncidentRepositoryImpl(FakeIncidentDao())
        evidenceRepository = EvidenceRepositoryImpl(FakeEvidenceDao())
        auditRepository = AuditRepositoryImpl(FakeAuditEventDao())

        stateMachine = IncidentStateMachine(incidentRepository, auditRepository)
        panicController = PanicController(stateMachine)

        keyManager = KeyStorageManagerImpl()
        val gcmStorage = AesGcmFileStorage()
        preRollBuffer = BoundedAudioPreRollBuffer(10000L)

        captureEngine = EvidenceCaptureEngine(
            evidenceRepository, keyManager, gcmStorage, preRollBuffer, tempDir
        )
        manifestManager = EvidenceManifestManager(incidentRepository, keyManager)

        // Connect Panic/State Machine -> Evidence Capture
        stateMachine.onIncidentActivated = { incident ->
            captureEngine.processBufferedPreRoll(incident.incidentId)
        }
    }

    @Test
    fun testEndToEndCriticalEvidencePipeline() = runBlocking {
        // 1. Simulate Pre-roll audio collection while monitoring
        stateMachine.startMonitoring()
        assertEquals(IncidentState.MONITORING, stateMachine.currentState.value)

        val chunk1 = AudioChunk("preroll_1", ShortArray(1600) { 500 })
        val chunk2 = AudioChunk("preroll_2", ShortArray(1600) { 800 })
        preRollBuffer.offerChunk(chunk1)
        preRollBuffer.offerChunk(chunk2)

        // 2. Trigger Panic Immediately (e.g., In-app Panic or gesture)
        stateMachine.activateIncident("TEST_PANIC_BUTTON")
        assertEquals(IncidentState.ACTIVE_INCIDENT, stateMachine.currentState.value)

        val activeIncident = stateMachine.currentIncident.value
        assertNotNull(activeIncident)

        // Verify pre-roll buffer was automatically flushed and persisted
        val persistedEntriesAfterActivation = evidenceRepository.getEvidenceForIncident(activeIncident!!.incidentId).first()
        assertEquals(2, persistedEntriesAfterActivation.size)

        // 3. Record continuous active audio chunk during ACTIVE_INCIDENT
        val chunk3 = AudioChunk("active_1", ShortArray(1600) { 1200 })
        captureEngine.capturePreRollAndAudioChunk(activeIncident.incidentId, chunk3, 2)

        val allEntries = evidenceRepository.getEvidenceForIncident(activeIncident.incidentId).first()
        assertEquals(3, allEntries.size)

        // 4. End incident -> Process remaining pre-roll, seal manifest with Merkle root & Keystore signature
        captureEngine.processBufferedPreRoll(activeIncident.incidentId)
        val finalEntries = evidenceRepository.getEvidenceForIncident(activeIncident.incidentId).first()
        val manifest = manifestManager.createAndSignManifest(activeIncident, finalEntries)
        stateMachine.sealIncident(manifest.merkleRoot, manifest.sealedAt)

        val sealedIncident = incidentRepository.getIncidentById(activeIncident.incidentId)
        assertNotNull(sealedIncident)
        assertEquals(IncidentState.SEALED, sealedIncident?.state)
        assertNotNull(sealedIncident?.finalMerkleRoot)

        // 5. Verification & Export
        val isVerified = EvidenceVerifier.verifyPackageIntegrity(
            manifest = manifest,
            evidenceEntries = finalEntries,
            keyStorageManager = keyManager,
            incidentState = sealedIncident?.state
        )
        assertTrue("Sealed evidence package must pass cryptographic verification", isVerified)

        val exportPkg = EvidenceExporter.createExportPackage(
            incident = sealedIncident!!,
            manifest = manifest,
            evidenceEntries = finalEntries,
            outputDir = tempDir,
            isIntegrityVerified = isVerified
        )

        assertTrue(exportPkg.isIntegrityVerified)
        assertNotNull(exportPkg.exportPath)
        assertTrue(File(exportPkg.exportPath).exists())

        // 6. Verify tampered evidence detection
        val tamperedEntries = finalEntries.toMutableList()
        tamperedEntries[0] = tamperedEntries[0].copy(sha256 = "0000000000000000000000000000000000000000000000000000000000000000")
        val isTamperedVerified = EvidenceVerifier.verifyPackageIntegrity(
            manifest = manifest,
            evidenceEntries = tamperedEntries,
            keyStorageManager = keyManager,
            incidentState = sealedIncident.state
        )
        assertFalse("Tampered evidence chunk must fail verification", isTamperedVerified)
    }
}
