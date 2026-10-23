package com.chaners.guiyuan.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val ProgressiveTopBarBlurRadius = 10f
private const val ProgressiveTopBarCurve = 2.2f
private const val ProgressiveTopBarFullStrengthFraction = 0.70f
private const val ProgressiveTopBarBlendAlpha = 0.3f

@Composable
internal fun rememberTopBarBackdrop(): LayerBackdrop? {
    if (!isRuntimeShaderSupported()) return null

    val surface = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surface)
        drawContent()
    }
}

@Composable
internal fun MiuixBlurredTopBar(
    backdrop: LayerBackdrop?,
    scrollBehavior: ScrollBehavior,
    content: @Composable (Color) -> Unit,
) {
    val blurActive = backdrop != null
    val surface = MiuixTheme.colorScheme.surface

    Box {
        if (blurActive) {
            val blurColors =
                BlurDefaults.blurColors(
                    blendColors =
                        listOf(
                            BlendColorEntry(
                                color = surface.copy(alpha = ProgressiveTopBarBlendAlpha),
                            ),
                        ),
                )

            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            alpha =
                                (-scrollBehavior.state.contentOffset / 48.dp.toPx())
                                    .coerceIn(0f, 1f)
                        }
                        .progressiveTextureBlur(
                            backdrop = backdrop,
                            shape = RectangleShape,
                            gradient =
                                ProgressiveBlur.Top.copy(
                                    startFraction = ProgressiveTopBarFullStrengthFraction,
                                    endFraction = 1f,
                                    curve = ProgressiveTopBarCurve,
                                ),
                            blurRadius = ProgressiveTopBarBlurRadius,
                            colors = blurColors,
                        ),
            )
        }

        content(if (blurActive) Color.Transparent else surface)
    }
}

internal fun Modifier.topBarBackdropSource(backdrop: LayerBackdrop?): Modifier =
    if (backdrop != null) {
        layerBackdrop(backdrop)
    } else {
        this
    }
