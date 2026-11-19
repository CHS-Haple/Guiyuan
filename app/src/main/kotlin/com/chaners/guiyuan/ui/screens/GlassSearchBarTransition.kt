// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

// The published MIUIX glass module cannot coexist with Guiyuan's adapted glass
// namespace. Guiyuan owns this source-matched transition until direct module
// adoption can preserve the accepted host/rail contracts.
private const val SEARCH_TOP_BAR_BLUR_RADIUS = 50f

// Map one second of spring motion onto the 400ms search transition.
internal val SearchModeEasing: Easing = run {
    val omega = 2.0 * PI / 0.75f
    val stiffness = (omega * omega).toFloat()
    val damping = (0.98f * 2.0 * omega).toFloat()
    val decay = -damping / 2f
    val frequency = sqrt(-(damping.toDouble() * damping - stiffness.toDouble() * 4)).toFloat() / 2f
    val sineCoefficient = decay / frequency
    Easing { fraction ->
        (
            1.0 + exp((decay * fraction).toDouble()) *
                (-cos((frequency * fraction).toDouble()) + sineCoefficient * sin((frequency * fraction).toDouble()))
            ).toFloat()
    }
}

/** Shared page opacity keeps content and its sampled top-bar background in sync. */
@Composable
fun rememberGlassSearchBarAlpha(expanded: Boolean): State<Float> {
    val transition = updateTransition(expanded, label = "glassSearchTopBarOpacity")
    return transition.animateFloat(
        transitionSpec = {
            tween(150, easing = if (targetState) CubicBezierEasing(0f, 0f, 0.67f, 1f) else CubicBezierEasing(0.26f, 0f, 0.6f, 0.2f))
        },
        label = "glassSearchTopBarAlpha",
    ) { if (it) 0f else 1f }
}

/** ActionBarView fades, shrinks to 0.9, and blurs while entering search mode. */
@Composable
fun Modifier.glassSearchBarTransition(expanded: Boolean, blurEnabled: Boolean = true, barAlpha: () -> Float): Modifier {
    val density = LocalDensity.current
    val layerOutset = remember(density) {
        val shadow = GlassShadows.Regular
        val shadowOutset = ((shadow.radius + maxOf(abs(shadow.offsetX), abs(shadow.offsetY))) / GlassDefaults.SourceDensity).dp
        // Keep a fixed halo for the shadows and maximum blur throughout the offscreen fade.
        with(density) { shadowOutset.toPx() } + SEARCH_TOP_BAR_BLUR_RADIUS * 3f
    }
    val transition = updateTransition(expanded, label = "glassSearchTopBar")
    val scale = transition.animateFloat(
        transitionSpec = { tween(400, easing = SearchModeEasing) },
        label = "glassSearchTopBarScale",
    ) { if (it) 0.9f else 1f }
    val blurActive = blurEnabled && isRuntimeShaderSupported()
    val blur = transition.animateFloat(
        transitionSpec = { tween(if (targetState) 150 else 120, easing = LinearEasing) },
        label = "glassSearchTopBarBlur",
    ) { if (it && blurActive) SEARCH_TOP_BAR_BLUR_RADIUS else 0f }
    return drawWithCache {
        val layer = obtainGraphicsLayer()
        val halo = ceil(layerOutset).toInt()
        val layerSize = IntSize(size.width.toInt() + halo * 2, size.height.toInt() + halo * 2)
        onDrawWithContent {
            val opacity = barAlpha().coerceIn(0f, 1f)
            val currentScale = scale.value
            val currentBlur = blur.value
            if (opacity > 0f) {
                if (opacity == 1f && currentScale == 1f && currentBlur == 0f) {
                    drawContent()
                } else {
                    // Keep the halo in the recorded bounds: Skiko can replay nested layers with
                    // LayerOutsets without applying their opacity or render effect.
                    layer.record(density = this@drawWithCache, size = layerSize) {
                        translate(halo.toFloat(), halo.toFloat()) { this@record.drawContent() }
                    }
                    layer.alpha = opacity
                    layer.scaleX = currentScale
                    layer.scaleY = currentScale
                    layer.renderEffect = if (blurActive && currentBlur > 0.5f) BlurEffect(currentBlur, currentBlur, TileMode.Decal) else null
                    translate(-halo.toFloat(), -halo.toFloat()) { drawLayer(layer) }
                }
            }
        }
    }
}
