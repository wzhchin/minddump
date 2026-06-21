package com.chin.minddump.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chin.minddump.R
import com.chin.minddump.storage.EntryType
import com.chin.minddump.storage.FileMetadata
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.storage.TodoState
import com.chin.minddump.ui.components.EntryCard
import com.chin.minddump.ui.components.AudioRecordingContent
import com.chin.minddump.ui.components.statusLabel
import com.chin.minddump.ui.components.nextEventDue
import com.chin.minddump.ui.components.VideoPlayerDialog
import com.chin.minddump.ui.components.ZoomableAsyncImage
import com.chin.minddump.ui.formatFriendlyDateTime
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
// Card scaffold constants — the single source of truth for the unified
// head · body · foot layout shared by every card type (m3e-restrained-cards).
// ──────────────────────────────────────────────
private val CARD_INSET = 16.dp
private val CARD_RHYTHM = 12.dp

// ──────────────────────────────────────────────
// Entry List
// ──────────────────────────────────────────────

/**
 * One row in the time-ordered feed: either a loose entry or a group summary card.
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
 * Renders a group on the unified scaffold: header (dot · Collection · time, with
 * pin/status cluster), body (bold name + byline + media strip preview), and a
 * foot of type-count chips. Tap opens the group detail; long-press opens the
 * group action menu.
 */
@OptIn(ExperimentalFoundationApi::class)
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
    val selectedOutline = if (isSelected && isMultiSelectMode) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, LocalExpressiveShapes.current.entryCard)
    } else {
        Modifier
    }

    EntryCard(
        onClick = {
            haptics.perform(HapticPattern.Tick)
            onClick()
        },
        onLongClick = {
            haptics.perform(HapticPattern.Buildup)
            onLongClick()
        },
        modifier = modifier.then(selectedOutline),
    ) {
        // ── HEAD: folder icon · time  +  cluster (pin / status) ──
        EntryCardHeader(
            typeColor = MaterialTheme.colorScheme.primary,
            kindIcon = Icons.Filled.CreateNewFolder,
            timestamp = formatRelativeEpoch(summary.latestModified),
            isPinned = isPinned,
            todoState = todoState,
            isMultiSelectMode = isMultiSelectMode,
            isSelected = isSelected,
            showLock = false,
            isEncrypted = false,
        )

        // ── BODY: bold name + byline + media strip ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = CARD_INSET, end = CARD_INSET, bottom = CARD_RHYTHM),
        ) {
            Text(
                text = summary.name.ifBlank { stringResource(R.string.group_unnamed) },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.group_member_count, summary.memberEntries.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        if (mediaMembers.isNotEmpty()) {
            GroupMediaCarousel(members = mediaMembers, interactable = !isMultiSelectMode)
        }

        // ── FOOT: type-count chips (omitted in multi-select) ──
        if (!isMultiSelectMode) {
            CardChipRow(
                modifier = Modifier.padding(start = CARD_INSET, end = CARD_INSET, bottom = CARD_INSET),
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
}

/**
 * Horizontal browsable carousel of a group's photo/video members, mirroring the
 * Material 3 Expressive card-preview pattern. Each tile is a large rounded media
 * thumbnail; videos get a play overlay.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
 * Renders a file entry as a single card (head · body · foot zones).
 */
@Composable
fun GroupedEntryItem(
    groupedEntry: GroupedEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
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
        modifier = modifier,
    )
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
    modifier: Modifier = Modifier,
) {
    val haptics = rememberPremiumHaptics()
    val typeColor = entry.type.toColor()
    val selectedOutline = if (isSelected && isMultiSelectMode) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, LocalExpressiveShapes.current.entryCard)
    } else {
        Modifier
    }

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
        modifier = modifier.then(selectedOutline),
        typeTint = typeColor,
    ) {
        // ── HEAD (identical for every type) ──
        // The header always sits as a solid top row, even over media. The
        // multi-select check lives in the dateline's first slot.
        val isEncrypted = FileMetadata.fromFile(entry.file)?.isEncrypted == true
        EntryCardHeader(
            typeColor = typeColor,
            kindIcon = entry.type.toIcon(),
            timestamp = formatRelativeTimestamp(entry.monthFolder, entry.timestamp),
            isPinned = entry.isPinned,
            todoState = entry.todoState,
            isMultiSelectMode = isMultiSelectMode,
            isSelected = isSelected,
            showLock = true,
            isEncrypted = isEncrypted,
        )

        // ── BODY (only this zone varies by type) ──
        when (entry.type) {
            EntryType.PHOTO -> PhotoEntryContent(
                entry,
                onLongClick = {
                    haptics.perform(HapticPattern.Buildup)
                    onLongClick()
                },
                interactable = !isMultiSelectMode,
            )
            EntryType.VIDEO -> VideoEntryContent(
                entry,
                onLongClick = {
                    haptics.perform(HapticPattern.Buildup)
                    onLongClick()
                },
                interactable = !isMultiSelectMode,
            )
            EntryType.TEXT -> TextEntryContent(entry)
            EntryType.RECORDING -> AudioRecordingContent(entry, interactable = !isMultiSelectMode)
            else -> FileEntryContent(entry)
        }

        // ── FOOT (tags + reminder; omitted in multi-select / when empty) ──
        val hasFooter = !isMultiSelectMode &&
            (entry.tags.isNotEmpty() || entry.events.isNotEmpty())
        if (hasFooter) {
            EntryCardMetaFooter(entry = entry)
        }
    }
}

