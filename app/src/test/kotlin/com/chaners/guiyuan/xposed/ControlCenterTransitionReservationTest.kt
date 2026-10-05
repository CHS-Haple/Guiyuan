package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlCenterTransitionReservationTest {
    @Test
    fun transitionReservationInterpolatesTotalWidthFromNativeProgress() {
        val spans =
            listOf(
                CombinedStatusControlCenterTransitionOwner.Policy.ReservationSpan(
                    sourceLeft = -10f,
                    sourceRight = 0f,
                    targetLeft = -30f,
                    targetRight = 0f,
                ),
            )

        assertEquals(
            10,
            CombinedStatusControlCenterTransitionOwner.Policy
                .resolveTransitionReservationWidth(
                    compactWidthPx = 10,
                    spans = spans,
                    progress = 0f,
                ),
        )
        assertEquals(
            20,
            CombinedStatusControlCenterTransitionOwner.Policy
                .resolveTransitionReservationWidth(
                    compactWidthPx = 10,
                    spans = spans,
                    progress = 0.5f,
                ),
        )
        assertEquals(
            30,
            CombinedStatusControlCenterTransitionOwner.Policy
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
                CombinedStatusControlCenterTransitionOwner.Policy.ReservationSpan(
                    sourceLeft = 0f,
                    sourceRight = 0f,
                    targetLeft = -180f,
                    targetRight = -105f,
                ),
            )

        val oldGeometryUnion =
            CombinedStatusControlCenterTransitionOwner.Policy.resolveReservationWidth(
                compactWidthPx = 105,
                spans = spans,
                progress = 0.25f,
            )
        val transitionWidth =
            CombinedStatusControlCenterTransitionOwner.Policy
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
                CombinedStatusControlCenterTransitionOwner.Policy.ReservationSpan(
                    sourceLeft = 0f,
                    sourceRight = 0f,
                    targetLeft = -180f,
                    targetRight = -105f,
                ),
            )
        val semantic =
            CombinedStatusControlCenterTransitionOwner.Policy
                .resolveTransitionReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    progress = 0.25f,
                )
        val native =
            CombinedStatusControlCenterTransitionOwner.Policy
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
                CombinedStatusControlCenterTransitionOwner.Policy.ReservationSpan(
                    sourceLeft = -105f,
                    sourceRight = 0f,
                    targetLeft = -240f,
                    targetRight = -135f,
                ),
            )

        val unprojected =
            CombinedStatusControlCenterTransitionOwner.Policy
                .resolveBatteryIslandNativePeerReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    semanticWidthPx = 220,
                    progress = 0.5f,
                )
        val projected =
            CombinedStatusControlCenterTransitionOwner.Policy
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
                CombinedStatusControlCenterTransitionOwner.Policy.ReservationSpan(
                    sourceLeft = -40f,
                    sourceRight = 0f,
                    targetLeft = -140f,
                    targetRight = 0f,
                ),
            )

        assertEquals(
            80,
            CombinedStatusControlCenterTransitionOwner.Policy
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
                CombinedStatusControlCenterTransitionOwner.Policy.ReservationSpan(
                    sourceLeft = 0f,
                    sourceRight = 0f,
                    targetLeft = -180f,
                    targetRight = -105f,
                ),
            )

        assertEquals(
            135,
            CombinedStatusControlCenterTransitionOwner.Policy
                .resolveBatteryIslandNativePeerReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    semanticWidthPx = 161,
                    progress = 0.75f,
                ),
        )
        assertEquals(
            180,
            CombinedStatusControlCenterTransitionOwner.Policy
                .resolveBatteryIslandNativePeerReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    semanticWidthPx = 180,
                    progress = 1f,
                ),
        )
        assertEquals(
            150,
            CombinedStatusControlCenterTransitionOwner.Policy
                .resolveBatteryIslandNativePeerReservationWidth(
                    compactWidthPx = 105,
                    spans =
                        listOf(
                            CombinedStatusControlCenterTransitionOwner.Policy.ReservationSpan(
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
            CombinedStatusControlCenterTransitionOwner.Policy.latentRevealVisibleFraction(
                current = transitionGeometry(centerX = 95f, centerY = 100f, width = 20f, height = 20f),
                target = target,
                visualExtent = 20f,
                reservationProgress = 0f,
            ),
            0.0001f,
        )
        assertEquals(
            0f,
            CombinedStatusControlCenterTransitionOwner.Policy.latentRevealVisibleFraction(
                current = transitionGeometry(centerX = 79f, centerY = 100f, width = 20f, height = 20f),
                target = target,
                visualExtent = 20f,
                reservationProgress = 1f,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusControlCenterTransitionOwner.Policy.latentRevealVisibleFraction(
                current = transitionGeometry(centerX = 90f, centerY = 100f, width = 20f, height = 20f),
                target = target,
                visualExtent = 20f,
                reservationProgress = 1f,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusControlCenterTransitionOwner.Policy.latentRevealVisibleFraction(
                current = transitionGeometry(centerX = 93f, centerY = 100f, width = 20f, height = 20f),
                target = target,
                visualExtent = 20f,
                reservationProgress = 0.35f,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusControlCenterTransitionOwner.Policy.latentRevealVisibleFraction(
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
            CombinedStatusControlCenterTransitionOwner.Policy
                .acceleratedLatentRevealProgress(0f),
            0.0001f,
        )
        assertTrue(
            CombinedStatusControlCenterTransitionOwner.Policy
                .acceleratedLatentRevealProgress(0.2f) > 0.5f,
        )
        assertEquals(
            1f,
            CombinedStatusControlCenterTransitionOwner.Policy
                .acceleratedLatentRevealProgress(0.35f),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusControlCenterTransitionOwner.Policy
                .acceleratedLatentRevealProgress(1f),
            0.0001f,
        )
    }

    @Test
    fun latentReservationProgressTracksVisibleEnvelopeCoverage() {
        assertEquals(
            0f,
            CombinedStatusControlCenterTransitionOwner.Policy.latentReservationProgress(
                compactWidthPx = 100,
                currentReservationPx = 100,
                requiredReservationPx = 200,
                visualWidthPx = 100f,
            ),
            0.0001f,
        )
        assertEquals(
            0.5f,
            CombinedStatusControlCenterTransitionOwner.Policy.latentReservationProgress(
                compactWidthPx = 100,
                currentReservationPx = 150,
                requiredReservationPx = 200,
                visualWidthPx = 100f,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusControlCenterTransitionOwner.Policy.latentReservationProgress(
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
            CombinedStatusPainter.MobileSignalMorphPolicy.outerSimilarityScale(
                targetWidthRatio = 1.5f,
                targetHeightRatio = 2f,
            )
        assertEquals(1f, outerScale, 0.0001f)
        assertEquals(
            1.5f,
            CombinedStatusPainter.MobileSignalMorphPolicy.exactTargetAxisCompensation(
                targetAxisRatio = 1.5f,
                outerScale = outerScale,
            ),
            0.0001f,
        )
        assertEquals(
            2f,
            CombinedStatusPainter.MobileSignalMorphPolicy.exactTargetAxisCompensation(
                targetAxisRatio = 2f,
                outerScale = outerScale,
            ),
            0.0001f,
        )
    }

    @Test
    fun exactBarTargetPreservesUniformOuterShrinkAndCompensatesAxes() {
        val outerScale =
            CombinedStatusPainter.MobileSignalMorphPolicy.outerSimilarityScale(
                targetWidthRatio = 0.75f,
                targetHeightRatio = 0.5f,
            )
        assertEquals(0.5f, outerScale, 0.0001f)
        assertEquals(
            1.5f,
            CombinedStatusPainter.MobileSignalMorphPolicy.exactTargetAxisCompensation(
                targetAxisRatio = 0.75f,
                outerScale = outerScale,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusPainter.MobileSignalMorphPolicy.exactTargetAxisCompensation(
                targetAxisRatio = 0.5f,
                outerScale = outerScale,
            ),
            0.0001f,
        )
    }

    @Test
    fun unmatchedComponentsClipOutFastWithoutChangingTheirScale() {
        assertEquals(
            1f,
            CombinedStatusControlCenterTransitionOwner.Policy.unmatchedExitVisibleFraction(0f),
            0.0001f,
        )
        assertEquals(
            0.125f,
            CombinedStatusControlCenterTransitionOwner.Policy.unmatchedExitVisibleFraction(0.5f),
            0.0001f,
        )
        assertEquals(
            0f,
            CombinedStatusControlCenterTransitionOwner.Policy.unmatchedExitVisibleFraction(1f),
            0.0001f,
        )
    }

    @Test
    fun horizontalClipKeepsPixelsOpaqueAndAnchorsTowardChosenEdge() {
        val policy = CombinedStatusControlCenterTransitionOwner.Policy
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
            CombinedStatusPainter.TransitionBounds(
                left = 10f,
                top = 20f,
                right = 50f,
                bottom = 60f,
            )
        val expanded =
            CombinedStatusControlCenterTransitionOwner.Policy.expandedClipBounds(
                bounds = source,
                widthScale = 1.5f,
                heightScale = 1.25f,
            )

        assertEquals(0f, expanded.left, 0.0001f)
        assertEquals(15f, expanded.top, 0.0001f)
        assertEquals(60f, expanded.right, 0.0001f)
        assertEquals(65f, expanded.bottom, 0.0001f)

        val unchanged =
            CombinedStatusControlCenterTransitionOwner.Policy.expandedClipBounds(
                bounds = source,
                widthScale = 0.75f,
                heightScale = Float.NaN,
            )
        assertEquals(source, unchanged)
    }

    @Test
    fun nativePeerTintNeverFallsBackToBatteryTintAuthority() {
        val policy = CombinedStatusControlCenterTransitionOwner.Policy
        val peer = 0xffe0e0e0.toInt()
        val cached = 0xffdddddd.toInt()

        assertEquals(peer, policy.selectNativeTransitionTint(peer, cached))
        assertEquals(cached, policy.selectNativeTransitionTint(null, cached))
        assertNull(policy.selectNativeTransitionTint(null, null))
    }
}
