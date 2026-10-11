package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CcTransitionGeometryTest {
    @Test
    fun mobileTypeHandoffComparesCurrentNativeGlyphNotDataSimOwnership() {
        val native5G = NativePresentationResolver.NetworkType(
            label = "5G",
            enhanced = false,
            source = NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
        )
        val native5GA = native5G.copy(label = "5GA")
        val nativePlus = native5G.copy(enhanced = true)

        assertEquals(false, CcTransitionPolicy.mobileTypeMatches("5GA", false, native5G))
        assertEquals(true, CcTransitionPolicy.mobileTypeMatches("5GA", false, native5GA))
        assertEquals(true, CcTransitionPolicy.mobileTypeMatches("5G-A", false, native5GA))
        assertEquals(true, CcTransitionPolicy.mobileTypeMatches("5G++", false, nativePlus))
        assertEquals(false, CcTransitionPolicy.mobileTypeMatches("4G", false, native5G))
        assertNull(CcTransitionPolicy.mobileTypeMatches("5GA", false, null))
    }

    @Test
    fun missingNativeEndpointUsesOneWholeComponentFadeCurve() {
        val policy = CcTransitionPolicy
        assertEquals(1f, policy.unmatchedExitOpacity(0f), 0f)
        assertEquals(0.125f, policy.unmatchedExitOpacity(0.5f), 0.0001f)
        assertEquals(0f, policy.unmatchedExitOpacity(1f), 0f)
        assertEquals(1f, policy.unmatchedExitOpacity(Float.NaN), 0f)

        // No target geometry means the source keeps its native carrier basis.
        val source = floatArrayOf(120f, 50f, 24f, 0f, 0f, 30f)
        val carrier = floatArrayOf(100f, 50f, 80f, 0f, 0f, 40f)
        val moved = carrier.copyOf().apply { this[0] += 20f }
        val carried = policy.rebaseSourceToCurrentCarrier(source, carrier, moved)
        assertEquals(140f, carried[0], 0.0001f)
        assertEquals(24f, carried[2], 0.0001f)
        assertEquals(30f, carried[5], 0.0001f)
    }

    @Test
    fun missingTextDoesNotReverseBatteryRingRetraction() {
        val none = com.chaners.guiyuan.xposed.battery.BatteryRingTransitionPolicy.ExitDirection.NONE
        val left = com.chaners.guiyuan.xposed.battery.BatteryRingTransitionPolicy.ExitDirection.LEFT
        val right = com.chaners.guiyuan.xposed.battery.BatteryRingTransitionPolicy.ExitDirection.RIGHT

        assertEquals(
            left,
            CcTransitionPolicy.batteryRingExitDirection(
                liveCenterDirection = none,
                nativeBatteryIslandActive = false,
                targetRowRtl = false,
            ),
        )
        assertEquals(
            right,
            CcTransitionPolicy.batteryRingExitDirection(
                liveCenterDirection = none,
                nativeBatteryIslandActive = false,
                targetRowRtl = true,
            ),
        )
        assertEquals(
            right,
            CcTransitionPolicy.batteryRingExitDirection(
                liveCenterDirection = right,
                nativeBatteryIslandActive = false,
                targetRowRtl = false,
            ),
        )
        assertEquals(
            left,
            CcTransitionPolicy.batteryRingExitDirection(
                liveCenterDirection = right,
                nativeBatteryIslandActive = true,
                targetRowRtl = false,
            ),
        )
    }

    @Test
    fun fakeCapacityLeaseDoesNotChangeEndAnchoredMotionCarrierCenter() {
        val expandedCarrier =
            floatArrayOf(
                872f,
                129.5f,
                728f,
                0f,
                0f,
                169f,
            )
        val logicalCarrier =
            CcTransitionPolicy
                .endAnchoredMotionCarrierGeometry(
                    carrierGeometry = expandedCarrier,
                    carrierWidth = 728,
                    carrierHeight = 169,
                    logicalWidth = 478,
                    isRtl = false,
                )
        requireNotNull(logicalCarrier)

        assertEquals(997f, logicalCarrier[0], 0.0001f)
        assertEquals(478f, logicalCarrier[2], 0.0001f)

        val source = floatArrayOf(1240f, 55f, 105f, 0f, 0f, 108f)
        val sourceCarrier = floatArrayOf(997f, 54f, 478f, 0f, 0f, 108f)
        val carried =
            CcTransitionPolicy
                .rebaseSourceToCurrentCarrier(
                    source = source,
                    sourceCarrier = sourceCarrier,
                    currentCarrier = logicalCarrier,
                )
        assertEquals(1240f, carried[0], 0.0001f)
    }

    @Test
    fun fakeCapacityLeaseKeepsRtlMotionCarrierStartAnchored() {
        val expandedCarrier =
            floatArrayOf(
                872f,
                129.5f,
                728f,
                0f,
                0f,
                169f,
            )
        val logicalCarrier =
            CcTransitionPolicy
                .endAnchoredMotionCarrierGeometry(
                    carrierGeometry = expandedCarrier,
                    carrierWidth = 728,
                    carrierHeight = 169,
                    logicalWidth = 478,
                    isRtl = true,
                )
        requireNotNull(logicalCarrier)
        assertEquals(747f, logicalCarrier[0], 0.0001f)
        assertEquals(478f, logicalCarrier[2], 0.0001f)
    }

    @Test
    fun carriedSourceUsesNativeCarrierMotionBeforeRootTargetInterpolation() {
        val source =
            floatArrayOf(75f, 70f, 10f, 0f, 0f, 10f)
        val target =
            floatArrayOf(235f, 150f, 20f, 0f, 0f, 20f)
        val sourceCarrier =
            floatArrayOf(50f, 50f, 100f, 0f, 0f, 100f)
        val currentCarrier =
            floatArrayOf(130f, 120f, 140f, 0f, 0f, 169f)

        val result =
            CcTransitionPolicy
                .interpolateCarriedSourceToRootTarget(
                    source = source,
                    target = target,
                    sourceCarrier = sourceCarrier,
                    currentCarrier = currentCarrier,
                    progress = 0.5f,
                    scalePolicy = StatusPainter.TransitionScalePolicy.TARGET,
                )

        assertEquals(195f, result[0], 0.0001f)
        assertEquals(145f, result[1], 0.0001f)
        assertEquals(15f, result[2], 0.0001f)
        assertEquals(15f, result[5], 0.0001f)
    }

    @Test
    fun carriedSourceDoesNotRescaleSourceOffsetAtGestureStart() {
        val source =
            floatArrayOf(75f, 70f, 10f, 0f, 0f, 10f)
        val target =
            floatArrayOf(235f, 150f, 20f, 0f, 0f, 20f)
        val sourceCarrier =
            floatArrayOf(50f, 50f, 100f, 0f, 0f, 108f)
        val currentCarrier =
            floatArrayOf(60f, 65f, 140f, 0f, 0f, 169f)

        val result =
            CcTransitionPolicy
                .interpolateCarriedSourceToRootTarget(
                    source = source,
                    target = target,
                    sourceCarrier = sourceCarrier,
                    currentCarrier = currentCarrier,
                    progress = 0f,
                    scalePolicy = StatusPainter.TransitionScalePolicy.TARGET,
                )

        assertEquals(85f, result[0], 0.0001f)
        assertEquals(85f, result[1], 0.0001f)
        assertEquals(10f, result[2], 0.0001f)
        assertEquals(10f, result[5], 0.0001f)
    }

    @Test
    fun carriedSourceFollowsLiveFakeCarrierAtStart() {
        val source =
            floatArrayOf(75f, 50f, 10f, 0f, 0f, 10f)
        val target =
            floatArrayOf(235f, 150f, 20f, 0f, 0f, 20f)
        val sourceCarrier =
            floatArrayOf(50f, 50f, 100f, 0f, 0f, 108f)
        val currentCarrier =
            floatArrayOf(60f, 58f, 140f, 0f, 0f, 169f)

        val result =
            CcTransitionPolicy
                .interpolateCarriedSourceToRootTarget(
                    source = source,
                    target = target,
                    sourceCarrier = sourceCarrier,
                    currentCarrier = currentCarrier,
                    progress = 0f,
                    scalePolicy = StatusPainter.TransitionScalePolicy.TARGET,
                )

        assertEquals(85f, result[0], 0.0001f)
        assertEquals(58f, result[1], 0.0001f)
        assertEquals(source[2], result[2], 0.0001f)
        assertEquals(source[5], result[5], 0.0001f)
    }

    @Test
    fun carriedSourceLandsOnAbsoluteRootTargetEvenWhenCarriersDoNotConverge() {
        val source =
            floatArrayOf(75f, 50f, 10f, 0f, 0f, 10f)
        val target =
            floatArrayOf(235f, 150f, 20f, 0f, 0f, 20f)
        val sourceCarrier =
            floatArrayOf(50f, 50f, 100f, 0f, 0f, 108f)
        val currentCarrier =
            floatArrayOf(154f, 136f, 140f, 0f, 0f, 169f)

        val result =
            CcTransitionPolicy
                .interpolateCarriedSourceToRootTarget(
                    source = source,
                    target = target,
                    sourceCarrier = sourceCarrier,
                    currentCarrier = currentCarrier,
                    progress = 1f,
                    scalePolicy = StatusPainter.TransitionScalePolicy.TARGET,
                )

        assertEquals(target[0], result[0], 0.0001f)
        assertEquals(target[1], result[1], 0.0001f)
        assertEquals(target[2], result[2], 0.0001f)
        assertEquals(target[5], result[5], 0.0001f)
    }

    @Test
    fun latentSingleIconCanLandOnRootTargetWithoutGrowingToLargeSlotBox() {
        val source =
            floatArrayOf(75f, 50f, 20f, 0f, 0f, 20f)
        val target =
            floatArrayOf(235f, 150f, 75f, 0f, 0f, 75f)
        val sourceCarrier =
            floatArrayOf(50f, 50f, 100f, 0f, 0f, 108f)
        val currentCarrier =
            floatArrayOf(154f, 136f, 140f, 0f, 0f, 169f)

        val result =
            CcTransitionPolicy
                .interpolateCarriedSourceToRootTarget(
                    source = source,
                    target = target,
                    sourceCarrier = sourceCarrier,
                    currentCarrier = currentCarrier,
                    progress = 1f,
                    scalePolicy = StatusPainter.TransitionScalePolicy.SHRINK_ONLY,
                )

        assertEquals(target[0], result[0], 0.0001f)
        assertEquals(target[1], result[1], 0.0001f)
        assertEquals(source[2], result[2], 0.0001f)
        assertEquals(source[5], result[5], 0.0001f)
    }

    @Test
    fun latentAdditionalMobileKeepsShrinkOnlyPathBasis() {
        val source = transitionGeometry(width = 20f, height = 20f)
        val target = transitionGeometry(width = 75f, height = 75f)

        val result =
            CcTransitionPolicy
                .interpolateSimilarityGeometry(
                    source = source,
                    target = target,
                    progress = 1f,
                    scalePolicy = StatusPainter.TransitionScalePolicy.SHRINK_ONLY,
                )

        assertEquals(20f, result[2], 0.0001f)
        assertEquals(20f, result[5], 0.0001f)
    }

    @Test
    fun nativeTargetHeightCanBoundLocalShapeWithoutOwningItsExactScale() {
        val ratio =
            CcTransitionPolicy.relativeGeometryHeight(
                target = transitionGeometry(width = 20f, height = 50f),
                current = transitionGeometry(width = 10f, height = 20f),
            )

        assertEquals(2.5f, ratio ?: -1f, 0.0001f)
    }


    @Test
    fun steadyTransitionSourceUsesHostEndSlotInsteadOfInnerBatteryCenter() {
        val host = floatArrayOf(300f, 54f, 600f, 0f, 0f, 108f)

        val ltr =
            CcTransitionPolicy.endAnchoredSlotGeometry(
                hostGeometry = host,
                hostWidth = 600,
                hostHeight = 108,
                slotWidth = 105,
                isRtl = false,
            )
        requireNotNull(ltr)
        assertEquals(547.5f, ltr[0], 0.0001f)
        assertEquals(54f, ltr[1], 0.0001f)
        assertEquals(105f, ltr[2], 0.0001f)
        assertEquals(108f, ltr[5], 0.0001f)

        val rtl =
            CcTransitionPolicy.endAnchoredSlotGeometry(
                hostGeometry = host,
                hostWidth = 600,
                hostHeight = 108,
                slotWidth = 105,
                isRtl = true,
            )
        requireNotNull(rtl)
        assertEquals(52.5f, rtl[0], 0.0001f)
    }

    @Test
    fun sourceGeometryKeepsNativePositionButUsesStableRenderBasis() {
        val nativePosition = floatArrayOf(100f, 200f, 60f, 0f, 0f, 40f)
        val stableRenderBasis = floatArrayOf(900f, 900f, 105f, 0f, 0f, 169f)

        val result =
            CcTransitionPolicy.composeSourceGeometry(
                positionAuthority = nativePosition,
                basisAuthority = stableRenderBasis,
            )

        assertEquals(100f, result[0], 0.0001f)
        assertEquals(200f, result[1], 0.0001f)
        assertEquals(105f, result[2], 0.0001f)
        assertEquals(169f, result[5], 0.0001f)
    }

    @Test
    fun missingNativeMobileTextHasNoGuessedSignalSlotEndpoint() {
        assertNull(
            CcTransitionPolicy.semanticFallbackBounds(
                preferredChildEntries = listOf("mobile_type_single", "mobile_type"),
                isRtl = false,
            ),
        )
        assertNull(
            CcTransitionPolicy.semanticFallbackBounds(
                preferredChildEntries = listOf("mobile_type"),
                isRtl = true,
            ),
        )

        val signal = CcTransitionPolicy.semanticFallbackBounds(
            preferredChildEntries = listOf("mobile_signal"),
            isRtl = false,
        )
        val rtlSignal = CcTransitionPolicy.semanticFallbackBounds(
            preferredChildEntries = listOf("mobile_signal"),
            isRtl = true,
        )
        requireNotNull(signal)
        requireNotNull(rtlSignal)
        assertEquals(0.48f, signal.left, 0.0001f)
        assertEquals(0.52f, rtlSignal.right, 0.0001f)
    }

    @Test
    fun tinySecondaryComponentsRemainVisibleToTopologyClassifier() {
        val fourBars =
            listOf(
                ParticipantVisualSnapshot.NormalizedRect(0.05f, 0.60f, 0.15f, 0.95f),
                ParticipantVisualSnapshot.NormalizedRect(0.30f, 0.48f, 0.40f, 0.95f),
                ParticipantVisualSnapshot.NormalizedRect(0.55f, 0.34f, 0.65f, 0.95f),
                ParticipantVisualSnapshot.NormalizedRect(0.80f, 0.18f, 0.90f, 0.95f),
            )
        val tinyDots =
            listOf(
                ParticipantVisualSnapshot.NormalizedRect(0.12f, 0.05f, 0.14f, 0.07f),
                ParticipantVisualSnapshot.NormalizedRect(0.42f, 0.05f, 0.44f, 0.07f),
                ParticipantVisualSnapshot.NormalizedRect(0.72f, 0.05f, 0.74f, 0.07f),
            )

        val retained =
            ParticipantVisualSnapshot.filterProbeComponents(
                components = fourBars + tinyDots,
                probeWidth = 96,
                probeHeight = 96,
            )

        assertEquals(7, retained.size)
        assertEquals(
            ParticipantVisualSnapshot.Topology.COMPOSITE,
            ParticipantVisualSnapshot.classifyComponents(retained),
        )
    }

    @Test
    fun dualRowCompositeCannotExposeExactFourBarCapability() {
        val components =
            listOf(
                ParticipantVisualSnapshot.NormalizedRect(0.05f, 0.60f, 0.15f, 0.95f),
                ParticipantVisualSnapshot.NormalizedRect(0.30f, 0.48f, 0.40f, 0.95f),
                ParticipantVisualSnapshot.NormalizedRect(0.55f, 0.34f, 0.65f, 0.95f),
                ParticipantVisualSnapshot.NormalizedRect(0.80f, 0.18f, 0.90f, 0.95f),
                ParticipantVisualSnapshot.NormalizedRect(0.05f, 0.05f, 0.15f, 0.12f),
                ParticipantVisualSnapshot.NormalizedRect(0.30f, 0.05f, 0.40f, 0.12f),
                ParticipantVisualSnapshot.NormalizedRect(0.55f, 0.05f, 0.65f, 0.12f),
                ParticipantVisualSnapshot.NormalizedRect(0.80f, 0.05f, 0.90f, 0.12f),
            )
        val envelope =
            ParticipantVisualSnapshot.NormalizedRect(
                left = 0.05f,
                top = 0.05f,
                right = 0.90f,
                bottom = 0.95f,
            )
        val snapshot =
            ParticipantVisualSnapshot.Snapshot(
                envelope = envelope,
                components = components,
                topology = ParticipantVisualSnapshot.classifyComponents(components),
            )

        assertEquals(
            ParticipantVisualSnapshot.Topology.COMPOSITE,
            snapshot.topology,
        )
        assertNull(snapshot.fourVerticalBarsWithinEnvelope())
    }

    @Test
    fun participantVisualTopologyDistinguishesFourBarsFromComposite() {
        val fourBars =
            listOf(
                ParticipantVisualSnapshot.NormalizedRect(0.05f, 0.60f, 0.15f, 0.95f),
                ParticipantVisualSnapshot.NormalizedRect(0.30f, 0.48f, 0.40f, 0.95f),
                ParticipantVisualSnapshot.NormalizedRect(0.55f, 0.34f, 0.65f, 0.95f),
                ParticipantVisualSnapshot.NormalizedRect(0.80f, 0.18f, 0.90f, 0.95f),
            )
        assertEquals(
            ParticipantVisualSnapshot.Topology.FOUR_VERTICAL_BARS,
            ParticipantVisualSnapshot.classifyComponents(fourBars),
        )

        val composite =
            fourBars +
                listOf(
                    ParticipantVisualSnapshot.NormalizedRect(0.05f, 0.05f, 0.15f, 0.12f),
                    ParticipantVisualSnapshot.NormalizedRect(0.30f, 0.05f, 0.40f, 0.12f),
                )
        assertEquals(
            ParticipantVisualSnapshot.Topology.COMPOSITE,
            ParticipantVisualSnapshot.classifyComponents(composite),
        )
    }
}
