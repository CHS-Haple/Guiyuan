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
            CombinedStatusTintState(
                appliedTint = 0xbf112233.toInt(),
                statusIconTint = 0xe6ffffff.toInt(),
            )

        val seed =
            HomeRenderSession.resolveInitialTintSeed(
                transferred = transferred,
                allowLiveSeed = false,
                liveState = {
                    liveReads += 1
                    CombinedStatusTintState(
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
            CombinedStatusTintState(
                appliedTint = 0xe6ffffff.toInt(),
            )

        val seed =
            HomeRenderSession.resolveInitialTintSeed(
                transferred =
                    CombinedStatusTintState(
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
                    CombinedStatusTintState(
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
