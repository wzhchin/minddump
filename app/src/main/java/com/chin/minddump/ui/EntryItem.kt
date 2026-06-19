package com.chin.minddump.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Notifications
import com.chin.minddump.storage.EventState
import com.chin.minddump.ui.components.nextEventDue
import com.chin.minddump.ui.formatFriendlyDateTime
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
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
import com.chin.minddump.ui.components.VideoPlayerDialog
import com.chin.minddump.ui.components.ZoomableAsyncImage
import com.chin.minddump.ui.theme.HapticPattern
import com.chin.minddump.ui.theme.LocalAnimationDuration
import com.chin.minddump.ui.theme.LocalExpressiveShapes
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
    val isPinned: Boolean

    data class Loose(
        val grouped: GroupedEntry
    ) : FeedItem {
        override val sortKey: Long get() = grouped.entry.file.lastModified()
        override val isPinned: Boolean get() = grouped.entry.isPinned
    }

    data class GroupCard(
        val summary: GroupSummary
    ) : FeedItem {
        override val sortKey: Long get() = summary.latestModified
        override val isPinned: Boolean
            get() = FileMetadata.fromFile(summary.groupDir)?.isPinned == true
    }
}

@Composable
fun EntryList(
    groupedEntries: List<GroupedEntry>,
    onEntryClick: (MindDumpEntry) -> Unit,
    onEntryLongClick: (MindDumpEntry) -> Unit,
    modifier: Modifier = Modifier,
    isMultiSelectMode: Boolean = false,
    selectedEntries: Set<MindDumpEntry> = emptySet(),
    groups: List<GroupSummary> = emptyList(),
    selectedGroups: Set<File> = emptySet(),
    onGroupClick: (File) -> Unit = {},
    onGroupLongClick: (File) -> Unit = {},
) {
    // The caller pre-scopes [groupedEntries] to the current view (the ViewModel's
    // already-grouped list, filtered to the current scope): at the root feed these
    // are the ungrouped entries (groupPath == null); inside a group they are that
    // group's direct members (groupPath == this dir). Either way every passed
    // entry is rendered as a loose item. Sub-group cards render alongside.
    val looseGrouped = groupedEntries.map { FeedItem.Loose(it) }
    val groupCards = groups.map { FeedItem.GroupCard(it) }

    // Newest-first, pinned-first: index 0 is the visual top (no reverseLayout),
    // so pinned + newest entries render above everything, like Twitter.
    val displayItems = (looseGrouped + groupCards).sortedWith(
        compareByDescending<FeedItem> { it.isPinned }
            .thenByDescending { it.sortKey },
    )

    if (displayItems.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            EmptyStateDefault()
        }
        return
    }

    // Item animation specs (hoisted so the animateItem calls stay within line
    // length and the specs are not re-created per item).
    val animDuration = LocalAnimationDuration.current
    val motionCurve = LocalMotionCurve.current
    val placementSpec: FiniteAnimationSpec<IntOffset> =
        tween(animDuration.medium, easing = motionCurve.decelerate)
    val fadeInSpec: FiniteAnimationSpec<Float> =
        tween(animDuration.medium, easing = motionCurve.decelerate)
    val fadeOutSpec: FiniteAnimationSpec<Float> = tween(animDuration.short)

    // Scroll-to-top (newest) when a fresh entry joins the feed, mirroring how
    // Twitter-like feeds surface new posts. Tracked against the previously seen
    // size so the initial composition and deletions don't trigger a jump.
    val listState = rememberLazyListState()
    val previousSize = remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(displayItems.size) {
        val prev = previousSize.value
        previousSize.value = displayItems.size
        if (prev != null && displayItems.size > prev) listState.animateScrollToItem(0)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
            // Idiomatic item-scoped animation: inserts/moves/removals animate
            // fluidly without re-firing an entrance on scroll. Replaces the old
            // per-item AnimatedVisibility(visible = true) wrapper whose exit spec
            // was dead and whose enter re-fired as items scrolled into view.
            when (item) {
                is FeedItem.Loose -> GroupedEntryItem(
                    groupedEntry = item.grouped,
                    onClick = { onEntryClick(item.grouped.entry) },
                    onLongClick = { onEntryLongClick(item.grouped.entry) },
                    onCommentClick = { comment -> onEntryClick(comment) },
                    isMultiSelectMode = isMultiSelectMode,
                    isSelected = item.grouped.entry in selectedEntries,
                    modifier = Modifier.animateItem(
                        placementSpec = placementSpec,
                        fadeInSpec = fadeInSpec,
                        fadeOutSpec = fadeOutSpec,
                    ),
                )

                is FeedItem.GroupCard -> GroupSummaryCard(
                    summary = item.summary,
                    onClick = { onGroupClick(item.summary.groupDir) },
                    onLongClick = { onGroupLongClick(item.summary.groupDir) },
                    isMultiSelectMode = isMultiSelectMode,
                    isSelected = item.summary.groupDir in selectedGroups,
                    modifier = Modifier.animateItem(
                        placementSpec = placementSpec,
                        fadeOutSpec = fadeOutSpec,
                    ),
                )
            }
        }
    }
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
    modifier: Modifier = Modifier,
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
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
        modifier = modifier,
    ) {
        // ── Media carousel preview (omitted when the group has no photos/videos) ──
        if (mediaMembers.isNotEmpty()) {
            GroupMediaCarousel(members = mediaMembers, interactable = !isMultiSelectMode)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Primary-colored dot — replaces the circular folder-icon avatar.
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
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
                MetaChip(
                    text = statusLabel(todoState),
                    containerColor = statusBadgeColor(todoState),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            if (isMultiSelectMode) {
                MultiSelectBadge(selected = isSelected)
                Spacer(modifier = Modifier.width(6.dp))
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
                    MetaChip(
                        text = "×$count",
                        containerColor = type.toColor().copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        leadingIcon = type.toIcon(),
                        iconTint = type.toColor(),
                    )
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
private fun GroupMediaCarousel(
    members: List<MindDumpEntry>,
    interactable: Boolean = true,
) {
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
        CarouselMediaTile(member = members[index], interactable = interactable)
    }
}

/**
 * One carousel tile. Photo tiles are decorative; video tiles open the in-app
 * player on tap. Each tile owns its own player-open state.
 */
@Composable
private fun CarouselMediaTile(
    member: MindDumpEntry,
    interactable: Boolean = true,
) {
    val isVideo = member.type == EntryType.VIDEO
    var showPlayer by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isVideo && interactable) {
                    Modifier.combinedClickable { showPlayer = true }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        coil3.compose.AsyncImage(
            model = member.file,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (isVideo) {
            VideoPlayOverlay()
        }
    }

    if (showPlayer) {
        VideoPlayerDialog(
            file = member.file,
            onDismissRequest = { showPlayer = false },
        )
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
    modifier: Modifier = Modifier,
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
        modifier = modifier,
    )
}

// ──────────────────────────────────────────────
// Comments — collapsed in-card list
// ──────────────────────────────────────────────

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
        val commentText by produceState<String?>(initialValue = null, key1 = comment.file) {
            value = withContext(Dispatchers.IO) {
                runCatching { comment.file.readText().take(500) }.getOrNull()
            }
        }
        Text(
            text = commentText ?: stringResource(R.string.content_loading),
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
    modifier: Modifier = Modifier,
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
        // The whole card is always interactive: outside multi-select it opens
        // the entry, inside multi-select the caller routes taps to selection.
        enabled = true,
        modifier = modifier,
        typeTint = entry.type.toColor(),
    ) {
        // ── Header + content (identical in and out of multi-select) ──
        // The per-type body always renders so the user can identify what they
        // are selecting. In multi-select a checkbox overlay reflects state.
        when (entry.type) {
            EntryType.PHOTO, EntryType.VIDEO -> {
                // Media-first hero: header floats over the edge-to-edge media.
                Box(modifier = Modifier.fillMaxWidth()) {
                    val openLong = {
                        haptics.perform(HapticPattern.Buildup)
                        onLongClick()
                    }
                    if (entry.type == EntryType.PHOTO) {
                        PhotoEntryContent(
                            entry,
                            onLongClick = openLong,
                            interactable = !isMultiSelectMode,
                        )
                    } else {
                        VideoEntryContent(
                            entry,
                            onLongClick = openLong,
                            interactable = !isMultiSelectMode,
                        )
                    }
                    EntryCardHeader(entry, floating = true)
                    if (isMultiSelectMode) {
                        MultiSelectBadge(
                            selected = isSelected,
                            modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                        )
                    }
                }
            }
            else -> {
                EntryCardHeader(entry, floating = false)
                when (entry.type) {
                    EntryType.TEXT -> TextEntryContent(entry)
                    EntryType.RECORDING -> AudioEntryContent(entry)
                    else -> FileEntryContent(entry)
                }
                if (isMultiSelectMode) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        MultiSelectBadge(selected = isSelected)
                    }
                }
            }
        }

        // ── Tags + reminder + comments footer (omitted in multi-select / when empty) ──
        val hasFooter = !isMultiSelectMode &&
            (entry.tags.isNotEmpty() || entry.events.isNotEmpty() || comments.isNotEmpty())
        if (hasFooter) {
            Spacer(modifier = Modifier.height(4.dp))
            EntryCardMetaFooter(
                entry = entry,
                comments = comments,
                onCommentClick = onCommentClick,
            )
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

/**
 * Selection badge shown over/inside a card while in multi-select. A filled
 * circle with a check when selected, an outlined circle otherwise. Read-only —
 * the enclosing card owns the tap that toggles selection.
 */
@Composable
private fun MultiSelectBadge(selected: Boolean, modifier: Modifier = Modifier) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier.size(24.dp),
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.padding(4.dp),
            )
        }
    }
}

