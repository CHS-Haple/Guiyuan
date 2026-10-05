package com.chaners.guiyuan.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.chaners.guiyuan.settings.AppThemeMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
internal fun GyTheme(
    themeMode: AppThemeMode,
    dynamicColorEnabled: Boolean,
    content: @Composable () -> Unit,
) {
    val colorSchemeMode =
        when {
            dynamicColorEnabled && themeMode == AppThemeMode.System ->
                ColorSchemeMode.MonetSystem
            dynamicColorEnabled && themeMode == AppThemeMode.Light ->
                ColorSchemeMode.MonetLight
            dynamicColorEnabled && themeMode == AppThemeMode.Dark ->
                ColorSchemeMode.MonetDark
            themeMode == AppThemeMode.System ->
                ColorSchemeMode.System
            themeMode == AppThemeMode.Light ->
                ColorSchemeMode.Light
            else ->
                ColorSchemeMode.Dark
        }
    val controller = remember(colorSchemeMode) {
        ThemeController(colorSchemeMode = colorSchemeMode)
    }
    MiuixTheme(controller = controller, content = content)
}
