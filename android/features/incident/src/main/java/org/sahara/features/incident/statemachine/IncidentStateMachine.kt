package org.sahara.features.incident.statemachine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.sahara.core.domain.models.AuditEvent
import org.sahara.core.domain.models.AuditResult
import org.sahara.core.domain.models.Incident
import org.sahara.core.domain.models.IncidentState
import org.sahara.core.domain.repository.AuditRepository
import org.sahara.core.domain.repository.IncidentRepository
import java.util.UUID

class IncidentStateMachine(
    private val incidentRepository: IncidentRepository,
    private val auditRepository: AuditRepository
) {

    private val _currentIncident = MutableStateFlow<Incident?>(null)
    val currentIncident: StateFlow<Incident?> = _currentIncident.asStateFlow()

    private val _currentState = MutableStateFlow(IncidentState.IDLE)
    val currentState: StateFlow<IncidentState> = _currentState.asStateFlow()

    suspend fun startMonitoring() {
        if (_currentState.value == IncidentState.IDLE) {
            updateState(IncidentState.MONITORING, "START_MONITORING")
        }
    }

    suspend fun stopMonitoring() {
        if (_currentState.value == IncidentState.MONITORING) {
            updateState(IncidentState.IDLE, "STOP_MONITORING")
        }
    }

    suspend fun onSuspiciousSignalDetected(source: String) {
        if (_currentState.value == IncidentState.MONITORING) {
            val incident = Incident(
                state = IncidentState.SUSPICIOUS_SIGNAL,
                triggerSources = listOf(source)
            )
            _currentIncident.value = incident
            incidentRepository.saveIncident(incident)
            updateState(IncidentState.SUSPICIOUS_SIGNAL, "SUSPICIOUS_SIGNAL_DETECTED", incident.incidentId)
        }
    }

    suspend fun transitionToCandidate() {
        if (_currentState.value == IncidentState.SUSPICIOUS_SIGNAL || _currentState.value == IncidentState.MONITORING) {
            val existing = _currentIncident.value ?: Incident(state = IncidentState.CANDIDATE_INCIDENT).also {
                incidentRepository.saveIncident(it)
                _currentIncident.value = it
            }
            updateState(IncidentState.CANDIDATE_INCIDENT, "TRANSITION_CANDIDATE", existing.incidentId)
        }
    }

    suspend fun transitionToPendingConfirmation() {
        val incident = _currentIncident.value
        if (incident != null && (_currentState.value == IncidentState.CANDIDATE_INCIDENT || _currentState.value == IncidentState.SUSPICIOUS_SIGNAL)) {
            updateState(IncidentState.PENDING_CONFIRMATION, "PENDING_CONFIRMATION", incident.incidentId)
        }
    }

    suspend fun activateIncident(triggerSource: String) {
        val existing = _currentIncident.value
        if (existing == null) {
            val incident = Incident(
                state = IncidentState.ACTIVE_INCIDENT,
                activatedAt = System.currentTimeMillis(),
                triggerSources = listOf(triggerSource)
            )
            _currentIncident.value = incident
            incidentRepository.saveIncident(incident)
            updateState(IncidentState.ACTIVE_INCIDENT, "ACTIVATE_INCIDENT_DIRECT", incident.incidentId)
        } else {
            if (_currentState.value != IncidentState.ACTIVE_INCIDENT && _currentState.value != IncidentState.SEALED) {
                val updated = existing.copy(
                    state = IncidentState.ACTIVE_INCIDENT,
                    activatedAt = System.currentTimeMillis(),
                    triggerSources = existing.triggerSources + triggerSource
                )
                _currentIncident.value = updated
                incidentRepository.saveIncident(updated)
                updateState(IncidentState.ACTIVE_INCIDENT, "ACTIVATE_INCIDENT", updated.incidentId)
            }
        }
    }

    suspend fun cancelIncident() {
        val incident = _currentIncident.value
        if (incident != null && _currentState.value != IncidentState.SEALED && _currentState.value != IncidentState.ARCHIVED) {
            val updated = incident.copy(state = IncidentState.CANCELLED)
            _currentIncident.value = updated
            incidentRepository.saveIncident(updated)
            updateState(IncidentState.CANCELLED, "CANCEL_INCIDENT", incident.incidentId)
        }
    }

    suspend fun sealIncident(merkleRoot: String) {
        val incident = _currentIncident.value
        if (incident != null && (_currentState.value == IncidentState.ACTIVE_INCIDENT || _currentState.value == IncidentState.CANCELLED)) {
            val updated = incident.copy(
                state = IncidentState.SEALED,
                sealedAt = System.currentTimeMillis(),
                finalMerkleRoot = merkleRoot
            )
            _currentIncident.value = updated
            incidentRepository.saveIncident(updated)
            updateState(IncidentState.SEALED, "SEAL_INCIDENT", incident.incidentId)
        }
    }

    private suspend fun updateState(newState: IncidentState, action: String, incidentId: UUID? = null) {
        _currentState.value = newState
        _currentIncident.value?.let {
            if (it.state != newState) {
                val updated = it.copy(state = newState)
                _currentIncident.value = updated
                incidentRepository.saveIncident(updated)
            }
        }
        auditRepository.recordAudit(
            AuditEvent(
                component = "INCIDENT_STATE_MACHINE",
                action = action,
                result = AuditResult.SUCCESS,
                incidentId = incidentId ?: _currentIncident.value?.incidentId,
                metadataJson = "{\"newState\":\"$newState\"}"
            )
        )
    }

    fun reset() {
        _currentState.value = IncidentState.IDLE
        _currentIncident.value = null
    }
}
