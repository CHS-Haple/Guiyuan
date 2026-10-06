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

internal enum class FloatingNavigationStyle {
    Standard,
    Blur,
    Glass,
}

internal enum class FloatingNavigationContent {
    IconOnly,
    IconAndText,
}

internal data class AppearanceSettings(
    val themeMode: AppThemeMode = AppThemeMode.System,
    val dynamicColorEnabled: Boolean = false,
    val floatingNavigationBarEnabled: Boolean = true,
    val floatingNavigationStyle: FloatingNavigationStyle = FloatingNavigationStyle.Glass,
    val floatingNavigationContent: FloatingNavigationContent = FloatingNavigationContent.IconOnly,
    val swipeBackEnabled: Boolean = true,
)

internal data class ThemeSelection(
    val mode: AppThemeMode,
    val dynamicColorEnabled: Boolean,
)

internal fun decodeFloatingNavigationStyle(
    storedStyle: String?,
    storedFloatingBlurEnabled: Boolean?,
    legacyBlurEnabled: Boolean?,
    legacyGlassEnabled: Boolean?,
): FloatingNavigationStyle {
    FloatingNavigationStyle.entries
        .firstOrNull { it.name == storedStyle }
        ?.let { return it }

    storedFloatingBlurEnabled?.let { enabled ->
        return if (enabled) FloatingNavigationStyle.Glass else FloatingNavigationStyle.Standard
    }

    val blurEnabled = legacyBlurEnabled ?: true
    if (!blurEnabled) return FloatingNavigationStyle.Standard

    return if (legacyGlassEnabled ?: true) {
        FloatingNavigationStyle.Glass
    } else {
        FloatingNavigationStyle.Blur
    }
}

internal fun decodeFloatingNavigationContent(storedContent: String?): FloatingNavigationContent =
    FloatingNavigationContent.entries
        .firstOrNull { it.name == storedContent }
        ?: FloatingNavigationContent.IconOnly

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
                    preferences[FloatingNavigationBarEnabledKey] ?: true,
                floatingNavigationStyle =
                    decodeFloatingNavigationStyle(
                        storedStyle = preferences[FloatingNavigationStyleKey],
                        storedFloatingBlurEnabled =
                            preferences[LegacyFloatingNavigationBlurEnabledKey],
                        legacyBlurEnabled = preferences[LegacyBlurEnabledKey],
                        legacyGlassEnabled = preferences[LegacyGlassBottomBarEnabledKey],
                    ),
                floatingNavigationContent =
                    decodeFloatingNavigationContent(
                        preferences[FloatingNavigationContentKey],
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

    suspend fun setFloatingNavigationBarEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[FloatingNavigationBarEnabledKey] = enabled
        }
    }

    suspend fun setFloatingNavigationStyle(style: FloatingNavigationStyle) {
        dataStore.edit { preferences ->
            preferences[FloatingNavigationStyleKey] = style.name
            preferences.remove(LegacyFloatingNavigationBlurEnabledKey)
            preferences.remove(LegacyGlassBottomBarEnabledKey)
            preferences.remove(LegacyBlurEnabledKey)
        }
    }

    suspend fun setFloatingNavigationContent(content: FloatingNavigationContent) {
        dataStore.edit { preferences ->
            preferences[FloatingNavigationContentKey] = content.name
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
        val LegacyGlassBottomBarEnabledKey = booleanPreferencesKey("glass_bottom_bar_enabled")
        val FloatingNavigationBarEnabledKey =
            booleanPreferencesKey("floating_navigation_bar_enabled")
        val FloatingNavigationStyleKey =
            stringPreferencesKey("floating_navigation_style")
        val FloatingNavigationContentKey =
            stringPreferencesKey("floating_navigation_content")
        val LegacyFloatingNavigationBlurEnabledKey =
            booleanPreferencesKey("floating_navigation_blur_enabled")
        val SwipeBackEnabledKey = booleanPreferencesKey("swipe_back_enabled")
    }
}

private const val LEGACY_DYNAMIC_THEME_MODE = "Dynamic"
