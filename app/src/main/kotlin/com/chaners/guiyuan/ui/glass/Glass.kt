// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.BackdropPipeline
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.effect
import top.yukonga.miuix.kmp.blur.noiseDither
import com.chaners.guiyuan.ui.glass.internal.GLASS_COLOR_BLEND_SHADER_KEY
import com.chaners.guiyuan.ui.glass.internal.buildColorBlendEffect
import top.yukonga.miuix.kmp.material.GlassColorBlendMode
import top.yukonga.miuix.kmp.material.GlassColorLayer
import top.yukonga.miuix.kmp.material.drawEdgeStroke
import top.yukonga.miuix.kmp.material.drawGlassMask
import top.yukonga.miuix.kmp.material.drawGlassShadow
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.material.EdgeStroke
import top.yukonga.miuix.kmp.material.GlassMaterial
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.ShaderBrushCache
import top.yukonga.miuix.kmp.material.GlassShadow

/** Applies a child material over the action bar's already blurred and colour-treated surface. */
internal fun Modifier.glassOnActionBar(
    backdrop: Backdrop,
    shape: GlassShape,
    alpha: Float,
    material: GlassMaterial?,
    underlayMaterial: GlassMaterial,
    stroke: EdgeStroke?,
    enabled: Boolean = true,
): Modifier = drawBackdrop(
    backdrop = backdrop,
    pipeline = GlassPipeline(
        glassShape = shape,
        alpha = alpha,
        material = material,
        underlayMaterial = underlayMaterial,
        stroke = stroke,
    ),
    contentBlendMode = BlendMode.SrcOver,
    enabled = enabled,
)
/**
 * Glass pipeline as a value: equal inputs keep the backdrop node, so a recomposition no longer
 * rebuilds the blur and colour passes. This local action-bar pipeline never adds a rim.
 */
private class GlassPipeline(
    private val glassShape: GlassShape,
    private val alpha: Float,
    private val material: GlassMaterial?,
    private val underlayMaterial: GlassMaterial,
    private val stroke: EdgeStroke?,
) : BackdropPipeline {

    private var cachedUnderlayBlendEffect: RenderEffect? = null
    private var cachedMaterialBlendEffect: RenderEffect? = null
    private val edgeBrushCache = ShaderBrushCache()
    private val maskBrushCache = ShaderBrushCache()

    override val shape: Shape get() = glassShape

    override fun configureEffects(scope: BackdropEffectScope) {
        with(scope) {
            noiseDither(GlassDefaults.NoiseCoefficient)
            val radius = material?.blurRadius?.value ?: (GlassDefaults.Style.blur.small / GlassDefaults.SourceDensity)
            blur(radius.fastCoerceIn(0f, BlurDefaults.MaxBlurRadius) * density)
            val underlay = cachedUnderlayBlendEffect ?: buildColorBlendEffect(
                underlayMaterial,
                1f,
                "${GLASS_COLOR_BLEND_SHADER_KEY}ActionBar",
            )?.also { cachedUnderlayBlendEffect = it }
            if (underlay != null) effect(underlay)
            if (material != null) {
                val blend = cachedMaterialBlendEffect ?: buildColorBlendEffect(material, alpha)
                    ?.also { cachedMaterialBlendEffect = it }
                if (blend != null) effect(blend)
            }
        }
    }

    override fun drawFront(scope: DrawScope) {
        with(scope) {
            // The action-bar underlay already supplies the material; only stroke and mask remain.
            stroke?.let { drawEdgeStroke(glassShape, it, alpha, brushCache = edgeBrushCache) }
            drawGlassMask(glassShape, brushCache = maskBrushCache)
        }
    }

    override fun hasSameBackdropEffects(other: BackdropPipeline): Boolean = other is GlassPipeline &&
        other.glassShape == glassShape &&
        other.alpha == alpha &&
        other.material == material &&
        other.underlayMaterial == underlayMaterial

    override fun equals(other: Any?): Boolean = other is GlassPipeline &&
        hasSameBackdropEffects(other) &&
        other.stroke == stroke

    override fun hashCode(): Int {
        var result = glassShape.hashCode()
        result = 31 * result + alpha.hashCode()
        result = 31 * result + material.hashCode()
        result = 31 * result + underlayMaterial.hashCode()
        result = 31 * result + stroke.hashCode()
        return result
    }

}

internal fun Modifier.glassShadow(
    shape: GlassShape,
    shadow: GlassShadow?,
    alpha: () -> Float,
): Modifier = if (shadow == null) {
    this
} else if (isRuntimeShaderSupported()) {
    // Follow upstream's per-draw-node ShaderBrushCache while retaining live alpha reads.
    this.drawWithCache {
        val brushCache = ShaderBrushCache()
        onDrawBehind { drawGlassShadow(shape, shadow, alpha(), brushCache = brushCache) }
    }
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
    radius = (shadow.radius * sourceScale).fastCoerceAtLeast(1f)
    offset = Offset(shadow.offsetX * sourceScale, shadow.offsetY * sourceScale)
    color = shadow.color
    this.alpha = alpha().fastCoerceIn(0f, 1f)
}

/** Tint floating glass with the active dynamic colour scheme; static themes retain their material. */
@Composable
internal fun dynamicGlassMaterial(
    base: GlassMaterial,
    tint: Color = MiuixTheme.colorScheme.surfaceContainerHigh,
    alpha: Float = 0.6f,
): GlassMaterial = if (MiuixTheme.isDynamicColor) {
    remember(base, tint, alpha) {
        base.copy(third = GlassColorLayer(tint.copy(alpha = alpha), GlassColorBlendMode.SrcOver))
    }
} else {
    base
}