// ──────────────────────────────────────────────
// Unified header — solid top row on every card type
// ──────────────────────────────────────────────

/**
 * Selection affordance shown in the header dateline's first slot during
 * multi-select. A filled primary circle with a check when selected, an outlined
 * ring otherwise. Read-only — the enclosing card owns the tap that toggles
 * selection. Lives in normal flow (first slot of the dateline), so it can never
 * overlap body or footer content on any card type.
 */
@Composable
private fun MultiSelectBadge(selected: Boolean, modifier: Modifier = Modifier) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.outline
    }
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content,
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline,
        ),
        modifier = modifier.size(22.dp),
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.padding(3.dp),
            )
        }
    }
}

/**
 * The single header layout used by every card type (m3e-restrained-cards).
 * A solid top row on the card surface: left dateline (multi-select check OR
 * type icon · relative time), right cluster (pin · status · lock).
 * No floating overlay, no scrim — media renders below this row.
 */
@Composable
private fun EntryCardHeader(
    typeColor: Color,
    kindIcon: ImageVector,
    timestamp: String,
    isPinned: Boolean,
    todoState: TodoState,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    showLock: Boolean,
    isEncrypted: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = CARD_INSET, top = 14.dp, end = CARD_INSET, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Dateline first slot: in multi-select the check replaces the type icon.
        if (isMultiSelectMode) {
            MultiSelectBadge(selected = isSelected)
        } else {
            Icon(
                imageVector = kindIcon,
                contentDescription = null,
                tint = typeColor,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(9.dp))

        // Relative timestamp.
        Text(
            text = timestamp,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.weight(1f))

        // Right-aligned cluster: pin + status + lock.
        IndicatorCluster(
            isPinned = isPinned,
            todoState = todoState,
            showLock = showLock,
            isEncrypted = isEncrypted,
        )
    }
}

