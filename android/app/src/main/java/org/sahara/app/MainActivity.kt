package org.sahara.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.sahara.app.export.EvidenceExporter
import org.sahara.app.export.ExportPackage
import org.sahara.app.ui.ExportVerifierScreen
import org.sahara.app.ui.HelpDirectoryScreen
import org.sahara.app.ui.MainDashboardScreen
import org.sahara.core.data.db.SaharaDatabase
import org.sahara.core.data.repository.AuditRepositoryImpl
import org.sahara.core.data.repository.EvidenceRepositoryImpl
import org.sahara.core.data.repository.IncidentRepositoryImpl
import org.sahara.core.domain.models.Incident
import org.sahara.core.domain.models.IncidentState
import org.sahara.core.security.crypto.AesGcmFileStorage
import org.sahara.core.security.crypto.KeyStorageManagerImpl
import org.sahara.features.incident.service.SafetyForegroundService
import org.sahara.features.incident.statemachine.IncidentStateMachine
import org.sahara.features.panic.controller.PanicController
import org.sahara.services.evidence.engine.EvidenceCaptureEngine
import org.sahara.services.evidence.manifest.EvidenceManifest
import org.sahara.services.evidence.manifest.EvidenceManifestManager
import org.sahara.services.evidence.manifest.EvidenceVerifier
import org.sahara.services.evidence.preroll.BoundedAudioPreRollBuffer
import java.io.File
import java.util.UUID

enum class Screen {
    DASHBOARD,
    HELP_DIRECTORY,
    VERIFIER
}

class MainActivity : ComponentActivity() {

    private lateinit var database: SaharaDatabase
    private lateinit var incidentRepository: IncidentRepositoryImpl
    private lateinit var evidenceRepository: EvidenceRepositoryImpl
    private lateinit var auditRepository: AuditRepositoryImpl
    private lateinit var stateMachine: IncidentStateMachine
    private lateinit var panicController: PanicController
    private lateinit var keyManager: KeyStorageManagerImpl
    private lateinit var captureEngine: EvidenceCaptureEngine
    private lateinit var manifestManager: EvidenceManifestManager
    private lateinit var preRollBuffer: BoundedAudioPreRollBuffer

    private var foregroundService: SafetyForegroundService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as SafetyForegroundService.LocalBinder
            foregroundService = binder.getService()
            foregroundService?.stateMachine = stateMachine
            foregroundService?.evidenceCaptureEngine = captureEngine
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            foregroundService = null
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = SaharaDatabase.getDatabase(applicationContext)
        incidentRepository = IncidentRepositoryImpl(database.incidentDao())
        evidenceRepository = EvidenceRepositoryImpl(database.evidenceDao())
        auditRepository = AuditRepositoryImpl(database.auditEventDao())

        stateMachine = IncidentStateMachine(incidentRepository, auditRepository)
        panicController = PanicController(stateMachine)

        keyManager = KeyStorageManagerImpl()
        val gcmStorage = AesGcmFileStorage()
        preRollBuffer = BoundedAudioPreRollBuffer()
        val storageDir = File(filesDir, "evidence")
        storageDir.mkdirs()

        captureEngine = EvidenceCaptureEngine(evidenceRepository, keyManager, gcmStorage, preRollBuffer, storageDir)
        manifestManager = EvidenceManifestManager(incidentRepository, keyManager)

