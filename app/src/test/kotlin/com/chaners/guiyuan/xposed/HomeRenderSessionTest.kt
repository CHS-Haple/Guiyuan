package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRenderSessionTest {
    @Test
    fun ownerReadinessDependsOnlyOnStructuralHomeRequirements() {
        assertTrue(
            HomeRenderSession.resolveOwnerReady(
                featureEnabled = true,
                modelReady = true,
                tintReady = true,
                layoutReady = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            HomeRenderSession.resolveOwnerReady(
                featureEnabled = false,
                modelReady = true,
                tintReady = true,
                layoutReady = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            HomeRenderSession.resolveOwnerReady(
                featureEnabled = true,
                modelReady = true,
                tintReady = true,
                layoutReady = false,
                hostAttached = true,
            ),
        )
    }

    @Test
    fun detachedHomeCarrierCannotBeReady() {
        assertFalse(HomeRenderSession.resolveOwnerReady(
            featureEnabled = true,
            modelReady = true,
            tintReady = true,
            layoutReady = true,
            hostAttached = false,
        ))
    }

    @Test
    fun nativeIntermediateLayoutDoesNotDiscardVerifiedHomeOwnership() {
        val retire = HomeRenderSession::shouldRetireUnresolvedAnchor
        assertFalse(retire(true, true, true, false, false))
        assertFalse(retire(true, true, true, false, true))
        assertFalse(retire(true, true, true, true, true))
        assertFalse(retire(false, true, true, true, false))
    }

    @Test
    fun completedNativeFrameAndRealDetachReleaseUnsafeHomeOwnership() {
        val retire = HomeRenderSession::shouldRetireUnresolvedAnchor
        assertTrue(retire(true, true, true, true, false))
        assertTrue(retire(true, false, true, false, true))
        assertTrue(retire(true, true, false, false, true))
    }

    @Test
    fun overlayVisibilityRequiresControlCenterOwnership() {
        assertTrue(
            HomeRenderSession.resolveOverlayVisible(
                featureEnabled = true,
                controlCenterAllowsHome = true,
                nativeHandoffActive = false,
            ),
        )

        assertFalse(
            HomeRenderSession.resolveOverlayVisible(
                featureEnabled = true,
                controlCenterAllowsHome = false,
                nativeHandoffActive = false,
            ),
        )
    }

    @Test
    fun overlayVisibilityHonorsFeatureAndHandoffGates() {
        assertFalse(
            HomeRenderSession.resolveOverlayVisible(
                featureEnabled = false,
                controlCenterAllowsHome = true,
                nativeHandoffActive = false,
            ),
        )
        assertFalse(
            HomeRenderSession.resolveOverlayVisible(
                featureEnabled = true,
                controlCenterAllowsHome = false,
                nativeHandoffActive = false,
            ),
        )
        assertFalse(
            HomeRenderSession.resolveOverlayVisible(
                featureEnabled = true,
                controlCenterAllowsHome = true,
                nativeHandoffActive = true,
            ),
        )
    }

    @Test
    fun transferredTintWinsWithoutReadingTransientLiveState() {
        var liveReads = 0
        val transferred =
            TintState(
                appliedTint = 0xbf112233.toInt(),
                statusIconTint = 0xe6ffffff.toInt(),
            )

        val seed =
            HomeRenderSession.resolveInitialTintSeed(
                transferred = transferred,
                allowLiveSeed = false,
                liveState = {
                    liveReads += 1
                    TintState(
                        appliedTint = 0xbf000000.toInt(),
                    )
                },
            )

        assertEquals("hotReloadTransfer", seed?.source)
        assertEquals(transferred, seed?.state)
        assertEquals(0, liveReads)
    }

    @Test
    fun invalidTransferredTintFallsBackToLiveNativeSeed() {
        var liveReads = 0
        val live =
            TintState(
                appliedTint = 0xe6ffffff.toInt(),
            )

        val seed =
            HomeRenderSession.resolveInitialTintSeed(
                transferred =
                    TintState(
                        appliedTint = 0x00112233,
                    ),
                allowLiveSeed = true,
                liveState = {
                    liveReads += 1
                    live
                },
            )

        assertEquals("seed", seed?.source)
        assertEquals(live, seed?.state)
        assertEquals(1, liveReads)
    }

    @Test
    fun legacyHotReloadWithoutTransferredTintWaitsForNativeEvent() {
        var liveReads = 0

        val seed =
            HomeRenderSession.resolveInitialTintSeed(
                transferred = null,
                allowLiveSeed = false,
                liveState = {
                    liveReads += 1
                    TintState(
                        appliedTint = 0xbf000000.toInt(),
                    )
                },
            )

        assertEquals(null, seed)
        assertEquals(0, liveReads)
    }
    @Test
    fun topOverflowExpandsOnlyPhysicalSurfaceWithoutMovingLogicalViewport() {
        val resolved =
            VerticalOverflowPolicy.resolve(
                logicalTopPx = 0,
                logicalHeightPx = 108,
                requestedTopOverflowPx = 18,
            )

        assertEquals(-18, resolved.physicalTopPx)
        assertEquals(126, resolved.physicalHeightPx)
        assertEquals(18, resolved.logicalTopInsetPx)
        assertEquals(0, resolved.physicalTopPx + resolved.logicalTopInsetPx)
    }

    @Test
    fun bottomOverflowExtendsPhysicalSurfaceWithoutMovingLogicalOrigin() {
        val resolved = VerticalOverflowPolicy.resolve(
            logicalTopPx = 0,
            logicalHeightPx = 108,
            requestedTopOverflowPx = 18,
            requestedBottomOverflowPx = 7,
        )
        assertEquals(-18, resolved.physicalTopPx)
        assertEquals(133, resolved.physicalHeightPx)
        assertEquals(18, resolved.logicalTopInsetPx)
        assertEquals(115, resolved.physicalTopPx + resolved.physicalHeightPx)
    }

    @Test
    fun homeParentAnchorDoesNotFollowIndependentChargingBatteryMotion() {
        val parent = requireNotNull(SteadyLayoutResolver.resolve(587, 108, 105, false))
        assertEquals(482, parent.left)
        assertEquals(587, parent.right)
        assertTrue(610 > parent.right) // B1908 transient native Battery endpoint
        assertEquals(parent, SteadyLayoutResolver.resolve(587, 108, 105, false))
        val wide = requireNotNull(SteadyLayoutResolver.resolve(1361, 108, 105, false))
        assertEquals(1256, wide.left)
        assertEquals(1361, wide.right)
    }

    @Test
    fun chargeIslandCanRetainHomeWhileChildLayoutWidthIsTransientlyZero() {
        val nativeWidth = NativeSlotGeometry.resolveStableChildWidth(
            layoutWidth = 0,
            measuredWidth = 105,
        )
        assertEquals(105, nativeWidth)
        val stable = requireNotNull(
            SteadyLayoutResolver.resolve(587, 108, requireNotNull(nativeWidth), false),
        )
        assertEquals(482, stable.left)
        assertEquals(587, stable.right)
    }

    @Test
    fun invalidHomeHostDoesNotClaimTheOverlay() {
        assertEquals(null, SteadyLayoutResolver.resolve(0, 108, 105, false))
        assertEquals(null, SteadyLayoutResolver.resolve(587, 0, 105, false))
        assertEquals(null, SteadyLayoutResolver.resolve(587, 108, 0, false))
    }

    @Test
    fun noOverflowPreservesOriginalPhysicalBounds() {
        val resolved =
            VerticalOverflowPolicy.resolve(
                logicalTopPx = 0,
                logicalHeightPx = 108,
                requestedTopOverflowPx = 0,
            )

        assertEquals(0, resolved.physicalTopPx)
        assertEquals(108, resolved.physicalHeightPx)
        assertEquals(0, resolved.logicalTopInsetPx)
    }
}
