package org.sahara.services.detection.log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.sahara.services.detection.models.SignalResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class DetectionLogEvent(
    val id: String = UUID.randomUUID().toString(),
    val signal: SignalResult,
    val audioData: ShortArray? = null,
    val timestampFormatted: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(signal.timestamp))
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DetectionLogEvent
        if (id != other.id) return false
        if (signal != other.signal) return false
        if (audioData != null) {
            if (other.audioData == null) return false
            if (!audioData.contentEquals(other.audioData)) return false
        } else if (other.audioData != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + signal.hashCode()
        result = 31 * result + (audioData?.contentHashCode() ?: 0)
        return result
    }
}

object DetectionLogManager {
    private val _events = MutableStateFlow<List<DetectionLogEvent>>(emptyList())
    val events: StateFlow<List<DetectionLogEvent>> = _events.asStateFlow()

    fun logEvent(signal: SignalResult, pcmAudio: ShortArray? = null) {
        val newEvent = DetectionLogEvent(
            signal = signal,
            audioData = pcmAudio?.clone()
        )
        _events.value = listOf(newEvent) + _events.value
    }

    fun clearLogs() {
        _events.value = emptyList()
    }
}
