package org.sahara.services.detection.tflite

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class TFLiteSpeechCommandsClassifier(context: Context? = null) {

    var isModelLoaded: Boolean = false
        private set

    var interpreter: Interpreter? = null
        private set

    var labels: List<String> = emptyList()
        private set

    val modelVersion: String = "TFLite-SpeechCommands-v1.0"

    init {
        if (context != null) {
            loadModelAndLabels(context)
        }
    }

    fun loadModelAndLabels(context: Context) {
        try {
            val assetManager = context.assets
            val fileDescriptor = assetManager.openFd("models/speech_commands.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val modelBuffer: ByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            interpreter = Interpreter(modelBuffer)

            labels = assetManager.open("models/speech_commands_labels.txt").bufferedReader().useLines { lines ->
                lines.map { it.trim() }.toList()
            }

            isModelLoaded = true
        } catch (e: Throwable) {
            isModelLoaded = false
            interpreter = null
        }
    }

    fun classifyAudioFrame(audioBuffer: ShortArray, sampleRate: Int = 16000): Float {
        if (!isModelLoaded || interpreter == null || audioBuffer.isEmpty()) {
            return -1f
        }

        return try {
            val inputBuffer = ByteBuffer.allocateDirect(audioBuffer.size * 4).order(ByteOrder.nativeOrder())
            for (sample in audioBuffer) {
                inputBuffer.putFloat((sample.toFloat() / 32768.0f).coerceIn(-1.0f, 1.0f))
            }
            inputBuffer.rewind()

            val outputScores = Array(1) { FloatArray(labels.size.coerceAtLeast(10)) }
            interpreter?.run(inputBuffer, outputScores)

            var maxConfidence = 0f
            for (score in outputScores[0]) {
                if (score > maxConfidence) {
                    maxConfidence = score
                }
            }
            maxConfidence
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
