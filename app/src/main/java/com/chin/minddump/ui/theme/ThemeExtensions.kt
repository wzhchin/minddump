package com.chin.minddump.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ──────────────────────────────────────────────
// Gradient Colors
// ──────────────────────────────────────────────

/**
 * Brand gradient color pairs used across the app.
 */
@Immutable
data class GradientColors(
    val primaryGradient: Pair<Color, Color> = Pair(Color.Unspecified, Color.Unspecified),
    val cardGradient: Pair<Color, Color> = Pair(Color.Unspecified, Color.Unspecified),
    val inputGradient: Pair<Color, Color> = Pair(Color.Unspecified, Color.Unspecified),
)

/** Convert a color pair to a [Brush.linearGradient] for convenience. */
fun GradientColors.primaryBrush(): Brush =
    Brush.linearGradient(colors = listOf(primaryGradient.first, primaryGradient.second))

fun GradientColors.cardBrush(): Brush =
    Brush.linearGradient(colors = listOf(cardGradient.first, cardGradient.second))

fun GradientColors.inputBrush(): Brush =
    Brush.linearGradient(colors = listOf(inputGradient.first, inputGradient.second))

val LocalGradientColors = staticCompositionLocalOf { GradientColors() }

// ──────────────────────────────────────────────
// Animation Duration
// ──────────────────────────────────────────────

@Immutable
data class AnimationDuration(
    val short: Int = 150,
    val medium: Int = 300,
    val long: Int = 500,
)

val LocalAnimationDuration = staticCompositionLocalOf { AnimationDuration() }

// ──────────────────────────────────────────────
// Motion Curve
// ──────────────────────────────────────────────

@Immutable
data class MotionCurve(
    val emphasize: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    val decelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f),
)

val LocalMotionCurve = staticCompositionLocalOf { MotionCurve() }

// ──────────────────────────────────────────────
// Reduced Motion Detection
// ──────────────────────────────────────────────

/**
 * Check if the user has enabled "Remove animations" in accessibility settings.
 * When true, all durations become 0 and curves become LinearEasing.
 */
fun Context.isReduceMotionEnabled(): Boolean {
    return Settings.Global.getFloat(
        contentResolver,
        Settings.Global.TRANSITION_ANIMATION_SCALE,
        1f,
    ) == 0f || Settings.Global.getFloat(
        contentResolver,
        Settings.Global.WINDOW_ANIMATION_SCALE,
        1f,
    ) == 0f
}

/**
 * Provide reduced-motion-aware animation constants.
 * Returns zero durations and linear curves when the user disables animations.
 */
@Composable
fun rememberAnimationDuration(reduceMotion: Boolean = false): AnimationDuration {
    return if (reduceMotion) {
        AnimationDuration(short = 0, medium = 0, long = 0)
    } else {
        AnimationDuration()
    }
}

@Composable
fun rememberMotionCurve(reduceMotion: Boolean = false): MotionCurve {
    return if (reduceMotion) {
        MotionCurve(
            emphasize = Easing { it },
            standard = Easing { it },
            decelerate = Easing { it },
        )
    } else {
        MotionCurve()
    }
}
