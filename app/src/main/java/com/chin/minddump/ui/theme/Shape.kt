package com.chin.minddump.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Systematic shape tokens for the M3 Expressive design system.
 *
 * Token hierarchy:
 * - cardLarge (28dp): Hero cards, full-width entry cards
 * - cardMedium (24dp): Standard cards, dialog surfaces
 * - cardSmall (16dp): Compact cards, nested containers
 * - buttonPill (50%): Primary CTA buttons, tags
 * - buttonRounded (20dp): Standard buttons, toggles
 * - buttonSquared (12dp): Icon buttons, small actions
 * - inputField (20dp): Text fields, search bars
 * - chip (12dp): Chips, badges, small indicators
 */
@Immutable
data class ExpressiveShapes(
    val cardLarge: Shape = RoundedCornerShape(28.dp),
    val cardMedium: Shape = RoundedCornerShape(24.dp),
    val cardSmall: Shape = RoundedCornerShape(16.dp),
    val buttonPill: Shape = CircleShape,
    val buttonRounded: Shape = RoundedCornerShape(20.dp),
    val buttonSquared: Shape = RoundedCornerShape(12.dp),
    val inputField: Shape = RoundedCornerShape(20.dp),
    val chip: Shape = RoundedCornerShape(12.dp),
    /** Asymmetric shape for entry list cards — small corner on bottom-left for stack effect */
    val entryCard: Shape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomEnd = 20.dp,
        bottomStart = 6.dp,
    ),
)

val LocalExpressiveShapes = staticCompositionLocalOf { ExpressiveShapes() }