/** Right-aligned pin / status / lock cluster. */
@Composable
private fun IndicatorCluster(
    isPinned: Boolean,
    todoState: TodoState,
    showLock: Boolean,
    isEncrypted: Boolean,
) {
    // Pin indicator — primaryContainer rounded square.
    if (isPinned) {
        Surface(
            shape = RoundedCornerShape(9.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = null,
                modifier = Modifier
                    .padding(6.dp)
                    .size(15.dp),
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
    }

    // Todo status badge — tonal pill per status; DONE/CANCEL struck-through.
    if (todoState != TodoState.NONE) {
        val isClosed = todoState == TodoState.DONE || todoState == TodoState.CANCEL
        MetaChip(
            text = statusLabel(todoState),
            containerColor = statusBadgeColor(todoState),
            contentColor = statusBadgeContentColor(todoState),
            textDecoration = if (isClosed) TextDecoration.LineThrough else TextDecoration.None,
        )
        Spacer(modifier = Modifier.width(6.dp))
    }

    // Lock indicator — surfaceContainerHigh tile.
    if (showLock && isEncrypted) {
        Surface(
            shape = RoundedCornerShape(7.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier
                    .padding(5.dp)
                    .size(14.dp),
            )
        }
    }
}

/** Background tone for a todo status badge, reflecting open vs closed states. */
@Composable
private fun statusBadgeColor(state: TodoState): Color =
    when (state) {
        TodoState.DONE -> MaterialTheme.colorScheme.surfaceContainerHighest
        TodoState.CANCEL -> MaterialTheme.colorScheme.surfaceContainerHighest
        TodoState.TODO -> MaterialTheme.colorScheme.tertiaryContainer
        TodoState.DOING -> MaterialTheme.colorScheme.primaryContainer
        TodoState.WAIT -> MaterialTheme.colorScheme.secondaryContainer
        TodoState.NONE -> MaterialTheme.colorScheme.surfaceContainerHighest
    }

/** Foreground tone for a todo status badge. */
@Composable
private fun statusBadgeContentColor(state: TodoState): Color =
    when (state) {
        TodoState.DONE, TodoState.CANCEL ->
            MaterialTheme.colorScheme.onSurfaceVariant
        TodoState.TODO -> MaterialTheme.colorScheme.onTertiaryContainer
        TodoState.DOING -> MaterialTheme.colorScheme.onPrimaryContainer
        TodoState.WAIT -> MaterialTheme.colorScheme.onSecondaryContainer
        TodoState.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
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
            .padding(start = CARD_INSET, end = CARD_INSET, bottom = CARD_RHYTHM),
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
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable { expanded = !expanded },
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Photo entry: edge-to-edge body below the header, tap-to-zoom thumbnail
// ──────────────────────────────────────────────

@Composable
private fun PhotoEntryContent(
    entry: MindDumpEntry,
    onLongClick: () -> Unit = {},
    interactable: Boolean = true,
) {
    val bodyTopShape = bodyTopClip()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(bodyTopShape)
            .padding(bottom = CARD_RHYTHM),
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

// ──────────────────────────────────────────────
// Audio entry: play affordance + name/duration — see AudioRecordingContent.kt.
// ──────────────────────────────────────────────

// ──────────────────────────────────────────────
// Video entry: thumbnail + play overlay, edge-to-edge body below the header
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(bodyTopClip())
            .padding(bottom = CARD_RHYTHM)
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
    if (showPlayer) {
        VideoPlayerDialog(
            file = entry.file,
            onDismissRequest = { showPlayer = false },
        )
    }
}

/**
 * Clip shape for media bodies: rounded on the top corners only so the media
 * reads as the card's body zone (below the header), not bleeding under it.
 */
@Composable
private fun bodyTopClip(): androidx.compose.ui.graphics.Shape {
    val shapes = LocalExpressiveShapes.current
    val topStart = topStartCornerSize(shapes)
    val none = androidx.compose.foundation.shape
        .CornerSize(0.dp)
    return RoundedCornerShape(
        topStart = topStart,
        topEnd = topStart,
        bottomEnd = none,
        bottomStart = none,
    )
}

/** The entryCard shape's top-start corner size, or 0 if it isn't a rounded shape. */
private fun topStartCornerSize(shapes: com.chin.minddump.ui.theme.ExpressiveShapes) =
    when (val entryCard = shapes.entryCard as? RoundedCornerShape) {
        null -> androidx.compose.foundation.shape
            .CornerSize(0.dp)
        else -> entryCard.topStart
    }

// ──────────────────────────────────────────────
// File entry: file name + icon tile
// ──────────────────────────────────────────────

@Composable
private fun FileEntryContent(entry: MindDumpEntry) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = CARD_INSET, end = CARD_INSET, bottom = CARD_RHYTHM),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = entry.type.toIcon(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(9.dp)
                        .size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = entry.file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = fileSizeLabel(entry.file),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

private fun fileSizeLabel(file: File): String {
    val kb = file.length() / 1024
    return if (kb < 1024) "$kb KB" else "%.1f MB".format(kb / 1024.0)
}

// ──────────────────────────────────────────────
// Type icon, color, and label mapping
// ──────────────────────────────────────────────

private fun EntryType.toIcon(): ImageVector = when (this) {
    EntryType.TEXT -> Icons.Filled.Edit
    EntryType.PHOTO -> Icons.Filled.PhotoCamera
    EntryType.RECORDING -> Icons.Filled.Mic
    EntryType.VIDEO -> Icons.Filled.Videocam
    EntryType.FILE -> Icons.AutoMirrored.Filled.InsertDriveFile
    EntryType.GROUP -> Icons.Filled.CreateNewFolder
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
    EntryType.GROUP -> MaterialTheme.colorScheme.onSurfaceVariant
    EntryType.UNKNOWN -> MaterialTheme.colorScheme.outline
}

// ──────────────────────────────────────────────
// Relative timestamp formatting
// ──────────────────────────────────────────────

/**
 * Format a relative timestamp from an epoch-millis value (e.g. a group's newest
 * member mtime). Mirrors the [formatRelativeTimestamp] branches but works off a
 * raw [Long] instead of the month-folder/timestamp filename pair.
 */
private fun formatRelativeEpoch(epochMillis: Long): String {
    if (epochMillis <= 0L) return ""
    val entryDateTime = LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(epochMillis),
        java.time.ZoneId.systemDefault(),
    )
    val now = LocalDateTime.now()
    val duration = Duration.between(entryDateTime, now)
    return when {
        duration.toMinutes() < 1 -> "刚刚"
        duration.toHours() < 1 -> "${duration.toMinutes()}分钟前"
        duration.toDays() < 1 && entryDateTime.toLocalDate() == now.toLocalDate() -> {
            entryDateTime.format(DateTimeFormatter.ofPattern("今天 HH:mm"))
        }
        duration.toDays() < 2 -> entryDateTime.format(DateTimeFormatter.ofPattern("昨天 HH:mm"))
        entryDateTime.year == now.year ->
            entryDateTime.format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
        else -> entryDateTime.format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm"))
    }
}

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
// Unified meta chip + chip row + card footer
// ──────────────────────────────────────────────

private val META_CHIP_SHAPE = RoundedCornerShape(9.dp)
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
    textDecoration: TextDecoration = TextDecoration.None,
) {
    Surface(
        shape = META_CHIP_SHAPE,
        color = containerColor,
        modifier = modifier,
    ) {
        if (leadingIcon == null) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                textDecoration = textDecoration,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(META_CHIP_ICON_SIZE),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    textDecoration = textDecoration,
                )
            }
        }
    }
}

