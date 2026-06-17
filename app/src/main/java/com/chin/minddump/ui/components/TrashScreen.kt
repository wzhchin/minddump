package com.chin.minddump.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chin.minddump.R
import com.chin.minddump.storage.EntryType
import com.chin.minddump.storage.TrashedItem
import java.util.concurrent.TimeUnit

/**
 * Full-screen recycle-bin overlay. Lists every trashed item (both spaces) with
 * per-item Restore / Delete-forever and a top-bar Empty-trash. Routed from
 * Settings; shown while [visible] is true. Never decrypts — type icon derives
 * from the filename only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    visible: Boolean,
    items: List<TrashedItem>,
    onRestore: (TrashedItem) -> Unit,
    onDeleteForever: (TrashedItem) -> Unit,
    onEmptyTrash: () -> Unit,
    onBack: () -> Unit,
) {
    if (!visible) return
    var emptyConfirm by remember { mutableStateOf(false) }
    var foreverTarget by remember { mutableStateOf<TrashedItem?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.trash), style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cancel),
                            )
                        }
                    },
                    actions = {
                        if (items.isNotEmpty()) {
                            TextButton(onClick = { emptyConfirm = true }) {
                                Text(stringResource(R.string.empty_trash))
                            }
                        }
                    },
                )
            },
        ) { padding ->
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.trash_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items, key = { it.file.absolutePath }) { item ->
                        TrashedItemRow(
                            item = item,
                            onRestore = { onRestore(item) },
                            onDeleteForever = { foreverTarget = item },
                        )
                    }
                }
            }
        }
    }

    if (emptyConfirm) {
        AlertDialog(
            onDismissRequest = { emptyConfirm = false },
            title = { Text(stringResource(R.string.empty_trash)) },
            text = { Text(stringResource(R.string.empty_trash_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    emptyConfirm = false
                    onEmptyTrash()
                }) {
                    Text(
                        stringResource(R.string.delete_forever),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { emptyConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    foreverTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { foreverTarget = null },
            title = { Text(stringResource(R.string.delete_forever)) },
            text = { Text(stringResource(R.string.delete_forever_confirm, target.file.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteForever(target)
                    foreverTarget = null
                }) {
                    Text(
                        stringResource(R.string.delete_forever),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { foreverTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun TrashedItemRow(
    item: TrashedItem,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = iconFor(item.type),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.file.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.trashed_n_days_ago, daysAgo(item.trashedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRestore) {
            Icon(
                Icons.Filled.Restore,
                contentDescription = stringResource(R.string.restore),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onDeleteForever) {
            Icon(
                Icons.Filled.DeleteForever,
                contentDescription = stringResource(R.string.delete_forever),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun iconFor(type: EntryType): ImageVector = when (type) {
    EntryType.TEXT -> Icons.Filled.TextFields
    EntryType.RECORDING -> Icons.Filled.Mic
    EntryType.PHOTO -> Icons.Filled.Image
    EntryType.VIDEO -> Icons.Filled.PlayArrow
    EntryType.FILE -> Icons.AutoMirrored.Filled.InsertDriveFile
    EntryType.UNKNOWN -> Icons.AutoMirrored.Filled.InsertDriveFile
}

private fun daysAgo(trashedAt: Long): Int {
    val ms = System.currentTimeMillis() - trashedAt
    return TimeUnit.MILLISECONDS
        .toDays(ms)
        .coerceAtLeast(0)
        .toInt()
}
