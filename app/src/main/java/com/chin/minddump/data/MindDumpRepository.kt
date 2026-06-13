package com.chin.minddump.data

import android.net.Uri
import com.chin.minddump.security.CryptoEngine
import com.chin.minddump.security.PasswordStore
import com.chin.minddump.storage.EntryType
import com.chin.minddump.storage.FileStorageEngine
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.storage.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
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
         * Save a text entry: write file + insert Room row.
         * Encrypts if Private space.
         */
        suspend fun saveTextEntry(space: Space, content: String): MindDumpEntry =
            withContext(Dispatchers.IO) {
                Timber.d("Saving text entry in %s space", space.name)
                val file = storageEngine.saveTextEntry(space, content)
                val isEncrypted = space == Space.PRIVATE && sessionPassword != null
                val finalFile = if (isEncrypted) {
                    encryptFile(file, sessionPassword!!)
                } else {
                    file
                }
                val entry = MindDumpEntry(
                    file = finalFile,
                    type = EntryType.TEXT,
                    space = space,
                    dateFolder = finalFile.parentFile?.name ?: "",
                    timestamp = extractTimestamp(finalFile.name),
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
         * Get recording file.
         */
        fun getRecordingFile(space: Space): File =
            storageEngine.getRecordingFile(space)

        /**
         * Get photo file.
         */
        fun getPhotoFile(space: Space): File =
            storageEngine.getPhotoFile(space)

        /**
         * Get video file.
         */
        fun getVideoFile(space: Space): File =
            storageEngine.getVideoFile(space)

        /**
         * Register a media file in Room after capture.
         * Encrypts if Private space.
         */
        suspend fun registerMediaFile(file: File, space: Space) =
            withContext(Dispatchers.IO) {
                val isEncrypted = space == Space.PRIVATE && sessionPassword != null
                val finalFile = if (isEncrypted) {
                    encryptFile(file, sessionPassword!!)
                } else {
                    file
                }
                val entry = MindDumpEntry(
                    file = finalFile,
                    type = EntryType.fromFileName(file.name),
                    space = space,
                    dateFolder = finalFile.parentFile?.name ?: "",
                    timestamp = extractTimestamp(file.name),
                )
                dao.insert(entry.toEntity(isEncrypted = isEncrypted))
            }

        /**
         * Import a file: copy + insert Room row.
         * Encrypts if Private space.
         */
        suspend fun importFile(space: Space, uri: Uri, fileName: String): MindDumpEntry =
            withContext(Dispatchers.IO) {
                val file = storageEngine.importFile(space, uri, fileName)
                val isEncrypted = space == Space.PRIVATE && sessionPassword != null
                val finalFile = if (isEncrypted) {
                    encryptFile(file, sessionPassword!!)
                } else {
                    file
                }
                val entry = MindDumpEntry(
                    file = finalFile,
                    type = EntryType.fromFileName(file.name),
                    space = space,
                    dateFolder = finalFile.parentFile?.name ?: "",
                    timestamp = extractTimestamp(file.name),
                )
                dao.insert(entry.toEntity(isEncrypted = isEncrypted))
                entry
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
         * Bidirectional reconcile: sync Room with actual filesystem state.
         * - Disk has & DB doesn't → insert
         * - DB has & Disk doesn't → delete orphan
         * - Both have but stale → update contentPreview / lastModified
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
                            val preview = if (entry.type == EntryType.TEXT) {
                                try {
                                    entry.file.readText().take(500)
                                } catch (_: Exception) {
                                    ""
                                }
                            } else {
                                ""
                            }
                            val isEncrypted = cryptoEngine.isEncryptedFile(entry.file)
                            entry.toEntity(contentPreview = preview, isEncrypted = isEncrypted)
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
                        val preview = if (dbEntity.type == EntryType.TEXT) {
                            try {
                                diskEntry.file.readText().take(500)
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

        // --- Work directory delegation ---

        fun hasStoragePermission(): Boolean = storageEngine.hasStoragePermission()

        fun isWorkDirConfigured(): Boolean = storageEngine.isWorkDirConfigured()

        fun getRootDirPath(): String = storageEngine.getRootDirPath()

        fun setWorkDir(path: String) = storageEngine.setWorkDir(path)

        fun countFiles(): Int = storageEngine.countFiles()

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

        private fun extractTimestamp(fileName: String): String {
            val cleanName = fileName.removeSuffix(".enc")
            val parts = cleanName.split("_")
            return if (parts.size >= 2) parts[1] else ""
        }
    }
