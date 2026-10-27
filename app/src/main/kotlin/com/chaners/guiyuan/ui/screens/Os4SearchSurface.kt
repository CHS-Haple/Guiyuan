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
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.glass.GlassShape
import top.yukonga.miuix.kmp.glass.GlassShadows
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.glass.glassPanel
import top.yukonga.miuix.kmp.glass.glassShadow

/** The search geometry stays fixed while only the surface transitions with page overlap. */
@Composable
internal fun Os4SearchSurface(
    backdrop: Backdrop?,
    materialProgress: State<Float>,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = GlassShape(22.dp)
    Box(modifier = modifier.glassShadow(shape, GlassShadows.Regular).clip(CircleShape)) {
        Box(Modifier.matchParentSize().background(GlassTopAppBarDefaults.buttonFill()))
        if (backdrop != null && isRuntimeShaderSupported()) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = materialProgress.value.coerceIn(0f, 1f) }
                    .glassPanel(
                        backdrop = backdrop,
                        shape = shape,
                        material = GlassTopAppBarDefaults.buttonMaterial(),
                        stroke = GlassTopAppBarDefaults.buttonStroke(),
                        shadow = null,
                        shading = true,
                    ),
            )
        }
        content()
    }
}
