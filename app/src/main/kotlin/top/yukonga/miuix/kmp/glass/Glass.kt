// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSpecified
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.BackdropPipeline
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.noiseDither
import top.yukonga.miuix.kmp.glass.internal.GLASS_COLOR_BLEND_SHADER_KEY
import top.yukonga.miuix.kmp.glass.internal.colorBlendEffect
import top.yukonga.miuix.kmp.material.drawEdgeStroke
import top.yukonga.miuix.kmp.material.drawGlassEdge
import top.yukonga.miuix.kmp.material.drawGlassMask
import top.yukonga.miuix.kmp.material.drawGlassRim
import top.yukonga.miuix.kmp.material.drawGlassShadow
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.glass.internal.glassEffect

/**
 * Renders this composable as a sheet of glass over [backdrop].
 *
 * @param backdrop The content behind the glass. This composable must sit *outside* the subtree the
 *   backdrop records — inside it the two form a cycle.
 * @param shape The silhouette. Drives the clip, the [stroke] and the shader, so all three agree.
 * @param style The material. Pick one from [GlassStyles] or start from [GlassDefaults.style].
 * @param alpha Opacity multiplier for the colour layers, shading and stroke.
 * @param tint Overrides [GlassInner.tint]; its alpha replaces the style's tint strength.
 *   [Color.Unspecified] keeps the style's own tint.
 * @param material The panel's own body: a blur radius plus the colour layers over it. It overrides
 *   the blur radius [style] asks for. `null` leaves the blurred backdrop as the whole body.
 * @param noiseCoefficient Noise dithering for the backdrop blur, which prevents banding over large
 *   flat areas. 0 disables it.
 * @param stroke Optional bloom stroke along the rim. `null` skips it.
 * @param contentBlendMode How this composable's content composites over the glass.
 * @param enabled Whether the material is active. When false the content draws on its own.
 * @param shading Whether the surface refracts and shades what it stands on. `true` for a control,
 *   `false` for a bar or a menu whose body is [material] alone.
 */
fun Modifier.glass(
    backdrop: Backdrop,
    shape: GlassShape,
    style: GlassStyle = GlassDefaults.Style,
    alpha: Float = 1f,
    tint: Color = Color.Unspecified,
    material: GlassMaterial? = null,
    noiseCoefficient: Float = GlassDefaults.NoiseCoefficient,
    stroke: GlassStroke? = null,
    contentBlendMode: BlendMode = BlendMode.SrcOver,
    enabled: Boolean = true,
    shading: Boolean = true,
): Modifier = glassImpl(
    backdrop = backdrop,
    shape = shape,
    style = style,
    alpha = alpha,
    tint = tint,
    material = material,
    underlayMaterial = null,
    noiseCoefficient = noiseCoefficient,
    stroke = stroke,
    contentBlendMode = contentBlendMode,
    enabled = enabled,
    shading = shading,
)

/** Applies a child material over the action bar's already blurred and colour-treated surface. */
internal fun Modifier.glassOnActionBar(
    backdrop: Backdrop,
    shape: GlassShape,
    style: GlassStyle,
    alpha: Float,
    material: GlassMaterial?,
    underlayMaterial: GlassMaterial,
    stroke: GlassStroke?,
    enabled: Boolean = true,
): Modifier = glassImpl(
    backdrop = backdrop,
    shape = shape,
    style = style,
    alpha = alpha,
    tint = Color.Unspecified,
    material = material,
    underlayMaterial = underlayMaterial,
    noiseCoefficient = GlassDefaults.NoiseCoefficient,
    stroke = stroke,
    contentBlendMode = BlendMode.SrcOver,
    enabled = enabled,
    shading = false,
)