        // Bind SafetyForegroundService
        val serviceIntent = Intent(this, SafetyForegroundService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            SaharaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SaharaAppUi()
                }
            }
        }
    }

    @Composable
    fun SaharaAppUi() {
        var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
        var isMonitoringActive by remember { mutableStateOf(false) }
        var activeIncidentState by remember { mutableStateOf(IncidentState.IDLE) }
        var recentExportPackage by remember { mutableStateOf<ExportPackage?>(null) }
        val scope = rememberCoroutineScope()

        when (currentScreen) {
            Screen.DASHBOARD -> {
                MainDashboardScreen(
                    isMonitoringActive = isMonitoringActive,
                    onToggleMonitoring = { enabled ->
                        isMonitoringActive = enabled
                        scope.launch {
                            if (enabled) {
                                stateMachine.startMonitoring()
                                activeIncidentState = IncidentState.MONITORING
                            } else {
                                stateMachine.stopMonitoring()
                                activeIncidentState = IncidentState.IDLE
                            }
                        }
                    },
                    onPanicTriggered = {
                        scope.launch {
                            panicController.triggerPanicImmediately("IN_APP_EMERGENCY_BUTTON")
                            activeIncidentState = IncidentState.ACTIVE_INCIDENT

                            // Drain pre-roll audio buffer into active incident evidence
                            val currentInc = stateMachine.currentIncident.value
                            if (currentInc != null) {
                                captureEngine.processBufferedPreRoll(currentInc.incidentId)
                            }
                        }
                    },
                    onStopAndSealIncident = {
                        scope.launch {
                            val currentInc = stateMachine.currentIncident.value
                            if (currentInc != null) {
                                val entries = evidenceRepository.getEvidenceForIncident(currentInc.incidentId).first()
                                if (entries.isNotEmpty()) {
                                    val manifest = manifestManager.createAndSignManifest(currentInc, entries)
                                    stateMachine.sealIncident(manifest.merkleRoot)
                                    activeIncidentState = IncidentState.SEALED
                                } else {
                                    stateMachine.cancelIncident()
                                    activeIncidentState = IncidentState.CANCELLED
                                }
                            }
                        }
                    },
                    incidentState = activeIncidentState,
                    onOpenHelpDirectory = { currentScreen = Screen.HELP_DIRECTORY },
                    onOpenVerifier = { currentScreen = Screen.VERIFIER }
                )
            }
            Screen.HELP_DIRECTORY -> {
                HelpDirectoryScreen(
                    onBack = { currentScreen = Screen.DASHBOARD }
                )
            }
            Screen.VERIFIER -> {
                ExportVerifierScreen(
                    exportPackage = recentExportPackage,
                    onVerifyPackage = {
                        scope.launch {
                            val incident = stateMachine.currentIncident.value
                            if (incident == null) {
                                val dummyUnsealedIncident = Incident(
                                    incidentId = UUID.randomUUID(),
                                    state = IncidentState.ACTIVE_INCIDENT
                                )
                                recentExportPackage = EvidenceExporter.createExportPackage(
                                    incident = dummyUnsealedIncident,
                                    manifest = null,
                                    evidenceEntries = emptyList(),
                                    outputDir = filesDir,
                                    isIntegrityVerified = false
                                )
                            } else {
                                val entries = evidenceRepository.getEvidenceForIncident(incident.incidentId).first()
                                if (entries.isEmpty() || incident.state != IncidentState.SEALED) {
                                    recentExportPackage = EvidenceExporter.createExportPackage(
                                        incident = incident,
                                        manifest = null,
                                        evidenceEntries = entries,
                                        outputDir = filesDir,
                                        isIntegrityVerified = false
                                    )
                                } else {
                                    val manifest = manifestManager.createAndSignManifest(incident, entries)
                                    val isVerified = EvidenceVerifier.verifyPackageIntegrity(
                                        manifest = manifest,
                                        evidenceEntries = entries,
                                        keyStorageManager = keyManager,
                                        incidentState = incident.state
                                    )
                                    recentExportPackage = EvidenceExporter.createExportPackage(
                                        incident = incident,
                                        manifest = manifest,
                                        evidenceEntries = entries,
                                        outputDir = filesDir,
                                        isIntegrityVerified = isVerified
                                    )
                                }
                            }
                        }
                    },
                    onBack = { currentScreen = Screen.DASHBOARD }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }
}

@Composable
fun SaharaTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
