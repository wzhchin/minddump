package com.chin.minddump.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

// ──────────────────────────────────────────────
// 7.1 Entry Skeleton Card
// ──────────────────────────────────────────────

/**
 * A single skeleton card placeholder that mimics a text entry card layout.
 * Uses a shimmer effect for visual polish during loading.
 */
@Composable
fun EntrySkeletonCard(
    isPhotoVariant: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val shimmerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val shimmerMidColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val shape = RoundedCornerShape(16.dp)

    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_progress",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .drawWithContent {
                drawContent()
                val width = size.width
                val gradientWidth = width * 0.6f
                val offset = (progress * (width + gradientWidth)) - gradientWidth
                val brush = Brush.horizontalGradient(
                    colors = listOf(shimmerColor, shimmerMidColor, shimmerColor),
                    startX = offset,
                    endX = offset + gradientWidth,
                )
                drawRect(brush = brush)
            },
    ) {
        if (isPhotoVariant) {
            // Photo placeholder: gray rectangle for image area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Text line placeholder
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                if (!isPhotoVariant) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// 7.2 Skeleton Entry List
// ──────────────────────────────────────────────

/**
 * Displays 5 skeleton cards with varied layouts (text/photo mix) during loading.
 */
@Composable
fun SkeletonEntryList(modifier: Modifier = Modifier) {
    val variants = listOf(false, true, false, false, true)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(count = 5) { index ->
            EntrySkeletonCard(isPhotoVariant = variants.getOrElse(index) { false })
        }
    }
}

// ──────────────────────────────────────────────
// 7.4 & 7.5 Empty States
// ──────────────────────────────────────────────

/**
 * Empty state for a fresh install — no entries yet.
 * Shows a thought-bubble icon with a subtle floating animation.
 */
@Composable
fun EmptyStateDefault(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_float")

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "float_offset",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Floating icon
        Icon(
            imageVector = Icons.Filled.ChatBubbleOutline,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer { translationY = floatOffset },
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "开始记录你的想法",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "点击下方输入栏，写下第一条想法",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Empty state for no search results.
 * Shows a magnifying glass icon.
 */
@Composable
fun EmptyStateNoResults(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "noresults_float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "float_offset",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer { translationY = floatOffset },
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "未找到结果",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "试试其他关键词",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
