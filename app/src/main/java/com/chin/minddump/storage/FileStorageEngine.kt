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
import timber.log.Timber

/**
 * Handles all file operations for MindDump.
 * Directory layout: {workDir}/{Public,Private}/YYYY-MM/
 * File naming: [9999-]{yymm-dd-HHMMSS}-[STATUS-]-f[-{originalName}].{extension}[.enc]
 * Group directories: [9999-]{yymm-dd-HHMMSS}-[STATUS-]-f[-{name}]/   (a directory, single level)
 * Meta sidecars: {ownerFileName}.meta[.enc]   (paired by the owner's full filename)
 *
 * Identity is the file path. Group membership is positional (a note sits in a
 * group because it lives in that group's directory). Groups are single-level —
 * a group directory contains only member files, never sub-groups.
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

        /** Name of the hidden trash holding dir, a sibling of Public/Private under root. */
        const val TRASH_DIR_NAME = ".trash"

        /** Entries trashed longer ago than this (days) are purged opportunistically. */
        const val TRASH_RETENTION_DAYS = 30L
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
     * Create a group directory at the month-top level. Groups are single-level,
     * so [parentDir] is intentionally ignored if non-null — a group is always
     * created directly under the current month directory. Callers must not pass
     * a group directory as [parentDir]; nesting is not supported.
     * Produces: {yymm-dd-HHMMSS}-f[-{name}]/
     */
    fun createGroup(space: Space, name: String?, parentDir: File? = null): File {
        @Suppress("UNUSED_PARAMETER")
        val ignoredNesting = parentDir // Groups are single-level; nesting is refused.
        val dir = ensureCurrentMonthDir(space)
        val suffix = if (name.isNullOrBlank()) "f" else "f-$name"
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
     * Move a file into a group directory, carrying its `.meta` sidecar along.
     * Returns the new file path after the move.
     */
    fun moveToGroup(file: File, groupDir: File): File = moveEntryToDir(file, groupDir)

    /**
     * Move a file out of its group directory to the month-top level, carrying
     * its `.meta` sidecar. Groups are single-level, so the destination is always
     * the month directory.
     */
    @Suppress("UNUSED_PARAMETER")
    fun moveOutOfGroup(file: File, space: Space): File {
        val groupDir = file.parentFile
            ?: error("File is not inside a group directory: ${file.absolutePath}")
        val destParent = groupDir.parentFile
            ?: error("Group directory has no parent: ${groupDir.absolutePath}")
        return moveEntryToDir(file, destParent)
    }

    /**
     * Move an owner file (and its `.meta` sidecar, if any) into [destParent].
     * Both travel together so the pairing survives the move; the sidecar is
     * optional (a bare note has none).
     */
    private fun moveEntryToDir(file: File, destParent: File): File {
        val dest = File(destParent, file.name)
        val moved = file.renameTo(dest)
        check(moved) { "Failed to move ${file.absolutePath} into ${destParent.absolutePath}" }
        val oldMeta = metaFile(file)
        if (oldMeta.exists()) {
            val newMeta = metaFile(dest)
            check(oldMeta.renameTo(newMeta)) {
                "Failed to move sidecar ${oldMeta.absolutePath} alongside ${dest.absolutePath}"
            }
        }
        return dest
    }

    /**
     * Dissolve a group: move every member file (carrying each one's `.meta`
     * sidecar) to the month-top level, then remove the now-empty group directory.
     * Groups are single-level, so members always go to the month directory —
     * there is no parent-group case.
     *
     * On a per-item failure, the exception propagates and any items already moved
     * remain at their destination (reconcile will re-index them).
     */
    fun dissolveGroup(groupDir: File) {
        val destParent = groupDir.parentFile
            ?: error("Group directory has no parent: ${groupDir.absolutePath}")
        groupDir.listFiles()?.forEach { child ->
            if (child.isDirectory) return@forEach // single-level: no sub-groups expected
            moveEntryToDir(child, destParent)
        }
        // A leftover sidecar whose owner moved is now an orphan; reconcile clears it,
        // but we also sweep obvious orphans here so the dir can be removed.
        groupDir.listFiles()?.forEach { orphan ->
            if (orphan.isFile && isMetaName(orphan.name)) orphan.delete()
        }
        val deleted = groupDir.delete()
        check(deleted) { "Failed to delete group directory ${groupDir.absolutePath}" }
    }

    /**
     * Remove a group directory that is already empty (its members were moved
     * elsewhere). No-op if the directory does not exist. Throws if it is not
     * empty — callers must ensure all members moved out first.
     */
    fun removeEmptyGroupDir(groupDir: File) {
        if (!groupDir.exists()) return
        check(groupDir.listFiles()?.isEmpty() ?: true) {
            "Refusing to remove non-empty group directory ${groupDir.absolutePath}"
        }
        check(groupDir.delete()) { "Failed to delete group directory ${groupDir.absolutePath}" }
    }

    /**
     * Rename a group directory's display name portion. Reuses the
     * `[9999-]{ts}-[STATUS-]-f[-{name}]` naming rule from [createGroup],
     * preserving any existing pin prefix and todo status, and carries the
     * group's `.meta` sidecar (a sibling in the parent) via [renameEntry].
     * Returns the renamed directory.
     */
    fun renameGroupDir(groupDir: File, newName: String?): File {
        val meta = FileMetadata.fromFile(groupDir) ?: error("Cannot parse group directory: ${groupDir.name}")
        val target = reassembleDirName(meta.timestamp, meta.isPinned, meta.todoState, newName)
        if (target == groupDir.name) return groupDir
        return renameEntry(groupDir, target)
    }

    /**
     * Toggle the `9999-` pin prefix on a file entry. Preserves status, original
     * name, extension, and encryption. Returns the renamed file.
     */
    fun setEntryPinned(file: File, pinned: Boolean): File {
        val meta = FileMetadata.fromFile(file) ?: error("Cannot parse file: ${file.name}")
        if (meta.isPinned == pinned) return file
        return renameEntryFile(file, meta.copy(isPinned = pinned))
    }

    /**
     * Set the todo status on a file entry. Pass [TodoState.NONE] to clear.
     * Preserves pin, original name, extension, and encryption.
     */
    fun setEntryStatus(file: File, state: TodoState): File {
        val meta = FileMetadata.fromFile(file) ?: error("Cannot parse file: ${file.name}")
        if (meta.todoState == state) return file
        return renameEntryFile(file, meta.copy(todoState = state))
    }

    /**
     * Toggle the `9999-` pin prefix on a group directory. Carries the `.meta`
     * sidecar alongside via [renameEntry].
     */
    fun setGroupPinned(groupDir: File, pinned: Boolean): File {
        val meta = FileMetadata.fromFile(groupDir) ?: error("Cannot parse group directory: ${groupDir.name}")
        if (meta.isPinned == pinned) return groupDir
        val target = reassembleDirName(meta.timestamp, pinned, meta.todoState, meta.originalName)
        return renameEntry(groupDir, target)
    }

    /**
     * Set the todo status on a group directory. Pass [TodoState.NONE] to clear.
     * Carries the `.meta` sidecar alongside via [renameEntry].
     */
    fun setGroupStatus(groupDir: File, state: TodoState): File {
        val meta = FileMetadata.fromFile(groupDir) ?: error("Cannot parse group directory: ${groupDir.name}")
        if (meta.todoState == state) return groupDir
        val target = reassembleDirName(meta.timestamp, meta.isPinned, state, meta.originalName)
        return renameEntry(groupDir, target)
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
     * Reassemble a group directory's name (no extension). Uses the `f` role token,
     * same as a file note — the directory itself is what marks a group.
     */
    private fun reassembleDirName(
        timestamp: String,
        isPinned: Boolean,
        state: TodoState,
        name: String?,
    ): String {
        val pin = if (isPinned) "9999-" else ""
        val status = state.code?.let { "$it-" } ?: ""
        val suffix = if (name.isNullOrBlank()) "f" else "f-$name"
        return "$pin$timestamp-$status$suffix"
    }

    /**
     * Rename a file entry to match [newMeta], guarding against collisions by
     * appending `_1`, `_2`, … to the original-name portion before the extension.
     * The owner's `.meta` sidecar (if any) is renamed alongside via [renameEntry].
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
        return renameEntry(file, target.name)
    }

    /**
     * Resolve the sidecar path for an owner entry. The sidecar is a sibling of
     * the owner (in the owner's parent directory) named `{ownerFileName}.meta`
     * (`.meta.enc` in Private). Pairing is a pure function of the owner's full
     * name — stateless, cold-rebuildable, and conflict-free (the owner filename
     * is unique within its directory).
     */
    fun sidecarFileFor(owner: File, encrypted: Boolean): File {
        val suffix = if (encrypted) ".meta.enc" else ".meta"
        return File(owner.parentFile, owner.name + suffix)
    }

    /** Whether a filename looks like a `.meta` / `.meta.enc` sidecar. */
    private fun isMetaName(name: String): Boolean =
        name.endsWith(".meta") || name.endsWith(".meta.enc")

    /**
     * The `.meta` sidecar paired to [owner] (either `.meta` or `.meta.enc`,
     * whichever exists on disk; null when neither does).
     */
    private fun metaFile(owner: File): File {
        val plain = sidecarFileFor(owner, encrypted = false)
        if (plain.exists()) return plain
        return sidecarFileFor(owner, encrypted = true)
    }

    /**
     * Write a plaintext (Public) sidecar's [content] to [sidecarFile], creating
     * parent dirs if needed. Callers delete the file when content is empty.
     */
    fun writeSidecarText(sidecarFile: File, content: String) {
        sidecarFile.parentFile?.mkdirs()
        sidecarFile.writeText(content)
    }

    /** Read a sidecar's text (caller handles decryption for encrypted ones). */
    fun readSidecarText(sidecarFile: File): String = sidecarFile.readText()

    /**
     * Scan group directories in the current month for a given space.
     * Equivalent to scanning the current month directory's children.
     */
    fun scanGroups(space: Space): List<File> = scanChildGroups(getCurrentMonthDir(space))

    /**
     * List the direct group sub-directories under [parentDir] (a month dir). With
     * single-level groups there are no sub-group cards inside a group directory,
     * so callers only invoke this against a month directory.
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
     * Scan all entries for a given space, sorted by filename (newest first).
     * Uses FileMetadata for parsing - skips non-MindDump files. A note is a file;
     * a group is a directory. Groups are single-level: the scanner enters a group
     * directory once, collects only its member files, and does NOT recurse for
     * nested sub-groups.
     *
     * Metadata sidecars (`.meta` / `.meta.enc`) are paired to their owner by the
     * owner's full filename (`name + ".meta"`), which is unique within a directory,
     * so a same-second note and a same-second group never share a sidecar. Private
     * encrypted sidecars are left unread here (lazy decryption happens on unlock),
     * so the owner is marked metaEncrypted = true with empty meta. Legacy
     * `{ts}-m.yaml` sidecars and `{targetTs}-n-...` comment files do not match the
     * `f`/`m` patterns and are skipped (not indexed).
     *
     * There is no synthesized tid - identity is the file path. groupPath records
     * which group a note lives in (the owning group directory's absolute path),
     * null at the month-top level. The absolute form matches [moveToGroup],
     * [indexGroupDir], the UI's `currentDir.absolutePath` scope filter, and the
     * `getMembersSnapshot` DAO call — it is the canonical form across the app.
     * A `workDir` move always triggers a full rebuild, so absolute paths are
     * re-rooted at rebuild time without staleness.
     */
    @Suppress("CyclomaticComplexMethod", "LongMethod", "NestedBlockDepth")
    fun scanEntries(space: Space): List<MindDumpEntry> {
        val spaceDir = File(getRootDir(), space.folderName)
        if (!spaceDir.exists()) return emptyList()

        val entries = mutableListOf<MindDumpEntry>()
        var currentMonth: String = ""

        fun emitOwner(owner: File, type: EntryType, role: EntryRole, groupPath: String?) {
            val sidecar = sidecarFor(owner)
            val meta = FileMetadata.fromFile(owner)
            entries.add(
                MindDumpEntry(
                    file = owner,
                    type = type,
                    space = space,
                    monthFolder = currentMonth,
                    role = role,
                    groupPath = groupPath,
                    isPinned = meta?.isPinned ?: false,
                    todoState = meta?.todoState ?: TodoState.NONE,
                    tags = sidecar?.parsed?.tags ?: emptyList(),
                    events = sidecar?.parsed?.events ?: emptyList(),
                    metaEncrypted = sidecar?.encrypted ?: false,
                ),
            )
        }

        fun indexLooseFile(file: File) {
            val meta = FileMetadata.fromFile(file) ?: return
            // META sidecars are consumed via sidecarFor; only FILE rows are owners.
            if (meta.role == EntryRole.FILE) emitOwner(file, meta.entryType, EntryRole.FILE, groupPath = null)
        }

        fun indexMember(member: File, groupDir: File) {
            if (!member.isFile) return
            val memberMeta = FileMetadata.fromFile(member) ?: return
            if (memberMeta.role != EntryRole.FILE) return
            emitOwner(member, memberMeta.entryType, EntryRole.FILE, groupPath = groupDir.absolutePath)
        }

        // Index a group directory and its direct member files (single level — no
        // recursion into sub-groups). Member notes are sidecar owners via [emitOwner].
        fun indexGroup(groupDir: File) {
            emitOwner(groupDir, EntryType.GROUP, EntryRole.GROUP, groupPath = null)
            groupDir.listFiles()?.forEach { member -> indexMember(member, groupDir) }
        }

        fun indexChild(child: File) {
            if (child.isDirectory) {
                if (FileMetadata.fromFile(child)?.role == EntryRole.GROUP) indexGroup(child)
            } else if (child.isFile) {
                indexLooseFile(child)
            }
        }

        // Scan a month directory. Group directories are indexed as GROUP owners and
        // entered once for their members; loose files are indexed directly.
        fun scanMonth(monthDir: File, monthFolder: String) {
            currentMonth = monthFolder
            monthDir.listFiles()?.forEach { child -> indexChild(child) }
        }

        spaceDir
            .listFiles()
            ?.filter { it.isDirectory && it.name != TRASH_DIR_NAME }
            ?.forEach { monthDir -> scanMonth(monthDir, monthDir.name) }

        // Sort by filename (descending) so the `9999-` pin sentinel floats pinned
        // entries above all real dates, and within each block the timestamp gives
        // newest-first order. Stable across edits — pinning, not editing, reorders.
        return entries.sortedByDescending { it.file.name }
    }

    /**
     * The `.meta` sidecar for [owner], parsed when readable (Public) or flagged
     * encrypted (Private, lazy). Returns null when no sidecar exists on disk
     * (a bare note). A sidecar is identified by exact full-name pairing — no
     * ambiguity, no orphaning of real owners.
     */
    private fun sidecarFor(owner: File): SidecarPayload? {
        val plain = sidecarFileFor(owner, encrypted = false)
        if (plain.exists()) return readSidecar(plain, isEncrypted = false)
        val enc = sidecarFileFor(owner, encrypted = true)
        if (enc.exists()) return readSidecar(enc, isEncrypted = true)
        return null
    }

    /**
     * A parsed sidecar: its decoded [EntryMeta] when readable (Public), or a flag
     * noting it exists but is encrypted (Private, lazy).
     */
    private data class SidecarPayload(
        val parsed: EntryMeta?,
        val encrypted: Boolean,
    )

    /**
     * Read a sidecar file. Public sidecars (`.meta`) are parsed immediately.
     * Private encrypted sidecars (`.meta.enc`) are NOT decrypted here — the
     * owner is marked encrypted so tags/events stay empty until unlock.
     */
    private fun readSidecar(file: File, isEncrypted: Boolean): SidecarPayload =
        if (isEncrypted) {
            SidecarPayload(parsed = null, encrypted = true)
        } else {
            val text = runCatching { file.readText() }.getOrElse {
                Timber.w(it, "Cannot read sidecar %s", file.name)
                return SidecarPayload(parsed = EntryMeta.EMPTY, encrypted = false)
            }
            SidecarPayload(parsed = MetaYamlCodec.decode(text), encrypted = false)
        }

    /**
     * Rename an owner (file note or group directory) to [newName], carrying its
     * `.meta` sidecar alongside so the owner/sidecar pairing survives every
     * rename. This is the single funnel for owner renames: pin, todo status,
     * slug edits, group renames, and group pin/status all route through it.
     *
     * OS `renameTo` moves one file at a time, so the owner and sidecar renames
     * are not instantaneously atomic. A crash between them leaves a new owner
     * whose sidecar is still under the old name; the next reconcile treats that
     * stale sidecar as an orphan (no owner) and clears it, and the new owner
     * reads as a bare note until its tags/events are re-added. Owner content is
     * never lost — only the (rebuildable) sidecar is at risk in that window.
     */
    fun renameEntry(old: File, newName: String): File {
        val parent = old.parentFile ?: error("Owner has no parent: ${old.absolutePath}")
        val target = File(parent, newName)
        check(!target.exists()) { "Target already exists: ${target.absolutePath}" }
        check(old.renameTo(target)) { "Failed to rename ${old.absolutePath} -> ${target.absolutePath}" }
        val oldMeta = metaFile(old)
        if (oldMeta.exists()) {
            val newMeta = metaFile(target)
            check(oldMeta.renameTo(newMeta)) {
                "Failed to rename sidecar ${oldMeta.absolutePath} -> ${newMeta.absolutePath}"
            }
        }
        return target
    }

    /**
     * Rename the originalName portion of a file, preserving pin, status, and
     * encryption. E.g., `9999-{ts}-TODO-f-oldname.pdf` → `9999-{ts}-TODO-f-newname.pdf`.
     * Routes through [renameEntryFile] → [renameEntry] so the sidecar travels.
     */
    fun renameEntrySlug(file: File, newOriginalName: String?): File {
        val meta = FileMetadata.fromFile(file) ?: error("Cannot parse file: ${file.name}")
        val cleanedName = newOriginalName?.ifBlank { null }
        return renameEntryFile(file, meta.copy(originalName = cleanedName))
    }

    /**
     * Move a file between spaces (Public ↔ Private), carrying its `.meta` sidecar.
     * Moves from current month dir to target space's month dir.
     */
    fun moveBetweenSpaces(file: File, targetSpace: Space): File {
        // Determine current month from file path (find the YYYY-MM directory)
        val monthFolder = findMonthFolder(file) ?: currentMonthStr()
        val targetDir = getSpaceDir(targetSpace, monthFolder)
        if (!targetDir.exists()) targetDir.mkdirs()
        return moveEntryToDir(file, targetDir)
    }

    /**
     * The trash holding directory, a sibling of Public/Private under the root.
     * Trashed files keep their exact relative path beneath their space, so restore
     * is a pure path inversion with no side store.
     */
    fun trashRoot(): File = File(getRootDir(), TRASH_DIR_NAME)

    /**
     * Relative path of [file] beneath its space root (the part after `Public/` or
     * `Private/`). Used to mirror the live location inside `.trash/<space>/`.
     */
    private fun relPathUnderSpace(file: File, space: Space): String {
        val spaceRoot = File(getRootDir(), space.folderName).absolutePath
        val abs = file.absolutePath
        check(abs.startsWith(spaceRoot)) {
            "File $abs is not under ${space.folderName} root $spaceRoot"
        }
        return abs.removePrefix(spaceRoot).removePrefix(File.separator)
    }

    /**
     * Soft-delete an entry: move its file into `.trash/<space>/<relPath>`, creating
     * the mirror directory structure. The rename refreshes mtime, which becomes the
     * trash age used for retention. Returns the trashed file.
     */
    fun trashEntry(entry: MindDumpEntry): File = trashFile(entry.file, entry.space)

    /**
     * Move an arbitrary live file belonging to [space] into the trash, preserving
     * its relative path. Powers both single-entry and batch trashing.
     */
    fun trashFile(file: File, space: Space): File {
        val rel = relPathUnderSpace(file, space)
        val target = File(File(trashRoot(), space.folderName), rel)
        target.parentFile?.mkdirs()
        check(file.renameTo(target)) {
            "Failed to trash ${file.absolutePath} -> ${target.absolutePath}"
        }
        return target
    }

    /**
     * Soft-delete a group directory: move the whole tree (members and nested
     * sub-groups) into `.trash/<space>/<relPath>`.
     */
    fun trashGroup(groupDir: File, space: Space): File {
        val rel = relPathUnderSpace(groupDir, space)
        val target = File(File(trashRoot(), space.folderName), rel)
        target.parentFile?.mkdirs()
        check(groupDir.renameTo(target)) {
            "Failed to trash group ${groupDir.absolutePath} -> ${target.absolutePath}"
        }
        return target
    }

    /**
     * Restore a trashed file/dir back under its space root at the preserved path.
     * On a collision with an existing live entry, append `_1`, `_2`, … to the name
     * so restore never overwrites. Returns the restored file/dir.
     */
    fun restoreTrashed(trashedFile: File, space: Space): File {
        val trashSpaceRoot = File(trashRoot(), space.folderName).absolutePath
        check(trashedFile.absolutePath.startsWith(trashSpaceRoot)) {
            "File ${trashedFile.absolutePath} is not in trash for ${space.folderName}"
        }
        val rel = trashedFile.absolutePath.removePrefix(trashSpaceRoot).removePrefix(File.separator)
        var target = File(File(getRootDir(), space.folderName), rel)
        if (target == trashedFile) return target
        var seq = 1
        while (target.exists()) {
            val suffix = "_$seq"
            val parent = target.parentFile ?: error("Restored target has no parent: $target")
            target = File(parent, trashedFile.name + suffix)
            seq++
        }
        target.parentFile?.mkdirs()
        check(trashedFile.renameTo(target)) {
            "Failed to restore ${trashedFile.absolutePath} -> ${target.absolutePath}"
        }
        return target
    }

    /**
     * Permanently delete a single trashed item (file or directory tree).
     */
    fun deleteTrashedForever(trashedFile: File): Boolean =
        if (trashedFile.isDirectory) trashedFile.deleteRecursively() else trashedFile.delete()

    /**
     * Permanently delete every trashed item by removing the whole `.trash/` tree.
     */
    fun emptyTrash(): Boolean = trashRoot().deleteRecursively()

    /**
     * Delete trashed files older than [retentionDays], measured by mtime (refreshed
     * at trash time). A trashed group directory is removed recursively once it (or
     * every member) is expired. Best-effort: logs and skips on per-item failure.
     */
    fun purgeExpired(retentionDays: Long = TRASH_RETENTION_DAYS) {
        val root = trashRoot()
        if (!root.exists()) return
        val cutoff = System.currentTimeMillis() - retentionDays * 24L * 60L * 60L * 1000L
        root.walkTopDown().forEach { node ->
            if (!node.exists()) return@forEach
            if (node == root) return@forEach
            if (node.lastModified() < cutoff) {
                val deleted = if (node.isDirectory) node.deleteRecursively() else node.delete()
                if (!deleted) Timber.w("Failed to purge expired trash item %s", node.absolutePath)
            }
        }
    }

    /**
     * List trashed items for [space], newest-trashed first. Files only (a trashed
     * group is represented by its member files, mirroring how live entries list).
     * Never decrypts — type is derived from the filename via [FileMetadata].
     */
    fun listTrashed(space: Space): List<TrashedItem> {
        val spaceTrash = File(trashRoot(), space.folderName)
        if (!spaceTrash.exists()) return emptyList()
        return spaceTrash
            .walkTopDown()
            .filter { it.isFile }
            .mapNotNull { file ->
                val meta = FileMetadata.fromFile(file) ?: return@mapNotNull null
                if (meta.role == EntryRole.GROUP) return@mapNotNull null
                TrashedItem(
                    file = file,
                    type = meta.entryType,
                    space = space,
                    trashedAt = file.lastModified(),
                )
            }.sortedByDescending { it.trashedAt }
            .toList()
    }

    /**
     * Delete an entry file permanently (bypasses trash). Retained for explicit
     * destructive paths; normal delete routes through [trashEntry].
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
