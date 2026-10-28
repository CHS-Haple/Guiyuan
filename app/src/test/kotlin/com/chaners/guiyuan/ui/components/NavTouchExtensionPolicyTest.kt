package com.chaners.guiyuan.ui.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        val choice = { x: Float, y: Float ->
            measuredNavTouchTarget(x, y, host, bar, items, 24f, 12f, 4f)
        }
        assertEquals(0, choice(80f, 82f))
        assertEquals(1, choice(160f, 82f))
        assertEquals(2, choice(303f, 82f))
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
        assertNull(measuredNavTouchTarget(200f, 80f, host, bar, items, 24f, 12f, 4f))
    }

    @Test
    fun glassContentInsetMatchesNativeTabBoundaries() {
        assertEquals(0, navTouchIndex(20f, 0f, 300f, 8f, 3, rtl = false))
        assertEquals(0, navTouchIndex(101f, 0f, 300f, 8f, 3, rtl = false))
        assertEquals(1, navTouchIndex(103f, 0f, 300f, 8f, 3, rtl = false))
        assertEquals(2, navTouchIndex(290f, 0f, 300f, 8f, 3, rtl = false))
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
