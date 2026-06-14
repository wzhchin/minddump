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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.chin.minddump.storage.FileMetadata
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.storage.TodoState
import com.chin.minddump.ui.components.DocumentChip
import com.chin.minddump.ui.components.EntryCard
import com.chin.minddump.ui.components.statusLabel
import com.chin.minddump.ui.GroupedEntry
import com.chin.minddump.ui.GroupSummary
import com.chin.minddump.ui.components.ZoomableAsyncImage
import com.chin.minddump.ui.theme.HapticPattern
import com.chin.minddump.ui.theme.LocalAnimationDuration
import com.chin.minddump.ui.theme.LocalMotionCurve
import com.chin.minddump.ui.theme.rememberPremiumHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ──────────────────────────────────────────────
// Entry List
// ──────────────────────────────────────────────

/**
 * One row in the time-ordered feed: either a loose entry (with its nested comments)
 * or a group summary card.
 */
sealed interface FeedItem {
    val sortKey: Long

    data class Loose(
        val grouped: GroupedEntry
    ) : FeedItem {
        override val sortKey: Long get() = grouped.entry.file.lastModified()
    }

    data class GroupCard(
        val summary: GroupSummary
    ) : FeedItem {
        override val sortKey: Long get() = summary.latestModified
    }
}

@Composable
fun EntryList(
    entries: List<MindDumpEntry>,
    onEntryClick: (MindDumpEntry) -> Unit,
    onEntryLongClick: (MindDumpEntry) -> Unit,
    modifier: Modifier = Modifier,
    isMultiSelectMode: Boolean = false,
    selectedEntries: Set<MindDumpEntry> = emptySet(),
    groups: List<GroupSummary> = emptyList(),
    onGroupClick: (File) -> Unit = {},
    onGroupLongClick: (File) -> Unit = {},
) {
    // The caller pre-scopes [entries] to the current view: at the root feed these
    // are the ungrouped entries (groupPath == null); inside a group they are that
    // group's direct members (groupPath == this dir). Either way every passed
    // entry is rendered as a loose item. Sub-group cards render alongside.
    val looseGrouped = groupEntriesForRender(entries)
        .map { FeedItem.Loose(it) }
    val groupCards = groups.map { FeedItem.GroupCard(it) }

    val displayItems = (looseGrouped + groupCards)
        .sortedByDescending { it.sortKey }

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
            key = { item ->
                when (item) {
                    is FeedItem.Loose -> "entry:${item.grouped.entry.file.absolutePath}"
                    is FeedItem.GroupCard -> "group:${item.summary.groupDir.absolutePath}"
                }
            },
            contentType = { item ->
                when (item) {
                    is FeedItem.Loose -> "entry:${item.grouped.entry.type.name}"
                    is FeedItem.GroupCard -> "group"
                }
            },
        ) { item ->
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
                when (item) {
                    is FeedItem.Loose -> GroupedEntryItem(
                        groupedEntry = item.grouped,
                        onClick = { onEntryClick(item.grouped.entry) },
                        onLongClick = { onEntryLongClick(item.grouped.entry) },
                        onCommentClick = { comment -> onEntryClick(comment) },
                        isMultiSelectMode = isMultiSelectMode,
                        isSelected = item.grouped.entry in selectedEntries,
                    )

                    is FeedItem.GroupCard -> GroupSummaryCard(
                        summary = item.summary,
                        onClick = { onGroupClick(item.summary.groupDir) },
                        onLongClick = { onGroupLongClick(item.summary.groupDir) },
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Comment nesting helper (render-time)
// ──────────────────────────────────────────────

/**
 * Nest comments under their target file entry for rendering.
 * Mirrors the ViewModel logic so the list can group loose entries' comments.
 */
private fun groupEntriesForRender(entries: List<MindDumpEntry>): List<GroupedEntry> {
    val files = entries.filter { it.role == EntryRole.FILE }
    val comments = entries.filter { it.role == EntryRole.COMMENT }
    val result = mutableListOf<GroupedEntry>()
    val matched = mutableSetOf<MindDumpEntry>()

    for (file in files) {
        val fileComments = comments.filter { it.targetTimestamp == file.timestamp && it !in matched }
        matched.addAll(fileComments)
        result.add(GroupedEntry(entry = file, comments = fileComments))
    }
    for (comment in comments) {
        if (comment !in matched) result.add(GroupedEntry(entry = comment, comments = emptyList()))
    }
    return result
}

// ──────────────────────────────────────────────
// Group Summary Card
// ──────────────────────────────────────────────

/**
 * Renders a group as a summary card: a horizontal media carousel previewing the
 * group's photo/video members (omitted when there are none), then a folder icon,
 * name, member count, and a row of the distinct entry types found inside (with
 * counts). Tap opens the group detail; long-press opens the group action menu.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GroupSummaryCard(
    summary: GroupSummary,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val haptics = rememberPremiumHaptics()
    val typeCounts = summary.memberEntries.groupingBy { it.type }.eachCount()
    val mediaMembers = summary.memberEntries
        .filter { it.type == EntryType.PHOTO || it.type == EntryType.VIDEO }
        .sortedByDescending { it.file.lastModified() }
    val groupMeta = FileMetadata.fromFile(summary.groupDir)
    val isPinned = groupMeta?.isPinned == true
    val todoState = groupMeta?.todoState ?: TodoState.NONE

    EntryCard(
        onClick = {
            haptics.perform(HapticPattern.Tick)
            onClick()
        },
        onLongClick = {
            haptics.perform(HapticPattern.Buildup)
            onLongClick()
        },
    ) {
        // ── Media carousel preview (omitted when the group has no photos/videos) ──
        if (mediaMembers.isNotEmpty()) {
            GroupMediaCarousel(members = mediaMembers)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = stringResource(R.string.group_media_preview),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = summary.name.ifBlank { stringResource(R.string.group_unnamed) },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // Pin indicator for pinned groups
            if (isPinned) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            // Todo status badge for statused groups
            if (todoState != TodoState.NONE) {
                Surface(
                    shape = CircleShape,
                    color = statusBadgeColor(todoState),
                ) {
                    Text(
                        text = statusLabel(todoState),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = stringResource(R.string.group_member_count, summary.memberEntries.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Type chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (typeCounts.isEmpty()) {
                Text(
                    text = stringResource(R.string.group_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            } else {
                typeCounts.forEach { (type, count) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = type.toIcon(),
                            contentDescription = type.name,
                            tint = type.toColor(),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "×$count",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Horizontal browsable carousel of a group's photo/video members, mirroring the
 * Material 3 Expressive card-preview pattern. Each tile is a large rounded media
 * thumbnail; videos get a play overlay.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupMediaCarousel(members: List<MindDumpEntry>) {
    val carouselState = rememberCarouselState { members.size }

    HorizontalMultiBrowseCarousel(
        state = carouselState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .height(180.dp),
        preferredItemWidth = 180.dp,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) { index ->
        val member = members[index]
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            coil3.compose.AsyncImage(
                model = member.file,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (member.type == EntryType.VIDEO) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}

/**
 * Renders a file entry as a single card with its comments folded inside it (an
 * expandable list). Orphan comments — whose parent file was deleted — get a
 * special indicator inside the card.
 */
@Composable
fun GroupedEntryItem(
    groupedEntry: GroupedEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCommentClick: (MindDumpEntry) -> Unit,
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
) {
    EntryItem(
        entry = groupedEntry.entry,
        onClick = onClick,
        onLongClick = onLongClick,
        isMultiSelectMode = isMultiSelectMode,
        isSelected = isSelected,
        comments = groupedEntry.comments,
        onCommentClick = onCommentClick,
        isOrphanComment = groupedEntry.entry.role == EntryRole.COMMENT,
    )
}

// ──────────────────────────────────────────────
// Comments — collapsed in-card list
// ──────────────────────────────────────────────

/**
 * Renders comments as a collapsed, expandable list inside the parent entry card:
 * an affordance summarizing the count, which expands to timestamped content
 * previews. Tapping a preview opens that comment.
 */
@Composable
private fun CommentListSection(
    comments: List<MindDumpEntry>,
    onCommentClick: (MindDumpEntry) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val haptics = rememberPremiumHaptics()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        // Expand/collapse affordance
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        haptics.perform(HapticPattern.Tick)
                        expanded = !expanded
                    },
                ).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(R.string.comment_label),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (expanded) {
                    stringResource(R.string.comments_collapse)
                } else {
                    stringResource(R.string.comments_expand, comments.size)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Expanded previews
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                comments.forEach { comment ->
                    CommentPreview(
                        comment = comment,
                        onClick = {
                            haptics.perform(HapticPattern.Tick)
                            onCommentClick(comment)
                        },
                    )
                }
            }
        }
    }
}

