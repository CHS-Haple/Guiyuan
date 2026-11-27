// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.chaners.guiyuan.ui.components.liquid.liquidPanelPressScale
import top.yukonga.miuix.kmp.glass.GlassNavigationRailDefaults
import top.yukonga.miuix.kmp.glass.glassPanel
import top.yukonga.miuix.kmp.glass.glassShadow
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Keep the material/shadow in the same layout and draw phase as upstream. */
@Composable
internal fun Modifier.railPanelSurfaceModifier(
    state: GlassNavigationRailState,
    geometry: RailPanelGeometry,
    motion: RailPanelMotion,
    surface: RailPanelSurface,
    shape: GlassShape,
): Modifier {
    // Do not replace the MIUIX rail's layout, morph, input or lifecycle owner.
    // Only the surface paint differs; the default Glass material path is unchanged.
    val override = surface.replacement?.panel
    val surfacePaint = if (override != null && surface.backdrop != null) {
        // The visible panel material is a sibling child, recorded before the selected lens.
        // Keep its original MIUIX Float shadow here, without double-painting the surface.
        Modifier.glassShadow(shape, GlassShadows.Float, alpha = 1f)
    } else if (override != null) {
        Modifier.glassShadow(shape, GlassShadows.Float, alpha = 1f).then(override(surface.backdrop, shape))
    } else {
        Modifier.glassPanel(
            backdrop = surface.backdrop,
            shape = shape,
            style = GlassDefaults.Style,
            alpha = 1f,
            material = GlassNavigationRailDefaults.material(),
            stroke = GlassNavigationRailDefaults.stroke(),
            shadow = GlassShadows.Float,
            shading = false,
            fallback = if (surface.backdrop == null || !isRuntimeShaderSupported()) {
                Modifier.clip(shape).background(MiuixTheme.colorScheme.surfaceContainer)
            } else {
                null
            },
        )
    }
    return this
        .layout { measurable, _ ->
            val fraction = state.progress.fastCoerceIn(0f, 1f)
            val width = lerp(geometry.endpoints.collapsedWidth.toPx(), geometry.endpoints.expandedWidth.toPx(), fraction).fastRoundToInt()
            val height = motion.panelHeight().roundToPx().coerceAtLeast(0)
            val placeable = measurable.measure(Constraints.fixed(width, height))
            layout(width, height) { placeable.place(0, 0) }
        }
        .offset {
            IntOffset(
                x = motion.panelStart().roundToPx(),
                y = lerp(geometry.endpoints.collapsedTop.toPx(), geometry.endpoints.expandedTop.toPx(), state.progress).fastRoundToInt(),
            )
        }
        // Scale only the placed panel. An outer layer would scale its X offset as well.
        .then(if (override != null) Modifier.graphicsLayer {
            val scale = liquidPanelPressScale(
                size.width, size.height,
                com.chaners.guiyuan.ui.components.liquid.LiquidFeedbackSpec.PressedPanelGrowth.toPx(),
                motion.liquid.press.value,
            )
            scaleX = scale
            scaleY = scale
        } else Modifier)
        .then(surfacePaint)
        // Liquid's material already uses the panel shape. Its pressed lens must
        // extend past the idle panel; Glass keeps the original child clip.
        .then(if (override == null) Modifier.clip(shape) else Modifier)
}
