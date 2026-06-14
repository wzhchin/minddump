package com.chin.minddump.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
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
import androidx.compose.ui.unit.dp
import com.chin.minddump.R
import com.chin.minddump.storage.EntryRole
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.storage.Space
import com.chin.minddump.storage.TodoState
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
@Suppress("LongParameterList", "LongMethod")
fun EntryActionDrawer(
    entry: MindDumpEntry,
    currentSpace: Space,
    groups: List<File>,
    onRename: (String?) -> Unit,
    onDelete: () -> Unit,
    onMultiSelect: () -> Unit,
    onMoveToGroup: (File) -> Unit,
    onCreateGroup: (String?) -> Unit,
    onMoveToSpace: (Space) -> Unit,
    onDismiss: () -> Unit,
    onMoveOutOfGroup: (() -> Unit)? = null,
    onTogglePin: (() -> Unit)? = null,
    onSetStatus: ((TodoState) -> Unit)? = null,
    onAddComment: ((String) -> Unit)? = null,
) {
    val haptics = rememberPremiumHaptics()
    val shapes = LocalExpressiveShapes.current
    val sheetState = rememberBottomSheetState(SheetValue.Hidden)

    var showRenameDialog by remember { mutableStateOf(false) }
    var showGroupPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showStatusPicker by remember { mutableStateOf(false) }
    var showCommentDialog by remember { mutableStateOf(false) }

    val isComment = entry.role == EntryRole.COMMENT

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
            // Entry name header
            Text(
                text = entry.file.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp),
            )

            // Action items
            // Pin toggle — comments cannot be pinned.
            if (!isComment && onTogglePin != null) {
                ActionItem(
                    icon = Icons.Filled.PushPin,
                    label = stringResource(if (entry.isPinned) R.string.unpin else R.string.pin),
                    onClick = {
                        haptics.perform(HapticPattern.Tick)
                        onTogglePin()
                        onDismiss()
                    },
                )
            }
            // Todo status — comments cannot carry a status.
            if (!isComment && onSetStatus != null) {
                ActionItem(
                    icon = statusIcon(entry.todoState),
                    label = stringResource(R.string.todo_status),
                    trailing = {
                        if (entry.todoState != TodoState.NONE) {
                            Text(
                                text = statusLabel(entry.todoState),
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
            // Add comment — comments cannot target other comments.
            if (!isComment && onAddComment != null) {
                ActionItem(
                    icon = Icons.AutoMirrored.Filled.Comment,
                    label = stringResource(R.string.add_comment),
                    onClick = {
                        haptics.perform(HapticPattern.Tick)
                        showCommentDialog = true
                    },
                )
            }
            ActionItem(
                icon = Icons.Filled.DriveFileRenameOutline,
                label = "重命名",
                onClick = {
                    haptics.perform(HapticPattern.Tick)
                    showRenameDialog = true
                },
            )
            ActionItem(
                icon = Icons.Filled.SelectAll,
                label = "多选",
                onClick = {
                    haptics.perform(HapticPattern.Tick)
                    onMultiSelect()
                    onDismiss()
                },
            )
            ActionItem(
                icon = Icons.Filled.Folder,
                label = "移动到组",
                onClick = {
                    haptics.perform(HapticPattern.Tick)
                    showGroupPicker = true
                },
            )
            if (entry.groupPath != null && onMoveOutOfGroup != null) {
                ActionItem(
                    icon = Icons.Filled.FolderOpen,
                    label = "移出该组",
                    onClick = {
                        haptics.perform(HapticPattern.Tick)
                        onMoveOutOfGroup()
                        onDismiss()
                    },
                )
            }
            val targetSpace = if (currentSpace == Space.PUBLIC) Space.PRIVATE else Space.PUBLIC
            val spaceIcon = if (targetSpace == Space.PRIVATE) Icons.Filled.Lock else Icons.Filled.LockOpen
            val spaceLabel = if (targetSpace == Space.PUBLIC) "移动到公共" else "移动到私有"
            ActionItem(
                icon = spaceIcon,
                label = spaceLabel,
                onClick = {
                    haptics.perform(HapticPattern.Tick)
                    onMoveToSpace(targetSpace)
                    onDismiss()
                },
            )
            ActionItem(
                icon = Icons.Filled.Delete,
                label = "删除",
                isDestructive = true,
                onClick = {
                    haptics.perform(HapticPattern.Tick)
                    showDeleteConfirm = true
                },
            )
        }
    }

    // Sub-dialogs
    if (showRenameDialog) {
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
        DeleteConfirmDialog(
            entry = entry,
            onConfirm = {
                onDelete()
                showDeleteConfirm = false
                onDismiss()
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    if (showStatusPicker) {
        StatusPickerSheet(
            current = entry.todoState,
            onSelect = { state ->
                onSetStatus?.invoke(state)
                showStatusPicker = false
                onDismiss()
            },
            onDismiss = { showStatusPicker = false },
        )
    }

    if (showCommentDialog) {
        CommentDialog(
            onConfirm = { content ->
                onAddComment?.invoke(content)
                showCommentDialog = false
                onDismiss()
            },
            onDismiss = { showCommentDialog = false },
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
// Comment Dialog
// ──────────────────────────────────────────────

@Composable
fun CommentDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val shapes = LocalExpressiveShapes.current
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = shapes.cardMedium,
        title = { Text(stringResource(R.string.add_comment)) },
        text = {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text(stringResource(R.string.comment_dialog_placeholder)) },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = content.isNotBlank(),
                onClick = { onConfirm(content.trim()) },
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
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
) {
    androidx.compose.material3.TopAppBar(
        title = { Text("已选 $selectedCount 项") },
        navigationIcon = {
            TextButton(onClick = onDone) { Text("取消") }
        },
        actions = {
            TextButton(onClick = onMergeToGroup) { Text("合并为分组") }
            TextButton(onClick = onDelete) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
            TextButton(onClick = onDone) { Text("完成") }
        },
    )
}

// ──────────────────────────────────────────────
// Group Action Sheet (long-press on a group card)
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupActionSheet(
    groupName: String,
    isPinned: Boolean,
    todoState: TodoState,
    onRename: () -> Unit,
    onDissolve: () -> Unit,
    onDismiss: () -> Unit,
    onTogglePin: (() -> Unit)? = null,
    onSetStatus: ((TodoState) -> Unit)? = null,
) {
    val haptics = rememberPremiumHaptics()
    val shapes = LocalExpressiveShapes.current
    val sheetState = rememberBottomSheetState(SheetValue.Hidden)
    var showStatusPicker by remember { mutableStateOf(false) }

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
            Text(
                text = groupName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp),
            )
            if (onTogglePin != null) {
                ActionItem(
                    icon = Icons.Filled.PushPin,
                    label = stringResource(if (isPinned) R.string.unpin else R.string.pin),
                    onClick = {
                        haptics.perform(HapticPattern.Tick)
                        onTogglePin()
                        onDismiss()
                    },
                )
            }
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
            ActionItem(
                icon = Icons.Filled.DriveFileRenameOutline,
                label = "重命名组",
                onClick = {
                    haptics.perform(HapticPattern.Tick)
                    onRename()
                },
            )
            ActionItem(
                icon = Icons.Filled.Delete,
                label = "解散分组",
                isDestructive = true,
                onClick = {
                    haptics.perform(HapticPattern.Tick)
                    onDissolve()
                },
            )
        }
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
