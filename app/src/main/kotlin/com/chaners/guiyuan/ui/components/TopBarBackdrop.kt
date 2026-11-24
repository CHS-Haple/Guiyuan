package com.chaners.guiyuan.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun rememberTopBarBackdrop(): LayerBackdrop? {
    // Glass controls still need the sampled backdrop when progressive blur is off.
    if (!isRuntimeShaderSupported()) return null

    val surface = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surface)
        drawContent()
    }
}

internal fun Modifier.topBarBackdropSource(backdrop: LayerBackdrop?): Modifier =
    if (backdrop != null) {
        layerBackdrop(backdrop)
    } else {
        this
    }
