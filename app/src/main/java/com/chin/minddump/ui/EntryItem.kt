package com.chin.minddump.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import com.chin.minddump.storage.EntryType
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.ui.components.BubblePosition
import com.chin.minddump.ui.components.BubbleRole
import com.chin.minddump.ui.components.DocumentChip
import com.chin.minddump.ui.components.GroupedMessageBubble
import com.chin.minddump.ui.components.ZoomableAsyncImage
import com.chin.minddump.ui.theme.HapticPattern
import com.chin.minddump.ui.theme.LocalAnimationDuration
import com.chin.minddump.ui.theme.LocalMotionCurve
import com.chin.minddump.ui.theme.rememberPremiumHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
) {
    if (entries.isEmpty()) {
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
            items = entries,
            key = { it.file.absolutePath },
            contentType = { it.type.name },
        ) { entry ->
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
                EntryItem(
                    entry = entry,
                    onClick = { onEntryClick(entry) },
                    onLongClick = { onEntryLongClick(entry) },
                )
            }
        }
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
                onLongClick = {
                    haptics.perform(HapticPattern.Buildup)
                    onLongClick()
                },
            ),
    ) {
        // ── Header row: type icon avatar + timestamp ──
        EntryCardHeader(entry)

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
private fun EntryCardHeader(entry: MindDumpEntry) {
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
            text = formatRelativeTimestamp(entry.dateFolder, entry.timestamp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.weight(1f))

        // Lock icon for encrypted entries
        if (entry.file.name.contains("enc") || entry.file.extension == "enc") {
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

private fun formatRelativeTimestamp(dateFolder: String, timestamp: String): String {
    val entryDate = try {
        LocalDate.parse(dateFolder)
    } catch (_: Exception) {
        return "$dateFolder $timestamp"
    }

    val entryTime = try {
        val inputFormat = DateTimeFormatter.ofPattern("HHmmss")
        LocalTime.parse(timestamp, inputFormat)
    } catch (_: Exception) {
        return dateFolder
    }

    val entryDateTime = LocalDateTime.of(entryDate, entryTime)
    val now = LocalDateTime.now()
    val duration = Duration.between(entryDateTime, now)

    return when {
        duration.toMinutes() < 1 -> "刚刚"
        duration.toHours() < 1 -> "${duration.toMinutes()}分钟前"
        duration.toDays() < 1 && entryDate == now.toLocalDate() -> {
            val fmt = DateTimeFormatter.ofPattern("今天 HH:mm")
            entryDateTime.format(fmt)
        }
        duration.toDays() < 2 -> {
            val fmt = DateTimeFormatter.ofPattern("昨天 HH:mm")
            entryDateTime.format(fmt)
        }
        entryDate.year == now.year -> {
            val fmt = DateTimeFormatter.ofPattern("M月d日 HH:mm")
            entryDateTime.format(fmt)
        }
        else -> {
            val fmt = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")
            entryDateTime.format(fmt)
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${bytes / (1024 * 1024)} MB"
}