private fun Modifier.glassImpl(
    backdrop: Backdrop,
    shape: GlassShape,
    style: GlassStyle,
    alpha: Float,
    tint: Color,
    material: GlassMaterial?,
    underlayMaterial: GlassMaterial?,
    noiseCoefficient: Float,
    stroke: GlassStroke?,
    contentBlendMode: BlendMode,
    enabled: Boolean,
    shading: Boolean,
): Modifier = drawBackdrop(
    backdrop = backdrop,
    pipeline = GlassPipeline(
        glassShape = shape,
        style = style,
        alpha = alpha,
        tint = tint,
        material = material,
        underlayMaterial = underlayMaterial,
        noiseCoefficient = noiseCoefficient,
        stroke = stroke,
        shading = shading,
    ),
    contentBlendMode = contentBlendMode,
    enabled = enabled,
)

/**
 * Glass pipeline as a value: equal inputs keep the backdrop node, so a recomposition no longer
 * rebuilds the blur, colour and shading passes.
 */
private data class GlassPipeline(
    private val glassShape: GlassShape,
    private val style: GlassStyle,
    private val alpha: Float,
    private val tint: Color,
    private val material: GlassMaterial?,
    private val underlayMaterial: GlassMaterial?,
    private val noiseCoefficient: Float,
    private val stroke: GlassStroke?,
    private val shading: Boolean,
) : BackdropPipeline {

    override val shape: Shape get() = glassShape

    override fun configureEffects(scope: BackdropEffectScope) {
        with(scope) {
            noiseDither(noiseCoefficient)
            val radius = material?.blurRadius?.value ?: (style.blur.small / GlassDefaults.SourceDensity)
            blur(radius.coerceIn(0f, BlurDefaults.MaxBlurRadius) * density)
            if (underlayMaterial != null) {
                colorBlendEffect(underlayMaterial, 1f, "${GLASS_COLOR_BLEND_SHADER_KEY}ActionBar")
            }
            if (material != null) colorBlendEffect(material, alpha)
            if (shading) glassEffect(style, glassShape, alpha, tint)
        }
    }

    override fun drawFront(scope: DrawScope) {
        with(scope) {
            // Stroke and rim share one silhouette pass; the mask trims it afterwards.
            val current = stroke
            val fuse = current != null && shading &&
                current.width.value > 0f &&
                current.blendMode == BlendMode.Plus
            if (current != null && fuse) {
                drawGlassEdge(
                    shape = glassShape,
                    stroke = current,
                    style = style,
                    surface = if (tint.isSpecified) tint else style.inner.tint,
                    alpha = alpha,
                )
                drawGlassMask(glassShape)
            } else {
                if (current != null) drawEdgeStroke(glassShape, current, alpha)
                drawGlassMask(glassShape)
                if (shading) {
                    drawGlassRim(glassShape, style, if (tint.isSpecified) tint else style.inner.tint, alpha)
                }
            }
        }
    }

}

/**
 * Casts [shadow] under this composable, in the shape of [shape].
 *
 * @param shape The silhouette casting the shadow. Use the same one [glass] gets.
 * @param shadow The shadow. Pick one from [GlassShadows]. `null` skips it.
 * @param alpha Opacity multiplier, to fade the shadow with the surface it belongs to.
 */
fun Modifier.glassShadow(
    shape: GlassShape,
    shadow: GlassShadow? = GlassShadows.Regular,
    alpha: Float = 1f,
): Modifier = glassShadow(shape, shadow) { alpha }

internal fun Modifier.glassShadow(
    shape: GlassShape,
    shadow: GlassShadow?,
    alpha: () -> Float,
): Modifier = if (shadow == null) {
    this
} else if (isRuntimeShaderSupported()) {
    this.drawBehind { drawGlassShadow(shape, shadow, alpha()) }
} else {
    fallbackGlassShadow(shape, shadow, alpha)
}

/** Draw-based fallback uses a different blur kernel from the runtime shader. */
internal fun Modifier.fallbackGlassShadow(
    shape: GlassShape,
    shadow: GlassShadow,
    alpha: () -> Float,
): Modifier = dropShadow(shape) {
    val sourceScale = density / GlassDefaults.SourceDensity
    radius = (shadow.radius * sourceScale).coerceAtLeast(1f)
    offset = Offset(shadow.offsetX * sourceScale, shadow.offsetY * sourceScale)
    color = shadow.color
    this.alpha = alpha().coerceIn(0f, 1f)
}