@Composable
private fun EntryCardHeader(
    entry: MindDumpEntry,
    showLock: Boolean = true,
    floating: Boolean = false,
) {
    val typeColor = entry.type.toColor()
    val headerRow: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Type-colored dot — replaces the circular type-icon avatar.
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(typeColor),
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Relative timestamp
            Text(
                text = formatRelativeTimestamp(entry.monthFolder, entry.timestamp),
                style = MaterialTheme.typography.labelMedium,
                color = if (floating) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            Spacer(modifier = Modifier.weight(1f))

            // Right-aligned indicator cluster: pin + status + lock.
            IndicatorCluster(entry = entry, showLock = showLock, floating = floating)
        }
    }

    if (floating) {
        // Floating chip overlaying the media hero, with a legibility scrim.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f),
                        1f to androidx.compose.ui.graphics.Color.Transparent,
                    ),
                ),
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                headerRow()
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            headerRow()
        }
    }
}

/** Right-aligned pin / status / lock cluster shared by the header variants. */
@Composable
private fun IndicatorCluster(
    entry: MindDumpEntry,
    showLock: Boolean,
    floating: Boolean,
) {
    val onSurface = if (floating) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Pin indicator for pinned entries
    if (entry.isPinned) {
        Icon(
            imageVector = Icons.Filled.PushPin,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (floating) onSurface else MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(4.dp))
    }

    // Todo status badge for statused entries
    if (entry.todoState != TodoState.NONE) {
        val isClosed = entry.todoState == TodoState.DONE || entry.todoState == TodoState.CANCEL
        MetaChip(
            text = statusLabel(entry.todoState),
            containerColor = if (floating) {
                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.25f)
            } else {
                statusBadgeColor(entry.todoState)
            },
            contentColor = if (floating) {
                onSurface.copy(alpha = if (isClosed) 0.7f else 1f)
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer.copy(
                    alpha = if (isClosed) 0.6f else 1f,
                )
            },
        )
        Spacer(modifier = Modifier.width(4.dp))
    }

    // Lock icon for encrypted entries. Drive this from the parsed metadata
    // property rather than a filename substring sniff — robust to renames.
    val isEncrypted = FileMetadata.fromFile(entry.file)?.isEncrypted == true
    if (showLock && isEncrypted) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = onSurface.copy(alpha = if (floating) 0.85f else 0.5f),
        )
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
    // Load lazily off the main thread. Initial value is null so we can show a
    // localized loading placeholder instead of flashing the raw file name.
    val textContent by produceState<String?>(initialValue = null, key1 = entry.file) {
        value = withContext(Dispatchers.IO) {
            runCatching { entry.file.readText().take(500) }
                .getOrNull()
        }
    }

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        when (val content = textContent) {
            null -> Text(
                text = stringResource(R.string.content_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )

            else -> {
                val isLong = content.length > 120
                val effectiveMaxLines = if (expanded || !isLong) Int.MAX_VALUE else 3

                // animateContentSize smoothly grows/shrinks the body as maxLines
                // changes, giving a real expand/collapse transition.
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = effectiveMaxLines,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.animateContentSize(
                        animationSpec = tween(
                            durationMillis = LocalAnimationDuration.current.medium,
                            easing = LocalMotionCurve.current.decelerate,
                        ),
                    ),
                )

                if (isLong) {
                    // Owns its own click: toggles expand/collapse WITHOUT opening
                    // the entry (the gesture is consumed here, not propagated to
                    // the card's tap-to-open). Tapping the body text still opens.
                    Text(
                        text = stringResource(
                            if (expanded) R.string.action_collapse else R.string.action_expand,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable { expanded = !expanded },
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Photo entry: tap-to-zoom thumbnail
// ──────────────────────────────────────────────

@Composable
private fun PhotoEntryContent(
    entry: MindDumpEntry,
    onLongClick: () -> Unit = {},
    interactable: Boolean = true,
) {
    MediaHeroClip { clip ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(clip),
            contentAlignment = Alignment.Center,
        ) {
            if (interactable) {
                ZoomableAsyncImage(
                    model = entry.file,
                    contentDescription = stringResource(R.string.photo),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onLongClick = onLongClick,
                )
            } else {
                coil3.compose.AsyncImage(
                    model = entry.file,
                    contentDescription = stringResource(R.string.photo),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
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

/**
 * Shared translucent play-button overlay, centered over a video tile. One size
 * spec (48dp circle / 28dp icon) used by both the entry-card video body and the
 * group-carousel video tile — previously two slightly different sizes.
 */
@Composable
private fun VideoPlayOverlay() {
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

@Composable
private fun VideoEntryContent(
    entry: MindDumpEntry,
    onLongClick: () -> Unit = {},
    interactable: Boolean = true,
) {
    var showPlayer by remember { mutableStateOf(false) }
    MediaHeroClip { clip ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(clip)
                .then(
                    if (interactable) {
                        Modifier.combinedClickable(
                            onClick = { showPlayer = true },
                            onLongClick = onLongClick,
                        )
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Plain thumbnail (no built-in tap-to-zoom): tapping opens the video player.
            coil3.compose.AsyncImage(
                model = entry.file,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            VideoPlayOverlay()
        }
    }
    if (showPlayer) {
        VideoPlayerDialog(
            file = entry.file,
            onDismissRequest = { showPlayer = false },
        )
    }
}

/**
 * Provides the edge-to-edge clip shape (card top corners only) for media hero
 * regions, so photo/video media bleeds under the floating header chip.
 */
@Composable
private fun MediaHeroClip(content: @Composable (androidx.compose.ui.graphics.Shape) -> Unit) {
    val shapes = LocalExpressiveShapes.current
    val topStart =
        (shapes.entryCard as? androidx.compose.foundation.shape.RoundedCornerShape)
            ?.topStart ?: androidx.compose.foundation.shape.CornerSize(0.dp)
    val clip = RoundedCornerShape(
        topStart = topStart,
        topEnd = topStart,
        bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp),
        bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp),
    )
    content(clip)
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
    // VIDEO is a non-error category: blend secondary + tertiary into a distinct
    // theme-derived accent instead of reusing the error token (which is reserved
    // for genuine error/urgent states like active recording).
    EntryType.VIDEO -> lerp(
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        0.5f,
    )

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

// ──────────────────────────────────────────────
// Card footer: tags + reminder at a glance
// ──────────────────────────────────────────────

// ──────────────────────────────────────────────
// Unified meta chip
// ──────────────────────────────────────────────

private val META_CHIP_SHAPE = RoundedCornerShape(8.dp)
private val META_CHIP_ICON_SIZE = 14.dp

/**
 * The single chip style used for every outer-card meta indicator: tags,
 * reminders, comment counts, todo badges, and group type counts. One shape,
 * one padding, one typography, one icon size — color still carries semantics.
 *
 * Pass [leadingIcon] for chips with an icon (reminder, comment, type count);
 * leave it `null` for text-only chips (tags, todo status text).
 */
@Composable
private fun MetaChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    iconTint: Color = contentColor,
) {
    Surface(
        shape = META_CHIP_SHAPE,
        color = containerColor,
        modifier = modifier,
    ) {
        if (leadingIcon == null) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(META_CHIP_ICON_SIZE),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor,
                )
            }
        }
    }
}

/**
 * Read-only summary of an entry's sidecar metadata, rendered as a wrap of tonal
 * chips below the card body. Tags become `#tag` chips; the soonest pending (or,
 * when all fired, the most-recent fired) event becomes a bell chip.
 */
@Composable
private fun EntryCardMetaFooter(
    entry: MindDumpEntry,
    comments: List<MindDumpEntry>,
    onCommentClick: (MindDumpEntry) -> Unit,
) {
    val reminderDue = nextEventDue(entry.events)
    val reminderEvent = remember(entry.events, reminderDue) {
        if (reminderDue == null) null
        else entry.events.firstOrNull { it.due == reminderDue }
    }
    var commentsExpanded by remember { mutableStateOf(false) }
    val haptics = rememberPremiumHaptics()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            entry.tags.forEach { tag ->
                MetaChip(
                    text = "#$tag",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            if (reminderEvent != null) {
                val isFired = reminderEvent.state == EventState.FIRED
                val label = buildString {
                    append(formatFriendlyDateTime(reminderEvent.due))
                    if (isFired) {
                        append(" · ")
                        append(stringResource(R.string.event_fired))
                    }
                }
                MetaChip(
                    text = label,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                        alpha = if (isFired) 0.4f else 0.7f,
                    ),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                        alpha = if (isFired) 0.6f else 1f,
                    ),
                    leadingIcon = Icons.Filled.Notifications,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                        alpha = if (isFired) 0.5f else 1f,
                    ),
                )
            }
            if (comments.isNotEmpty()) {
                val label = if (commentsExpanded) {
                    stringResource(R.string.comments_collapse)
                } else {
                    stringResource(R.string.comments_expand, comments.size)
                }
                MetaChip(
                    text = label,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.primary,
                    leadingIcon = Icons.Filled.Edit,
                    iconTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.clickable {
                        haptics.perform(HapticPattern.Tick)
                        commentsExpanded = !commentsExpanded
                    },
                )
            }
        }

        // Expanded comment previews live directly under the chip row so the
        // whole meta footer is one self-contained block.
        AnimatedVisibility(visible = commentsExpanded && comments.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
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
