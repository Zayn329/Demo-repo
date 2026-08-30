package org.sahara.services.detection.fusion

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.sahara.core.domain.models.DetectorType
import org.sahara.core.domain.models.IncidentState
import org.sahara.services.detection.models.DetectionConfig
import org.sahara.services.detection.models.SignalResult
import java.util.concurrent.CopyOnWriteArrayList

sealed class FusionDecision {
    data class EnterPossibleDistress(val primarySignal: SignalResult) : FusionDecision()
    data class ConfirmIncident(val activeSignals: List<SignalResult>) : FusionDecision()
    object CandidateExpired : FusionDecision()
}

class SignalFusionEngine(val config: DetectionConfig) {

    private val activeSignals = CopyOnWriteArrayList<SignalResult>()
    private val _decisionFlow = MutableSharedFlow<FusionDecision>(extraBufferCapacity = 64)
    val decisionFlow: Flow<FusionDecision> = _decisionFlow.asSharedFlow()

    var currentState: IncidentState = IncidentState.MONITORING
        private set

    fun onSignalReceived(signal: SignalResult) {
        cleanExpiredSignals(signal.timestamp)
        activeSignals.add(signal)

        when (currentState) {
            IncidentState.MONITORING -> {
                if (signal.detectorType == DetectorType.KEYWORD && signal.confidence >= config.keywordConfidenceThreshold) {
                    currentState = IncidentState.CANDIDATE_INCIDENT
                    _decisionFlow.tryEmit(FusionDecision.EnterPossibleDistress(signal))
                }
            }
            IncidentState.CANDIDATE_INCIDENT, IncidentState.PENDING_CONFIRMATION -> {
                if (evaluateConfirmationRule()) {
                    currentState = IncidentState.ACTIVE_INCIDENT
                    _decisionFlow.tryEmit(FusionDecision.ConfirmIncident(activeSignals.toList()))
                }
            }
            else -> { /* Inactive or already Active */ }
        }
    }

    fun checkConfirmationTimeout(currentTime: Long) {
        cleanExpiredSignals(currentTime)
        if (currentState == IncidentState.CANDIDATE_INCIDENT || currentState == IncidentState.PENDING_CONFIRMATION) {
            if (!evaluateConfirmationRule()) {
                currentState = IncidentState.MONITORING
                activeSignals.clear()
                _decisionFlow.tryEmit(FusionDecision.CandidateExpired)
            }
        }
    }

    internal fun evaluateConfirmationRule(): Boolean {
        val hasKeyword = activeSignals.any { it.detectorType == DetectorType.KEYWORD && it.confidence >= config.keywordConfidenceThreshold }
        val hasScream = activeSignals.any { it.detectorType == DetectorType.SCREAM && it.confidence >= config.screamConfidenceThreshold }
        val hasMotion = activeSignals.any { it.detectorType == DetectorType.MOTION }

        return when (config.signalCombinationRule.uppercase()) {
            "KEYWORD OR (SCREAM AND MOTION)" -> hasKeyword || (hasScream && hasMotion)
            "KEYWORD" -> hasKeyword
            "SCREAM AND MOTION" -> hasScream && hasMotion
            else -> hasKeyword || (hasScream && hasMotion)
        }
    }

    private fun cleanExpiredSignals(currentTime: Long) {
        val windowMs = config.confirmationWindowSeconds * 1000L
        activeSignals.removeIf { currentTime - it.timestamp > windowMs }
    }

    fun resetState() {
        currentState = IncidentState.MONITORING
        activeSignals.clear()
    }
}