/**
 * A wrapping chip row used by the card footers (entry meta + group type counts).
 * One [androidx.compose.foundation.layout.FlowRow] so chips wrap freely.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CardChipRow(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.FlowRowScope.() -> Unit,
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/**
 * Read-only summary of an entry's sidecar metadata, rendered as a wrap of tonal
 * chips below the card body. Tags become `#tag` chips; the soonest pending (or,
 * when all fired, the most-recent fired) event becomes a bell chip.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun EntryCardMetaFooter(
    entry: MindDumpEntry,
) {
    val reminderDue = nextEventDue(entry.events)
    val reminderEvent = remember(entry.events, reminderDue) {
        if (reminderDue == null) null
        else entry.events.firstOrNull { it.due == reminderDue }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = CARD_INSET, end = CARD_INSET, bottom = CARD_INSET),
    ) {
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            entry.tags.forEach { tag ->
                MetaChip(
                    text = "#$tag",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            if (reminderEvent != null) {
                val isFired = reminderEvent.state == com.chin.minddump.storage.EventState.FIRED
                val label = buildString {
                    append(formatFriendlyDateTime(reminderEvent.due))
                    if (isFired) {
                        append(" · ")
                        append(stringResource(R.string.event_fired))
                    }
                }
                MetaChip(
                    text = label,
                    containerColor = if (isFired) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    contentColor = if (isFired) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    leadingIcon = Icons.Filled.Notifications,
                    iconTint = if (isFired) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                )
            }
        }
    }
}
