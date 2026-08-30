package org.sahara.services.evidence.preroll

import java.util.concurrent.ConcurrentLinkedQueue

data class AudioChunk(
    val id: String,
    val data: ShortArray,
    val timestamp: Long = System.currentTimeMillis()
)

class BoundedAudioPreRollBuffer(private val maxBufferDurationMs: Long = 10000L) {

    private val queue = ConcurrentLinkedQueue<AudioChunk>()

    fun offerChunk(chunk: AudioChunk) {
        queue.add(chunk)
        pruneOldChunks(chunk.timestamp)
    }

    fun getBufferedChunks(currentTime: Long = System.currentTimeMillis()): List<AudioChunk> {
        pruneOldChunks(currentTime)
        return queue.toList()
    }

    fun clear() {
        queue.clear()
    }

    private fun pruneOldChunks(currentTime: Long) {
        val cutoff = currentTime - maxBufferDurationMs
        while (queue.isNotEmpty() && queue.peek()!!.timestamp < cutoff) {
            queue.poll()
        }
    }
}
