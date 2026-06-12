package com.chin.minddump.security

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class CryptoEngineTest {

    private lateinit var cryptoEngine: CryptoEngine
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        cryptoEngine = CryptoEngine()
        tempDir = File(System.getProperty("java.io.tmpdir"), "crypto_test_${System.nanoTime()}")
        tempDir.mkdirs()
    }

    private fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `encrypt then decrypt returns original data`() {
        val original = "Hello, Private Space!".toByteArray()
        val password = "test1234"
        val encrypted = cryptoEngine.encrypt(original, password)
        val decrypted = cryptoEngine.decrypt(encrypted, password)
        assertArrayEquals(original, decrypted)
        cleanup()
    }

    @Test
    fun `encrypted data is different from original`() {
        val original = "Secret data".toByteArray()
        val encrypted = cryptoEngine.encrypt(original, "password")
        assertFalse(original.contentEquals(encrypted))
        cleanup()
    }

    @Test(expected = Exception::class)
    fun `decrypt with wrong password throws exception`() {
        val original = "Secret".toByteArray()
        val encrypted = cryptoEngine.encrypt(original, "correct_password")
        cryptoEngine.decrypt(encrypted, "wrong_password")
    }

    @Test
    fun `encryptFile and decryptFile roundtrip`() {
        val sourceFile = File(tempDir, "test.txt").apply { writeText("Test file content") }
        val encryptedFile = File(tempDir, "test.txt.enc")
        val decryptedFile = File(tempDir, "test_decrypted.txt")
        val password = "file_password"

        cryptoEngine.encryptFile(sourceFile, encryptedFile, password)
        assertTrue(encryptedFile.exists())
        assertFalse(sourceFile.readBytes().contentEquals(encryptedFile.readBytes()))

        cryptoEngine.decryptFile(encryptedFile, decryptedFile, password)
        assertEquals("Test file content", decryptedFile.readText())
        cleanup()
    }

    @Test
    fun `isEncryptedFile returns true for enc files`() {
        val encFile = File(tempDir, "test.md.enc").apply { writeBytes(ByteArray(50)) }
        assertTrue(cryptoEngine.isEncryptedFile(encFile))
        cleanup()
    }

    @Test
    fun `isEncryptedFile returns false for non-enc files`() {
        val regularFile = File(tempDir, "test.md").apply { writeText("hello") }
        assertFalse(cryptoEngine.isEncryptedFile(regularFile))
        cleanup()
    }

    @Test
    fun `different passwords produce different ciphertext`() {
        val data = "Same data".toByteArray()
        val encrypted1 = cryptoEngine.encrypt(data, "password1")
        val encrypted2 = cryptoEngine.encrypt(data, "password2")
        assertFalse(encrypted1.contentEquals(encrypted2))
        cleanup()
    }

    @Test
    fun `same password different salts produce different ciphertext`() {
        val data = "Same data".toByteArray()
        val password = "same_password"
        val encrypted1 = cryptoEngine.encrypt(data, password)
        val encrypted2 = cryptoEngine.encrypt(data, password)
        // Different random salts should produce different ciphertext
        assertFalse(encrypted1.contentEquals(encrypted2))
        cleanup()
    }
}
