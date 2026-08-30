package org.sahara.services.detection.models

import org.sahara.core.domain.models.DetectorType

data class DetectionConfig(
    val triggerWords: List<String> = listOf("help", "save me", "stop"),
    val keywordConfidenceThreshold: Float = 0.50f,
    val screamConfidenceThreshold: Float = 0.50f,
    val motionGForceThreshold: Float = 2.0f,
    val signalCombinationRule: String = "KEYWORD OR (SCREAM AND MOTION)",
    val confirmationWindowSeconds: Int = 6
)

data class SignalResult(
    val detectorType: DetectorType,
    val confidence: Float,
    val label: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
