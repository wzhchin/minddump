package com.chin.minddump.ui

import android.app.Activity
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ──────────────────────────────────────────────
// Color palette
// ──────────────────────────────────────────────

internal val Blue10 = Color(0xFF001F28)
internal val Blue40 = Color(0xFF006780)
internal val Blue90 = Color(0xFFB8EAFF)
internal val DarkPurpleGray10 = Color(0xFF201A1B)
internal val DarkPurpleGray90 = Color(0xFFECDFE0)
internal val DarkPurpleGray95 = Color(0xFFFAEEEF)
internal val DarkPurpleGray99 = Color(0xFFFCFCFC)
internal val Orange10 = Color(0xFF380D00)
internal val Orange40 = Color(0xFFA23F16)
internal val Orange90 = Color(0xFFFFDBCF)
internal val Purple10 = Color(0xFF36003C)
internal val Purple30 = Color(0xFF702776)
internal val Purple40 = Color(0xFF8B418F)
internal val Purple80 = Color(0xFFFFA9FE)
internal val Purple90 = Color(0xFFFFD6FA)
internal val PurpleGray30 = Color(0xFF4D444C)
internal val PurpleGray50 = Color(0xFF7F747C)
internal val PurpleGray60 = Color(0xFF998D96)
internal val PurpleGray80 = Color(0xFFD0C3CC)
internal val PurpleGray90 = Color(0xFFEDDEE8)
internal val Red40 = Color(0xFFBA1A1A)
internal val Red80 = Color(0xFFFFB4AB)
internal val Red90 = Color(0xFFFFDAD6)

// ──────────────────────────────────────────────
// Color schemes
// ──────────────────────────────────────────────

private val LightDefaultColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Color.White,
    primaryContainer = Purple90,
    onPrimaryContainer = Purple10,
    secondary = Orange40,
    onSecondary = Color.White,
    secondaryContainer = Orange90,
    onSecondaryContainer = Orange10,
    tertiary = Blue40,
    onTertiary = Color.White,
    tertiaryContainer = Blue90,
    onTertiaryContainer = Blue10,
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Blue10,
    background = DarkPurpleGray99,
    onBackground = DarkPurpleGray10,
    surface = DarkPurpleGray99,
    onSurface = DarkPurpleGray10,
    surfaceVariant = PurpleGray90,
    onSurfaceVariant = PurpleGray30,
    inverseSurface = DarkPurpleGray10,
    inverseOnSurface = DarkPurpleGray95,
    outline = PurpleGray50,
)

private val DarkDefaultColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = Purple10,
    primaryContainer = Purple30,
    onPrimaryContainer = Purple90,
    secondary = Orange90,
    onSecondary = Orange10,
    tertiary = Blue90,
    onTertiary = Blue10,
    error = Red80,
    onError = Blue10,
    errorContainer = Red90,
    background = DarkPurpleGray10,
    onBackground = DarkPurpleGray90,
    surface = DarkPurpleGray10,
    onSurface = DarkPurpleGray90,
    surfaceVariant = PurpleGray30,
    onSurfaceVariant = PurpleGray80,
    inverseSurface = DarkPurpleGray90,
    inverseOnSurface = DarkPurpleGray10,
    outline = PurpleGray60,
)

// ──────────────────────────────────────────────
// Theme extension: Background
// ──────────────────────────────────────────────

@Immutable
data class BackgroundTheme(
    val color: Color = Color.Unspecified,
    val tonalElevation: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Unspecified,
)

val LocalBackgroundTheme = staticCompositionLocalOf { BackgroundTheme() }

// ──────────────────────────────────────────────
// Theme extension: Tint
// ──────────────────────────────────────────────

@Immutable
data class TintTheme(
    val iconTint: Color = Color.Unspecified,
)

val LocalTintTheme = staticCompositionLocalOf { TintTheme() }

// ──────────────────────────────────────────────
// Typography (only styles actually used)
// ──────────────────────────────────────────────

private val AppTypography = Typography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Bottom,
            trim = LineHeightStyle.Trim.LastLineBottom,
        ),
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.LastLineBottom,
        ),
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.LastLineBottom,
        ),
    ),
)

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
// Main theme
// ──────────────────────────────────────────────

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
private fun supportsDynamicTheming() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun MindDumpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    // Color scheme
    val colorScheme = when {
        supportsDynamicTheming() -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkDefaultColorScheme
        else -> LightDefaultColorScheme
    }

    // Background theme
    val backgroundTheme = BackgroundTheme(
        color = colorScheme.surface,
        tonalElevation = 2.dp,
    )

    // Tint theme
    val tintTheme = if (supportsDynamicTheming()) {
        TintTheme(colorScheme.primary)
    } else {
        TintTheme()
    }

    // Status bar
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // Composition locals
    CompositionLocalProvider(
        LocalBackgroundTheme provides backgroundTheme,
        LocalTintTheme provides tintTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
