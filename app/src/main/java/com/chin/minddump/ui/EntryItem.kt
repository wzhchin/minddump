package com.chin.minddump.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Scale
import com.chin.minddump.storage.EntryType
import com.chin.minddump.storage.MindDumpEntry
import com.chin.minddump.ui.theme.LocalAnimationDuration
import com.chin.minddump.ui.theme.LocalGradientColors
import com.chin.minddump.ui.theme.LocalMotionCurve
import com.chin.minddump.ui.theme.animatePressScale
import com.chin.minddump.ui.theme.shimmerEffect
import com.chin.minddump.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryItem(
    entry: MindDumpEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .animatePressScale()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            when (entry.type) {
                EntryType.PHOTO -> PhotoEntryContent(entry)
                EntryType.TEXT -> TextEntryContent(entry)
                EntryType.RECORDING -> AudioEntryContent(entry)
                EntryType.VIDEO -> VideoEntryContent(entry)
                else -> OtherEntryContent(entry)
            }

            // Lock icon for encrypted entries
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}

// ──────────────────────────────────────────────
// Text entry: gradient accent bar on left
// ──────────────────────────────────────────────

@Composable
private fun TextEntryContent(entry: MindDumpEntry) {
    val gradientColors = LocalGradientColors.current
    val accentBrush = Brush.verticalGradient(
        colors = listOf(
            gradientColors.primaryGradient.first,
            gradientColors.primaryGradient.second,
        ),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    brush = accentBrush,
                    cornerRadius = CornerRadius(2.dp.toPx()),
                    topLeft = Offset.Zero,
                    size = Size(4.dp.toPx(), size.height),
                )
            }
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
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

            Text(
                text = textContent,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = effectiveMaxLines,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // "展开" indicator if text exceeds 3 lines
            if (!expanded && textContent.length > 120) {
                Text(
                    text = "展开",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatEntryMeta(entry),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// Photo entry: image + gradient overlay
// ──────────────────────────────────────────────

@Composable
private fun PhotoEntryContent(entry: MindDumpEntry) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val context = LocalContext.current
        var isLoading by remember { mutableStateOf(true) }
        var isError by remember { mutableStateOf(false) }
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(context)
                .data(entry.file)
                .size(600)
                .scale(Scale.FILL)
                .build(),
            onState = { state ->
                isLoading = state is AsyncImagePainter.State.Loading
                isError = state is AsyncImagePainter.State.Error
            },
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shimmerEffect(),
                )
            }
            Image(
                painter = painter,
                contentDescription = stringResource(R.string.photo),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = if (isError) 0f else 1f,
            )

            // Bottom gradient overlay
            val overlayBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                ),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .drawBehind {
                        drawRect(brush = overlayBrush)
                    },
            )
        }

        // Content area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = formatEntryMeta(entry),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ──────────────────────────────────────────────
// Audio entry: waveform bars + pulse indicator
// ──────────────────────────────────────────────

@Composable
private fun AudioEntryContent(entry: MindDumpEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Waveform decoration bars
        WaveformBars(
            barCount = 5,
            barWidth = 3.dp,
            maxHeight = 24.dp,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.file.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatEntryMeta(entry),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Animated waveform bars decoration for audio entries.
 */
@Composable
private fun WaveformBars(
    barCount: Int = 5,
    barWidth: Dp = 3.dp,
    maxHeight: Dp = 24.dp,
    color: Color,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val animDuration = LocalAnimationDuration.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until barCount) {
            val heightFraction by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600 + i * 100,
                        easing = { it },
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "waveform_bar_$i",
            )
            Box(
                modifier = Modifier
                    .size(width = barWidth, height = maxHeight * heightFraction)
                    .clip(RoundedCornerShape(1.dp))
                    .drawBehind {
                        drawRect(color = color)
                    },
            )
        }
    }
}

// ──────────────────────────────────────────────
// Video entry: thumbnail + play overlay
// ──────────────────────────────────────────────

@Composable
private fun VideoEntryContent(entry: MindDumpEntry) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val context = LocalContext.current
        var isLoading by remember { mutableStateOf(true) }
        var isError by remember { mutableStateOf(false) }
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(context)
                .data(entry.file)
                .size(600)
                .scale(Scale.FILL)
                .build(),
            onState = { state ->
                isLoading = state is AsyncImagePainter.State.Loading
                isError = state is AsyncImagePainter.State.Error
            },
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shimmerEffect(),
                )
            }
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = if (isError) 0f else 1f,
            )

            // Play button overlay
            val playBgColor = MaterialTheme.colorScheme.surface
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .graphicsLayer { alpha = 0.7f }
                    .drawBehind {
                        drawRect(color = playBgColor)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp),
                )
            }

            // Bottom gradient + duration badge
            val videoOverlayBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                ),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .drawBehind {
                        drawRect(brush = videoOverlayBrush)
                    },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Videocam,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = formatEntryMeta(entry),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ──────────────────────────────────────────────
// Other / fallback entry
// ──────────────────────────────────────────────

@Composable
private fun OtherEntryContent(entry: MindDumpEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when (entry.type) {
                EntryType.FILE -> Icons.AutoMirrored.Filled.InsertDriveFile
                EntryType.UNKNOWN -> Icons.Filled.Description
                else -> Icons.Filled.Description
            },
            contentDescription = entry.type.name,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.file.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatEntryMeta(entry),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = entry.dateFolder,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        val inputFormat = DateTimeFormatter.ofPattern("HHmmss")
        val outputFormat = DateTimeFormatter.ofPattern("HH:mm:ss")
        java.time.LocalTime.parse(entry.timestamp, inputFormat).format(outputFormat)
    } catch (_: Exception) {
        entry.timestamp
    }
    return "$timeStr · $sizeStr"
}
