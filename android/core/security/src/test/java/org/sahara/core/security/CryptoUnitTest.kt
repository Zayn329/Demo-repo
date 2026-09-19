package org.sahara.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.sahara.core.security.crypto.AesGcmFileStorage
import org.sahara.core.security.crypto.KeyStorageManagerImpl
import org.sahara.core.security.crypto.MerkleTree
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class CryptoUnitTest {

    private val keyManager = KeyStorageManagerImpl()
    private val gcmStorage = AesGcmFileStorage()

    @Test
    fun testAesGcmEncryptionDecryptionRoundtrip() {
        val dataKey = keyManager.generatePerIncidentDataKey()
        val originalText = "Sensitive Evidence Data Payload 12345"
        val inputStream = ByteArrayInputStream(originalText.toByteArray())
        val outputStream = ByteArrayOutputStream()

        val format = gcmStorage.encryptAndSave(inputStream, outputStream, dataKey)
        assertEquals("AES-256-GCM", format)

        val encryptedBytes = outputStream.toByteArray()
        assertTrue(encryptedBytes.size > 12)

        val decryptedBytes = gcmStorage.decryptAndRead(ByteArrayInputStream(encryptedBytes), dataKey)
        assertEquals(originalText, String(decryptedBytes))
    }

    @Test
    fun testPersistentFallbackKeyStorageAcrossInstances() {
        val testDir = java.io.File(System.getProperty("java.io.tmpdir"), "test_fallback_keys_${System.currentTimeMillis()}")
        testDir.mkdirs()
        try {
            val manager1 = KeyStorageManagerImpl(fallbackDir = testDir)
            val masterKey1 = manager1.getOrCreateMasterKey("incident_master_alias")
            assertNotNull(masterKey1)

            val manager2 = KeyStorageManagerImpl(fallbackDir = testDir)
            val masterKey2 = manager2.getOrCreateMasterKey("incident_master_alias")
            assertNotNull(masterKey2)

            assertEquals("Persisted master key bytes must match across process restarts", masterKey1.encoded.toList(), masterKey2.encoded.toList())
        } finally {
            testDir.deleteRecursively()
        }
    }

    @Test
    fun testKeyWrappingUnwrapping() {
        val masterKey = keyManager.generatePerIncidentDataKey()
        val dataKey = keyManager.generatePerIncidentDataKey()

        val wrapped = keyManager.wrapKey(dataKey, masterKey)
        assertNotNull(wrapped)

        val unwrapped = keyManager.unwrapKey(wrapped, masterKey)
        assertEquals(dataKey.encoded.toList(), unwrapped.encoded.toList())
    }

    @Test
    fun testMerkleTreeRootAndTampering() {
        val hash1 = MerkleTree.computeSha256("chunk1".toByteArray())
        val hash2 = MerkleTree.computeSha256("chunk2".toByteArray())

        val root1 = MerkleTree.buildMerkleRoot(listOf(hash1, hash2))
        assertNotNull(root1)

        val tamperedHash2 = MerkleTree.computeSha256("tampered_chunk2".toByteArray())
        val root2 = MerkleTree.buildMerkleRoot(listOf(hash1, tamperedHash2))

        assertFalse("Tampered chunk must result in different Merkle root", root1 == root2)
    }
}
