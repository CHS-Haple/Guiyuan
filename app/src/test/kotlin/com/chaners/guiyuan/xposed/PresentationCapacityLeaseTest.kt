package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresentationCapacityLeaseTest {
    @Test
    fun transitionReservationCannotShrinkBelowCompactWidth() {
        assertEquals(
            105,
            EndReservationPolicy.resolveRequestedSlotWidth(
                compactSlotWidthPx = 105,
                transitionRequestedSlotWidthPx = 80,
            ),
        )
        assertEquals(
            168,
            EndReservationPolicy.resolveRequestedSlotWidth(
                compactSlotWidthPx = 105,
                transitionRequestedSlotWidthPx = 168,
            ),
        )
        assertEquals(
            105,
            EndReservationPolicy.resolveRequestedSlotWidth(
                compactSlotWidthPx = 105,
                transitionRequestedSlotWidthPx = null,
            ),
        )
    }

    @Test
    fun homePrivacyReservesGuiyuanEndWidthIndependentlyOfBatteryHideFlag() {
        val privacyVisible = HomePrivacyLayoutPolicy.occupiesEnd("START_SHOW_PRIVACY")
        val privacyHidden = HomePrivacyLayoutPolicy.occupiesEnd("COMPLETE_HIDE_PRIVACY")
        assertTrue(privacyVisible)
        assertFalse(privacyHidden)

        for (nativeHide in listOf(false, true)) {
            assertEquals(
                "Privacy must retain the end slot when nativeHide=$nativeHide",
                105,
                EndReservationPolicy.resolvePaddingEndDelta(
                    nativeHide = nativeHide || privacyVisible,
                    actualBatteryWidthPx = 105,
                    requestedSlotWidthPx = 105,
                ),
            )
            assertEquals(
                "Without privacy, only nativeHide reserves the end slot",
                if (nativeHide) 105 else 0,
                EndReservationPolicy.resolvePaddingEndDelta(
                    nativeHide = nativeHide || privacyHidden,
                    actualBatteryWidthPx = 105,
                    requestedSlotWidthPx = 105,
                ),
            )
        }
    }

    @Test
    fun endReservationKeepsOneResolvedEndBoundaryAcrossBatteryStates() {
        assertEquals(
            0,
            EndReservationPolicy.resolvePaddingEndDelta(
                nativeHide = false,
                actualBatteryWidthPx = 105,
                requestedSlotWidthPx = 105,
            ),
        )
        assertEquals(
            -30,
            EndReservationPolicy.resolvePaddingEndDelta(
                nativeHide = false,
                actualBatteryWidthPx = 135,
                requestedSlotWidthPx = 105,
            ),
        )
        assertEquals(
            105,
            EndReservationPolicy.resolvePaddingEndDelta(
                nativeHide = true,
                actualBatteryWidthPx = 135,
                requestedSlotWidthPx = 105,
            ),
        )
    }

    @Test
    fun hiddenPrearmNativeWidthResetIsAdoptedInsteadOfFailNative() {
        assertEquals(
            FakeCarrierCapacityLeasePolicy
                .ExistingLeaseAction.ADOPT_HIDDEN_NATIVE,
            FakeCarrierCapacityLeasePolicy
                .resolveExistingLeaseAction(
                    visibleCycleActive = false,
                    liveWidthPx = 587,
                    appliedWidthPx = 836,
                    currentParentContentWidthPx = 836,
                    leasedParentContentWidthPx = 836,
                ),
        )
    }

    @Test
    fun visibleLeaseWidthMismatchStillFailsNative() {
        assertEquals(
            FakeCarrierCapacityLeasePolicy
                .ExistingLeaseAction.FAIL_WRITER_CONFLICT,
            FakeCarrierCapacityLeasePolicy
                .resolveExistingLeaseAction(
                    visibleCycleActive = true,
                    liveWidthPx = 587,
                    appliedWidthPx = 836,
                    currentParentContentWidthPx = 836,
                    leasedParentContentWidthPx = 836,
                ),
        )
    }

    @Test
    fun unchangedHiddenLeaseIsReused() {
        assertEquals(
            FakeCarrierCapacityLeasePolicy
                .ExistingLeaseAction.REUSE,
            FakeCarrierCapacityLeasePolicy
                .resolveExistingLeaseAction(
                    visibleCycleActive = false,
                    liveWidthPx = 836,
                    appliedWidthPx = 836,
                    currentParentContentWidthPx = 836,
                    leasedParentContentWidthPx = 836,
                ),
        )
    }

    @Test
    fun hiddenLeaseDoesNotAdoptAcrossParentContractChange() {
        assertEquals(
            FakeCarrierCapacityLeasePolicy
                .ExistingLeaseAction.FAIL_WRITER_CONFLICT,
            FakeCarrierCapacityLeasePolicy
                .resolveExistingLeaseAction(
                    visibleCycleActive = false,
                    liveWidthPx = 587,
                    appliedWidthPx = 836,
                    currentParentContentWidthPx = 900,
                    leasedParentContentWidthPx = 836,
                ),
        )
    }

    @Test
    fun settledLandscapeOverhangReplacesHiddenPortraitLeaseWithoutParentWidthWrite() {
        // B1879 captured: native portrait lease 587 -> 826, followed by
        // SystemUI's landscape carrier 1362 over end-anchored parent 1309.
        assertEquals(
            FakeCarrierCapacityLeasePolicy.ExistingLeaseAction.ADOPT_HIDDEN_NATIVE,
            FakeCarrierCapacityLeasePolicy.resolveExistingLeaseAction(
                visibleCycleActive = false,
                liveWidthPx = 1362,
                appliedWidthPx = 826,
                currentParentContentWidthPx = 1309,
                leasedParentContentWidthPx = 826,
                verifiedNativeOverhang = true,
            ),
        )
    }

    @Test
    fun unverifiedOrVisibleNativeOverhangNeverSupersedesLease() {
        for ((visible, verified) in listOf(
            true to true,
            true to false,
            false to false,
        )) {
            assertEquals(
                FakeCarrierCapacityLeasePolicy.ExistingLeaseAction.FAIL_WRITER_CONFLICT,
                FakeCarrierCapacityLeasePolicy.resolveExistingLeaseAction(
                    visibleCycleActive = visible,
                    liveWidthPx = 1362,
                    appliedWidthPx = 826,
                    currentParentContentWidthPx = 1309,
                    leasedParentContentWidthPx = 826,
                    verifiedNativeOverhang = verified,
                ),
            )
        }
        assertEquals(
            FakeCarrierCapacityLeasePolicy.ExistingLeaseAction.FAIL_WRITER_CONFLICT,
            FakeCarrierCapacityLeasePolicy.resolveExistingLeaseAction(
                visibleCycleActive = false,
                liveWidthPx = 826,
                appliedWidthPx = 826,
                currentParentContentWidthPx = 1309,
                leasedParentContentWidthPx = 826,
                verifiedNativeOverhang = true,
            ),
        )
    }

    @Test
    fun fakeCarrierCapacityLeaseUsesOnlyVerifiedParentContentWidth() {
        assertEquals(
            250,
            EndReservationPolicy
                .resolveFakeCarrierCapacityDelta(
                    nativeCarrierWidthPx = 587,
                    parentContentWidthPx = 837,
                ),
        )
        assertEquals(
            0,
            EndReservationPolicy
                .resolveFakeCarrierCapacityDelta(
                    nativeCarrierWidthPx = 587,
                    parentContentWidthPx = 587,
                ),
        )
        assertEquals(
            null,
            EndReservationPolicy
                .resolveFakeCarrierCapacityDelta(
                    nativeCarrierWidthPx = 588,
                    parentContentWidthPx = 587,
                ),
        )
    }

    @Test
    fun verifiedNativeEndOverhangPreservesSystemUiWidthWithoutCarrierLease() {
        // Observed target geometry: fake status area 1365px, narrower
        // end-anchored native root 1309px. No parent-width mutation is owned.
        assertTrue(
            FakeCarrierCapacityLeasePolicy.mayInheritNativeOverhang(
                nativeCarrierWidthPx = 1365,
                parentContentWidthPx = 1309,
                laidOutCarrierWidthPx = 1365,
                attached = true,
                endAnchored = true,
            ),
        )
        assertFalse(
            FakeCarrierCapacityLeasePolicy.mayInheritNativeOverhang(
                nativeCarrierWidthPx = 1365,
                parentContentWidthPx = 1309,
                laidOutCarrierWidthPx = 1309,
                attached = true,
                endAnchored = true,
            ),
        )
        for ((attached, endAnchored) in listOf(false to true, true to false)) {
            assertFalse(
                FakeCarrierCapacityLeasePolicy.mayInheritNativeOverhang(
                    nativeCarrierWidthPx = 1365,
                    parentContentWidthPx = 1309,
                    laidOutCarrierWidthPx = 1365,
                    attached = attached,
                    endAnchored = endAnchored,
                ),
            )
        }
        assertFalse(
            FakeCarrierCapacityLeasePolicy.mayInheritNativeOverhang(
                nativeCarrierWidthPx = 1309,
                parentContentWidthPx = 1309,
                laidOutCarrierWidthPx = 1309,
                attached = true,
                endAnchored = true,
            ),
        )
    }

    @Test
    fun overhangDoesNotBypassMeasuredPeerEndReservationCapacity() {
        assertFalse(EndReservationPolicy.fitsFakeNativePeerRow(
            rowWidthPx = 1230, contentWidthPx = 1190, targetPaddingEndPx = 105,
        ))
        assertTrue(EndReservationPolicy.fitsFakeNativePeerRow(
            rowWidthPx = 1230, contentWidthPx = 900, targetPaddingEndPx = 105,
        ))
    }

    @Test
    fun lateEligibleControlCenterCanAdoptAlreadyCompletedNativeLayout() {
        assertTrue(
            VisualMaskPolicy.shouldAdoptExistingNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = true,
                layoutRequested = false,
                width = 478,
                height = 108,
            ),
        )
        assertFalse(
            VisualMaskPolicy.shouldAdoptExistingNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = false,
                layoutRequested = false,
                width = 478,
                height = 108,
            ),
        )
        assertFalse(
            VisualMaskPolicy.shouldAdoptExistingNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = true,
                layoutRequested = true,
                width = 478,
                height = 108,
            ),
        )
        assertFalse(
            VisualMaskPolicy.shouldAdoptExistingNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = true,
                layoutRequested = false,
                capacityLeaseAwaitingLayout = true,
                width = 478,
                height = 108,
            ),
        )
        assertFalse(
            VisualMaskPolicy.shouldAdoptExistingNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = true,
                layoutRequested = false,
                width = 0,
                height = 108,
            ),
        )
    }
}
