package com.chin.minddump.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1B6B4A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA4F5C8),
    onPrimaryContainer = Color(0xFF002112),
    secondary = Color(0xFF4E6355),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E8D6),
    onSecondaryContainer = Color(0xFF0B1F15),
    tertiary = Color(0xFF3B6470),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBFE9F7),
    onTertiaryContainer = Color(0xFF001F27),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBFDF8),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFBFDF8),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDDE5DB),
    onSurfaceVariant = Color(0xFF414942),
    outline = Color(0xFF717971),
    outlineVariant = Color(0xFFC1C9C0),
    inverseSurface = Color(0xFF2E312D),
    inverseOnSurface = Color(0xFFF0F1EC),
    inversePrimary = Color(0xFF88D8AD),
    surfaceTint = Color(0xFF1B6B4A),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF88D8AD),
    onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF005234),
    onPrimaryContainer = Color(0xFFA4F5C8),
    secondary = Color(0xFFB4CCBA),
    onSecondary = Color(0xFF203529),
    secondaryContainer = Color(0xFF374B3F),
    onSecondaryContainer = Color(0xFFD0E8D6),
    tertiary = Color(0xFFA3CDDB),
    onTertiary = Color(0xFF033641),
    tertiaryContainer = Color(0xFF214C58),
    onTertiaryContainer = Color(0xFFBFE9F7),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF191C1A),
    onBackground = Color(0xFFE1E3DE),
    surface = Color(0xFF191C1A),
    onSurface = Color(0xFFE1E3DE),
    surfaceVariant = Color(0xFF414942),
    onSurfaceVariant = Color(0xFFC1C9C0),
    outline = Color(0xFF8B938B),
    outlineVariant = Color(0xFF414942),
    inverseSurface = Color(0xFFE1E3DE),
    inverseOnSurface = Color(0xFF191C1A),
    inversePrimary = Color(0xFF1B6B4A),
    surfaceTint = Color(0xFF88D8AD),
)

@Composable
fun MindDumpTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    // Dynamic color on Android 12+, but respect the space-based theme override
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme -> {
            // Use dynamic light for Public space on Android 12+
            dynamicLightColorScheme(context)
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> {
            // Use dynamic dark for Private space on Android 12+
            dynamicDarkColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
