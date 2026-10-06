package com.chaners.guiyuan.xposed

import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeBatterySuppressionTest {
    @Test
    fun replacementDoesNotOverrideNativeVisibleLayout() {
        assertFalse(
            NativeBatterySuppression.resolveNativeLayoutHide(
                nativeRequestedHide = false,
            ),
        )
    }

    @Test
    fun nativeHideRemainsAuthoritativeWhileReplacementIsActive() {
        assertTrue(
            NativeBatterySuppression.resolveNativeLayoutHide(
                nativeRequestedHide = true,
            ),
        )
    }

    @Test
    fun activeSuppressionKeepsChargingSlotButRemovesGlyph() {
        assertEquals(
            View.INVISIBLE,
            NativeBatterySuppression.resolveChargingPresentationVisibility(
                nativeVisibility = View.VISIBLE,
                suppressionActive = true,
            ),
        )
    }

    @Test
    fun activeSuppressionPreservesNativeGoneState() {
        assertEquals(
            View.GONE,
            NativeBatterySuppression.resolveChargingPresentationVisibility(
                nativeVisibility = View.GONE,
                suppressionActive = true,
            ),
        )
    }

    @Test
    fun activeSuppressionPreservesNativeInvisibleState() {
        assertEquals(
            View.INVISIBLE,
            NativeBatterySuppression.resolveChargingPresentationVisibility(
                nativeVisibility = View.INVISIBLE,
                suppressionActive = true,
            ),
        )
    }

    @Test
    fun inactiveSuppressionPreservesNativeVisibility() {
        assertEquals(
            View.VISIBLE,
            NativeBatterySuppression.resolveChargingPresentationVisibility(
                nativeVisibility = View.VISIBLE,
                suppressionActive = false,
            ),
        )
    }
}
