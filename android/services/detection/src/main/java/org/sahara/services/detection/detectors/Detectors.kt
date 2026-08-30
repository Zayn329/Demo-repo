package org.sahara.services.detection.detectors

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.sahara.core.domain.models.DetectorType
import org.sahara.services.detection.models.DetectionConfig
import org.sahara.services.detection.models.SignalResult

class KeywordDetector(private val config: DetectionConfig) {

    private val _detectionFlow = MutableSharedFlow<SignalResult>(extraBufferCapacity = 64)
    val detectionFlow: Flow<SignalResult> = _detectionFlow.asSharedFlow()

    fun processAudioChunk(audioBuffer: ShortArray, sampleRate: Int = 16000): Float {
        val calculatedConfidence = analyzeKeywordPcm(audioBuffer, sampleRate)
        if (calculatedConfidence >= config.keywordConfidenceThreshold) {
            _detectionFlow.tryEmit(
                SignalResult(
                    detectorType = DetectorType.KEYWORD,
                    confidence = calculatedConfidence,
                    label = config.triggerWords.firstOrNull() ?: "help"
                )
            )
        }
        return calculatedConfidence
    }

    internal fun analyzeKeywordPcm(audioBuffer: ShortArray, sampleRate: Int): Float {
        if (audioBuffer.isEmpty() || sampleRate <= 0) return 0f
        var energy = 0.0
        for (sample in audioBuffer) {
            energy += (sample * sample).toDouble()
        }
        val rms = Math.sqrt(energy / audioBuffer.size)
        val normalizedEnergy = (rms / 32768.0).toFloat()
        return (normalizedEnergy * 3.0f).coerceAtMost(1.0f)
    }
}

class ScreamDetector(private val config: DetectionConfig) {

    private val _detectionFlow = MutableSharedFlow<SignalResult>(extraBufferCapacity = 64)
    val detectionFlow: Flow<SignalResult> = _detectionFlow.asSharedFlow()

    fun processAudioChunk(audioBuffer: ShortArray, sampleRate: Int = 16000): Float {
        val screamConfidence = analyzeScreamAudio(audioBuffer, sampleRate)
        if (screamConfidence >= config.screamConfidenceThreshold) {
            _detectionFlow.tryEmit(
                SignalResult(
                    detectorType = DetectorType.SCREAM,
                    confidence = screamConfidence,
                    label = "scream_high_pitch"
                )
            )
        }
        return screamConfidence
    }

    internal fun analyzeScreamAudio(audioBuffer: ShortArray, sampleRate: Int): Float {
        if (audioBuffer.isEmpty() || sampleRate <= 0) return 0f
        var zeroCrossings = 0
        var maxAmplitude = 0
        for (i in 0 until audioBuffer.size - 1) {
            val current = audioBuffer[i].toInt()
            val next = audioBuffer[i + 1].toInt()
            if ((current >= 0 && next < 0) || (current < 0 && next >= 0)) {
                zeroCrossings++
            }
            val absVal = Math.abs(current)
            if (absVal > maxAmplitude) {
                maxAmplitude = absVal
            }
        }
        val zcr = zeroCrossings.toFloat() / audioBuffer.size.toFloat()
        val amplitudeRatio = maxAmplitude.toFloat() / 32768.0f

        // High zero-crossing frequency (scream / high pitch) combined with vocal acoustic intensity
        if (zcr in 0.05f..0.65f && amplitudeRatio >= 0.15f) {
            return (amplitudeRatio * 2.0f).coerceAtMost(1.0f)
        }
        return (amplitudeRatio * 0.5f).coerceAtMost(0.29f)
    }
}

class MotionDetector(private val config: DetectionConfig) {

    private val _detectionFlow = MutableSharedFlow<SignalResult>(extraBufferCapacity = 64)
    val detectionFlow: Flow<SignalResult> = _detectionFlow.asSharedFlow()

    fun processSensorData(x: Float, y: Float, z: Float) {
        val gForce = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat() / 9.81f
        if (gForce >= config.motionGForceThreshold) {
            val confidence = (gForce / (config.motionGForceThreshold * 2f)).coerceAtMost(1.0f)
            _detectionFlow.tryEmit(
                SignalResult(
                    detectorType = DetectorType.MOTION,
                    confidence = confidence,
                    label = "impact_anomaly"
                )
            )
        }
    }
}
