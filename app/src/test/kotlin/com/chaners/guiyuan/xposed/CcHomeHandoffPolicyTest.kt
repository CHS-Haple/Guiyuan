package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CcHomeHandoffPolicyTest {
    private fun yieldToCc(
        visible: Boolean,
        fraction: Float,
        awaitingNativeCommit: Boolean = false,
    ) = CcHomeHandoffPolicy.yieldToNativePanel(
        nativePanelVisible = visible,
        nativeExpansion = fraction,
        nativeZeroAwaitingCommit = awaitingNativeCommit,
    )

    @Test
    fun slowCloseReturnsHomeAfterNativeZeroCallbackBodyNotBefore() {
        assertTrue(yieldToCc(visible = true, fraction = 0.65f))
        assertTrue(yieldToCc(visible = true, fraction = 0.12f))
        // The intercepted callback runs before native proceed, so this
        // synchronous edge still belongs to the native CC scene.
        assertTrue(yieldToCc(
            visible = true, fraction = 0f, awaitingNativeCommit = true,
        ))
        // After native proceed, zero is the exact settled Home handoff.
        assertFalse(yieldToCc(visible = true, fraction = 0f))
        assertFalse(yieldToCc(
            visible = false, fraction = 0f, awaitingNativeCommit = true,
        ))
    }

    @Test
    fun repeatedZeroDoesNotRevokeAnAlreadyCommittedHomeOwner() {
        assertTrue(CcHomeHandoffPolicy.awaitNativeZeroCommit(
            nativePanelVisible = true,
            previousFraction = 0.12f,
            currentFraction = 0f,
            ccOwnedBeforeCallback = true,
        ))
        assertFalse(CcHomeHandoffPolicy.awaitNativeZeroCommit(
            nativePanelVisible = true,
            previousFraction = 0f,
            currentFraction = 0f,
            ccOwnedBeforeCallback = false,
        ))
        assertFalse(CcHomeHandoffPolicy.awaitNativeZeroCommit(
            nativePanelVisible = false,
            previousFraction = 0.12f,
            currentFraction = 0f,
            ccOwnedBeforeCallback = true,
        ))
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
    @Test
    fun fakeVisualWaitsForHomeDrawOnlyWithAClaimedReplacement() {
        assertTrue(CcHomeHandoffPolicy.retainFakeUntilHomeDraw(
            panelHidden = true, homeClaimed = true, homeFrameReady = false,
        ))
        assertFalse(CcHomeHandoffPolicy.retainFakeUntilHomeDraw(
            panelHidden = true, homeClaimed = true, homeFrameReady = true,
        ))
        assertFalse(CcHomeHandoffPolicy.retainFakeUntilHomeDraw(
            panelHidden = true, homeClaimed = false, homeFrameReady = false,
        ))
        assertFalse(CcHomeHandoffPolicy.retainFakeUntilHomeDraw(
            panelHidden = false, homeClaimed = true, homeFrameReady = false,
        ))
    }
}
