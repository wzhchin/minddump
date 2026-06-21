package com.chin.minddump.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chin.minddump.R
import com.chin.minddump.storage.EntryEvent
import com.chin.minddump.storage.FileMetadata
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.storage.Space
import com.chin.minddump.storage.TodoState
import com.chin.minddump.ui.formatFriendlyDateTime
import com.chin.minddump.ui.theme.HapticPattern
import com.chin.minddump.ui.theme.LocalExpressiveShapes
import com.chin.minddump.ui.theme.rememberPremiumHaptics
import java.io.File

// ──────────────────────────────────────────────
// Entry Action Drawer (Task 5.1)
// ──────────────────────────────────────────────

// Action drawer aggregates many callbacks + their conditional sub-dialogs by design.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
fun EntryActionDrawer(
    onDismiss: () -> Unit,
    entry: MindDumpEntry? = null,
    groupTarget: File? = null,
    currentSpace: Space = Space.PUBLIC,
    groups: List<File> = emptyList(),
    onRename: (String?) -> Unit = {},
    onDelete: () -> Unit = {},
    onMultiSelect: () -> Unit = {},
    onMoveToGroup: (File) -> Unit = {},
    onCreateGroup: (String?) -> Unit = {},
    onMoveToSpace: (Space) -> Unit = {},
    onMoveOutOfGroup: (() -> Unit)? = null,
    onTogglePin: (() -> Unit)? = null,
    onSetStatus: ((TodoState) -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onEditTags: (() -> Unit)? = null,
    onAddEvent: (() -> Unit)? = null,
    // Group-only callbacks. Used only when [groupTarget] is non-null.
    onGroupDelete: (() -> Unit)? = null,
    onGroupDissolve: (() -> Unit)? = null,
) {
    require((entry == null) xor (groupTarget == null)) {
        "Exactly one of entry / groupTarget must be non-null"
    }
    val haptics = rememberPremiumHaptics()
    val shapes = LocalExpressiveShapes.current
    val sheetState = rememberBottomSheetState(SheetValue.Hidden)

    var showRenameDialog by remember { mutableStateOf(false) }
    var showGroupPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDissolveConfirm by remember { mutableStateOf(false) }
    var showStatusPicker by remember { mutableStateOf(false) }

    val isGroup = groupTarget != null

    // Target-derived presentation values.
    val groupMeta = groupTarget?.let { FileMetadata.fromFile(it) }
    val headerText = if (isGroup) {
        groupTarget!!
            .name
            .substringAfter("-f", groupTarget!!.name)
            .ifBlank { stringResource(R.string.group_unnamed) }
    } else {
        entry!!.file.name
    }
    val isPinned = if (isGroup) groupMeta?.isPinned == true else entry!!.isPinned
    val todoState = if (isGroup) (groupMeta?.todoState ?: TodoState.NONE) else entry!!.todoState
    val entryTags = if (isGroup) emptyList() else entry!!.tags
    val entryEvents = if (isGroup) emptyList() else entry!!.events

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = shapes.cardLarge as RoundedCornerShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            // Header: entry file name or group display name.
            Text(
                text = headerText,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp),
            )

            // Quick-action bar: label-free icon buttons that wrap to a second row.
            // Pure-presentation reorganization of the previously full-width rows;
            // every onClick, conditional-visibility rule, and sub-dialog below is
            // unchanged.
            val onSurfaceTint = MaterialTheme.colorScheme.onSurface
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Pin toggle — a pinned entry gets a primary-tint cue so the state
                // reads without a label.
                if (!isGroup && onTogglePin != null) {
                    IconButton(onClick = {
                        haptics.perform(HapticPattern.Tick)
                        onTogglePin()
                        onDismiss()
                    }) {
                        Icon(
                            Icons.Filled.PushPin,
                            contentDescription = stringResource(
                                if (isPinned) R.string.unpin else R.string.pin,
                            ),
                            tint = if (isPinned) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                onSurfaceTint
                            },
                        )
                    }
                }
                // Group pin toggle — groups CAN be pinned; entry uses the branch above.
                if (isGroup && onTogglePin != null) {
                    IconButton(onClick = {
                        haptics.perform(HapticPattern.Tick)
                        onTogglePin()
                    }) {
                        Icon(
                            Icons.Filled.PushPin,
                            contentDescription = stringResource(
                                if (isPinned) R.string.unpin else R.string.pin,
                            ),
                            tint = if (isPinned) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                onSurfaceTint
                            },
                        )
                    }
                }
                // Share — exports to other apps.
                if (onShare != null) {
                    IconButton(onClick = {
                        haptics.perform(HapticPattern.Tick)
                        onShare()
                        onDismiss()
                    }) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = stringResource(R.string.share),
                            tint = onSurfaceTint,
                        )
                    }
                }
                IconButton(onClick = {
                    haptics.perform(HapticPattern.Tick)
                    if (isGroup) {
                        onRename(null)
                        onDismiss()
                    } else {
                        showRenameDialog = true
                    }
                }) {
                    Icon(
                        Icons.Filled.DriveFileRenameOutline,
                        contentDescription = stringResource(R.string.action_rename),
                        tint = onSurfaceTint,
                    )
                }
                IconButton(onClick = {
                    haptics.perform(HapticPattern.Tick)
                    onMultiSelect()
                    onDismiss()
                }) {
                    Icon(
                        Icons.Filled.SelectAll,
                        contentDescription = stringResource(R.string.action_multi_select),
                        tint = onSurfaceTint,
                    )
                }
                if (!isGroup) {
                    IconButton(onClick = {
                        haptics.perform(HapticPattern.Tick)
                        showGroupPicker = true
                    }) {
                        Icon(
                            Icons.Filled.Folder,
                            contentDescription = stringResource(R.string.action_move_to_group),
                            tint = onSurfaceTint,
                        )
                    }
                }
                if (!isGroup && entry!!.groupPath != null && onMoveOutOfGroup != null) {
                    IconButton(onClick = {
                        haptics.perform(HapticPattern.Tick)
                        onMoveOutOfGroup()
                        onDismiss()
                    }) {
                        Icon(
                            Icons.Filled.FolderOpen,
                            contentDescription = stringResource(R.string.action_move_out_of_group),
                            tint = onSurfaceTint,
                        )
                    }
                }
                if (!isGroup) {
                    val targetSpace = if (currentSpace == Space.PUBLIC) Space.PRIVATE else Space.PUBLIC
                    val spaceIcon = if (targetSpace == Space.PRIVATE) Icons.Filled.Lock else Icons.Filled.LockOpen
                    val spaceLabelRes = if (targetSpace == Space.PUBLIC) {
                        R.string.action_move_to_public
                    } else {
                        R.string.action_move_to_private
                    }
                    IconButton(onClick = {
                        haptics.perform(HapticPattern.Tick)
                        onMoveToSpace(targetSpace)
                        onDismiss()
                    }) {
                        Icon(
                            spaceIcon,
                            contentDescription = stringResource(spaceLabelRes),
                            tint = onSurfaceTint,
                        )
                    }
                }
            }

            // Detail rows: full-width rows whose trailing summary surfaces state.

            // Todo status.
            if (onSetStatus != null) {
                ActionItem(
                    icon = statusIcon(todoState),
                    label = stringResource(R.string.todo_status),
                    trailing = {
                        if (todoState != TodoState.NONE) {
                            Text(
                                text = statusLabel(todoState),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        haptics.perform(HapticPattern.Tick)
                        showStatusPicker = true
                    },
                )
            }
            // Tags — open the tag editor sheet; shows existing tags inline.
            if (!isGroup && onEditTags != null) {
                ActionItem(
                    icon = Icons.Filled.Label,
                    label = stringResource(R.string.tag_add),
                    trailing = {
                        if (entryTags.isNotEmpty()) {
                            Text(
                                text = entryTags.joinToString(" ") { "#$it" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        haptics.perform(HapticPattern.Tick)
                        onEditTags()
                        onDismiss()
                    },
                )
            }
            // Scheduled event — open the date/time picker; shows the due time.
            if (!isGroup && onAddEvent != null) {
                ActionItem(
                    icon = Icons.Filled.Notifications,
                    label = stringResource(R.string.event_add),
                    trailing = {
                        nextEventDue(entryEvents)?.let { due ->
                            Text(
                                text = formatFriendlyDateTime(due),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        haptics.perform(HapticPattern.Tick)
                        onAddEvent()
                        onDismiss()
                    },
                )
            }
            // Dissolve — group only. Members move to the parent; folder removed.
            if (isGroup && onGroupDissolve != null) {
                ActionItem(
                    icon = Icons.Filled.FolderDelete,
                    label = stringResource(R.string.group_dissolve),
                    onClick = {
                        haptics.perform(HapticPattern.Tick)
                        showDissolveConfirm = true
                    },
                )
            }
            ActionItem(
                icon = Icons.Filled.Delete,
                label = stringResource(R.string.delete_action),
                isDestructive = true,
                onClick = {
                    haptics.perform(HapticPattern.Tick)
                    showDeleteConfirm = true
                },
            )
        }
    }

    // Sub-dialogs
    if (showRenameDialog && entry != null) {
        RenameDialog(
            currentName = extractOriginalName(entry),
            onConfirm = { newName ->
                onRename(newName)
                showRenameDialog = false
                onDismiss()
            },
            onDismiss = { showRenameDialog = false },
        )
    }

    if (showGroupPicker) {
        GroupPickerSheet(
            groups = groups,
            onSelectGroup = { groupDir ->
                onMoveToGroup(groupDir)
                showGroupPicker = false
                onDismiss()
            },
            onCreateGroup = { name ->
                onCreateGroup(name)
                showGroupPicker = false
                onDismiss()
            },
            onDismiss = { showGroupPicker = false },
        )
    }

    if (showDeleteConfirm) {
        if (entry != null) {
            DeleteConfirmDialog(
                entry = entry,
                onConfirm = {
                    onDelete()
                    showDeleteConfirm = false
                    onDismiss()
                },
                onDismiss = { showDeleteConfirm = false },
            )
        } else {
            // Group delete: whole tree to trash.
            ConfirmDialog(
                title = stringResource(R.string.group_delete_confirm_title),
                message = stringResource(R.string.group_delete_confirm, headerText),
                confirmLabel = stringResource(R.string.delete_action),
                onConfirm = {
                    onGroupDelete?.invoke()
                    showDeleteConfirm = false
                    onDismiss()
                },
                onDismiss = { showDeleteConfirm = false },
            )
        }
    }

    if (showDissolveConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.group_dissolve),
            message = stringResource(R.string.group_dissolve_confirm, headerText),
            confirmLabel = stringResource(R.string.group_dissolve),
            onConfirm = {
                onGroupDissolve?.invoke()
                showDissolveConfirm = false
                onDismiss()
            },
            onDismiss = { showDissolveConfirm = false },
        )
    }

    if (showStatusPicker) {
        StatusPickerSheet(
            current = todoState,
            onSelect = { state ->
                onSetStatus?.invoke(state)
                showStatusPicker = false
                onDismiss()
            },
            onDismiss = { showStatusPicker = false },
        )
    }
}

/** Localized label for a [TodoState], used in badges and the status picker. */
@Composable
fun statusLabel(state: TodoState): String = when (state) {
    TodoState.NONE -> stringResource(R.string.status_none)
    TodoState.TODO -> stringResource(R.string.status_todo)
    TodoState.DOING -> stringResource(R.string.status_doing)
    TodoState.WAIT -> stringResource(R.string.status_wait)
    TodoState.DONE -> stringResource(R.string.status_done)
    TodoState.CANCEL -> stringResource(R.string.status_cancel)
}

/** Leading icon for a [TodoState] in the drawer. */
private fun statusIcon(state: TodoState): ImageVector = when (state) {
    TodoState.DONE -> Icons.Filled.CheckCircle
    TodoState.CANCEL -> Icons.Filled.CheckCircle
    else -> Icons.Filled.TaskAlt
}

/**
 * The due time to surface in the reminder row's trailing summary: the soonest
 * pending event, else (if all have fired) the most-recent fired event, else null
 * when the entry has no events. The picker remains the place to manage them.
 */
fun nextEventDue(events: List<EntryEvent>): java.time.LocalDateTime? {
    if (events.isEmpty()) return null
    val pending = events
        .filter { it.state == com.chin.minddump.storage.EventState.PENDING }
        .minByOrNull { it.due }
    if (pending != null) return pending.due
    return events
        .filter { it.state == com.chin.minddump.storage.EventState.FIRED }
        .maxByOrNull { it.due }
        ?.due
}

private fun extractOriginalName(entry: MindDumpEntry): String? {
    val name = entry.file.name.removeSuffix(".enc")
    val dashFIndex = name.indexOf("-f-")
    return if (dashFIndex >= 0) {
        val afterF = name.substring(dashFIndex + 3)
        val dotIndex = afterF.lastIndexOf('.')
        if (dotIndex > 0) afterF.substring(0, dotIndex) else null
    } else {
        null
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val color = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.weight(1f))
            trailing()
        }
    }
}

