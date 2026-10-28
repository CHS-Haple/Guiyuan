package com.chaners.guiyuan.ui.components

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
