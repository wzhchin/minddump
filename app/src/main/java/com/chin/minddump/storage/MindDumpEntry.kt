package com.chin.minddump.storage

import java.io.File

/**
 * Represents a single entry in MindDump — the in-memory domain type served
 * to the UI. There are only two physical kinds: a file (a note) and a directory
 * (a group). Comments are removed. The DB has no `role` column; kind is
 * expressed by [type], which carries a GROUP value for directory containers.
 *
 * Identity is the file path: the OS guarantees uniqueness within a directory,
 * and the path is what the user sees, `grep`s, and moves. There is no
 * synthesized `tid` — see `restrained-file-storage` for why it was removed.
 * Group membership is positional: a note "is in" a group because its file sits
 * in that group directory, captured here as [groupPath] (the owning group
 * directory's absolute path, null at the month-bucket root). Groups are
 * single-level. A `workDir` move always triggers a full DB rebuild, so absolute
 * paths re-root at rebuild time without staleness.
 *
 * [timestamp] is recovered from the filename (the leading `yyMM-dd-HHMMSS`
 * segment) for display and sorting — it is NOT an identity.
 */
data class MindDumpEntry(
    val file: File,
    val type: EntryType,
    val space: Space,
    val monthFolder: String, // YYYY-MM
    val role: EntryRole = EntryRole.FILE, // in-memory only; NOT a DB column. GROUP→type.
    val groupPath: String? = null, // Owning group dir absolute path; null at month-top root
    val isPinned: Boolean = false, // Encoded as a `9999-` filename prefix
    val todoState: TodoState = TodoState.NONE, // Encoded as a status token in the filename
    val tags: List<String> = emptyList(), // From the `.meta` sidecar (served from the tags table)
    val events: List<EntryEvent> = emptyList(), // From the `.meta` sidecar (served from the events table)
    val metaEncrypted: Boolean = false, // True when a Private sidecar is not yet decrypted
) {
    /**
     * The second-resolution timestamp segment (`yyMM-dd-HHmmss`) parsed from the
     * filename. For display/sorting only — never an identity.
     */
    val timestamp: String
        get() = FileMetadata.fromFile(file)?.timestamp ?: ""
}

enum class Space(
    val folderName: String,
) {
    PUBLIC("Public"),
    PRIVATE("Private"),
}

/**
 * A file currently held in `.trash/`. The original live location is recoverable
 * from the file's preserved relative path beneath its space; [trashedAt] (the
 * file's mtime, refreshed at trash time) drives retention. Content is never
 * decrypted — [type] is derived from the filename.
 */
data class TrashedItem(
    val file: File,
    val type: EntryType,
    val space: Space,
    val trashedAt: Long,
)

enum class EntryType {
    TEXT,
    RECORDING,
    PHOTO,
    VIDEO,
    FILE,
    GROUP, // Directory containers — rows in `entries` whose membership is via groupPath
    UNKNOWN,
    ;

    companion object {
        /** Type detection purely by file extension. Never produces GROUP (that is dir-only). */
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
