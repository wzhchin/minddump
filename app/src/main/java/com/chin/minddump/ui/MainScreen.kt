package com.chin.minddump.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chin.minddump.audio.AudioRecorder
import com.chin.minddump.R
import com.chin.minddump.storage.EntryRole
import com.chin.minddump.storage.EntryType
import com.chin.minddump.storage.FileMetadata
import com.chin.minddump.storage.TodoState
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.storage.Space
import com.chin.minddump.ui.components.BiometricGate
import com.chin.minddump.ui.components.EntryActionDrawer
import com.chin.minddump.ui.components.GroupActionSheet
import com.chin.minddump.ui.components.GroupPickerSheet
import com.chin.minddump.ui.components.MigrationDialog
import com.chin.minddump.ui.components.MultiSelectTopBar
import com.chin.minddump.ui.components.PasswordInputDialog
import com.chin.minddump.ui.components.PasswordSetupDialog
import com.chin.minddump.ui.components.RebuildDatabaseDialog
import com.chin.minddump.ui.components.SettingsDialog
import com.chin.minddump.ui.components.SpaceSelectionDialog
import com.chin.minddump.ui.theme.AppThemeMode
import com.chin.minddump.ui.theme.HapticPattern
import com.chin.minddump.ui.theme.LocalAnimationDuration
import com.chin.minddump.ui.theme.LocalExpressiveShapes
import com.chin.minddump.ui.theme.rememberPremiumHaptics
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun MainScreen(
    viewModel: MindDumpViewModel = viewModel(),
    audioRecorder: AudioRecorder,
    onNavigateToCamera: () -> Unit = {},
    onNavigateToFullscreenEdit: (entryPath: String?) -> Unit = {},
    onNavigateToStatistics: () -> Unit = {},
    onNavigateToGroupDetail: (groupPath: String) -> Unit = {},
    // The current scope directory. null = root feed (the current month dir,
    // treated as the root group); a non-null File = an open group page. The same
    // composable serves every level — the route carries which dir is current.
    currentDir: java.io.File? = null,
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val animDuration = LocalAnimationDuration.current
    val shapes = LocalExpressiveShapes.current
    val haptics = rememberPremiumHaptics()
    val scope = rememberCoroutineScope()

    // The root month directory is the root group; an open group is just a deeper
    // current dir. Set it once per route entry — never cleared by a dispose
    // (returning to root navigates back to the root route, whose currentDir is
    // null and re-sets the scope via this same effect).
    LaunchedEffect(currentDir) { viewModel.setCurrentGroupDir(currentDir) }

    val isGroupScope = currentDir != null
    val groupName = currentDir?.let {
        FileMetadata.fromFile(it)?.originalName ?: it.name
    } ?: ""
    // Direct members of the current scope: root → ungrouped entries, group →
    // entries whose groupPath matches this dir.
    val scopeMembers = uiState.entries.filter { entry ->
        entry.groupPath == currentDir?.absolutePath
    }
    // Sub-group cards: month-top groups at root, child groups inside a group.
    val scopeGroups = if (isGroupScope) uiState.childGroups else uiState.groups

    @Suppress("UnusedPrivateProperty")
    var entryToDelete by remember { mutableStateOf<MindDumpEntry?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Search bar expanded state
    var searchExpanded by remember { mutableStateOf(false) }

    // App bar background color animation
    val appBarContainerColor by animateColorAsState(
        targetValue = if (searchExpanded) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(animDuration.short),
        label = "appbar_bg",
    )

    // --- Launchers ---
    val storagePermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.checkStoragePermission()
        }

    val audioPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.setAudioPermissionGranted(granted)
            if (granted) {
                audioRecorder.start(context, viewModel.getRecordingFile())
                viewModel.startRecording()
            }
        }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.setCameraPermissionGranted(granted)
            if (granted) onNavigateToCamera()
        }

    val fileImportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { viewModel.importFile(it, getFileName(context, it)) }
        }

    val dirPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            uri?.let { handleDirPick(context, it, viewModel) }
        }

    // --- Init effects ---
    LaunchedEffect(Unit) {
        if (!viewModel.uiState.value.storagePermissionGranted) {
            requestStoragePermission(storagePermissionLauncher)
        }
    }

    LaunchedEffect(uiState.storagePermissionGranted) {
        if (uiState.storagePermissionGranted && !viewModel.isWorkDirConfigured()) {
            dirPickerLauncher.launch(null)
        }
    }

    // Refresh the group list the moment the action drawer or merge picker opens,
    // so the picker always reflects the latest disk state (fixes the
    // "暂无分组" bug where groups were only loaded as a side-effect of refresh).
    // Scope-aware: child groups inside a group page, month-top groups at root.
    LaunchedEffect(uiState.selectedEntryForAction, uiState.isMultiSelectMode) {
        if (uiState.selectedEntryForAction != null || uiState.isMultiSelectMode) {
            viewModel.refreshForCurrentScope()
        }
    }

    // --- Theme + Layout ---
    // Private space forces a dark theme (privacy), overriding the user's mode pref.
    val baseThemePrefs = viewModel.themePreferences.collectAsState().value
    val themePrefs = if (uiState.isDarkTheme) {
        baseThemePrefs.copy(mode = AppThemeMode.DARK)
    } else {
        baseThemePrefs
    }
    MindDumpTheme(preferences = themePrefs) {
        NiaBackground {
            Scaffold(
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            actionColor = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                topBar = {
                    if (uiState.isMultiSelectMode) {
                        MultiSelectTopBar(
                            selectedCount = uiState.selectedEntries.size,
                            onMergeToGroup = { viewModel.showGroupMergePicker() },
                            onDelete = { viewModel.deleteSelectedEntries() },
                            onDone = { viewModel.exitMultiSelectMode() },
                        )
                    } else if (isGroupScope) {
                        // A group page: back arrow + group name. No search/stats/
                        // settings here — those are root-feed affordances.
                        TopAppBar(
                            title = { Text(groupName.ifBlank { "未命名分组" }) },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "返回",
                                    )
                                }
                            },
                        )
                    } else {
                        CenterAlignedTopAppBar(
                            title = {
                                // Search field when expanded
                                AnimatedVisibility(
                                    visible = searchExpanded,
                                    enter = expandHorizontally(
                                        expandFrom = Alignment.Start,
                                        animationSpec = tween(animDuration.medium),
                                    ) + fadeIn(tween(animDuration.medium)),
                                    exit = shrinkHorizontally(
                                        shrinkTowards = Alignment.End,
                                        animationSpec = tween(animDuration.medium),
                                    ) + fadeOut(tween(animDuration.medium)),
                                ) {
                                    OutlinedTextField(
                                        value = uiState.searchQuery,
                                        onValueChange = { viewModel.updateSearchQuery(it) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(end = 8.dp),
                                        placeholder = { Text(stringResource(R.string.search_placeholder)) },
                                        singleLine = true,
                                        shape = shapes.inputField,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        ),
                                        trailingIcon = {
                                            if (uiState.searchQuery.isNotEmpty()) {
                                                IconButton(onClick = {
                                                    viewModel.clearSearch()
                                                    searchExpanded = false
                                                }) {
                                                    Icon(
                                                        Icons.Filled.Search,
                                                        contentDescription = stringResource(R.string.cancel),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                            }
                                        },
                                    )
                                }
                            },
                            actions = {
                                if (!searchExpanded) {
                                    // Search
                                    IconButton(onClick = {
                                        haptics.perform(HapticPattern.Tick)
                                        searchExpanded = true
                                    }) {
                                        Icon(
                                            Icons.Filled.Search,
                                            contentDescription = stringResource(R.string.search_placeholder),
                                            tint = LocalTintTheme.current.iconTint
                                                .takeIf { it != Color.Unspecified }
                                                ?: MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    // Statistics
                                    IconButton(onClick = {
                                        haptics.perform(HapticPattern.Tick)
                                        onNavigateToStatistics()
                                    }) {
                                        Icon(
                                            Icons.Filled.BarChart,
                                            contentDescription = "统计",
                                            tint = LocalTintTheme.current.iconTint
                                                .takeIf { it != Color.Unspecified }
                                                ?: MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    // Settings
                                    IconButton(onClick = {
                                        haptics.perform(HapticPattern.Tick)
                                        viewModel.setShowSettings(true)
                                    }) {
                                        Icon(
                                            Icons.Filled.Settings,
                                            contentDescription = stringResource(R.string.settings),
                                            tint = LocalTintTheme.current.iconTint
                                                .takeIf { it != Color.Unspecified }
                                                ?: MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = appBarContainerColor,
                            ),
                        )
                    }
                },
                bottomBar = {
                    // Hide the input bar during multi-select
                    if (!uiState.isMultiSelectMode) {
                        InputBar(
                            inputText = uiState.inputText,
                            isRecording = uiState.isRecording,
                            currentSpace = uiState.currentSpace,
                            showSpaceToggle = !isGroupScope,
                            actions = InputBarActions(
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
                                onImportClick = { fileImportLauncher.launch(arrayOf("*/*")) },
                                onSpaceToggle = { viewModel.requestSpaceSwitch() },
                                onFullscreenClick = { onNavigateToFullscreenEdit(null) },
                            ),
                        )
                    }
                },
                containerColor = Color.Transparent,
            ) { paddingValues ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                ) {
                    // Entry list
                    Box(modifier = Modifier.fillMaxSize()) {
                        EntryList(
                            entries = scopeMembers,
                            onEntryClick = { entry ->
                                if (uiState.isMultiSelectMode) {
                                    viewModel.toggleEntrySelection(entry)
                                } else {
                                    onEntryOpen(
                                        context = context,
                                        entry = entry,
                                        onTextEdit = { file -> onNavigateToFullscreenEdit(file.absolutePath) },
                                    )
                                }
                            },
                            onEntryLongClick = { entry ->
                                if (uiState.isMultiSelectMode) {
                                    viewModel.toggleEntrySelection(entry)
                                } else {
                                    viewModel.selectEntryForAction(entry)
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            isMultiSelectMode = uiState.isMultiSelectMode,
                            selectedEntries = uiState.selectedEntries,
                            groups = scopeGroups,
                            onGroupClick = { groupDir ->
                                // Drill in: the route entry owns setCurrentGroupDir.
                                onNavigateToGroupDetail(groupDir.absolutePath)
                            },
                            onGroupLongClick = { groupDir ->
                                haptics.perform(HapticPattern.Buildup)
                                viewModel.selectGroupForMenu(groupDir)
                            },
                        )
                    }
                }
            }

            // --- Dialogs ---
            uiState.selectedEntryForAction?.let { entry ->
                EntryActionDrawer(
                    entry = entry,
                    currentSpace = uiState.currentSpace,
                    groups = scopeGroups.map { it.groupDir },
                    onRename = { newName ->
                        viewModel.renameEntry(entry, newName)
                    },
                    onDelete = {
                        viewModel.deleteEntry(entry)
                    },
                    onMultiSelect = {
                        viewModel.enterMultiSelectMode(entry)
                    },
                    onMoveToGroup = { groupDir ->
                        viewModel.moveToGroup(entry, groupDir)
                    },
                    onCreateGroup = { name ->
                        viewModel.createAndMoveToGroup(entry, name)
                    },
                    onMoveToSpace = { targetSpace ->
                        viewModel.moveEntryToSpace(entry, targetSpace)
                    },
                    onTogglePin = {
                        viewModel.toggleEntryPinned(entry)
                    },
                    onSetStatus = { state ->
                        viewModel.setEntryStatus(entry, state)
                    },
                    onAddComment = { content ->
                        viewModel.addComment(entry, content)
                    },
                    onDismiss = { viewModel.clearEntryAction() },
                )
            }

            // Long-press group action menu
            uiState.groupMenuFor?.let { groupDir ->
                val groupMeta = FileMetadata.fromFile(groupDir)
                GroupActionSheet(
                    groupName = groupDir.name.substringAfter("-g", groupDir.name),
                    isPinned = groupMeta?.isPinned == true,
                    todoState = groupMeta?.todoState ?: TodoState.NONE,
                    onRename = {
                        viewModel.dismissGroupMenu()
                        viewModel.requestRenameGroup(groupDir)
                    },
                    onDissolve = {
                        viewModel.dismissGroupMenu()
                        viewModel.dissolveGroup(groupDir)
                    },
                    onTogglePin = {
                        viewModel.toggleGroupPinned(groupDir)
                    },
                    onSetStatus = { state ->
                        viewModel.setGroupStatus(groupDir, state)
                    },
                    onDismiss = { viewModel.dismissGroupMenu() },
                )
            }

            // Rename group dialog
            uiState.groupToRename?.let { groupDir ->
                val shapes2 = LocalExpressiveShapes.current
                var newName by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { viewModel.cancelRenameGroup() },
                    shape = shapes2.cardMedium,
                    title = { Text("重命名组") },
                    text = {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("组名（可选）") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.renameGroup(groupDir, newName.ifBlank { null })
                        }) { Text("确定") }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.cancelRenameGroup() }) { Text("取消") }
                    },
                )
            }

            // Multi-select merge-to-group picker
            if (uiState.showGroupMergePicker) {
                GroupPickerSheet(
                    groups = uiState.groups.map { it.groupDir },
                    onSelectGroup = { groupDir -> viewModel.mergeSelectedIntoGroup(groupDir) },
                    onCreateGroup = { name -> viewModel.mergeSelectedIntoNewGroup(name) },
                    onDismiss = { viewModel.dismissGroupMergePicker() },
                )
            }

            if (uiState.showSettings) {
                SettingsDialog(
                    workDir = uiState.workDir,
                    themePreferences = baseThemePrefs,
                    onSeedColorChange = { color -> viewModel.setSeedColor(color) },
                    onPaletteStyleChange = { style -> viewModel.setPaletteStyle(style) },
                    onThemeModeChange = { mode -> viewModel.setThemeMode(mode) },
                    onAmoledChange = { enabled -> viewModel.setAmoled(enabled) },
                    onChangeDir = { dirPickerLauncher.launch(null) },
                    onRebuildDatabase = { viewModel.showRebuildDatabaseDialog() },
                    onDismiss = { viewModel.setShowSettings(false) },
                )
            }

            if (uiState.showRebuildDatabaseDialog) {
                RebuildDatabaseDialog(
                    running = uiState.isRebuildingDatabase,
                    onConfirm = {
                        scope.launch {
                            val count = viewModel.rebuildDatabase()
                            snackbarHostState.showSnackbar(
                                message = context.getString(R.string.rebuild_database_success, count),
                                duration = SnackbarDuration.Short,
                            )
                        }
                    },
                    onDismiss = { viewModel.dismissRebuildDatabaseDialog() },
                )
            }

            if (uiState.showMigrationDialog) {
                MigrationDialog(
                    currentFileCount = uiState.currentFileCount,
                    newDirFileCount = uiState.newDirFileCount,
                    onConfirm = { viewModel.confirmMigration() },
                    onSkip = { viewModel.skipMigration() },
                    onCancel = { viewModel.cancelMigration() },
                )
            }

            val activity = context as? androidx.fragment.app.FragmentActivity
            if (uiState.pendingSpaceSwitch && activity != null) {
                BiometricGate(
                    activity = activity,
                    onSuccess = { viewModel.confirmSpaceSwitch() },
                    onError = { viewModel.cancelSpaceSwitch() },
                )
            }

            if (uiState.showPasswordSetup) {
                PasswordSetupDialog(
                    onConfirm = { viewModel.setPassword(it) },
                    onDismiss = { viewModel.cancelPasswordDialog() },
                )
            }

            if (uiState.showPasswordInput) {
                PasswordInputDialog(
                    onConfirm = { viewModel.verifyPassword(it) },
                    onDismiss = { viewModel.cancelPasswordDialog() },
                )
            }

            // --- Share intent dialog ---
            uiState.pendingShareItems?.let { items ->
                SpaceSelectionDialog(
                    items = items,
                    onPublicSelected = {
                        viewModel.confirmShare(Space.PUBLIC)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = context.getString(R.string.share_saved),
                                duration = SnackbarDuration.Short,
                            )
                        }
                    },
                    onPrivateSelected = {
                        viewModel.confirmShare(Space.PRIVATE)
                        val msg = if (!viewModel.isSessionUnlocked()) {
                            context.getString(R.string.share_saved_unencrypted)
                        } else {
                            context.getString(R.string.share_saved)
                        }
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = msg,
                                duration = SnackbarDuration.Short,
                            )
                        }
                    },
                    onDismiss = { viewModel.cancelShare() },
                )
            }
        }
    }
}

