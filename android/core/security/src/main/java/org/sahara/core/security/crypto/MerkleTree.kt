package org.sahara.core.security.crypto

import java.security.MessageDigest

object MerkleTree {

    fun computeSha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun buildMerkleRoot(leafHashes: List<String>): String {
        if (leafHashes.isEmpty()) return computeSha256("EMPTY_TREE".toByteArray())
        if (leafHashes.size == 1) return leafHashes.first()

        var currentLevel = leafHashes
        while (currentLevel.size > 1) {
            val nextLevel = mutableListOf<String>()
            for (i in currentLevel.indices step 2) {
                val left = currentLevel[i]
                val right = if (i + 1 < currentLevel.size) currentLevel[i + 1] else left
                val combinedHash = computeSha256((left + right).toByteArray())
                nextLevel.add(combinedHash)
            }
            currentLevel = nextLevel
        }
        return currentLevel.first()
    }
}
