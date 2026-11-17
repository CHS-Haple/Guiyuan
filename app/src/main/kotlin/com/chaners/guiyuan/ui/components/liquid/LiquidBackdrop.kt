// Adapted from compose-miuix-ui Liquid Glass example and AndroidLiquidGlass (Apache-2.0).
package com.chaners.guiyuan.ui.components.liquid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import com.chaners.guiyuan.settings.LiquidMode
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.material.drawEdgeStroke
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.colorControls
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

internal fun liquidNavSupported(): Boolean = isRuntimeShaderSupported()

@Composable
internal fun rememberLiquidNavBackdrop(surfaceColor: Color): LayerBackdrop =
    rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

internal fun Modifier.liquidNavBackdropSource(backdrop: LayerBackdrop): Modifier =
    layerBackdrop(backdrop)

internal fun BackdropEffectScope.vibrancy() {
    colorControls(brightness = 0f, contrast = 1f, saturation = 1.5f)
}

@Stable
internal class CombinedBackdrop(
    private val first: Backdrop,
    private val second: Backdrop,
) : Backdrop {
    override val isCoordinatesDependent: Boolean =
        first.isCoordinatesDependent || second.isCoordinatesDependent
    override val offsetResidualX: Float get() = first.offsetResidualX
    override val offsetResidualY: Float get() = first.offsetResidualY

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Float,
    ) {
        with(first) { drawBackdrop(density, coordinates, layerBlock, downscaleFactor) }
        with(second) { drawBackdrop(density, coordinates, layerBlock, downscaleFactor) }
    }
}

@Composable
internal fun rememberCombinedBackdrop(first: Backdrop, second: Backdrop): Backdrop =
    remember(first, second) { CombinedBackdrop(first, second) }

/**
 * Material-only adapter for the OS3 native and OS4 Glass navigation rails.
 * Geometry, expansion, selection, motion and semantics stay with the original rail.
 * This surface is outside the recorded page backdrop, never inside its own source.
 */
@Composable
internal fun Modifier.liquidRailSurface(
    backdrop: Backdrop?,
    shape: CornerBasedShape,
    mode: LiquidMode,
): Modifier {
    val color = MiuixTheme.colorScheme.surfaceContainer
    if (backdrop == null) return this.clip(shape).background(color)
    val params = mode.params()
    val tilt = rememberGlassTilt(interactive = true)
    val rim = remember(tilt) { derivedStateOf { litHighlight(tilt.value, -45f) } }
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            padding = maxOf(padding, 40.dp.toPx())
            vibrancy()
            blur(params.blur.toPx(), params.blur.toPx())
            lens(params.refractionHeight.toPx(), params.refractionAmount.toPx())
        },
        onDrawFront = {
            drawEdgeStroke(shape = shape, stroke = rim.value, alpha = 0.75f)
        },
        onDrawSurface = {
            drawRect(color.copy(alpha = params.surfaceAlpha))
        },
    )
}
