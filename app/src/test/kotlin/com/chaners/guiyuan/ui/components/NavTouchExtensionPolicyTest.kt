package com.chaners.guiyuan.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class NavTouchExtensionPolicyTest {
    @Test
    fun safeExtensionOnlyUsesWhitespaceAboveGestureInset() {
        assertEquals(12.dp, navTouchExtensionHeight(40.dp, 24.dp))
        assertEquals(8.dp, navTouchExtensionHeight(36.dp, 24.dp))
        assertEquals(0.dp, navTouchExtensionHeight(24.dp, 24.dp))
        assertEquals(0.dp, navTouchExtensionHeight(0.dp, 24.dp))
        assertEquals(12.dp, navTouchExtensionHeight(56.dp, 0.dp))
    }

    @Test
    fun publishedMiuixFloatingBarUsesMeasuredUnequalItemCenters() {
        val host = Rect(0f, 0f, 400f, 140f)
        val bar = Rect(45f, 20f, 355f, 76f)
        val items = listOf(
            Rect(58f, 20f, 102f, 76f),
            Rect(120f, 20f, 235f, 76f),
            Rect(256f, 20f, 340f, 76f),
        )
        val safeBottom = minOf(bar.bottom + 12f, host.bottom - 24f - 4f)
        val choice = { x: Float, y: Float ->
            measuredNavTouchTarget(Offset(x, y), bar, items, safeBottom)
        }
        assertEquals(0, choice(80f, 82f))
        assertEquals(1, choice(160f, 82f))
        assertEquals(2, choice(303f, 82f))
        assertNull(choice(111f, 82f)) // Blank space between actual MIUIX items stays inert.
        assertNull(choice(245f, 82f))
        assertNull(choice(25f, 82f))
        assertNull(choice(160f, 100f))
        assertNull(choice(160f, 74f))
        assertNull(choice(160f, 90f))
        assertNull(choice(160f, 88f))
    }

    @Test
    fun gestureInsetCanSuppressMeasuredFloatingExtension() {
        val host = Rect(0f, 0f, 400f, 102f)
        val bar = Rect(40f, 0f, 360f, 76f)
        val items = listOf(Rect(50f, 0f, 350f, 76f))
        val safeBottom = minOf(bar.bottom + 12f, host.bottom - 24f - 4f)
        assertNull(measuredNavTouchTarget(Offset(200f, 80f), bar, items, safeBottom))
    }

    @Test
    fun glassContentInsetMatchesNativeTabBoundaries() {
        assertEquals(0, navTouchIndex(20f, 0f, 300f, 8f, 3, rtl = false))
        assertEquals(0, navTouchIndex(101f, 0f, 300f, 8f, 3, rtl = false))
        assertEquals(1, navTouchIndex(103f, 0f, 300f, 8f, 3, rtl = false))
        assertEquals(2, navTouchIndex(290f, 0f, 300f, 8f, 3, rtl = false))
    }

    @Test
    fun glassTrailingActionKeepsTheBottomHitStripOnDestinationSlots() {
        // The companion app uses the upstream 54dp button and 12dp action spacing.
        val reserved = 54f + 12f
        val ltr = navTouchHorizontalBounds(352f, 0f, reserved, rtl = false)
        assertEquals(0f to 286f, ltr)
        assertEquals(0, navTouchIndex(16f, ltr.first, ltr.second, 8f, 3, rtl = false))
        assertEquals(1, navTouchIndex(143f, ltr.first, ltr.second, 8f, 3, rtl = false))
        assertEquals(2, navTouchIndex(272f, ltr.first, ltr.second, 8f, 3, rtl = false))
        assertNull(navTouchIndex(310f, ltr.first, ltr.second, 8f, 3, rtl = false))

        val rtl = navTouchHorizontalBounds(352f, 0f, reserved, rtl = true)
        assertEquals(66f to 352f, rtl)
        assertEquals(0, navTouchIndex(340f, rtl.first, rtl.second, 8f, 3, rtl = true))
        assertEquals(2, navTouchIndex(80f, rtl.first, rtl.second, 8f, 3, rtl = true))
        assertNull(navTouchIndex(42f, rtl.first, rtl.second, 8f, 3, rtl = true))
    }

    @Test
    fun trailingActionReceivesItsOwnSafeBottomHitRegionWithoutStealingDestinations() {
        val width = 352f
        val size = 54f
        val reserved = size + 12f
        val ltr = navTouchHorizontalBounds(width, 0f, reserved, rtl = false)
        assertTrue(navTouchActionHit(310f, ltr, reserved, size, rtl = false))
        assertTrue(navTouchActionHit(351f, ltr, reserved, size, rtl = false))
        assertFalse(navTouchActionHit(291f, ltr, reserved, size, rtl = false))
        assertFalse(navTouchActionHit(272f, ltr, reserved, size, rtl = false))
        assertNull(navTouchIndex(310f, ltr.first, ltr.second, 8f, 3, rtl = false))
        assertEquals(2, navTouchIndex(272f, ltr.first, ltr.second, 8f, 3, rtl = false))

        val rtl = navTouchHorizontalBounds(width, 0f, reserved, rtl = true)
        assertTrue(navTouchActionHit(2f, rtl, reserved, size, rtl = true))
        assertTrue(navTouchActionHit(53f, rtl, reserved, size, rtl = true))
        assertFalse(navTouchActionHit(61f, rtl, reserved, size, rtl = true))
        assertFalse(navTouchActionHit(80f, rtl, reserved, size, rtl = true))
        assertNull(navTouchIndex(2f, rtl.first, rtl.second, 8f, 3, rtl = true))
        assertEquals(2, navTouchIndex(80f, rtl.first, rtl.second, 8f, 3, rtl = true))
        assertFalse(navTouchActionHit(310f, ltr, 0f, size, rtl = false))
        assertFalse(navTouchActionHit(310f, ltr, reserved, 0f, rtl = false))
        assertFalse(navTouchActionHit(310f, ltr, 40f, size, rtl = false))
    }

    @Test
    fun trailingSearchExcludesOnlyItsOwnLogicalEndHitRegion() {
        val ltr = navTouchHorizontalBounds(400f, padding = 24f, trailingActionWidth = 66f, rtl = false)
        assertEquals(24f to 310f, ltr)
        assertEquals(2, navTouchIndex(302f, ltr.first, ltr.second, 4f, 3, rtl = false))
        assertNull(navTouchIndex(342f, ltr.first, ltr.second, 4f, 3, rtl = false))

        val rtl = navTouchHorizontalBounds(400f, padding = 24f, trailingActionWidth = 66f, rtl = true)
        assertEquals(90f to 376f, rtl)
        assertEquals(2, navTouchIndex(94f, rtl.first, rtl.second, 4f, 3, rtl = true))
        assertNull(navTouchIndex(40f, rtl.first, rtl.second, 4f, 3, rtl = true))
        assertEquals(24f to 376f, navTouchHorizontalBounds(400f, 24f, 0f, rtl = false))
    }

    @Test
    fun liquidSelectionRespectsOuterInsetAndRtl() {
        assertEquals(0, navTouchIndex(48f, 36f, 324f, 4f, 3, rtl = false))
        assertEquals(2, navTouchIndex(48f, 36f, 324f, 4f, 3, rtl = true))
        assertEquals(2, navTouchIndex(315f, 36f, 324f, 4f, 3, rtl = false))
        assertEquals(0, navTouchIndex(315f, 36f, 324f, 4f, 3, rtl = true))
        assertNull(navTouchIndex(24f, 36f, 324f, 4f, 3, rtl = false))
        assertNull(navTouchIndex(325f, 36f, 324f, 4f, 3, rtl = false))
        assertNull(navTouchIndex(70f, 36f, 324f, 4f, 0, rtl = false))
        assertNull(navTouchIndex(70f, 36f, 42f, 4f, 3, rtl = false))
    }
}
