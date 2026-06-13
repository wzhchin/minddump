package com.chin.minddump.ui

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chin.minddump.data.MindDumpRepository
import com.chin.minddump.storage.EntryRole
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.storage.ShareItem
import com.chin.minddump.storage.Space
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * A file entry with its associated comments nested.
 */
data class GroupedEntry(
    val entry: MindDumpEntry,
    val comments: List<MindDumpEntry>,
)

data class MindDumpUiState(
    val currentSpace: Space = Space.PUBLIC,
    val isDarkTheme: Boolean = false,
    val entries: List<MindDumpEntry> = emptyList(),
    val inputText: String = "",
    val isRecording: Boolean = false,
    val storagePermissionGranted: Boolean = false,
    val audioPermissionGranted: Boolean = false,
    val cameraPermissionGranted: Boolean = false,
    // Biometric auth for Private space
    val pendingSpaceSwitch: Boolean = false,
    // Private space password gate
    val showPasswordSetup: Boolean = false,
    val showPasswordInput: Boolean = false,
    // Search
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    // Work directory settings
    val workDir: String = "",
    val showSettings: Boolean = false,
    val showMigrationDialog: Boolean = false,
    val pendingNewDir: String? = null,
    val currentFileCount: Int = 0,
    val newDirFileCount: Int = 0,
    // Share intent
    val pendingShareItems: List<ShareItem>? = null,
    val shareError: String? = null,
    // Entry actions
    val selectedEntryForAction: MindDumpEntry? = null,
    // Multi-select
    val isMultiSelectMode: Boolean = false,
    val selectedEntries: Set<MindDumpEntry> = emptySet(),
    // Grouped entries (files + their comments)
    val groupedEntries: List<GroupedEntry> = emptyList(),
    // Group directories in the current month
    val groups: List<File> = emptyList(),
)

