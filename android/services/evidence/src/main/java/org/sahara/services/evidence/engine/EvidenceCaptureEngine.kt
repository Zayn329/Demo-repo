package org.sahara.services.evidence.engine

import org.sahara.core.domain.models.EvidenceEntry
import org.sahara.core.domain.models.EvidenceType
import org.sahara.core.domain.repository.EvidenceRepository
import org.sahara.core.security.crypto.AesGcmFileStorage
import org.sahara.core.security.crypto.KeyStorageManagerImpl
import org.sahara.core.security.crypto.MerkleTree
import org.sahara.services.evidence.preroll.AudioChunk
import org.sahara.services.evidence.preroll.BoundedAudioPreRollBuffer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

class EvidenceCaptureEngine(
    private val evidenceRepository: EvidenceRepository,
    private val keyStorageManager: KeyStorageManagerImpl,
    private val aesGcmStorage: AesGcmFileStorage,
    private val preRollBuffer: BoundedAudioPreRollBuffer,
    private val storageDir: File
) {

    suspend fun processBufferedPreRoll(incidentId: UUID): List<EvidenceEntry> {
        val bufferedChunks = preRollBuffer.getBufferedChunks()
        val entries = mutableListOf<EvidenceEntry>()
        for ((index, chunk) in bufferedChunks.withIndex()) {
            val entry = capturePreRollAndAudioChunk(incidentId, chunk, index)
            entries.add(entry)
        }
        preRollBuffer.clear()
        return entries
    }

    suspend fun capturePreRollAndAudioChunk(
        incidentId: UUID,
        chunk: AudioChunk,
        chunkIndex: Int
    ): EvidenceEntry {
        val masterKey = keyStorageManager.getOrCreateMasterKey("incident_master_key")
        val dataKey = keyStorageManager.generatePerIncidentDataKey()
        val wrappedKey = keyStorageManager.wrapKey(dataKey, masterKey)
        val wrappedKeyHex = wrappedKey.joinToString("") { "%02x".format(it) }

        val pcmBytes = ByteArray(chunk.data.size * 2)
        for (i in chunk.data.indices) {
            val sample = chunk.data[i].toInt()
            pcmBytes[i * 2] = (sample and 0x00FF).toByte()
            pcmBytes[i * 2 + 1] = ((sample shr 8) and 0x00FF).toByte()
        }

        val outputStream = ByteArrayOutputStream()
        aesGcmStorage.encryptAndSave(ByteArrayInputStream(pcmBytes), outputStream, dataKey)
        val encryptedData = outputStream.toByteArray()

        val sha256Hash = MerkleTree.computeSha256(encryptedData)
        val encryptedFile = File(storageDir, "${incidentId}_chunk_${chunkIndex}.bin")
        encryptedFile.writeBytes(encryptedData)

        val signature = keyStorageManager.signData("incident_signing_key", sha256Hash.toByteArray())
        val sigRef = "sig_${signature.joinToString("") { "%02x".format(it) }.take(16)}|key_$wrappedKeyHex"

        val entry = EvidenceEntry(
            incidentId = incidentId,
            type = EvidenceType.AUDIO,
            encryptedPath = encryptedFile.absolutePath,
            sha256 = sha256Hash,
            signatureReference = sigRef,
            chunkIndex = chunkIndex
        )
        evidenceRepository.saveEvidence(entry)
        return entry
    }
}
