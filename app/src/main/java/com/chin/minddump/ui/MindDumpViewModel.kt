package com.chin.minddump.ui

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chin.minddump.data.MindDumpRepository
import com.chin.minddump.data.ThemePreferencesRepository
import com.chin.minddump.storage.EntryRole
import com.chin.minddump.storage.FileMetadata
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.storage.ShareItem
import com.chin.minddump.storage.Space
import com.chin.minddump.storage.TodoState
import com.chin.minddump.storage.TrashedItem
import com.chin.minddump.ui.theme.AppPaletteStyle
import com.chin.minddump.ui.theme.AppThemeMode
import com.chin.minddump.ui.theme.ThemePreferences
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
import androidx.compose.ui.graphics.Color

/**
 * A file entry with its associated comments nested.
 */
data class GroupedEntry(
    val entry: MindDumpEntry,
    val comments: List<MindDumpEntry>,
)

/**
 * Custom intent actions backing the static launcher shortcuts. Shared between the
 * manifest intent-filters, [shortcut's res/xml/shortcuts.xml], and the
 * [MindDumpViewModel.dispatchShortcutAction] dispatcher.
 */
object ShortcutActions {
    const val NEW_TEXT = "com.chin.minddump.action.NEW_TEXT"
    const val PHOTO = "com.chin.minddump.action.PHOTO"
    const val RECORD = "com.chin.minddump.action.RECORD"
    const val OPEN_PUBLIC = "com.chin.minddump.action.OPEN_PUBLIC"
    const val OPEN_PRIVATE = "com.chin.minddump.action.OPEN_PRIVATE"
}

/**
 * A group directory summary for rendering in the main list.
 * [memberEntries] powers the count, type chips, and the newest-member timestamp
 * used to position the group card in the time-ordered feed.
 */
data class GroupSummary(
    val groupDir: File,
    val name: String,
    val memberEntries: List<MindDumpEntry>,
) {
    /** Newest member's lastModified, used as the sort key for the group card. */
    val latestModified: Long get() = memberEntries.maxOfOrNull { it.file.lastModified() } ?: 0L
}

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
    val showRebuildDatabaseDialog: Boolean = false,
    val isRebuildingDatabase: Boolean = false,
    val showMigrationDialog: Boolean = false,
    val pendingNewDir: String? = null,
    val currentFileCount: Int = 0,
    val newDirFileCount: Int = 0,
    // Share intent
    val pendingShareItems: List<ShareItem>? = null,
    val shareError: String? = null,
    // Outbound share: a one-shot payload to hand to the system Share sheet, or a
    // Locked signal to surface as a message. Null = nothing pending.
    val shareResult: MindDumpRepository.ShareResult? = null,
    // Entry actions
    val selectedEntryForAction: MindDumpEntry? = null,
    // Multi-select
    val isMultiSelectMode: Boolean = false,
    val selectedEntries: Set<MindDumpEntry> = emptySet(),
    // Grouped entries (files + their comments)
    val groupedEntries: List<GroupedEntry> = emptyList(),
    // Group directories in the current month, with members for rendering
    val groups: List<GroupSummary> = emptyList(),
    // The currently open group page's directory (null on the main feed). Source
    // of truth for in-group write/create scope and child-group rendering.
    val currentGroupDir: File? = null,
    // Sub-group cards for the open group page (month-top groups use [groups])
    val childGroups: List<GroupSummary> = emptyList(),
    // Text/comment entry open in the fullscreen editor's edit mode (null in new-entry mode)
    val entryToEdit: File? = null,
    // Multi-select: show the merge-to-group picker
    val showGroupMergePicker: Boolean = false,
    // Rename group dialog state
    val groupToRename: File? = null,
    // Long-press group action menu state
    val groupMenuFor: File? = null,
    // Recycle bin (soft-delete) state
    val showTrash: Boolean = false,
    val trashedItems: List<TrashedItem> = emptyList(),
    // One-shot action staged by a launcher shortcut (long-press app icon), consumed
    // by the UI to fire the matching capture/navigation entry point.
    val pendingShortcutAction: ShortcutAction? = null,
)

/**
 * Launcher-shortcut actions. PHOTO/RECORD are staged for the UI to consume (they
 * need permission launchers that live in composables); the rest are handled
 * directly when dispatched.
 */
