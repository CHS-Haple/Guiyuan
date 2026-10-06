package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePresentationTest {
    @Test
    fun tempEntriesRestoreOwnedOnly() {
        val slots = mutableListOf("alarm_clock", "wifi")
        HomePresentation.OwnedListEntries.withTemporaryEntries(
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
    fun ignoredSlotRestoreRemovesOwnedDelta() {
        val existing = listOf("alarm_clock", "wifi")
        val requested = listOf("wifi", "mobile", "no_sim")
        val owned =
            HomePresentation.PersistentIgnoredSlotPolicy.ownedDelta(
                existing = existing,
                requested = requested,
            )
        assertEquals(listOf("mobile", "no_sim"), owned)

        val live = listOf("alarm_clock", "wifi", "mobile", "no_sim", "vpn")
        assertEquals(
            listOf("alarm_clock", "wifi", "vpn"),
            HomePresentation.PersistentIgnoredSlotPolicy.restoreTarget(
                live = live,
                ownedEntries = owned,
            ),
        )
    }

    @Test
    fun handoffRestoreSkipsNativeSetter() {
        assertFalse(
            HomePresentation.PersistentIgnoredSlotPolicy
                .useNativeSetterOnRestore(
                    requestLayout = false,
                ),
        )
        assertTrue(
            HomePresentation.PersistentIgnoredSlotPolicy
                .useNativeSetterOnRestore(
                    requestLayout = true,
                ),
        )
    }

    @Test
    fun reservationKeepsCompactWidth() {
        assertEquals(
            105,
            HomePresentation.EndReservationPolicy.resolveRequestedSlotWidth(
                compactSlotWidthPx = 105,
                requestedSlotWidthPx = 80,
            ),
        )
        assertEquals(
            168,
            HomePresentation.EndReservationPolicy.resolveRequestedSlotWidth(
                compactSlotWidthPx = 105,
                requestedSlotWidthPx = 168,
            ),
        )
        assertEquals(
            105,
            HomePresentation.EndReservationPolicy.resolveRequestedSlotWidth(
                compactSlotWidthPx = 105,
                requestedSlotWidthPx = null,
            ),
        )
    }

    @Test
    fun endReservationKeepsBoundary() {
        assertEquals(
            0,
            HomePresentation.EndReservationPolicy.resolvePaddingEndDelta(
                nativeHide = false,
                actualBatteryWidthPx = 105,
                requestedSlotWidthPx = 105,
            ),
        )
        assertEquals(
            -30,
            HomePresentation.EndReservationPolicy.resolvePaddingEndDelta(
                nativeHide = false,
                actualBatteryWidthPx = 135,
                requestedSlotWidthPx = 105,
            ),
        )
        assertEquals(
            105,
            HomePresentation.EndReservationPolicy.resolvePaddingEndDelta(
                nativeHide = true,
                actualBatteryWidthPx = 135,
                requestedSlotWidthPx = 105,
            ),
        )
    }

    @Test
    fun hiddenPrearmAdoptsWidthReset() {
        assertEquals(
            HomePresentation.CapacityLeasePolicy
                .ExistingLeaseAction.ADOPT_HIDDEN_NATIVE,
            HomePresentation.CapacityLeasePolicy
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
            HomePresentation.CapacityLeasePolicy
                .ExistingLeaseAction.FAIL_WRITER_CONFLICT,
            HomePresentation.CapacityLeasePolicy
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
            HomePresentation.CapacityLeasePolicy
                .ExistingLeaseAction.REUSE,
            HomePresentation.CapacityLeasePolicy
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
    fun hiddenLeaseRejectsParentChange() {
        assertEquals(
            HomePresentation.CapacityLeasePolicy
                .ExistingLeaseAction.FAIL_WRITER_CONFLICT,
            HomePresentation.CapacityLeasePolicy
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
    fun capacityLeaseUsesVerifiedParent() {
        assertEquals(
            250,
            HomePresentation.EndReservationPolicy
                .capacityDelta(
                    nativeCarrierWidthPx = 587,
                    parentContentWidthPx = 837,
                ),
        )
        assertEquals(
            0,
            HomePresentation.EndReservationPolicy
                .capacityDelta(
                    nativeCarrierWidthPx = 587,
                    parentContentWidthPx = 587,
                ),
        )
        assertEquals(
            null,
            HomePresentation.EndReservationPolicy
                .capacityDelta(
                    nativeCarrierWidthPx = 588,
                    parentContentWidthPx = 587,
                ),
        )
    }

    @Test
    fun deferredCcKeepsNativeUntilLayout() {
        assertTrue(
            HomePresentation.VisualMaskPolicy
                .shouldKeepNativeBeforeCutover(
                    deferVisualMaskUntilLayout = true,
                ),
        )
        assertFalse(
            HomePresentation.VisualMaskPolicy
                .shouldKeepNativeBeforeCutover(
                    deferVisualMaskUntilLayout = false,
                ),
        )
    }

    @Test
    fun retargetReleaseIgnoresOldScene() {
        assertFalse(
            HomePresentation.KeyguardFamilyHandoffPolicy.shouldRelease(
                activeSurface = HomePresentation.KeyguardFamilySurface.AOD,
                requestedSurface = HomePresentation.KeyguardFamilySurface.KEYGUARD,
            ),
        )
        assertFalse(
            HomePresentation.KeyguardFamilyHandoffPolicy.shouldRelease(
                activeSurface = HomePresentation.KeyguardFamilySurface.KEYGUARD,
                requestedSurface = HomePresentation.KeyguardFamilySurface.AOD,
            ),
        )
        assertTrue(
            HomePresentation.KeyguardFamilyHandoffPolicy.shouldRelease(
                activeSurface = HomePresentation.KeyguardFamilySurface.AOD,
                requestedSurface = HomePresentation.KeyguardFamilySurface.AOD,
            ),
        )
    }

    @Test
    fun aodPreMaskNeedsDeferredHandoff() {
        assertTrue(
            HomePresentation.VisualMaskPolicy.shouldPreMaskBeforeCompactCutover(
                deferVisualMaskUntilLayout = true,
                preMaskBeforeLayout = true,
            ),
        )
        assertFalse(
            HomePresentation.VisualMaskPolicy.shouldPreMaskBeforeCompactCutover(
                deferVisualMaskUntilLayout = true,
                preMaskBeforeLayout = false,
            ),
        )
        assertFalse(
            HomePresentation.VisualMaskPolicy.shouldPreMaskBeforeCompactCutover(
                deferVisualMaskUntilLayout = false,
                preMaskBeforeLayout = true,
            ),
        )
    }

    @Test
    fun lateCcAdoptsCompletedLayout() {
        assertTrue(
            HomePresentation.VisualMaskPolicy.shouldAdoptNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = true,
                layoutRequested = false,
                width = 478,
                height = 108,
            ),
        )
        assertFalse(
            HomePresentation.VisualMaskPolicy.shouldAdoptNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = false,
                layoutRequested = false,
                width = 478,
                height = 108,
            ),
        )
        assertFalse(
            HomePresentation.VisualMaskPolicy.shouldAdoptNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = true,
                layoutRequested = true,
                width = 478,
                height = 108,
            ),
        )
        assertFalse(
            HomePresentation.VisualMaskPolicy.shouldAdoptNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = true,
                layoutRequested = false,
                capacityLeaseAwaitingLayout = true,
                width = 478,
                height = 108,
            ),
        )
        assertFalse(
            HomePresentation.VisualMaskPolicy.shouldAdoptNativeLayout(
                deferVisualMaskUntilLayout = true,
                laidOut = true,
                layoutRequested = false,
                width = 0,
                height = 108,
            ),
        )
    }

    @Test
    fun visualHandoffDefersLayoutMutation() {
        assertFalse(
            HomePresentation.DeferredNativeLayoutPolicy
                .shouldWriteNativeLayout(
                    nativeLayoutOwnershipDeferred = true,
                ),
        )
        assertFalse(
            HomePresentation.DeferredNativeLayoutPolicy
                .shouldCompleteCompactLayout(
                    nativeLayoutOwnershipDeferred = true,
                ),
        )
        assertTrue(
            HomePresentation.DeferredNativeLayoutPolicy
                .shouldWriteNativeLayout(
                    nativeLayoutOwnershipDeferred = false,
                ),
        )
        assertTrue(
            HomePresentation.DeferredNativeLayoutPolicy
                .shouldCompleteCompactLayout(
                    nativeLayoutOwnershipDeferred = false,
                ),
        )
    }

    @Test
    fun reloadHandoffSkipsMidLayout() {
        assertFalse(
            HomePresentation.HotReloadHandoffPolicy
                .shouldRequestLayoutOnRelease(
                    continuousHandoff = true,
                ),
        )
        assertTrue(
            HomePresentation.HotReloadHandoffPolicy
                .shouldRequestLayoutOnRelease(
                    continuousHandoff = false,
                ),
        )
    }

    @Test
    fun batteryWidthLossDefersAfterCc() {
        assertTrue(
            HomePresentation.EndReservationPolicy
                .shouldDeferBatteryWidth(
                    retainOnTransientLoss = true,
                    compactLayoutReady = true,
                ),
        )
        assertFalse(
            HomePresentation.EndReservationPolicy
                .shouldDeferBatteryWidth(
                    retainOnTransientLoss = true,
                    compactLayoutReady = false,
                ),
        )
        assertFalse(
            HomePresentation.EndReservationPolicy
                .shouldDeferBatteryWidth(
                    retainOnTransientLoss = false,
                    compactLayoutReady = true,
                ),
        )
    }

    @Test
    fun temporaryEntriesRestoreAfterFailure() {
        val slots = mutableListOf("alarm_clock")
        runCatching {
            HomePresentation.OwnedListEntries.withTemporaryEntries(
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
    fun islandCapacityCountsExtraWidth() {
        assertEquals(
            0,
            HomePresentation.EndReservationPolicy
                .capacityRequirement(
                    nativeHide = true,
                    compactSlotWidthPx = 105,
                    reservationDeltaPx = 105,
                ),
        )
        assertEquals(
            144,
            HomePresentation.EndReservationPolicy
                .capacityRequirement(
                    nativeHide = true,
                    compactSlotWidthPx = 105,
                    reservationDeltaPx = 249,
                ),
        )
        assertEquals(
            249,
            HomePresentation.EndReservationPolicy
                .capacityRequirement(
                    nativeHide = true,
                    compactSlotWidthPx = 105,
                    reservationDeltaPx = 354,
                ),
        )
        assertEquals(
            249,
            HomePresentation.EndReservationPolicy
                .capacityRequirement(
                    nativeHide = false,
                    compactSlotWidthPx = 105,
                    reservationDeltaPx = 249,
                ),
        )
    }

    @Test
    fun ccReservationStopsAtCapacity() {
        assertEquals(
            354,
            HomePresentation.EndReservationPolicy
                .boundedReservationDelta(
                    nativeHide = true,
                    compactSlotWidthPx = 105,
                    requestedReservationDeltaPx = 354,
                    capacityDeltaPx = 249,
                ),
        )
        assertEquals(
            354,
            HomePresentation.EndReservationPolicy
                .boundedReservationDelta(
                    nativeHide = true,
                    compactSlotWidthPx = 105,
                    requestedReservationDeltaPx = 382,
                    capacityDeltaPx = 249,
                ),
        )
        assertEquals(
            200,
            HomePresentation.EndReservationPolicy
                .boundedReservationDelta(
                    nativeHide = false,
                    compactSlotWidthPx = 105,
                    requestedReservationDeltaPx = 200,
                    capacityDeltaPx = 249,
                ),
        )
        assertEquals(
            249,
            HomePresentation.EndReservationPolicy
                .boundedReservationDelta(
                    nativeHide = false,
                    compactSlotWidthPx = 105,
                    requestedReservationDeltaPx = 382,
                    capacityDeltaPx = 249,
                ),
        )
    }

    @Test
    fun peerMirrorScopedToHomeCc() {
        assertTrue(
            HomePresentation.SteadyPeerMirrorPolicy
                .shouldUseHomeMirror(SourceScene.HOME),
        )
        assertFalse(
            HomePresentation.SteadyPeerMirrorPolicy
                .shouldUseHomeMirror(SourceScene.KEYGUARD),
        )
        assertFalse(
            HomePresentation.SteadyPeerMirrorPolicy
                .shouldUseHomeMirror(SourceScene.UNKNOWN),
        )
    }

    @Test
    fun peerMirrorUsesHomeIslandState() {
        assertTrue(
            HomePresentation.SteadyPeerMirrorPolicy
                .isIslandHidden(
                    visibleState = 2,
                    inIslandState = 10,
                ),
        )
        assertFalse(
            HomePresentation.SteadyPeerMirrorPolicy
                .isIslandHidden(
                    visibleState = 2,
                    inIslandState = 20,
                ),
        )
        assertFalse(
            HomePresentation.SteadyPeerMirrorPolicy
                .isIslandHidden(
                    visibleState = 0,
                    inIslandState = 10,
                ),
        )
    }

    @Test
    fun peerMirrorSuppressesFakeIslandOnly() {
        assertFalse(
            HomePresentation.SteadyPeerMirrorPolicy
                .exposeFakeIslandShowing(
                    nativeIslandShowing = true,
                    steadyMirrorActive = true,
                ),
        )
        assertTrue(
            HomePresentation.SteadyPeerMirrorPolicy
                .exposeFakeIslandShowing(
                    nativeIslandShowing = true,
                    steadyMirrorActive = false,
                ),
        )
        assertFalse(
            HomePresentation.SteadyPeerMirrorPolicy
                .exposeFakeIslandShowing(
                    nativeIslandShowing = false,
                    steadyMirrorActive = true,
                ),
        )
    }

    @Test
    fun ccFailureWithoutSessionIsNoOp() {
        assertFalse(
            HomePresentation.failControlCenterPresentation(
                "unit-test-no-session",
            ),
        )
    }

    @Test
    fun ccHotPathSkipsDiagnostics() {
        assertFalse(
            HomePresentation.HotPathDiagnosticPolicy
                .shouldReportCcLayout(
                    detailedDiagnosticsEnabled = false,
                    transitionReservationActive = false,
                ),
        )
        assertFalse(
            HomePresentation.HotPathDiagnosticPolicy
                .shouldReportCcLayout(
                    detailedDiagnosticsEnabled = true,
                    transitionReservationActive = true,
                ),
        )
        assertTrue(
            HomePresentation.HotPathDiagnosticPolicy
                .shouldReportCcLayout(
                    detailedDiagnosticsEnabled = true,
                    transitionReservationActive = false,
                ),
        )

    }



    @Test
    fun familyOwnershipResumesAfterRetarget() {
        assertTrue(
            HomePresentation.DeferredNativeLayoutPolicy
                .shouldResumeOwnershipForRetarget(
                    nativeLayoutOwnershipDeferred = true,
                    nextDeferNativeLayoutOwnership = false,
                ),
        )
        assertFalse(
            HomePresentation.DeferredNativeLayoutPolicy
                .shouldResumeOwnershipForRetarget(
                    nativeLayoutOwnershipDeferred = true,
                    nextDeferNativeLayoutOwnership = true,
                ),
        )
        assertFalse(
            HomePresentation.DeferredNativeLayoutPolicy
                .shouldResumeOwnershipForRetarget(
                    nativeLayoutOwnershipDeferred = false,
                    nextDeferNativeLayoutOwnership = false,
                ),
        )
    }



    @Test
    fun activationRequiresCurrentOwner() {
        assertTrue(
            HomePresentation.ActivationCommitPolicy.canReportSuccess(
                ownerStillCurrent = true,
                surfaceStillCurrent = true,
            ),
        )
        assertFalse(
            HomePresentation.ActivationCommitPolicy.canReportSuccess(
                ownerStillCurrent = false,
                surfaceStillCurrent = true,
            ),
        )
        assertFalse(
            HomePresentation.ActivationCommitPolicy.canReportSuccess(
                ownerStillCurrent = true,
                surfaceStillCurrent = false,
            ),
        )
    }


}
