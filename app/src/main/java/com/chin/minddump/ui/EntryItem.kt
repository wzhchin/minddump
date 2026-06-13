package com.chin.minddump.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chin.minddump.R
import com.chin.minddump.storage.EntryRole
import com.chin.minddump.storage.EntryType
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.ui.components.BubblePosition
import com.chin.minddump.ui.components.BubbleRole
import com.chin.minddump.ui.components.DocumentChip
import com.chin.minddump.ui.components.GroupedMessageBubble
import com.chin.minddump.ui.GroupedEntry
import com.chin.minddump.ui.components.ZoomableAsyncImage
import com.chin.minddump.ui.theme.HapticPattern
import com.chin.minddump.ui.theme.LocalAnimationDuration
import com.chin.minddump.ui.theme.LocalMotionCurve
import com.chin.minddump.ui.theme.rememberPremiumHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ──────────────────────────────────────────────
// Entry List
// ──────────────────────────────────────────────

@Composable
fun EntryList(
    entries: List<MindDumpEntry>,
    onEntryClick: (MindDumpEntry) -> Unit,
    onEntryLongClick: (MindDumpEntry) -> Unit,
    modifier: Modifier = Modifier,
    isMultiSelectMode: Boolean = false,
    selectedEntries: Set<MindDumpEntry> = emptySet(),
    groupedEntries: List<GroupedEntry> = emptyList(),
) {
    // Use grouped entries if available, otherwise fall back to flat list
    val displayItems = if (groupedEntries.isNotEmpty()) {
        groupedEntries
    } else {
        entries.map { GroupedEntry(it, emptyList()) }
    }

    if (displayItems.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            EmptyStateDefault()
        }
        return
    }

    val animDuration = LocalAnimationDuration.current
    val curve = LocalMotionCurve.current.decelerate

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        reverseLayout = true,
    ) {
        items(
            items = displayItems,
            key = { it.entry.file.absolutePath },
            contentType = { it.entry.type.name },
        ) { grouped ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = tween(
                        durationMillis = animDuration.medium,
                        easing = curve,
                    ),
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = animDuration.medium,
                        easing = curve,
                    ),
                ),
                exit = fadeOut(
                    animationSpec = tween(durationMillis = animDuration.short),
                ) + shrinkVertically(
                    animationSpec = tween(durationMillis = animDuration.short),
                ),
            ) {
                GroupedEntryItem(
                    groupedEntry = grouped,
                    onClick = { onEntryClick(grouped.entry) },
                    onLongClick = { onEntryLongClick(grouped.entry) },
                    onCommentClick = { comment -> onEntryClick(comment) },
                    isMultiSelectMode = isMultiSelectMode,
                    isSelected = grouped.entry in selectedEntries,
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// Grouped Entry (file + nested comments)
// ──────────────────────────────────────────────

/**
 * Renders a file entry with its comments nested inside the same bubble.
 * Orphan comments (no parent file) get a special indicator.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupedEntryItem(
    groupedEntry: GroupedEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCommentClick: (MindDumpEntry) -> Unit,
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
) {
    val entry = groupedEntry.entry
    val comments = groupedEntry.comments
    val isOrphanComment = entry.role == EntryRole.COMMENT

    Column(modifier = Modifier.fillMaxWidth()) {
        // Main entry bubble
        EntryItem(
            entry = entry,
            onClick = onClick,
            onLongClick = onLongClick,
            isMultiSelectMode = isMultiSelectMode,
            isSelected = isSelected,
        )

        // Nested comments
        if (comments.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                comments.forEach { comment ->
                    CommentBubble(
                        comment = comment,
                        onClick = { onCommentClick(comment) },
                    )
                }
            }
        }

        // Orphan comment indicator
        if (isOrphanComment) {
            Text(
                text = "💬 原始文件已删除",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 12.dp, top = 2.dp),
            )
        }
    }
}

// ──────────────────────────────────────────────
// Comment Bubble
// ──────────────────────────────────────────────

@Composable
private fun CommentBubble(
    comment: MindDumpEntry,
    onClick: () -> Unit,
) {
    val haptics = rememberPremiumHaptics()

    GroupedMessageBubble(
        position = BubblePosition.SINGLE,
        role = BubbleRole.ASSISTANT,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    haptics.perform(HapticPattern.Tick)
                    onClick()
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 8.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Comment icon
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "评论",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = formatRelativeTimestamp(comment.monthFolder, comment.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Comment content
        val commentText by produceState(initialValue = comment.file.name, key1 = comment.file) {
            value = withContext(Dispatchers.IO) {
                try {
                    comment.file.readText().take(500)
                } catch (_: Exception) {
                    comment.file.name
                }
            }
        }

        Text(
            text = commentText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 10.dp),
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ──────────────────────────────────────────────
// Unified Entry Card
// ──────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryItem(
    entry: MindDumpEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
) {
    val haptics = rememberPremiumHaptics()

    GroupedMessageBubble(
        position = BubblePosition.SINGLE,
        role = BubbleRole.ASSISTANT,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isMultiSelectMode) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier.combinedClickable(
                        onClick = {
                            haptics.perform(HapticPattern.Tick)
                            onClick()
                        },
                        onLongClick = {
                            haptics.perform(HapticPattern.Buildup)
                            onLongClick()
                        },
                    )
                },
            ),
    ) {
        // Multi-select checkbox
        if (isMultiSelectMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 8.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                )
                Spacer(modifier = Modifier.width(8.dp))
                EntryCardHeader(entry, showLock = !isMultiSelectMode)
            }
        } else {
            // ── Header row: type icon avatar + timestamp ──
            EntryCardHeader(entry)
        }

        // ── Content area per type ──
        when (entry.type) {
            EntryType.TEXT -> TextEntryContent(entry)
            EntryType.PHOTO -> PhotoEntryContent(entry)
            EntryType.RECORDING -> AudioEntryContent(entry)
            EntryType.VIDEO -> VideoEntryContent(entry)
            else -> FileEntryContent(entry)
        }
    }
}

// ──────────────────────────────────────────────
// Header: type icon avatar + relative timestamp
// ──────────────────────────────────────────────

@Composable
private fun EntryCardHeader(entry: MindDumpEntry, showLock: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Circular type icon avatar
        val typeIcon = entry.type.toIcon()
        val typeColor = entry.type.toColor()

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(typeColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = typeIcon,
                contentDescription = entry.type.name,
                tint = typeColor,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Relative timestamp
        Text(
            text = formatRelativeTimestamp(entry.monthFolder, entry.timestamp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.weight(1f))

        // Lock icon for encrypted entries
        if (showLock && (entry.file.name.contains("enc") || entry.file.extension == "enc")) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}

// ──────────────────────────────────────────────
// Text entry
// ──────────────────────────────────────────────

@Composable
private fun TextEntryContent(entry: MindDumpEntry) {
    val textContent by produceState(initialValue = entry.file.name, key1 = entry.file) {
        value = withContext(Dispatchers.IO) {
            try {
                entry.file.readText().take(500)
            } catch (_: Exception) {
                entry.file.name
            }
        }
    }

    var expanded by remember { mutableStateOf(false) }
    val effectiveMaxLines = if (expanded) Int.MAX_VALUE else 3

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        Text(
            text = textContent,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = effectiveMaxLines,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (!expanded && textContent.length > 120) {
            Text(
                text = "展开",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

// ──────────────────────────────────────────────
// Photo entry: tap-to-zoom thumbnail
// ──────────────────────────────────────────────

@Composable
private fun PhotoEntryContent(entry: MindDumpEntry) {
    val innerShape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(bottom = 8.dp)
            .clip(innerShape)
            .height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        ZoomableAsyncImage(
            model = entry.file,
            contentDescription = stringResource(R.string.photo),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

// ──────────────────────────────────────────────
// Audio entry: document chip
// ──────────────────────────────────────────────

@Composable
private fun AudioEntryContent(entry: MindDumpEntry) {
    DocumentChip(
        fileName = entry.file.name,
        mimeType = "audio/mp4",
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(bottom = 8.dp),
    )
}

// ──────────────────────────────────────────────
// Video entry: thumbnail + play overlay
// ──────────────────────────────────────────────

@Composable
private fun VideoEntryContent(entry: MindDumpEntry) {
    val innerShape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(bottom = 8.dp)
            .clip(innerShape)
            .height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        ZoomableAsyncImage(
            model = entry.file,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // Play button overlay
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

// ──────────────────────────────────────────────
// File entry: file name + icon
// ──────────────────────────────────────────────

@Composable
private fun FileEntryContent(entry: MindDumpEntry) {
    DocumentChip(
        fileName = entry.file.name,
        mimeType = null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(bottom = 8.dp),
    )
}

// ──────────────────────────────────────────────
// Type icon & color mapping
// ──────────────────────────────────────────────

private fun EntryType.toIcon(): ImageVector = when (this) {
    EntryType.TEXT -> Icons.Filled.Edit
    EntryType.PHOTO -> Icons.Filled.PhotoCamera
    EntryType.RECORDING -> Icons.Filled.Mic
    EntryType.VIDEO -> Icons.Filled.Videocam
    EntryType.FILE -> Icons.AutoMirrored.Filled.InsertDriveFile
    EntryType.UNKNOWN -> Icons.Filled.HelpOutline
}

@Composable
private fun EntryType.toColor(): Color = when (this) {
    EntryType.TEXT -> MaterialTheme.colorScheme.primary
    EntryType.PHOTO -> MaterialTheme.colorScheme.tertiary
    EntryType.RECORDING -> MaterialTheme.colorScheme.secondary
    EntryType.VIDEO -> MaterialTheme.colorScheme.error
    EntryType.FILE -> MaterialTheme.colorScheme.onSurfaceVariant
    EntryType.UNKNOWN -> MaterialTheme.colorScheme.outline
}

// ──────────────────────────────────────────────
// Relative timestamp formatting
// ──────────────────────────────────────────────

/**
 * Format relative timestamp from monthFolder (YYYY-MM) + timestamp (yymm-dd-HHMMSS).
 */
private fun formatRelativeTimestamp(monthFolder: String, timestamp: String): String {
    // Timestamp format: yymm-dd-HHMMSS (e.g., 2506-13-143022)
    val entryDateTime = try {
        val tsFormat = DateTimeFormatter.ofPattern("yyMM-dd-HHmmss")
        LocalDateTime.parse(timestamp, tsFormat)
    } catch (_: Exception) {
        return "$monthFolder $timestamp"
    }

    val now = LocalDateTime.now()
    val duration = Duration.between(entryDateTime, now)

    return when {
        duration.toMinutes() < 1 -> "刚刚"
        duration.toHours() < 1 -> "${duration.toMinutes()}分钟前"
        duration.toDays() < 1 && entryDateTime.toLocalDate() == now.toLocalDate() -> {
            val fmt = DateTimeFormatter.ofPattern("今天 HH:mm")
            entryDateTime.format(fmt)
        }
        duration.toDays() < 2 -> {
            val fmt = DateTimeFormatter.ofPattern("昨天 HH:mm")
            entryDateTime.format(fmt)
        }
        entryDateTime.year == now.year -> {
            val fmt = DateTimeFormatter.ofPattern("M月d日 HH:mm")
            entryDateTime.format(fmt)
        }
        else -> {
            val fmt = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")
            entryDateTime.format(fmt)
        }
    }
}
