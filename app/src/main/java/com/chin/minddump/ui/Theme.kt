package com.chin.minddump.ui

import android.app.Activity
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.chin.minddump.ui.theme.AppThemeMode
import com.chin.minddump.ui.theme.ExpressiveShapes
import com.chin.minddump.ui.theme.GradientColors
import com.chin.minddump.ui.theme.LocalAnimationDuration
import com.chin.minddump.ui.theme.LocalExpressiveShapes
import com.chin.minddump.ui.theme.LocalGradientColors
import com.chin.minddump.ui.theme.LocalMotionCurve
import com.chin.minddump.ui.theme.ThemePreferences
import com.chin.minddump.ui.theme.createTypography
import com.chin.minddump.ui.theme.isReduceMotionEnabled
import com.chin.minddump.ui.theme.rememberAnimationDuration
import com.chin.minddump.ui.theme.rememberMotionCurve
import com.materialkolor.DynamicMaterialTheme

// ──────────────────────────────────────────────
// Default brand seed (used below Android 12, or when the user hasn't picked one)
// ──────────────────────────────────────────────

private val DefaultSeedColor = Color(0xFF8B418F)

// ──────────────────────────────────────────────
// Background / tint theme extensions (unchanged)
// ──────────────────────────────────────────────

@Immutable
data class BackgroundTheme(
    val color: Color = Color.Unspecified,
    val tonalElevation: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Unspecified,
)

val LocalBackgroundTheme = staticCompositionLocalOf { BackgroundTheme() }

@Immutable
data class TintTheme(
    val iconTint: Color = Color.Unspecified,
)

val LocalTintTheme = staticCompositionLocalOf { TintTheme() }

// ──────────────────────────────────────────────
// Typography (from Type.kt — Google Sans Flex M3 Expressive)
// ──────────────────────────────────────────────

private val AppTypography = createTypography()

// ──────────────────────────────────────────────
// Background component
// ──────────────────────────────────────────────

@Composable
fun NiaBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val color = LocalBackgroundTheme.current.color
    val tonalElevation = LocalBackgroundTheme.current.tonalElevation
    Surface(
        color = if (color == Color.Unspecified) Color.Transparent else color,
        tonalElevation = if (tonalElevation == androidx.compose.ui.unit.Dp.Unspecified) 0.dp else tonalElevation,
        modifier = modifier.fillMaxSize(),
    ) {
        CompositionLocalProvider(LocalAbsoluteTonalElevation provides 0.dp) {
            content()
        }
    }
}

// ──────────────────────────────────────────────
// Main theme — materialKolor DynamicMaterialTheme driven by ThemePreferences
// ──────────────────────────────────────────────

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
private fun supportsDynamicTheming() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * The seed color actually used to derive the scheme. When the user hasn't picked a
 * custom seed, follow the system accent on Android 12+ (preserving the prior
 * dynamic-color behavior); otherwise fall back to the app default brand seed.
 */
@Composable
private fun resolveSeedColor(prefs: ThemePreferences): Color {
    prefs.seedColor?.let { return it }
    return if (supportsDynamicTheming()) {
        colorResource(android.R.color.system_accent1_200)
    } else {
        DefaultSeedColor
    }
}

private fun resolveDarkTheme(prefs: ThemePreferences): Boolean? = when (prefs.mode) {
    AppThemeMode.SYSTEM -> null
    AppThemeMode.LIGHT -> false
    AppThemeMode.DARK -> true
}

@Composable
fun MindDumpTheme(
    preferences: ThemePreferences,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val darkTheme = resolveDarkTheme(preferences) ?: systemDark
    val seedColor = resolveSeedColor(preferences)

    // Reduced motion (does not depend on the color scheme).
    val reduceMotion = context.isReduceMotionEnabled()
    val animationDuration = rememberAnimationDuration(reduceMotion)
    val motionCurve = rememberMotionCurve(reduceMotion)

    val shapes = ExpressiveShapes()

    DynamicMaterialTheme(
        seedColor = seedColor,
        isDark = darkTheme,
        isAmoled = preferences.amoled && darkTheme,
        style = preferences.paletteStyle.toMaterialKolor(),
        typography = AppTypography,
    ) {
        // Status bar — read inside the themed content so it reflects the generated scheme.
        val view = LocalView.current
        val surfaceColor = MaterialTheme.colorScheme.surface
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as Activity).window
                @Suppress("DEPRECATION")
                window.statusBarColor = surfaceColor.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }

        // Provide the scheme-derived tokens inside DynamicMaterialTheme so they
        // reflect the freshly generated color scheme.
        val backgroundTheme = BackgroundTheme(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        )
        val tintTheme = TintTheme(MaterialTheme.colorScheme.primary)
        val gradientColors = GradientColors(
            primaryGradient = Pair(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary,
            ),
            cardGradient = Pair(
                MaterialTheme.colorScheme.surfaceContainerLow,
                MaterialTheme.colorScheme.surfaceContainer,
            ),
            inputGradient = Pair(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
        )
        CompositionLocalProvider(
            LocalBackgroundTheme provides backgroundTheme,
            LocalTintTheme provides tintTheme,
            LocalGradientColors provides gradientColors,
            LocalAnimationDuration provides animationDuration,
            LocalMotionCurve provides motionCurve,
            LocalExpressiveShapes provides shapes,
        ) {
            content()
        }
    }
}
