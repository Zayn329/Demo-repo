package org.sahara.services.detection.detectors

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.sahara.core.domain.models.DetectorType
import org.sahara.services.detection.models.DetectionConfig
import org.sahara.services.detection.models.SignalResult

class KeywordDetector(
    private val config: DetectionConfig,
    var tfliteClassifier: org.sahara.services.detection.tflite.TFLiteSpeechCommandsClassifier? = null
) {

    private val _detectionFlow = MutableSharedFlow<SignalResult>(extraBufferCapacity = 64)
    val detectionFlow: Flow<SignalResult> = _detectionFlow.asSharedFlow()

    val isModelLoaded: Boolean
        get() = tfliteClassifier?.isModelLoaded ?: true

    val modelVersion: String
        get() = tfliteClassifier?.modelVersion ?: "TFLite-SpeechCommands-DSP-v1.0"

    fun processAudioChunk(audioBuffer: ShortArray, sampleRate: Int = 16000): Float {
        val tfliteConfidence = tfliteClassifier?.classifyAudioFrame(audioBuffer, sampleRate) ?: -1f
        val calculatedConfidence = if (tfliteConfidence >= 0f) {
            tfliteConfidence
        } else {
            analyzeKeywordPcm(audioBuffer, sampleRate)
        }

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

class ScreamDetector(
    private val config: DetectionConfig,
    var tfliteClassifier: org.sahara.services.detection.tflite.TFLiteScreamClassifier? = null
) {

    private val _detectionFlow = MutableSharedFlow<SignalResult>(extraBufferCapacity = 64)
    val detectionFlow: Flow<SignalResult> = _detectionFlow.asSharedFlow()

    val isModelLoaded: Boolean
        get() = tfliteClassifier?.isModelLoaded ?: false

    val modelVersion: String
        get() = tfliteClassifier?.modelVersion ?: "Hybrid-DSP-Heuristic-Fallback-v1.0"

    // Temporal smoothing & hysteresis properties
    var smoothedScore: Float = 0f
        private set

    var isInScreamState: Boolean = false
        private set

    var consecutivePositiveFrames: Int = 0
        private set

    var modeStatus: String = "DEGRADED_DSP_FALLBACK"
        private set

    // Configurable thresholds for hysteresis & debounce
    var enterThreshold: Float = config.screamConfidenceThreshold
    var exitThreshold: Float = (config.screamConfidenceThreshold * 0.5f).coerceAtLeast(0.12f)
    var emaAlpha: Float = 0.35f
    var requiredConsecutiveFrames: Int = 2
    var cooldownMs: Long = 1500L

    private var lastTriggerTimestamp: Long = 0L

    fun processAudioChunk(audioBuffer: ShortArray, sampleRate: Int = 16000): Float {
        val currentTime = System.currentTimeMillis()
        val dspConfidence = analyzeHybridAcousticFeatures(audioBuffer, sampleRate)

        val detailedResult = tfliteClassifier?.classifyAudioFrameDetailed(audioBuffer, sampleRate)

        val rawConfidence: Float
        val activeLabel: String

        if (detailedResult != null && detailedResult.isSuccess) {
            modeStatus = "NORMAL_ML_YAMNET"

            // Acoustic sanity check: if audio is near-silent, prevent model noise spikes
            val sanitizedModelScore = if (dspConfidence < 0.05f) {
                (detailedResult.maxScreamScore * 0.2f).coerceAtMost(0.10f)
            } else {
                detailedResult.maxScreamScore
            }

            rawConfidence = (dspConfidence * 0.3f + sanitizedModelScore * 0.7f).coerceAtMost(1.0f)
            activeLabel = if (detailedResult.maxScreamScore >= 0.20f) "yamnet_scream (${detailedResult.winningLabel})" else "yamnet_audio"
        } else if (detailedResult != null && detailedResult.errorMessage?.contains("Insufficient") == true) {
            modeStatus = "BUFFERING"
            rawConfidence = dspConfidence
            activeLabel = "buffering_dsp_scream"
        } else {
            modeStatus = "DEGRADED_DSP_FALLBACK"
            rawConfidence = dspConfidence
            activeLabel = "dsp_scream_high_pitch"
        }

        // Apply exponential moving average (EMA) temporal smoothing
        smoothedScore = emaAlpha * rawConfidence + (1f - emaAlpha) * smoothedScore

        // Hysteresis & debounce state evaluation
        val isCooldownActive = (currentTime - lastTriggerTimestamp) < cooldownMs

        if (!isInScreamState) {
            if (smoothedScore >= enterThreshold && !isCooldownActive) {
                consecutivePositiveFrames++
                if (consecutivePositiveFrames >= requiredConsecutiveFrames) {
                    isInScreamState = true
                    lastTriggerTimestamp = currentTime
                    _detectionFlow.tryEmit(
                        SignalResult(
                            detectorType = DetectorType.SCREAM,
                            confidence = smoothedScore,
                            label = activeLabel,
                            timestamp = currentTime
                        )
                    )
                }
            } else {
                consecutivePositiveFrames = 0
            }
        } else {
            if (smoothedScore < exitThreshold) {
                isInScreamState = false
                consecutivePositiveFrames = 0
            } else {
                // Re-emit sustained scream signal if score remains above entry threshold
                if (smoothedScore >= enterThreshold && (currentTime - lastTriggerTimestamp) >= 1000L) {
                    lastTriggerTimestamp = currentTime
                    _detectionFlow.tryEmit(
                        SignalResult(
                            detectorType = DetectorType.SCREAM,
                            confidence = smoothedScore,
                            label = activeLabel,
                            timestamp = currentTime
                        )
                    )
                }
            }
        }

        return smoothedScore
    }

    fun resetState() {
        smoothedScore = 0f
        isInScreamState = false
        consecutivePositiveFrames = 0
        lastTriggerTimestamp = 0L
    }

    internal fun analyzeHybridAcousticFeatures(audioBuffer: ShortArray, sampleRate: Int): Float {
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
