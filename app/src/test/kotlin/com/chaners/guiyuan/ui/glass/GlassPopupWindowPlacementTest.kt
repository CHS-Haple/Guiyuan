// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.chaners.guiyuan.ui.glass

import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test

/** Window-space positions must not be clamped against a host-local zero origin. */
class GlassPopupWindowPlacementTest {
    private val window = Rect(80f, 40f, 480f, 640f)
    private val margin = 12f

    @Test
    fun popupClampsToInsetWindowEdges() {
        val anchor = Rect(440f, 610f, 475f, 630f)
        val result = placeGlassPopup(
            anchor,
            Size(200f, 240f),
            margin,
            window,
            LayoutDirection.Ltr,
        )
        assertEquals(Rect(268f, 388f, 468f, 628f), result.rect)
        assertEquals(false, result.alignTop)
    }

    @Test
    fun secondaryPopupClampsWithinOffsetHostSafeArea() {
        val anchor = Rect(400f, 580f, 470f, 620f)
        val result = placeGlassSecondaryPopup(
            anchor,
            Size(250f, 280f),
            margin,
            window,
            LayoutDirection.Ltr,
        )
        assertEquals(Rect(218f, 348f, 468f, 628f), result)
    }

    @Test
    fun toolbarPopupAlignmentMirrorsWithLayoutDirection() {
        val anchor = Rect(220f, 100f, 260f, 140f)
        val panel = Size(120f, 100f)
        fun left(direction: LayoutDirection, alignment: Alignment.Horizontal): Float =
            placeGlassPopup(anchor, panel, margin, window, direction, alignment).rect.left

        assertEquals(220f, left(LayoutDirection.Ltr, Alignment.Start), 0f)
        assertEquals(140f, left(LayoutDirection.Rtl, Alignment.Start), 0f)
        assertEquals(180f, left(LayoutDirection.Ltr, Alignment.CenterHorizontally), 0f)
        assertEquals(180f, left(LayoutDirection.Rtl, Alignment.CenterHorizontally), 0f)
        assertEquals(140f, left(LayoutDirection.Ltr, Alignment.End), 0f)
        assertEquals(220f, left(LayoutDirection.Rtl, Alignment.End), 0f)
    }

    @Test
    fun startAlignmentStillClampsToWindowSafeMargins() {
        val result = placeGlassPopup(
            Rect(440f, 100f, 475f, 140f),
            Size(120f, 100f),
            margin,
            window,
            LayoutDirection.Ltr,
            Alignment.Start,
        )
        assertEquals(348f, result.rect.left, 0f)
    }

    // Native MIUIX now owns GlassDropdownPopup positioning; its upstream tests cover that path.

    @Test
    fun popupNearTopEdgeUsesItsActualAnchorPosition() {
        val anchor = Rect(100f, 44f, 180f, 64f)
        val result = placeGlassPopup(anchor, Size(180f, 100f), margin, window, LayoutDirection.Ltr)
        assertEquals(44f, result.rect.top)
        assertEquals(true, result.alignTop)
    }
}
