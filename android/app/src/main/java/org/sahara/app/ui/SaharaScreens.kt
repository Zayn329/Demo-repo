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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sahara.app.export.ExportPackage
import org.sahara.app.help.HelpContact
import org.sahara.app.help.OfflineHelpDirectory
import org.sahara.core.domain.models.IncidentState

@Composable
fun MainDashboardScreen(
    isMonitoringActive: Boolean,
    onToggleMonitoring: (Boolean) -> Unit,
    onPanicTriggered: () -> Unit,
    onStopAndSealIncident: () -> Unit,
    incidentState: IncidentState,
    onOpenHelpDirectory: () -> Unit,
    onOpenVerifier: () -> Unit
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
                text = "Offline-First Personal Safety",
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

        // Prominent Emergency Panic & Seal Controls
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
                    .height(80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Text(
                    text = "EMERGENCY PANIC",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onOpenHelpDirectory) {
                Text("Help Directory")
            }
            Button(onClick = onOpenVerifier) {
                Text("Export & Verify")
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
