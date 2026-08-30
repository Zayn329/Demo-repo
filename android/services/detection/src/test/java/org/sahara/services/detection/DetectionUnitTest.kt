package org.sahara.services.detection

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.sahara.core.domain.models.DetectorType
import org.sahara.core.domain.models.IncidentState
import org.sahara.services.detection.detectors.KeywordDetector
import org.sahara.services.detection.detectors.MotionDetector
import org.sahara.services.detection.detectors.ScreamDetector
import org.sahara.services.detection.fusion.FusionDecision
import org.sahara.services.detection.fusion.SignalFusionEngine
import org.sahara.services.detection.models.DetectionConfig
import org.sahara.services.detection.models.SignalResult

class DetectionUnitTest {

    private lateinit var config: DetectionConfig
    private lateinit var keywordDetector: KeywordDetector
    private lateinit var screamDetector: ScreamDetector
    private lateinit var motionDetector: MotionDetector
    private lateinit var fusionEngine: SignalFusionEngine

    @Before
    fun setup() {
        config = DetectionConfig()
        keywordDetector = KeywordDetector(config)
        screamDetector = ScreamDetector(config)
        motionDetector = MotionDetector(config)
        fusionEngine = SignalFusionEngine(config)
    }

    @Test
    fun testKeywordDetectorThreshold() {
        val quietAudio = ShortArray(1600) { 100 }
        val confidenceLow = keywordDetector.analyzeKeywordPcm(quietAudio, 16000)
        assertTrue("Low energy should yield low confidence", confidenceLow < config.keywordConfidenceThreshold)

        val loudAudio = ShortArray(1600) { 20000 }
        val confidenceHigh = keywordDetector.analyzeKeywordPcm(loudAudio, 16000)
        assertTrue("Loud audio should exceed threshold", confidenceHigh >= config.keywordConfidenceThreshold)
    }

    @Test
    fun testScreamDetectorFrequencyAnalysis() {
        val nonScreamAudio = ShortArray(1600) { (it % 100).toShort() }
        val nonScreamConf = screamDetector.analyzeScreamAudio(nonScreamAudio, 16000)
        assertTrue("Patterned low amp audio should not be scream", nonScreamConf < config.screamConfidenceThreshold)

        val screamAudio = ShortArray(1600) { i ->
            if (i % 4 == 0) 25000.toShort() else (-25000).toShort()
        }
        val screamConf = screamDetector.analyzeScreamAudio(screamAudio, 16000)
        assertTrue("High zero-crossing and loud amplitude is scream", screamConf >= config.screamConfidenceThreshold)
    }

    @Test
    fun testSignalFusionRuleEvaluation() = runBlocking {
        assertEquals(IncidentState.MONITORING, fusionEngine.currentState)

        val keywordSignal = SignalResult(DetectorType.KEYWORD, 0.85f, "help", System.currentTimeMillis())
        fusionEngine.onSignalReceived(keywordSignal)

        assertEquals(IncidentState.CANDIDATE_INCIDENT, fusionEngine.currentState)
        assertTrue("Keyword signal alone satisfies default rule", fusionEngine.evaluateConfirmationRule())

        fusionEngine.resetState()
        val screamSignal = SignalResult(DetectorType.SCREAM, 0.80f, "scream", System.currentTimeMillis())
        fusionEngine.onSignalReceived(screamSignal)
        assertFalse("Scream alone does not satisfy default rule", fusionEngine.evaluateConfirmationRule())

        val motionSignal = SignalResult(DetectorType.MOTION, 0.90f, "impact", System.currentTimeMillis())
        fusionEngine.onSignalReceived(motionSignal)
        assertTrue("Scream AND Motion satisfies default rule", fusionEngine.evaluateConfirmationRule())
    }

    @Test
    fun testConfirmationTimeoutExpiration() {
        val keywordSignal = SignalResult(DetectorType.KEYWORD, 0.85f, "help", System.currentTimeMillis() - 10000L)
        fusionEngine.onSignalReceived(keywordSignal)

        fusionEngine.checkConfirmationTimeout(System.currentTimeMillis())
        assertEquals(IncidentState.MONITORING, fusionEngine.currentState)
    }
}
