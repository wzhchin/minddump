package com.chin.minddump.ui.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle

/**
 * User-tunable theme configuration, persisted via [ThemePreferencesRepository].
 *
 * @property seedColor  the seed color the whole color scheme derives from.
 *   `null` means "follow the system accent on Android 12+ (Material You), else the
 *   app default seed" — preserving the pre-existing dynamic-color behavior.
 * @property paletteStyle  the materialKolor palette style used to derive the scheme.
 * @property amoled  when true, dark mode renders near-true-black backgrounds.
 * @property mode  light/dark/system preference.
 */
data class ThemePreferences(
    val seedColor: Color? = null,
    val paletteStyle: AppPaletteStyle = AppPaletteStyle.TONAL_SPOT,
    val amoled: Boolean = false,
    val mode: AppThemeMode = AppThemeMode.SYSTEM,
)

/**
 * App-local palette-style enum (zh-CN + en labels live in strings.xml). Maps 1:1 to
 * [com.materialkolor.PaletteStyle] via [toMaterialKolor].
 */
enum class AppPaletteStyle {
    TONAL_SPOT,
    NEUTRAL,
    VIBRANT,
    EXPRESSIVE,
    RAINBOW,
    FRUIT_SALAD,
    MONOCHROME,
    FIDELITY,
    CONTENT,
    ;

    fun toMaterialKolor(): PaletteStyle = when (this) {
        TONAL_SPOT -> PaletteStyle.TonalSpot
        NEUTRAL -> PaletteStyle.Neutral
        VIBRANT -> PaletteStyle.Vibrant
        EXPRESSIVE -> PaletteStyle.Expressive
        RAINBOW -> PaletteStyle.Rainbow
        FRUIT_SALAD -> PaletteStyle.FruitSalad
        MONOCHROME -> PaletteStyle.Monochrome
        FIDELITY -> PaletteStyle.Fidelity
        CONTENT -> PaletteStyle.Content
    }
}

/** Light / dark / system preference. */
enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
