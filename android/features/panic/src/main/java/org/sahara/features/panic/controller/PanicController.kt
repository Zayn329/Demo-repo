package org.sahara.features.panic.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.sahara.features.incident.statemachine.IncidentStateMachine

sealed class PanicState {
    object Idle : PanicState()
    data class Countdown(val secondsRemaining: Int) : PanicState()
    object Active : PanicState()
    object Cancelled : PanicState()
}

class PanicController(
    private val stateMachine: IncidentStateMachine,
    private val cancellationWindowSeconds: Int = 5,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private var countdownJob: Job? = null

    private val _panicState = MutableStateFlow<PanicState>(PanicState.Idle)
    val panicState: StateFlow<PanicState> = _panicState.asStateFlow()

    fun triggerPanic(triggerSource: String = "IN_APP_BUTTON") {
        if (_panicState.value is PanicState.Active) return

        countdownJob?.cancel()
        countdownJob = scope.launch {
            for (i in cancellationWindowSeconds downTo 1) {
                _panicState.value = PanicState.Countdown(i)
                delay(1000L)
            }
            _panicState.value = PanicState.Active
            stateMachine.activateIncident(triggerSource)
        }
    }

    fun triggerPanicImmediately(triggerSource: String = "PHYSICAL_GESTURE") {
        countdownJob?.cancel()
        _panicState.value = PanicState.Active
        scope.launch {
            stateMachine.activateIncident(triggerSource)
        }
    }

    fun cancelPanic() {
        countdownJob?.cancel()
        val currentState = _panicState.value
        _panicState.value = PanicState.Cancelled
        scope.launch {
            if (currentState is PanicState.Active) {
                stateMachine.cancelIncident()
            }
        }
    }

    fun reset() {
        countdownJob?.cancel()
        _panicState.value = PanicState.Idle
    }
}
