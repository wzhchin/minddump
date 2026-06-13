package com.chin.minddump.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Position of a bubble within a group - determines corner styling.
 *
 * Ported from LastChat's GroupedMessageBubble. MindDump renders every entry as a
 * standalone SINGLE bubble (fully rounded, no stacking), but the full
 * BubblePosition/BubbleRole API is kept for fidelity.
 */
enum class BubblePosition {
    /** Single bubble, not part of a group */
    SINGLE,
    /** First bubble in a group (top) */
    FIRST,
    /** Middle bubble in a group */
    MIDDLE,
    /** Last bubble in a group (bottom) */
    LAST
}

/**
 * Role determines alignment and color scheme.
 */
enum class BubbleRole {
    USER,
    ASSISTANT,
    ACTIVITY
}

/**
 * A flat message bubble with grouped corner radii support. Zero elevation, no shadow.
 *
 * When bubbles are stacked in a group, the inner corners (where they meet)
 * are smaller to create a visual "stack" effect like in iMessage.
 */
@Composable
fun GroupedMessageBubble(
    position: BubblePosition,
    role: BubbleRole,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    contentColor: Color? = null,
    largeRadius: Dp = 20.dp,
    smallRadius: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val defaultContainerColor = when (role) {
        BubbleRole.USER -> MaterialTheme.colorScheme.primaryContainer
        BubbleRole.ASSISTANT -> MaterialTheme.colorScheme.surfaceContainerHigh
        BubbleRole.ACTIVITY -> MaterialTheme.colorScheme.surfaceContainerHighest
    }

    val defaultContentColor = when (role) {
        BubbleRole.USER -> MaterialTheme.colorScheme.onPrimaryContainer
        BubbleRole.ASSISTANT -> MaterialTheme.colorScheme.onSurface
        BubbleRole.ACTIVITY -> MaterialTheme.colorScheme.onSurface
    }

    // For assistant (left-aligned): small corners on the left side where bubbles stack.
    // For user (right-aligned): small corners on the right side where bubbles stack.
    val isLeftAligned = role != BubbleRole.USER

    val shape = when (position) {
        BubblePosition.SINGLE -> RoundedCornerShape(largeRadius)

        BubblePosition.FIRST -> if (isLeftAligned) {
            RoundedCornerShape(
                topStart = largeRadius,
                topEnd = largeRadius,
                bottomEnd = largeRadius,
                bottomStart = smallRadius,
            )
        } else {
            RoundedCornerShape(
                topStart = largeRadius,
                topEnd = largeRadius,
                bottomEnd = smallRadius,
                bottomStart = largeRadius,
            )
        }

        BubblePosition.MIDDLE -> if (isLeftAligned) {
            RoundedCornerShape(
                topStart = smallRadius,
                topEnd = largeRadius,
                bottomEnd = largeRadius,
                bottomStart = smallRadius,
            )
        } else {
            RoundedCornerShape(
                topStart = largeRadius,
                topEnd = smallRadius,
                bottomEnd = smallRadius,
                bottomStart = largeRadius,
            )
        }

        BubblePosition.LAST -> if (isLeftAligned) {
            RoundedCornerShape(
                topStart = smallRadius,
                topEnd = largeRadius,
                bottomEnd = largeRadius,
                bottomStart = largeRadius,
            )
        } else {
            RoundedCornerShape(
                topStart = largeRadius,
                topEnd = smallRadius,
                bottomEnd = largeRadius,
                bottomStart = largeRadius,
            )
        }
    }

    if (onClick != null) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor ?: defaultContainerColor,
            contentColor = contentColor ?: defaultContentColor,
            onClick = onClick,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                content = content,
            )
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor ?: defaultContainerColor,
            contentColor = contentColor ?: defaultContentColor,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                content = content,
            )
        }
    }
}

/**
 * Helper to determine bubble position in a list.
 */
fun getBubblePosition(index: Int, total: Int): BubblePosition = when {
    total == 1 -> BubblePosition.SINGLE
    index == 0 -> BubblePosition.FIRST
    index == total - 1 -> BubblePosition.LAST
    else -> BubblePosition.MIDDLE
}
