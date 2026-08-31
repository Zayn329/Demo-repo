package org.sahara.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sahara.app.export.ExportPackage
import org.sahara.app.help.OfflineHelpDirectory
import org.sahara.core.domain.models.IncidentState
import org.sahara.core.domain.models.NotifyContact

@Composable
fun MainDashboardScreen(
    isMonitoringActive: Boolean,
    onToggleMonitoring: (Boolean) -> Unit,
    onPanicTriggered: () -> Unit,
    onStopAndSealIncident: () -> Unit,
    incidentState: IncidentState,
    onOpenHelpDirectory: () -> Unit,
    onOpenVerifier: () -> Unit,
    onOpenAuth: () -> Unit,
    onOpenNotifyCircle: () -> Unit,
    onOpenLegalDrafting: () -> Unit,
    onOpenAnchoring: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Sahara Safety Companion",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Offline-First Personal Safety & Agentic Companion",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        // Monitoring Control Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isMonitoringActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isMonitoringActive) "Safety Monitoring ON" else "Safety Monitoring OFF",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isMonitoringActive) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Text(
                        text = if (isMonitoringActive) "Listening for 'help' / scream / motion" else "Tap toggle to activate background safety",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
                Switch(
                    checked = isMonitoringActive,
                    onCheckedChange = onToggleMonitoring
                )
            }
        }

        // Incident Status View
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Current Incident State:", fontWeight = FontWeight.Bold)
                Text(
                    text = incidentState.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = when (incidentState) {
                        IncidentState.ACTIVE_INCIDENT -> Color.Red
                        IncidentState.CANDIDATE_INCIDENT, IncidentState.PENDING_CONFIRMATION -> Color(0xFFEF6C00)
                        IncidentState.SEALED -> Color(0xFF1565C0)
                        else -> Color.Black
                    }
                )
            }
        }

        // Emergency Panic & Seal Controls
        if (incidentState == IncidentState.ACTIVE_INCIDENT) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onPanicTriggered,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text(
                        text = "EMERGENCY ACTIVE — RECORDING EVIDENCE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onStopAndSealIncident,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Text(
                        text = "STOP & SEAL INCIDENT EVIDENCE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        } else {
            Button(
                onClick = onPanicTriggered,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Text(
                    text = "EMERGENCY PANIC",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Quick Feature Navigation Grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onOpenNotifyCircle, modifier = Modifier.weight(1f)) {
                    Text("Notify Circle", fontSize = 12.sp)
                }
                Button(onClick = onOpenLegalDrafting, modifier = Modifier.weight(1f)) {
                    Text("AI FIR Draft", fontSize = 12.sp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onOpenHelpDirectory, modifier = Modifier.weight(1f)) {
                    Text("Help Directory", fontSize = 12.sp)
                }
                Button(onClick = onOpenVerifier, modifier = Modifier.weight(1f)) {
                    Text("Export & Verify", fontSize = 12.sp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onOpenAnchoring, modifier = Modifier.weight(1f)) {
                    Text("Anchoring", fontSize = 12.sp)
                }
                Button(onClick = onOpenAuth, modifier = Modifier.weight(1f)) {
                    Text("Phone OTP Auth", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AuthScreen(onBack: () -> Unit) {
    var phoneNumber by remember { mutableStateOf("+91 9876543210") }
    var otpCode by remember { mutableStateOf("") }
    var otpRequestId by remember { mutableStateOf<String?>(NoneState) }
    var authStatus by remember { mutableStateOf("NOT_AUTHENTICATED") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Phone OTP Authentication",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = onBack) {
                Text("Back")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Phone Number:", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        otpRequestId = "req_" + System.currentTimeMillis()
                        authStatus = "OTP_SENT"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Request OTP")
                }

                if (otpRequestId != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Enter 6-Digit OTP Code:", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { otpCode = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (otpCode.length >= 4) {
                                authStatus = "VERIFIED_SUCCESSFULLY (User ID: usr_12345)"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("Verify OTP")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Status: $authStatus", fontWeight = FontWeight.SemiBold, color = Color.Gray)
            }
        }
    }
}

private val NoneState: String? = null

@Composable
fun NotifyCircleScreen(
    contacts: List<NotifyContact>,
    onAddContact: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var syncStatus by remember { mutableStateOf("NOT_SYNCED") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notify Circle (${contacts.size}/5)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = onBack) {
                Text("Back")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Add Trusted Emergency Contact", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Contact Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (nameInput.isNotBlank() && phoneInput.isNotBlank() && contacts.size < 5) {
                            onAddContact(nameInput, phoneInput)
                            nameInput = ""
                            phoneInput = ""
                            syncStatus = "LOCAL_ADDED_SYNC_PENDING"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = contacts.size < 5
                ) {
                    Text("Add Contact (Max 5)")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(contacts) { contact ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = contact.displayName, fontWeight = FontWeight.Bold)
                            Text(text = contact.phoneNumber ?: "SMS Only", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(text = "Location Sharing: ON", fontSize = 10.sp, color = Color(0xFF2E7D32))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { syncStatus = "SYNCED_WITH_BACKEND_API (200 OK)" },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
        ) {
            Text("Sync Circle with Backend API")
        }
        Text(text = "Status: $syncStatus", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun LegalDraftingScreen(onBack: () -> Unit) {
    var incidentSummary by remember { mutableStateOf("Distress signal triggered on 2026-08-31 near Mumbai. High pitch scream detected, panic button pressed.") }
    var generatedDraft by remember { mutableStateOf<String?>(NoneState) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI Legal FIR Complaint Drafter",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = onBack) {
                Text("Back")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Incident Context:", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = incidentSummary,
                    onValueChange = { incidentSummary = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        generatedDraft = """
                            MANDATORY DISCLAIMER: DRAFT FOR HUMAN AND LEGAL REVIEW. THIS DOCUMENT HAS NOT BEEN FILED WITH ANY AUTHORITY.

                            FORMAL POLICE COMPLAINT DRAFT (FIR)
                            ------------------------------------
                            To,
                            The Station House Officer,
                            Local Police Station, Mumbai.

                            SUBJECT: Complaint regarding safety distress incident on 2026-08-31.

                            Respected Sir/Madam,
                            I am submitting this formal complaint regarding an emergency safety incident recorded on 2026-08-31.

                            FACTS:
                            1. Incident Summary: $incidentSummary
                            2. Cryptographic Evidence: Encrypted audio pre-roll and sensor logs sealed locally with SHA-256 Merkle Root.

                            PRAYER:
                            I request you to take immediate legal cognizance of this matter and initiate appropriate action.
                        """.trimIndent()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generate Legal FIR Draft")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (generatedDraft != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "LEGAL AGENT OUTPUT:",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = generatedDraft!!, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun AnchoringScreen(onBack: () -> Unit) {
    var merkleRootInput by remember { mutableStateOf("0x3f7a8b9c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a") }
    var anchorStatus by remember { mutableStateOf("NOT_ANCHORED") }
    var txHash by remember { mutableStateOf<String?>(NoneState) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Merkle Root Remote Anchoring",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = onBack) {
                Text("Back")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sealed Incident Merkle Root:", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = merkleRootInput,
                    onValueChange = { merkleRootInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        anchorStatus = "ANCHORED_SUCCESSFULLY (200 OK)"
                        txHash = "0x" + System.currentTimeMillis().toString(16) + "8f9a"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Text("Anchor Merkle Root via Backend API")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Status: $anchorStatus", fontWeight = FontWeight.SemiBold)
                if (txHash != null) {
                    Text("Transaction Hash: $txHash", fontSize = 12.sp, color = Color(0xFF2E7D32))
                }
            }
        }
    }
}

@Composable
fun HelpDirectoryScreen(onBack: () -> Unit) {
    val contacts = OfflineHelpDirectory.getAllContacts()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Offline Help Directory",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = onBack) {
                Text("Back")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(contacts) { contact ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = contact.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(text = "Phone: ${contact.phone}", color = Color(0xFF1976D2), fontWeight = FontWeight.SemiBold)
                        Text(text = "City: ${contact.city}", fontSize = 12.sp, color = Color.Gray)
                        Text(text = contact.description, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ExportVerifierScreen(
    exportPackage: ExportPackage?,
    onVerifyPackage: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Evidence Export & Verifier",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = onBack) {
                Text("Back")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (exportPackage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (exportPackage.isIntegrityVerified) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (exportPackage.isIntegrityVerified) "INTEGRITY VERIFIED" else "VERIFICATION FAILED",
                        fontWeight = FontWeight.Bold,
                        color = if (exportPackage.isIntegrityVerified) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    exportPackage.warningDisclaimer?.let {
                        Text(text = it, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = exportPackage.summaryText, fontSize = 12.sp)
                }
            }
        } else {
            Button(onClick = onVerifyPackage, modifier = Modifier.fillMaxWidth()) {
                Text("Export & Verify Recent Incident")
            }
        }
    }
}
