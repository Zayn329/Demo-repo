package org.sahara.core.security.crypto

import org.sahara.core.security.interfaces.EncryptedFileStorage
import java.io.DataInputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AesGcmFileStorage : EncryptedFileStorage {

    private val secureRandom = SecureRandom()

    override fun encryptAndSave(dataStream: InputStream, outputStream: OutputStream, secretKey: SecretKey): String {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)
        outputStream.write(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        CipherOutputStream(outputStream, cipher).use { cipherOut ->
            dataStream.copyTo(cipherOut)
        }
        return "AES-256-GCM"
    }

    override fun decryptAndRead(inputStream: InputStream, secretKey: SecretKey): ByteArray {
        val dataIn = DataInputStream(inputStream)
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        dataIn.readFully(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        CipherInputStream(dataIn, cipher).use { cipherIn ->
            return cipherIn.readBytes()
        }
    }

    companion object {
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}
