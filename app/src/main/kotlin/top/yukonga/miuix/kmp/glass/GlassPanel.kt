// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.material.drawEdgeStroke
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported

/**
 * Makes this composable a sheet of glass over [backdrop], with the shadow it casts.
 *
 * A device without runtime shaders has no backdrop to sample, so [fallback] defines the surface.
 *
 * @param backdrop The [Backdrop] behind the glass. `null` uses [fallback].
 * @param shape The silhouette. It drives the shadow, the clip and the shader alike.
 * @param style The material.
 * @param alpha Opacity multiplier for the whole surface, shadow included.
 * @param material The panel's own body. `null` leaves the blurred backdrop as the whole of it.
 * @param stroke Optional bloom stroke along the rim.
 * @param shadow The shadow. `null` removes it.
 * @param shading Whether the surface refracts and shades what it stands on. `true` for a control,
 *   `false` for a bar or a menu.
 * @param fallback What the surface is when there is no backdrop. The default clips to [shape] and
 *   leaves whatever the caller paints next as the body.
 */
fun Modifier.glassPanel(
    backdrop: Backdrop?,
    shape: GlassShape,
    style: GlassStyle = GlassDefaults.Style,
    alpha: Float = 1f,
    material: GlassMaterial? = null,
    stroke: GlassStroke? = null,
    shadow: GlassShadow? = GlassShadows.Regular,
    shading: Boolean = true,
    fallback: Modifier? = null,
): Modifier = if (backdrop == null || !isRuntimeShaderSupported()) {
    this
        .graphicsLayer { this.alpha = alpha }
        .glassShadow(shape, shadow)
        .then(fallback ?: Modifier.clip(shape))
        .drawWithContent {
            drawContent()
            stroke?.let { drawEdgeStroke(shape, it, 1f) }
        }
} else {
    this
        .glassShadow(shape, shadow, alpha)
        .glass(
            backdrop = backdrop,
            shape = shape,
            style = style,
            alpha = alpha,
            material = material,
            stroke = stroke,
            shading = shading,
        )
}
