package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CcHomeHandoffPolicyTest {
    private fun yieldToCc(visible: Boolean, fraction: Float) =
        CcHomeHandoffPolicy.yieldToNativePanel(
            nativePanelVisible = visible,
            nativeExpansion = fraction,
        )

    @Test
    fun slowCloseRestoresHomeAtZeroBeforeNativeVisibleFalse() {
        assertTrue(yieldToCc(visible = true, fraction = 0.65f))
        assertTrue(yieldToCc(visible = true, fraction = 0.12f))
        // Slow drag reaches native zero while visible callback remains true.
        assertFalse(yieldToCc(visible = true, fraction = 0f))
        assertFalse(yieldToCc(visible = false, fraction = 0f))
    }

    @Test
    fun delayedReadyCallbackCannotHideHomeAfterSlowClose() {
        assertFalse(yieldToCc(visible = true, fraction = 0f))
        assertFalse(yieldToCc(visible = true, fraction = 0f))
    }

    @Test
    fun partialPullRetainsNativeCcOwnershipAfterProjectionFailure() {
        assertTrue(yieldToCc(visible = true, fraction = 0.4f))
        assertTrue(yieldToCc(visible = true, fraction = 0.9114128f))
        assertFalse(yieldToCc(visible = true, fraction = 0f))
    }

    @Test
    fun fastCloseAndReopenFollowLatestNativeSignals() {
        assertFalse(yieldToCc(visible = false, fraction = 0.7f))
        assertFalse(yieldToCc(visible = true, fraction = 0f))
        assertTrue(yieldToCc(visible = true, fraction = 0.05f))
    }

    @Test
    fun invalidNativeFractionDoesNotStealHomeOwnership() {
        assertFalse(yieldToCc(visible = true, fraction = Float.NaN))
        assertFalse(yieldToCc(visible = true, fraction = Float.POSITIVE_INFINITY))
    }
}
