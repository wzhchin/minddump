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
    val groupPath: String? = null, // For files inside a group: the group directory path
    val isPinned: Boolean = false, // Encoded as a `9999-` filename prefix
    val todoState: TodoState = TodoState.NONE, // Encoded as a status token in the filename
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

/**
 * Todo status encoded as an uppercase token between the timestamp and the role
 * char in a filename. [NONE] writes no token — plain notes are not todos.
 * Only FILE and GROUP roles may carry a status; comments never do.
 */
enum class TodoState(
    val code: String?,
) {
    NONE(null),
    TODO("TODO"),
    DOING("DOING"),
    WAIT("WAIT"),
    DONE("DONE"),
    CANCEL("CANCEL"),
    ;

    companion object {
        fun fromCode(code: String?): TodoState = entries.firstOrNull { it.code == code } ?: NONE
    }
}
