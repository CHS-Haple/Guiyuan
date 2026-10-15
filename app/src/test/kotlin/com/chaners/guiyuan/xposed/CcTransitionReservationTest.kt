package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CcTransitionReservationTest {
    @Test
    fun nativeChargeRetargetPinsVisualsAndConvergesAtBothNativeEndpoints() {
        val p = 0.6f
        val peerShift = CcTransitionPolicy.retargetShift(
            priorShift = 0f, priorAnchor = null, progress = p,
            nativeDelta = -30f,
        )
        val batteryShift = CcTransitionPolicy.retargetShift(
            priorShift = 0f, priorAnchor = null, progress = p,
            nativeDelta = -15f,
        )
        assertEquals(30f, peerShift, 0.0001f)
        assertEquals(15f, batteryShift, 0.0001f)
        assertEquals(0f, CcTransitionPolicy.retargetWeight(0f, p), 0.0001f)
        assertEquals(1f, CcTransitionPolicy.retargetWeight(p, p), 0.0001f)
        assertEquals(0.5f, CcTransitionPolicy.retargetWeight(0.8f, p), 0.0001f)
        assertEquals(0f, CcTransitionPolicy.retargetWeight(1f, p), 0.0001f)
    }

    @Test
    fun rapidChargeReversalsAtHeldFractionDoNotAccumulateDisplacement() {
        val plugged = CcTransitionPolicy.retargetShift(
            priorShift = 0f, priorAnchor = null, progress = 0.6f,
            nativeDelta = -30f,
        )
        val unplugged = CcTransitionPolicy.retargetShift(
            priorShift = plugged, priorAnchor = 0.6f, progress = 0.6f,
            nativeDelta = 30f,
        )
        assertEquals(0f, unplugged, 0.0001f)
    }

    @Test
    fun nativeFakeRowChargeResizeDoesNotFabricateSourceMotion() {
        val root = floatArrayOf(912f, 0f, 1f, 0f, 0f, 1f)
        val chargingRow = floatArrayOf(1522f, 0f, 1f, 0f, 0f, 1f)
        val unpluggedRow = floatArrayOf(1567f, 0f, 1f, 0f, 0f, 1f)
        val offset = chargingRow[0] - root[0]
        val before = CcTransitionPolicy.rootAnchoredCarrier(chargingRow, root, offset)
        val after = CcTransitionPolicy.rootAnchoredCarrier(unpluggedRow, root, offset)
        assertEquals(before[0], after[0], 0.0001f)

        val held = 0.6f
        val correction = CcTransitionPolicy.retargetShift(
            priorShift = 0f, priorAnchor = null,
            progress = held, nativeDelta = 30f,
        )
        assertEquals(0f, 30f + correction, 0.0001f)
        assertEquals(0f, correction *
            CcTransitionPolicy.retargetWeight(0f, held), 0.0001f)
        assertEquals(0f, correction *
            CcTransitionPolicy.retargetWeight(1f, held), 0.0001f)
    }

    @Test
    fun dualRowMorphKeepsItsOwnPhasesAcrossNativeLayoutRetarget() {
        val source = floatArrayOf(0f, 0f, 2f, 0f, 0f, 2f)
        val original = floatArrayOf(100f, 0f, 2f, 0f, 0f, 2f)
        val target = original.copyOf().apply { this[0] += 30f }
        val correction = CcTransitionPolicy.retargetShift(
            priorShift = 0f, priorAnchor = null, progress = 0.6f,
            nativeDelta = 30f,
        )
        target[0] += correction
        for (p in listOf(0.3f, 0.6f, 0.85f, 1f)) {
            val before = CcTransitionPolicy.dualMobileRowMotionGeometry(
                source = source,
                target = original,
                groupTravelX = 100f,
                groupTravelY = 0f,
                progress = p,
                scalePolicy = StatusPainter.TransitionScalePolicy.SHRINK_ONLY,
            )
            val after = CcTransitionPolicy.dualMobileRowMotionGeometry(
                source = source,
                target = target,
                groupTravelX = 130f + correction,
                groupTravelY = 0f,
                progress = p,
                scalePolicy = StatusPainter.TransitionScalePolicy.SHRINK_ONLY,
            )
            assertEquals(before[0], after[0], 0.0001f)
        }
    }

    @Test
    fun chargingFollowerReceivesRebasedBatteryNumberTarget() {
        val follower = floatArrayOf(10f, 0f, 2f, 0f, 0f, 2f)
        val numberSource = floatArrayOf(0f, 0f, 2f, 0f, 0f, 2f)
        val nativeTarget = floatArrayOf(100f, 0f, 2f, 0f, 0f, 2f)
        val correction = CcTransitionPolicy.retargetShift(
            priorShift = 0f, priorAnchor = null, progress = 0.6f,
            nativeDelta = 15f,
        )
        val rebasedTarget = nativeTarget.copyOf().apply {
            this[0] += 15f + correction
        }
        val before = CcTransitionPolicy.followAnchorGeometry(
            follower = follower,
            sourceAnchor = numberSource,
            currentAnchor = nativeTarget,
        )
        val after = CcTransitionPolicy.followAnchorGeometry(
            follower = follower,
            sourceAnchor = numberSource,
            currentAnchor = rebasedTarget,
        )
        assertEquals(before[0], after[0], 0.0001f)
    }

    @Test
    fun finalRowLayoutMayMoveInTwoPassesAfterBatteryMeasurement() {
        val p = 0.6f
        val first = CcTransitionPolicy.retargetShift(
            priorShift = 0f, priorAnchor = null, progress = p,
            nativeDelta = 15f,
        )
        val second = CcTransitionPolicy.retargetShift(
            priorShift = first, priorAnchor = p, progress = p,
            nativeDelta = 15f,
        )
        assertEquals(-30f, second, 0.0001f)
    }

    @Test
    fun transitionReservationInterpolatesTotalWidthFromNativeProgress() {
        val spans =
            listOf(
                CcTransitionPolicy.ReservationSpan(
                    sourceLeft = -10f,
                    sourceRight = 0f,
                    targetLeft = -30f,
                    targetRight = 0f,
                ),
            )

        assertEquals(
            10,
            CcTransitionPolicy
                .resolveTransitionReservationWidth(
                    compactWidthPx = 10,
                    spans = spans,
                    progress = 0f,
                ),
        )
        assertEquals(
            20,
            CcTransitionPolicy
                .resolveTransitionReservationWidth(
                    compactWidthPx = 10,
                    spans = spans,
                    progress = 0.5f,
                ),
        )
        assertEquals(
            30,
            CcTransitionPolicy
                .resolveTransitionReservationWidth(
                    compactWidthPx = 10,
                    spans = spans,
                    progress = 1f,
                ),
        )
    }

    @Test
    fun totalWidthInterpolationAvoidsOverlappingSpanDeadZone() {
        val spans =
            listOf(
                CcTransitionPolicy.ReservationSpan(
                    sourceLeft = 0f,
                    sourceRight = 0f,
                    targetLeft = -180f,
                    targetRight = -105f,
                ),
            )

        val oldGeometryUnion =
            CcTransitionPolicy.resolveReservationWidth(
                compactWidthPx = 105,
                spans = spans,
                progress = 0.25f,
            )
        val transitionWidth =
            CcTransitionPolicy
                .resolveTransitionReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    progress = 0.25f,
                )

        assertEquals(105, oldGeometryUnion)
        assertTrue(transitionWidth > 105)
        assertTrue(transitionWidth < 180)
    }

    @Test
    fun batteryIslandNativePeerReservationDoesNotPreReserveLatentGap() {
        val spans =
            listOf(
                CcTransitionPolicy.ReservationSpan(
                    sourceLeft = 0f,
                    sourceRight = 0f,
                    targetLeft = -180f,
                    targetRight = -105f,
                ),
            )
        val semantic =
            CcTransitionPolicy
                .resolveTransitionReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    progress = 0.25f,
                )
        val native =
            CcTransitionPolicy
                .resolveBatteryIslandNativePeerReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    semanticWidthPx = semantic,
                    progress = 0.25f,
                )

        assertTrue(semantic > 105)
        assertEquals(105, native)
    }

    @Test
    fun batteryIslandNativePeerReservationProjectsTargetIntoCurrentFakeEndFrame() {
        val spans =
            listOf(
                CcTransitionPolicy.ReservationSpan(
                    sourceLeft = -105f,
                    sourceRight = 0f,
                    targetLeft = -240f,
                    targetRight = -135f,
                ),
            )

        val unprojected =
            CcTransitionPolicy
                .resolveBatteryIslandNativePeerReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    semanticWidthPx = 220,
                    progress = 0.5f,
                )
        val projected =
            CcTransitionPolicy
                .resolveBatteryIslandNativePeerReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    semanticWidthPx = 220,
                    progress = 0.5f,
                    targetEndOffsetPx = 120f,
                )

        assertEquals(173, unprojected)
        assertEquals(113, projected)
    }

    @Test
    fun batteryIslandNativePeerReservationIgnoresEndSideProjection() {
        val spans =
            listOf(
                CcTransitionPolicy.ReservationSpan(
                    sourceLeft = -40f,
                    sourceRight = 0f,
                    targetLeft = -140f,
                    targetRight = 0f,
                ),
            )

        assertEquals(
            80,
            CcTransitionPolicy
                .resolveBatteryIslandNativePeerReservationWidth(
                    compactWidthPx = 40,
                    spans = spans,
                    semanticWidthPx = 140,
                    progress = 1f,
                    targetEndOffsetPx = 60f,
                ),
        )
    }

    @Test
    fun batteryIslandNativePeerReservationFollowsCurrentSpanAndConverges() {
        val spans =
            listOf(
                CcTransitionPolicy.ReservationSpan(
                    sourceLeft = 0f,
                    sourceRight = 0f,
                    targetLeft = -180f,
                    targetRight = -105f,
                ),
            )

        assertEquals(
            135,
            CcTransitionPolicy
                .resolveBatteryIslandNativePeerReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    semanticWidthPx = 161,
                    progress = 0.75f,
                ),
        )
        assertEquals(
            180,
            CcTransitionPolicy
                .resolveBatteryIslandNativePeerReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    semanticWidthPx = 180,
                    progress = 1f,
                ),
        )
        assertEquals(
            150,
            CcTransitionPolicy
                .resolveBatteryIslandNativePeerReservationWidth(
                    compactWidthPx = 105,
                    spans =
                        listOf(
                            CcTransitionPolicy.ReservationSpan(
                                sourceLeft = -105f,
                                sourceRight = 0f,
                                targetLeft = -220f,
                                targetRight = 0f,
                            ),
                        ),
                    semanticWidthPx = 150,
                    progress = 1f,
                ),
        )
    }

    @Test
    fun latentRevealRequiresRealVisualReservationAndTargetProximity() {
        val target = transitionGeometry(centerX = 100f, centerY = 100f, width = 20f, height = 20f)

        assertEquals(
            0f,
            CcTransitionPolicy.latentRevealVisibleFraction(
                current = transitionGeometry(centerX = 95f, centerY = 100f, width = 20f, height = 20f),
                target = target,
                visualExtent = 20f,
                reservationProgress = 0f,
            ),
            0.0001f,
        )
        assertEquals(
            0f,
            CcTransitionPolicy.latentRevealVisibleFraction(
                current = transitionGeometry(centerX = 79f, centerY = 100f, width = 20f, height = 20f),
                target = target,
                visualExtent = 20f,
                reservationProgress = 1f,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            CcTransitionPolicy.latentRevealVisibleFraction(
                current = transitionGeometry(centerX = 90f, centerY = 100f, width = 20f, height = 20f),
                target = target,
                visualExtent = 20f,
                reservationProgress = 1f,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            CcTransitionPolicy.latentRevealVisibleFraction(
                current = transitionGeometry(centerX = 93f, centerY = 100f, width = 20f, height = 20f),
                target = target,
                visualExtent = 20f,
                reservationProgress = 0.35f,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            CcTransitionPolicy.latentRevealVisibleFraction(
                current = target,
                target = target,
                visualExtent = 20f,
                reservationProgress = 1f,
            ),
            0.0001f,
        )
    }

    @Test
    fun latentRevealAcceleratesAfterOccupancyUnlockWithoutChangingTheGate() {
        assertEquals(
            0f,
            CcTransitionPolicy
                .acceleratedLatentRevealProgress(0f),
            0.0001f,
        )
        assertTrue(
            CcTransitionPolicy
                .acceleratedLatentRevealProgress(0.2f) > 0.5f,
        )
        assertEquals(
            1f,
            CcTransitionPolicy
                .acceleratedLatentRevealProgress(0.35f),
            0.0001f,
        )
        assertEquals(
            1f,
            CcTransitionPolicy
                .acceleratedLatentRevealProgress(1f),
            0.0001f,
        )
    }

    @Test
    fun latentReservationProgressTracksVisibleEnvelopeCoverage() {
        assertEquals(
            0f,
            CcTransitionPolicy.latentReservationProgress(
                compactWidthPx = 100,
                currentReservationPx = 100,
                requiredReservationPx = 200,
                visualWidthPx = 100f,
            ),
            0.0001f,
        )
        assertEquals(
            0.5f,
            CcTransitionPolicy.latentReservationProgress(
                compactWidthPx = 100,
                currentReservationPx = 150,
                requiredReservationPx = 200,
                visualWidthPx = 100f,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            CcTransitionPolicy.latentReservationProgress(
                compactWidthPx = 100,
                currentReservationPx = 200,
                requiredReservationPx = 200,
                visualWidthPx = 100f,
            ),
            0.0001f,
        )
    }

    @Test
    fun exactBarTargetCompensatesInsideSimilarityBasis() {
        val outerScale =
            MobileSignalMorphPolicy.outerSimilarityScale(
                targetWidthRatio = 1.5f,
                targetHeightRatio = 2f,
            )
        assertEquals(1f, outerScale, 0.0001f)
        assertEquals(
            1.5f,
            MobileSignalMorphPolicy.exactTargetAxisCompensation(
                targetAxisRatio = 1.5f,
                outerScale = outerScale,
            ),
            0.0001f,
        )
        assertEquals(
            2f,
            MobileSignalMorphPolicy.exactTargetAxisCompensation(
                targetAxisRatio = 2f,
                outerScale = outerScale,
            ),
            0.0001f,
        )
    }

    @Test
    fun exactBarTargetPreservesUniformOuterShrinkAndCompensatesAxes() {
        val outerScale =
            MobileSignalMorphPolicy.outerSimilarityScale(
                targetWidthRatio = 0.75f,
                targetHeightRatio = 0.5f,
            )
        assertEquals(0.5f, outerScale, 0.0001f)
        assertEquals(
            1.5f,
            MobileSignalMorphPolicy.exactTargetAxisCompensation(
                targetAxisRatio = 0.75f,
                outerScale = outerScale,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            MobileSignalMorphPolicy.exactTargetAxisCompensation(
                targetAxisRatio = 0.5f,
                outerScale = outerScale,
            ),
            0.0001f,
        )
    }

    @Test
    fun unmatchedComponentsFadeOutFastWithoutChangingTheirScale() {
        assertEquals(
            1f,
            CcTransitionPolicy.unmatchedExitOpacity(0f),
            0.0001f,
        )
        assertEquals(
            0.125f,
            CcTransitionPolicy.unmatchedExitOpacity(0.5f),
            0.0001f,
        )
        assertEquals(
            0f,
            CcTransitionPolicy.unmatchedExitOpacity(1f),
            0.0001f,
        )
    }

    @Test
    fun horizontalClipKeepsPixelsOpaqueAndAnchorsTowardChosenEdge() {
        val policy = CcTransitionPolicy
        val rightAnchored =
            policy.horizontalClipBounds(
                left = 0f,
                top = 10f,
                right = 100f,
                bottom = 30f,
                visibleFraction = 0.25f,
                anchorRight = true,
            )
        requireNotNull(rightAnchored)
        assertEquals(75f, rightAnchored[0], 0.0001f)
        assertEquals(100f, rightAnchored[2], 0.0001f)

        val leftAnchored =
            policy.horizontalClipBounds(
                left = 0f,
                top = 10f,
                right = 100f,
                bottom = 30f,
                visibleFraction = 0.25f,
                anchorRight = false,
            )
        requireNotNull(leftAnchored)
        assertEquals(0f, leftAnchored[0], 0.0001f)
        assertEquals(25f, leftAnchored[2], 0.0001f)

        assertNull(
            policy.horizontalClipBounds(
                left = 0f,
                top = 0f,
                right = 100f,
                bottom = 20f,
                visibleFraction = 0f,
                anchorRight = true,
            ),
        )
    }

    @Test
    fun latentMobileClipEnvelopeCoversExactTargetAxisExpansion() {
        val source =
            StatusPainter.TransitionBounds(
                left = 10f,
                top = 20f,
                right = 50f,
                bottom = 60f,
            )
        val expanded =
            CcTransitionPolicy.expandedClipBounds(
                bounds = source,
                widthScale = 1.5f,
                heightScale = 1.25f,
            )

        assertEquals(0f, expanded.left, 0.0001f)
        assertEquals(15f, expanded.top, 0.0001f)
        assertEquals(60f, expanded.right, 0.0001f)
        assertEquals(65f, expanded.bottom, 0.0001f)

        val unchanged =
            CcTransitionPolicy.expandedClipBounds(
                bounds = source,
                widthScale = 0.75f,
                heightScale = Float.NaN,
            )
        assertEquals(source, unchanged)
    }

    @Test
    fun nativePeerTintNeverFallsBackToBatteryTintAuthority() {
        val policy = CcTransitionPolicy
        val peer = 0xffe0e0e0.toInt()
        val cached = 0xffdddddd.toInt()

        assertEquals(peer, policy.selectNativeTransitionTint(peer, cached))
        assertEquals(cached, policy.selectNativeTransitionTint(null, cached))
        assertNull(policy.selectNativeTransitionTint(null, null))
    }
}
