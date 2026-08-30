package org.sahara.app.export

import org.sahara.core.domain.models.EvidenceEntry
import org.sahara.core.domain.models.Incident
import org.sahara.services.evidence.manifest.EvidenceManifest
import java.io.File

data class ExportPackage(
    val exportPath: String,
    val summaryText: String,
    val isIntegrityVerified: Boolean,
    val warningDisclaimer: String? = null
)

object EvidenceExporter {

    const val LEGAL_ADMISSIBILITY_DISCLAIMER =
        "LEGAL DISCLAIMER: Technical integrity protection (SHA-256 Merkle root and ECDSA signature) " +
        "verifies that this exported safety evidence package has not been tampered with since sealing. " +
        "This technical verification does not guarantee court admissibility or police acceptance. " +
        "Consult legal counsel before formal submission."

    fun createExportPackage(
        incident: Incident,
        manifest: EvidenceManifest,
        evidenceEntries: List<EvidenceEntry>,
        outputDir: File,
        isIntegrityVerified: Boolean
    ): ExportPackage {
        val exportFile = File(outputDir, "sahara_export_${incident.incidentId}.json")

        val summaryText = buildString {
            append("SAHARA SAFETY COMPANION — INCIDENT EVIDENCE SUMMARY\n")
            append("----------------------------------------------------\n")
            append("Incident ID: ${incident.incidentId}\n")
            append("Created At: ${incident.createdAt}\n")
            append("Sealed At: ${incident.sealedAt ?: "N/A"}\n")
            append("Status: ${incident.state}\n")
            append("Trigger Sources: ${incident.triggerSources.joinToString(", ")}\n")
            append("Merkle Root: ${manifest.merkleRoot}\n")
            append("Integrity Verification: ${if (isIntegrityVerified) "VERIFIED" else "FAILED"}\n\n")
            append("EVIDENCE CHUNKS:\n")
            for (entry in evidenceEntries) {
                append(" - Chunk #${entry.chunkIndex ?: 0} (${entry.type}): SHA256=${entry.sha256}\n")
            }
            append("\n$LEGAL_ADMISSIBILITY_DISCLAIMER\n")
        }

        val jsonContent = """
            {
              "incidentId": "${incident.incidentId}",
              "createdAt": ${incident.createdAt},
              "sealedAt": ${incident.sealedAt ?: 0},
              "state": "${incident.state}",
              "isIntegrityVerified": $isIntegrityVerified,
              "manifest": {
                "merkleRoot": "${manifest.merkleRoot}",
                "signature": "${manifest.signature}",
                "signingKeyMetadata": "${manifest.signingKeyMetadata}"
              },
              "evidenceChunks": [
                ${evidenceEntries.joinToString(",") { """{"index": ${it.chunkIndex ?: 0}, "type": "${it.type}", "sha256": "${it.sha256}"}""" }}
              ],
              "legalDisclaimer": "$LEGAL_ADMISSIBILITY_DISCLAIMER"
            }
        """.trimIndent()

        exportFile.writeText(jsonContent)

        val warning = if (!isIntegrityVerified) {
            "CRITICAL WARNING: Integrity verification failed for this package! Evidence or manifest may have been modified."
        } else null

        return ExportPackage(
            exportPath = exportFile.absolutePath,
            summaryText = summaryText,
            isIntegrityVerified = isIntegrityVerified,
            warningDisclaimer = warning
        )
    }
}
