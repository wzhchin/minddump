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
@Suppress("TooManyFunctions") // File ops are inherently granular; grouped by concern
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
     * [targetDir] overrides the destination (e.g. an open group directory); when
     * null the entry is written to the current month directory as before.
     */
    fun saveTextEntry(
        space: Space,
        content: String,
        targetDir: File? = null,
    ): File {
        val dir = targetDir ?: ensureCurrentMonthDir(space)
        val file = uniqueFile(dir, "${nowTimestampStr()}-f", "md")
        file.writeText(content)
        return file
    }

    /**
     * Overwrite an existing text entry's content in place. The file path and
     * identity are preserved; only the bytes change (bumping lastModified).
     */
    fun overwriteText(file: File, content: String) {
        file.writeText(content)
    }

    /**
     * Get the output file for a recording.
     * Produces: {yymm-dd-HHMMSS}-f.m4a
     */
    fun getRecordingFile(space: Space, targetDir: File? = null): File {
        val dir = targetDir ?: ensureCurrentMonthDir(space)
        return uniqueFile(dir, "${nowTimestampStr()}-f", "m4a")
    }

    /**
     * Get the output file for a photo.
     * Produces: {yymm-dd-HHMMSS}-f.jpg
     */
    fun getPhotoFile(space: Space, targetDir: File? = null): File {
        val dir = targetDir ?: ensureCurrentMonthDir(space)
        return uniqueFile(dir, "${nowTimestampStr()}-f", "jpg")
    }

    /**
     * Get the output file for a video.
     * Produces: {yymm-dd-HHMMSS}-f.mp4
     */
    fun getVideoFile(space: Space, targetDir: File? = null): File {
        val dir = targetDir ?: ensureCurrentMonthDir(space)
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
     * Create a group directory. With [parentDir] == null it is created under the
     * current month (legacy behavior); with a non-null [parentDir] it is created
     * inside that group directory, enabling nesting.
     * Produces: {yymm-dd-HHMMSS}-g[-{name}]/
     */
    fun createGroup(space: Space, name: String?, parentDir: File? = null): File {
        val dir = parentDir ?: ensureCurrentMonthDir(space)
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
     * Move a file out of its group directory. If the group is nested (its parent
     * is itself a group), the file moves into that parent group; otherwise it
     * moves to the month directory.
     */
    @Suppress("UNUSED_PARAMETER")
    fun moveOutOfGroup(file: File, space: Space): File {
        val groupDir = file.parentFile
            ?: error("File is not inside a group directory: ${file.absolutePath}")
        val destParent = dissolveDestination(groupDir)
        val dest = File(destParent, file.name)
        val moved = file.renameTo(dest)
        check(moved) { "Failed to move ${file.absolutePath} out of group" }
        return dest
    }

    /**
     * Where members of [groupDir] should go when it is dissolved: its parent
     * directory (the parent group if nested, else the month directory).
     */
    private fun dissolveDestination(groupDir: File): File =
        groupDir.parentFile ?: error("Group directory has no parent: ${groupDir.absolutePath}")

    /**
     * Dissolve a group: move every member (loose files and sub-group directories)
     * into the group's parent location, then remove the now-empty group directory.
     * Nesting-aware — members of a sub-group move into the parent group, not the
     * month directory.
     *
     * On a per-item failure, the exception propagates and any items already moved
     * remain at their destination (reconcile will re-index them).
     */
    fun dissolveGroup(groupDir: File) {
        val destParent = dissolveDestination(groupDir)
        groupDir.listFiles()?.forEach { child ->
            val dest = File(destParent, child.name)
            val moved = child.renameTo(dest)
            check(moved) { "Failed to move ${child.absolutePath} out of group ${groupDir.absolutePath}" }
        }
        val deleted = groupDir.delete()
        check(deleted) { "Failed to delete group directory ${groupDir.absolutePath}" }
    }

    /**
     * Rename a group directory's display name portion.
     * Reuses the `[9999-]{ts}-[STATUS-]-g[-{name}]` naming rule from [createGroup],
     * preserving any existing pin prefix and todo status.
     * Returns the renamed directory.
     */
    fun renameGroupDir(groupDir: File, newName: String?): File {
        val meta = FileMetadata.fromFile(groupDir) ?: error("Cannot parse group directory: ${groupDir.name}")
        val newDir = File(groupDir.parent, reassembleDirName(meta.timestamp, meta.isPinned, meta.todoState, newName))
        check(!newDir.exists()) { "Target group directory already exists: ${newDir.absolutePath}" }
        val renamed = groupDir.renameTo(newDir)
        check(renamed) { "Failed to rename group directory ${groupDir.absolutePath}" }
        return newDir
    }

    /**
     * Toggle the `9999-` pin prefix on a file entry. Preserves status, original
     * name, extension, and encryption. Returns the renamed file.
     */
    fun setEntryPinned(file: File, pinned: Boolean): File {
        val meta = FileMetadata.fromFile(file) ?: error("Cannot parse file: ${file.name}")
        if (meta.role == EntryRole.COMMENT) error("Comments cannot be pinned: ${file.name}")
        if (meta.isPinned == pinned) return file
        return renameEntryFile(file, meta.copy(isPinned = pinned))
    }

    /**
     * Set the todo status on a file entry. Pass [TodoState.NONE] to clear.
     * Preserves pin, original name, extension, and encryption.
     */
    fun setEntryStatus(file: File, state: TodoState): File {
        val meta = FileMetadata.fromFile(file) ?: error("Cannot parse file: ${file.name}")
        if (meta.role == EntryRole.COMMENT) error("Comments cannot carry a status: ${file.name}")
        if (meta.todoState == state) return file
        return renameEntryFile(file, meta.copy(todoState = state))
    }

    /**
     * Toggle the `9999-` pin prefix on a group directory.
     */
    fun setGroupPinned(groupDir: File, pinned: Boolean): File {
        val meta = FileMetadata.fromFile(groupDir) ?: error("Cannot parse group directory: ${groupDir.name}")
        if (meta.isPinned == pinned) return groupDir
        val newDir = File(groupDir.parent, reassembleDirName(meta.timestamp, pinned, meta.todoState, meta.originalName))
        check(!newDir.exists()) { "Target group directory already exists: ${newDir.absolutePath}" }
        val renamed = groupDir.renameTo(newDir)
        check(renamed) { "Failed to pin group directory ${groupDir.absolutePath}" }
        return newDir
    }

    /**
     * Set the todo status on a group directory. Pass [TodoState.NONE] to clear.
     */
    fun setGroupStatus(groupDir: File, state: TodoState): File {
        val meta = FileMetadata.fromFile(groupDir) ?: error("Cannot parse group directory: ${groupDir.name}")
        if (meta.todoState == state) return groupDir
        val newDir = File(groupDir.parent, reassembleDirName(meta.timestamp, meta.isPinned, state, meta.originalName))
        check(!newDir.exists()) { "Target group directory already exists: ${newDir.absolutePath}" }
        val renamed = groupDir.renameTo(newDir)
        check(renamed) { "Failed to set status on group directory ${groupDir.absolutePath}" }
        return newDir
    }

    /**
     * Reassemble a file entry's filename from its parsed metadata, honoring pin
     * prefix, status token, role, original name, extension, and encryption.
     */
    private fun reassembleFileName(meta: FileMetadata): String {
        val pin = if (meta.isPinned) "9999-" else ""
        val status = meta.todoState.code?.let { "$it-" } ?: ""
        val name = meta.originalName?.let { "-$it" } ?: ""
        val enc = if (meta.isEncrypted) ".enc" else ""
        return "$pin${meta.timestamp}-$status${meta.role.code}$name.${meta.extension}$enc"
    }

    /**
     * Reassemble a group directory's name (no extension).
     */
    private fun reassembleDirName(
        timestamp: String,
        isPinned: Boolean,
        state: TodoState,
        name: String?,
    ): String {
        val pin = if (isPinned) "9999-" else ""
        val status = state.code?.let { "$it-" } ?: ""
        val suffix = if (name.isNullOrBlank()) "g" else "g-$name"
        return "$pin$timestamp-$status$suffix"
    }

    /**
     * Rename a file entry to match [newMeta], guarding against collisions by
     * appending `_1`, `_2`, … to the original-name portion before the extension.
     */
    private fun renameEntryFile(file: File, newMeta: FileMetadata): File {
        val parent = file.parentFile ?: error("File has no parent: ${file.absolutePath}")

        fun targetFor(originalName: String?): File =
            File(parent, reassembleFileName(newMeta.copy(originalName = originalName)))

        var target = targetFor(newMeta.originalName)
        if (target == file) return file
        var seq = 1
        while (target.exists()) {
            val suffixed = newMeta.originalName?.let { name -> "${name}_$seq" } ?: "entry_$seq"
            target = targetFor(suffixed)
            seq++
        }
        val renamed = file.renameTo(target)
        check(renamed) { "Failed to rename ${file.absolutePath} -> ${target.absolutePath}" }
        return target
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
     * Equivalent to scanning the current month directory's children.
     */
    fun scanGroups(space: Space): List<File> = scanChildGroups(getCurrentMonthDir(space))

    /**
     * List the direct group sub-directories under [parentDir] (a month dir or a
     * group dir). Powers both month-top group cards and nested sub-group cards.
     */
    fun scanChildGroups(parentDir: File): List<File> {
        if (!parentDir.exists()) return emptyList()
        return parentDir
            .listFiles()
            ?.filter { it.isDirectory && FileMetadata.fromFile(it)?.role == EntryRole.GROUP }
            ?.sortedByDescending { it.name }
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
                            isPinned = meta.isPinned,
                            todoState = meta.todoState,
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

        // Sort by filename (descending) so the `9999-` pin sentinel floats pinned
        // entries above all real dates, and within each block the timestamp gives
        // newest-first order. Stable across edits — pinning, not editing, reorders.
        return entries.sortedByDescending { it.file.name }
    }

    /**
     * Rename the originalName portion of a file, preserving pin, status, and
     * encryption. E.g., `9999-{ts}-TODO-f-oldname.pdf` → `9999-{ts}-TODO-f-newname.pdf`.
     */
    fun renameEntry(file: File, newOriginalName: String?): File {
        val meta = FileMetadata.fromFile(file) ?: error("Cannot parse file: ${file.name}")
        val cleanedName = newOriginalName?.ifBlank { null }
        return renameEntryFile(file, meta.copy(originalName = cleanedName))
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
