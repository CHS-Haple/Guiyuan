package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CcTransitionReservationTest {
    @Test
    fun reservationInterpolatesWidth() {
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
                .reservationWidth(
                    compactWidthPx = 10,
                    spans = spans,
                    progress = 0f,
                ),
        )
        assertEquals(
            20,
            CcTransitionPolicy
                .reservationWidth(
                    compactWidthPx = 10,
                    spans = spans,
                    progress = 0.5f,
                ),
        )
        assertEquals(
            30,
            CcTransitionPolicy
                .reservationWidth(
                    compactWidthPx = 10,
                    spans = spans,
                    progress = 1f,
                ),
        )
    }

    @Test
    fun widthInterpolationAvoidsDeadZone() {
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
                .reservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    progress = 0.25f,
                )

        assertEquals(105, oldGeometryUnion)
        assertTrue(transitionWidth > 105)
        assertTrue(transitionWidth < 180)
    }

    @Test
    fun islandReservationSkipsLatentGap() {
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
                .reservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    progress = 0.25f,
                )
        val native =
            CcTransitionPolicy
                .batteryPeerReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    semanticWidthPx = semantic,
                    progress = 0.25f,
                )

        assertTrue(semantic > 105)
        assertEquals(105, native)
    }

    @Test
    fun islandReservationProjectsIntoFakeFrame() {
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
                .batteryPeerReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    semanticWidthPx = 220,
                    progress = 0.5f,
                )
        val projected =
            CcTransitionPolicy
                .batteryPeerReservationWidth(
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
    fun islandReservationIgnoresEndSide() {
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
                .batteryPeerReservationWidth(
                    compactWidthPx = 40,
                    spans = spans,
                    semanticWidthPx = 140,
                    progress = 1f,
                    targetEndOffsetPx = 60f,
                ),
        )
    }

    @Test
    fun islandReservationConverges() {
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
                .batteryPeerReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    semanticWidthPx = 161,
                    progress = 0.75f,
                ),
        )
        assertEquals(
            180,
            CcTransitionPolicy
                .batteryPeerReservationWidth(
                    compactWidthPx = 105,
                    spans = spans,
                    semanticWidthPx = 180,
                    progress = 1f,
                ),
        )
        assertEquals(
            150,
            CcTransitionPolicy
                .batteryPeerReservationWidth(
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
    fun latentRevealNeedsSpaceAndProximity() {
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
    fun latentRevealAcceleratesAfterUnlock() {
        assertEquals(
            0f,
            CcTransitionPolicy
                .fastLatentRevealProgress(0f),
            0.0001f,
        )
        assertTrue(
            CcTransitionPolicy
                .fastLatentRevealProgress(0.2f) > 0.5f,
        )
        assertEquals(
            1f,
            CcTransitionPolicy
                .fastLatentRevealProgress(0.35f),
            0.0001f,
        )
        assertEquals(
            1f,
            CcTransitionPolicy
                .fastLatentRevealProgress(1f),
            0.0001f,
        )
    }

    @Test
    fun latentReservationTracksCoverage() {
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
    fun exactBarsCompensateWithinBasis() {
        val outerScale =
            StatusPainter.MobileSignalMorphPolicy.outerSimilarityScale(
                targetWidthRatio = 1.5f,
                targetHeightRatio = 2f,
            )
        assertEquals(1f, outerScale, 0.0001f)
        assertEquals(
            1.5f,
            StatusPainter.MobileSignalMorphPolicy.exactTargetAxisCompensation(
                targetAxisRatio = 1.5f,
                outerScale = outerScale,
            ),
            0.0001f,
        )
        assertEquals(
            2f,
            StatusPainter.MobileSignalMorphPolicy.exactTargetAxisCompensation(
                targetAxisRatio = 2f,
                outerScale = outerScale,
            ),
            0.0001f,
        )
    }

    @Test
    fun exactBarsKeepOuterShrink() {
        val outerScale =
            StatusPainter.MobileSignalMorphPolicy.outerSimilarityScale(
                targetWidthRatio = 0.75f,
                targetHeightRatio = 0.5f,
            )
        assertEquals(0.5f, outerScale, 0.0001f)
        assertEquals(
            1.5f,
            StatusPainter.MobileSignalMorphPolicy.exactTargetAxisCompensation(
                targetAxisRatio = 0.75f,
                outerScale = outerScale,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            StatusPainter.MobileSignalMorphPolicy.exactTargetAxisCompensation(
                targetAxisRatio = 0.5f,
                outerScale = outerScale,
            ),
            0.0001f,
        )
    }

    @Test
    fun unmatchedComponentsClipWithoutScale() {
        assertEquals(
            1f,
            CcTransitionPolicy.unmatchedExitVisibleFraction(0f),
            0.0001f,
        )
        assertEquals(
            0.125f,
            CcTransitionPolicy.unmatchedExitVisibleFraction(0.5f),
            0.0001f,
        )
        assertEquals(
            0f,
            CcTransitionPolicy.unmatchedExitVisibleFraction(1f),
            0.0001f,
        )
    }

    @Test
    fun horizontalClipKeepsOpaqueEdge() {
        val policy = CcTransition
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
    fun latentMobileClipCoversTargetAxes() {
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
    fun peerTintNeverUsesBatteryTint() {
        val policy = CcTransition
        val peer = 0xffe0e0e0.toInt()
        val cached = 0xffdddddd.toInt()

        assertEquals(peer, policy.selectNativeTransitionTint(peer, cached))
        assertEquals(cached, policy.selectNativeTransitionTint(null, cached))
        assertNull(policy.selectNativeTransitionTint(null, null))
    }
}
