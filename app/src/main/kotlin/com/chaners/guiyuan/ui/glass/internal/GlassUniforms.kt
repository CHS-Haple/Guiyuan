// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.glass.internal

import androidx.compose.ui.graphics.RenderEffect
import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.buildRuntimeShaderEffect
import top.yukonga.miuix.kmp.material.GlassColorBlendMode
import top.yukonga.miuix.kmp.material.GlassColorLayer
import top.yukonga.miuix.kmp.material.GlassMaterial
import top.yukonga.miuix.kmp.material.RuntimeShader

/** Creates a reusable colour-blend effect independent of surface dimensions. */
internal fun BackdropEffectScope.buildColorBlendEffect(
    material: GlassMaterial,
    alpha: Float,
    key: String = GLASS_COLOR_BLEND_SHADER_KEY,
): RenderEffect? {
    val first = material.first
    val second = material.second
    val third = material.third
    val count = 1 + (if (second != null) 1 else 0) + (if (third != null) 1 else 0)
    val surfaceAlpha = alpha.coerceIn(0f, 1f)
    return buildRuntimeShaderEffect(key, GLASS_COLOR_BLEND_SHADER, "child") {
        setFloatUniform("in_blend0", first.color.red, first.color.green, first.color.blue, first.color.alpha * surfaceAlpha)
        setLayerUniform(1, second, surfaceAlpha)
        setLayerUniform(2, third, surfaceAlpha)
        setFloatUniform(
            "in_blendMode",
            first.mode.id,
            second?.mode?.id ?: 0f,
            third?.mode?.id ?: 0f,
            count.toFloat(),
        )
    }
}

/** Writes one optional colour layer, or clears the slot when [layer] is absent. */
private fun RuntimeShader.setLayerUniform(index: Int, layer: GlassColorLayer?, surfaceAlpha: Float) {
    if (layer == null) {
        setFloatUniform("in_blend$index", 0f, 0f, 0f, 0f)
    } else {
        setFloatUniform("in_blend$index", layer.color.red, layer.color.green, layer.color.blue, layer.color.alpha * surfaceAlpha)
    }
}

/** Blend-mode ID; must match the switch in `GLASS_COLOR_BLEND_SHADER`. */
private val GlassColorBlendMode.id: Float
    get() = when (this) {
        GlassColorBlendMode.SrcOver -> 0f
        GlassColorBlendMode.PlusDarker -> 1f
        GlassColorBlendMode.PlusLighter -> 2f
        GlassColorBlendMode.SoftLight -> 3f
        GlassColorBlendMode.HardLight -> 4f
        GlassColorBlendMode.Overlay -> 5f
        GlassColorBlendMode.Luminosity -> 6f
        GlassColorBlendMode.ColorDodge -> 7f
        GlassColorBlendMode.ColorBurn -> 8f
    }
