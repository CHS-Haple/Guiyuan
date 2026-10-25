package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresentationPoliciesTest {
    @Test
    fun ownedEntriesPreserveExistingAndRestoreOnlyAddedSlots() {
        val slots = mutableListOf("alarm_clock", "wifi")
        val owned = OwnedEntries.add(slots, listOf("wifi", "mobile", "no_sim"))
        assertEquals(listOf("mobile", "no_sim"), owned)
        assertEquals(listOf("alarm_clock", "wifi", "mobile", "no_sim"), slots)
        OwnedEntries.restore(slots, owned)
        assertEquals(listOf("alarm_clock", "wifi"), slots)
    }

    @Test
    fun rollbackFailureKeepsOriginalAdditionError() {
        val addError = IllegalStateException("add-failed")
        val rollbackError = IllegalStateException("rollback-failed")
        val slots =
            object : ArrayList<String>() {
                override fun add(element: String): Boolean {
                    if (element == "airplane") throw addError
                    return super.add(element)
                }

                override fun remove(element: String): Boolean {
                    if (element == "mobile") throw rollbackError
                    return super.remove(element)
                }
            }
        slots.add("native")
        val error =
            runCatching {
                OwnedEntries.add(slots, listOf("mobile", "airplane"))
            }.exceptionOrNull()
        assertTrue(error === addError)
        assertEquals(listOf(rollbackError), error?.suppressed?.toList())
    }

    @Test
    fun restoreAttemptsEveryOwnedEntryAfterOneRemovalFails() {
        val removalError = IllegalStateException("no-sim-removal-failed")
        val slots =
            object : ArrayList<String>() {
                override fun remove(element: String): Boolean {
                    if (element == "no_sim") throw removalError
                    return super.remove(element)
                }
            }
        slots.addAll(listOf("native", "mobile", "no_sim", "airplane"))
        val error =
            runCatching {
                OwnedEntries.restore(slots, listOf("mobile", "no_sim", "airplane"))
            }.exceptionOrNull()
        assertTrue(error === removalError)
        assertEquals(listOf("native", "no_sim"), slots)
    }

    @Test
    fun persistentIgnoredSlotRestoreRemovesOnlySessionOwnedDelta() {
        val existing = listOf("alarm_clock", "wifi")
        val requested = listOf("wifi", "mobile", "no_sim")
        val owned =
            OwnedEntries.delta(
                existing = existing,
                requested = requested,
            )
        assertEquals(listOf("mobile", "no_sim"), owned)

        val live = listOf("alarm_clock", "wifi", "mobile", "no_sim", "vpn")
        assertEquals(
            listOf("alarm_clock", "wifi", "vpn"),
            OwnedEntries.restoreTarget(
                live = live,
                ownedEntries = owned,
            ),
        )
    }

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

    @Test
    fun chargingIslandCapacityCountsOnlyExpansionBeyondCompactSlot() {
        assertEquals(
            0,
            EndReservationPolicy
                .resolveFakeCarrierCapacityRequirement(
                    nativeHide = true,
                    compactSlotWidthPx = 105,
                    reservationDeltaPx = 105,
                ),
        )
        assertEquals(
            144,
            EndReservationPolicy
                .resolveFakeCarrierCapacityRequirement(
                    nativeHide = true,
                    compactSlotWidthPx = 105,
                    reservationDeltaPx = 249,
                ),
        )
        assertEquals(
            249,
            EndReservationPolicy
                .resolveFakeCarrierCapacityRequirement(
                    nativeHide = true,
                    compactSlotWidthPx = 105,
                    reservationDeltaPx = 354,
                ),
        )
        assertEquals(
            249,
            EndReservationPolicy
                .resolveFakeCarrierCapacityRequirement(
                    nativeHide = false,
                    compactSlotWidthPx = 105,
                    reservationDeltaPx = 249,
                ),
        )
    }

    @Test
    fun controlCenterNativeReservationStopsAtPhysicalCarrierCapacity() {
        assertEquals(
            354,
            EndReservationPolicy
                .resolveCapacityBoundedReservationDelta(
                    nativeHide = true,
                    compactSlotWidthPx = 105,
                    requestedReservationDeltaPx = 354,
                    capacityDeltaPx = 249,
                ),
        )
        assertEquals(
            354,
            EndReservationPolicy
                .resolveCapacityBoundedReservationDelta(
                    nativeHide = true,
                    compactSlotWidthPx = 105,
                    requestedReservationDeltaPx = 382,
                    capacityDeltaPx = 249,
                ),
        )
        assertEquals(
            200,
            EndReservationPolicy
                .resolveCapacityBoundedReservationDelta(
                    nativeHide = false,
                    compactSlotWidthPx = 105,
                    requestedReservationDeltaPx = 200,
                    capacityDeltaPx = 249,
                ),
        )
        assertEquals(
            249,
            EndReservationPolicy
                .resolveCapacityBoundedReservationDelta(
                    nativeHide = false,
                    compactSlotWidthPx = 105,
                    requestedReservationDeltaPx = 382,
                    capacityDeltaPx = 249,
                ),
        )
    }

    @Test
    fun nativeFakePeerRowUsesMeasuredSlackBeyondCarrierLease() {
        // Synthetic native measure boundaries, not a claim about device pixels.
        assertTrue(
            EndReservationPolicy.fitsFakeNativePeerRow(
                rowWidthPx = 200, contentWidthPx = 120, targetPaddingEndPx = 80,
            ),
        )
        assertFalse(
            EndReservationPolicy.fitsFakeNativePeerRow(
                rowWidthPx = 200, contentWidthPx = 121, targetPaddingEndPx = 80,
            ),
        )
        assertFalse(
            EndReservationPolicy.fitsFakeNativePeerRow(
                rowWidthPx = 0, contentWidthPx = 0, targetPaddingEndPx = 0,
            ),
        )
        assertFalse(
            EndReservationPolicy.fitsFakeNativePeerRow(
                rowWidthPx = 200, contentWidthPx = 120, targetPaddingEndPx = -1,
            ),
        )
    }

    @Test
    fun steadyPeerMirrorUsesOnlyHomeNativeIslandHiddenState() {
        assertTrue(
            SteadyPeerMirrorPolicy
                .isIslandHidden(
                    visibleState = 2,
                    inIslandState = 10,
                ),
        )
        assertFalse(
            SteadyPeerMirrorPolicy
                .isIslandHidden(
                    visibleState = 2,
                    inIslandState = 20,
                ),
        )
        assertFalse(
            SteadyPeerMirrorPolicy
                .isIslandHidden(
                    visibleState = 0,
                    inIslandState = 10,
                ),
        )
    }

    @Test
    fun steadyPeerMirrorSuppressesOnlyFakeSecondIslandDecision() {
        assertFalse(
            SteadyPeerMirrorPolicy
                .exposeFakeIslandShowing(
                    nativeIslandShowing = true,
                    steadyMirrorActive = true,
                ),
        )
        assertTrue(
            SteadyPeerMirrorPolicy
                .exposeFakeIslandShowing(
                    nativeIslandShowing = true,
                    steadyMirrorActive = false,
                ),
        )
        assertFalse(
            SteadyPeerMirrorPolicy
                .exposeFakeIslandShowing(
                    nativeIslandShowing = false,
                    steadyMirrorActive = true,
                ),
        )
    }


}
