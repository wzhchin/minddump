package com.chin.minddump.storage

import java.io.File

/**
 * Parsed metadata extracted from a MindDump filename.
 *
 * Naming convention:
 *   File:    [9999-]{yymm-dd-HHMMSS}-[STATUS-]-f[-{originalName}].{extension}[.enc]
 *   Group:   [9999-]{yymm-dd-HHMMSS}-[STATUS-]-f[-{name}]/   (a directory, same grammar as a file)
 *   Meta:    {ownerFileName}.meta[.enc]   (sidecar paired by the owner's full name)
 *
 * Where `9999-` is an optional pin prefix (a sort sentinel) and `STATUS` is an
 * optional todo-status token (`TODO|DOING|WAIT|DONE|CANCEL`). The meta sidecar
 * never carries a pin prefix or a status. Timestamp format: `\d{4}-\d{2}-\d{6}`
 * (e.g., 2506-13-143022).
 *
 * A note and a group share the `f` role token — they are distinguished purely
 * by physical form: a file is a note, a directory is a group. The meta sidecar
 * is paired to its owner by the owner's full filename (plus `.meta`), which is
 * unique within a directory (filesystem-enforced), so a same-second note and a
 * same-second group never share a sidecar. Comments are removed; there is no
 * `n` role.
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
        // 4: role char, 5: extra (originalName), 6: ext, 7: enc.
        private val FILE_PATTERN = Regex(
            """^(9999-)?(\d{4}-\d{2}-\d{6})-(?:($STATUS)-)?([fm])(?:-(.+?))?\.(\w+)(\.enc)?$""",
        )

        // Directory form: no extension; status + name both optional.
        // Same `f` role as a file note — it is the directory-ness that marks a group.
        // Group 1: pin prefix, 2: timestamp, 3: status token, 4: name.
        private val DIR_PATTERN = Regex(
            """^(9999-)?(\d{4}-\d{2}-\d{6})-(?:($STATUS)-)?f(?:-(.+))?$""",
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
            val extra = match.groupValues[5] // originalName
            val ext = match.groupValues[6]
            val enc = match.groupValues[7]
            val isEncrypted = enc == ".enc"

            val role = when (roleChar) {
                "f" -> EntryRole.FILE
                "m" -> EntryRole.META
                else -> return null
            }

            // For files: extra = originalName (blank when absent).
            // Meta sidecars have no original-name component.
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
 * Role identifiers in MindDump filenames. **Filename-parsing only** — the DB has
 * no `role` column; entry kind is expressed by [EntryType] (with a GROUP value for
 * directory containers). Comments are removed, so there is no COMMENT role; the
 * `g` group role collapsed into `f` (a group is detected by being a directory).
 * META marks the `.meta` sidecar.
 */
enum class EntryRole(
    val code: String,
) {
    FILE("f"),
    GROUP("f"), // Same token as FILE — a directory is the marker, not the token.
    META("m"),
}
