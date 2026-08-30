package org.sahara.services.evidence.manifest

import org.sahara.core.domain.models.EvidenceEntry
import org.sahara.core.domain.models.Incident
import org.sahara.core.domain.models.IncidentState
import org.sahara.core.domain.repository.IncidentRepository
import org.sahara.core.security.crypto.KeyStorageManagerImpl
import org.sahara.core.security.crypto.MerkleTree
import java.util.UUID

data class EvidenceManifest(
    val incidentId: String,
    val createdAt: Long,
    val sealedAt: Long,
    val chunkHashes: List<String>,
    val merkleRoot: String,
    val signingKeyMetadata: String,
    val signature: String,
    val appVersion: String = "1.0.0"
)

class EvidenceManifestManager(
    private val incidentRepository: IncidentRepository,
    private val keyStorageManager: KeyStorageManagerImpl
) {

    suspend fun createAndSignManifest(
        incident: Incident,
        evidenceEntries: List<EvidenceEntry>
    ): EvidenceManifest {
        val sortedHashes = evidenceEntries.sortedBy { it.chunkIndex ?: 0 }.map { it.sha256 }
        val merkleRoot = MerkleTree.buildMerkleRoot(sortedHashes)

        val sealedAt = System.currentTimeMillis()
        val rawManifestPayload = "${incident.incidentId}|$merkleRoot|$sealedAt"
        val signatureBytes = keyStorageManager.signData("manifest_key", rawManifestPayload.toByteArray())
        val signatureStr = signatureBytes.joinToString("") { "%02x".format(it) }

        val manifest = EvidenceManifest(
            incidentId = incident.incidentId.toString(),
            createdAt = incident.createdAt,
            sealedAt = sealedAt,
            chunkHashes = sortedHashes,
            merkleRoot = merkleRoot,
            signingKeyMetadata = "AndroidKeystore/manifest_key",
            signature = signatureStr
        )

        incidentRepository.updateState(
            id = incident.incidentId,
            state = IncidentState.SEALED,
            sealedAt = sealedAt,
            merkleRoot = merkleRoot
        )

        return manifest
    }
}

object EvidenceVerifier {

    fun verifyPackageIntegrity(
        manifest: EvidenceManifest,
        evidenceEntries: List<EvidenceEntry>,
        keyStorageManager: KeyStorageManagerImpl
    ): Boolean {
        val sortedHashes = evidenceEntries.sortedBy { it.chunkIndex ?: 0 }.map { it.sha256 }
        val recomputedMerkleRoot = MerkleTree.buildMerkleRoot(sortedHashes)

        if (recomputedMerkleRoot != manifest.merkleRoot) {
            return false
        }

        val rawManifestPayload = "${manifest.incidentId}|${manifest.merkleRoot}|${manifest.sealedAt}"
        val signatureBytes = manifest.signature.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        return keyStorageManager.verifySignature("manifest_key", rawManifestPayload.toByteArray(), signatureBytes)
    }
}
