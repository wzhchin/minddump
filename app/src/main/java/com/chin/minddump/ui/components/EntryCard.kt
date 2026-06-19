package com.chin.minddump.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.chin.minddump.ui.theme.LocalExpressiveShapes

private const val CARD_TINT_ALPHA_LIGHT = 0.06f
private const val CARD_TINT_ALPHA_DARK = 0.04f

/**
 * The calm Material 3 Expressive surface every entry renders on.
 *
 * Restrained layered treatment (m3e-restrained-cards):
 *  - a single calm tonal container (`surfaceContainerLow`) as the base, NOT a
 *    gradient wash — depth comes from tonal elevation, not paint,
 *  - an optional faint type-tint overlay (very low alpha) so heterogeneous feeds
 *    still read at a glance without color blocks,
 *  - a hairline `outlineVariant` border (reduced alpha),
 *  - a light two-layer tonal + ambient elevation,
 *  - the asymmetric [com.chin.minddump.ui.theme.ExpressiveShapes.entryCard] shape.
 *
 * Pass [typeTint] for entry cards to carry the type wash; pass `null` (default)
 * for surfaces that should stay neutral (e.g. the group card).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntryCard(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    typeTint: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shapes = LocalExpressiveShapes.current
    val isDark = isSystemInDarkTheme()
    val effectiveTint = typeTint?.takeIf { it.isSpecified }
    val cardBorder = androidx.compose.foundation.BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(
            alpha = if (isDark) 0.35f else 0.55f,
        ),
    )
    val cardModifier = modifier
        .fillMaxWidth()
        .drawBehind {
            if (effectiveTint != null) {
                // In dark theme, blend the tint toward black so a saturated
                // type color does not read as a color block at low alpha.
                val tint = if (isDark) lerp(Color.Black, effectiveTint, 0.4f) else effectiveTint
                drawRect(color = tint, alpha = if (isDark) CARD_TINT_ALPHA_DARK else CARD_TINT_ALPHA_LIGHT)
            }
        }.combinedClickable(
            enabled = enabled,
            onClick = onClick,
            onLongClick = onLongClick,
        ).border(border = cardBorder, shape = shapes.entryCard)

    Surface(
        modifier = cardModifier,
        shape = shapes.entryCard,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
    ) {
        Column(content = content)
    }
}
