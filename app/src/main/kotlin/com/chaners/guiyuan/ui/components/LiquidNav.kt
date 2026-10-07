package com.chaners.guiyuan.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.settings.NavStyle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.isRuntimeShaderSupported

internal val NavStyle.requiresLiquidBackdrop: Boolean
    get() = this == NavStyle.Liquid

internal fun liquidNavSupported(): Boolean = isRuntimeShaderSupported()

@Composable
internal fun rememberLiquidNavBackdrop(surfaceColor: Color): LayerBackdrop =
    rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

internal fun Modifier.liquidNavBackdropSource(backdrop: LayerBackdrop): Modifier =
    layerBackdrop(backdrop)

internal fun Modifier.liquidNavMaterial(
    backdrop: Backdrop,
    dark: Boolean,
): Modifier =
    drawBackdrop(
        backdrop = backdrop,
        shape = { RoundedCornerShape(percent = 50) },
        effects = {
            // Match Backdrop's own LiquidBottomTabs material recipe.
            vibrancy()
            blur(8.dp.toPx())
            lens(
                refractionHeight = 24.dp.toPx(),
                refractionAmount = 24.dp.toPx(),
            )
        },
        onDrawSurface = {
            drawRect(
                if (dark) {
                    Color(0xFF121212).copy(alpha = 0.40f)
                } else {
                    Color(0xFFFAFAFA).copy(alpha = 0.40f)
                },
            )
        },
    )
