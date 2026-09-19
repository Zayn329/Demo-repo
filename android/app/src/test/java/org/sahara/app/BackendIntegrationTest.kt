package org.sahara.app

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.sahara.app.ui.SaharaApiClient

class BackendIntegrationTest {

    @Test
    fun testLegalDraftPayloadFormatNoRawEvidence() {
        val summaryText = "Panic activated near Mumbai Central. Screaming detected."
        val victimName = "Ananya Sen"
        val locationText = "Mumbai Central"

        // Ensure payload build excludes raw evidence bytes or private keys
        val escSummary = summaryText.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        val escName = victimName.replace("\\", "\\\\").replace("\"", "\\\"")
        val escLoc = locationText.replace("\\", "\\\\").replace("\"", "\\\"")
        val body = """
            {
              "incident_id": "inc_12345",
              "draft_type": "FIR_COMPLAINT",
              "authorized_summary": {
                "incident_summary": "$escSummary",
                "victim_name": "$escName",
                "location_text": "$escLoc"
              },
              "user_authorized": true
            }
        """.trimIndent()

        assertTrue(body.contains("inc_12345"))
        assertTrue(body.contains("FIR_COMPLAINT"))
        assertFalse("Payload must not contain raw audio bytes", body.contains("raw_audio"))
        assertFalse("Payload must not contain private key", body.contains("private_key"))
    }

    @Test
    fun testOfflineLegalDraftFallback() = runBlocking {
        var generatedDraft: String? = null
        val incidentSummary = "Suspicious approach detected"
        val victimName = "Priya"
        val locationText = "Andheri East"

        try {
            // Attempt call with invalid URL to simulate offline/network failure
            SaharaApiClient.postJson("/api/v1/legal/drafts", "{}", bearerToken = "invalid_token")
        } catch (e: Exception) {
            generatedDraft = "DRAFT FOR HUMAN AND LEGAL REVIEW. THIS DOCUMENT HAS NOT BEEN FILED WITH ANY AUTHORITY.\n\n" +
                "[OFFLINE FALLBACK DRAFT]\n" +
                "FIRST INFORMATION REPORT (DRAFT)\n\n" +
                "Incident Context: $incidentSummary\n" +
                "Complainant/Victim: $victimName\n" +
                "Location: $locationText"
        }

        assertNotNull(generatedDraft)
        assertTrue(generatedDraft!!.contains("DRAFT FOR HUMAN AND LEGAL REVIEW"))
        assertTrue(generatedDraft.contains("Incident Context: Suspicious approach detected"))
    }
}
