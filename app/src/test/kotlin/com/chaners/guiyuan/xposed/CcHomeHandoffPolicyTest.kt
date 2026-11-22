package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CcHomeHandoffPolicyTest {
    private fun yieldToCc(visible: Boolean, fraction: Float, ready: Boolean) =
        CcHomeHandoffPolicy.yieldToProjection(
            nativePanelVisible = visible,
            nativeExpansion = fraction,
            projectionReady = ready,
        )

    @Test
    fun slowCloseRestoresHomeAtZeroBeforeNativeVisibleFalse() {
        assertTrue(yieldToCc(visible = true, fraction = 0.65f, ready = true))
        assertTrue(yieldToCc(visible = true, fraction = 0.12f, ready = true))
        // Slow drag reaches native zero while visible callback remains true.
        assertFalse(yieldToCc(visible = true, fraction = 0f, ready = true))
        assertFalse(yieldToCc(visible = false, fraction = 0f, ready = true))
    }

    @Test
    fun delayedReadyCallbackCannotHideHomeAfterSlowClose() {
        assertFalse(yieldToCc(visible = true, fraction = 0f, ready = false))
        assertFalse(yieldToCc(visible = true, fraction = 0f, ready = true))
    }

    @Test
    fun partialPullKeepsExistingHandoffRules() {
        assertFalse(yieldToCc(visible = true, fraction = 0.4f, ready = false))
        assertTrue(yieldToCc(visible = true, fraction = 0.4f, ready = true))
        assertFalse(yieldToCc(visible = true, fraction = 0.4f, ready = false))
    }

    @Test
    fun fastCloseAndReopenFollowLatestNativeSignals() {
        assertFalse(yieldToCc(visible = false, fraction = 0.7f, ready = true))
        assertFalse(yieldToCc(visible = true, fraction = 0f, ready = true))
        assertTrue(yieldToCc(visible = true, fraction = 0.05f, ready = true))
    }

    @Test
    fun nativeHeaderExitEndsClosingMaskBeforeDelayedFractionZero() {
        // Landscape-to-portrait exit: both native header endpoints have left
        // the visible native root, even though the last fraction is positive.
        assertTrue(
            CcHomeHandoffPolicy.nativeCloseVisualCompleted(
                closing = true,
                fakeHeaderVisible = false,
                finalHeaderVisible = false,
            ),
        )
        // Half pulls and native crossfades still own their source while
        // at least one original status-bar endpoint can be seen.
        assertFalse(CcHomeHandoffPolicy.nativeCloseVisualCompleted(false, false, false))
        assertFalse(CcHomeHandoffPolicy.nativeCloseVisualCompleted(true, true, false))
        assertFalse(CcHomeHandoffPolicy.nativeCloseVisualCompleted(true, false, true))
        assertFalse(CcHomeHandoffPolicy.nativeCloseVisualCompleted(true, true, true))
    }

    @Test
    fun invalidNativeFractionDoesNotStealHomeOwnership() {
        assertFalse(yieldToCc(visible = true, fraction = Float.NaN, ready = true))
        assertFalse(yieldToCc(visible = true, fraction = Float.POSITIVE_INFINITY, ready = true))
    }
}
