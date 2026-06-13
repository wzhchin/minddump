package com.chin.minddump.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chin.minddump.storage.FileMetadata
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.ui.components.EntryActionDrawer

/**
 * Shows the members of the currently selected group ([uiState.selectedGroup]).
 * Members render the same as on the main list; long-press opens the action
 * drawer with an added "移出该组" option.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    viewModel: MindDumpViewModel,
    onBack: () -> Unit,
    onNavigateToFullscreenEdit: (entryPath: String?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val groupDir = uiState.selectedGroup
    val groupName = groupDir?.let { FileMetadata.fromFile(it)?.originalName ?: it.name }
        ?: "分组"

    val members = if (groupDir != null) {
        uiState.entries.filter { it.groupPath == groupDir.absolutePath }
    } else {
        emptyList()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(groupName.ifBlank { "未命名分组" }) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearSelectedGroup()
                        onBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (members.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Text(
                        text = "空分组",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    items(
                        items = members,
                        key = { it.file.absolutePath },
                    ) { entry ->
                        GroupedEntryItem(
                            groupedEntry = GroupedEntry(entry, emptyList()),
                            onClick = {
                                onEntryOpen(
                                    context = context,
                                    entry = entry,
                                    onTextEdit = { file -> onNavigateToFullscreenEdit(file.absolutePath) },
                                )
                            },
                            onLongClick = { viewModel.selectEntryForAction(entry) },
                            onCommentClick = { /* no comments in detail view */ },
                        )
                    }
                }
            }
        }
    }

    // Action drawer for a selected member — includes 移出该组
    uiState.selectedEntryForAction?.let { entry ->
        EntryActionDrawer(
            entry = entry,
            currentSpace = uiState.currentSpace,
            groups = emptyList(),
            onRename = { newName -> viewModel.renameEntry(entry, newName) },
            onDelete = { viewModel.deleteEntry(entry) },
            onMultiSelect = { viewModel.enterMultiSelectMode(entry) },
            onMoveToGroup = { groupDir -> viewModel.moveToGroup(entry, groupDir) },
            onCreateGroup = { name -> viewModel.createAndMoveToGroup(entry, name) },
            onMoveToSpace = { targetSpace -> viewModel.moveEntryToSpace(entry, targetSpace) },
            onDismiss = { viewModel.clearEntryAction() },
            onMoveOutOfGroup = {
                viewModel.moveEntryOutOfGroup(entry)
                viewModel.clearEntryAction()
            },
        )
    }
}
