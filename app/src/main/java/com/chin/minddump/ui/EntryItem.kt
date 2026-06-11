package com.chin.minddump.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chin.minddump.storage.EntryType
import com.chin.minddump.storage.MindDumpEntry
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryList(
    entries: List<MindDumpEntry>,
    onEntryClick: (MindDumpEntry) -> Unit,
    onEntryLongClick: (MindDumpEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "还没有任何记录\n点击下方输入框开始录入",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        reverseLayout = true
    ) {
        items(entries, key = { it.file.absolutePath }) { entry ->
            EntryItem(
                entry = entry,
                onClick = { onEntryClick(entry) },
                onLongClick = { onEntryLongClick(entry) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryItem(
    entry: MindDumpEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // Use filled card for text entries (tonal elevation), outlined for others
    if (entry.type == EntryType.TEXT) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {
            TextEntryContent(entry)
        }
    } else {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
        ) {
            OtherEntryContent(entry)
        }
    }
}

@Composable
private fun TextEntryContent(entry: MindDumpEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Notes,
            contentDescription = entry.type.name,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            val textContent = remember(entry.file) {
                try {
                    entry.file.readText().take(500)
                } catch (_: Exception) {
                    entry.file.name
                }
            }
            Text(
                text = textContent,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatEntryMeta(entry),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OtherEntryContent(entry: MindDumpEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (entry.type) {
                EntryType.RECORDING -> Icons.Filled.Mic
                EntryType.PHOTO -> Icons.Filled.PhotoCamera
                EntryType.VIDEO -> Icons.Filled.Videocam
                EntryType.FILE -> Icons.AutoMirrored.Filled.InsertDriveFile
                EntryType.UNKNOWN -> Icons.Filled.Description
                else -> Icons.Filled.Description
            },
            contentDescription = entry.type.name,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.file.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatEntryMeta(entry),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = entry.dateFolder,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatEntryMeta(entry: MindDumpEntry): String {
    val size = entry.file.length()
    val sizeStr = when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> "${size / (1024 * 1024)} MB"
    }
    val timeStr = try {
        val input = SimpleDateFormat("HHmmss", Locale.getDefault())
        val output = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        output.format(input.parse(entry.timestamp) ?: "")
    } catch (_: Exception) {
        entry.timestamp
    }
    return "$timeStr · $sizeStr"
}
