package org.sahara.services.detection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.sahara.core.domain.models.DetectorType
import org.sahara.services.detection.detectors.ScreamDetector
import org.sahara.services.detection.models.DetectionConfig
import org.sahara.services.detection.models.SignalResult
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
    }

    @Test
    fun testLowLevelBackgroundNoiseNoScream() {
        val bgNoise = ShortArray(1600) { (it % 200 - 100).toShort() }
        val score = screamDetector.processAudioChunk(bgNoise, 16000)
        assertTrue("Background noise confidence should remain below enter threshold", score < screamDetector.enterThreshold)
        assertFalse("Background noise should not enter scream state", screamDetector.isInScreamState)
    }

    @Test
    fun testIsolatedLoudSpikeNoConfirmedScream() {
        val spikeAudio = ShortArray(1600) { i -> if (i % 4 == 0) 28000.toShort() else (-28000).toShort() }
        val quietAudio = ShortArray(1600) { 0 }

        screamDetector.processAudioChunk(spikeAudio, 16000)
        assertEquals(1, screamDetector.consecutivePositiveFrames)
        assertFalse("Single noisy spike should not immediately trigger confirmed scream state", screamDetector.isInScreamState)

        screamDetector.processAudioChunk(quietAudio, 16000)
        assertEquals(0, screamDetector.consecutivePositiveFrames)
        assertFalse("Single spike followed by silence should reset scream state", screamDetector.isInScreamState)
    }

    @Test
    fun testSustainedScreamTriggersEvent() = runBlocking {
        val screamAudio = ShortArray(1600) { i -> if (i % 4 == 0) 28000.toShort() else (-28000).toShort() }

        var emittedSignal: SignalResult? = null
        val collectJob = launch(Dispatchers.Unconfined) {
            screamDetector.detectionFlow.collect {
                emittedSignal = it
            }
        }

        screamDetector.processAudioChunk(screamAudio, 16000)
        screamDetector.processAudioChunk(screamAudio, 16000)

        assertTrue("Sustained scream audio should trigger scream state", screamDetector.isInScreamState)
        assertNotNull("SignalResult should be emitted on detectionFlow", emittedSignal)
        assertEquals(DetectorType.SCREAM, emittedSignal?.detectorType)
        assertTrue(emittedSignal!!.confidence >= screamDetector.enterThreshold)

        collectJob.cancel()
    }

    @Test
    fun testHysteresisPreventsRapidToggling() {
        val screamAudio = ShortArray(1600) { i -> if (i % 4 == 0) 28000.toShort() else (-28000).toShort() }
        val mediumAudio = ShortArray(1600) { i -> if (i % 8 == 0) 10000.toShort() else (-10000).toShort() }

        // Enter scream state
        screamDetector.processAudioChunk(screamAudio, 16000)
        screamDetector.processAudioChunk(screamAudio, 16000)
        assertTrue(screamDetector.isInScreamState)

        // Drop to medium audio (below enterThreshold 0.30, but above exitThreshold ~0.15)
        screamDetector.processAudioChunk(mediumAudio, 16000)
        assertTrue("Hysteresis should keep detector in scream state when score is above exit threshold", screamDetector.isInScreamState)
    }

    @Test
    fun testRingBufferAccumulationAndInsufficientAudioHandling() {
        val classifier = TFLiteScreamClassifier(null)
        val dummyAudio = ShortArray(1600) { 1000 }

        val detailedResult = classifier.classifyAudioFrameDetailed(dummyAudio)
        assertFalse("Classifier returns isSuccess = false when uninitialized or buffering", detailedResult.isSuccess)

        classifier.close()
    }

    @Test
    fun testDynamicLabelMappingForScreamCategories() {
        val classifier = TFLiteScreamClassifier(null)
        val labelsList = listOf(
            "Speech", "Music", "Silence", "Dog Bark", "Shout",
            "Cat Meow", "Screaming", "Bellow", "Yell", "Children shouting"
        )
        classifier.updateLabels(labelsList)

        val mappedIndices = classifier.screamLabelIndices
        assertTrue("Mapped indices should include Scream, Shout, Yell, Bellow, Children shouting", mappedIndices.containsAll(listOf(4, 6, 7, 8, 9)))

        classifier.close()
    }

    @Test
    fun testDegradedDSPModeStatus() {
        screamDetector.tfliteClassifier = null
        val dummyAudio = ShortArray(1600) { 500 }

        screamDetector.processAudioChunk(dummyAudio, 16000)
        assertEquals("DEGRADED_DSP_FALLBACK", screamDetector.modeStatus)
    }
}
