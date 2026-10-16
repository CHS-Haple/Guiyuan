package com.chaners.guiyuan.ui.theme

import android.content.Context
import android.graphics.Color as AndroidColor
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.chaners.guiyuan.settings.ThemeMode
import org.json.JSONObject
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

@Composable
internal fun GyTheme(
    themeMode: ThemeMode,
    dynamicColorEnabled: Boolean,
    colorSpec: ThemeColorSpec? = null,
    paletteStyle: ThemePaletteStyle = ThemePaletteStyle.TonalSpot,
    content: @Composable () -> Unit,
) {
    val colorSchemeMode =
        when {
            dynamicColorEnabled && themeMode == ThemeMode.System ->
                ColorSchemeMode.MonetSystem
            dynamicColorEnabled && themeMode == ThemeMode.Light ->
                ColorSchemeMode.MonetLight
            dynamicColorEnabled && themeMode == ThemeMode.Dark ->
                ColorSchemeMode.MonetDark
            themeMode == ThemeMode.System ->
                ColorSchemeMode.System
            themeMode == ThemeMode.Light ->
                ColorSchemeMode.Light
            else ->
                ColorSchemeMode.Dark
        }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val seed = remember(context, configuration, dynamicColorEnabled, colorSpec) {
        if (dynamicColorEnabled && colorSpec != null) systemPaletteSeed(context) else null
    }
    val controller = remember(colorSchemeMode, colorSpec, paletteStyle, seed) {
        ThemeController(
            colorSchemeMode = colorSchemeMode,
            keyColor = seed,
            colorSpec = colorSpec ?: ThemeColorSpec.Spec2021,
            paletteStyle = if (colorSpec == null) ThemePaletteStyle.TonalSpot else paletteStyle,
        )
    }
    MiuixTheme(controller = controller, content = content)
}

// MIUIX reads this same system palette setting internally, but doesn't expose the seed.
private fun systemPaletteSeed(context: Context): Color {
    val hex = runCatching {
        val setting = Settings.Secure.getString(
            context.contentResolver,
            "theme_customization_overlay_packages",
        ) ?: ""
        JSONObject(setting).optString("android.theme.customization.system_palette")
    }.getOrNull()
    if (!hex.isNullOrBlank()) {
        runCatching {
            Color(AndroidColor.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
        }.getOrNull()?.let { return it }
    }
    return Color(context.getColor(android.R.color.system_accent1_500))
}