@HiltViewModel
@Suppress("TooManyFunctions")
class MindDumpViewModel
    @Inject
    constructor(
        private val repository: MindDumpRepository,
        @ApplicationContext context: Context,
    ) : ViewModel() {

        private val contentResolver: ContentResolver = context.contentResolver

        private val _uiState = MutableStateFlow(MindDumpUiState())
        val uiState: StateFlow<MindDumpUiState> = _uiState.asStateFlow()

        // Track current space and search query as flows
        private val currentSpaceFlow = MutableStateFlow(Space.PUBLIC)
        private val searchQueryFlow = MutableStateFlow("")

        // Observe entries: switches between all entries and search results
        @OptIn(ExperimentalCoroutinesApi::class)
        val entriesFlow: StateFlow<List<MindDumpEntry>> =
            combine(currentSpaceFlow, searchQueryFlow) { space, query -> space to query }
                .flatMapLatest { (space, query) ->
                    if (query.isBlank()) {
                        repository.getEntries(space)
                    } else {
                        repository.searchEntries(space, query)
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )

        init {
            checkStoragePermission()
            _uiState.update {
                it.copy(
                    workDir = repository.getRootDirPath(),
                )
            }

            // Sync entries from collection into uiState
            viewModelScope.launch {
                entriesFlow.collect { entries ->
                    val grouped = groupEntriesWithComments(entries)
                    _uiState.update { it.copy(entries = entries, groupedEntries = grouped) }
                }
            }

            // Initial sync from disk
            viewModelScope.launch(Dispatchers.IO) {
                repository.reconcileWithDisk(Space.PUBLIC)
                repository.reconcileWithDisk(Space.PRIVATE)
            }
        }

        fun checkStoragePermission() {
            val granted = repository.hasStoragePermission()
            _uiState.update { it.copy(storagePermissionGranted = granted) }
        }

        /**
         * Request a space switch.
         * Public → Private: Check if password is set, show setup or input dialog
         * Private → Public: Switch directly
         */
        fun requestSpaceSwitch() {
            val targetSpace = if (_uiState.value.currentSpace == Space.PUBLIC) Space.PRIVATE else Space.PUBLIC
            if (targetSpace == Space.PRIVATE) {
                // Already unlocked? Go straight in
                if (repository.isSessionUnlocked()) {
                    applySpaceSwitch(Space.PRIVATE)
                    return
                }
                // Need password setup or verification
                if (repository.hasPrivatePassword()) {
                    _uiState.update { it.copy(showPasswordInput = true) }
                } else {
                    _uiState.update { it.copy(showPasswordSetup = true) }
                }
            } else {
                applySpaceSwitch(targetSpace)
            }
        }

        /**
         * Set password for first-time Private access.
         */
        fun setPassword(password: String) {
            repository.setPassword(password)
            _uiState.update { it.copy(showPasswordSetup = false) }
            applySpaceSwitch(Space.PRIVATE)
        }

        /**
         * Verify password for Private access.
         */
        fun verifyPassword(password: String): Boolean {
            val valid = repository.verifyAndCachePassword(password)
            if (valid) {
                _uiState.update { it.copy(showPasswordInput = false) }
                applySpaceSwitch(Space.PRIVATE)
            }
            return valid
        }

        /**
         * Cancel password dialog.
         */
        fun cancelPasswordDialog() {
            _uiState.update {
                it.copy(
                    showPasswordSetup = false,
                    showPasswordInput = false,
                )
            }
        }

        /**
         * Called when biometric auth succeeds — complete the switch to Private.
         */
        fun confirmSpaceSwitch() {
            applySpaceSwitch(Space.PRIVATE)
        }

        /**
         * Called when biometric auth fails or is cancelled.
         */
        fun cancelSpaceSwitch() {
            _uiState.update { it.copy(pendingSpaceSwitch = false) }
        }

        private fun applySpaceSwitch(newSpace: Space) {
            currentSpaceFlow.value = newSpace
            _uiState.update {
                it.copy(
                    currentSpace = newSpace,
                    isDarkTheme = newSpace == Space.PRIVATE,
                    pendingSpaceSwitch = false,
                )
            }
        }

        /**
         * Manual refresh — re-scans disk and updates Room.
         */
        fun refreshEntries() {
            viewModelScope.launch(Dispatchers.IO) {
                val space = _uiState.value.currentSpace
                repository.reconcileWithDisk(space)
                val groups = repository.scanGroups(space)
                _uiState.update { it.copy(groups = groups) }
            }
        }

        fun updateInputText(text: String) {
            _uiState.update { it.copy(inputText = text) }
        }

        fun updateSearchQuery(query: String) {
            searchQueryFlow.value = query
            _uiState.update { it.copy(searchQuery = query, isSearching = query.isNotBlank()) }
        }

        fun clearSearch() {
            searchQueryFlow.value = ""
            _uiState.update { it.copy(searchQuery = "", isSearching = false) }
        }

        fun submitText() {
            val text = _uiState.value.inputText.trim()
            if (text.isEmpty()) return

            viewModelScope.launch(Dispatchers.IO) {
                repository.saveTextEntry(_uiState.value.currentSpace, text)
                _uiState.update { it.copy(inputText = "") }
            }
        }

        fun startRecording() {
            _uiState.update { it.copy(isRecording = true) }
        }

        fun getRecordingFile(): File = repository.getRecordingFile(_uiState.value.currentSpace)

        fun stopRecording() {
            _uiState.update { it.copy(isRecording = false) }
            refreshEntries()
        }

        fun getPhotoFile(): File = repository.getPhotoFile(_uiState.value.currentSpace)

        fun getVideoFile(): File = repository.getVideoFile(_uiState.value.currentSpace)

        fun onMediaCaptured() {
            refreshEntries()
        }

        fun importFile(
            uri: Uri,
            fileName: String,
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    repository.importFile(_uiState.value.currentSpace, uri, fileName)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to import file")
                    _uiState.update { it.copy(shareError = "1") }
                }
            }
        }

        fun deleteEntry(entry: MindDumpEntry) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.deleteEntry(entry)
            }
        }

        fun setAudioPermissionGranted(granted: Boolean) {
            _uiState.update { it.copy(audioPermissionGranted = granted) }
        }

        fun setCameraPermissionGranted(granted: Boolean) {
            _uiState.update { it.copy(cameraPermissionGranted = granted) }
        }

        // --- Share intent handling ---

        /**
         * Parse an incoming share intent and set pendingShareItems state.
         * The UI layer will show a space-selection dialog when items are pending.
         */
        fun handleShareIntent(intent: Intent) {
            val action = intent.action
            if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return

            val items = mutableListOf<ShareItem>()

            // Extract text
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) {
                items.add(ShareItem.Text(text))
            }

            // Extract file URI(s)
            if (action == Intent.ACTION_SEND) {
                val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                uri?.let {
                    val fileName = resolveFileName(it)
                    items.add(ShareItem.File(it, fileName))
                }
            } else {
                val uris = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                uris?.forEach { uri ->
                    val fileName = resolveFileName(uri)
                    items.add(ShareItem.File(uri, fileName))
                }
            }

            if (items.isEmpty()) {
                Timber.w("Share intent contained no extractable content")
                return
            }

            Timber.d("Received %d share item(s)", items.size)
            _uiState.update { it.copy(pendingShareItems = items, shareError = null) }
        }

        /**
         * Resolve the original file name from a content URI.
         * Falls back to lastPathSegment → "shared_file".
         */
        private fun resolveFileName(uri: Uri): String {
            return try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        return@use cursor.getString(nameIndex)
                    }
                    null
                } ?: uri.lastPathSegment
            } catch (e: Exception) {
                Timber.w(e, "Failed to resolve file name for URI: %s", uri)
                uri.lastPathSegment
            } ?: "shared_file"
        }

        /**
         * Save all pending share items to the selected space.
         * Clears pending state on completion.
         */
        fun confirmShare(space: Space) {
            val items = _uiState.value.pendingShareItems ?: return
            viewModelScope.launch(Dispatchers.IO) {
                var errorCount = 0
                for (item in items) {
                    try {
                        when (item) {
                            is ShareItem.Text -> repository.saveTextEntry(space, item.content)
                            is ShareItem.File -> repository.importFile(space, item.uri, item.fileName)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to save share item")
                        errorCount++
                    }
                }
                _uiState.update {
                    it.copy(
                        pendingShareItems = null,
                        shareError = if (errorCount > 0) "$errorCount" else null,
                    )
                }
            }
        }

        /**
         * Discard pending share items without saving.
         */
        fun cancelShare() {
            _uiState.update { it.copy(pendingShareItems = null, shareError = null) }
        }

        fun clearShareError() {
            _uiState.update { it.copy(shareError = null) }
        }

        // --- Entry actions (drawer) ---

        fun selectEntryForAction(entry: MindDumpEntry) {
            _uiState.update { it.copy(selectedEntryForAction = entry) }
        }

        fun clearEntryAction() {
            _uiState.update { it.copy(selectedEntryForAction = null) }
        }

        fun renameEntry(entry: MindDumpEntry, newName: String?) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.renameEntry(entry, newName)
                refreshEntries()
            }
        }

        fun moveToGroup(entry: MindDumpEntry, groupDir: File) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.moveToGroup(entry, groupDir)
                refreshEntries()
            }
        }

        fun createGroup(name: String?) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.createGroup(_uiState.value.currentSpace, name)
                refreshEntries()
            }
        }

        /**
         * Create a new group and move the entry into it atomically.
         */
        fun createAndMoveToGroup(entry: MindDumpEntry, name: String?) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.createAndMoveToGroup(entry, _uiState.value.currentSpace, name)
                refreshEntries()
            }
        }

        fun moveEntryToSpace(entry: MindDumpEntry, targetSpace: Space) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.moveEntryToSpace(entry, targetSpace)
                refreshEntries()
            }
        }

        // --- Multi-select ---

        fun enterMultiSelectMode(entry: MindDumpEntry) {
            _uiState.update {
                it.copy(
                    isMultiSelectMode = true,
                    selectedEntries = setOf(entry),
                    selectedEntryForAction = null,
                )
            }
        }

        fun toggleEntrySelection(entry: MindDumpEntry) {
            _uiState.update { state ->
                val current = state.selectedEntries
                val updated = if (entry in current) {
                    current - entry
                } else {
                    current + entry
                }
                state.copy(
                    selectedEntries = updated,
                    isMultiSelectMode = updated.isNotEmpty(),
                )
            }
        }

        fun exitMultiSelectMode() {
            _uiState.update {
                it.copy(
                    isMultiSelectMode = false,
                    selectedEntries = emptySet(),
                )
            }
        }

        fun deleteSelectedEntries() {
            val entries = _uiState.value.selectedEntries.toList()
            viewModelScope.launch(Dispatchers.IO) {
                entries.forEach { repository.deleteEntry(it) }
                _uiState.update {
                    it.copy(
                        isMultiSelectMode = false,
                        selectedEntries = emptySet(),
                    )
                }
            }
        }

        /**
         * Whether the Private space session is currently unlocked.
         */
        fun isSessionUnlocked(): Boolean = repository.isSessionUnlocked()

        /**
         * Group entries with their comments.
         * Comments (role=N) are nested under their target entry (matched by targetTimestamp).
         * Orphan comments (no matching target) appear as standalone entries.
         */
        private fun groupEntriesWithComments(entries: List<MindDumpEntry>): List<GroupedEntry> {
            val files = entries.filter { it.role == EntryRole.FILE }
            val comments = entries.filter { it.role == EntryRole.COMMENT }

            // Build a map of timestamp → file entry for quick lookup
            val result = mutableListOf<GroupedEntry>()
            val matchedComments = mutableSetOf<MindDumpEntry>()

            // For each file, find its comments
            for (file in files) {
                val fileComments = comments.filter {
                    it.targetTimestamp == file.timestamp && it !in matchedComments
                }
                matchedComments.addAll(fileComments)
                result.add(GroupedEntry(entry = file, comments = fileComments))
            }

            // Orphan comments (no matching target file)
            for (comment in comments) {
                if (comment !in matchedComments) {
                    result.add(GroupedEntry(entry = comment, comments = emptyList()))
                }
            }

            return result
        }

        // --- Work directory ---

        fun isWorkDirConfigured(): Boolean = repository.isWorkDirConfigured()

        fun setShowSettings(show: Boolean) {
            _uiState.update { it.copy(showSettings = show) }
        }

        /**
         * Request migration to a new directory.
         * Checks both current and new directory for existing files.
         * Shows migration dialog if either has files; otherwise switches directly.
         */
        fun requestMigration(newPath: String) {
            val currentCount = repository.countFiles()
            val newDir = File(newPath)
            val newCount = repository.countFilesIn(newDir)

            if (currentCount == 0 && newCount == 0) {
                applyNewDir(newPath)
                return
            }

            _uiState.update {
                it.copy(
                    showMigrationDialog = true,
                    pendingNewDir = newPath,
                    currentFileCount = currentCount,
                    newDirFileCount = newCount,
                )
            }
        }

        fun confirmMigration() {
            val newPath = _uiState.value.pendingNewDir ?: return
            viewModelScope.launch(Dispatchers.IO) {
                val newRoot = File(newPath)
                repository.migrateTo(newRoot)
                applyNewDir(newPath)
            }
        }

        fun skipMigration() {
            val newPath = _uiState.value.pendingNewDir ?: return
            applyNewDir(newPath)
        }

        fun cancelMigration() {
            _uiState.update {
                it.copy(
                    showMigrationDialog = false,
                    pendingNewDir = null,
                    currentFileCount = 0,
                    newDirFileCount = 0,
                )
            }
        }

        private fun applyNewDir(newPath: String) {
            repository.setWorkDir(newPath)
            _uiState.update {
                it.copy(
                    workDir = newPath,
                    showMigrationDialog = false,
                    showSettings = false,
                    pendingNewDir = null,
                    currentFileCount = 0,
                    newDirFileCount = 0,
                )
            }
            refreshEntries()
        }
    }
