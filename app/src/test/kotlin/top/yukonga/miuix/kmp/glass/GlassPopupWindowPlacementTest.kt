// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

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
    fun dropdownUsesWindowCenterNotHostLocalWidth() {
        val row = Rect(90f, 200f, 470f, 260f)
        val value = Rect(110f, 220f, 140f, 245f)
        val result = placeGlassDropdownPopup(row, value, Size(200f, 160f), margin, window)
        assertEquals(Rect(110f, 220f, 310f, 380f), result.rect)
    }

    @Test
    fun dropdownUsesValueForVerticalFlipNearWindowBottom() {
        val row = Rect(90f, 540f, 470f, 600f)
        val value = Rect(110f, 560f, 140f, 585f)
        val size = Size(200f, 160f)
        val result = placeGlassDropdownPopup(row, value, size, margin, window)
        assertEquals(Rect(110f, 425f, 310f, 585f), result.rect)
        assertEquals(false, result.alignTop)
    }

    @Test
    fun dropdownWithoutValueKeepsRowBasedPlacement() {
        val row = Rect(90f, 540f, 470f, 600f)
        val size = Size(200f, 160f)
        for (direction in LayoutDirection.entries) {
            val original = placeGlassPopup(row, size, margin, window, direction)
            val actual = placeGlassDropdownPopup(row, null, size, margin, window, direction)
            assertEquals(original.rect, actual.rect)
            assertEquals(original.alignTop, actual.alignTop)
        }
    }
}
