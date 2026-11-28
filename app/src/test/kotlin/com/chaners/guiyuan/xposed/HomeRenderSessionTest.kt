package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRenderSessionTest {
    @Test
    fun ownerReadinessRequiresAllStructuralEvidence() {
        assertTrue(
            ProjectionReadinessPolicy.structuralReady(
                featureEnabled = true,
                modelReady = true,
                tintReady = true,
                layoutReady = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            ProjectionReadinessPolicy.structuralReady(
                featureEnabled = false,
                modelReady = true,
                tintReady = true,
                layoutReady = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            ProjectionReadinessPolicy.structuralReady(
                featureEnabled = true,
                modelReady = true,
                tintReady = true,
                layoutReady = false,
                hostAttached = true,
            ),
        )
        assertFalse(
            ProjectionReadinessPolicy.structuralReady(
                featureEnabled = true,
                modelReady = false,
                tintReady = true,
                layoutReady = true,
                hostAttached = true,
            ),
        )
        assertFalse(
            ProjectionReadinessPolicy.structuralReady(
                featureEnabled = true,
                modelReady = true,
                tintReady = false,
                layoutReady = true,
                hostAttached = true,
            ),
        )
    }

    @Test
    fun detachedHomeCarrierCannotBeReady() {
        assertFalse(ProjectionReadinessPolicy.structuralReady(
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
}
