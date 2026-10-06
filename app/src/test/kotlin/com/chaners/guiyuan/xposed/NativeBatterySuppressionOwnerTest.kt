package com.chaners.guiyuan.xposed

import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeBatterySuppressionOwnerTest {
    @Test
    fun replacementDoesNotOverrideNativeVisibleLayout() {
        assertFalse(
            NativeBatterySuppressionOwner.resolveNativeLayoutHide(
                nativeRequestedHide = false,
            ),
        )
    }

    @Test
    fun nativeHideRemainsAuthoritativeWhileReplacementIsActive() {
        assertTrue(
            NativeBatterySuppressionOwner.resolveNativeLayoutHide(
                nativeRequestedHide = true,
            ),
        )
    }

    @Test
    fun activeSuppressionKeepsChargingSlotButRemovesGlyph() {
        assertEquals(
            View.INVISIBLE,
            NativeBatterySuppressionOwner.resolveChargingVisibility(
                nativeVisibility = View.VISIBLE,
                suppressionActive = true,
            ),
        )
    }

    @Test
    fun activeSuppressionPreservesNativeGoneState() {
        assertEquals(
            View.GONE,
            NativeBatterySuppressionOwner.resolveChargingVisibility(
                nativeVisibility = View.GONE,
                suppressionActive = true,
            ),
        )
    }

    @Test
    fun activeSuppressionPreservesNativeInvisibleState() {
        assertEquals(
            View.INVISIBLE,
            NativeBatterySuppressionOwner.resolveChargingVisibility(
                nativeVisibility = View.INVISIBLE,
                suppressionActive = true,
            ),
        )
    }

    @Test
    fun inactiveSuppressionPreservesNativeVisibility() {
        assertEquals(
            View.VISIBLE,
            NativeBatterySuppressionOwner.resolveChargingVisibility(
                nativeVisibility = View.VISIBLE,
                suppressionActive = false,
            ),
        )
    }
}
