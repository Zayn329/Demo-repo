package org.sahara.services.detection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.sahara.core.domain.models.DetectorType
import org.sahara.services.detection.detectors.DspSanityStatus
import org.sahara.services.detection.detectors.ScreamDetector
import org.sahara.services.detection.models.DetectionConfig
import org.sahara.services.detection.models.SignalResult
import org.sahara.services.detection.tflite.ScreamInferenceResult
import org.sahara.services.detection.tflite.TFLiteScreamClassifier

class ScreamDetectorUnitTest {

    private lateinit var config: DetectionConfig
    private lateinit var screamDetector: ScreamDetector

    @Before
    fun setup() {
        config = DetectionConfig(screamConfidenceThreshold = 0.30f)
        screamDetector = ScreamDetector(config)
    }

    @Test
    fun testSilenceYieldsNoScream() {
        val silenceAudio = ShortArray(1600) { 0 }
        val score = screamDetector.processAudioChunk(silenceAudio, 16000)
        assertTrue("Silence should yield low confidence score", score < screamDetector.enterThreshold)
        assertFalse("Silence should not enter scream state", screamDetector.isInScreamState)
        val features = screamDetector.analyzeAcousticFeatures(silenceAudio, 16000)
        assertEquals(DspSanityStatus.SILENT_REJECT, features.status)
    }

    @Test
    fun testLowLevelBackgroundNoiseNoScream() {
        val bgNoise = ShortArray(1600) { (it % 200 - 100).toShort() }
        val score = screamDetector.processAudioChunk(bgNoise, 16000)
        assertTrue("Background noise confidence should remain below enter threshold", score < screamDetector.enterThreshold)
        assertFalse("Background noise should not enter scream state", screamDetector.isInScreamState)
    }

    @Test
    fun testLoudNotificationTransientRejectedByDSP() {
        // High peak amplitude with extremely short pulse/high crest factor
        val notificationImpulse = ShortArray(1600) { i -> if (i in 100..105) 30000.toShort() else 0 }
        val features = screamDetector.analyzeAcousticFeatures(notificationImpulse, 16000)

        assertEquals("Transient high peak impulse should be classified as TRANSIENT_REJECT by DSP", DspSanityStatus.TRANSIENT_REJECT, features.status)

        val score = screamDetector.processAudioChunk(notificationImpulse, 16000)
        assertTrue("Notification transient score should stay well below threshold", score < screamDetector.enterThreshold)
        assertFalse("Notification transient should not trigger scream state", screamDetector.isInScreamState)
    }

    @Test
    fun testClapOrObjectImpactTransientRejected() {
        // Impact sample: single sharp spike then immediate decay
        val impactAudio = ShortArray(1600) { i -> if (i == 50) 31000.toShort() else if (i == 51) (-25000).toShort() else 0 }
        val features = screamDetector.analyzeAcousticFeatures(impactAudio, 16000)

        assertEquals(DspSanityStatus.TRANSIENT_REJECT, features.status)
    }

    @Test
    fun testNormalSpeechPassesDSPWithoutTriggeringFallback() {
        // Speech sample: moderate ZCR, moderate RMS
        val speechAudio = ShortArray(1600) { i -> (Math.sin(i * 0.05) * 3000).toInt().toShort() }
        val features = screamDetector.analyzeAcousticFeatures(speechAudio, 16000)

        assertEquals(DspSanityStatus.PASS, features.status)
        val score = screamDetector.processAudioChunk(speechAudio, 16000)
        assertFalse("Normal speech in fallback mode should not exceed threshold without sustained scream features", screamDetector.isInScreamState)
    }

    @Test
    fun testYamnetNegativeWithHighDspEnergyMustNotTriggerScream() = runBlocking {
        // Mock a healthy YAMNet classifier returning 0.05 scream confidence
        val fakeClassifier = object : TFLiteScreamClassifier(null) {
            override fun classifyAudioFrameDetailed(audioBuffer: ShortArray, sampleRate: Int): ScreamInferenceResult {
                return ScreamInferenceResult(
                    maxScreamScore = 0.05f,
                    winningLabel = "speech",
                    winningScore = 0.85f,
                    isSuccess = true
                )
            }
        }
        screamDetector.tfliteClassifier = fakeClassifier

        var emittedSignal: SignalResult? = null
        val collectJob = launch(Dispatchers.Unconfined) {
            screamDetector.detectionFlow.collect { emittedSignal = it }
        }

        val loudAudio = ShortArray(1600) { i -> if (i % 4 == 0) 28000.toShort() else (-28000).toShort() }
        val score = screamDetector.processAudioChunk(loudAudio, 16000)

        assertEquals("NORMAL_ML_YAMNET", screamDetector.modeStatus)
        assertTrue("Final score must reflect low YAMNet confidence (0.05), not high DSP energy", score <= 0.05f)
        assertFalse("YAMNet negative + DSP high MUST NOT produce scream state", screamDetector.isInScreamState)

        collectJob.cancel()
    }

