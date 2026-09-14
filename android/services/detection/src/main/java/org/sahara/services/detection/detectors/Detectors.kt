package org.sahara.services.detection.detectors

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.sahara.core.domain.models.DetectorType
import org.sahara.services.detection.models.DetectionConfig
import org.sahara.services.detection.models.SignalResult

enum class DspSanityStatus {
    PASS,
    TRANSIENT_REJECT,
    SILENT_REJECT
}

data class DspAcousticFeatures(
    val rms: Float,
    val peakRatio: Float,
    val crestFactor: Float,
    val zcr: Float,
    val status: DspSanityStatus
)

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
        val dspFeatures = analyzeAcousticFeatures(audioBuffer, sampleRate)

        val detailedResult = tfliteClassifier?.classifyAudioFrameDetailed(audioBuffer, sampleRate)

        val rawConfidence: Float
        val yamnetScore: Float
        val dspStatusStr: String
        val activeLabel: String

        val isYamnetHealthy = detailedResult != null && detailedResult.isSuccess

        if (isYamnetHealthy) {
            modeStatus = "NORMAL_ML_YAMNET"
            yamnetScore = detailedResult!!.maxScreamScore
            dspStatusStr = dspFeatures.status.name

            // NORMAL_ML_YAMNET: YAMNet is authoritative. DSP is supporting sanity check.
            rawConfidence = when (dspFeatures.status) {
                DspSanityStatus.SILENT_REJECT -> yamnetScore.coerceAtMost(0.05f)
                DspSanityStatus.TRANSIENT_REJECT -> yamnetScore.coerceAtMost(0.10f)
                DspSanityStatus.PASS -> yamnetScore
            }
        } else if (detailedResult != null && detailedResult.errorMessage?.contains("Insufficient") == true) {
            modeStatus = "BUFFERING"
            yamnetScore = -1f
            dspStatusStr = dspFeatures.status.name
            rawConfidence = 0.0f
        } else {
            modeStatus = "DEGRADED_DSP_FALLBACK"
            yamnetScore = -1f
            dspStatusStr = dspFeatures.status.name

            // DEGRADED_DSP_FALLBACK mode: Conservative heuristic requiring sustained energy
            rawConfidence = if (dspFeatures.status == DspSanityStatus.PASS &&
                dspFeatures.rms >= 0.10f && dspFeatures.peakRatio >= 0.15f &&
                dspFeatures.zcr in 0.05f..0.50f && dspFeatures.crestFactor in 1.2f..3.8f
            ) {
                (dspFeatures.rms * 1.8f).coerceAtMost(0.80f)
            } else {
                0.0f
            }
        }

        // Apply exponential moving average (EMA) temporal smoothing
        smoothedScore = emaAlpha * rawConfidence + (1f - emaAlpha) * smoothedScore

        // Hysteresis & debounce state evaluation
        val isCooldownActive = (currentTime - lastTriggerTimestamp) < cooldownMs
        val effectiveReqFrames = if (modeStatus == "DEGRADED_DSP_FALLBACK") 4 else requiredConsecutiveFrames

        val currentStateStr: String

        if (!isInScreamState) {
            if (smoothedScore >= enterThreshold && !isCooldownActive) {
                consecutivePositiveFrames++
                if (consecutivePositiveFrames >= effectiveReqFrames) {
                    isInScreamState = true
                    currentStateStr = "TRIGGERED"
                    lastTriggerTimestamp = currentTime
                } else {
                    currentStateStr = "CANDIDATE"
                }
            } else {
                consecutivePositiveFrames = 0
                currentStateStr = "IDLE"
            }
        } else {
            if (smoothedScore < exitThreshold) {
                isInScreamState = false
                consecutivePositiveFrames = 0
                currentStateStr = "IDLE"
            } else {
                currentStateStr = "TRIGGERED"
            }
        }

        // Formulate diagnostic string for in-app Detection Log UI
        activeLabel = if (modeStatus == "NORMAL_ML_YAMNET") {
            "YAMNet: %.2f | DSP: %s | Final: %.2f | Mode: %s | State: %s".format(
                yamnetScore, dspStatusStr, smoothedScore, modeStatus, currentStateStr
            )
        } else if (modeStatus == "DEGRADED_DSP_FALLBACK") {
            "DSP: %.2f | Source: DSP_FALLBACK | Mode: %s | State: %s".format(
                rawConfidence, modeStatus, currentStateStr
            )
        } else {
            "Mode: %s | State: %s".format(modeStatus, currentStateStr)
        }

        // Emit signal event for detection flow / detection log when relevant decision occurs
        val isRelevantDecision = isInScreamState ||
                currentStateStr == "CANDIDATE" ||
                yamnetScore >= 0.12f ||
                rawConfidence >= 0.12f ||
                dspFeatures.status == DspSanityStatus.TRANSIENT_REJECT

        if (isRelevantDecision) {
            val isTriggeredEvent = isInScreamState && (currentTime - lastTriggerTimestamp < 1000L || consecutivePositiveFrames == effectiveReqFrames)
            if (isTriggeredEvent || currentStateStr == "CANDIDATE" || dspFeatures.status == DspSanityStatus.TRANSIENT_REJECT || yamnetScore >= 0.15f) {
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

        return smoothedScore
    }

    fun resetState() {
        smoothedScore = 0f
        isInScreamState = false
        consecutivePositiveFrames = 0
        lastTriggerTimestamp = 0L
    }

    fun analyzeAcousticFeatures(audioBuffer: ShortArray, sampleRate: Int = 16000): DspAcousticFeatures {
        if (audioBuffer.isEmpty() || sampleRate <= 0) {
            return DspAcousticFeatures(0f, 0f, 0f, 0f, DspSanityStatus.SILENT_REJECT)
        }

        var sumSquare = 0.0
        var maxAmplitude = 0
        var zeroCrossings = 0

        val size = audioBuffer.size
        for (i in 0 until size) {
            val sample = audioBuffer[i].toInt()
            sumSquare += sample.toDouble() * sample.toDouble()
            val absVal = Math.abs(sample)
            if (absVal > maxAmplitude) {
                maxAmplitude = absVal
            }
            if (i < size - 1) {
                val next = audioBuffer[i + 1].toInt()
                if ((sample >= 0 && next < 0) || (sample < 0 && next >= 0)) {
                    zeroCrossings++
                }
            }
        }

        val rms = Math.sqrt(sumSquare / size).toFloat()
        val normalizedRms = rms / 32768.0f
        val peakRatio = maxAmplitude.toFloat() / 32768.0f
        val crestFactor = if (normalizedRms > 0.0001f) peakRatio / normalizedRms else 0f
        val zcr = zeroCrossings.toFloat() / (size - 1).coerceAtLeast(1).toFloat()

        val status = when {
            normalizedRms < 0.012f && peakRatio < 0.025f -> DspSanityStatus.SILENT_REJECT
            crestFactor > 4.2f && peakRatio > 0.04f -> DspSanityStatus.TRANSIENT_REJECT
            zcr > 0.62f && peakRatio < 0.30f -> DspSanityStatus.TRANSIENT_REJECT
            else -> DspSanityStatus.PASS
        }

        return DspAcousticFeatures(
            rms = normalizedRms,
            peakRatio = peakRatio,
            crestFactor = crestFactor,
            zcr = zcr,
            status = status
        )
    }

    internal fun analyzeHybridAcousticFeatures(audioBuffer: ShortArray, sampleRate: Int): Float {
        val features = analyzeAcousticFeatures(audioBuffer, sampleRate)
        if (features.status == DspSanityStatus.PASS) {
            return (features.peakRatio * 2.0f).coerceAtMost(1.0f)
        }
        return (features.peakRatio * 0.5f).coerceAtMost(0.29f)
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