// ──────────────────────────────────────────────
// Generic confirmation dialog (group delete / dissolve)
// ──────────────────────────────────────────────

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberPremiumHaptics()
    val shapes = LocalExpressiveShapes.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = shapes.cardMedium,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = {
                haptics.perform(HapticPattern.Thud)
                onConfirm()
            }) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                haptics.perform(HapticPattern.Cancel)
                onDismiss()
            }) {
                Text("取消")
            }
        },
    )
}

// ──────────────────────────────────────────────
// Rename Dialog (Task 5.2)
// ──────────────────────────────────────────────

@Composable
fun RenameDialog(
    currentName: String?,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val shapes = LocalExpressiveShapes.current
    var newName by remember { mutableStateOf(currentName ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = shapes.cardMedium,
        title = { Text("重命名") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("文件名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(newName.ifBlank { null })
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

// ──────────────────────────────────────────────
// Group Picker Sheet (Task 5.3)
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupPickerSheet(
    groups: List<File>,
    onSelectGroup: (File) -> Unit,
    onCreateGroup: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val shapes = LocalExpressiveShapes.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = shapes.cardLarge as RoundedCornerShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = "移动到组",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp, start = 8.dp),
            )

            // Create new group option
            ActionItem(
                icon = Icons.Filled.CreateNewFolder,
                label = "新建组",
                onClick = { showCreateDialog = true },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Existing groups
            if (groups.isEmpty()) {
                Text(
                    text = "暂无分组",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(groups, key = { it.absolutePath }) { groupDir ->
                        ActionItem(
                            icon = Icons.Filled.Folder,
                            label = groupDir.name,
                            onClick = { onSelectGroup(groupDir) },
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                newGroupName = ""
            },
            shape = shapes.cardMedium,
            title = { Text("新建组") },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("组名（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onCreateGroup(newGroupName.ifBlank { null })
                    showCreateDialog = false
                    newGroupName = ""
                }) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    newGroupName = ""
                }) {
                    Text("取消")
                }
            },
        )
    }
}

// ──────────────────────────────────────────────
// Multi-Select Top Bar
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectTopBar(
    selectedCount: Int,
    onMergeToGroup: () -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
    onShare: () -> Unit = {},
    selectedGroupCount: Int = 0,
) {
    val title = if (selectedGroupCount > 0) {
        stringResource(R.string.multiselect_count_with_groups, selectedCount, selectedGroupCount)
    } else {
        selectedCount.toString()
    }
    androidx.compose.material3.TopAppBar(
        title = {
            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        actions = {
            TextButton(onClick = onShare) { Text(stringResource(R.string.share)) }
            TextButton(onClick = onMergeToGroup) { Text("合并为分组") }
            TextButton(onClick = onDelete) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
            TextButton(onClick = onDone) { Text("完成") }
        },
    )
}

// ──────────────────────────────────────────────
// Todo Status Picker Sheet
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusPickerSheet(
    current: TodoState,
    onSelect: (TodoState) -> Unit,
    onDismiss: () -> Unit,
) {
    val shapes = LocalExpressiveShapes.current
    val haptics = rememberPremiumHaptics()
    val options = listOf(
        TodoState.NONE,
        TodoState.TODO,
        TodoState.DOING,
        TodoState.WAIT,
        TodoState.DONE,
        TodoState.CANCEL,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = shapes.cardLarge as RoundedCornerShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.todo_status),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp, start = 8.dp),
            )
            options.forEach { state ->
                ActionItem(
                    icon = statusIcon(state),
                    label = statusLabel(state),
                    trailing = {
                        if (state == current) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                    onClick = {
                        haptics.perform(HapticPattern.Tick)
                        onSelect(state)
                    },
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// Tag Editor Sheet
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditorSheet(
    tags: List<String>,
    suggestions: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val shapes = LocalExpressiveShapes.current
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    val matchingSuggestions = remember(input, suggestions) {
        if (input.isBlank()) suggestions
        else suggestions
            .filter { it.contains(input.trim(), ignoreCase = true) }
            .filterNot { existing -> tags.any { it.equals(existing, ignoreCase = true) } }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = shapes.cardLarge as RoundedCornerShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.tag_add),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp, start = 8.dp),
            )

            // Existing tags as removable chips.
            if (tags.isNotEmpty()) {
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.forEach { tag ->
                        InputChip(
                            label = { Text("#$tag") },
                            selected = false,
                            onClick = { onRemove(tag) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(InputChipDefaults.IconSize),
                                )
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it
                    error = false
                },
                label = { Text(stringResource(R.string.tag_add)) },
                isError = error,
                supportingText = if (error) {
                    { Text(stringResource(R.string.tag_invalid)) }
                } else {
                    null
                },
                singleLine = true,
                trailingIcon = {
                    TextButton(
                        onClick = {
                            val candidate = input.trim()
                            if (candidate.isEmpty()) return@TextButton
                            if (com.chin.minddump.storage.TagValidator
                                    .normalize(candidate) == null) {
                                error = true
                            } else {
                                onAdd(candidate)
                                input = ""
                            }
                        },
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // Autocomplete suggestions.
            if (matchingSuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.tag_filter_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                )
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    matchingSuggestions.take(20).forEach { suggestion ->
                        AssistChip(
                            onClick = { onAdd(suggestion) },
                            label = { Text("#$suggestion") },
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Event Date/Time Picker
// ──────────────────────────────────────────────

/**
 * Two-step picker: choose a date, then a time. Invokes [onConfirm] with the
 * combined local date-time, or [onDismiss]. Returns null if the chosen time is
 * in the past (caller should warn).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDateTimePicker(
    onConfirm: (java.time.LocalDateTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val shapes = LocalExpressiveShapes.current
    val datePickerState = rememberDatePickerState()
    var pickingTime by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()

    if (!pickingTime) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            shape = shapes.cardMedium,
            confirmButton = {
                TextButton(
                    enabled = datePickerState.selectedDateMillis != null,
                    onClick = { pickingTime = true },
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = shapes.cardMedium,
            title = { Text(stringResource(R.string.event_due_at)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val dateMs = datePickerState.selectedDateMillis ?: return@TextButton
                    val instant = java.time.Instant.ofEpochMilli(dateMs)
                    val date = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    val time = java.time.LocalTime.of(timePickerState.hour, timePickerState.minute)
                    onConfirm(java.time.LocalDateTime.of(date, time))
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

// ──────────────────────────────────────────────
// Reminder Sheet (WeChat-style)
// ──────────────────────────────────────────────

/**
 * WeChat-style reminder authoring sheet. Lists the entry's existing reminders
 * (removable), offers quick-selection chips for the next few days, and a
 * "custom…" affordance that opens [EventDateTimePicker] for an exact date/time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSheet(
    events: List<EntryEvent>,
    onSchedule: (java.time.LocalDateTime) -> Unit,
    onRemove: (EntryEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    val shapes = LocalExpressiveShapes.current
    var showCustomPicker by remember { mutableStateOf(false) }
    val haptics = rememberPremiumHaptics()
    val now = remember { java.time.LocalDateTime.now() }

    // Quick-selection chips: (labelRes, computeDue) — due is never in the past.
    val quickOptions: List<Pair<Int, () -> java.time.LocalDateTime>> = remember(now) {
        listOf(
            R.string.reminder_today to { nextHourToday(now) },
            R.string.reminder_tomorrow to {
                java.time.LocalDateTime.of(now.toLocalDate().plusDays(1), java.time.LocalTime.of(9, 0))
            },
            R.string.reminder_day_after to {
                java.time.LocalDateTime.of(now.toLocalDate().plusDays(2), java.time.LocalTime.of(9, 0))
            },
            R.string.reminder_next_monday to {
                val monday = nextMonday(now.toLocalDate())
                java.time.LocalDateTime.of(monday, java.time.LocalTime.of(9, 0))
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = shapes.cardLarge as RoundedCornerShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.reminder_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp, start = 8.dp),
            )

            // Existing reminders — removable rows.
            if (events.isNotEmpty()) {
                events.sortedBy { it.due }.forEach { event ->
                    ReminderRow(event = event, onRemove = {
                        haptics.perform(HapticPattern.Tick)
                        onRemove(event)
                    })
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            }

            // Quick-selection chips.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                quickOptions.forEach { (labelRes, compute) ->
                    val due = compute()
                    val enabled = due.isAfter(now)
                    SuggestionChip(
                        enabled = enabled,
                        onClick = {
                            if (enabled) {
                                haptics.perform(HapticPattern.Tick)
                                onSchedule(due)
                                onDismiss()
                            }
                        },
                        label = {
                            Text(stringResource(labelRes) + " " + formatFriendlyDateTime(due))
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom date+time entry.
            FilledTonalButton(
                onClick = {
                    haptics.perform(HapticPattern.Tick)
                    showCustomPicker = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.reminder_custom))
            }
        }
    }

    if (showCustomPicker) {
        EventDateTimePicker(
            onConfirm = { dateTime ->
                onSchedule(dateTime)
                showCustomPicker = false
                onDismiss()
            },
            onDismiss = { showCustomPicker = false },
        )
    }
}

@Composable
private fun ReminderRow(event: EntryEvent, onRemove: () -> Unit) {
    val isFired = event.state == com.chin.minddump.storage.EventState.FIRED
    val suffixRes = if (isFired) R.string.event_fired else R.string.event_pending
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = if (isFired) 0.4f else 1f),
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatFriendlyDateTime(event.due),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isFired) 0.6f else 1f),
            )
            Text(
                text = stringResource(suffixRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.event_remove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Today at the next hour, clamped to 08:00..22:00; falls to tomorrow 09:00 if past. */
private fun nextHourToday(now: java.time.LocalDateTime): java.time.LocalDateTime {
    var hour = (now.hour + 1).coerceIn(8, 22)
    var candidate = now.toLocalDate().atTime(hour, 0)
    // If today is already exhausted (hour clamped to 22 and still in the past),
    // roll to tomorrow 09:00.
    if (!candidate.isAfter(now)) {
        candidate = now.toLocalDate().plusDays(1).atTime(9, 0)
    }
    return candidate
}

private fun nextMonday(today: java.time.LocalDate): java.time.LocalDate {
    val dow = today.dayOfWeek.value // Mon=1..Sun=7
    val delta = ((8 - dow) % 7).let { if (it == 0) 7 else it } // days until next Monday
    return today.plusDays(delta.toLong())
}
