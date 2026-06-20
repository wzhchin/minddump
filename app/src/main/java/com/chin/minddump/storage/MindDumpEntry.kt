package com.chin.minddump.storage

import java.io.File

/**
 * Represents a single entry (file) in MindDump — the in-memory domain type served
 * to the UI. It unifies rows from the `entries` and `comments` tables: a comment
 * is reconstructed with [role] == COMMENT so the existing comment UI works without
 * knowing which table it came from. The DB itself has no `role` column (entry kind
 * is expressed by [type], which gains a GROUP value for directory containers).
 *
 * Identity is [tid], the rebuild-stable epoch-millis primary key. Group membership
 * and nesting are expressed by [parentId] (the owning/nesting group's tid), which
 * replaces the former absolute-path `groupPath` string.
 */
data class MindDumpEntry(
    val file: File,
    val type: EntryType,
    val space: Space,
    val monthFolder: String, // YYYY-MM
    val tid: Long, // epoch-millis + same-second offset; rebuild-stable PK
    val role: EntryRole = EntryRole.FILE, // in-memory only; NOT a DB column. GROUP→type, comment→COMMENT
    val parentId: Long? = null, // Owning group's tid (membership) or parent group's tid (nesting); null at root
    val targetTid: Long? = null, // For comments: the owner entry's tid
    val commentTargetTs: String? = null, // Transient: owner targetTs before targetTid resolution (comments only)
    val isPinned: Boolean = false, // Encoded as a `9999-` filename prefix
    val todoState: TodoState = TodoState.NONE, // Encoded as a status token in the filename
    val tags: List<String> = emptyList(), // From the `m` sidecar (served from the tags table)
    val events: List<EntryEvent> = emptyList(), // From the `m` sidecar (served from the events table)
    val metaEncrypted: Boolean = false, // True when a Private sidecar is not yet decrypted
) {
    /** The second-resolution timestamp segment (`yyMM-dd-HHmmss`) recovered from [tid]. */
    val timestamp: String
        get() = Tid.TIMESTAMP_FORMAT.format(
            java.time.Instant
                .ofEpochMilli(tid)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime(),
        )
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
    GROUP, // Directory containers — rows in `entries` whose parent/child links are via parentId
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
