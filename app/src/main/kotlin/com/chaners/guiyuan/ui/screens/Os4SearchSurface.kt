package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.glass.GlassMaterials
import top.yukonga.miuix.kmp.glass.GlassShape
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.glass.glassPanel
import top.yukonga.miuix.kmp.theme.MiuixTheme
/** The search geometry stays fixed while only the surface transitions with page overlap. */
@Composable
internal fun Os4SearchSurface(
    backdrop: Backdrop?,
    materialProgress: State<Float>,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Box(modifier = modifier.clip(CircleShape)) {
        Box(Modifier.matchParentSize().background(colors.surfaceContainerHigh))
        if (backdrop != null && isRuntimeShaderSupported()) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = materialProgress.value.coerceIn(0f, 1f) }
                    .glassPanel(
                        backdrop = backdrop,
                        shape = GlassShape(22.dp),
                        material = GlassMaterials.puredThinGlass(
                            colors.background.luminance() < 0.5f,
                        ),
                        stroke = GlassTopAppBarDefaults.buttonStroke(),
                        shadow = null,
                        shading = false,
                    ),
            )
        }
        content()
    }
}
