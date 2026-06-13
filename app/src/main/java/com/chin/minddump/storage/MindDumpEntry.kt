package com.chin.minddump.storage

import java.io.File

/**
 * Represents a single entry (file) in MindDump.
 */
data class MindDumpEntry(
    val file: File,
    val type: EntryType,
    val space: Space,
    val monthFolder: String, // YYYY-MM
    val timestamp: String, // yymm-dd-HHMMSS
    val role: EntryRole = EntryRole.FILE,
    val targetTimestamp: String? = null, // For comments: the target's timestamp prefix
    val groupPath: String? = null,      // For files inside a group: the group directory path
)

enum class Space(
    val folderName: String,
) {
    PUBLIC("Public"),
    PRIVATE("Private"),
}

enum class EntryType {
    TEXT,
    RECORDING,
    PHOTO,
    VIDEO,
    FILE,
    UNKNOWN,
    ;

    companion object {
        /** Type detection purely by file extension. */
        fun fromExtension(ext: String): EntryType = when (ext.lowercase()) {
            "md" -> TEXT
            "m4a", "aac" -> RECORDING
            "jpg", "jpeg", "png" -> PHOTO
            "mp4" -> VIDEO
            else -> FILE
        }
    }
}