    @Test
    fun testYamnetPositiveWithReasonableDspAllowsScream() = runBlocking {
        // Mock YAMNet returning 0.55 scream confidence
        val fakeClassifier = object : TFLiteScreamClassifier(null) {
            override fun classifyAudioFrameDetailed(audioBuffer: ShortArray, sampleRate: Int): ScreamInferenceResult {
                return ScreamInferenceResult(
                    maxScreamScore = 0.55f,
                    winningLabel = "scream",
                    winningScore = 0.55f,
                    isSuccess = true
                )
            }
        }
        screamDetector.tfliteClassifier = fakeClassifier

        val screamAudio = ShortArray(1600) { i -> (Math.sin(i * 0.2) * 8000).toInt().toShort() }

        // Process audio chunks across frames to allow EMA smoothing to ramp up and trigger
        repeat(4) { screamDetector.processAudioChunk(screamAudio, 16000) }

        assertTrue("YAMNet positive with reasonable DSP allows scream trigger after EMA ramp-up", screamDetector.isInScreamState)
    }

    @Test
    fun testYamnetPositiveWithTransientDspSuppressesScream() {
        val fakeClassifier = object : TFLiteScreamClassifier(null) {
            override fun classifyAudioFrameDetailed(audioBuffer: ShortArray, sampleRate: Int): ScreamInferenceResult {
                return ScreamInferenceResult(
                    maxScreamScore = 0.40f,
                    winningLabel = "scream",
                    winningScore = 0.40f,
                    isSuccess = true
                )
            }
        }
        screamDetector.tfliteClassifier = fakeClassifier

        // Audio has sharp transient impulse -> TRANSIENT_REJECT
        val transientAudio = ShortArray(1600) { i -> if (i == 100) 30000.toShort() else 0 }

        val score = screamDetector.processAudioChunk(transientAudio, 16000)
        assertTrue("Score should be suppressed to <= 0.10 when transient rejected", score <= 0.10f)
        assertFalse("Transient reject suppresses YAMNet activation", screamDetector.isInScreamState)
    }

    @Test
    fun testDegradedDSPFallbackModeRequiresStrongerPersistence() {
        screamDetector.tfliteClassifier = null // Force YAMNet unavailable

        val loudScreamAudio = ShortArray(1600) { i -> (Math.sin(i * 0.2) * 12000).toInt().toShort() }

        // Frame 1
        screamDetector.processAudioChunk(loudScreamAudio, 16000)
        assertEquals("DEGRADED_DSP_FALLBACK", screamDetector.modeStatus)
        assertFalse("1-frame DSP spike in fallback mode MUST NOT trigger", screamDetector.isInScreamState)

        // Frame 2
        screamDetector.processAudioChunk(loudScreamAudio, 16000)
        assertFalse("2 frames in fallback mode MUST NOT trigger", screamDetector.isInScreamState)

        // Repeat processing to verify persistence requirement
        repeat(5) { screamDetector.processAudioChunk(loudScreamAudio, 16000) }

        assertTrue("Fallback mode triggers after sustained consecutive frames of DSP score", screamDetector.isInScreamState)
    }

    @Test
    fun testHysteresisAndCooldownRemainFunctional() {
        val screamAudio = ShortArray(1600) { i -> (Math.sin(i * 0.2) * 12000).toInt().toShort() }
        val mediumAudio = ShortArray(1600) { i -> (Math.sin(i * 0.2) * 5000).toInt().toShort() }

        screamDetector.tfliteClassifier = null

        // Trigger scream in fallback mode
        repeat(6) { screamDetector.processAudioChunk(screamAudio, 16000) }
        assertTrue(screamDetector.isInScreamState)

        // Drop to medium audio
        screamDetector.processAudioChunk(mediumAudio, 16000)
        assertTrue("Hysteresis keeps detector in scream state above exit threshold", screamDetector.isInScreamState)
    }
}
