package com.chin.minddump.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages the Private space password using EncryptedSharedPreferences.
 * The password hash is stored securely with Android Keystore-backed encryption.
 */
class PasswordStore(
    context: Context
) {

    private val masterKey = MasterKey
        .Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "minddump_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    companion object {
        private const val KEY_PASSWORD_HASH = "private_password_hash"
        private const val KEY_HAS_PASSWORD = "has_password"
    }

    /**
     * Check if a password has been set for Private space.
     */
    fun hasPassword(): Boolean = prefs.getBoolean(KEY_HAS_PASSWORD, false)

    /**
     * Save the password (stores its hash, not plaintext).
     */
    fun savePassword(password: String) {
        val hash = hashPassword(password)
        prefs
            .edit()
            .putString(KEY_PASSWORD_HASH, hash)
            .putBoolean(KEY_HAS_PASSWORD, true)
            .apply()
    }

    /**
     * Verify a password against the stored hash.
     */
    fun verifyPassword(password: String): Boolean {
        if (!hasPassword()) return false
        val storedHash = prefs.getString(KEY_PASSWORD_HASH, null) ?: return false
        return hashPassword(password) == storedHash
    }

    /**
     * Clear the stored password.
     */
    fun clearPassword() {
        prefs
            .edit()
            .remove(KEY_PASSWORD_HASH)
            .putBoolean(KEY_HAS_PASSWORD, false)
            .apply()
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray(Charsets.UTF_8)
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }
}