/**
 * One expanded comment: a timestamp + a few-line content preview. Tapping opens it.
 */
@Composable
private fun CommentPreview(
    comment: MindDumpEntry,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = formatRelativeTimestamp(comment.monthFolder, comment.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp),
        )
        val commentText by produceState(initialValue = comment.file.name, key1 = comment.file) {
            value = withContext(Dispatchers.IO) {
                runCatching { comment.file.readText().take(500) }
                    .getOrDefault(comment.file.name)
            }
        }
        Text(
            text = commentText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f),
            maxLines = 3,
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
    comments: List<MindDumpEntry> = emptyList(),
    onCommentClick: (MindDumpEntry) -> Unit = {},
    isOrphanComment: Boolean = false,
) {
    val haptics = rememberPremiumHaptics()

    EntryCard(
        onClick = {
            haptics.perform(HapticPattern.Tick)
            onClick()
        },
        onLongClick = {
            haptics.perform(HapticPattern.Buildup)
            onLongClick()
        },
        // In multi-select the whole card toggles selection on tap.
        enabled = !isMultiSelectMode,
    ) {
        // Multi-select checkbox
        if (isMultiSelectMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 12.dp, end = 12.dp)
                    .combinedClickable(
                        onClick = {
                            haptics.perform(HapticPattern.Tick)
                            onClick()
                        },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                )
                Spacer(modifier = Modifier.width(8.dp))
                EntryCardHeader(entry, showLock = false)
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

        // ── In-card collapsed comments (omitted in multi-select) ──
        if (!isMultiSelectMode && comments.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            CommentListSection(comments = comments, onCommentClick = onCommentClick)
        }

        // ── Orphan-comment indicator (its parent file was deleted) ──
        if (!isMultiSelectMode && isOrphanComment) {
            Text(
                text = stringResource(R.string.orphan_comment),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
            )
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

        // Pin indicator for pinned entries
        if (entry.isPinned) {
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        // Todo status badge for statused entries
        if (entry.todoState != TodoState.NONE) {
            val isClosed = entry.todoState == TodoState.DONE || entry.todoState == TodoState.CANCEL
            Surface(
                shape = CircleShape,
                color = statusBadgeColor(entry.todoState),
            ) {
                Text(
                    text = statusLabel(entry.todoState),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                        alpha = if (isClosed) 0.6f else 1f,
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

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

/** Background tone for a todo status badge, reflecting open vs closed states. */
@Composable
private fun statusBadgeColor(state: TodoState): Color =
    when (state) {
        TodoState.DONE, TodoState.CANCEL ->
            MaterialTheme.colorScheme.surfaceContainerHighest
        TodoState.TODO ->
            MaterialTheme.colorScheme.tertiaryContainer
        TodoState.DOING ->
            MaterialTheme.colorScheme.primaryContainer
        TodoState.WAIT ->
            MaterialTheme.colorScheme.secondaryContainer
        TodoState.NONE ->
            MaterialTheme.colorScheme.surfaceContainerHighest
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
    EntryType.UNKNOWN -> Icons.AutoMirrored.Filled.HelpOutline
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
