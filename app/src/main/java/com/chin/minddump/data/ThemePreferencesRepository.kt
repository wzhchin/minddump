package com.chin.minddump.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chin.minddump.ui.theme.AppPaletteStyle
import com.chin.minddump.ui.theme.AppThemeMode
import com.chin.minddump.ui.theme.ThemePreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

/**
 * Persists [ThemePreferences] in a DataStore. `seedColor == null` (sentinel ARGB value
 * [NO_SEED]) means "follow the system accent / app default", preserving the
 * pre-existing dynamic-color behavior.
 */
@Singleton
class ThemePreferencesRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val seedColorKey = intPreferencesKey("seed_color")
        private val paletteStyleKey = stringPreferencesKey("palette_style")
        private val amoledKey = booleanPreferencesKey("amoled")
        private val modeKey = stringPreferencesKey("theme_mode")

        val preferences: Flow<ThemePreferences> = context.themeDataStore.data.map { prefs ->
            ThemePreferences(
                seedColor = prefs[seedColorKey]?.takeIf { it != NO_SEED }?.let(::Color),
                paletteStyle = prefs[paletteStyleKey]
                    ?.let { name -> AppPaletteStyle.entries.firstOrNull { it.name == name } }
                    ?: AppPaletteStyle.TONAL_SPOT,
                amoled = prefs[amoledKey] ?: false,
                mode = prefs[modeKey]
                    ?.let { name -> AppThemeMode.entries.firstOrNull { it.name == name } }
                    ?: AppThemeMode.SYSTEM,
            )
        }

        suspend fun setSeedColor(color: Color?) {
            context.themeDataStore.edit { prefs ->
                if (color == null) {
                    prefs.remove(seedColorKey)
                } else {
                    prefs[seedColorKey] = color.toArgb()
                }
            }
        }

        suspend fun setPaletteStyle(style: AppPaletteStyle) {
            context.themeDataStore.edit { prefs ->
                prefs[paletteStyleKey] = style.name
            }
        }

        suspend fun setAmoled(enabled: Boolean) {
            context.themeDataStore.edit { prefs ->
                prefs[amoledKey] = enabled
            }
        }

        suspend fun setMode(mode: AppThemeMode) {
            context.themeDataStore.edit { prefs ->
                prefs[modeKey] = mode.name
            }
        }

        private companion object {
            /** Sentinel stored ARGB meaning "no custom seed — follow system/default". */
            const val NO_SEED = Int.MIN_VALUE
        }
    }
