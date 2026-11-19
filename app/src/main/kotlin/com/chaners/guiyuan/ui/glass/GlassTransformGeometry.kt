// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.glass

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp

/** Simplified anchors omit the opening copy but retain it during exit. */
internal fun shouldRenderAnchorContent(simplified: Boolean, show: Boolean): Boolean = !simplified || !show

/** Retained exit content is visual only, even before the transition starts its first exit frame. */
internal fun isTransformPopupInteractive(show: Boolean, stacked: Boolean): Boolean = show && !stacked

/** Visibility follows the anchor and motion independently of material colour-layer opacity. */
internal fun transformPanelAlpha(
    floating: Boolean,
    anchorAlpha: Float,
    geometryProgress: Float,
    iconMaterial: Float,
): Float = if (floating) {
    lerp(anchorAlpha, 1f, geometryProgress.coerceIn(0f, 1f))
} else {
    iconMaterial
}

/** Interpolates bounds from the anchor to the expanded menu. */
internal fun transformFrame(
    anchor: Rect,
    end: Size,
    window: Rect,
    margin: Float,
    gap: Float,
    sizeFraction: Float,
    positionFraction: Float,
    startRadius: Dp,
    endRadius: Dp,
    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
): GlassPopupFrame {
    val placement = placeGlassPopup(anchor.translate(0f, gap), end, margin, window, layoutDirection)
    val width = anchor.width + (end.width - anchor.width) * sizeFraction
    val height = anchor.height + (end.height - anchor.height) * sizeFraction
    val centerX = anchor.center.x + (placement.rect.center.x - anchor.center.x) * positionFraction
    val centerY = anchor.center.y + (placement.rect.center.y - anchor.center.y) * positionFraction
    return GlassPopupFrame(
        rect = Rect(
            left = centerX - width / 2f,
            top = centerY - height / 2f,
            right = centerX + width / 2f,
            bottom = centerY + height / 2f,
        ),
        cornerRadius = startRadius + (endRadius - startRadius) * sizeFraction,
    )
}
