package com.chin.minddump.storage

import java.io.File

/**
 * Parsed metadata extracted from a MindDump filename.
 *
 * Naming convention:
 *   File:    {yymm-dd-HHMMSS}-f[-{originalName}].{extension}[.enc]
 *   Comment: {yymm-dd-HHMMSS}-n-{yymm-dd-HHMMSS}.md[.enc]
 *   Group:   {yymm-dd-HHMMSS}-g[-{name}]/   (directory)
 *
 * Timestamp format: \d{4}-\d{2}-\d{4} (e.g., 2506-13-143022)
 */
data class FileMetadata(
    val timestamp: String, // yymm-dd-HHMMSS (e.g., 2506-13-143022)
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
        // Timestamp: yyMM-dd-HHmmss = \d{4}-\d{2}-\d{6} (14 chars)
        private val FILE_PATTERN = Regex(
            """^(\d{4}-\d{2}-\d{6})-([fng])(?:-(.+?))?\.(\w+)(\.enc)?$""",
        )
        private val DIR_PATTERN = Regex(
            """^(\d{4}-\d{2}-\d{6})-g(?:-(.+))?$""",
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

            val timestamp = match.groupValues[1]
            val roleChar = match.groupValues[2]
            val extra = match.groupValues[3] // originalName or commentTs
            val ext = match.groupValues[4]
            val enc = match.groupValues[5]
            val isEncrypted = enc == ".enc"

            val role = when (roleChar) {
                "f" -> EntryRole.FILE
                "n" -> EntryRole.COMMENT
                "g" -> EntryRole.GROUP
                else -> return null
            }

            // For files: extra = originalName
            // For comments: extra = commentTs (no originalName)
            val originalName = when (role) {
                EntryRole.FILE -> extra?.ifBlank { null }
                else -> null
            }

            return FileMetadata(
                timestamp = timestamp,
                role = role,
                originalName = originalName,
                extension = ext,
                isEncrypted = isEncrypted,
                rawFileName = name,
            )
        }

        private fun parseDirectory(name: String): FileMetadata? {
            val match = DIR_PATTERN.matchEntire(name) ?: return null

            return FileMetadata(
                timestamp = match.groupValues[1],
                role = EntryRole.GROUP,
                originalName = match.groupValues[2]?.ifBlank { null },
                extension = "",
                isEncrypted = false,
                rawFileName = name,
            )
        }
    }
}

/**
 * Role identifiers in MindDump filenames.
 * Used to distinguish files, comments, and group directories.
 */
enum class EntryRole(
    val code: String
) {
    FILE("f"),
    COMMENT("n"),
    GROUP("g"),
}
