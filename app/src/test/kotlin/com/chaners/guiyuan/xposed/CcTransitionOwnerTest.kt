package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CcTransitionOwnerTest {
    @Test
    fun targetTypographyStyleConvergesBeforeNativeHandoff() {
        assertEquals(
            0f,
            TransitionTypographyPolicy.styleProgress(0.42f),
            0.0001f,
        )
        assertTrue(
            TransitionTypographyPolicy.styleProgress(0.70f) in 0f..1f,
        )
        assertEquals(
            1f,
            TransitionTypographyPolicy.styleProgress(0.88f),
            0.0001f,
        )
        assertEquals(
            1f,
            TransitionTypographyPolicy.styleProgress(1f),
            0.0001f,
        )
    }

    @Test
    fun exactTextGeometryReachesNativeBasisInsteadOfSimilarityEnvelope() {
        val source = transitionGeometry(centerX = 10f, centerY = 20f, width = 10f, height = 20f)
        val target = transitionGeometry(centerX = 100f, centerY = 200f, width = 30f, height = 24f)

        val result =
            CcTransitionPolicy.interpolateGeometry(
                source = source,
                target = target,
                progress = 1f,
            )

        target.forEachIndexed { index, value ->
            assertEquals(value, result[index], 0.0001f)
        }
    }

    @Test
    fun chargingGlyphUsesOpaqueClipHideAndAcceleratedLateReveal() {
        val policy = BatteryNumberFollowerPolicy
        val (hideStart, hideEnd) = policy.sourceHideWindow()
        val (revealStart, revealEnd) = policy.targetRevealWindow()

        assertEquals(0.85f, revealStart, 0.0001f)
        assertEquals(0.90f, revealEnd, 0.0001f)

        assertEquals(0f, policy.chargingRingLifetimeProgress(hideStart), 0.001f)
        assertEquals(0.40f, policy.chargingRingLifetimeProgress(hideEnd), 0.001f)
        assertEquals(0.322f, policy.chargingRingRemaining(hideEnd), 0.01f)
        assertEquals(1f, policy.chargingSourceVisibleFraction(hideStart), 0.001f)
        val hideMid = (hideStart + hideEnd) / 2f
        assertEquals(0.20f, policy.chargingRingLifetimeProgress(hideMid), 0.001f)
        assertEquals(0.50f, policy.chargingSourceVisibleFraction(hideMid), 0.001f)
        assertEquals(0f, policy.chargingSourceVisibleFraction(hideEnd), 0.001f)
        assertEquals(
            1f,
            policy.chargingVisibleFraction(
                progress = hideStart,
                targetAvailable = true,
            ),
            0.0001f,
        )
        assertEquals(
            0f,
            policy.chargingVisibleFraction(
                progress = hideEnd,
                targetAvailable = true,
            ),
            0.0001f,
        )

        // The glyph is fully clipped before its independent target travel starts.
        assertTrue(policy.chargingMotionProgress(hideEnd + 0.05f) > 0f)
        assertEquals(
            0f,
            policy.chargingVisibleFraction(
                progress = revealStart,
                targetAvailable = true,
            ),
            0.0001f,
        )
        assertTrue(
            policy.chargingVisibleFraction(
                progress = (revealStart + revealEnd) / 2f,
                targetAvailable = true,
            ) in 0f..1f,
        )
        assertEquals(
            1f,
            policy.chargingVisibleFraction(
                progress = revealEnd,
                targetAvailable = true,
            ),
            0.0001f,
        )

        // Fail-native: without a reliable charging target there is no reveal.
        assertEquals(
            0f,
            policy.chargingVisibleFraction(
                progress = 1f,
                targetAvailable = false,
            ),
            0.0001f,
        )
    }

    @Test
    fun chargingGlyphNeverUsesItsOwnTargetMotionWhileSourceClipRemains() {
        val policy = BatteryNumberFollowerPolicy
        val (_, hideEnd) = policy.sourceHideWindow()
        var observedPartialClip = false

        for (sample in 0..400) {
            val progress = sample / 1000f
            val sourceVisible = policy.chargingSourceVisibleFraction(progress)
            if (sourceVisible in 0.0001f..0.9999f) {
                observedPartialClip = true
            }
            if (sourceVisible > 0f) {
                assertEquals(
                    0f,
                    policy.chargingMotionProgress(progress),
                    0.0001f,
                )
            }
        }

        assertTrue(observedPartialClip)
        assertEquals(0f, policy.chargingSourceVisibleFraction(hideEnd), 0.0001f)
        assertTrue(policy.chargingMotionProgress(hideEnd + 0.05f) > 0f)
    }

    @Test
    fun chargingGlyphFollowerPreservesItsRelativeGeometryToBatteryNumber() {
        val numberSource =
            transitionGeometry(
                centerX = 100f,
                centerY = 50f,
                width = 20f,
                height = 10f,
            )
        val chargingSource =
            transitionGeometry(
                centerX = 130f,
                centerY = 50f,
                width = 6f,
                height = 6f,
            )
        val numberCurrent =
            transitionGeometry(
                centerX = 200f,
                centerY = 80f,
                width = 30f,
                height = 15f,
            )

        val follower =
            CcTransitionPolicy.followAnchorGeometry(
                follower = chargingSource,
                sourceAnchor = numberSource,
                currentAnchor = numberCurrent,
            )
        requireNotNull(follower)

        // Number grows by 1.5x and moves; charging glyph follows the exact same
        // transform, preserving its source-relative offset and scale.
        assertEquals(245f, follower[0], 0.0001f)
        assertEquals(80f, follower[1], 0.0001f)
        assertEquals(9f, follower[2], 0.0001f)
        assertEquals(9f, follower[5], 0.0001f)
    }

    @Test
    fun transitionTintHoldsEndsAndChangesOnlyInMiddlePhase() {
        val policy = CcTransitionPolicy
        val source = 0xffff6600.toInt()
        val target = 0xe6ffffff.toInt()

        assertEquals(0f, policy.transitionTintProgress(0f), 0.0001f)
        assertTrue(policy.transitionTintProgress(0.01f) > 0f)
        assertEquals(0.5f, policy.transitionTintProgress(0.225f), 0.0001f)
        assertEquals(1f, policy.transitionTintProgress(0.45f), 0.0001f)
        assertEquals(1f, policy.transitionTintProgress(0.90f), 0.0001f)

        assertEquals(
            source,
            policy.interpolateColor(
                source = source,
                target = target,
                progress = 0f,
            ),
        )
        assertEquals(
            0xf3ffb380.toInt(),
            policy.interpolateColor(
                source = source,
                target = target,
                progress = 0.225f,
            ),
        )
        assertEquals(
            target,
            policy.interpolateColor(
                source = source,
                target = target,
                progress = 0.80f,
            ),
        )
    }

    @Test
    fun followSystemParticipantsUseLiveNativeTintWhileCustomTintUsesOptionalTransition() {
        val policy = CcTransitionPolicy
        val source = 0xff202020.toInt()
        val target = 0xffeeeeee.toInt()

        assertEquals(
            source,
            policy.resolveTransitionTint(
                source = source,
                target = target,
                progress = 0.50f,
                tinted = true,
                transitionEnabled = false,
            ),
        )
        assertEquals(
            target,
            policy.resolveTransitionTint(
                source = source,
                target = target,
                progress = 0.50f,
                tinted = false,
                transitionEnabled = false,
            ),
        )
        assertEquals(
            policy.interpolateColor(source, target, 0.50f),
            policy.resolveTransitionTint(
                source = source,
                target = target,
                progress = 0.50f,
                tinted = true,
                transitionEnabled = true,
            ),
        )
        assertEquals(
            target,
            policy.resolveTransitionTint(
                source = source,
                target = target,
                progress = 0.20f,
                tinted = false,
                transitionEnabled = true,
            ),
        )
    }

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
            MobileSignalMorphPolicy.targetMaxBarHeight(
                sourceBoundsHeight = 40f,
                diameter = 10f,
                targetHeightRatio = 1.25f,
            ),
            0.0001f,
        )
    }
}
