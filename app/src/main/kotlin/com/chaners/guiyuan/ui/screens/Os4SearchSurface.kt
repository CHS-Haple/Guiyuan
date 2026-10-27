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
import top.yukonga.miuix.kmp.glass.GlassDefaults
import top.yukonga.miuix.kmp.glass.glassOnActionBar
import top.yukonga.miuix.kmp.glass.glassShadow
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** The search geometry stays fixed while only the surface transitions with page overlap. */
@Composable
internal fun Os4SearchSurface(
    backdrop: Backdrop?,
    materialProgress: State<Float>,
    shadowProgress: State<Float>,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = GlassShape(22.dp)
    val material = GlassTopAppBarDefaults.buttonMaterial().copy(
        blurRadius = GlassTopAppBarDefaults.nestedMaterialBlurRadius(),
    )
    Box(
        modifier = modifier
            .glassShadow(shape, GlassShadows.Regular) { shadowProgress.value }
            .clip(CircleShape),
    ) {
        Box(Modifier.matchParentSize().background(MiuixTheme.colorScheme.surfaceContainerHigh))
        if (backdrop != null && isRuntimeShaderSupported()) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = materialProgress.value.coerceIn(0f, 1f) }
                    .glassOnActionBar(
                        backdrop = backdrop,
                        shape = shape,
                        style = GlassDefaults.Style,
                        alpha = 1f,
                        material = material,
                        underlayMaterial = GlassTopAppBarDefaults.actionBarUnderlayMaterial(),
                        stroke = GlassTopAppBarDefaults.buttonStroke(),
                    ),
            )
        }
        content()
    }
}
