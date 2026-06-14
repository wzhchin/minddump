package com.chin.minddump.data

import android.net.Uri
import com.chin.minddump.security.CryptoEngine
import com.chin.minddump.security.PasswordStore
import com.chin.minddump.storage.EntryRole
import com.chin.minddump.storage.EntryType
import com.chin.minddump.storage.FileMetadata
import com.chin.minddump.storage.FileStorageEngine
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.storage.Space
import com.chin.minddump.storage.TodoState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions")
class MindDumpRepository
    @Inject
    constructor(
        private val database: MindDumpDatabase,
        private val storageEngine: FileStorageEngine,
        private val cryptoEngine: CryptoEngine,
        private val passwordStore: PasswordStore,
    ) {
        private val dao get() = database.entryDao()

        // Session-cached password for Private space (cleared on app exit)
        @Volatile
        private var sessionPassword: String? = null

        /**
         * Check if Private space has a password set.
         */
        fun hasPrivatePassword(): Boolean = passwordStore.hasPassword()

        /**
         * Set the password for Private space (first-time setup).
         */
        fun setPassword(password: String) {
            Timber.i("Private password set")
            passwordStore.savePassword(password)
            sessionPassword = password
        }

        /**
         * Verify and cache the password for the current session.
         */
        fun verifyAndCachePassword(password: String): Boolean {
            val valid = passwordStore.verifyPassword(password)
            if (valid) {
                sessionPassword = password
            }
            return valid
        }

        /**
         * Clear the session password cache.
         */
        fun clearSessionPassword() {
            sessionPassword = null
        }

        /**
         * Check if session is unlocked (password cached).
         */
        fun isSessionUnlocked(): Boolean = sessionPassword != null

        /**
         * Observe entries for a space, sorted by lastModified DESC.
         */
        fun getEntries(space: Space): Flow<List<MindDumpEntry>> =
            dao.getAll(space).map { entities ->
                entities.map { it.toEntry() }
            }

        /**
         * Search entries using FTS.
         */
        fun searchEntries(space: Space, query: String): Flow<List<MindDumpEntry>> =
            dao.search(space, query).map { entities ->
                entities.map { it.toEntry() }
            }

        /**
         * Save a text entry: write file + encrypt if Private + insert Room row.
         * [targetGroupDir] non-null writes the entry directly into that group
         * directory and stamps its `groupPath`; null writes to the month top.
         */
        suspend fun saveTextEntry(
            space: Space,
            content: String,
            targetGroupDir: File? = null,
        ): MindDumpEntry =
            withContext(Dispatchers.IO) {
                Timber.d("Saving text entry in %s space", space.name)
                val file = storageEngine.saveTextEntry(space, content, targetGroupDir)
                val finalFile = encryptIfNeeded(file, space)
                val meta = FileMetadata.fromFile(finalFile)!!
                val isEncrypted = meta.isEncrypted
                val entry = MindDumpEntry(
                    file = finalFile,
                    type = meta.entryType,
                    space = space,
                    monthFolder = monthFolderOf(finalFile),
                    timestamp = meta.timestamp,
                    role = EntryRole.FILE,
                    groupPath = targetGroupDir?.absolutePath,
                )
                dao.insert(
                    entry.toEntity(
                        contentPreview = content.take(500),
                        isEncrypted = isEncrypted,
                    ),
                )
                entry
            }

        /**
         * Get recording file. [targetGroupDir] non-null targets an open group.
         */
        fun getRecordingFile(space: Space, targetGroupDir: File? = null): File =
            storageEngine.getRecordingFile(space, targetGroupDir)

        /**
         * Get photo file. [targetGroupDir] non-null targets an open group.
         */
        fun getPhotoFile(space: Space, targetGroupDir: File? = null): File =
            storageEngine.getPhotoFile(space, targetGroupDir)

        /**
         * Get video file. [targetGroupDir] non-null targets an open group.
         */
        fun getVideoFile(space: Space, targetGroupDir: File? = null): File =
            storageEngine.getVideoFile(space, targetGroupDir)

        /**
         * Register a media file in Room after capture.
         * Encrypts if Private space.
         */
        suspend fun registerMediaFile(file: File, space: Space) =
            withContext(Dispatchers.IO) {
                val finalFile = encryptIfNeeded(file, space)
                val meta = FileMetadata.fromFile(finalFile)!!
                val isEncrypted = meta.isEncrypted
                val entry = MindDumpEntry(
                    file = finalFile,
                    type = meta.entryType,
                    space = space,
                    monthFolder = monthFolderOf(finalFile),
                    timestamp = meta.timestamp,
                    role = EntryRole.FILE,
                    groupPath = groupPathOf(finalFile),
                )
                dao.insert(entry.toEntity(isEncrypted = isEncrypted))
            }

        /**
         * Import a file: copy + encrypt if Private + insert Room row.
         */
        suspend fun importFile(space: Space, uri: Uri, fileName: String): MindDumpEntry =
            withContext(Dispatchers.IO) {
                val file = storageEngine.importFile(space, uri, fileName)
                val finalFile = encryptIfNeeded(file, space)
                val meta = FileMetadata.fromFile(finalFile)!!
                val isEncrypted = meta.isEncrypted
                val entry = MindDumpEntry(
                    file = finalFile,
                    type = meta.entryType,
                    space = space,
                    monthFolder = monthFolderOf(finalFile),
                    timestamp = meta.timestamp,
                    role = EntryRole.FILE,
                    groupPath = groupPathOf(finalFile),
                )
                dao.insert(entry.toEntity(isEncrypted = isEncrypted))
                entry
            }

        /**
         * Save a comment targeting a specific entry.
         */
        suspend fun saveComment(
            space: Space,
            targetEntry: MindDumpEntry,
            content: String,
        ): MindDumpEntry =
            withContext(Dispatchers.IO) {
                val targetDir = targetEntry.file.parentFile ?: error("Target entry has no parent directory")
                val file = storageEngine.saveComment(targetDir, targetEntry.timestamp, content)
                val finalFile = encryptIfNeeded(file, space)
                val meta = FileMetadata.fromFile(finalFile)!!
                val isEncrypted = meta.isEncrypted
                val entry = MindDumpEntry(
                    file = finalFile,
                    type = meta.entryType,
                    space = space,
                    monthFolder = finalFile.parentFile?.name ?: "",
                    timestamp = meta.timestamp,
                    role = EntryRole.COMMENT,
                    targetTimestamp = targetEntry.timestamp,
                    groupPath = targetEntry.groupPath,
                )
                dao.insert(
                    entry.toEntity(
                        contentPreview = content.take(500),
                        isEncrypted = isEncrypted,
                    ),
                )
                entry
            }

        /**
         * Outcome of saving an edit to an existing entry.
         * [Locked] means the session password is gone, so the UI keeps the editor
         * open instead of discarding the user's text.
         */
        sealed interface EditSaveResult {
            data class Saved(
                val entry: MindDumpEntry
            ) : EditSaveResult
            data object Locked : EditSaveResult
        }

        /**
         * Overwrite an existing text/comment entry's content in place.
         * Preserves the file's path and identity; bumps lastModified so the entry
         * floats to the top of the feed. Re-encrypts when the entry was encrypted.
         * Returns [EditSaveResult.Locked] (instead of throwing) when an encrypted
         * entry can't be saved because the session is no longer unlocked.
         */
        suspend fun saveEntryEdit(entry: MindDumpEntry, newText: String): EditSaveResult =
            withContext(Dispatchers.IO) {
                val wasEncrypted = cryptoEngine.isEncryptedFile(entry.file)
                if (wasEncrypted) {
                    val password = sessionPassword
                        ?: return@withContext EditSaveResult.Locked
                    // Plaintext is written to a working file, then re-encrypted to the
                    // original .enc path (mirrors moveEntryToSpace's encrypt flow).
                    val workFile = File(storageEngine.getRootDir(), ".cache/${entry.file.nameWithoutExtension}")
                    workFile.parentFile?.mkdirs()
                    storageEngine.overwriteText(workFile, newText)
                    cryptoEngine.encryptFile(workFile, entry.file, password)
                    check(entry.file.exists() && entry.file.length() > 0) {
                        "Re-encryption failed for ${entry.file.absolutePath}"
                    }
                    workFile.delete()
                } else {
                    storageEngine.overwriteText(entry.file, newText)
                }

                dao.deleteByPath(entry.file.absolutePath)
                val refreshed = entry // path/type/role unchanged; lastModified flows in via toEntity
                dao.insert(
                    refreshed.toEntity(
                        contentPreview = newText.take(500),
                        isEncrypted = wasEncrypted,
                    ),
                )
                EditSaveResult.Saved(refreshed)
            }

        /**
         * Load the plaintext content of a text/comment entry for editing.
         * Decrypts to a cache temp file first when the entry is encrypted.
         */
        suspend fun loadEntryText(entry: MindDumpEntry): String =
            withContext(Dispatchers.IO) {
                if (cryptoEngine.isEncryptedFile(entry.file)) {
                    val password = sessionPassword ?: error("Session not unlocked")
                    val tempFile = File(storageEngine.getRootDir(), ".cache/${entry.file.nameWithoutExtension}")
                    tempFile.parentFile?.mkdirs()
                    cryptoEngine.decryptFile(entry.file, tempFile, password)
                    tempFile.readText()
                } else {
                    entry.file.readText()
                }
            }

        /**
         * Create a new group and return its directory.
         */
        suspend fun createGroup(space: Space, name: String?, parentGroupDir: File? = null): File =
            withContext(Dispatchers.IO) {
                storageEngine.createGroup(space, name, parentGroupDir)
            }

        /**
         * Create a group and move an entry into it in one operation.
         * [parentGroupDir] non-null creates a sub-group under it.
         */
        suspend fun createAndMoveToGroup(
            entry: MindDumpEntry,
            space: Space,
            name: String?,
            parentGroupDir: File? = null,
        ): File =
            withContext(Dispatchers.IO) {
                val groupDir = storageEngine.createGroup(space, name, parentGroupDir)
                moveToGroup(entry, groupDir)
                groupDir
            }

        /**
         * Create a group and move a batch of entries into it.
         * [parentGroupDir] non-null creates a sub-group under it.
         */
        suspend fun createAndMoveGroupForEntries(
            entries: List<MindDumpEntry>,
            space: Space,
            name: String?,
            parentGroupDir: File? = null,
        ): File =
            withContext(Dispatchers.IO) {
                val groupDir = storageEngine.createGroup(space, name, parentGroupDir)
                entries.forEach { moveToGroup(it, groupDir) }
                groupDir
            }

        /**
         * Move a batch of entries into an existing group directory.
         */
        suspend fun moveEntriesToGroup(entries: List<MindDumpEntry>, groupDir: File) =
            withContext(Dispatchers.IO) {
                entries.forEach { moveToGroup(it, groupDir) }
            }

        /**
         * Move an entry out of its group back to the month directory.
         */
        suspend fun moveEntryOutOfGroup(entry: MindDumpEntry, space: Space) =
            withContext(Dispatchers.IO) {
                moveOutOfGroup(entry, space)
            }

        /**
         * Rename a group directory and rewrite every member's groupPath.
         * Falls back to [reconcileWithDisk] to guarantee DB/disk consistency.
         */
        suspend fun renameGroup(groupDir: File, space: Space, newName: String?): File =
            withContext(Dispatchers.IO) {
                val oldPath = groupDir.absolutePath
                val members = dao.getEntriesInGroupSnapshot(oldPath)
                val newDir = storageEngine.renameGroupDir(groupDir, newName)
                val newPath = newDir.absolutePath
                members.forEach { entity ->
                    val movedFile = File(newPath, entity.filePath.substringAfterLast('/'))
                    dao.deleteByPath(entity.filePath)
                    dao.insert(
                        entity.copy(
                            filePath = movedFile.absolutePath,
                            groupPath = newPath,
                            lastModified = movedFile.lastModified(),
                        ),
                    )
                }
                reconcileWithDisk(space)
                newDir
            }

        /**
         * Dissolve a group: move every member back to the month directory and
         * delete the now-empty group directory. Files are preserved.
         */
        suspend fun dissolveGroup(groupDir: File, space: Space) =
            withContext(Dispatchers.IO) {
                val oldPath = groupDir.absolutePath
                val members = dao.getEntriesInGroupSnapshot(oldPath)
                storageEngine.dissolveGroup(groupDir)
                val monthDir = groupDir.parentFile
                members.forEach { entity ->
                    val movedFile = File(monthDir, entity.filePath.substringAfterLast('/'))
                    dao.deleteByPath(entity.filePath)
                    dao.insert(
                        entity.copy(
                            filePath = movedFile.absolutePath,
                            groupPath = null,
                            lastModified = movedFile.lastModified(),
                        ),
                    )
                }
                reconcileWithDisk(space)
            }

        /**
         * Move an entry into a group directory.
         */
        suspend fun moveToGroup(entry: MindDumpEntry, groupDir: File) =
            withContext(Dispatchers.IO) {
                val newFile = storageEngine.moveToGroup(entry.file, groupDir)
                val meta = FileMetadata.fromFile(newFile)!!
                val updatedEntry = entry.copy(
                    file = newFile,
                    groupPath = groupDir.absolutePath,
                )
                dao.deleteByPath(entry.file.absolutePath)
                dao.insert(updatedEntry.toEntity(isEncrypted = meta.isEncrypted))
            }

        /**
         * Move an entry out of its group back to the month directory.
         */
        suspend fun moveOutOfGroup(entry: MindDumpEntry, space: Space) =
            withContext(Dispatchers.IO) {
                val newFile = storageEngine.moveOutOfGroup(entry.file, space)
                val updatedEntry = entry.copy(
                    file = newFile,
                    groupPath = null,
                )
                val meta = FileMetadata.fromFile(newFile)!!
                dao.deleteByPath(entry.file.absolutePath)
                dao.insert(updatedEntry.toEntity(isEncrypted = meta.isEncrypted))
            }

        /**
         * Move an entry between Public and Private spaces.
         * Handles encrypt/decrypt as needed.
         */
        suspend fun moveEntryToSpace(entry: MindDumpEntry, targetSpace: Space) =
            withContext(Dispatchers.IO) {
                // Decrypt if currently encrypted
                var workingFile = entry.file
                if (cryptoEngine.isEncryptedFile(entry.file)) {
                    val password = sessionPassword ?: error("Session not unlocked")
                    val tempFile = File(storageEngine.getRootDir(), ".cache/${entry.file.nameWithoutExtension}")
                    if (!tempFile.parentFile?.exists()!!) tempFile.parentFile?.mkdirs()
                    cryptoEngine.decryptFile(entry.file, tempFile, password)
                    entry.file.delete()
                    workingFile = tempFile
                }

                // Move to target space directory
                val newFile = storageEngine.moveBetweenSpaces(workingFile, targetSpace)

                // Encrypt if target is Private and password is set
                val finalFile = if (targetSpace == Space.PRIVATE && sessionPassword != null) {
                    encryptFile(newFile, sessionPassword!!)
                } else {
                    newFile
                }

                val meta = FileMetadata.fromFile(finalFile)!!
                val updatedEntry = entry.copy(
                    file = finalFile,
                    space = targetSpace,
                    monthFolder = finalFile.parentFile?.name ?: "",
                    groupPath = null, // Moving between spaces leaves group
                )
                dao.deleteByPath(entry.file.absolutePath)
                dao.insert(updatedEntry.toEntity(isEncrypted = meta.isEncrypted))
            }

        /**
         * Rename the originalName portion of an entry's file.
         */
        suspend fun renameEntry(entry: MindDumpEntry, newName: String?): MindDumpEntry =
            withContext(Dispatchers.IO) {
                val newFile = storageEngine.renameEntry(entry.file, newName)
                val meta = FileMetadata.fromFile(newFile)!!
                val updatedEntry = entry.copy(file = newFile)
                dao.deleteByPath(entry.file.absolutePath)
                dao.insert(updatedEntry.toEntity(isEncrypted = meta.isEncrypted))
                updatedEntry
            }

        /**
         * Toggle the pin (置顶) state of a file entry. Pinning is encoded as a
         * `9999-` filename prefix; this renames the file on disk and re-indexes
         * the row so the feed reorders. Comments cannot be pinned.
         */
        suspend fun setEntryPinned(entry: MindDumpEntry, pinned: Boolean): MindDumpEntry =
            withContext(Dispatchers.IO) {
                val newFile = storageEngine.setEntryPinned(entry.file, pinned)
                val meta = FileMetadata.fromFile(newFile)!!
                val updatedEntry = entry.copy(file = newFile, isPinned = pinned)
                dao.deleteByPath(entry.file.absolutePath)
                dao.insert(updatedEntry.toEntity(isEncrypted = meta.isEncrypted))
                updatedEntry
            }

        /**
         * Set the todo status of a file entry, encoded as a status token in the
         * filename. Pass [TodoState.NONE] to clear. Comments cannot carry status.
         */
        suspend fun setEntryStatus(entry: MindDumpEntry, state: TodoState): MindDumpEntry =
            withContext(Dispatchers.IO) {
                val newFile = storageEngine.setEntryStatus(entry.file, state)
                val meta = FileMetadata.fromFile(newFile)!!
                val updatedEntry = entry.copy(file = newFile, todoState = state)
                dao.deleteByPath(entry.file.absolutePath)
                dao.insert(updatedEntry.toEntity(isEncrypted = meta.isEncrypted))
                updatedEntry
            }

        /**
         * Toggle the pin state of a group directory and re-index every member
         * row so group paths update.
         */
        suspend fun setGroupPinned(groupDir: File, space: Space, pinned: Boolean): File =
            withContext(Dispatchers.IO) {
                val oldPath = groupDir.absolutePath
                val members = dao.getEntriesInGroupSnapshot(oldPath)
                val newDir = storageEngine.setGroupPinned(groupDir, pinned)
                val newPath = newDir.absolutePath
                members.forEach { entity ->
                    val movedFile = File(newPath, entity.filePath.substringAfterLast('/'))
                    dao.deleteByPath(entity.filePath)
                    dao.insert(
                        entity.copy(
                            filePath = movedFile.absolutePath,
                            groupPath = newPath,
                            lastModified = movedFile.lastModified(),
                        ),
                    )
                }
                reconcileWithDisk(space)
                newDir
            }

        /**
         * Set the todo status of a group directory and re-index members.
         */
        suspend fun setGroupStatus(groupDir: File, space: Space, state: TodoState): File =
            withContext(Dispatchers.IO) {
                val oldPath = groupDir.absolutePath
                val members = dao.getEntriesInGroupSnapshot(oldPath)
                val newDir = storageEngine.setGroupStatus(groupDir, state)
                val newPath = newDir.absolutePath
                members.forEach { entity ->
                    val movedFile = File(newPath, entity.filePath.substringAfterLast('/'))
                    dao.deleteByPath(entity.filePath)
                    dao.insert(
                        entity.copy(
                            filePath = movedFile.absolutePath,
                            groupPath = newPath,
                            lastModified = movedFile.lastModified(),
                        ),
                    )
                }
                reconcileWithDisk(space)
                newDir
            }

        /**
         * Delete an entry: remove file + delete Room row.
         */
        suspend fun deleteEntry(entry: MindDumpEntry) =
            withContext(Dispatchers.IO) {
                Timber.d("Deleting entry: %s", entry.file.name)
                storageEngine.deleteEntry(entry)
                dao.deleteByPath(entry.file.absolutePath)
            }

        /**
         * Decrypt a file for viewing. Returns a temp file in cache.
         */
        suspend fun decryptForViewing(encryptedFile: File): File =
            withContext(Dispatchers.IO) {
                val password = sessionPassword ?: error("Session not unlocked")
                val cacheDir = File(storageEngine.getRootDir(), ".cache")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val tempFile = File(cacheDir, encryptedFile.nameWithoutExtension)
                cryptoEngine.decryptFile(encryptedFile, tempFile, password)
                tempFile
            }

        /**
         * Clean up temp decrypted files.
         */
        fun cleanDecryptedCache() {
            val cacheDir = File(storageEngine.getRootDir(), ".cache")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { it.delete() }
            }
        }

        /**
         * Wipe the database and repopulate it entirely from disk.
         * Files are the source of truth — nothing on disk is touched.
         * Clears both spaces then re-scans them, and rebuilds the FTS index.
         */
        suspend fun rebuildDatabase() =
            withContext(Dispatchers.IO) {
                Timber.i("Rebuilding database from disk")
                dao.clearAll()
                reconcileWithDisk(Space.PUBLIC)
                reconcileWithDisk(Space.PRIVATE)
                dao.rebuildFtsIndex()
                Timber.i("Database rebuild complete")
            }

        /**
         * Bidirectional reconcile: sync Room with actual filesystem state.
         * Derives role, targetTimestamp, groupPath from disk using FileMetadata.
         */
        suspend fun reconcileWithDisk(space: Space) =
            withContext(Dispatchers.IO) {
                Timber.d("Reconciling entries from disk for %s", space.name)
                val diskEntries = storageEngine.scanEntries(space)
                val diskPathMap = diskEntries.associateBy { it.file.absolutePath }
                val dbEntities = dao.getAllSnapshot(space)
                val dbPathMap = dbEntities.associateBy { it.filePath }

                // 1. Insert new (disk has, DB doesn't)
                val toInsert = diskEntries.filter { it.file.absolutePath !in dbPathMap }
                if (toInsert.isNotEmpty()) {
                    dao.insertAll(
                        toInsert.map { entry ->
                            val preview = contentPreviewOf(entry)
                            val isEncrypted = cryptoEngine.isEncryptedFile(entry.file)
                            // Derive targetTimestamp for comments from disk
                            val targetTs = if (entry.role == EntryRole.COMMENT) {
                                entry.timestamp // Already set by scanEntries
                            } else {
                                null
                            }
                            entry.copy(targetTimestamp = targetTs).toEntity(
                                contentPreview = preview,
                                isEncrypted = isEncrypted,
                            )
                        },
                    )
                }

                // 2. Delete orphans (DB has, disk doesn't)
                val orphanPaths = dbEntities
                    .filter { it.filePath !in diskPathMap }
                    .map { it.filePath }
                if (orphanPaths.isNotEmpty()) {
                    dao.deleteByPaths(orphanPaths)
                }

                // 3. Update stale entries (lastModified changed)
                val toUpdate = dbEntities
                    .filter { dbEntity ->
                        val diskEntry = diskPathMap[dbEntity.filePath] ?: return@filter false
                        dbEntity.lastModified != diskEntry.file.lastModified()
                    }.map { dbEntity ->
                        val diskEntry = diskPathMap[dbEntity.filePath]!!
                        val preview = if (diskEntry.type == EntryType.TEXT) {
                            try {
                                // For encrypted files, skip preview extraction
                                if (cryptoEngine.isEncryptedFile(diskEntry.file)) ""
                                else diskEntry.file.readText().take(500)
                            } catch (_: Exception) {
                                ""
                            }
                        } else {
                            ""
                        }
                        dbEntity.copy(
                            lastModified = diskEntry.file.lastModified(),
                            contentPreview = preview,
                            isEncrypted = cryptoEngine.isEncryptedFile(diskEntry.file),
                            groupPath = diskEntry.groupPath,
                            targetTimestamp = diskEntry.targetTimestamp,
                            isPinned = diskEntry.isPinned,
                            todoState = diskEntry.todoState,
                        )
                    }
                if (toUpdate.isNotEmpty()) {
                    dao.updateAll(toUpdate)
                }

                Timber.d(
                    "Reconcile %s: +%d inserted, -%d orphans, ~%d updated",
                    space.name,
                    toInsert.size,
                    orphanPaths.size,
                    toUpdate.size,
                )
            }

        /**
         * Scan group directories for a given space.
         */
        fun scanGroups(space: Space): List<File> =
            storageEngine.scanGroups(space)

        /**
         * Scan the direct sub-group directories under [parentDir] (a group page's
         * nested sub-groups, or a month directory's top-level groups).
         */
        fun scanChildGroups(parentDir: File): List<File> =
            storageEngine.scanChildGroups(parentDir)

        // --- Work directory delegation ---

        fun hasStoragePermission(): Boolean = storageEngine.hasStoragePermission()

        fun isWorkDirConfigured(): Boolean = storageEngine.isWorkDirConfigured()

        fun getRootDirPath(): String = storageEngine.getRootDirPath()

        fun setWorkDir(path: String) = storageEngine.setWorkDir(path)

        fun countFiles(): Int = storageEngine.countFiles()

        /** Total entry rows currently in the database (both spaces). */
        suspend fun countAllTotal(): Int = dao.countAll()

        fun countFilesIn(dir: File): Int = storageEngine.countFilesIn(dir)

        // --- Statistics delegation ---

        fun getEntryCountByDay(space: Space, limit: Int = 90): Flow<List<DayCount>> =
            dao.getEntryCountByDay(space, limit)

        fun getEntryCountByType(space: Space): Flow<List<TypeCount>> =
            dao.getEntryCountByType(space)

        fun getHourlyDistribution(space: Space): Flow<List<HourCount>> =
            dao.getHourlyDistribution(space)

        fun countFlow(space: Space): Flow<Int> =
            dao.countFlow(space)

        suspend fun migrateTo(newRoot: File) =
            withContext(Dispatchers.IO) {
                storageEngine.migrateTo(newRoot)
            }

        // --- Private helpers ---

        private fun encryptFile(source: File, password: String): File {
            val encryptedFile = File(source.parent, source.name + ".enc")
            cryptoEngine.encryptFile(source, encryptedFile, password)
            check(encryptedFile.exists() && encryptedFile.length() > 0) {
                "Encryption failed: output file missing or empty — keeping plaintext ${source.absolutePath}"
            }
            source.delete()
            return encryptedFile
        }

        private fun encryptIfNeeded(file: File, space: Space): File {
            if (space != Space.PRIVATE || sessionPassword == null) return file
            return encryptFile(file, sessionPassword!!)
        }

        /**
         * Resolve the YYYY-MM month folder for a file, walking up from nested
         * group directories to the month directory that contains them.
         */
        private fun monthFolderOf(file: File): String {
            val monthPattern = Regex("""^\d{4}-\d{2}$""")
            var node: File? = file.parentFile
            while (node != null) {
                if (monthPattern.matches(node.name)) return node.name
                node = node.parentFile
            }
            return file.parentFile?.name ?: ""
        }

        /**
         * The immediate containing group directory for [file], or null if it sits
         * directly in a month directory. Determined from the parent dir's role.
         */
        private fun groupPathOf(file: File): String? {
            val parent = file.parentFile ?: return null
            return if (FileMetadata.fromFile(parent)?.role == EntryRole.GROUP) parent.absolutePath else null
        }

        private fun contentPreviewOf(entry: MindDumpEntry): String =
            if (entry.type == EntryType.TEXT && !cryptoEngine.isEncryptedFile(entry.file)) {
                try {
                    entry.file.readText().take(500)
                } catch (_: Exception) {
                    ""
                }
            } else {
                ""
            }
    }
