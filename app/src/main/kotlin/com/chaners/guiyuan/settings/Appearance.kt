package com.chaners.guiyuan.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

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

internal enum class LiquidMode(val stored: String) {
    Blur("Blur8"),
    Balanced("Balanced"),
    Clear("Clear"),
}

internal data class Appearance(
    val theme: ThemeMode = ThemeMode.System,
    val topBarProgressiveBlur: Boolean = true,
    val dynamicColor: Boolean = false,
    val customPalette: Boolean = false,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.Spec2021,
    val paletteStyle: ThemePaletteStyle = ThemePaletteStyle.TonalSpot,
    val navEnabled: Boolean = true,
    val navTouchExtension: Boolean = true,
    val navStyle: NavStyle = NavStyle.Glass,
    val navContent: NavContent = NavContent.IconAndText,
    val liquidMode: LiquidMode = LiquidMode.Balanced,
    val swipeBack: Boolean = true,
    val uiScalePercent: Int = 100,
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

internal fun decodeNavContent(
    storedContent: String?,
    default: NavContent = NavContent.IconOnly,
): NavContent =
    NavContent.entries
        .firstOrNull { it.name == storedContent }
        ?: default

internal fun decodeLiquidMode(storedMode: String?): LiquidMode =
    if (storedMode == "Default" || storedMode == "Blur") LiquidMode.Balanced
    else LiquidMode.entries.firstOrNull { it.stored == storedMode } ?: LiquidMode.Balanced

internal fun uiScalePercent(value: Int?): Int = (value ?: 100).coerceIn(80, 110)

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

private val uiStyleKey = stringPreferencesKey("ui_style")
private val topBarProgressiveBlurKey = booleanPreferencesKey("top_bar_progressive_blur")
private val navContentKey = stringPreferencesKey("floating_navigation_content")
private val legacyLiquidContentKey = stringPreferencesKey("floating_navigation_liquid_content")
private val navStyleKey = stringPreferencesKey("floating_navigation_style")
private val os4UpgradeKey = booleanPreferencesKey("ui_style_os4_upgrade_done")

internal fun migrateAppearance(prefs: MutablePreferences) {
    // Preserve the visible navigation choice, not the inactive style's remembered value.
    val content = when {
        prefs.asMap().isEmpty() -> NavContent.IconAndText
        prefs[navStyleKey] == NavStyle.Liquid.name -> decodeNavContent(
            prefs[legacyLiquidContentKey] ?: prefs[navContentKey],
            NavContent.IconAndText,
        )
        else -> decodeNavContent(prefs[navContentKey])
    }
    prefs[navContentKey] = content.name
    prefs.remove(legacyLiquidContentKey)
    prefs.remove(uiStyleKey)
    prefs.remove(os4UpgradeKey)
}

internal class AppearanceRepo(context: Context) {
    private val store = context.applicationContext.appearanceStore

    val settings: Flow<Appearance> = store.data
        .onStart { store.edit { prefs -> migrateAppearance(prefs) } }
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
            val style =
                decodeNavStyle(
                    storedStyle = prefs[navStyleKey],
                    storedFloatingBlurEnabled = prefs[legacyNavBlurKey],
                    legacyBlurEnabled = prefs[legacyBlurKey],
                    legacyGlassEnabled = prefs[legacyGlassKey],
                )
            val colorSpec = decodeColorSpec(prefs[colorSpecKey])
            Appearance(
                theme = theme.mode,
                topBarProgressiveBlur = prefs[topBarProgressiveBlurKey] ?: true,
                dynamicColor = theme.dynamicColor,
                customPalette = decodeCustomPalette(prefs[customPaletteKey], colorSpec),
                colorSpec = colorSpec ?: ThemeColorSpec.Spec2021,
                paletteStyle = decodePalette(prefs[paletteStyleKey]),
                navEnabled = prefs[navEnabledKey] ?: true,
                navTouchExtension = prefs[navTouchExtensionKey] ?: true,
                navStyle = style,
                navContent = decodeNavContent(prefs[navContentKey]),
                liquidMode = decodeLiquidMode(prefs[liquidModeKey]),
                swipeBack = prefs[swipeBackKey] ?: true,
                uiScalePercent = uiScalePercent(prefs[uiScaleKey]),
            )
        }

    suspend fun setTopBarProgressiveBlur(enabled: Boolean) {
        store.edit { prefs -> prefs[topBarProgressiveBlurKey] = enabled }
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

    suspend fun setCustomPalette(enabled: Boolean) {
        store.edit { prefs -> prefs[customPaletteKey] = enabled }
    }

    suspend fun setColorSpec(spec: ThemeColorSpec) {
        store.edit { prefs -> prefs[colorSpecKey] = spec.name }
    }

    suspend fun setPaletteStyle(style: ThemePaletteStyle) {
        store.edit { prefs -> prefs[paletteStyleKey] = style.name }
    }

    suspend fun setNavEnabled(enabled: Boolean) {
        store.edit { prefs ->
            prefs[navEnabledKey] = enabled
        }
    }

    suspend fun setNavTouchExtension(enabled: Boolean) {
        store.edit { prefs -> prefs[navTouchExtensionKey] = enabled }
    }

    suspend fun setNavStyle(style: NavStyle) {
        store.edit { prefs ->
            prefs[navStyleKey] = style.name
            prefs.remove(legacyNavBlurKey)
            prefs.remove(legacyGlassKey)
            prefs.remove(legacyBlurKey)
        }
    }

    suspend fun setNavContent(content: NavContent) {
        store.edit { prefs -> prefs[navContentKey] = content.name }
    }

    suspend fun setLiquidMode(mode: LiquidMode) {
        store.edit { prefs ->
            prefs[liquidModeKey] = mode.stored
        }
    }

    suspend fun setSwipeBack(enabled: Boolean) {
        store.edit { prefs ->
            prefs[swipeBackKey] = enabled
        }
    }

    suspend fun setUiScalePercent(value: Int) {
        store.edit { prefs ->
            prefs[uiScaleKey] = uiScalePercent(value)
        }
    }

    private companion object {
        val themeKey = stringPreferencesKey("theme_mode")
        val dynamicKey = booleanPreferencesKey("dynamic_color_enabled")
        val customPaletteKey = booleanPreferencesKey("custom_palette_enabled")
        val colorSpecKey = stringPreferencesKey("theme_color_spec")
        val paletteStyleKey = stringPreferencesKey("theme_palette_style")
        val legacyBlurKey = booleanPreferencesKey("blur_enabled")
        val legacyGlassKey = booleanPreferencesKey("glass_bottom_bar_enabled")
        val navEnabledKey = booleanPreferencesKey("floating_navigation_bar_enabled")
        val navTouchExtensionKey = booleanPreferencesKey("floating_navigation_touch_extension")
        val liquidModeKey = stringPreferencesKey("floating_navigation_liquid_mode")
        val legacyNavBlurKey = booleanPreferencesKey("floating_navigation_blur_enabled")
        val swipeBackKey = booleanPreferencesKey("swipe_back_enabled")
        val uiScaleKey = intPreferencesKey("ui_scale_percent")
    }
}

private const val LEGACY_DYNAMIC_THEME_MODE = "Dynamic"
