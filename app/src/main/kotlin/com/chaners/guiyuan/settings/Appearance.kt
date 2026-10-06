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

internal enum class ThemeMode {
    System,
    Light,
    Dark,
}

internal enum class NavStyle {
    Standard,
    Blur,
    Glass,
}

internal enum class NavContent {
    IconOnly,
    IconAndText,
}

internal data class Appearance(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColorEnabled: Boolean = false,
    val floatingNavigationBarEnabled: Boolean = true,
    val floatingNavigationStyle: NavStyle = NavStyle.Glass,
    val floatingNavigationContent: NavContent = NavContent.IconOnly,
    val swipeBackEnabled: Boolean = true,
)

internal data class ThemeChoice(
    val mode: ThemeMode,
    val dynamicColorEnabled: Boolean,
)

internal fun decodeNavStyle(
    storedStyle: String?,
    storedFloatingBlurEnabled: Boolean?,
    legacyBlurEnabled: Boolean?,
    legacyGlassEnabled: Boolean?,
): NavStyle {
    NavStyle.entries
        .firstOrNull { it.name == storedStyle }
        ?.let { return it }

    storedFloatingBlurEnabled?.let { enabled ->
        return if (enabled) NavStyle.Glass else NavStyle.Standard
    }

    val blurEnabled = legacyBlurEnabled ?: true
    if (!blurEnabled) return NavStyle.Standard

    return if (legacyGlassEnabled ?: true) {
        NavStyle.Glass
    } else {
        NavStyle.Blur
    }
}

internal fun decodeNavContent(storedContent: String?): NavContent =
    NavContent.entries
        .firstOrNull { it.name == storedContent }
        ?: NavContent.IconOnly

internal fun decodeTheme(
    storedMode: String?,
    storedDynamicColorEnabled: Boolean?,
): ThemeChoice {
    val legacyDynamic = storedMode == LEGACY_DYNAMIC_THEME_MODE
    val mode =
        ThemeMode.entries.firstOrNull { it.name == storedMode }
            ?: ThemeMode.System
    return ThemeChoice(
        mode = mode,
        dynamicColorEnabled = storedDynamicColorEnabled ?: legacyDynamic,
    )
}

private val Context.appearanceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "appearance",
)

internal class AppearanceRepo(context: Context) {
    private val dataStore = context.applicationContext.appearanceDataStore

    val settings: Flow<Appearance> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            val themeSelection =
                decodeTheme(
                    storedMode = preferences[ThemeModeKey],
                    storedDynamicColorEnabled = preferences[DynamicColorEnabledKey],
                )
            Appearance(
                themeMode = themeSelection.mode,
                dynamicColorEnabled = themeSelection.dynamicColorEnabled,
                floatingNavigationBarEnabled =
                    preferences[FloatingNavigationBarEnabledKey] ?: true,
                floatingNavigationStyle =
                    decodeNavStyle(
                        storedStyle = preferences[NavStyleKey],
                        storedFloatingBlurEnabled =
                            preferences[LegacyFloatingNavigationBlurEnabledKey],
                        legacyBlurEnabled = preferences[LegacyBlurEnabledKey],
                        legacyGlassEnabled = preferences[LegacyGlassBottomBarEnabledKey],
                    ),
                floatingNavigationContent =
                    decodeNavContent(
                        preferences[NavContentKey],
                    ),
                swipeBackEnabled = preferences[SwipeBackEnabledKey] ?: true,
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[ThemeModeKey] = mode.name
        }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            if (preferences[ThemeModeKey] == LEGACY_DYNAMIC_THEME_MODE) {
                preferences[ThemeModeKey] = ThemeMode.System.name
            }
            preferences[DynamicColorEnabledKey] = enabled
        }
    }

    suspend fun setFloatingNavigationBarEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[FloatingNavigationBarEnabledKey] = enabled
        }
    }

    suspend fun setNavStyle(style: NavStyle) {
        dataStore.edit { preferences ->
            preferences[NavStyleKey] = style.name
            preferences.remove(LegacyFloatingNavigationBlurEnabledKey)
            preferences.remove(LegacyGlassBottomBarEnabledKey)
            preferences.remove(LegacyBlurEnabledKey)
        }
    }

    suspend fun setNavContent(content: NavContent) {
        dataStore.edit { preferences ->
            preferences[NavContentKey] = content.name
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
        val NavStyleKey =
            stringPreferencesKey("floating_navigation_style")
        val NavContentKey =
            stringPreferencesKey("floating_navigation_content")
        val LegacyFloatingNavigationBlurEnabledKey =
            booleanPreferencesKey("floating_navigation_blur_enabled")
        val SwipeBackEnabledKey = booleanPreferencesKey("swipe_back_enabled")
    }
}

private const val LEGACY_DYNAMIC_THEME_MODE = "Dynamic"
