package org.sahara.services.detection.tflite

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

data class ScreamInferenceResult(
    val maxScreamScore: Float,
    val winningLabel: String,
    val winningScore: Float,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

class TFLiteScreamClassifier(context: Context? = null) {

    companion object {
        private const val TAG = "TFLiteScreamClassifier"
        const val DEFAULT_SAMPLE_RATE = 16000
        const val DEFAULT_WINDOW_SAMPLES = 15600 // YAMNet expected 0.975s window
        const val DEFAULT_CLASS_COUNT = 521
    }

    var isModelLoaded: Boolean = false
        private set

    var interpreter: Interpreter? = null
        private set

    var labels: List<String> = emptyList()
        private set

    var screamLabelIndices: List<Int> = listOf(6, 7, 9, 10, 11) // YAMNet AudioSet Audio indices for Scream, Bellow, Yell, Children shouting, Screaming
        private set

    var requiredSampleCount: Int = DEFAULT_WINDOW_SAMPLES
        private set

    var outputClassCount: Int = DEFAULT_CLASS_COUNT
        private set

    val modelVersion: String = "YAMNet-TFLite-v1.0-AudioSet"

    // Ring buffer to accumulate streaming microphone audio chunks
    private val bufferLock = ReentrantLock()
    private var sampleBuffer = ShortArray(DEFAULT_WINDOW_SAMPLES)
    private var writeIndex = 0
    private var accumulatedSampleCount = 0

    // Reusable buffers for zero-allocation hot-loop inference
    private var directInputBuffer: ByteBuffer? = null
    private var outputScoresArray: Array<FloatArray>? = null

    init {
        if (context != null) {
            loadModelAndLabels(context)
        }
    }

    fun updateLabels(newLabels: List<String>) {
        labels = newLabels
        screamLabelIndices = parseScreamLabelIndices(newLabels)
    }

    internal fun parseScreamLabelIndices(rawLabels: List<String>): List<Int> {
        val targetKeywords = listOf(
            "scream", "screaming", "shout", "yell", "children shouting",
            "bellow", "groan", "distress vocal", "cry", "crying", "whimper"
        )
        val matchedIndices = rawLabels.mapIndexedNotNull { index, label ->
            val lowerLabel = label.lowercase()
            if (targetKeywords.any { kw -> lowerLabel.contains(kw) }) index else null
        }
        return if (matchedIndices.isNotEmpty()) matchedIndices else listOf(6, 7, 9, 10, 11)
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

            val options = Interpreter.Options().apply {
                setNumThreads(2)
            }
            val newInterpreter = Interpreter(modelBuffer, options)

            // Inspect input tensor shape dynamically
            val inputTensor = newInterpreter.getInputTensor(0)
            val inputShape = inputTensor.shape()
            requiredSampleCount = if (inputShape.isNotEmpty()) inputShape.last() else DEFAULT_WINDOW_SAMPLES

            // Inspect output tensor shape dynamically
            val outputTensor = newInterpreter.getOutputTensor(0)
            val outputShape = outputTensor.shape()
            outputClassCount = if (outputShape.size >= 2) outputShape[1] else DEFAULT_CLASS_COUNT

            // Allocate direct byte buffer and output array matching exact model dimensions
            directInputBuffer = ByteBuffer.allocateDirect(requiredSampleCount * 4).order(ByteOrder.nativeOrder())
            outputScoresArray = Array(1) { FloatArray(outputClassCount) }

            // Allocate ring buffer with capacity for window
            sampleBuffer = ShortArray(requiredSampleCount)
            writeIndex = 0
            accumulatedSampleCount = 0

            interpreter = newInterpreter

            // Load labels dynamically from assets
            val loadedLabels = assetManager.open("models/yamnet_labels.txt").bufferedReader().useLines { lines ->
                lines.map { line ->
                    val parts = line.split(":", limit = 2)
                    if (parts.size == 2) parts[1].trim().removeSurrounding("\"") else line
                }.toList()
            }
            updateLabels(loadedLabels)

            isModelLoaded = true
            Log.i(TAG, "YAMNet model loaded successfully. Input samples=$requiredSampleCount, Output classes=$outputClassCount, Scream indices=$screamLabelIndices")
        } catch (e: Throwable) {
            isModelLoaded = false
            interpreter = null
            Log.e(TAG, "Failed to load YAMNet TFLite model or labels: ${e.message}", e)
        }
    }

    /**
     * Appends short PCM samples from live microphone input into ring buffer.
     */
    fun appendAudioSamples(audioBuffer: ShortArray) {
        if (audioBuffer.isEmpty()) return
        bufferLock.withLock {
            val len = audioBuffer.size
            for (i in 0 until len) {
                sampleBuffer[writeIndex] = audioBuffer[i]
                writeIndex = (writeIndex + 1) % sampleBuffer.size
            }
            accumulatedSampleCount = (accumulatedSampleCount + len).coerceAtMost(sampleBuffer.size)
        }
    }

    /**
     * Extracts latest window of requiredSampleCount samples normalized to [-1.0, 1.0].
     */
    private fun getNormalizedWindow(outBuffer: ByteBuffer) {
        bufferLock.withLock {
            outBuffer.rewind()
            val capacity = sampleBuffer.size
            val startPos = if (accumulatedSampleCount < capacity) 0 else writeIndex
            for (i in 0 until capacity) {
                val idx = (startPos + i) % capacity
                val floatSample = (sampleBuffer[idx].toFloat() / 32768.0f).coerceIn(-1.0f, 1.0f)
                outBuffer.putFloat(floatSample)
            }
            outBuffer.rewind()
        }
    }

    /**
     * Executes inference on accumulated ring buffer samples.
     * Returns ScreamInferenceResult containing maxScreamScore and winning label.
     */
    fun classifyAudioFrame(audioBuffer: ShortArray, sampleRate: Int = DEFAULT_SAMPLE_RATE): Float {
        val result = classifyAudioFrameDetailed(audioBuffer, sampleRate)
        return if (result.isSuccess) result.maxScreamScore else -1f
    }

    fun classifyAudioFrameDetailed(audioBuffer: ShortArray, sampleRate: Int = DEFAULT_SAMPLE_RATE): ScreamInferenceResult {
        appendAudioSamples(audioBuffer)

        if (!isModelLoaded || interpreter == null) {
            return ScreamInferenceResult(
                maxScreamScore = -1f,
                winningLabel = "uninitialized",
                winningScore = 0f,
                isSuccess = false,
                errorMessage = "TFLite Scream Model is not loaded"
            )
        }

        if (accumulatedSampleCount < requiredSampleCount) {
            return ScreamInferenceResult(
                maxScreamScore = -1f,
                winningLabel = "buffering",
                winningScore = 0f,
                isSuccess = false,
                errorMessage = "Insufficient audio accumulated ($accumulatedSampleCount/$requiredSampleCount samples)"
            )
        }

        val inputBuf = directInputBuffer ?: return ScreamInferenceResult(-1f, "buffer_error", 0f, false, "Input buffer unallocated")
        val outputScores = outputScoresArray ?: return ScreamInferenceResult(-1f, "buffer_error", 0f, false, "Output scores unallocated")

        return try {
            getNormalizedWindow(inputBuf)

            interpreter?.run(inputBuf, outputScores)

            val scores = outputScores[0]
            var maxScreamScore = 0f
            var winningIdx = 0
            var maxOverallScore = 0f

            for (i in scores.indices) {
                val score = scores[i]
                if (score > maxOverallScore) {
                    maxOverallScore = score
                    winningIdx = i
                }
            }

            for (idx in screamLabelIndices) {
                if (idx < scores.size) {
                    val score = scores[idx]
                    if (score > maxScreamScore) {
                        maxScreamScore = score
                    }
                }
            }

            val winningLabel = if (winningIdx < labels.size) labels[winningIdx] else "class_$winningIdx"

            ScreamInferenceResult(
                maxScreamScore = maxScreamScore,
                winningLabel = winningLabel,
                winningScore = maxOverallScore,
                isSuccess = true
            )
        } catch (e: Throwable) {
            Log.e(TAG, "YAMNet inference exception: ${e.message}", e)
            ScreamInferenceResult(
                maxScreamScore = -1f,
                winningLabel = "error",
                winningScore = 0f,
                isSuccess = false,
                errorMessage = e.message ?: "Inference exception"
            )
        }
    }

    fun resetBuffer() {
        bufferLock.withLock {
            sampleBuffer.fill(0)
            writeIndex = 0
            accumulatedSampleCount = 0
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        isModelLoaded = false
        resetBuffer()
    }
}
