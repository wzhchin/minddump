package com.chin.minddump.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chin.minddump.ui.theme.LocalExpressiveShapes

/**
 * The expressive card surface every entry renders on.
 *
 * Replaces the legacy [GroupedMessageBubble] chat-bubble metaphor. A calm
 * `surfaceContainer` tone with the large rounded shape (28dp), a light tonal
 * elevation to lift it off the background, and a 1px `outlineVariant` border for a
 * crisp edge. Tap/long-press are wired with tactile feedback.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryCard(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shapes = LocalExpressiveShapes.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            ).border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shapes.cardLarge,
            ),
        shape = shapes.cardLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
    ) {
        Column(content = content)
    }
}
