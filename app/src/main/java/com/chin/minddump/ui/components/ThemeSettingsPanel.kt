package com.chin.minddump.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chin.minddump.R
import com.chin.minddump.ui.theme.AppPaletteStyle
import com.chin.minddump.ui.theme.AppThemeMode
import com.chin.minddump.ui.theme.LocalExpressiveShapes
import com.chin.minddump.ui.theme.ThemePreferences

/** A curated set of seed-color presets, in rainbow order. */
private data class SeedPreset(
    val color: Color,
    val labelRes: Int
)

private val SeedPresets: List<SeedPreset> = listOf(
    SeedPreset(Color(0xFFE63946), R.string.color_red),
    SeedPreset(Color(0xFFF4A261), R.string.color_orange),
    SeedPreset(Color(0xFFE9C46A), R.string.color_yellow),
    SeedPreset(Color(0xFF2A9D8F), R.string.color_green),
    SeedPreset(Color(0xFF00B4D8), R.string.color_teal),
    SeedPreset(Color(0xFF3A86FF), R.string.color_blue),
    SeedPreset(Color(0xFF8B418F), R.string.color_purple),
    SeedPreset(Color(0xFFE5589E), R.string.color_pink),
)

/**
 * Material 3 Expressive theme configuration: seed color, palette style, dark mode,
 * AMOLED. Driven by [preferences]; mutations go through the callbacks so the caller
 * can persist them.
 */
@Composable
fun ThemeSettingsPanel(
    preferences: ThemePreferences,
    onSeedColorChange: (Color?) -> Unit,
    onPaletteStyleChange: (AppPaletteStyle) -> Unit,
    onModeChange: (AppThemeMode) -> Unit,
    onAmoledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shapes = LocalExpressiveShapes.current

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // ── Seed color ──
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.theme_seed_color),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                SeedPresets.forEach { preset ->
                    SeedColorDot(
                        color = preset.color,
                        selected = preferences.seedColor == preset.color,
                        onClick = { onSeedColorChange(preset.color) },
                        contentDescription = stringResource(preset.labelRes),
                    )
                }
                // "Follow system" option — clears the custom seed.
                val systemSelected = preferences.seedColor == null
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .then(
                            if (systemSelected) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            } else {
                                Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            },
                        ).clickable { onSeedColorChange(null) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.theme_seed_system),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ── Palette style ──
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.theme_palette_style),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppPaletteStyle.entries.forEach { style ->
                    FilterChip(
                        selected = preferences.paletteStyle == style,
                        onClick = { onPaletteStyleChange(style) },
                        label = { Text(style.displayName()) },
                        shape = shapes.chip,
                        colors = FilterChipDefaults.filterChipColors(),
                    )
                }
            }
        }

        // ── Dark mode (segmented) ──
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.theme_mode),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            val modes = listOf(AppThemeMode.SYSTEM, AppThemeMode.LIGHT, AppThemeMode.DARK)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                modes.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = preferences.mode == mode,
                        onClick = { onModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                    ) {
                        Text(mode.displayName())
                    }
                }
            }
        }

        // ── AMOLED ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    stringResource(R.string.theme_amoled),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(
                checked = preferences.amoled,
                onCheckedChange = onAmoledChange,
            )
        }
    }
}

@Composable
private fun SeedColorDot(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                },
            ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun AppPaletteStyle.displayName(): String = stringResource(
    when (this) {
        AppPaletteStyle.TONAL_SPOT -> R.string.palette_tonal_spot
        AppPaletteStyle.NEUTRAL -> R.string.palette_neutral
        AppPaletteStyle.VIBRANT -> R.string.palette_vibrant
        AppPaletteStyle.EXPRESSIVE -> R.string.palette_expressive
        AppPaletteStyle.RAINBOW -> R.string.palette_rainbow
        AppPaletteStyle.FRUIT_SALAD -> R.string.palette_fruit_salad
        AppPaletteStyle.MONOCHROME -> R.string.palette_monochrome
        AppPaletteStyle.FIDELITY -> R.string.palette_fidelity
        AppPaletteStyle.CONTENT -> R.string.palette_content
    },
)

@Composable
private fun AppThemeMode.displayName(): String = stringResource(
    when (this) {
        AppThemeMode.SYSTEM -> R.string.theme_mode_system
        AppThemeMode.LIGHT -> R.string.theme_mode_light
        AppThemeMode.DARK -> R.string.theme_mode_dark
    },
)
