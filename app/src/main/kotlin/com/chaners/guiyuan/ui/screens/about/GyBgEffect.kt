package com.chaners.guiyuan.ui.screens.about

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import top.yukonga.miuix.kmp.effect.bg.BgEffectBackground
import top.yukonga.miuix.kmp.effect.bg.DeviceType
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GyBgEffect(
    playing: Boolean,
    bgModifier: Modifier = Modifier,
    alpha: () -> Float = { 1f },
) {
    val surface = MiuixTheme.colorScheme.surface
    BgEffectBackground(
        dynamicBackground = playing,
        deviceType = if (LocalConfiguration.current.screenWidthDp >= 600) DeviceType.PAD else DeviceType.PHONE,
        isDarkTheme = surface.luminance() < 0.5f,
        surface = surface,
        modifier = Modifier.fillMaxSize(),
        bgModifier = bgModifier,
        isFullSize = true,
        alpha = alpha,
    ) {}
}
