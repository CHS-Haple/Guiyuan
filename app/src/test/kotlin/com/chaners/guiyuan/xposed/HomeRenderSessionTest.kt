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
    fun nativeHomeCarrierAnchorFollowsRotationWithoutChangingNativeViews() {
        val portrait = HomeCarrierAnchorPolicy.resolve(
            hostWidthPx = 587,
            hostHeightPx = 108,
            carrierWidthPx = 105,
            carrierLeftPx = 482,
        )
        assertEquals(482, portrait?.left)
        assertEquals(587, portrait?.right)
        assertEquals(105, portrait?.visualWidth)

        // Identical parent dimensions but different real carrier coordinates:
        // the old host-end estimate would always return 655.
        val before = HomeCarrierAnchorPolicy.resolve(
            hostWidthPx = 760,
            hostHeightPx = 108,
            carrierWidthPx = 105,
            carrierLeftPx = 655,
        )
        val afterNativeTranslation = HomeCarrierAnchorPolicy.resolve(
            hostWidthPx = 760,
            hostHeightPx = 108,
            carrierWidthPx = 105,
            carrierLeftPx = 605,
        )
        assertEquals(655, before?.left)
        assertEquals(605, afterNativeTranslation?.left)
        assertEquals(710, afterNativeTranslation?.right)
        assertEquals(105, afterNativeTranslation?.visualWidth)
    }

    @Test
    fun expandedChargingBatteryRetainsCompactVisualAtNativeEnd() {
        // SystemUI widens its battery object from 105 to 135 px. Anchoring
        // the compact visual to battery_icon_container.left would shift the
        // entire ring 30 px toward the previous peer row.
        val charging = HomeCarrierAnchorPolicy.resolveExpandedBattery(
            hostWidthPx = 600,
            hostHeightPx = 108,
            carrierWidthPx = 105,
            carrierLeftPx = 465,
            nativeBatteryWidthPx = 135,
            nativeBatteryRightPx = 600,
        )
        assertEquals(495, charging?.left)
        assertEquals(600, charging?.right)
        assertEquals(105, charging?.visualWidth)

        val normal = HomeCarrierAnchorPolicy.resolveExpandedBattery(
            hostWidthPx = 600,
            hostHeightPx = 108,
            carrierWidthPx = 105,
            carrierLeftPx = 495,
            nativeBatteryWidthPx = 105,
            nativeBatteryRightPx = 600,
        )
        assertEquals(normal?.left, charging?.left)
        assertEquals(normal?.right, charging?.right)

        // Native battery width can shrink before its inner carrier moves.
        val beforeCoreRelayout = HomeCarrierAnchorPolicy.resolveExpandedBattery(
            hostWidthPx = 600,
            hostHeightPx = 108,
            carrierWidthPx = 105,
            carrierLeftPx = 465,
            nativeBatteryWidthPx = 105,
            nativeBatteryRightPx = 600,
        )
        assertEquals(charging?.left, beforeCoreRelayout?.left)
        assertEquals(charging?.right, beforeCoreRelayout?.right)
    }

    @Test
    fun expandedChargingBatteryKeepsNativeViewportBoundary() {
        assertEquals(
            null,
            HomeCarrierAnchorPolicy.resolveExpandedBattery(
                hostWidthPx = 600,
                hostHeightPx = 108,
                carrierWidthPx = 105,
                carrierLeftPx = 465,
                nativeBatteryWidthPx = 135,
                nativeBatteryRightPx = 635,
            ),
        )
    }

    @Test
    fun invalidOrUnfinishedNativeHomeCarrierNeverUsesStaleHostEndEstimate() {
        assertEquals(null, HomeCarrierAnchorPolicy.resolve(587, 108, 105, -1))
        assertEquals(null, HomeCarrierAnchorPolicy.resolve(587, 108, 105, 500))
        assertEquals(null, HomeCarrierAnchorPolicy.resolve(587, 108, 0, 482))
        assertEquals(null, HomeCarrierAnchorPolicy.resolve(587, 0, 105, 482))
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
