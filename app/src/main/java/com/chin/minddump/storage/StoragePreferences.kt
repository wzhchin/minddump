package com.chin.minddump.storage

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists the user-selected work directory across sessions.
 */
class StoragePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("minddump_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_WORK_DIR = "work_dir"
    }

    /**
     * Returns the saved work directory, or null if not yet configured.
     */
    fun getWorkDir(): String? {
        return prefs.getString(KEY_WORK_DIR, null)
    }

    /**
     * Save the work directory path.
     */
    fun setWorkDir(path: String) {
        prefs.edit().putString(KEY_WORK_DIR, path).apply()
    }

    /**
     * Returns true if a work directory has been configured.
     */
    fun isConfigured(): Boolean {
        return prefs.contains(KEY_WORK_DIR)
    }
}
