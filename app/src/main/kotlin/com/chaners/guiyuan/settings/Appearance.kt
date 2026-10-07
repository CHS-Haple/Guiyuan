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
    Liquid,
}

internal enum class NavContent {
    IconOnly,
    IconAndText,
}

internal enum class LiquidMode {
    Blur,
    Clear,
}

internal data class Appearance(
    val theme: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = false,
    val navEnabled: Boolean = true,
    val navStyle: NavStyle = NavStyle.Glass,
    val navContent: NavContent = NavContent.IconOnly,
    val liquidMode: LiquidMode = LiquidMode.Clear,
    val swipeBack: Boolean = true,
)

internal data class ThemeChoice(
    val mode: ThemeMode,
    val dynamicColor: Boolean,
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

internal fun defaultNavContentFor(
    style: NavStyle,
    storedContent: String?,
): NavContent? =
    if (style == NavStyle.Liquid && storedContent == null) {
        NavContent.IconAndText
    } else {
        null
    }

internal fun decodeLiquidMode(storedMode: String?): LiquidMode =
    LiquidMode.entries
        .firstOrNull { it.name == storedMode }
        ?: LiquidMode.Clear

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
        dynamicColor = storedDynamicColorEnabled ?: legacyDynamic,
    )
}

private val Context.appearanceStore: DataStore<Preferences> by preferencesDataStore(
    name = "appearance",
)

internal class AppearanceRepo(context: Context) {
    private val store = context.applicationContext.appearanceStore

    val settings: Flow<Appearance> = store.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { prefs ->
            val theme =
                decodeTheme(
                    storedMode = prefs[themeKey],
                    storedDynamicColorEnabled = prefs[dynamicKey],
                )
            Appearance(
                theme = theme.mode,
                dynamicColor = theme.dynamicColor,
                navEnabled = prefs[navEnabledKey] ?: true,
                navStyle =
                    decodeNavStyle(
                        storedStyle = prefs[navStyleKey],
                        storedFloatingBlurEnabled = prefs[legacyNavBlurKey],
                        legacyBlurEnabled = prefs[legacyBlurKey],
                        legacyGlassEnabled = prefs[legacyGlassKey],
                    ),
                navContent = decodeNavContent(prefs[navContentKey]),
                liquidMode = decodeLiquidMode(prefs[liquidModeKey]),
                swipeBack = prefs[swipeBackKey] ?: true,
            )
        }

    suspend fun setTheme(mode: ThemeMode) {
        store.edit { prefs ->
            prefs[themeKey] = mode.name
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        store.edit { prefs ->
            if (prefs[themeKey] == LEGACY_DYNAMIC_THEME_MODE) {
                prefs[themeKey] = ThemeMode.System.name
            }
            prefs[dynamicKey] = enabled
        }
    }

    suspend fun setNavEnabled(enabled: Boolean) {
        store.edit { prefs ->
            prefs[navEnabledKey] = enabled
        }
    }

    suspend fun setNavStyle(style: NavStyle) {
        store.edit { prefs ->
            prefs[navStyleKey] = style.name
            defaultNavContentFor(style, prefs[navContentKey])?.let { content ->
                prefs[navContentKey] = content.name
            }
            prefs.remove(legacyNavBlurKey)
            prefs.remove(legacyGlassKey)
            prefs.remove(legacyBlurKey)
        }
    }

    suspend fun setNavContent(content: NavContent) {
        store.edit { prefs ->
            prefs[navContentKey] = content.name
        }
    }

    suspend fun setLiquidMode(mode: LiquidMode) {
        store.edit { prefs ->
            prefs[liquidModeKey] = mode.name
        }
    }

    suspend fun setSwipeBack(enabled: Boolean) {
        store.edit { prefs ->
            prefs[swipeBackKey] = enabled
        }
    }

    private companion object {
        val themeKey = stringPreferencesKey("theme_mode")
        val dynamicKey = booleanPreferencesKey("dynamic_color_enabled")
        val legacyBlurKey = booleanPreferencesKey("blur_enabled")
        val legacyGlassKey = booleanPreferencesKey("glass_bottom_bar_enabled")
        val navEnabledKey = booleanPreferencesKey("floating_navigation_bar_enabled")
        val navStyleKey = stringPreferencesKey("floating_navigation_style")
        val navContentKey = stringPreferencesKey("floating_navigation_content")
        val liquidModeKey = stringPreferencesKey("floating_navigation_liquid_mode")
        val legacyNavBlurKey = booleanPreferencesKey("floating_navigation_blur_enabled")
        val swipeBackKey = booleanPreferencesKey("swipe_back_enabled")
    }
}

private const val LEGACY_DYNAMIC_THEME_MODE = "Dynamic"
