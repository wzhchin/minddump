package com.chin.minddump.storage

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Handles all file operations for MindDump.
 * Directory layout: {workDir}/{Public,Private}/YYYY-MM/
 * File naming: {yymm-dd-HHMMSS}-f[-{originalName}].{extension}
 * Comment naming: {targetTs}-n-{yymm-dd-HHMMSS}.md
 * Group directories: {yymm-dd-HHMMSS}-g[-{name}]/
 */
class FileStorageEngine(
    private val context: Context,
) {
    private val prefs = StoragePreferences(context)

    companion object {
        private const val DEFAULT_ROOT_DIR = "MindDump"
        private val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
        private val TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyMM-dd-HHmmss")
    }

    private fun nowTimestampStr(): String = LocalDateTime.now().format(TIMESTAMP_FORMAT)

    private fun currentMonthStr(): String = LocalDate.now().format(MONTH_FORMAT)

    /**
     * Get the current root directory.
     * Falls back to /sdcard/MindDump/ if not yet configured.
     */
    fun getRootDir(): File {
        val path = prefs.getWorkDir()
        return if (path != null) File(path) else File(Environment.getExternalStorageDirectory(), DEFAULT_ROOT_DIR)
    }

    /**
     * Get the display path for the current root directory.
     */
    fun getRootDirPath(): String = getRootDir().absolutePath

    /**
     * Set a new work directory.
     */
    fun setWorkDir(path: String) {
        prefs.setWorkDir(path)
    }

    /**
     * Check if a work directory has been configured.
     */
    fun isWorkDirConfigured(): Boolean = prefs.isConfigured()

    fun getSpaceDir(
        space: Space,
        month: String,
    ): File = File(getRootDir(), "${space.folderName}/$month")

    fun getCurrentMonthDir(space: Space): File = getSpaceDir(space, currentMonthStr())

    /**
     * Check if we have external storage access.
     */
    fun hasStoragePermission(): Boolean = Environment.isExternalStorageManager()

    /**
     * Ensure the directory for the current month exists for the given space.
     */
    fun ensureCurrentMonthDir(space: Space): File {
        val dir = getCurrentMonthDir(space)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Generate a unique file in [dir] with the given [baseName] and [ext].
     * Appends _1, _2, ... suffix on collision.
     */
    private fun uniqueFile(dir: File, baseName: String, ext: String): File {
        var file = File(dir, "$baseName.$ext")
        var seq = 1
        while (file.exists()) {
            file = File(dir, "${baseName}_$seq.$ext")
            seq++
        }
        return file
    }

    /**
     * Save a text entry as Markdown.
     * Produces: {yymm-dd-HHMMSS}-f.md
     */
    fun saveTextEntry(
        space: Space,
        content: String,
    ): File {
        val dir = ensureCurrentMonthDir(space)
        val file = uniqueFile(dir, "${nowTimestampStr()}-f", "md")
        file.writeText(content)
        return file
    }

    /**
     * Get the output file for a recording.
     * Produces: {yymm-dd-HHMMSS}-f.m4a
     */
    fun getRecordingFile(space: Space): File {
        val dir = ensureCurrentMonthDir(space)
        return uniqueFile(dir, "${nowTimestampStr()}-f", "m4a")
    }

    /**
     * Get the output file for a photo.
     * Produces: {yymm-dd-HHMMSS}-f.jpg
     */
    fun getPhotoFile(space: Space): File {
        val dir = ensureCurrentMonthDir(space)
        return uniqueFile(dir, "${nowTimestampStr()}-f", "jpg")
    }

    /**
     * Get the output file for a video.
     * Produces: {yymm-dd-HHMMSS}-f.mp4
     */
    fun getVideoFile(space: Space): File {
        val dir = ensureCurrentMonthDir(space)
        return uniqueFile(dir, "${nowTimestampStr()}-f", "mp4")
    }

    /**
     * Import a file from a content URI.
     * Produces: {yymm-dd-HHMMSS}-f-{originalFileName}
     * @throws IOException if the URI cannot be opened or the result is empty.
     */
    fun importFile(
        space: Space,
        uri: Uri,
        originalFileName: String,
    ): File {
        val dir = ensureCurrentMonthDir(space)
        val timestamp = nowTimestampStr()
        val destFile = File(dir, "$timestamp-f-$originalFileName")
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open input stream for URI: $uri")
        inputStream.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        if (!destFile.exists() || destFile.length() == 0L) {
            destFile.delete()
            throw IOException("Imported file is empty or missing: ${destFile.absolutePath}")
        }
        return destFile
    }

    /**
     * Create a group directory under the current month.
     * Produces: {yymm-dd-HHMMSS}-g[-{name}]/
     */
    fun createGroup(space: Space, name: String?): File {
        val dir = ensureCurrentMonthDir(space)
        val suffix = if (name.isNullOrBlank()) "g" else "g-$name"
        var groupDir = File(dir, "${nowTimestampStr()}-$suffix")
        var seq = 1
        while (groupDir.exists()) {
            groupDir = File(dir, "${nowTimestampStr()}-${suffix}_$seq")
            seq++
        }
        groupDir.mkdirs()
        return groupDir
    }

    /**
     * Move a file into a group directory.
     * Returns the new file path after the move.
     */
    fun moveToGroup(file: File, groupDir: File): File {
        val dest = File(groupDir, file.name)
        val moved = file.renameTo(dest)
        check(moved) { "Failed to move ${file.absolutePath} into group ${groupDir.absolutePath}" }
        return dest
    }

    /**
     * Move a file out of its group directory back to the parent month directory.
     */
    @Suppress("UNUSED_PARAMETER")
    fun moveOutOfGroup(file: File, space: Space): File {
        // Parent of group dir is the month dir
        val monthDir = file.parentFile?.parentFile
            ?: error("File is not inside a group directory: ${file.absolutePath}")
        val dest = File(monthDir, file.name)
        val moved = file.renameTo(dest)
        check(moved) { "Failed to move ${file.absolutePath} out of group" }
        return dest
    }

    /**
     * Save a comment file targeting a specific entry.
     * Produces: {targetTs}-n-{yymm-dd-HHMMSS}.md
     * The comment is placed in the same directory as the target.
     */
    fun saveComment(targetDir: File, targetTimestamp: String, content: String): File {
        val file = uniqueFile(targetDir, "$targetTimestamp-n-${nowTimestampStr()}", "md")
        file.writeText(content)
        return file
    }

    /**
     * Scan group directories in the current month for a given space.
     */
    fun scanGroups(space: Space): List<File> {
        val monthDir = getCurrentMonthDir(space)
        if (!monthDir.exists()) return emptyList()

        return monthDir
            .listFiles()
            ?.filter { it.isDirectory && FileMetadata.fromFile(it)?.role == EntryRole.GROUP }
            ?: emptyList()
    }

    /**
     * Scan all entries for a given space, sorted by lastModified (newest first).
     * Uses FileMetadata for parsing — skips non-MindDump files.
     * Recursively scans group directories and populates groupPath.
     */
    fun scanEntries(space: Space): List<MindDumpEntry> {
        val spaceDir = File(getRootDir(), space.folderName)
        if (!spaceDir.exists()) return emptyList()

        val entries = mutableListOf<MindDumpEntry>()

        fun scanDirectory(dir: File, monthFolder: String, groupPath: String?) {
            dir
                .listFiles()
                ?.filter { it.isFile }
                ?.forEach { file ->
                    val meta = FileMetadata.fromFile(file) ?: return@forEach
                    // Only index FILE and COMMENT roles, skip GROUP (those are directories)
                    if (meta.role == EntryRole.GROUP) return@forEach

                    entries.add(
                        MindDumpEntry(
                            file = file,
                            type = meta.entryType,
                            space = space,
                            monthFolder = monthFolder,
                            timestamp = meta.timestamp,
                            role = meta.role,
                            targetTimestamp = null, // derived during reconciliation
                            groupPath = groupPath,
                        ),
                    )
                }

            // Recurse into group directories
            dir
                .listFiles()
                ?.filter { it.isDirectory }
                ?.forEach { subDir ->
                    val subMeta = FileMetadata.fromFile(subDir)
                    if (subMeta?.role == EntryRole.GROUP) {
                        scanDirectory(subDir, monthFolder, subDir.absolutePath)
                    }
                }
        }

        spaceDir
            .listFiles()
            ?.filter { it.isDirectory }
            ?.forEach { monthDir ->
                val monthFolder = monthDir.name // YYYY-MM
                scanDirectory(monthDir, monthFolder, null)
            }

        return entries.sortedByDescending { it.file.lastModified() }
    }

    /**
     * Rename the originalName portion of a file.
     * E.g., {ts}-f-oldname.pdf → {ts}-f-newname.pdf
     */
    fun renameEntry(file: File, newOriginalName: String?): File {
        val meta = FileMetadata.fromFile(file) ?: error("Cannot parse file: ${file.name}")
        val encSuffix = if (meta.isEncrypted) ".enc" else ""

        val newBaseName = if (newOriginalName.isNullOrBlank()) {
            "${meta.timestamp}-f"
        } else {
            "${meta.timestamp}-f-$newOriginalName"
        }

        val newFile = File(file.parent, "$newBaseName.${meta.extension}$encSuffix")
        val renamed = file.renameTo(newFile)
        check(renamed) { "Failed to rename ${file.absolutePath}" }
        return newFile
    }

    /**
     * Move a file between spaces (Public ↔ Private).
     * Moves from current month dir to target space's month dir.
     */
    fun moveBetweenSpaces(file: File, targetSpace: Space): File {
        val meta = FileMetadata.fromFile(file) ?: error("Cannot parse file: ${file.name}")

        // Determine current month from file path (find the YYYY-MM directory)
        val monthFolder = findMonthFolder(file) ?: currentMonthStr()
        val targetDir = getSpaceDir(targetSpace, monthFolder)
        if (!targetDir.exists()) targetDir.mkdirs()

        val dest = File(targetDir, file.name)
        val moved = file.renameTo(dest)
        check(moved) { "Failed to move ${file.absolutePath} to ${targetSpace.name}" }
        return dest
    }

    /**
     * Delete an entry file.
     */
    fun deleteEntry(entry: MindDumpEntry): Boolean = entry.file.delete()

    /**
     * Count all files recursively under the current root directory.
     */
    fun countFiles(): Int {
        val root = getRootDir()
        if (!root.exists()) return 0
        return root.walkTopDown().filter { it.isFile }.count()
    }

    /**
     * Count all files recursively under a given directory.
     */
    fun countFilesIn(dir: File): Int {
        if (!dir.exists()) return 0
        return dir.walkTopDown().filter { it.isFile }.count()
    }

    /**
     * Migrate all files from the current root directory to a new root.
     * Preserves the {Public,Private}/YYYY-MM/ structure.
     */
    fun migrateTo(newRoot: File) {
        val oldRoot = getRootDir()
        if (!oldRoot.exists()) return

        // Copy entire directory tree
        oldRoot.copyRecursively(newRoot, overwrite = true)
        // Delete old files
        oldRoot.deleteRecursively()
    }

    /**
     * Find the YYYY-MM folder in a file's path hierarchy.
     */
    private fun findMonthFolder(file: File): String? {
        var current = file.parentFile
        while (current != null) {
            val name = current.name
            if (name.matches(Regex("\\d{4}-\\d{2}"))) {
                return name
            }
            current = current.parentFile
        }
        return null
    }
}
