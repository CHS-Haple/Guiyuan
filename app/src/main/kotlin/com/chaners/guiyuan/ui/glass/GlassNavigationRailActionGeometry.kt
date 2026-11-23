// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.glass

import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.chaners.guiyuan.ui.components.liquid.LiquidFeedbackSpec
import com.chaners.guiyuan.ui.components.liquid.liquidPanelPressScale
import top.yukonga.miuix.kmp.glass.GlassNavigationBarDefaults
import top.yukonga.miuix.kmp.glass.GlassNavigationRailDefaults

internal class RailPlacement(
    val geometry: RailPanelGeometry,
    val motion: RailPanelMotion,
)

/** One derived layout coordinate shared by the panel, lens and independent action. */
internal fun Density.railCollapsedVisualCenterOffset(
    progress: Float,
    actionFraction: Float,
    collapsedWidth: Dp,
    panelHeight: Dp,
    liquid: RailLiquidMotion?,
): Dp {
    val fraction = progress.fastCoerceIn(0f, 1f)
    val collapsed = 1f - fraction
    val reserve = (GlassNavigationBarDefaults.ActionSpacing + GlassNavigationRailDefaults.CollapsedHeight).toPx()
    val groupShift = railCollapsedActionGroupShift(reserve, actionFraction, collapsed)
    if (liquid == null || actionFraction <= 0f) return groupShift.toDp()

    val width = collapsedWidth +
        (GlassNavigationRailDefaults.ExpandedWidth - collapsedWidth) * fraction
    val widthPx = width.toPx()
    val scale = liquidPanelPressScale(
        widthPx, panelHeight.toPx(), LiquidFeedbackSpec.PressedPanelGrowth.toPx(), liquid.press.value,
    )
    val lensShift = railLiquidGroupCenterCorrection(
        liquid.lensEdge.overhangPx(widthPx, scale, LiquidFeedbackSpec.PressedLensGrowth.toPx()),
        collapsed,
    )
    return (groupShift + lensShift).toDp()
}

/** Reserve the independent action inside the same visual centre as the capsule. */
internal fun railCollapsedActionGroupShift(reservedWidthPx: Float, actionFraction: Float, collapsedFraction: Float): Float =
    -reservedWidthPx * actionFraction.fastCoerceIn(0f, 1f) * collapsedFraction.fastCoerceIn(0f, 1f) / 2f

/** One right-overhanging lens must not move the centre of the capsule-action group. */
internal fun railLiquidGroupCenterCorrection(endOverhangPx: Float, collapsedFraction: Float): Float =
    -endOverhangPx.coerceAtLeast(0f) * collapsedFraction.fastCoerceIn(0f, 1f) / 2f

/** Render the action next to the existing rail panel at its physical press edge. */
internal fun Density.railTrailingActionOffset(
    progress: Float,
    placement: RailPlacement,
    actionSize: Dp,
    liquid: Boolean,
): IntOffset {
    val capsuleWidth = placement.geometry.endpoints.collapsedWidth
    val panelWidth = capsuleWidth +
        (GlassNavigationRailDefaults.ExpandedWidth - capsuleWidth) * progress
    val panelStart = placement.motion.panelStart()
    val panelHeightPx = placement.motion.panelHeight().toPx()
    val panelWidthPx = panelWidth.toPx()
    val actionSizePx = actionSize.toPx()
    val x = (panelStart + railSearchActionSideOffset(panelWidth, progress, actionSize)).toPx()
    val y = (GlassNavigationRailDefaults.CollapsedTop + RailExpandedToggleTop * progress).toPx()

    val press = if (liquid) placement.motion.liquid.press.value.fastCoerceIn(0f, 1f) else 0f
    val scale = if (liquid) {
        liquidPanelPressScale(
            panelWidthPx, panelHeightPx, LiquidFeedbackSpec.PressedPanelGrowth.toPx(), press,
        )
    } else 1f
    val lensOverhang = if (liquid) placement.motion.liquid.lensEdge.overhangPx(
        panelWidthPx, scale, LiquidFeedbackSpec.PressedLensGrowth.toPx(),
    ) else 0f

    return IntOffset(
        x = (x + railSearchActionPlannedShift(
            x + actionSizePx / 2f - panelStart.toPx() - panelWidthPx / 2f,
            panelWidthPx / 2f, scale, 1f - progress, lensOverhang,
        )).fastRoundToInt(),
        y = (y + railSearchActionPressShift(
            y + actionSizePx / 2f,
            GlassNavigationRailDefaults.CollapsedTop.toPx() + panelHeightPx / 2f,
            scale,
        )).fastRoundToInt(),
    )
}

/** A point follows the panel's existing centre-origin scale; no independent press animation. */
internal fun railSearchActionPressShift(actionCenter: Float, panelCenter: Float, scale: Float): Float =
    (actionCenter - panelCenter) * (scale - 1f)

/** Collapsed: follow the actual visible end. Expanded: preserve the native header's scale. */
internal fun railSearchActionPlannedShift(
    distanceFromPanelCenter: Float,
    panelHalfWidthPx: Float,
    scale: Float,
    collapsedFraction: Float,
    lensOverhangPx: Float,
): Float {
    val nativeShift = distanceFromPanelCenter * (scale - 1f)
    val visibleEndShift = panelHalfWidthPx * (scale - 1f) + lensOverhangPx
    return lerp(nativeShift, visibleEndShift, collapsedFraction.fastCoerceIn(0f, 1f))
}

/** Logical end of the capsule when collapsed; right-side header slot when expanded. */
internal fun railSearchActionSideOffset(panelWidth: Dp, progress: Float, actionSize: Dp): Dp {
    val fraction = progress.fastCoerceIn(0f, 1f)
    return panelWidth + GlassNavigationBarDefaults.ActionSpacing * (1f - fraction) -
        (RailExpandedToggleStart + actionSize) * fraction
}
