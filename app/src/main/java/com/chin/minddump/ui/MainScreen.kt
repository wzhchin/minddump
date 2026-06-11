package com.chin.minddump.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chin.minddump.audio.AudioRecorder
import com.chin.minddump.camera.CameraManager
import com.chin.minddump.storage.Space
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MindDumpViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val audioRecorder = remember { AudioRecorder() }
    val cameraManager = remember { CameraManager() }

    // Delete confirmation dialog
    var entryToDelete by remember { mutableStateOf<com.chin.minddump.storage.MindDumpEntry?>(null) }

    // Storage permission launcher
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.checkStoragePermission()
    }

    // Audio permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setAudioPermissionGranted(granted)
        if (granted) {
            val file = viewModel.getRecordingFile()
            audioRecorder.start(context, file)
            viewModel.startRecording()
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setCameraPermissionGranted(granted)
        if (granted) {
            viewModel.setCameraOpen(true)
        }
    }

    // File import launcher
    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(context, it)
            viewModel.importFile(it, fileName)
        }
    }

    // Grant storage permission on first launch
    LaunchedEffect(Unit) {
        if (!viewModel.uiState.value.storagePermissionGranted) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:com.chin.minddump")
                }
                storagePermissionLauncher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                storagePermissionLauncher.launch(intent)
            }
        }
    }

    MindDumpTheme(darkTheme = uiState.isDarkTheme) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // Main content
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (uiState.currentSpace) {
                                    Space.PUBLIC -> "MindDump · Public"
                                    Space.PRIVATE -> "MindDump · Private"
                                }
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                bottomBar = {
                    InputBar(
                        inputText = uiState.inputText,
                        isRecording = uiState.isRecording,
                        onInputChange = { viewModel.updateInputText(it) },
                        onSubmit = { viewModel.submitText() },
                        onRecordClick = {
                            if (uiState.isRecording) {
                                audioRecorder.stop()
                                viewModel.stopRecording()
                            } else {
                                audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onCameraClick = {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        },
                        onImportClick = {
                            fileImportLauncher.launch(arrayOf("*/*"))
                        }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Entry list
                    EntryList(
                        entries = uiState.entries,
                        onEntryClick = { entry ->
                            openFile(context, entry.file)
                        },
                        onEntryLongClick = { entry ->
                            entryToDelete = entry
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 72.dp) // space for FAB
                    )

                    // Space switch FAB - bottom right, above input bar
                    SpaceSwitchButton(
                        currentSpace = uiState.currentSpace,
                        onClick = { viewModel.toggleSpace() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 80.dp)
                    )
                }
            }

            // Camera overlay - FULL SCREEN on top of everything
            if (uiState.isCameraOpen) {
                // Ensure output files and directories exist
                val photoFile = viewModel.getPhotoFile()
                val videoFile = viewModel.getVideoFile()
                photoFile.parentFile?.mkdirs()
                videoFile.parentFile?.mkdirs()
                cameraManager.setOutputFiles(
                    photo = photoFile,
                    video = videoFile
                )
                CameraScreen(
                    cameraManager = cameraManager,
                    onClose = { viewModel.setCameraOpen(false) },
                    onCaptured = {
                        viewModel.setCameraOpen(false)
                        viewModel.onMediaCaptured()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Delete confirmation dialog
        entryToDelete?.let { entry ->
            AlertDialog(
                onDismissRequest = { entryToDelete = null },
                title = { Text("删除记录") },
                text = { Text("确定要删除 \"${entry.file.name}\" 吗？") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteEntry(entry)
                        entryToDelete = null
                    }) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { entryToDelete = null }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

private fun openFile(context: android.content.Context, file: File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, getMimeType(file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "无法打开文件: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun getMimeType(file: File): String {
    return when {
        file.name.endsWith(".md") || file.name.endsWith(".txt") -> "text/markdown"
        file.name.endsWith(".m4a") || file.name.endsWith(".aac") -> "audio/mp4"
        file.name.endsWith(".jpg") || file.name.endsWith(".jpeg") -> "image/jpeg"
        file.name.endsWith(".mp4") -> "video/mp4"
        file.name.endsWith(".png") -> "image/png"
        file.name.endsWith(".pdf") -> "application/pdf"
        else -> "application/octet-stream"
    }
}

private fun getFileName(context: android.content.Context, uri: Uri): String {
    var name = "unknown"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) {
            name = cursor.getString(nameIndex)
        }
    }
    return name
}