enum class ShortcutAction {
    NEW_TEXT,
    PHOTO,
    RECORD,
    OPEN_PUBLIC,
    OPEN_PRIVATE,
}

@HiltViewModel
@Suppress("TooManyFunctions", "LargeClass")
class MindDumpViewModel
    @Inject
    constructor(
        private val repository: MindDumpRepository,
        private val themePreferencesRepository: ThemePreferencesRepository,
        @ApplicationContext context: Context,
    ) : ViewModel() {

        private val contentResolver: ContentResolver = context.contentResolver

        private val _uiState = MutableStateFlow(MindDumpUiState())
        val uiState: StateFlow<MindDumpUiState> = _uiState.asStateFlow()

        /** Theme preferences, reactively collected for the theme composable + settings UI. */
        val themePreferences: StateFlow<ThemePreferences> =
            themePreferencesRepository.preferences.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ThemePreferences(),
            )

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
                    refreshGroups()
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

        // --- Launcher shortcuts (long-press app icon) ---

        /**
         * Dispatch a launcher-shortcut action by its intent action string. Space
         * switches are applied immediately; capture/edit actions are staged as
         * [ShortcutAction] for the UI to consume once its permission launchers and
         * nav controller exist.
         */
        fun dispatchShortcutAction(action: String?) {
            val shortcut = when (action) {
                ShortcutActions.NEW_TEXT -> ShortcutAction.NEW_TEXT
                ShortcutActions.PHOTO -> ShortcutAction.PHOTO
                ShortcutActions.RECORD -> ShortcutAction.RECORD
                ShortcutActions.OPEN_PUBLIC -> {
                    requestSwitchToSpace(Space.PUBLIC)
                    return
                }
                ShortcutActions.OPEN_PRIVATE -> {
                    requestSwitchToSpace(Space.PRIVATE)
                    return
                }
                else -> return
            }
            _uiState.update { it.copy(pendingShortcutAction = shortcut) }
        }

        /**
         * Clear a consumed staged shortcut action.
         */
        fun consumeShortcutAction() {
            _uiState.update { it.copy(pendingShortcutAction = null) }
        }

        /**
         * Switch straight to [space] from a shortcut, mirroring the in-app toggle
         * but without flipping back if already there.
         */
        private fun requestSwitchToSpace(space: Space) {
            if (space == Space.PRIVATE && !repository.isSessionUnlocked()) {
                if (repository.hasPrivatePassword()) {
                    _uiState.update { it.copy(showPasswordInput = true, pendingSpaceSwitch = true) }
                } else {
                    _uiState.update { it.copy(showPasswordSetup = true, pendingSpaceSwitch = true) }
                }
            } else {
                applySpaceSwitch(space)
            }
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
            }
            refreshGroups()
        }

        /**
         * Rebuild the group summaries for the picker and main-list cards.
         * Groups are derived from the current entry set (which carries groupPath)
         * so the cards always reflect what the list already knows about.
         */
        fun refreshGroups() {
            viewModelScope.launch(Dispatchers.IO) {
                val summaries = buildSummaries(repository.scanGroups(_uiState.value.currentSpace))
                _uiState.update { it.copy(groups = summaries) }
            }
        }

        /**
         * Refresh the sub-group cards for the open group page (or clear them on
         * the main feed).
         */
        fun refreshChildGroups() {
            viewModelScope.launch(Dispatchers.IO) {
                val parent = _uiState.value.currentGroupDir
                val summaries = if (parent != null) buildSummaries(repository.scanChildGroups(parent)) else emptyList()
                _uiState.update { it.copy(childGroups = summaries) }
            }
        }

        /**
         * Build group summaries for a set of group directories, resolving members
         * from the current entry set's [MindDumpEntry.groupPath].
         */
        private fun buildSummaries(dirGroups: List<File>): List<GroupSummary> {
            val membersByPath = _uiState.value.entries.groupBy { it.groupPath }
            return dirGroups.map { dir ->
                GroupSummary(
                    groupDir = dir,
                    name = FileMetadata.fromFile(dir)?.originalName
                        ?: dir.name.substringAfter("-g", dir.name),
                    memberEntries = membersByPath[dir.absolutePath] ?: emptyList(),
                )
            }
        }

        /**
         * Refresh entries + whichever group list applies to the current scope
         * (month-top [groups] on the feed, [childGroups] inside a group page).
         * Public so screens can force a scope-aware refresh (e.g. before opening
         * the move-to-group picker).
         */
        fun refreshForCurrentScope() {
            refreshEntries()
            if (_uiState.value.currentGroupDir != null) refreshChildGroups() else refreshGroups()
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
                repository.saveTextEntry(_uiState.value.currentSpace, text, _uiState.value.currentGroupDir)
                _uiState.update { it.copy(inputText = "") }
                refreshForCurrentScope()
            }
        }

        fun startRecording() {
            _uiState.update { it.copy(isRecording = true) }
        }

        fun getRecordingFile(): File =
            repository.getRecordingFile(_uiState.value.currentSpace, _uiState.value.currentGroupDir)

        fun stopRecording() {
            _uiState.update { it.copy(isRecording = false) }
            refreshForCurrentScope()
        }

        fun getPhotoFile(): File =
            repository.getPhotoFile(_uiState.value.currentSpace, _uiState.value.currentGroupDir)

        fun getVideoFile(): File =
            repository.getVideoFile(_uiState.value.currentSpace, _uiState.value.currentGroupDir)

        fun onMediaCaptured() {
            refreshForCurrentScope()
        }

        fun importFile(
            uri: Uri,
            fileName: String,
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    repository.importFile(_uiState.value.currentSpace, uri, fileName)
                    refreshForCurrentScope()
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

        /**
         * Resolve [entries] to a shareable payload and expose it for the UI to
         * hand to the system Share sheet. Surfaces [ShareResult.Locked] when an
         * encrypted Private entry can't be decrypted, instead of sharing.
         */
        fun shareEntries(entries: List<MindDumpEntry>) {
            if (entries.isEmpty()) return
            viewModelScope.launch(Dispatchers.IO) {
                val result = repository.prepareEntriesForShare(entries)
                _uiState.update { it.copy(shareResult = result) }
            }
        }

        /**
         * Resolve every member of a group and share them together. An empty group
         * emits nothing (no empty Share sheet).
         */
        fun shareGroup(groupDir: File) {
            viewModelScope.launch(Dispatchers.IO) {
                val members = repository.getGroupMemberEntries(groupDir)
                if (members.isEmpty()) return@launch
                val result = repository.prepareEntriesForShare(members)
                _uiState.update { it.copy(shareResult = result) }
            }
        }

        /** Clear the one-shot [shareResult] after the UI has acted on it. */
        fun consumeShareResult() {
            _uiState.update { it.copy(shareResult = null) }
        }

        /**
         * Clear transient plaintext files decrypted to `.cache/` for viewing and
         * sharing. Called from the activity's [onStop] so decrypted copies don't
         * outlive the user's session with the app.
         */
        fun clearDecryptedCache() {
            repository.cleanDecryptedCache()
        }

        fun renameEntry(entry: MindDumpEntry, newName: String?) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.renameEntry(entry, newName)
                refreshEntries()
            }
        }

        /** Toggle the pin (置顶) state of an entry. Comments are ignored. */
        fun toggleEntryPinned(entry: MindDumpEntry) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.setEntryPinned(entry, !entry.isPinned)
                refreshForCurrentScope()
            }
        }

        /** Set the todo status of an entry. [TodoState.NONE] clears it. */
        fun setEntryStatus(entry: MindDumpEntry, state: TodoState) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.setEntryStatus(entry, state)
                refreshForCurrentScope()
            }
        }

        /**
         * Create a comment targeting [targetEntry]. Blank content is ignored.
         * The comment lands in the target's directory as `{targetTs}-n-{nowTs}.md`
         * and refreshes the current scope so it appears in the parent card's folded list.
         */
        fun addComment(targetEntry: MindDumpEntry, content: String) {
            if (content.isBlank()) return
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveComment(_uiState.value.currentSpace, targetEntry, content)
                refreshForCurrentScope()
            }
        }

        fun moveToGroup(entry: MindDumpEntry, groupDir: File) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.moveToGroup(entry, groupDir)
                refreshForCurrentScope()
            }
        }

        fun createGroup(name: String?) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.createGroup(_uiState.value.currentSpace, name, _uiState.value.currentGroupDir)
                refreshForCurrentScope()
            }
        }

        /**
         * Create a new group and move the entry into it atomically. When a group
         * page is open, the new group is created under it (a sub-group).
         */
        fun createAndMoveToGroup(entry: MindDumpEntry, name: String?) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.createAndMoveToGroup(
                    entry,
                    _uiState.value.currentSpace,
                    name,
                    _uiState.value.currentGroupDir,
                )
                refreshForCurrentScope()
            }
        }

        /**
         * Move an entry out of its group to the parent location (parent group if
         * nested, else the month directory).
         */
        fun moveEntryOutOfGroup(entry: MindDumpEntry) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.moveEntryOutOfGroup(entry, _uiState.value.currentSpace)
                refreshForCurrentScope()
            }
        }

        // --- Group-level actions ---

        /**
         * Set the in-scope directory: null = root feed (current month dir), a
         * non-null File = an open group. Drives in-scope write/create targets and
         * child-group rendering.
         *
         * Called exactly once per route entry from `MainScreen`'s
         * `LaunchedEffect(currentDir)` — never cleared by a composition dispose
         * (returning to root navigates back to the root route, whose currentDir is
         * null and re-sets the scope here). This keeps the scope live while a
         * capture route sits on the back stack above the group page, so in-group
         * photo/video/audio land in the open group.
         */
        fun setCurrentGroupDir(groupDir: File?) {
            _uiState.update { it.copy(currentGroupDir = groupDir, childGroups = emptyList()) }
            if (groupDir != null) refreshChildGroups() else refreshGroups()
        }

        /** Show the long-press action menu for a group card. */
        fun selectGroupForMenu(groupDir: File) {
            _uiState.update { it.copy(groupMenuFor = groupDir) }
        }

        fun dismissGroupMenu() {
            _uiState.update { it.copy(groupMenuFor = null) }
        }

        /** Open a text/comment entry in the fullscreen editor's edit mode. */
        fun openEntryForEdit(file: File) {
            _uiState.update { it.copy(entryToEdit = file) }
        }

        /** Leave the fullscreen editor edit mode. */
        fun clearEntryToEdit() {
            _uiState.update { it.copy(entryToEdit = null) }
        }

        /**
         * Save an edited entry's new text back to its file. Returns true on success,
         * false if the encrypted session is locked (caller toasts and keeps editing).
         */
        suspend fun saveEntryEdit(
            entry: MindDumpEntry,
            newText: String
        ): Boolean = when (val result = repository.saveEntryEdit(entry, newText)) {
            is MindDumpRepository.EditSaveResult.Saved -> {
                refreshEntries()
                true
            }
            MindDumpRepository.EditSaveResult.Locked -> false
        }

        /** Load the plaintext content of an entry for editor pre-fill. */
        suspend fun loadEntryText(entry: MindDumpEntry): String =
            repository.loadEntryText(entry)

        /** Show the rename-group dialog. */
        fun requestRenameGroup(groupDir: File) {
            _uiState.update { it.copy(groupToRename = groupDir) }
        }

        fun cancelRenameGroup() {
            _uiState.update { it.copy(groupToRename = null) }
        }

        fun renameGroup(groupDir: File, newName: String?) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.renameGroup(groupDir, _uiState.value.currentSpace, newName)
                _uiState.update { it.copy(groupToRename = null) }
                refreshForCurrentScope()
            }
        }

        /**
         * Dissolve a group: members move to the parent location (parent group if
         * nested, else the month directory), then the empty group directory is
         * removed. Files are preserved.
         */
        fun dissolveGroup(groupDir: File) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.dissolveGroup(groupDir, _uiState.value.currentSpace)
                refreshForCurrentScope()
            }
        }

        /** Toggle the pin (置顶) state of a group directory. */
        fun toggleGroupPinned(groupDir: File) {
            viewModelScope.launch(Dispatchers.IO) {
                val meta = FileMetadata.fromFile(groupDir)
                repository.setGroupPinned(groupDir, _uiState.value.currentSpace, meta?.isPinned != true)
                _uiState.update { it.copy(groupMenuFor = null) }
                refreshForCurrentScope()
            }
        }

        /** Set the todo status of a group directory. */
        fun setGroupStatus(groupDir: File, state: TodoState) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.setGroupStatus(groupDir, _uiState.value.currentSpace, state)
                _uiState.update { it.copy(groupMenuFor = null) }
                refreshForCurrentScope()
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
                    showGroupMergePicker = false,
                )
            }
        }

        /** Show the group picker to merge the selected entries into a group. */
        fun showGroupMergePicker() {
            _uiState.update { it.copy(showGroupMergePicker = true) }
        }

        fun dismissGroupMergePicker() {
            _uiState.update { it.copy(showGroupMergePicker = false) }
        }

        /** Merge all selected entries into an existing group. */
        fun mergeSelectedIntoGroup(groupDir: File) {
            val entries = _uiState.value.selectedEntries.toList()
            viewModelScope.launch(Dispatchers.IO) {
                repository.moveEntriesToGroup(entries, groupDir)
                _uiState.update {
                    it.copy(
                        isMultiSelectMode = false,
                        selectedEntries = emptySet(),
                        showGroupMergePicker = false,
                    )
                }
                refreshForCurrentScope()
            }
        }

        /** Create a new group and merge all selected entries into it. */
        fun mergeSelectedIntoNewGroup(name: String?) {
            val entries = _uiState.value.selectedEntries.toList()
            viewModelScope.launch(Dispatchers.IO) {
                repository.createAndMoveGroupForEntries(
                    entries,
                    _uiState.value.currentSpace,
                    name,
                    _uiState.value.currentGroupDir,
                )
                _uiState.update {
                    it.copy(
                        isMultiSelectMode = false,
                        selectedEntries = emptySet(),
                        showGroupMergePicker = false,
                    )
                }
                refreshForCurrentScope()
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

        // --- Recycle bin (soft delete) ---

        /** Open the trash list, refreshing its contents. */
        fun openTrash() {
            refreshTrash()
            _uiState.update { it.copy(showTrash = true) }
        }

        /** Close the trash list and refresh the live feed (a restore may have changed it). */
        fun closeTrash() {
            _uiState.update { it.copy(showTrash = false) }
            refreshForCurrentScope()
        }

        /** Re-scan the trash for both spaces. */
        fun refreshTrash() {
            viewModelScope.launch(Dispatchers.IO) {
                val items = (repository.listTrashed(Space.PUBLIC) + repository.listTrashed(Space.PRIVATE))
                    .sortedByDescending { it.trashedAt }
                _uiState.update { it.copy(trashedItems = items) }
            }
        }

        /** Restore a trashed item back to its live space, then refresh both lists. */
        fun restoreTrashed(item: TrashedItem) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.restoreTrashed(item.file, item.space)
                refreshTrash()
            }
        }

        /** Permanently delete a single trashed item. */
        fun deleteTrashedForever(item: TrashedItem) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.deleteTrashedForever(item.file)
                refreshTrash()
            }
        }

        /** Permanently delete every trashed item. */
        fun emptyTrash() {
            viewModelScope.launch(Dispatchers.IO) {
                repository.emptyTrash()
                refreshTrash()
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

        // ── Theme preferences ──

        fun setSeedColor(color: Color?) {
            viewModelScope.launch { themePreferencesRepository.setSeedColor(color) }
        }

        fun setPaletteStyle(style: AppPaletteStyle) {
            viewModelScope.launch { themePreferencesRepository.setPaletteStyle(style) }
        }

        fun setAmoled(enabled: Boolean) {
            viewModelScope.launch { themePreferencesRepository.setAmoled(enabled) }
        }

        fun setThemeMode(mode: AppThemeMode) {
            viewModelScope.launch { themePreferencesRepository.setMode(mode) }
        }

        fun showRebuildDatabaseDialog() {
            _uiState.update { it.copy(showRebuildDatabaseDialog = true) }
        }

        fun dismissRebuildDatabaseDialog() {
            _uiState.update { it.copy(showRebuildDatabaseDialog = false) }
        }

        /**
         * Wipe the SQLite/FTS tables and rebuild them from disk. Files are
         * untouched. Returns the total number of entries now in the database.
         */
        suspend fun rebuildDatabase(): Int {
            _uiState.update { it.copy(isRebuildingDatabase = true) }
            return try {
                repository.rebuildDatabase()
                refreshForCurrentScope()
                repository.countAllTotal()
            } finally {
                _uiState.update {
                    it.copy(
                        isRebuildingDatabase = false,
                        showRebuildDatabaseDialog = false,
                    )
                }
            }
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
