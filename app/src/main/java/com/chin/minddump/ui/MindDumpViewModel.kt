package com.chin.minddump.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chin.minddump.data.MindDumpRepository
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.storage.Space
import dagger.hilt.android.lifecycle.HiltViewModel
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
import java.io.File
import javax.inject.Inject

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
)

@HiltViewModel
class MindDumpViewModel
    @Inject
    constructor(
        private val repository: MindDumpRepository,
    ) : ViewModel() {

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
                }
                .stateIn(
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
                    _uiState.update { it.copy(entries = entries) }
                }
            }

            // Initial sync from disk
            viewModelScope.launch(Dispatchers.IO) {
                repository.refreshFromDisk(Space.PUBLIC)
                repository.refreshFromDisk(Space.PRIVATE)
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
                repository.refreshFromDisk(_uiState.value.currentSpace)
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

        fun getRecordingFile(): File {
            return repository.getRecordingFile(_uiState.value.currentSpace)
        }

        fun stopRecording() {
            _uiState.update { it.copy(isRecording = false) }
            refreshEntries()
        }

        fun getPhotoFile(): File {
            return repository.getPhotoFile(_uiState.value.currentSpace)
        }

        fun getVideoFile(): File {
            return repository.getVideoFile(_uiState.value.currentSpace)
        }

        fun onMediaCaptured() {
            refreshEntries()
        }

        fun importFile(
            uri: Uri,
            fileName: String,
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.importFile(_uiState.value.currentSpace, uri, fileName)
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
