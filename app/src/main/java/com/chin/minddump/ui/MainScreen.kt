package com.chin.minddump.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
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

    // SAF directory picker launcher
    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            // Persist permission so it survives app restarts
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, flags)

            val path = resolveTreeUriToPath(it)
            if (path != null) {
                viewModel.requestMigration(path)
            } else {
                Toast.makeText(context, "无法解析目录路径", Toast.LENGTH_SHORT).show()
            }
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

    // First-launch directory picker
    LaunchedEffect(uiState.storagePermissionGranted) {
        if (uiState.storagePermissionGranted && !viewModel.isWorkDirConfigured()) {
            dirPickerLauncher.launch(null)
        }
    }

    MindDumpTheme(darkTheme = uiState.isDarkTheme) {
        NiaBackground {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                // Main content
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = when (uiState.currentSpace) {
                                        Space.PUBLIC -> "MindDump · Public"
                                        Space.PRIVATE -> "MindDump · Private"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            },
                            actions = {
                                IconButton(onClick = { viewModel.setShowSettings(true) }) {
                                    Icon(
                                        Icons.Filled.Settings,
                                        contentDescription = "设置",
                                        tint = LocalTintTheme.current.iconTint
                                            .takeIf { it != Color.Unspecified }
                                            ?: MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = Color.Transparent,
                            )
                        )
                    },
                    bottomBar = {
                        InputBar(
                            inputText = uiState.inputText,
                            isRecording = uiState.isRecording,
                            currentSpace = uiState.currentSpace,
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
                            },
                            onSpaceToggle = { viewModel.toggleSpace() },
                            onFullscreenClick = {
                                viewModel.setFullscreenEdit(true)
                            }
                        )
                    },
                    containerColor = Color.Transparent,
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
                            modifier = Modifier.fillMaxSize()
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

                    // Fullscreen edit overlay
                    if (uiState.isFullscreenEdit) {
                        FullscreenEditScreen(
                            text = uiState.inputText,
                            onTextChange = { viewModel.updateInputText(it) },
                            onClose = { viewModel.setFullscreenEdit(false) },
                            onSubmit = {
                                viewModel.submitText()
                                viewModel.setFullscreenEdit(false)
                            }
                        )
                    }
                } // BoxWithConstraints

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

                // Settings dialog
                if (uiState.showSettings) {
                    AlertDialog(
                        onDismissRequest = { viewModel.setShowSettings(false) },
                        title = { Text("设置") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    "工作目录",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    uiState.workDir,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = { dirPickerLauncher.launch(null) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("更改目录")
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { viewModel.setShowSettings(false) }) {
                                Text("完成")
                            }
                        }
                    )
                }

                // Migration confirmation dialog
                if (uiState.showMigrationDialog) {
                    AlertDialog(
                        onDismissRequest = { viewModel.cancelMigration() },
                        title = { Text("迁移数据") },
                        text = {
                            val lines = mutableListOf<String>()
                            if (uiState.currentFileCount > 0) {
                                lines.add("当前目录有 ${uiState.currentFileCount} 个文件。")
                            }
                            if (uiState.newDirFileCount > 0) {
                                lines.add("新目录已有 ${uiState.newDirFileCount} 个文件。")
                            }
                            lines.add("是否将文件移动到新目录？")
                            Text(lines.joinToString("\n"))
                        },
                        confirmButton = {
                            TextButton(onClick = { viewModel.confirmMigration() }) {
                                Text("移动")
                            }
                        },
                        dismissButton = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { viewModel.cancelMigration() }) {
                                    Text("取消")
                                }
                                TextButton(onClick = { viewModel.skipMigration() }) {
                                    Text("直接切换")
                                }
                            }
                        }
                    )
                }
            } // NiaBackground
        } // MindDumpTheme
}

/**
 * Resolve a SAF tree URI to an absolute file system path.
 * Attempts multiple strategies since Android doesn't provide a reliable API for this.
 */
private fun resolveTreeUriToPath(treeUri: Uri): String? {
    val docId = DocumentsContract.getTreeDocumentId(treeUri)
    // docId format is typically like "primary:Downloads/MyFolder"
    // or "home:MyFolder" or a raw path
    val parts = docId.split(":")
    return when {
        parts.size == 2 -> {
            val storageId = parts[0]
            val relativePath = parts[1]
            when (storageId) {
                "primary" -> {
                    val basePath = android.os.Environment.getExternalStorageDirectory().absolutePath
                    if (relativePath.isEmpty()) basePath else "$basePath/$relativePath"
                }
                "home" -> {
                    val basePath = android.os.Environment.getExternalStorageDirectory().absolutePath
                    "$basePath/$relativePath"
                }
                else -> {
                    // Removable storage: /storage/{storageId}/{relativePath}
                    if (relativePath.isEmpty()) "/storage/$storageId" else "/storage/$storageId/$relativePath"
                }
            }
        }
        parts.size == 1 -> {
            // Could be a full path or just a storage ID
            val path = parts[0]
            if (path.startsWith("/")) path else "/storage/$path"
        }
        else -> null
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
