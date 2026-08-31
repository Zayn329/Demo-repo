package org.sahara.services.detection.tflite

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class TFLiteScreamClassifier(context: Context? = null) {

    var isModelLoaded: Boolean = false
        private set

    var interpreter: Interpreter? = null
        private set

    var labels: List<String> = emptyList()
        private set

    val modelVersion: String = "YAMNet-TFLite-v1.0-AudioSet"

    init {
        if (context != null) {
            loadModelAndLabels(context)
        }
    }

    fun loadModelAndLabels(context: Context) {
        try {
            val assetManager = context.assets
            val fileDescriptor = assetManager.openFd("models/yamnet.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val modelBuffer: ByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            interpreter = Interpreter(modelBuffer)

            // Load labels
            labels = assetManager.open("models/yamnet_labels.txt").bufferedReader().useLines { lines ->
                lines.map { line ->
                    val parts = line.split(":", limit = 2)
                    if (parts.size == 2) parts[1].trim().removeSurrounding("\"") else line
                }.toList()
            }

            isModelLoaded = true
        } catch (e: Throwable) {
            isModelLoaded = false
            interpreter = null
        }
    }

    fun classifyAudioFrame(audioBuffer: ShortArray, sampleRate: Int = 16000): Float {
        if (!isModelLoaded || interpreter == null || audioBuffer.isEmpty()) {
            return -1f // Indicates degraded mode or model unavailable
        }

        return try {
            // YAMNet expects float input waveform in range [-1.0, 1.0]
            val inputBuffer = ByteBuffer.allocateDirect(audioBuffer.size * 4).order(ByteOrder.nativeOrder())
            for (sample in audioBuffer) {
                inputBuffer.putFloat((sample.toFloat() / 32768.0f).coerceIn(-1.0f, 1.0f))
            }
            inputBuffer.rewind()

            // Run inference
            val outputScores = Array(1) { FloatArray(labels.size.coerceAtLeast(521)) }
            interpreter?.run(inputBuffer, outputScores)

            // Evaluate scream / yell / shout classes (indices ~6: Shout, 9: Yell, 11: Screaming)
            val screamIndices = listOf(6, 9, 10, 11)
            var maxScreamScore = 0f
            for (idx in screamIndices) {
                if (idx < outputScores[0].size) {
                    val score = outputScores[0][idx]
                    if (score > maxScreamScore) {
                        maxScreamScore = score
                    }
                }
            }
            maxScreamScore
        } catch (e: Throwable) {
            -1f
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        isModelLoaded = false
    }
}
