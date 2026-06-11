package com.chin.minddump.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chin.minddump.storage.EntryType
import com.chin.minddump.storage.FileStorageEngine
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.storage.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MindDumpUiState(
    val currentSpace: Space = Space.PUBLIC,
    val isDarkTheme: Boolean = false,
    val entries: List<MindDumpEntry> = emptyList(),
    val inputText: String = "",
    val isRecording: Boolean = false,
    val isCameraOpen: Boolean = false,
    val storagePermissionGranted: Boolean = false,
    val audioPermissionGranted: Boolean = false,
    val cameraPermissionGranted: Boolean = false,
)

class MindDumpViewModel(application: Application) : AndroidViewModel(application) {

    private val storageEngine = FileStorageEngine(application)

    private val _uiState = MutableStateFlow(MindDumpUiState())
    val uiState: StateFlow<MindDumpUiState> = _uiState.asStateFlow()

    init {
        checkStoragePermission()
    }

    fun checkStoragePermission() {
        val granted = storageEngine.hasStoragePermission()
        _uiState.value = _uiState.value.copy(storagePermissionGranted = granted)
        if (granted) {
            refreshEntries()
        }
    }

    fun toggleSpace() {
        val newSpace = if (_uiState.value.currentSpace == Space.PUBLIC) Space.PRIVATE else Space.PUBLIC
        _uiState.value = _uiState.value.copy(
            currentSpace = newSpace,
            isDarkTheme = newSpace == Space.PRIVATE
        )
        refreshEntries()
    }

    fun refreshEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            val entries = storageEngine.scanEntries(_uiState.value.currentSpace)
            _uiState.value = _uiState.value.copy(entries = entries)
        }
    }

    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun submitText() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            storageEngine.saveTextEntry(_uiState.value.currentSpace, text)
            _uiState.value = _uiState.value.copy(inputText = "")
            refreshEntries()
        }
    }

    fun startRecording() {
        _uiState.value = _uiState.value.copy(isRecording = true)
    }

    fun getRecordingFile(): java.io.File {
        return storageEngine.getRecordingFile(_uiState.value.currentSpace)
    }

    fun stopRecording() {
        _uiState.value = _uiState.value.copy(isRecording = false)
        refreshEntries()
    }

    fun getPhotoFile(): java.io.File {
        return storageEngine.getPhotoFile(_uiState.value.currentSpace)
    }

    fun getVideoFile(): java.io.File {
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
        _uiState.value = _uiState.value.copy(audioPermissionGranted = granted)
    }

    fun setCameraPermissionGranted(granted: Boolean) {
        _uiState.value = _uiState.value.copy(cameraPermissionGranted = granted)
    }

    fun setCameraOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isCameraOpen = open)
    }
}
