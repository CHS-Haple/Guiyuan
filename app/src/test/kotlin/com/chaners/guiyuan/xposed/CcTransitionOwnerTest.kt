package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.network.InternetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CcTransitionOwnerTest {
    @Test
    fun dualSignalTargetNeedsVerifiedAggregateOrTwoMatchingSubscriptions() {
        val policy = CcTransitionPolicy
        val active = setOf(1, 4)
        assertEquals(
            CcTransitionPolicy.DualMobileTarget.AGGREGATED,
            policy.dualMobileTarget(active, listOf(null), aggregateProven = true),
        )
        assertEquals(
            CcTransitionPolicy.DualMobileTarget.AGGREGATED,
            policy.dualMobileTarget(active, listOf(4), aggregateProven = true),
        )
        assertEquals(
            CcTransitionPolicy.DualMobileTarget.NONE,
            policy.dualMobileTarget(active, listOf(7), aggregateProven = true),
        )
        assertEquals(
            CcTransitionPolicy.DualMobileTarget.NONE,
            policy.dualMobileTarget(active, listOf(1), aggregateProven = false),
        )
        assertEquals(
            CcTransitionPolicy.DualMobileTarget.SEPARATE,
            policy.dualMobileTarget(active, listOf(4, 1), aggregateProven = false),
        )
        listOf(listOf(1, 1), listOf(1, null), listOf(1, 7), emptyList(), listOf(1, 4, 7))
            .forEach { ids ->
                assertEquals(
                    CcTransitionPolicy.DualMobileTarget.NONE,
                    policy.dualMobileTarget(active, ids, aggregateProven = true),
                )
            }
        assertEquals(
            CcTransitionPolicy.DualMobileTarget.NONE,
            policy.dualMobileTarget(setOf(1), listOf(null), aggregateProven = true),
        )
    }

    @Test
    fun physicalSimSlotIdentityMapsTwoNativeRootsRegardlessOfViewOrder() {
        val slots = mapOf(0 to 4, 1 to 1)
        val separate = NativePresentationResolver.Mode.DUAL_SEPARATE
        assertEquals(
            listOf(1, 0),
            CcTransitionPolicy.dualRootRowIndices(separate, slots, listOf(1, 4)),
        )
        assertEquals(
            listOf(0, 1),
            CcTransitionPolicy.dualRootRowIndices(separate, slots, listOf(4, 1)),
        )
        for (ids in listOf(listOf(1, 1), listOf(1, null), listOf(1, 7))) {
            assertEquals(
                null,
                CcTransitionPolicy.dualRootRowIndices(separate, slots, ids),
            )
        }
        assertEquals(
            null,
            CcTransitionPolicy.dualRootRowIndices(
                NativePresentationResolver.Mode.DUAL_AGGREGATED,
                slots,
                listOf(1, 4),
            ),
        )
        assertEquals(
            null,
            CcTransitionPolicy.dualRootRowIndices(
                separate,
                mapOf(0 to 4),
                listOf(1, 4),
            ),
        )
    }

    @Test
    fun dualNativeGroupUsesBothTargetsWithoutGuessingRows() {
        val first = floatArrayOf(10f, 20f, 8f, 0f, 0f, 6f)
        val second = floatArrayOf(30f, 20f, 8f, 0f, 0f, 6f)
        val merged = CcTransitionPolicy.dualMobileGroupGeometry(listOf(first, second))
        val bounds = requireNotNull(merged)
        listOf(20f, 20f, 28f, 0f, 0f, 6f).forEachIndexed { index, value ->
            assertEquals(value, bounds[index], 0.0001f)
        }
        assertTrue(CcTransitionPolicy.dualMobileGroupGeometry(listOf(first))!!.contentEquals(first))
        assertEquals(null, CcTransitionPolicy.dualMobileGroupGeometry(listOf(first, second, first)))
        assertEquals(
            null,
            CcTransitionPolicy.dualMobileGroupGeometry(
                listOf(first, second.copyOf().also { it[3] = 6f }),
            ),
        )
        val mirrored = CcTransitionPolicy.dualMobileGroupGeometry(
            listOf(
                first.copyOf().also { it[2] = -8f },
                second.copyOf().also { it[2] = -8f },
            ),
        )
        assertEquals(-28f, requireNotNull(mirrored)[2], 0.0001f)
        assertEquals(
            null,
            CcTransitionPolicy.dualMobileGroupGeometry(
                listOf(first, floatArrayOf(Float.NaN, 2f, 8f, 0f, 0f, 6f)),
            ),
        )
    }

    @Test
    fun dualRowsStayVerticallyPairedDuringMovingCollapse() {
        val top = transitionGeometry(centerX = 20f, centerY = 10f, width = 40f, height = 10f)
        val bottom = transitionGeometry(centerX = 20f, centerY = 32f, width = 40f, height = 10f)
        val topTarget = transitionGeometry(centerX = 100f, centerY = 5f, width = 12f, height = 18f)
        val bottomTarget = transitionGeometry(centerX = 135f, centerY = 49f, width = 8f, height = 20f)

        fun frame(source: FloatArray, target: FloatArray, progress: Float): FloatArray =
            CcTransitionPolicy.dualMobileRowMotionGeometry(
                source = source,
                target = target,
                groupTravelX = 60f,
                groupTravelY = 15f,
                progress = progress,
                scalePolicy = StatusPainter.TransitionScalePolicy.SHRINK_ONLY,
            )

        for (progress in listOf(0f, 0.08f, 0.15f, 0.24f, 0.30f)) {
            val a = frame(top, topTarget, progress)
            val b = frame(bottom, bottomTarget, progress)
            assertEquals(a[0], b[0], 0.0001f)
            assertEquals(22f, b[1] - a[1], 0.0001f)
            assertEquals(a[2], b[2], 0.0001f)
            assertEquals(a[5], b[5], 0.0001f)
            assertEquals(
                20f + 60f * MobileSignalMorphPolicy.lockstepProgress(progress),
                a[0], 0.0001f,
            )
        }

        val separatedTop = frame(top, topTarget, 0.435f)
        val separatedBottom = frame(bottom, bottomTarget, 0.435f)
        assertTrue(separatedTop[0] < separatedBottom[0])
        for (progress in listOf(0.57f, 0.72f, 1f)) {
            val a = frame(top, topTarget, progress)
            val b = frame(bottom, bottomTarget, progress)
            assertEquals(topTarget[0], a[0], 0.0001f)
            assertEquals(topTarget[1], a[1], 0.0001f)
            assertEquals(bottomTarget[0], b[0], 0.0001f)
            assertEquals(bottomTarget[1], b[1], 0.0001f)
            // SHRINK_ONLY uses one similarity scale rather than stretching to the target.
            assertEquals(12f, a[2], 0.0001f)
            assertEquals(8f, b[2], 0.0001f)
        }
        for (boundary in listOf(0.30f, 0.57f)) {
            val before = frame(top, topTarget, boundary - 0.00001f)
            val after = frame(top, topTarget, boundary + 0.00001f)
            assertEquals(before[0], after[0], 0.01f)
            assertEquals(before[1], after[1], 0.01f)
        }
    }

    @Test
    fun dualCenterNeedsMatchingDataSubscriptionAndNativeType() {
        val source =
            CenterIndicator.MobileType("5G", false, InternetState.VALIDATED)
        val target =
            NativePresentationResolver.NetworkType(
                "5G",
                false,
                NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
            )
        fun matches(
            dataSubId: Int,
            targetSubId: Int?,
            nativeType: NativePresentationResolver.NetworkType?,
        ) = CcTransitionPolicy.matchesDualMobileTypeTarget(
                dataSubId = dataSubId,
                targetSubId = targetSubId,
                source = source,
                target = nativeType,
            )

        assertEquals(true, matches(4, 4, target))
        assertEquals(false, matches(4, 1, target))
        assertEquals(false, matches(-1, 4, target))
        assertEquals(false, matches(4, null, target))
        assertEquals(false, matches(4, 4, target.copy(label = "4G")))
        assertEquals(false, matches(4, 4, null))
    }

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
        val hideStart = 0f
        val hideMid = 0.09f
        val hideEnd = 0.181f
        val revealStart = 0.85f
        val revealEnd = 0.90f

        assertEquals(0f, policy.chargingRingLifetimeProgress(hideStart), 0.001f)
        assertEquals(0.40f, policy.chargingRingLifetimeProgress(0.18f), 0.001f)
        assertEquals(1f, policy.chargingSourceVisibleFraction(hideStart), 0.001f)
        assertEquals(0.20f, policy.chargingRingLifetimeProgress(hideMid), 0.001f)
        assertEquals(0.50f, policy.chargingSourceVisibleFraction(hideMid), 0.001f)
        assertEquals(0f, policy.chargingSourceVisibleFraction(hideEnd), 0.001f)
        // Ring-coupled source hide is the same with or without an endpoint.
        assertEquals(
            policy.chargingSourceVisibleFraction(hideMid),
            policy.chargingVisibleFraction(hideMid, targetAvailable = false),
            0.0001f,
        )
        assertEquals(
            policy.chargingSourceVisibleFraction(hideMid),
            policy.chargingVisibleFraction(hideMid, targetAvailable = true),
            0.0001f,
        )
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
        val hideEnd = 0.181f
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