// --- Helper functions ---

private fun requestStoragePermission(
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
) {
    try {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:com.chin.minddump")
        }
        launcher.launch(intent)
    } catch (_: ActivityNotFoundException) {
        launcher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
    }
}

private fun handleDirPick(
    context: Context,
    uri: Uri,
    viewModel: MindDumpViewModel,
) {
    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    context.contentResolver.takePersistableUriPermission(uri, flags)

    val path = resolveTreeUriToPath(uri)
    if (path != null) {
        val dir = File(path)
        if (dir.exists() && dir.canWrite()) {
            viewModel.requestMigration(path)
        } else {
            Toast.makeText(context, context.getString(R.string.dir_not_writable), Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, context.getString(R.string.dir_unsupported), Toast.LENGTH_LONG).show()
    }
}

private fun resolveTreeUriToPath(treeUri: Uri): String? {
    val docId = DocumentsContract.getTreeDocumentId(treeUri)
    val parts = docId.split(":")

    return when {
        parts.size == 2 -> {
            val storageId = parts[0]
            val relativePath = parts[1]
            when (storageId) {
                "primary", "home" -> {
                    val basePath = android.os.Environment
                        .getExternalStorageDirectory()
                        .absolutePath
                    if (relativePath.isEmpty()) basePath else "$basePath/$relativePath"
                }
                else -> {
                    val candidate = if (relativePath.isEmpty()) {
                        "/storage/$storageId"
                    } else {
                        "/storage/$storageId/$relativePath"
                    }
                    if (File(candidate).exists()) candidate else null
                }
            }
        }
        parts.size == 1 -> {
            val path = parts[0]
            if (path.startsWith("/") && File(path).exists()) path else null
        }
        else -> null
    }
}

fun openFile(context: Context, file: File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, getMimeType(file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.cannot_open_file, e.message), Toast.LENGTH_SHORT).show()
    }
}

/**
 * Unified tap-to-open dispatch: text entries and comments open the in-app
 * fullscreen editor; everything else opens in the external viewer. Used by the
 * main list, group detail, and comment bubbles so the rule lives in one place.
 */
fun onEntryOpen(
    context: Context,
    entry: MindDumpEntry,
    onTextEdit: (File) -> Unit,
) {
    if (entry.type == EntryType.TEXT || entry.role == EntryRole.COMMENT) {
        onTextEdit(entry.file)
    } else {
        openFile(context, entry.file)
    }
}

private fun getMimeType(file: File): String =
    when {
        file.name.endsWith(".md") || file.name.endsWith(".txt") -> "text/markdown"
        file.name.endsWith(".m4a") || file.name.endsWith(".aac") -> "audio/mp4"
        file.name.endsWith(".jpg") || file.name.endsWith(".jpeg") -> "image/jpeg"
        file.name.endsWith(".mp4") -> "video/mp4"
        file.name.endsWith(".png") -> "image/png"
        file.name.endsWith(".pdf") -> "application/pdf"
        else -> "application/octet-stream"
    }

fun getFileName(context: Context, uri: Uri): String {
    var name = "unknown"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) {
            name = cursor.getString(nameIndex)
        }
    }
    return name
}
