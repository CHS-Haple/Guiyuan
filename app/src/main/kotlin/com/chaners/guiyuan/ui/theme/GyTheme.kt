package com.chaners.guiyuan.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.chaners.guiyuan.settings.ThemeMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
internal fun GyTheme(
    themeMode: ThemeMode,
    dynamicColorEnabled: Boolean,
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
    val controller = remember(colorSchemeMode) {
        ThemeController(colorSchemeMode = colorSchemeMode)
    }
    MiuixTheme(controller = controller, content = content)
}
