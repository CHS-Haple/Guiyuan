package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUiHomePresentationOwnerTest {
    @Test
    fun temporaryEntriesPreserveExistingAndRestoreOnlyOwnedEntries() {
        val slots = mutableListOf("alarm_clock", "wifi")
        SystemUiHomePresentationOwner.OwnedListEntries.withTemporaryEntries(
            target = slots,
            entries = listOf("wifi", "mobile", "no_sim"),
        ) {
            assertEquals(
                listOf("alarm_clock", "wifi", "mobile", "no_sim"),
                slots,
            )
        }
        assertEquals(listOf("alarm_clock", "wifi"), slots)
    }

    @Test
    fun persistentIgnoredSlotRestoreRemovesOnlySessionOwnedDelta() {
        val existing = listOf("alarm_clock", "wifi")
        val requested = listOf("wifi", "mobile", "no_sim")
        val owned =
            SystemUiHomePresentationOwner.PersistentIgnoredSlotPolicy.ownedDelta(
                existing = existing,
                requested = requested,
            )
        assertEquals(listOf("mobile", "no_sim"), owned)

        val live = listOf("alarm_clock", "wifi", "mobile", "no_sim", "vpn")
        assertEquals(
            listOf("alarm_clock", "wifi", "vpn"),
            SystemUiHomePresentationOwner.PersistentIgnoredSlotPolicy.restoreTarget(
                live = live,
                ownedEntries = owned,
            ),
        )
    }

    @Test
    fun persistentIgnoredSlotRestoreAvoidsNativeSetterDuringContinuousHandoff() {
        assertFalse(
            SystemUiHomePresentationOwner.PersistentIgnoredSlotPolicy
                .shouldUseNativeSetterOnRestore(
                    requestLayout = false,
                ),
        )
        assertTrue(
            SystemUiHomePresentationOwner.PersistentIgnoredSlotPolicy
                .shouldUseNativeSetterOnRestore(
                    requestLayout = true,
                ),
        )
    }

    @Test
    fun transitionReservationCannotShrinkBelowCompactWidth() {
        assertEquals(
            105,
            SystemUiHomePresentationOwner.EndReservationPolicy.resolveRequestedSlotWidth(
                compactSlotWidthPx = 105,
                transitionRequestedSlotWidthPx = 80,
            ),
        )
        assertEquals(
            168,
            SystemUiHomePresentationOwner.EndReservationPolicy.resolveRequestedSlotWidth(
                compactSlotWidthPx = 105,
                transitionRequestedSlotWidthPx = 168,
            ),
        )
        assertEquals(
            105,
            SystemUiHomePresentationOwner.EndReservationPolicy.resolveRequestedSlotWidth(
                compactSlotWidthPx = 105,
                transitionRequestedSlotWidthPx = null,
            ),
        )
    }

    @Test
    fun endReservationKeepsOneResolvedEndBoundaryAcrossBatteryStates() {
        assertEquals(
            0,
            SystemUiHomePresentationOwner.EndReservationPolicy.resolvePaddingEndDelta(
                nativeHide = false,
                actualBatteryWidthPx = 105,
                requestedSlotWidthPx = 105,
            ),
        )
        assertEquals(
            -30,
            SystemUiHomePresentationOwner.EndReservationPolicy.resolvePaddingEndDelta(
                nativeHide = false,
                actualBatteryWidthPx = 135,
                requestedSlotWidthPx = 105,
            ),
        )
        assertEquals(
            105,
            SystemUiHomePresentationOwner.EndReservationPolicy.resolvePaddingEndDelta(
                nativeHide = true,
                actualBatteryWidthPx = 135,
                requestedSlotWidthPx = 105,
            ),
        )
    }

    @Test
    fun fakeCarrierCapacityLeaseUsesOnlyVerifiedParentContentWidth() {
        assertEquals(
            250,
            SystemUiHomePresentationOwner.EndReservationPolicy
                .resolveFakeCarrierCapacityDelta(
                    nativeCarrierWidthPx = 587,
                    parentContentWidthPx = 837,
                ),
        )
        assertEquals(
            0,
            SystemUiHomePresentationOwner.EndReservationPolicy
                .resolveFakeCarrierCapacityDelta(
                    nativeCarrierWidthPx = 587,
                    parentContentWidthPx = 587,
                ),
        )
        assertEquals(
            null,
            SystemUiHomePresentationOwner.EndReservationPolicy
                .resolveFakeCarrierCapacityDelta(
                    nativeCarrierWidthPx = 588,
                    parentContentWidthPx = 587,
                ),
        )
    }

    @Test
    fun deferredControlCenterCutoverPreservesNativeVisualsUntilCompactLayout() {
        assertTrue(
            SystemUiHomePresentationOwner.VisualMaskPolicy
                .shouldPreserveNativeBeforeCompactCutover(
                    deferVisualMaskUntilLayout = true,
                ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.VisualMaskPolicy
                .shouldPreserveNativeBeforeCompactCutover(
                    deferVisualMaskUntilLayout = false,
                ),
        )
    }

    @Test
    fun keyguardFamilyReleaseIgnoresOldSceneAfterSuccessfulRetarget() {
        assertFalse(
            SystemUiHomePresentationOwner.KeyguardFamilyHandoffPolicy.shouldRelease(
                activeSurface = SystemUiHomePresentationOwner.KeyguardFamilySurface.AOD,
                requestedSurface = SystemUiHomePresentationOwner.KeyguardFamilySurface.KEYGUARD,
            ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.KeyguardFamilyHandoffPolicy.shouldRelease(
                activeSurface = SystemUiHomePresentationOwner.KeyguardFamilySurface.KEYGUARD,
                requestedSurface = SystemUiHomePresentationOwner.KeyguardFamilySurface.AOD,
            ),
        )
        assertTrue(
            SystemUiHomePresentationOwner.KeyguardFamilyHandoffPolicy.shouldRelease(
                activeSurface = SystemUiHomePresentationOwner.KeyguardFamilySurface.AOD,
                requestedSurface = SystemUiHomePresentationOwner.KeyguardFamilySurface.AOD,
            ),
        )
    }

    @Test
    fun aodPreMaskRequiresDeferredLayoutAndExplicitHandoffRequest() {
        assertTrue(
            SystemUiHomePresentationOwner.VisualMaskPolicy.shouldPreMaskBeforeCompactCutover(
                deferVisualMaskUntilLayout = true,
                preMaskBeforeLayout = true,
            ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.VisualMaskPolicy.shouldPreMaskBeforeCompactCutover(
                deferVisualMaskUntilLayout = true,
                preMaskBeforeLayout = false,
            ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.VisualMaskPolicy.shouldPreMaskBeforeCompactCutover(
                deferVisualMaskUntilLayout = false,
                preMaskBeforeLayout = true,
            ),
        )
    }

    @Test
    fun lateEligibleControlCenterCanAdoptAlreadyCompletedNativeLayout() {
        assertTrue(
            SystemUiHomePresentationOwner.VisualMaskPolicy.shouldAdoptExistingNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = true,
                layoutRequested = false,
                width = 478,
                height = 108,
            ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.VisualMaskPolicy.shouldAdoptExistingNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = false,
                layoutRequested = false,
                width = 478,
                height = 108,
            ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.VisualMaskPolicy.shouldAdoptExistingNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = true,
                layoutRequested = true,
                width = 478,
                height = 108,
            ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.VisualMaskPolicy.shouldAdoptExistingNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = true,
                layoutRequested = false,
                capacityLeaseAwaitingLayout = true,
                width = 478,
                height = 108,
            ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.VisualMaskPolicy.shouldAdoptExistingNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = true,
                layoutRequested = false,
                width = 0,
                height = 108,
            ),
        )
    }

    @Test
    fun visualOnlyKeyguardHandoffDefersNativeLayoutMutationAndCompletion() {
        assertFalse(
            SystemUiHomePresentationOwner.DeferredNativeLayoutPolicy
                .shouldWriteNativeLayout(
                    nativeLayoutOwnershipDeferred = true,
                ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.DeferredNativeLayoutPolicy
                .shouldCompleteCompactLayout(
                    nativeLayoutOwnershipDeferred = true,
                ),
        )
        assertTrue(
            SystemUiHomePresentationOwner.DeferredNativeLayoutPolicy
                .shouldWriteNativeLayout(
                    nativeLayoutOwnershipDeferred = false,
                ),
        )
        assertTrue(
            SystemUiHomePresentationOwner.DeferredNativeLayoutPolicy
                .shouldCompleteCompactLayout(
                    nativeLayoutOwnershipDeferred = false,
                ),
        )
    }

    @Test
    fun continuousHotReloadHandoffSuppressesIntermediateLayoutRequest() {
        assertFalse(
            SystemUiHomePresentationOwner.HotReloadHandoffPolicy
                .shouldRequestLayoutOnRelease(
                    continuousHandoff = true,
                ),
        )
        assertTrue(
            SystemUiHomePresentationOwner.HotReloadHandoffPolicy
                .shouldRequestLayoutOnRelease(
                    continuousHandoff = false,
                ),
        )
    }

    @Test
    fun transientLiveBatteryWidthLossIsDeferredOnlyAfterControlCenterCutover() {
        assertTrue(
            SystemUiHomePresentationOwner.EndReservationPolicy
                .shouldDeferLiveBatteryWidthUnavailable(
                    retainOnTransientLoss = true,
                    compactLayoutReady = true,
                ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.EndReservationPolicy
                .shouldDeferLiveBatteryWidthUnavailable(
                    retainOnTransientLoss = true,
                    compactLayoutReady = false,
                ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.EndReservationPolicy
                .shouldDeferLiveBatteryWidthUnavailable(
                    retainOnTransientLoss = false,
                    compactLayoutReady = true,
                ),
        )
    }

    @Test
    fun temporaryEntriesRestoreAfterFailure() {
        val slots = mutableListOf("alarm_clock")
        runCatching {
            SystemUiHomePresentationOwner.OwnedListEntries.withTemporaryEntries(
                target = slots,
                entries = listOf("wifi", "mobile"),
            ) {
                error("expected")
            }
        }
        assertEquals(listOf("alarm_clock"), slots)
        assertTrue("wifi" !in slots && "mobile" !in slots)
    }
    @Test
    fun chargingIslandCapacityCountsOnlyExpansionBeyondCompactSlot() {
        assertEquals(
            0,
            SystemUiHomePresentationOwner.EndReservationPolicy
                .resolveFakeCarrierCapacityRequirement(
                    nativeHide = true,
                    compactSlotWidthPx = 105,
                    reservationDeltaPx = 105,
                ),
        )
        assertEquals(
            144,
            SystemUiHomePresentationOwner.EndReservationPolicy
                .resolveFakeCarrierCapacityRequirement(
                    nativeHide = true,
                    compactSlotWidthPx = 105,
                    reservationDeltaPx = 249,
                ),
        )
        assertEquals(
            249,
            SystemUiHomePresentationOwner.EndReservationPolicy
                .resolveFakeCarrierCapacityRequirement(
                    nativeHide = true,
                    compactSlotWidthPx = 105,
                    reservationDeltaPx = 354,
                ),
        )
        assertEquals(
            249,
            SystemUiHomePresentationOwner.EndReservationPolicy
                .resolveFakeCarrierCapacityRequirement(
                    nativeHide = false,
                    compactSlotWidthPx = 105,
                    reservationDeltaPx = 249,
                ),
        )
    }

    @Test
    fun chargingIslandNativeReservationStopsAtPhysicalCarrierCapacity() {
        assertEquals(
            354,
            SystemUiHomePresentationOwner.EndReservationPolicy
                .resolveCapacityBoundedReservationDelta(
                    nativeHide = true,
                    compactSlotWidthPx = 105,
                    requestedReservationDeltaPx = 354,
                    capacityDeltaPx = 249,
                ),
        )
        assertEquals(
            354,
            SystemUiHomePresentationOwner.EndReservationPolicy
                .resolveCapacityBoundedReservationDelta(
                    nativeHide = true,
                    compactSlotWidthPx = 105,
                    requestedReservationDeltaPx = 382,
                    capacityDeltaPx = 249,
                ),
        )
        assertEquals(
            382,
            SystemUiHomePresentationOwner.EndReservationPolicy
                .resolveCapacityBoundedReservationDelta(
                    nativeHide = false,
                    compactSlotWidthPx = 105,
                    requestedReservationDeltaPx = 382,
                    capacityDeltaPx = 249,
                ),
        )
    }

    @Test
    fun steadyPeerMirrorUsesOnlyHomeNativeIslandHiddenState() {
        assertTrue(
            SystemUiHomePresentationOwner.SteadyPeerMirrorPolicy
                .isIslandHidden(
                    visibleState = 2,
                    inIslandState = 10,
                ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.SteadyPeerMirrorPolicy
                .isIslandHidden(
                    visibleState = 2,
                    inIslandState = 20,
                ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.SteadyPeerMirrorPolicy
                .isIslandHidden(
                    visibleState = 0,
                    inIslandState = 10,
                ),
        )
    }

    @Test
    fun steadyPeerMirrorSuppressesOnlyFakeSecondIslandDecision() {
        assertFalse(
            SystemUiHomePresentationOwner.SteadyPeerMirrorPolicy
                .exposeFakeIslandShowing(
                    nativeIslandShowing = true,
                    steadyMirrorActive = true,
                ),
        )
        assertTrue(
            SystemUiHomePresentationOwner.SteadyPeerMirrorPolicy
                .exposeFakeIslandShowing(
                    nativeIslandShowing = true,
                    steadyMirrorActive = false,
                ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.SteadyPeerMirrorPolicy
                .exposeFakeIslandShowing(
                    nativeIslandShowing = false,
                    steadyMirrorActive = true,
                ),
        )
    }

    @Test
    fun controlCenterPresentationFailureIsNoOpWithoutActiveSession() {
        assertFalse(
            SystemUiHomePresentationOwner.failControlCenterPresentation(
                "unit-test-no-session",
            ),
        )
    }

    @Test
    fun controlCenterHotPathDiagnosticsStayOutOfActiveTransitionFrames() {
        assertFalse(
            SystemUiHomePresentationOwner.HotPathDiagnosticPolicy
                .shouldReportControlCenterLayoutState(
                    detailedDiagnosticsEnabled = false,
                    transitionReservationActive = false,
                ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.HotPathDiagnosticPolicy
                .shouldReportControlCenterLayoutState(
                    detailedDiagnosticsEnabled = true,
                    transitionReservationActive = true,
                ),
        )
        assertTrue(
            SystemUiHomePresentationOwner.HotPathDiagnosticPolicy
                .shouldReportControlCenterLayoutState(
                    detailedDiagnosticsEnabled = true,
                    transitionReservationActive = false,
                ),
        )
    }



    @Test
    fun deferredFamilyOwnershipResumesWhenRetargetLeavesVisualOnlyBoundary() {
        assertTrue(
            SystemUiHomePresentationOwner.DeferredNativeLayoutPolicy
                .shouldResumeOwnershipForRetarget(
                    nativeLayoutOwnershipDeferred = true,
                    nextDeferNativeLayoutOwnership = false,
                ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.DeferredNativeLayoutPolicy
                .shouldResumeOwnershipForRetarget(
                    nativeLayoutOwnershipDeferred = true,
                    nextDeferNativeLayoutOwnership = true,
                ),
        )
        assertFalse(
            SystemUiHomePresentationOwner.DeferredNativeLayoutPolicy
                .shouldResumeOwnershipForRetarget(
                    nativeLayoutOwnershipDeferred = false,
                    nextDeferNativeLayoutOwnership = false,
                ),
        )
    }


}
