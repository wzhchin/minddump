package com.chin.minddump.storage

import java.io.File

/**
 * Parsed metadata extracted from a MindDump filename.
 *
 * Naming convention:
 *   File:    [9999-]{yymm-dd-HHMMSS}-[STATUS-]-f[-{originalName}].{extension}[.enc]
 *   Comment: {yymm-dd-HHMMSS}-n-{yymm-dd-HHMMSS}.md[.enc]
 *   Group:   [9999-]{yymm-dd-HHMMSS}-[STATUS-]-g[-{name}]/   (directory)
 *   Meta:    {yymm-dd-HHMMSS}-m.yaml[.enc]   (sidecar paired by timestamp)
 *
 * Where `9999-` is an optional pin prefix (a sort sentinel) and `STATUS` is an
 * optional todo-status token (`TODO|DOING|WAIT|DONE|CANCEL`). Comments and meta
 * never carry a pin prefix or a status. Timestamp format: `\d{4}-\d{2}-\d{6}`
 * (e.g., 2506-13-143022). The three free-form positions never collide: the
 * status token is an uppercase word sitting before the lowercase role char,
 * while `{originalName}` sits after the role char.
 *
 * The `m` (META) role is a sidecar that carries an owner entry's structured
 * metadata (tags, scheduled events). It is paired to its owner by **timestamp
 * alignment**: it shares the owner's timestamp and lives as a sibling file in
 * the same directory. It never has a pin, status, or original-name component.
 */
data class FileMetadata(
    val timestamp: String, // yymm-dd-HHMMSS (e.g., 2506-13-143022)
    val isPinned: Boolean, // encoded as a `9999-` filename prefix
    val todoState: TodoState, // encoded as a status token (NONE when absent)
    val role: EntryRole,
    val originalName: String?, // nullable, only for imported files
    val extension: String, // without dot
    val isEncrypted: Boolean,
    val rawFileName: String,
) {
    /** Derive entry type from file extension. */
    val entryType: EntryType
        get() = EntryType.fromExtension(extension)

    companion object {
        // Status token accepted between the timestamp and the role char.
        private const val STATUS = """(?:TODO|DOING|WAIT|DONE|CANCEL)"""

        // [9999-](timestamp)-[STATUS-](role)[-extra].(ext)(.enc)
        // Group 1: pin prefix (without dash), 2: timestamp, 3: status token,
        // 4: role char, 5: extra (originalName or comment targetTs), 6: ext, 7: enc.
        private val FILE_PATTERN = Regex(
            """^(9999-)?(\d{4}-\d{2}-\d{6})-(?:($STATUS)-)?([fngm])(?:-(.+?))?\.(\w+)(\.enc)?$""",
        )

        // Directory form: no extension; status + name both optional.
        // Group 1: pin prefix, 2: timestamp, 3: status token, 4: name.
        private val DIR_PATTERN = Regex(
            """^(9999-)?(\d{4}-\d{2}-\d{6})-(?:($STATUS)-)?g(?:-(.+))?$""",
        )

        /**
         * Parse a file into structured metadata.
         * Returns null if the file does not follow MindDump naming convention.
         */
        fun fromFile(file: File): FileMetadata? {
            val name = file.name
            val isDirectory = file.isDirectory

            return if (isDirectory) {
                parseDirectory(name)
            } else {
                parseFile(name)
            }
        }

        private fun parseFile(name: String): FileMetadata? {
            val match = FILE_PATTERN.matchEntire(name) ?: return null

            val isPinned = match.groupValues[1].isNotEmpty()
            val timestamp = match.groupValues[2]
            val statusToken = match.groupValues[3].takeIf { it.isNotEmpty() }
            val roleChar = match.groupValues[4]
            val extra = match.groupValues[5] // originalName or commentTs
            val ext = match.groupValues[6]
            val enc = match.groupValues[7]
            val isEncrypted = enc == ".enc"

            val role = when (roleChar) {
                "f" -> EntryRole.FILE
                "n" -> EntryRole.COMMENT
                "g" -> EntryRole.GROUP
                "m" -> EntryRole.META
                else -> return null
            }

            // For files: extra = originalName (blank when absent)
            // For comments: extra = commentTs (no originalName)
            val originalName = when (role) {
                EntryRole.FILE -> extra.ifBlank { null }
                else -> null
            }

            return FileMetadata(
                timestamp = timestamp,
                isPinned = isPinned,
                todoState = TodoState.fromCode(statusToken),
                role = role,
                originalName = originalName,
                extension = ext,
                isEncrypted = isEncrypted,
                rawFileName = name,
            )
        }

        private fun parseDirectory(name: String): FileMetadata? {
            val match = DIR_PATTERN.matchEntire(name) ?: return null

            val isPinned = match.groupValues[1].isNotEmpty()
            val statusToken = match.groupValues[3].takeIf { it.isNotEmpty() }

            return FileMetadata(
                timestamp = match.groupValues[2],
                isPinned = isPinned,
                todoState = TodoState.fromCode(statusToken),
                role = EntryRole.GROUP,
                originalName = match.groupValues[4].ifBlank { null },
                extension = "",
                isEncrypted = false,
                rawFileName = name,
            )
        }
    }
}

/**
 * Role identifiers in MindDump filenames.
 * Used to distinguish files, comments, group directories, and metadata sidecars.
 */
enum class EntryRole(
    val code: String
) {
    FILE("f"),
    COMMENT("n"),
    GROUP("g"),
    META("m"),
}
