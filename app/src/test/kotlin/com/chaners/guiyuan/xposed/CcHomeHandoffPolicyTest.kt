package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CcHomeHandoffPolicyTest {
    @Test
    fun rotatedPortraitHeaderOutsideSteadySlotMayRevealHomeWithoutReleasingCc() {
        // B1931: Home source y=85..254, both native CC headers y=387
        // on closing. The window contains no CC header over the Home slot.
        assertTrue(CcHomeCloseUnderlayPolicy.shouldShow(
            nativePanelVisible = true,
            closing = true,
            homeSlotBottomPx = 254,
            fakeHeaderTopPx = 387,
            finalHeaderTopPx = 387,
        ))
        assertTrue(CcHomeCloseUnderlayPolicy.shouldShow(
            nativePanelVisible = true,
            closing = true,
            homeSlotBottomPx = 254,
            fakeHeaderTopPx = 260,
            finalHeaderTopPx = 260,
        ))
    }

    @Test
    fun nativeHeaderOverlapOrNonClosingRejectsHomeUnderlay() {
        fun show(
            closing: Boolean = true,
            fake: Int? = 387,
            final: Int? = 387,
            home: Int? = 254,
            nativeVisible: Boolean = true,
        ) = CcHomeCloseUnderlayPolicy.shouldShow(
            nativePanelVisible = nativeVisible,
            closing = closing,
            homeSlotBottomPx = home,
            fakeHeaderTopPx = fake,
            finalHeaderTopPx = final,
        )
        assertFalse(show(closing = false))
        assertFalse(show(fake = 181, final = 181))
        assertFalse(show(fake = 387, final = 181))
        assertFalse(show(fake = 94, final = 94))
        assertFalse(show(nativeVisible = false))
        assertFalse(show(fake = null))
        assertFalse(show(home = null))
    }

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
}
