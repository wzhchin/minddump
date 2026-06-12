package com.chin.minddump.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chin.minddump.storage.FileStorageEngine
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.storage.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

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
    // Work directory settings
    val workDir: String = "",
    val showSettings: Boolean = false,
    val showMigrationDialog: Boolean = false,
    val pendingNewDir: String? = null,
    val currentFileCount: Int = 0,
    val newDirFileCount: Int = 0,
)

class MindDumpViewModel(application: Application) : AndroidViewModel(application) {

    private val storageEngine = FileStorageEngine(application)

    private val _uiState = MutableStateFlow(MindDumpUiState())
    val uiState: StateFlow<MindDumpUiState> = _uiState.asStateFlow()

    init {
        checkStoragePermission()
        _uiState.update { it.copy(
            workDir = storageEngine.getRootDirPath()
        ) }
    }

    fun checkStoragePermission() {
        val granted = storageEngine.hasStoragePermission()
        _uiState.update { it.copy(storagePermissionGranted = granted) }
        if (granted) {
            refreshEntries()
        }
    }

    /**
     * Request a space switch. If switching to Private, sets pendingSpaceSwitch
     * to trigger biometric auth. If switching to Public, completes immediately.
     */
    fun requestSpaceSwitch() {
        val targetSpace = if (_uiState.value.currentSpace == Space.PUBLIC) Space.PRIVATE else Space.PUBLIC
        if (targetSpace == Space.PRIVATE) {
            _uiState.update { it.copy(pendingSpaceSwitch = true) }
        } else {
            applySpaceSwitch(targetSpace)
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
        _uiState.update { it.copy(
            currentSpace = newSpace,
            isDarkTheme = newSpace == Space.PRIVATE,
            pendingSpaceSwitch = false,
        ) }
        refreshEntries()
    }

    fun refreshEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            val entries = storageEngine.scanEntries(_uiState.value.currentSpace)
            _uiState.update { it.copy(entries = entries) }
        }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun submitText() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            storageEngine.saveTextEntry(_uiState.value.currentSpace, text)
            _uiState.update { it.copy(inputText = "") }
            refreshEntries()
        }
    }

    fun startRecording() {
        _uiState.update { it.copy(isRecording = true) }
    }

    fun getRecordingFile(): File {
        return storageEngine.getRecordingFile(_uiState.value.currentSpace)
    }

    fun stopRecording() {
        _uiState.update { it.copy(isRecording = false) }
        refreshEntries()
    }

    fun getPhotoFile(): File {
        return storageEngine.getPhotoFile(_uiState.value.currentSpace)
    }

    fun getVideoFile(): File {
        return storageEngine.getVideoFile(_uiState.value.currentSpace)
    }

    fun onMediaCaptured() {
        refreshEntries()
    }

    fun importFile(uri: Uri, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            storageEngine.importFile(_uiState.value.currentSpace, uri, fileName)
            refreshEntries()
        }
    }

    fun deleteEntry(entry: MindDumpEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            storageEngine.deleteEntry(entry)
            refreshEntries()
        }
    }

    fun setAudioPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(audioPermissionGranted = granted) }
    }

    fun setCameraPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(cameraPermissionGranted = granted) }
    }

    // --- Work directory ---

    fun isWorkDirConfigured(): Boolean = storageEngine.isWorkDirConfigured()

    fun setShowSettings(show: Boolean) {
        _uiState.update { it.copy(showSettings = show) }
    }

    /**
     * Request migration to a new directory.
     * Checks both current and new directory for existing files.
     * Shows migration dialog if either has files; otherwise switches directly.
     */
    fun requestMigration(newPath: String) {
        val currentCount = storageEngine.countFiles()
        val newDir = File(newPath)
        val newCount = storageEngine.countFilesIn(newDir)

        if (currentCount == 0 && newCount == 0) {
            // Both empty — switch directly
            applyNewDir(newPath)
            return
        }

        _uiState.update { it.copy(
            showMigrationDialog = true,
            pendingNewDir = newPath,
            currentFileCount = currentCount,
            newDirFileCount = newCount,
        ) }
    }

    fun confirmMigration() {
        val newPath = _uiState.value.pendingNewDir ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val newRoot = File(newPath)
            storageEngine.migrateTo(newRoot)
            applyNewDir(newPath)
        }
    }

    fun skipMigration() {
        val newPath = _uiState.value.pendingNewDir ?: return
        applyNewDir(newPath)
    }

    fun cancelMigration() {
        _uiState.update { it.copy(
            showMigrationDialog = false,
            pendingNewDir = null,
            currentFileCount = 0,
            newDirFileCount = 0,
        ) }
    }

    private fun applyNewDir(newPath: String) {
        storageEngine.setWorkDir(newPath)
        _uiState.update { it.copy(
            workDir = newPath,
            showMigrationDialog = false,
            showSettings = false,
            pendingNewDir = null,
            currentFileCount = 0,
            newDirFileCount = 0,
        ) }
        refreshEntries()
    }
}
