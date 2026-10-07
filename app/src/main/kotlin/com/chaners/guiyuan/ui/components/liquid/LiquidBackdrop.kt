package com.chaners.guiyuan.ui.components.liquid

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.isRuntimeShaderSupported

internal fun liquidNavSupported(): Boolean = isRuntimeShaderSupported()

@Composable
internal fun rememberLiquidNavBackdrop(
    surfaceColor: Color,
): LayerBackdrop =
    rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

internal fun Modifier.liquidNavBackdropSource(
    backdrop: LayerBackdrop,
): Modifier =
    layerBackdrop(backdrop)
