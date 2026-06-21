package com.chin.minddump.data

import android.net.Uri
import com.chin.minddump.notification.EventScheduler
import com.chin.minddump.security.CryptoEngine
import com.chin.minddump.security.PasswordStore
import com.chin.minddump.storage.EntryEvent
import com.chin.minddump.storage.EntryMeta
import com.chin.minddump.storage.EventState
import com.chin.minddump.storage.FileMetadata
import com.chin.minddump.storage.FileStorageEngine
import com.chin.minddump.storage.MetaYamlCodec
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.storage.Space
import com.chin.minddump.storage.TagValidator
import com.chin.minddump.storage.TrashedItem
import com.chin.minddump.storage.TodoState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions", "LargeClass")
class MindDumpRepository
    @Inject
    constructor(
        private val database: MindDumpDatabase,
        private val storageEngine: FileStorageEngine,
        private val cryptoEngine: CryptoEngine,
        private val passwordStore: PasswordStore,
        private val eventScheduler: EventScheduler,
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
         * Called after Private is unlocked: decrypt every Private owner's sidecar
         * and backfill its tags/events into Room (clearing [MindDumpEntry.metaEncrypted]).
         * Returns all now-readable [EntryEvent]s with their owning entry, so the
         * scheduler can register future-dues (past-dues are NOT retro-fired).
         */
        suspend fun onPrivateUnlocked(): List<Pair<MindDumpEntry, EntryEvent>> =
            withContext(Dispatchers.IO) {
                val owners = dao.getAllSnapshot(Space.PRIVATE)
                val firedKeys = mutableListOf<Pair<MindDumpEntry, EntryEvent>>()
                owners.forEach { entity ->
                    val entry = entity.toEntry(tags = dao.tagsFor(entity.filePath))
                    val meta = loadEntryMeta(entry)
                    if (meta.isEmpty && !hasSidecar(entry)) {
                        upsertEntry(entry.copy(metaEncrypted = false))
                        return@forEach
                    }
                    val refreshed = entry.copy(
                        tags = meta.tags,
                        events = meta.events,
                        metaEncrypted = false,
                    )
                    upsertEntry(refreshed)
                    replaceEventRows(refreshed, meta.events)
                    // Register only future pending events; past-dues stay silent (no retro-fire).
                    registerEvents(refreshed)
                    meta.events.forEach { ev -> firedKeys.add(refreshed to ev) }
                }
                firedKeys
            }

        /** Whether an owner currently has a sidecar file on disk. */
        private fun hasSidecar(entry: MindDumpEntry): Boolean =
            runCatching {
                storageEngine.sidecarFileFor(entry.file, entry.space == Space.PRIVATE).exists()
            }.getOrDefault(false)

        /**
         * Attach an owner's tags AND events (from the relation tables) so the feed
         * footer can render tag chips and the next-event reminder chip. Events are
         * needed for display (the reminder); without this join `entry.events` would
         * be empty and reminders would never appear on cards.
         */
        private suspend fun EntryEntity.withMeta(): MindDumpEntry =
            toEntry(
                tags = dao.tagsFor(filePath),
                events = dao.eventsFor(filePath).map { it.toEntryEvent() },
            )

        /**
         * Observe entries for a space (content + group rows, newest first), with each
         * owner's tags/events joined in. Comments are removed.
         */
        fun getEntries(space: Space): Flow<List<MindDumpEntry>> =
            dao.getAll(space).map { entities -> entities.map { it.withMeta() } }

        /** One-shot snapshot of all entries in a space (for boot re-registration). */
        suspend fun getAllEntries(space: Space): List<MindDumpEntry> =
            withContext(Dispatchers.IO) {
                dao.getAllSnapshot(space).map { it.withMeta() }
            }

        /** Observe entries in [space] carrying [tag] (joined via the tags table). */
        fun getEntriesByTag(space: Space, tag: String): Flow<List<MindDumpEntry>> =
            dao.getByTag(space, tag).map { entities -> entities.map { it.withMeta() } }

        /**
         * Search entries whose content contains [query] as a substring.
         *
         * Uses a GLOB substring match on the raw `contentPreview` column rather
         * than the FTS index, so CJK phrases match correctly without a tokenizer.
         * Returns an empty flow for a blank query so we never run an all-matching
         * `**` scan.
         */
        fun searchEntries(space: Space, query: String): Flow<List<MindDumpEntry>> {
            val pattern = SearchGlob.toPattern(query) ?: return flowOf(emptyList())
            return dao.search(space, pattern).map { entities -> entities.map { it.withMeta() } }
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
                    groupPath = groupPathOf(targetGroupDir),
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
                    groupPath = groupPathOf(finalFile.parentFile),
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
                    groupPath = groupPathOf(finalFile.parentFile),
                )
                dao.insert(entry.toEntity(isEncrypted = isEncrypted))
                entry
            }

        /**
         * Outcome of saving an edit to an existing entry.
         * [Locked] means the session password is gone, so the UI keeps the editor
         * open instead of discarding the user's text.
         */
        sealed interface EditSaveResult {
            data class Saved(
                val entry: MindDumpEntry,
            ) : EditSaveResult
            data object Locked : EditSaveResult
        }

        /**
         * Outcome of resolving entries for outbound sharing.
         * [Payload] carries the plaintext files ready to hand to a share intent;
         * [Locked] means an encrypted Private entry was requested while the
         * session password is gone, so nothing is shared.
         */
        sealed interface ShareResult {
            data class Payload(
                val files: List<File>,
            ) : ShareResult
            data object Locked : ShareResult
        }

        /**
         * Overwrite an existing text entry's content in place.
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

                upsertEntry(
                    entry,
                    contentPreview = newText.take(500),
                    isEncrypted = wasEncrypted,
                )
                EditSaveResult.Saved(entry)
            }

        /**
         * Load the plaintext content of a text entry for editing.
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

        // ── Metadata sidecar (tags + scheduled events) ──

        /**
         * Load the parsed [EntryMeta] for [entry], decrypting the Private sidecar
         * when the session is unlocked. Returns [EntryMeta.EMPTY] when there is no
         * sidecar or the Private sidecar is still encrypted (locked).
         */
        suspend fun loadEntryMeta(entry: MindDumpEntry): EntryMeta =
            withContext(Dispatchers.IO) {
                val encrypted = entry.space == Space.PRIVATE
                val sidecar = storageEngine.sidecarFileFor(entry.file, encrypted)
                if (!sidecar.exists()) return@withContext EntryMeta.EMPTY
                val text = if (encrypted) {
                    val password = sessionPassword ?: return@withContext EntryMeta.EMPTY
                    val temp = File(storageEngine.getRootDir(), ".cache/${sidecar.nameWithoutExtension}")
                    temp.parentFile?.mkdirs()
                    runCatching {
                        cryptoEngine.decryptFile(sidecar, temp, password)
                        temp.readText().also { temp.delete() }
                    }.getOrElse {
                        Timber.w(it, "Failed to decrypt sidecar %s", sidecar.name)
                        return@withContext EntryMeta.EMPTY
                    }
                } else {
                    storageEngine.readSidecarText(sidecar)
                }
                MetaYamlCodec.decode(text)
            }

        /**
         * Persist [meta] as the sidecar for [entry], encrypting in Private.
         * Removes the sidecar file entirely when [meta] is empty (no tags and no
         * events), so no empty `.meta` lingers. Refreshes the owner's Room row.
         */
        suspend fun saveEntryMeta(entry: MindDumpEntry, meta: EntryMeta): MindDumpEntry =
            withContext(Dispatchers.IO) {
                val encrypted = entry.space == Space.PRIVATE
                val sidecar = storageEngine.sidecarFileFor(entry.file, encrypted)

                if (meta.isEmpty) {
                    sidecar.delete()
                } else {
                    val yaml = MetaYamlCodec.encode(meta)
                    if (encrypted) {
                        val password = sessionPassword ?: error("Session not unlocked for Private meta")
                        val work = File(storageEngine.getRootDir(), ".cache/${sidecar.nameWithoutExtension}")
                        work.parentFile?.mkdirs()
                        storageEngine.writeSidecarText(work, yaml)
                        cryptoEngine.encryptFile(work, sidecar, password)
                        work.delete()
                    } else {
                        storageEngine.writeSidecarText(sidecar, yaml)
                    }
                }

                // Refresh the owner row + its tag/event rows so the bumped sidecar
                // mtime and new tags/events flow in.
                val refreshed = entry.copy(
                    tags = meta.tags,
                    events = meta.events,
                    metaEncrypted = false,
                )
                upsertEntry(
                    refreshed,
                    contentPreview = contentPreviewOf(refreshed),
                    isEncrypted = cryptoEngine.isEncryptedFile(entry.file),
                    lastModified = effectiveLastModified(refreshed),
                )
                replaceTagRows(refreshed.file.absolutePath, meta.tags)
                replaceEventRows(refreshed, meta.events)
                refreshed
            }

        /** Replace an owner's tag rows. */
        private suspend fun replaceTagRows(filePath: String, tags: List<String>) {
            dao.deleteTagsFor(filePath)
            if (tags.isNotEmpty()) dao.insertTags(tags.map { TagEntity(filePath, it) })
        }

        /** Replace an owner's event rows (sidecar is authority; rows re-register alarms). */
        private suspend fun replaceEventRows(entry: MindDumpEntry, events: List<EntryEvent>) {
            val path = entry.file.absolutePath
            dao.deleteEventsFor(path)
            if (events.isNotEmpty()) {
                dao.insertEvents(events.map { it.toEventEntity(path) })
            }
        }

        /** Add a tag to [entry] (case-insensitive dedup). No-op if invalid/present. */
        suspend fun addTag(entry: MindDumpEntry, tag: String): MindDumpEntry =
            withContext(Dispatchers.IO) {
                val meta = loadEntryMeta(entry)
                val updated = meta.copy(tags = TagValidator.addUnique(meta.tags, tag))
                if (updated.tags == meta.tags) entry else saveEntryMeta(entry, updated)
            }

        /** Remove a tag from [entry] (case-insensitive match). */
        suspend fun removeTag(entry: MindDumpEntry, tag: String): MindDumpEntry =
            withContext(Dispatchers.IO) {
                val meta = loadEntryMeta(entry)
                val updated = meta.copy(tags = TagValidator.remove(meta.tags, tag))
                saveEntryMeta(entry, updated)
            }

        /** Schedule a [once] pending event on [entry] and register its alarm. */
        suspend fun addEvent(entry: MindDumpEntry, event: EntryEvent): MindDumpEntry =
            withContext(Dispatchers.IO) {
                val meta = loadEntryMeta(entry)
                val updated = meta.copy(events = meta.events + event)
                val refreshed = saveEntryMeta(entry, updated)
                scheduleEvent(refreshed, event)
                refreshed
            }

        /**
         * Mark a fired event's state by its DB row id. Used by the alarm receiver
         * after firing. Persists to the sidecar (authority) and updates the row.
         */
        suspend fun markEventFired(
            ownerPath: String,
            eventId: Long,
        ) = withContext(Dispatchers.IO) {
            val entity = dao.findByPath(ownerPath) ?: return@withContext
            val entry = entity.toEntry(tags = dao.tagsFor(entity.filePath))
            val meta = loadEntryMeta(entry)
            val events = dao.eventsFor(entity.filePath)
            val fired = events.firstOrNull { it.id == eventId } ?: return@withContext
            val updatedMeta = meta.copy(
                events = meta.events.map { ev ->
                    if (ev.due.toString() == fired.due) ev.copy(state = EventState.FIRED) else ev
                },
            )
            // Sidecar is authority; refresh both sidecar and the events row.
            saveEntryMeta(entry, updatedMeta)
        }

        /**
         * Remove a single event (matched by its due time, the sidecar identity):
         * drop it from the sidecar (deleting the sidecar when it becomes empty) and
         * cancel its registered alarm by row id.
         */
        suspend fun removeEvent(entry: MindDumpEntry, event: EntryEvent): MindDumpEntry =
            withContext(Dispatchers.IO) {
                val row = dao.eventsFor(entry.file.absolutePath).firstOrNull { it.due == event.due.toString() }
                val meta = loadEntryMeta(entry)
                val updated = meta.copy(events = meta.events.filterNot { it == event })
                val refreshed = saveEntryMeta(entry, updated)
                row?.let { eventScheduler.cancelById(it.id) }
                refreshed
            }

        /** Snapshot all distinct tags in a space (for autocomplete). */
        suspend fun distinctTags(space: Space): List<String> =
            withContext(Dispatchers.IO) { dao.distinctTags(space) }

        /** Register one event's alarm (cancel-then-set by event row id, idempotent). */
        private suspend fun scheduleEvent(entry: MindDumpEntry, event: EntryEvent) {
            val eventId = dao
                .eventsFor(entry.file.absolutePath)
                .firstOrNull { it.due == event.due.toString() }
                ?.id ?: return
            eventScheduler.schedule(
                owner = entry.file,
                ownerName = entry.file.name,
                space = entry.space,
                eventId = eventId,
                dueAtMillis = event.dueMillis(),
                alreadyFired = event.state == EventState.FIRED,
            )
        }

        /** Register all pending/future events for one entry (post-unlock, post-edit). */
        suspend fun registerEvents(entry: MindDumpEntry) {
            val events = dao.eventsFor(entry.file.absolutePath).map { it.toEntryEvent() }
            events.forEach { ev -> if (ev.state != EventState.FIRED) scheduleEvent(entry, ev) }
        }

        /**
         * Register all pending Public events (e.g. at app start, after reconcile).
         * Private events are registered on unlock via [onPrivateUnlocked].
         */
        suspend fun registerAllPublicEvents() = withContext(Dispatchers.IO) {
            getAllEntries(Space.PUBLIC).forEach { entry -> registerEvents(entry) }
        }

        /**
         * Create a new group and return its directory. Groups are single-level, so
         * [parentGroupDir] (if passed) is ignored — a group is always created at the
         * month-top level.
         */
        suspend fun createGroup(space: Space, name: String?, parentGroupDir: File? = null): File =
            withContext(Dispatchers.IO) {
                @Suppress("UNUSED_PARAMETER")
                val ignoredNesting = parentGroupDir // single-level: nesting refused
                storageEngine.createGroup(space, name, parentGroupDir).also { groupDir ->
                    indexGroupDir(groupDir, space)
                }
            }

        /** Insert/refresh a group container row (type=GROUP) with filePath + groupPath from disk. */
        private suspend fun indexGroupDir(groupDir: File, space: Space) {
            val meta = FileMetadata.fromFile(groupDir)
            dao.insert(
                EntryEntity(
                    filePath = groupDir.absolutePath,
                    type = com.chin.minddump.storage.EntryType.GROUP,
                    space = space,
                    monthFolder = monthFolderOf(groupDir),
                    lastModified = groupDir.lastModified(),
                    groupPath = null, // groups live at the month-top level
                    isPinned = meta?.isPinned ?: false,
                    todoState = meta?.todoState ?: TodoState.NONE,
                ),
            )
        }

        /**
         * Create a group and move an entry into it in one operation. Groups are
         * single-level, so [parentGroupDir] (if passed) is ignored.
         */
        suspend fun createAndMoveToGroup(
            entry: MindDumpEntry,
            space: Space,
            name: String?,
            parentGroupDir: File? = null,
        ): File =
            withContext(Dispatchers.IO) {
                val groupDir = storageEngine.createGroup(space, name, parentGroupDir)
                indexGroupDir(groupDir, space)
                moveToGroup(entry, groupDir)
                groupDir
            }

        /**
         * Create a group and move a batch of entries into it. Single-level, so
         * [parentGroupDir] (if passed) is ignored.
         */
        suspend fun createAndMoveGroupForEntries(
            entries: List<MindDumpEntry>,
            space: Space,
            name: String?,
            parentGroupDir: File? = null,
        ): File =
            withContext(Dispatchers.IO) {
                val groupDir = storageEngine.createGroup(space, name, parentGroupDir)
                indexGroupDir(groupDir, space)
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
         * Rename a group directory: the group row's filePath changes (identity is
         * the path), so the row is re-keyed; member rows keep their groupPath but
         * it now points at the new directory — reconcile re-stamps them.
         */
        suspend fun renameGroup(groupDir: File, space: Space, newName: String?): File =
            withContext(Dispatchers.IO) {
                val newDir = storageEngine.renameGroupDir(groupDir, newName)
                reconcileWithDisk(space)
                newDir
            }

        /**
         * Dissolve a group: move every member back to the month-top level and
         * delete the now-empty group directory. Single-level, so there is no
         * parent-group reparenting — members always go to the month directory.
         */
        suspend fun dissolveGroup(groupDir: File, space: Space) =
            withContext(Dispatchers.IO) {
                storageEngine.dissolveGroup(groupDir)
                reconcileWithDisk(space)
            }

        /**
         * Remove a group directory that is already empty (its members moved
         * elsewhere, e.g. after a multi-select re-cluster merge). Reconciles so
         * the dropped group no longer appears in the index. Idempotent.
         */
        suspend fun removeEmptyGroupDir(groupDir: File, space: Space) =
            withContext(Dispatchers.IO) {
                storageEngine.removeEmptyGroupDir(groupDir)
                reconcileWithDisk(space)
            }

        /**
         * Move an entry into a group directory. Identity (path) changes; the row
         * is re-keyed at the new path with the group's path as groupPath.
         */
        suspend fun moveToGroup(entry: MindDumpEntry, groupDir: File) =
            withContext(Dispatchers.IO) {
                val newFile = storageEngine.moveToGroup(entry.file, groupDir)
                val meta = FileMetadata.fromFile(newFile)!!
                moveEntryRow(entry, newFile, meta.isEncrypted) { it.copy(groupPath = groupDir.absolutePath) }
            }

        /**
         * Move an entry out of its group back to the month directory.
         */
        suspend fun moveOutOfGroup(entry: MindDumpEntry, space: Space) =
            withContext(Dispatchers.IO) {
                val newFile = storageEngine.moveOutOfGroup(entry.file, space)
                val meta = FileMetadata.fromFile(newFile)!!
                moveEntryRow(entry, newFile, meta.isEncrypted) { it.copy(groupPath = null) }
            }

        /**
         * Re-index an entry row after its file moved: delete the old path row,
         * insert at the new path with the [transform] applied (e.g. groupPath change).
         */
        private suspend fun moveEntryRow(
            entry: MindDumpEntry,
            newFile: File,
            isEncrypted: Boolean,
            transform: (EntryEntity) -> EntryEntity,
        ) {
            dao.deleteByPath(entry.file.absolutePath)
            dao.insert(
                transform(
                    entry
                        .copy(file = newFile)
                        .toEntity(
                            contentPreview = contentPreviewOf(entry.copy(file = newFile)),
                            isEncrypted = isEncrypted,
                        ).copy(lastModified = newFile.lastModified()),
                ),
            )
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
                moveEntryRow(entry, finalFile, meta.isEncrypted) {
                    it.copy(space = targetSpace, monthFolder = monthFolderOf(finalFile), groupPath = null)
                }
            }

        /**
         * Rename the originalName portion of an entry's file.
         */
        suspend fun renameEntry(entry: MindDumpEntry, newName: String?): MindDumpEntry =
            withContext(Dispatchers.IO) {
                val newFile = storageEngine.renameEntrySlug(entry.file, newName)
                val meta = FileMetadata.fromFile(newFile)!!
                moveEntryRow(entry, newFile, meta.isEncrypted) { it }
                entry.copy(file = newFile)
            }

        /**
         * Toggle the pin (置顶) state of a file entry. Pinning is encoded as a
         * `9999-` filename prefix; this renames the file on disk (carrying its
         * sidecar) and re-indexes the row so the feed reorders.
         */
        suspend fun setEntryPinned(entry: MindDumpEntry, pinned: Boolean): MindDumpEntry =
            withContext(Dispatchers.IO) {
                val newFile = storageEngine.setEntryPinned(entry.file, pinned)
                val meta = FileMetadata.fromFile(newFile)!!
                moveEntryRow(entry, newFile, meta.isEncrypted) { it.copy(isPinned = pinned) }
                entry.copy(file = newFile, isPinned = pinned)
            }

        /**
         * Set the todo status of a file entry, encoded as a status token in the
         * filename. Pass [TodoState.NONE] to clear.
         */
        suspend fun setEntryStatus(entry: MindDumpEntry, state: TodoState): MindDumpEntry =
            withContext(Dispatchers.IO) {
                val newFile = storageEngine.setEntryStatus(entry.file, state)
                val meta = FileMetadata.fromFile(newFile)!!
                moveEntryRow(entry, newFile, meta.isEncrypted) { it.copy(todoState = state) }
                entry.copy(file = newFile, todoState = state)
            }

        /**
         * Toggle the pin state of a group directory: rename on disk (carrying its
         * sidecar) + reconcile so the row re-keys at the new path.
         */
        suspend fun setGroupPinned(groupDir: File, space: Space, pinned: Boolean): File =
            withContext(Dispatchers.IO) {
                val newDir = storageEngine.setGroupPinned(groupDir, pinned)
                reconcileWithDisk(space)
                newDir
            }

        /**
         * Set the todo status of a group directory: rename on disk (carrying its
         * sidecar) + reconcile.
         */
        suspend fun setGroupStatus(groupDir: File, space: Space, state: TodoState): File =
            withContext(Dispatchers.IO) {
                val newDir = storageEngine.setGroupStatus(groupDir, state)
                reconcileWithDisk(space)
                newDir
            }

        /**
         * Delete an entry: move its file to `.trash/` (recoverable) and drop the
         * Room row so it leaves the feed immediately. Restore re-indexes it via the
         * normal reconcile once the file is back at a live path.
         */
        suspend fun deleteEntry(entry: MindDumpEntry) =
            withContext(Dispatchers.IO) {
                Timber.d("Trashing entry: %s", entry.file.name)
                storageEngine.trashEntry(entry)
                dao.deleteByPath(entry.file.absolutePath)
            }

        /**
         * Soft-delete a group directory: move the whole tree into `.trash/` and drop
         * every member + the group row. Members travel with the group and restore together.
         */
        suspend fun deleteGroup(groupDir: File, space: Space) =
            withContext(Dispatchers.IO) {
                Timber.d("Trashing group: %s", groupDir.name)
                // Drop the group row and all its direct members (single level).
                deleteGroupSubtree(groupDir.absolutePath)
                storageEngine.trashGroup(groupDir, space)
            }

        /**
         * Delete a group row and every direct member beneath it. Single-level, so
         * members are exactly the rows whose groupPath matches the group's path.
         */
        private suspend fun deleteGroupSubtree(groupPath: String) {
            dao.getMembersSnapshot(groupPath).forEach { member -> dao.deleteByPath(member.filePath) }
            dao.deleteByPath(groupPath)
        }

        /**
         * Restore a trashed item back to its live location, then reconcile so the
         * Room row returns.
         */
        suspend fun restoreTrashed(trashedFile: File, space: Space) =
            withContext(Dispatchers.IO) {
                storageEngine.restoreTrashed(trashedFile, space)
                reconcileWithDisk(space)
            }

        /**
         * Permanently delete a single trashed item. No Room row to touch — it was
         * already removed at trash time.
         */
        suspend fun deleteTrashedForever(trashedFile: File) =
            withContext(Dispatchers.IO) {
                storageEngine.deleteTrashedForever(trashedFile)
            }

        /**
         * Permanently delete every trashed item.
         */
        suspend fun emptyTrash() =
            withContext(Dispatchers.IO) {
                storageEngine.emptyTrash()
            }

        /**
         * Purge trashed items older than the retention window. Best-effort.
         */
        suspend fun purgeExpiredTrash() =
            withContext(Dispatchers.IO) {
                runCatching { storageEngine.purgeExpired() }
                    .onFailure { Timber.e(it, "Trash purge failed") }
            }

        /**
         * List trashed items for [space], newest-trashed first.
         */
        fun listTrashed(space: Space): List<TrashedItem> = storageEngine.listTrashed(space)

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
         * Resolve a batch of entries to plaintext [File]s for outbound sharing.
         * Encrypted Private (`.enc`) entries are decrypted to `.cache/` under the
         * FileProvider-exposed root; plaintext entries are returned as-is. The
         * decrypted temp files share the same lifecycle as view-decryption files
         * and are cleared by [cleanDecryptedCache].
         *
         * Returns [ShareResult.Locked] (without preparing anything) when any
         * encrypted entry is requested while the session password is gone.
         */
        suspend fun prepareEntriesForShare(entries: List<MindDumpEntry>): ShareResult =
            withContext(Dispatchers.IO) {
                // Bail before any side effect if we'd hit a locked encrypted entry.
                val needsPassword = entries.any { cryptoEngine.isEncryptedFile(it.file) }
                if (needsPassword && sessionPassword == null) {
                    return@withContext ShareResult.Locked
                }

                val resolved = entries.map { entry -> resolveShareFile(entry) }
                ShareResult.Payload(resolved)
            }

        /**
         * Resolve one entry to a shareable plaintext [File]: decrypt `.enc`
         * entries to a `.cache/` temp file, return plaintext entries directly.
         */
        private fun resolveShareFile(entry: MindDumpEntry): File =
            if (cryptoEngine.isEncryptedFile(entry.file)) {
                val cacheDir = File(storageEngine.getRootDir(), ".cache")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val tempFile = File(cacheDir, entry.file.nameWithoutExtension)
                cryptoEngine.decryptFile(entry.file, tempFile, sessionPassword!!)
                tempFile
            } else {
                entry.file
            }

        /**
         * Load the member entries of a group directory for whole-group sharing.
         */
        suspend fun getGroupMemberEntries(groupDir: File): List<MindDumpEntry> =
            withContext(Dispatchers.IO) {
                dao.getMembersSnapshot(groupDir.absolutePath).map {
                    it.toEntry(tags = dao.tagsFor(it.filePath))
                }
            }

        /**
         * Wipe the database and repopulate it entirely from disk.
         * Files are the source of truth — nothing on disk is touched.
         * Clears entries/tags/events then re-scans both spaces, and rebuilds FTS.
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
         * Reconcile: sync Room with actual filesystem state. Disk is the source of
         * truth. Single-pass: identity (filePath) and groupPath are derivable from
         * disk position, so no insert ordering is needed. Comments are gone.
         */
        @Suppress("CyclomaticComplexity", "LongMethod")
        suspend fun reconcileWithDisk(space: Space) =
            withContext(Dispatchers.IO) {
                Timber.d("Reconciling entries from disk for %s", space.name)
                // Opportunistic retention: drop expired trash before re-scanning.
                runCatching { storageEngine.purgeExpired() }
                    .onFailure { Timber.e(it, "Trash purge failed") }
                val diskEntries = storageEngine.scanEntries(space)
                val diskPathMap = diskEntries.associateBy { it.file.absolutePath }
                val dbEntities = dao.getAllSnapshot(space)

                // 1. Insert/update content + group rows (disk has it).
                diskEntries.forEach { entry ->
                    val preview = contentPreviewOf(entry)
                    val isEncrypted = cryptoEngine.isEncryptedFile(entry.file)
                    dao.deleteByPath(entry.file.absolutePath)
                    dao.insert(
                        entry
                            .toEntity(contentPreview = preview, isEncrypted = isEncrypted)
                            .copy(lastModified = effectiveLastModified(entry)),
                    )
                    replaceTagRows(entry.file.absolutePath, entry.tags)
                    if (!entry.metaEncrypted) replaceEventRows(entry, entry.events)
                }

                // 2. Delete orphans (DB has, disk doesn't).
                val orphanEntryPaths = dbEntities
                    .filter { it.filePath !in diskPathMap }
                    .map { it.filePath }
                if (orphanEntryPaths.isNotEmpty()) dao.deleteByPaths(orphanEntryPaths)

                Timber.d(
                    "Reconcile %s: ~%d entries, -%d orphans",
                    space.name,
                    diskEntries.size,
                    orphanEntryPaths.size,
                )
            }

        /**
         * Scan group directories for a given space.
         */
        fun scanGroups(space: Space): List<File> =
            storageEngine.scanGroups(space)

        /**
         * Scan the direct group directories under [parentDir] (a month directory).
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
         * Resolve the YYYY-MM month folder for a file, walking up from a group
         * directory to the month directory that contains it.
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
         * Insert (or replace) an entry row at its current path, honoring an optional
         * [contentPreview], [isEncrypted], and [lastModified] override. Centralizes
         * the delete-then-insert re-keying that identity-by-path requires.
         */
        private suspend fun upsertEntry(
            entry: MindDumpEntry,
            contentPreview: String = contentPreviewOf(entry),
            isEncrypted: Boolean = cryptoEngine.isEncryptedFile(entry.file),
            lastModified: Long = entry.file.lastModified(),
        ) {
            dao.deleteByPath(entry.file.absolutePath)
            dao.insert(
                entry
                    .toEntity(contentPreview = contentPreview, isEncrypted = isEncrypted)
                    .copy(lastModified = lastModified),
            )
        }

        /**
         * The path of the group containing [dir], or null when [dir] is not itself a
         * group directory (the entry sits at the month-top level). Used to resolve
         * groupPath for a newly written entry. Identity/groupPath are absolute paths
         * here; they are stored verbatim (consistent across a session's workDir).
         */
        private fun groupPathOf(dir: File?): String? {
            val parent = dir ?: return null
            return if (FileMetadata.fromFile(parent)?.role == com.chin.minddump.storage.EntryRole.GROUP) {
                parent.absolutePath
            } else {
                null
            }
        }

        private fun contentPreviewOf(entry: MindDumpEntry): String =
            if (entry.type == com.chin.minddump.storage.EntryType.TEXT &&
                !cryptoEngine.isEncryptedFile(entry.file)
            ) {
                try {
                    entry.file.readText().take(500)
                } catch (_: Exception) {
                    ""
                }
            } else {
                ""
            }

        /**
         * The mtime used for staleness: the later of the owner file and its
         * metadata sidecar, so editing only the sidecar (a tag/event change)
         * still marks the entry stale and refreshes Room. Falls back to the
         * owner mtime when no sidecar exists.
         */
        private fun effectiveLastModified(entry: MindDumpEntry): Long {
            val ownerMtime = entry.file.lastModified()
            val encrypted = entry.space == Space.PRIVATE
            val sidecar = runCatching {
                storageEngine.sidecarFileFor(entry.file, encrypted)
            }.getOrNull() ?: return ownerMtime
            if (!sidecar.exists()) return ownerMtime
            return maxOf(ownerMtime, sidecar.lastModified())
        }
    }
