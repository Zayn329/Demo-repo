package org.sahara.services.mesh.relay

import org.sahara.services.mesh.models.MeshPacket
import java.util.concurrent.ConcurrentHashMap

class MeshDeduplicationCache(private val maxCapacity: Int = 1000) {

    private val seenPacketIds = ConcurrentHashMap.newKeySet<String>()

    fun hasSeen(packetId: String): Boolean {
        return seenPacketIds.contains(packetId)
    }

    fun markSeen(packetId: String): Boolean {
        if (seenPacketIds.size >= maxCapacity) {
            seenPacketIds.clear() // Cache rotation
        }
        return seenPacketIds.add(packetId)
    }

    fun clear() {
        seenPacketIds.clear()
    }
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
