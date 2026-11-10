package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CcTransitionSignalScaleTest {
    @Test
    fun mobileTypeWeightInterpolatesToNativeTarget() {
        assertEquals(
            800,
            MobileTypeTransitionPolicy.resolveWeight(
                sourceWeight = 800,
                targetWeight = 500,
                progress = 0f,
            ),
        )
        assertEquals(
            650,
            MobileTypeTransitionPolicy.resolveWeight(
                sourceWeight = 800,
                targetWeight = 500,
                progress = 0.5f,
            ),
        )
        assertEquals(
            500,
            MobileTypeTransitionPolicy.resolveWeight(
                sourceWeight = 800,
                targetWeight = 500,
                progress = 1f,
            ),
        )
    }

    @Test
    fun mobileTypeWeightFailsNativeWhenTargetTypographyIsUnavailable() {
        assertEquals(
            800,
            MobileTypeTransitionPolicy.resolveWeight(
                sourceWeight = 800,
                targetWeight = null,
                progress = 1f,
            ),
        )
    }

    @Test
    fun shrinkOnlyScalePolicyNeverEnlargesSemanticGlyphs() {
        val source = transitionGeometry(width = 10f, height = 10f)
        val target = transitionGeometry(width = 30f, height = 20f)

        val result =
            CcTransitionPolicy.interpolateSimilarityGeometry(
                source = source,
                target = target,
                progress = 1f,
                scalePolicy = StatusPainter.TransitionScalePolicy.SHRINK_ONLY,
            )

        assertEquals(10f, result[2], 0.0001f)
        assertEquals(10f, result[5], 0.0001f)
    }

    @Test
    fun targetScalePolicyStillAllowsWifiOpticalConvergence() {
        val source = transitionGeometry(width = 10f, height = 10f)
        val target = transitionGeometry(width = 20f, height = 20f)

        val result =
            CcTransitionPolicy.interpolateSimilarityGeometry(
                source = source,
                target = target,
                progress = 1f,
                scalePolicy = StatusPainter.TransitionScalePolicy.TARGET,
            )

        assertEquals(20f, result[2], 0.0001f)
        assertEquals(20f, result[5], 0.0001f)
    }

    @Test
    fun fourBarSnapshotTargetsRemainOrderedAndBounded() {
        val snapshot =
            ParticipantVisualSnapshot.Snapshot(
                envelope =
                    ParticipantVisualSnapshot.NormalizedRect(
                        left = 0.1f,
                        top = 0.2f,
                        right = 0.9f,
                        bottom = 0.9f,
                    ),
                components =
                    listOf(
                        ParticipantVisualSnapshot.NormalizedRect(0.1f, 0.55f, 0.2f, 0.9f),
                        ParticipantVisualSnapshot.NormalizedRect(0.3f, 0.45f, 0.4f, 0.9f),
                        ParticipantVisualSnapshot.NormalizedRect(0.5f, 0.35f, 0.6f, 0.9f),
                        ParticipantVisualSnapshot.NormalizedRect(0.7f, 0.2f, 0.8f, 0.9f),
                    ),
                topology =
                    ParticipantVisualSnapshot.Topology.FOUR_VERTICAL_BARS,
            )

        val bars = requireNotNull(snapshot.fourVerticalBarsWithinEnvelope())
        assertEquals(4, bars.size)
        assertTrue(bars.zipWithNext().all { (left, right) -> left.centerX < right.centerX })
        assertTrue(bars.all { bar -> bar.left >= 0f && bar.right <= 1f })
        assertTrue(bars.all { bar -> bar.top >= 0f && bar.bottom <= 1f })
    }

    @Test
    fun roundedCapsAreIncludedInsideTheNativeOpticalHeightBudget() {
        assertEquals(
            45f,
            MobileSignalTargetGeometry.targetMaxBarHeight(
                sourceBoundsHeight = 40f,
                diameter = 10f,
                targetHeightRatio = 1.25f,
            ),
            0.0001f,
        )
    }
}
