package org.sahara.services.evidence

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.sahara.core.data.repository.EvidenceRepositoryImpl
import org.sahara.core.data.repository.IncidentRepositoryImpl
import org.sahara.core.domain.models.Incident
import org.sahara.core.domain.models.IncidentState
import org.sahara.core.security.crypto.AesGcmFileStorage
import org.sahara.core.security.crypto.KeyStorageManagerImpl
import org.sahara.core.testing.fakes.FakeEvidenceDao
import org.sahara.core.testing.fakes.FakeIncidentDao
import org.sahara.services.evidence.engine.EvidenceCaptureEngine
import org.sahara.services.evidence.manifest.EvidenceManifestManager
import org.sahara.services.evidence.manifest.EvidenceVerifier
import org.sahara.services.evidence.preroll.AudioChunk
import org.sahara.services.evidence.preroll.BoundedAudioPreRollBuffer
import java.io.File

class EvidenceEngineUnitTest {

    private lateinit var keyManager: KeyStorageManagerImpl
    private lateinit var gcmStorage: AesGcmFileStorage
    private lateinit var preRollBuffer: BoundedAudioPreRollBuffer
    private lateinit var evidenceRepository: EvidenceRepositoryImpl
    private lateinit var incidentRepository: IncidentRepositoryImpl
    private lateinit var captureEngine: EvidenceCaptureEngine
    private lateinit var manifestManager: EvidenceManifestManager
    private lateinit var tempDir: File

    @Before
    fun setup() {
        keyManager = KeyStorageManagerImpl()
        gcmStorage = AesGcmFileStorage()
        preRollBuffer = BoundedAudioPreRollBuffer(10000L)
        evidenceRepository = EvidenceRepositoryImpl(FakeEvidenceDao())
        incidentRepository = IncidentRepositoryImpl(FakeIncidentDao())

        tempDir = File(System.getProperty("java.io.tmpdir"), "evidence_test_" + System.currentTimeMillis())
        tempDir.mkdirs()

        captureEngine = EvidenceCaptureEngine(
            evidenceRepository, keyManager, gcmStorage, preRollBuffer, tempDir
        )
        manifestManager = EvidenceManifestManager(incidentRepository, keyManager)
    }

    @Test
    fun testPreRollBufferRetentionAndPruning() {
        val now = System.currentTimeMillis()
        preRollBuffer.offerChunk(AudioChunk("c1", ShortArray(100), now - 15000L))
        preRollBuffer.offerChunk(AudioChunk("c2", ShortArray(100), now - 5000L))

        val buffered = preRollBuffer.getBufferedChunks(now)
        assertEquals(1, buffered.size)
        assertEquals("c2", buffered[0].id)
    }

    @Test
    fun testEvidenceCaptureAndManifestSealing() = runBlocking {
        val incident = Incident(state = IncidentState.ACTIVE_INCIDENT)
        incidentRepository.saveIncident(incident)

        val chunk1 = AudioChunk("c1", ShortArray(1600) { 1000 })
        captureEngine.capturePreRollAndAudioChunk(incident.incidentId, chunk1, 0)

        val chunk2 = AudioChunk("c2", ShortArray(1600) { 2000 })
        captureEngine.capturePreRollAndAudioChunk(incident.incidentId, chunk2, 1)

        val savedEntries = evidenceRepository.getEvidenceForIncident(incident.incidentId).first()
        assertEquals(2, savedEntries.size)

        val manifest = manifestManager.createAndSignManifest(incident, savedEntries)
        assertNotNull(manifest.merkleRoot)
        assertNotNull(manifest.signature)

        val updatedIncident = incidentRepository.getIncidentById(incident.incidentId)
        assertEquals(IncidentState.SEALED, updatedIncident?.state)

        val isValid = EvidenceVerifier.verifyPackageIntegrity(manifest, savedEntries, keyManager)
        assertTrue("Manifest signature and Merkle root should verify", isValid)
    }

    @Test
    fun testTamperedEvidenceFailsVerification() = runBlocking {
        val incident = Incident(state = IncidentState.ACTIVE_INCIDENT)
        incidentRepository.saveIncident(incident)

        val entry = captureEngine.capturePreRollAndAudioChunk(incident.incidentId, AudioChunk("c1", ShortArray(100)), 0)
        val savedEntries = listOf(entry)
        val manifest = manifestManager.createAndSignManifest(incident, savedEntries)

        val tamperedEntry = entry.copy(sha256 = "tampered_hash_value")
        val isValid = EvidenceVerifier.verifyPackageIntegrity(manifest, listOf(tamperedEntry), keyManager)

        assertFalse("Tampered evidence entry must fail integrity verification", isValid)
    }
}
