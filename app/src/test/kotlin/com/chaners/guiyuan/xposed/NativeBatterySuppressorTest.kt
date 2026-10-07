package com.chaners.guiyuan.xposed

import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Test

class NativeBatterySuppressorTest {
    @Test
    fun activeSuppressionKeepsChargingSlotButRemovesGlyph() {
        assertEquals(
            View.INVISIBLE,
            NativeBatterySuppressor.resolveChargingVisibility(
                nativeVisibility = View.VISIBLE,
                suppressionActive = true,
            ),
        )
    }

    @Test
    fun activeSuppressionPreservesNativeGoneState() {
        assertEquals(
            View.GONE,
            NativeBatterySuppressor.resolveChargingVisibility(
                nativeVisibility = View.GONE,
                suppressionActive = true,
            ),
        )
    }

    @Test
    fun activeSuppressionPreservesNativeInvisibleState() {
        assertEquals(
            View.INVISIBLE,
            NativeBatterySuppressor.resolveChargingVisibility(
                nativeVisibility = View.INVISIBLE,
                suppressionActive = true,
            ),
        )
    }

    @Test
    fun inactiveSuppressionPreservesNativeVisibility() {
        assertEquals(
            View.VISIBLE,
            NativeBatterySuppressor.resolveChargingVisibility(
                nativeVisibility = View.VISIBLE,
                suppressionActive = false,
            ),
        )
    }
}
