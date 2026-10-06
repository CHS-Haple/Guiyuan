package com.chaners.guiyuan.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal enum class AppThemeMode {
    System,
    Light,
    Dark,
}

internal enum class FloatingNavStyle {
    Standard,
    Blur,
    Glass,
}

internal enum class FloatingNavContent {
    IconOnly,
    IconAndText,
}

internal data class AppearanceSettings(
    val themeMode: AppThemeMode = AppThemeMode.System,
    val dynamicColorEnabled: Boolean = false,
    val floatingNavigationBarEnabled: Boolean = true,
    val floatingNavigationStyle: FloatingNavStyle = FloatingNavStyle.Glass,
    val floatingNavigationContent: FloatingNavContent = FloatingNavContent.IconOnly,
    val swipeBackEnabled: Boolean = true,
)

internal data class ThemeSelection(
    val mode: AppThemeMode,
    val dynamicColorEnabled: Boolean,
)

internal fun decodeFloatingNavStyle(
    storedStyle: String?,
    storedFloatingBlurEnabled: Boolean?,
    legacyBlurEnabled: Boolean?,
    legacyGlassEnabled: Boolean?,
): FloatingNavStyle {
    FloatingNavStyle.entries
        .firstOrNull { it.name == storedStyle }
        ?.let { return it }

    storedFloatingBlurEnabled?.let { enabled ->
        return if (enabled) FloatingNavStyle.Glass else FloatingNavStyle.Standard
    }

    val blurEnabled = legacyBlurEnabled ?: true
    if (!blurEnabled) return FloatingNavStyle.Standard

    return if (legacyGlassEnabled ?: true) {
        FloatingNavStyle.Glass
    } else {
        FloatingNavStyle.Blur
    }
}

internal fun decodeFloatingNavContent(storedContent: String?): FloatingNavContent =
    FloatingNavContent.entries
        .firstOrNull { it.name == storedContent }
        ?: FloatingNavContent.IconOnly

internal fun decodeThemeSelection(
    storedMode: String?,
    storedDynamicColorEnabled: Boolean?,
): ThemeSelection {
    val legacyDynamic = storedMode == LEGACY_DYNAMIC_THEME_MODE
    val mode =
        AppThemeMode.entries.firstOrNull { it.name == storedMode }
            ?: AppThemeMode.System
    return ThemeSelection(
        mode = mode,
        dynamicColorEnabled = storedDynamicColorEnabled ?: legacyDynamic,
    )
}

private val Context.appearanceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "appearance",
)

internal class AppearanceRepo(context: Context) {
    private val dataStore = context.applicationContext.appearanceDataStore

    val settings: Flow<AppearanceSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            val themeSelection =
                decodeThemeSelection(
                    storedMode = preferences[ThemeModeKey],
                    storedDynamicColorEnabled = preferences[DynamicColorEnabledKey],
                )
            AppearanceSettings(
                themeMode = themeSelection.mode,
                dynamicColorEnabled = themeSelection.dynamicColorEnabled,
                floatingNavigationBarEnabled =
                    preferences[FloatingNavEnabledKey] ?: true,
                floatingNavigationStyle =
                    decodeFloatingNavStyle(
                        storedStyle = preferences[FloatingNavStyleKey],
                        storedFloatingBlurEnabled =
                            preferences[LegacyFloatingNavBlurKey],
                        legacyBlurEnabled = preferences[LegacyBlurEnabledKey],
                        legacyGlassEnabled = preferences[LegacyGlassBarKey],
                    ),
                floatingNavigationContent =
                    decodeFloatingNavContent(
                        preferences[FloatingNavContentKey],
                    ),
                swipeBackEnabled = preferences[SwipeBackEnabledKey] ?: true,
            )
        }

    suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { preferences ->
            preferences[ThemeModeKey] = mode.name
        }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            if (preferences[ThemeModeKey] == LEGACY_DYNAMIC_THEME_MODE) {
                preferences[ThemeModeKey] = AppThemeMode.System.name
            }
            preferences[DynamicColorEnabledKey] = enabled
        }
    }

    suspend fun setFloatingNavEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[FloatingNavEnabledKey] = enabled
        }
    }

    suspend fun setFloatingNavStyle(style: FloatingNavStyle) {
        dataStore.edit { preferences ->
            preferences[FloatingNavStyleKey] = style.name
            preferences.remove(LegacyFloatingNavBlurKey)
            preferences.remove(LegacyGlassBarKey)
            preferences.remove(LegacyBlurEnabledKey)
        }
    }

    suspend fun setFloatingNavContent(content: FloatingNavContent) {
        dataStore.edit { preferences ->
            preferences[FloatingNavContentKey] = content.name
        }
    }

    suspend fun setSwipeBackEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SwipeBackEnabledKey] = enabled
        }
    }

    private companion object {
        val ThemeModeKey = stringPreferencesKey("theme_mode")
        val DynamicColorEnabledKey = booleanPreferencesKey("dynamic_color_enabled")
        val LegacyBlurEnabledKey = booleanPreferencesKey("blur_enabled")
        val LegacyGlassBarKey = booleanPreferencesKey("glass_bottom_bar_enabled")
        val FloatingNavEnabledKey =
            booleanPreferencesKey("floating_navigation_bar_enabled")
        val FloatingNavStyleKey =
            stringPreferencesKey("floating_navigation_style")
        val FloatingNavContentKey =
            stringPreferencesKey("floating_navigation_content")
        val LegacyFloatingNavBlurKey =
            booleanPreferencesKey("floating_navigation_blur_enabled")
        val SwipeBackEnabledKey = booleanPreferencesKey("swipe_back_enabled")
    }
}

private const val LEGACY_DYNAMIC_THEME_MODE = "Dynamic"
