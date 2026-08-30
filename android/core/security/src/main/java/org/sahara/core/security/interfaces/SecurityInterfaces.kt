package org.sahara.core.security.interfaces

import java.io.InputStream
import java.io.OutputStream
import javax.crypto.SecretKey

interface KeyStorageManager {
    fun getOrCreateMasterKey(alias: String): SecretKey
    fun generatePerIncidentDataKey(): SecretKey
    fun wrapKey(dataKey: SecretKey, masterKey: SecretKey): ByteArray
    fun unwrapKey(wrappedKey: ByteArray, masterKey: SecretKey): SecretKey
    fun signData(alias: String, data: ByteArray): ByteArray
    fun verifySignature(alias: String, data: ByteArray, signature: ByteArray): Boolean
}

interface EncryptedFileStorage {
    fun encryptAndSave(dataStream: InputStream, outputStream: OutputStream, secretKey: SecretKey): String
    fun decryptAndRead(inputStream: InputStream, secretKey: SecretKey): ByteArray
}
