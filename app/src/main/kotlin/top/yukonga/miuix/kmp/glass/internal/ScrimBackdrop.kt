// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass.internal

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BackdropEffectScope

/** Decorates the clean scene in the existing padded GPU recording, before the blur pass. */
@Stable
internal class ScrimBackdrop(
    private val backdrop: Backdrop,
    private val scrimColor: State<Color>,
    private val scale: State<Float>? = null,
) : Backdrop by backdrop {

    // The scale-only path reuses the same callback across GPU recordings.
    private val scaleOnlyLayerBlock: GraphicsLayerScope.() -> Unit = {
        scale?.let {
            scaleX *= it.value
            scaleY *= it.value
        }
    }

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Float,
    ) {
        with(backdrop) {
            drawBackdrop(
                density = density,
                coordinates = coordinates,
                layerBlock = when {
                    scale == null -> layerBlock
                    layerBlock == null -> scaleOnlyLayerBlock
                    else -> {
                        {
                            layerBlock.invoke(this)
                            scaleOnlyLayerBlock.invoke(this)
                        }
                    }
                },
                downscaleFactor = downscaleFactor,
            )
        }
        val color = scrimColor.value
        if (color.alpha > 0f) {
            val padding = (density as? BackdropEffectScope)?.padding ?: 0f
            val scaledPadding = padding / downscaleFactor
            // Recording has translated by padding. Cover that margin too, without filling pixels
            // outside the scene: SrcAtop retains their alpha instead of creating a dark halo.
            drawRect(color, topLeft = Offset(-scaledPadding, -scaledPadding), size = size, blendMode = BlendMode.SrcAtop)
        }
    }
}
