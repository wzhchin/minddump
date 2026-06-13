package com.chin.minddump.security

import java.io.File
import java.security.SecureRandom
import timber.log.Timber
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-CBC encryption engine with PBKDF2 key derivation.
 *
 * File format: [16 bytes salt][16 bytes IV][encrypted data]
 */
class CryptoEngine {

    private val secureRandom = SecureRandom()
    private val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

    companion object {
        private const val SALT_LENGTH = 16
        private const val IV_LENGTH = 16
        private const val KEY_LENGTH = 256
        private const val ITERATIONS = 100_000
        private const val ALGORITHM = "AES"
        private const val KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA256"
    }

    /**
     * Derive a 256-bit AES key from password and salt using PBKDF2.
     */
    fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, ALGORITHM)
    }

    /**
     * Encrypt data with a password.
     * Returns: [salt][IV][encrypted data]
     */
    fun encrypt(data: ByteArray, password: String): ByteArray {
        val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { secureRandom.nextBytes(it) }
        val key = deriveKey(password, salt)

        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        val encrypted = cipher.doFinal(data)

        return salt + iv + encrypted
    }

    /**
     * Decrypt data with a password.
     * Input format: [salt][IV][encrypted data]
     */
    fun decrypt(data: ByteArray, password: String): ByteArray {
        require(data.size > SALT_LENGTH + IV_LENGTH) { "Data too short to be encrypted" }

        val salt = data.copyOfRange(0, SALT_LENGTH)
        val iv = data.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)
        val encrypted = data.copyOfRange(SALT_LENGTH + IV_LENGTH, data.size)
        val key = deriveKey(password, salt)

        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        return cipher.doFinal(encrypted)
    }

    /**
     * Encrypt a file. Writes output to [destFile].
     */
    fun encryptFile(sourceFile: File, destFile: File, password: String) {
        Timber.d("Encrypting file: %s", sourceFile.name)
        val data = sourceFile.readBytes()
        val encrypted = encrypt(data, password)
        destFile.writeBytes(encrypted)
    }

    /**
     * Decrypt a file. Writes output to [destFile].
     */
    fun decryptFile(sourceFile: File, destFile: File, password: String) {
        val data = sourceFile.readBytes()
        val decrypted = decrypt(data, password)
        destFile.writeBytes(decrypted)
    }

    /**
     * Check if a file appears to be encrypted (has the right header size).
     */
    fun isEncryptedFile(file: File): Boolean = file.exists() && file.length() > SALT_LENGTH + IV_LENGTH && file.name.endsWith(".enc")
}
