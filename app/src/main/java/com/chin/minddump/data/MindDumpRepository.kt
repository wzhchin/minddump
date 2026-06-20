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
import com.chin.minddump.storage.Tid
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
                    val entry = entity.toEntry(tags = dao.tagsFor(entity.tid))
                    val meta = loadEntryMeta(entry)
                    if (meta.isEmpty && !hasSidecar(entry)) {
                        dao.deleteByPath(entry.file.absolutePath)
                        dao.insert(
                            entry
                                .copy(metaEncrypted = false)
                                .toEntity(
                                    contentPreview = contentPreviewOf(entry),
                                    isEncrypted = cryptoEngine.isEncryptedFile(entry.file),
                                ).copy(lastModified = effectiveLastModified(entry)),
                        )
                        return@forEach
                    }
                    val refreshed = entry.copy(
                        tags = meta.tags,
                        events = meta.events,
                        metaEncrypted = false,
                    )
                    dao.deleteByPath(entry.file.absolutePath)
                    dao.insert(
                        refreshed
                            .toEntity(
                                contentPreview = contentPreviewOf(refreshed),
                                isEncrypted = cryptoEngine.isEncryptedFile(refreshed.file),
                            ).copy(lastModified = effectiveLastModified(refreshed)),
                    )
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
            toEntry(tags = dao.tagsFor(tid), events = dao.eventsFor(tid).map { it.toEntryEvent() })

        /**
         * Observe entries for a space (content + group rows, newest first), with each
         * owner's tags/events joined in, and its comments merged as COMMENT entries.
         */
        fun getEntries(space: Space): Flow<List<MindDumpEntry>> =
            dao.getAll(space).map { entities ->
                val withMeta = entities.map { it.withMeta() }
                mergeComments(withMeta, space)
            }

        /** One-shot snapshot of all entries in a space (for boot re-registration). */
        suspend fun getAllEntries(space: Space): List<MindDumpEntry> =
            withContext(Dispatchers.IO) {
                val withMeta = dao.getAllSnapshot(space).map { it.withMeta() }
                mergeComments(withMeta, space)
            }

        /**
         * Merge a space's comments (from the comments table) into the entry list,
         * reconstructing each as a [MindDumpEntry] with role == COMMENT so the UI's
         * comment presentation is unchanged. Only content/group entries carry tags.
         */
        private suspend fun mergeComments(entries: List<MindDumpEntry>, space: Space): List<MindDumpEntry> =
            withContext(Dispatchers.IO) {
                val comments = dao.getCommentsSnapshot(space).map { it.toEntry() }
                entries + comments
            }

        /** Observe entries in [space] carrying [tag] (joined via the tags table). */
        fun getEntriesByTag(space: Space, tag: String): Flow<List<MindDumpEntry>> =
            dao.getByTag(space, tag).map { entities ->
                entities.map { it.withMeta() }
            }

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
            return dao.search(space, pattern).map { entities ->
                entities.map { it.withMeta() }
            }
        }

        /**
         * Save a text entry: write file + encrypt if Private + insert Room row.
         * [targetGroupDir] non-null writes the entry directly into that group
         * directory and stamps its `parentId`; null writes to the month top.
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
                    tid = Tid.tidOfStem(finalFile.nameWithoutExtension),
                    parentId = groupTidOf(targetGroupDir),
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
                    tid = Tid.tidOfStem(finalFile.nameWithoutExtension),
                    parentId = groupTidOf(finalFile.parentFile),
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
                    tid = Tid.tidOfStem(finalFile.nameWithoutExtension),
                    parentId = groupTidOf(finalFile.parentFile),
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
                val commentTs = Tid.parseCommentStem(finalFile.nameWithoutExtension)
                    ?: error("Not a comment filename: ${finalFile.name}")
                val entry = MindDumpEntry(
                    file = finalFile,
                    type = meta.entryType,
                    space = space,
                    monthFolder = "",
                    tid = commentTs.ownTid,
                    parentId = groupTidOf(finalFile.parentFile),
                    targetTid = targetEntry.tid,
                )
                dao.insertComment(
                    CommentEntity(
                        tid = entry.tid,
                        targetTid = targetEntry.tid,
                        filePath = finalFile.absolutePath,
                        space = space,
                        contentPreview = content.take(500),
                        lastModified = finalFile.lastModified(),
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
         * Overwrite an existing text/comment entry's content in place.
         * Preserves the file's path and identity; bumps lastModified so the entry
         * floats to the top of the feed. Re-encrypts when the entry was encrypted.
         * Returns [EditSaveResult.Locked] (instead of throwing) when an encrypted
         * entry can't be saved because the session is no longer unlocked.
         */
        suspend fun saveEntryEdit(entry: MindDumpEntry, newText: String): EditSaveResult =
            withContext(Dispatchers.IO) {
                val isComment = entry.role == com.chin.minddump.storage.EntryRole.COMMENT
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

                if (isComment) {
                    updateCommentRow(entry, newText.take(500))
                } else {
                    dao.deleteByPath(entry.file.absolutePath)
                    val refreshed = entry // path/type/tid unchanged; lastModified flows in via toEntity
                    dao.insert(
                        refreshed.toEntity(
                            contentPreview = newText.take(500),
                            isEncrypted = wasEncrypted,
                        ),
                    )
                }
                EditSaveResult.Saved(entry)
            }

        /** Refresh a comment's contentPreview/lastModified in place (filePath unchanged). */
        private suspend fun updateCommentRow(entry: MindDumpEntry, preview: String) {
            val existing = dao
                .getCommentsSnapshot(entry.space)
                .firstOrNull { it.filePath == entry.file.absolutePath } ?: return
            dao.deleteCommentByTid(existing.tid)
            dao.insertComment(
                existing.copy(contentPreview = preview, lastModified = entry.file.lastModified()),
            )
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
         * events), so no empty `m.yaml` lingers. Refreshes the owner's Room row.
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
                dao.deleteByPath(entry.file.absolutePath)
                val refreshed = entry.copy(
                    tags = meta.tags,
                    events = meta.events,
                    metaEncrypted = false,
                )
                dao.insert(
                    refreshed
                        .toEntity(
                            contentPreview = contentPreviewOf(refreshed),
                            isEncrypted = cryptoEngine.isEncryptedFile(entry.file),
                        ).copy(lastModified = effectiveLastModified(refreshed)),
                )
                replaceTagRows(refreshed.tid, meta.tags)
                replaceEventRows(refreshed, meta.events)
                refreshed
            }

        /** Replace an owner's tag rows. */
        private suspend fun replaceTagRows(tid: Long, tags: List<String>) {
            dao.deleteTagsFor(tid)
            if (tags.isNotEmpty()) dao.insertTags(tags.map { TagEntity(tid, it) })
        }

        /** Replace an owner's event rows (sidecar is authority; rows re-register alarms). */
        private suspend fun replaceEventRows(entry: MindDumpEntry, events: List<EntryEvent>) {
            dao.deleteEventsFor(entry.tid)
            if (events.isNotEmpty()) {
                dao.insertEvents(events.map { it.toEventEntity(entry.tid) })
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
            val entry = entity.toEntry(tags = dao.tagsFor(entity.tid))
            val meta = loadEntryMeta(entry)
            val events = dao.eventsFor(entry.tid)
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
                val row = dao.eventsFor(entry.tid).firstOrNull { it.due == event.due.toString() }
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
            val eventId = dao.eventsFor(entry.tid).firstOrNull { it.due == event.due.toString() }?.id ?: return
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
            val events = dao.eventsFor(entry.tid).map { it.toEntryEvent() }
            events.forEach { ev -> if (ev.state != EventState.FIRED) scheduleEvent(entry, ev) }
        }

        /**
         * Register all pending Public events (e.g. at app start, after reconcile).
         * Private events are registered on unlock via [onPrivateUnlocked].
         */
        suspend fun registerAllPublicEvents() = withContext(Dispatchers.IO) {
            getAllEntries(Space.PUBLIC).forEach { entry ->
                if (entry.role != com.chin.minddump.storage.EntryRole.COMMENT) registerEvents(entry)
            }
        }

        /**
         * Create a new group and return its directory.
         */
        suspend fun createGroup(space: Space, name: String?, parentGroupDir: File? = null): File =
            withContext(Dispatchers.IO) {
                storageEngine.createGroup(space, name, parentGroupDir).also { groupDir ->
                    indexGroupDir(groupDir, space)
                }
            }

        /** Insert/refresh a group container row (type=GROUP) with tid + parentId from disk. */
        private suspend fun indexGroupDir(groupDir: File, space: Space) {
            val tid = Tid.tidOfStem(groupDir.name)
            val meta = FileMetadata.fromFile(groupDir)
            val parentTid = groupTidOf(groupDir.parentFile)
            dao.insert(
                EntryEntity(
                    tid = tid,
                    filePath = groupDir.absolutePath,
                    type = com.chin.minddump.storage.EntryType.GROUP,
                    space = space,
                    monthFolder = monthFolderOf(groupDir),
                    lastModified = groupDir.lastModified(),
                    parentId = parentTid,
                    isPinned = meta?.isPinned ?: false,
                    todoState = meta?.todoState ?: TodoState.NONE,
                ),
            )
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
                indexGroupDir(groupDir, space)
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
         * Rename a group directory: O(1) on the index — only the group row's
         * filePath changes; members keep their parentId. Falls back to reconcile.
         */
        suspend fun renameGroup(groupDir: File, space: Space, newName: String?): File =
            withContext(Dispatchers.IO) {
                val oldTid = Tid.tidOfStem(groupDir.name)
                val newDir = storageEngine.renameGroupDir(groupDir, newName)
                // The group's tid is unchanged (rename preserves the timestamp),
                // so only the filePath column needs updating.
                val existing = dao.findByTid(oldTid)
                if (existing != null) {
                    dao.deleteByTid(oldTid)
                    dao.insert(existing.copy(filePath = newDir.absolutePath, lastModified = newDir.lastModified()))
                }
                reconcileWithDisk(space)
                newDir
            }

        /**
         * Dissolve a group: move every member back to the group's parent location
         * and delete the now-empty group directory. Members' parentId is reparented
         * to the dissolved group's own parentId (nesting-aware).
         */
        suspend fun dissolveGroup(groupDir: File, space: Space) =
            withContext(Dispatchers.IO) {
                val groupTid = Tid.tidOfStem(groupDir.name)
                val groupRow = dao.findByTid(groupTid)
                val newParentId = groupRow?.parentId
                storageEngine.dissolveGroup(groupDir)
                // Reparent all direct members to the dissolved group's parent.
                dao.getMembersSnapshot(groupTid).forEach { member ->
                    dao.deleteByTid(member.tid)
                    dao.insert(member.copy(parentId = newParentId))
                }
                if (groupRow != null) dao.deleteByTid(groupTid)
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
         * Move an entry into a group directory.
         */
        suspend fun moveToGroup(entry: MindDumpEntry, groupDir: File) =
            withContext(Dispatchers.IO) {
                val newFile = storageEngine.moveToGroup(entry.file, groupDir)
                val groupTid = Tid.tidOfStem(groupDir.name)
                val meta = FileMetadata.fromFile(newFile)!!
                moveEntryRow(entry, newFile, meta.isEncrypted) { it.copy(parentId = groupTid) }
            }

        /**
         * Move an entry out of its group back to the month directory.
         */
        suspend fun moveOutOfGroup(entry: MindDumpEntry, space: Space) =
            withContext(Dispatchers.IO) {
                val newFile = storageEngine.moveOutOfGroup(entry.file, space)
                val meta = FileMetadata.fromFile(newFile)!!
                moveEntryRow(entry, newFile, meta.isEncrypted) { it.copy(parentId = null) }
            }

        /**
         * Re-index an entry row after its file moved: delete the old path row,
         * insert at the new path with the [transform] applied (e.g. parentId change).
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
                    it.copy(space = targetSpace, monthFolder = monthFolderOf(finalFile), parentId = null)
                }
            }

        /**
         * Rename the originalName portion of an entry's file.
         */
        suspend fun renameEntry(entry: MindDumpEntry, newName: String?): MindDumpEntry =
            withContext(Dispatchers.IO) {
                val newFile = storageEngine.renameEntry(entry.file, newName)
                val meta = FileMetadata.fromFile(newFile)!!
                moveEntryRow(entry, newFile, meta.isEncrypted) { it }
                entry.copy(file = newFile)
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
                moveEntryRow(entry, newFile, meta.isEncrypted) { it.copy(isPinned = pinned) }
                entry.copy(file = newFile, isPinned = pinned)
            }

        /**
         * Set the todo status of a file entry, encoded as a status token in the
         * filename. Pass [TodoState.NONE] to clear. Comments cannot carry status.
         */
        suspend fun setEntryStatus(entry: MindDumpEntry, state: TodoState): MindDumpEntry =
            withContext(Dispatchers.IO) {
                val newFile = storageEngine.setEntryStatus(entry.file, state)
                val meta = FileMetadata.fromFile(newFile)!!
                moveEntryRow(entry, newFile, meta.isEncrypted) { it.copy(todoState = state) }
                entry.copy(file = newFile, todoState = state)
            }

        /**
         * Toggle the pin state of a group directory: rename on disk + update only
         * the group row (members' parentId unchanged).
         */
        suspend fun setGroupPinned(groupDir: File, space: Space, pinned: Boolean): File =
            withContext(Dispatchers.IO) {
                val groupTid = Tid.tidOfStem(groupDir.name)
                val newDir = storageEngine.setGroupPinned(groupDir, pinned)
                val existing = dao.findByTid(groupTid)
                if (existing != null) {
                    dao.deleteByTid(groupTid)
                    dao.insert(
                        existing.copy(
                            filePath = newDir.absolutePath,
                            isPinned = pinned,
                            lastModified = newDir.lastModified(),
                        ),
                    )
                }
                reconcileWithDisk(space)
                newDir
            }

        /**
         * Set the todo status of a group directory: rename on disk + update only
         * the group row (members' parentId unchanged).
         */
        suspend fun setGroupStatus(groupDir: File, space: Space, state: TodoState): File =
            withContext(Dispatchers.IO) {
                val groupTid = Tid.tidOfStem(groupDir.name)
                val newDir = storageEngine.setGroupStatus(groupDir, state)
                val existing = dao.findByTid(groupTid)
                if (existing != null) {
                    dao.deleteByTid(groupTid)
                    dao.insert(
                        existing.copy(
                            filePath = newDir.absolutePath,
                            todoState = state,
                            lastModified = newDir.lastModified(),
                        ),
                    )
                }
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
                if (entry.role == com.chin.minddump.storage.EntryRole.COMMENT) {
                    dao.deleteCommentByPath(entry.file.absolutePath)
                } else {
                    dao.deleteByPath(entry.file.absolutePath)
                }
            }

        /**
         * Soft-delete a group directory: move the whole tree into `.trash/` and drop
         * every member + the group row. Members travel with the group and restore together.
         */
        suspend fun deleteGroup(groupDir: File, space: Space) =
            withContext(Dispatchers.IO) {
                Timber.d("Trashing group: %s", groupDir.name)
                val groupTid = Tid.tidOfStem(groupDir.name)
                // Drop the group row and all descendants (cascades via the parent chain).
                deleteGroupSubtree(groupTid)
                storageEngine.trashGroup(groupDir, space)
            }

        /** Delete a group row and every descendant entry row beneath it. */
        private suspend fun deleteGroupSubtree(rootTid: Long) {
            val stack = ArrayDeque<Long>()
            stack.addLast(rootTid)
            while (stack.isNotEmpty()) {
                val tid = stack.removeLast()
                dao.getMembersSnapshot(tid).forEach { child ->
                    if (child.type == com.chin.minddump.storage.EntryType.GROUP) stack.addLast(child.tid)
                    dao.deleteByTid(child.tid)
                }
                dao.deleteByTid(tid)
            }
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
                val groupTid = dao.tidOfGroup(groupDir.absolutePath) ?: return@withContext emptyList()
                dao.getMembersSnapshot(groupTid).map { it.toEntry(tags = dao.tagsFor(it.tid)) }
            }

        /**
         * Wipe the database and repopulate it entirely from disk.
         * Files are the source of truth — nothing on disk is touched.
         * Clears all four tables then re-scans both spaces, and rebuilds FTS.
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
         * Single-pass: tid/parentId are derivable from filenames, so no insert
         * ordering is needed. Comments resolve their targetTid against scanned owners.
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
                val dbComments = dao.getCommentsSnapshot(space).associateBy { it.filePath }

                // Owners (FILE/GROUP) by their second-resolution timestamp prefix, so a
                // comment's targetTs can resolve to the owner's tid (the owner's collision
                // offset is not encoded in the comment filename).
                val ownerTidByTs = diskEntries
                    .filter { it.role != com.chin.minddump.storage.EntryRole.COMMENT }
                    .groupBy { it.timestamp }

                fun resolveCommentTargetTid(comment: MindDumpEntry): Long? {
                    val targetTs = comment.commentTargetTs ?: return null
                    val candidates = ownerTidByTs[targetTs] ?: return null
                    return candidates.firstOrNull()?.tid
                }

                // 1. Insert/update content + group rows (disk has it).
                val toUpsert = diskEntries
                    .filter { it.role != com.chin.minddump.storage.EntryRole.COMMENT }
                toUpsert.forEach { entry ->
                    val preview = contentPreviewOf(entry)
                    val isEncrypted = cryptoEngine.isEncryptedFile(entry.file)
                    dao.deleteByPath(entry.file.absolutePath)
                    dao.insert(
                        entry
                            .toEntity(contentPreview = preview, isEncrypted = isEncrypted)
                            .copy(lastModified = effectiveLastModified(entry)),
                    )
                    replaceTagRows(entry.tid, entry.tags)
                    if (!entry.metaEncrypted) replaceEventRows(entry, entry.events)
                }

                // 2. Insert/update comment rows, resolving targetTid.
                diskEntries
                    .filter { it.role == com.chin.minddump.storage.EntryRole.COMMENT }
                    .forEach { comment ->
                        val isEncrypted = cryptoEngine.isEncryptedFile(comment.file)
                        val targetTid = resolveCommentTargetTid(comment)
                        dao.deleteCommentByPath(comment.file.absolutePath)
                        dao.insertComment(
                            CommentEntity(
                                tid = comment.tid,
                                targetTid = targetTid ?: 0L,
                                filePath = comment.file.absolutePath,
                                space = space,
                                contentPreview = commentPreviewOf(comment),
                                lastModified = comment.file.lastModified(),
                                isEncrypted = isEncrypted,
                            ),
                        )
                    }

                // 3. Delete orphans (DB has, disk doesn't).
                val orphanEntryPaths = dbEntities
                    .filter { it.filePath !in diskPathMap }
                    .map { it.filePath }
                if (orphanEntryPaths.isNotEmpty()) dao.deleteByPaths(orphanEntryPaths)
                val orphanCommentPaths = dbComments.values
                    .filter { it.filePath !in diskPathMap }
                    .map { it.filePath }
                orphanCommentPaths.forEach { dao.deleteCommentByPath(it) }

                Timber.d(
                    "Reconcile %s: ~%d entries, ~%d comments, -%d orphans",
                    space.name,
                    toUpsert.size,
                    diskEntries.count { it.role == com.chin.minddump.storage.EntryRole.COMMENT },
                    orphanEntryPaths.size,
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
         * The tid of the group containing [dir], or null when [dir] is not itself a
         * group directory (the entry sits at the month-top level). Used to resolve
         * parentId for a newly written entry.
         */
        private fun groupTidOf(dir: File?): Long? {
            val parent = dir ?: return null
            return if (FileMetadata.fromFile(parent)?.role == com.chin.minddump.storage.EntryRole.GROUP) {
                Tid.tidOfStem(parent.name)
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

        /** Preview for a comment file (TEXT body, possibly encrypted). */
        private fun commentPreviewOf(comment: MindDumpEntry): String =
            if (!cryptoEngine.isEncryptedFile(comment.file)) {
                try {
                    comment.file.readText().take(500)
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
