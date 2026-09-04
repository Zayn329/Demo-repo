package org.sahara.services.mesh.relay

import org.sahara.services.mesh.models.MeshPacket
import java.util.concurrent.ConcurrentHashMap

class MeshDeduplicationCache(
    private val maxCapacity: Int = 1000,
    private val ttlMillis: Long = 10 * 60 * 1000L
) {

    private val seenPacketTimestamps = ConcurrentHashMap<String, Long>()

    fun hasSeen(packetId: String): Boolean {
        val timestamp = seenPacketTimestamps[packetId] ?: return false
        if (System.currentTimeMillis() - timestamp > ttlMillis) {
            seenPacketTimestamps.remove(packetId)
            return false
        }
        return true
    }

    fun markSeen(packetId: String): Boolean {
        val now = System.currentTimeMillis()
        evictExpired(now)
        if (hasSeen(packetId)) {
            return false
        }
        if (seenPacketTimestamps.size >= maxCapacity) {
            evictLru()
        }
        return seenPacketTimestamps.putIfAbsent(packetId, now) == null
    }

    internal fun evictExpired(now: Long = System.currentTimeMillis()) {
        val iterator = seenPacketTimestamps.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > ttlMillis) {
                iterator.remove()
            }
        }
    }

    internal fun evictLru() {
        val oldest = seenPacketTimestamps.minByOrNull { it.value }
        if (oldest != null) {
            seenPacketTimestamps.remove(oldest.key)
        }
    }

    fun clear() {
        seenPacketTimestamps.clear()
    }

    fun size(): Int = seenPacketTimestamps.size
}

class NearbyConnectionsMeshRelay(
    private val deduplicationCache: MeshDeduplicationCache = MeshDeduplicationCache()
) {

    private val relayedPackets = mutableListOf<MeshPacket>()

    fun processIncomingPacket(packet: MeshPacket): MeshRelayResult {
        if (deduplicationCache.hasSeen(packet.packetId)) {
            return MeshRelayResult.DUPLICATE_IGNORED
        }
        deduplicationCache.markSeen(packet.packetId)

        if (packet.hopCount >= packet.maxHops) {
            return MeshRelayResult.HOP_LIMIT_EXCEEDED
        }

        val incrementedPacket = packet.copy(hopCount = packet.hopCount + 1)
        relayedPackets.add(incrementedPacket)
        return MeshRelayResult.ACCEPTED_FOR_RELAY(incrementedPacket)
    }

    fun getRelayedPackets(): List<MeshPacket> = relayedPackets.toList()
}

sealed class MeshRelayResult {
    object DUPLICATE_IGNORED : MeshRelayResult()
    object HOP_LIMIT_EXCEEDED : MeshRelayResult()
    data class ACCEPTED_FOR_RELAY(val forwardedPacket: MeshPacket) : MeshRelayResult()
}
