package com.chaners.guiyuan.xposed

import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeBatterySuppressionTest {
    @Test
    fun replacementKeepsNativeLayout() {
        assertFalse(
            NativeBatterySuppression.resolveNativeLayoutHide(
                nativeRequestedHide = false,
            ),
        )
    }

    @Test
    fun nativeHideStaysAuthoritative() {
        assertTrue(
            NativeBatterySuppression.resolveNativeLayoutHide(
                nativeRequestedHide = true,
            ),
        )
    }

    @Test
    fun suppressionKeepsSlotRemovesGlyph() {
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
    fun suppressionKeepsNativeHidden() {
        assertEquals(
            View.INVISIBLE,
            NativeBatterySuppression.resolveChargingPresentationVisibility(
                nativeVisibility = View.INVISIBLE,
                suppressionActive = true,
            ),
        )
    }

    @Test
    fun inactiveKeepsNativeVisibility() {
        assertEquals(
            View.VISIBLE,
            NativeBatterySuppression.resolveChargingPresentationVisibility(
                nativeVisibility = View.VISIBLE,
                suppressionActive = false,
            ),
        )
    }
}
