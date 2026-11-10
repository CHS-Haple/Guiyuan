package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.xposed.network.InternetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CcTransitionMorphGeometryTest {
    @Test
    fun chargingFollowsEightDotsInsteadOfBatteryNumber() {
        val top = transitionGeometry(centerX = 20f, centerY = 10f, width = 40f, height = 10f)
        val bottom = transitionGeometry(centerX = 20f, centerY = 32f, width = 40f, height = 10f)
        val sourceGroup = requireNotNull(CcTransitionPolicy.dualMobileGroupGeometry(listOf(top, bottom)))
        // The bolt sits between the two rows, not above the ring.
        val bolt = transitionGeometry(
            centerX = sourceGroup[0],
            centerY = sourceGroup[1],
            width = 10f,
            height = 12f,
        )
        val topTarget = transitionGeometry(centerX = 108f, centerY = 15f, width = 12f, height = 18f)
        val bottomTarget = transitionGeometry(centerX = 138f, centerY = 44f, width = 12f, height = 18f)
        val numberSource = transitionGeometry(centerX = 20f, centerY = -30f, width = 20f, height = 10f)
        val numberCurrent = transitionGeometry(centerX = 20f, centerY = -80f, width = 20f, height = 10f)

        for (progress in listOf(0f, 0.15f, 0.30f, 0.435f, 0.57f)) {
            val rows = listOf(top to topTarget, bottom to bottomTarget).map { (source, target) ->
                CcTransitionPolicy.dualMobileRowMotionGeometry(
                    source = source,
                    target = target,
                    groupTravelX = 65f,
                    groupTravelY = 8f,
                    progress = progress,
                    scalePolicy = StatusPainter.TransitionScalePolicy.SHRINK_ONLY,
                )
            }
            val mobileCurrent = requireNotNull(CcTransitionPolicy.dualMobileGroupGeometry(rows))
            val follower = requireNotNull(
                CcTransitionPolicy.followAnchorGeometry(
                    follower = bolt,
                    sourceAnchor = sourceGroup,
                    currentAnchor = mobileCurrent,
                ),
            )
            // Identical center and scaling to the verified dot envelope.
            assertEquals(mobileCurrent[0], follower[0], 0.0001f)
            assertEquals(mobileCurrent[1], follower[1], 0.0001f)
            assertEquals(mobileCurrent[2] / sourceGroup[2] * bolt[2], follower[2], 0.0001f)
            // The unrelated top battery number moves vertically in the opposite direction.
            if (progress > 0f) {
                val batteryFollower = requireNotNull(
                    CcTransitionPolicy.followAnchorGeometry(
                        follower = bolt,
                        sourceAnchor = numberSource,
                        currentAnchor = numberCurrent,
                    ),
                )
                assertTrue(kotlin.math.abs(follower[1] - batteryFollower[1]) > 10f)
            }
        }
    }

    @Test
    fun chargingSourceVisibilityUsesIndependentFadeTimeline() {
        // Regression: the dot group has not reached its move-phase fade at
        // 30%, but the established charging source has already disappeared.
        // Its path may follow these rows; its clip timeline must not.
        val originalChargingFade = BatteryNumberFollowerPolicy
        assertTrue(originalChargingFade.chargingSourceVisibleFraction(0.12f) in 0f..1f)
        assertEquals(0f, originalChargingFade.chargingSourceVisibleFraction(0.30f), 0.0001f)
        assertEquals(1f, 1f - MobileSignalStagePolicy.moveProgress(0.30f), 0.0001f)
        for (progress in listOf(0.05f, 0.12f, 0.18f, 0.30f, 0.57f, 0.90f)) {
            assertEquals(
                originalChargingFade.chargingSourceVisibleFraction(progress),
                originalChargingFade.chargingVisibleFraction(
                    progress = progress,
                    targetAvailable = false,
                ),
                0.0001f,
            )
        }
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
                20f + 60f * MobileSignalStagePolicy.lockstepProgress(progress),
                a[0], 0.0001f,
            )
        }

        val separatedTop = frame(top, topTarget, 0.435f)
        val separatedBottom = frame(bottom, bottomTarget, 0.435f)
        assertTrue(separatedTop[0] < separatedBottom[0])
        // Once the anchors split, each row rejoins the single-SIM projection.
        for (progress in listOf(0.57f, 0.72f, 1f)) {
            for ((source, target) in listOf(top to topTarget, bottom to bottomTarget)) {
                val actual = frame(source, target, progress)
                val single = CcTransitionPolicy.interpolateSimilarityGeometry(
                    source = source,
                    target = target,
                    progress = progress,
                    scalePolicy = StatusPainter.TransitionScalePolicy.SHRINK_ONLY,
                )
                for (axis in actual.indices) {
                    assertEquals(single[axis], actual[axis], 0.0001f)
                }
            }
        }
        assertTrue(frame(top, topTarget, 0.57f)[0] < frame(top, topTarget, 1f)[0])
        assertTrue(frame(bottom, bottomTarget, 0.57f)[0] < frame(bottom, bottomTarget, 1f)[0])
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
    fun aggregateBarsAboveDotsKeepPairedFlightAndReachNativeRowGap() {
        val xs = listOf(0.12f, 0.35f, 0.58f, 0.81f)
        val top = xs.mapIndexed { i, x ->
            ParticipantVisualSnapshot.NormalizedRect(
                x, 0.43f - i * 0.07f, x + 0.08f, 0.52f,
            )
        }
        val bottom = xs.map { x ->
            ParticipantVisualSnapshot.NormalizedRect(x, 0.73f, x + 0.08f, 0.81f)
        }
        val rows = ParticipantVisualSnapshot.Snapshot(
            envelope = ParticipantVisualSnapshot.NormalizedRect(0f, 0f, 1f, 1f),
            components = top + bottom,
            topology = ParticipantVisualSnapshot.Topology.COMPOSITE,
        ).dualMobileRowsWithinEnvelope()
        requireNotNull(rows)
        assertEquals(ParticipantVisualSnapshot.RowShape.BARS, rows.topShape)
        assertEquals(ParticipantVisualSnapshot.RowShape.DOTS, rows.bottomShape)

        val bounds = StatusPainter.TransitionBounds(0f, 0f, 80f, 70f)
        val anchors = CcTransitionPolicy.aggregateAnchors(
            bounds = bounds,
            top = rows.top.first(),
            bottom = rows.bottom.first(),
            topShape = rows.topShape,
            bottomShape = rows.bottomShape,
            widthCompensation = 1f,
            heightCompensation = 1f,
            dotDiameter = 6f,
        )
        assertEquals(12.8f, anchors[0], 0.0001f)
        assertEquals(20.5f, anchors[2] - anchors[1], 0.0001f)

        val sourceX = 10f
        val upperY = 15f
        val lowerY = 29f
        fun pairedDots(p: Float): FloatArray {
            val move = MobileSignalStagePolicy.moveProgress(p)
            val spread = MobileSignalStagePolicy.rowProgress(p)
            return floatArrayOf(
                MobileSignalMorphPolicy.dotCoordinate(
                    sourceX, sourceX, anchors[0], anchors[0], 0f, move, spread,
                ),
                MobileSignalMorphPolicy.dotCoordinate(
                    upperY, upperY, anchors[1], anchors[1], 0f, move, spread,
                ),
                MobileSignalMorphPolicy.dotCoordinate(
                    sourceX, sourceX, anchors[0], anchors[0], 0f, move, spread,
                ),
                MobileSignalMorphPolicy.dotCoordinate(
                    lowerY, lowerY, anchors[2], anchors[2], 0f, move, spread,
                ),
            )
        }
        var lastGap = 14f
        for (p in listOf(0f, 0.15f, 0.30f, 0.38f, 0.47f, 0.57f)) {
            val paired = pairedDots(p)
            assertEquals(paired[0], paired[2], 0.0001f)
            val gap = paired[3] - paired[1]
            assertTrue(gap >= lastGap - 0.0001f)
            lastGap = gap
        }
        assertEquals(anchors[0], pairedDots(0.57f)[0], 0.0001f)
        assertEquals(anchors[2] - anchors[1],
            pairedDots(0.57f)[3] - pairedDots(0.57f)[1], 0.0001f)
        // Reverse uses the same native fractions, with no independent row clock.
        for (p in listOf(0.57f, 0.38f, 0.15f, 0f)) {
            assertEquals(pairedDots(p)[0], pairedDots(p)[2], 0.0001f)
        }
    }

    // One validated native fixture shared by independent type and activity contracts.
    private fun aggregateRootPresentation(): NativePresentationResolver.Snapshot =
        NativePresentationResolver.Snapshot(
            mode = NativePresentationResolver.Mode.DUAL_AGGREGATED,
            boundRoots = 2,
            visibleRoots = 1,
            activeSubscriptionIds = listOf(1, 4),
            activeSubsAuthoritative = true,
            platformActiveSubscriptionIds = setOf(1, 4),
            presentationRootSubscriptionId = 1,
            effectiveDataSubscriptionId = 4,
            networkTypeSubscriptionId = 4,
            networkType = NativePresentationResolver.NetworkType(
                "5G", false, NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
            ),
        )

    @Test
    fun aggregateTypeRequiresSoleVerifiedNativeRoot() {
        val presentation = aggregateRootPresentation()
        fun verified(
            state: NativePresentationResolver.Snapshot? = presentation,
            dataSubId: Int = 4,
            targetSubId: Int? = 1,
            soleNativeRoot: Boolean = true,
        ) = CcTransitionPolicy.verifiedAggregateTypeRoot(
                state, dataSubId, targetSubId, soleNativeRoot,
            )
        assertTrue(verified())
        assertEquals(false, verified(soleNativeRoot = false))
        assertEquals(false, verified(state = presentation.copy(
            mode = NativePresentationResolver.Mode.DUAL_SEPARATE,
        )))
        assertEquals(false, verified(state = presentation.copy(visibleRoots = 2)))
        assertEquals(false, verified(state = presentation.copy(activeSubsAuthoritative = false)))
        assertEquals(false, verified(state = presentation.copy(
            platformActiveSubscriptionIds = setOf(1, 7),
        )))
        assertEquals(false, verified(state = presentation.copy(networkTypeSubscriptionId = 1)))
        assertEquals(false, verified(state = presentation.copy(
            presentationRootSubscriptionId = 4,
        )))
        assertEquals(false, verified(dataSubId = 7))
        assertEquals(false, verified(targetSubId = 7))
        assertEquals(false, verified(state = null))
    }

    @Test
    fun aggregateMobileActivityKeepsTheSelectedDataPathOnOneNativeRoot() {
        val presentation = aggregateRootPresentation()
        // The only visible FINAL mobile root is owned by SIM1, but the
        // native activity drawable can represent the selected SIM2 data path.
        fun activity(
            state: NativePresentationResolver.Snapshot? = presentation,
            dataSubId: Int = 4,
            targetSubId: Int? = 1,
            soleVisibleRoot: Boolean = true,
        ) = CcTransitionPolicy.mobileActivityRootMatches(
            state, dataSubId, targetSubId, soleVisibleRoot,
        )
        assertTrue(activity())
        assertTrue(activity(targetSubId = 4, soleVisibleRoot = false))
        assertFalse(activity(soleVisibleRoot = false))
        assertFalse(activity(dataSubId = 7))
        assertFalse(activity(targetSubId = 7))
        assertFalse(activity(state = presentation.copy(
            mode = NativePresentationResolver.Mode.DUAL_SEPARATE,
        )))
        assertFalse(activity(state = presentation.copy(
            platformActiveSubscriptionIds = setOf(1, 7),
        )))
        assertFalse(activity(state = presentation.copy(
            networkTypeSubscriptionId = 1,
        )))
        assertTrue(activity(targetSubId = null))
        assertFalse(activity(state = null))
    }

    @Test
    fun aggregateTypeHandoffRejectsMismatchedNativeLabelOrTypography() {
        val presentation = aggregateRootPresentation()
        val target = requireNotNull(presentation.networkType)
        val source = CenterIndicator.MobileType("5G", false, InternetState.VALIDATED)
        val verifiedAggregateRoot = CcTransitionPolicy.verifiedAggregateTypeRoot(
            presentation, 4, 1, true,
        )
        assertTrue(CcTransitionPolicy.matchesDualMobileTypeTarget(
            dataSubId = 4,
            targetSubId = 1,
            source = source,
            target = target,
            verifiedAggregateRoot = verifiedAggregateRoot,
        ))
        assertEquals(false, CcTransitionPolicy.matchesDualMobileTypeTarget(
            dataSubId = 4,
            targetSubId = 1,
            source = source,
            target = target.copy(label = "4G"),
            verifiedAggregateRoot = verifiedAggregateRoot,
        ))
        assertEquals(false, CcTransitionPolicy.matchesDualMobileTypeTarget(
            dataSubId = 4,
            targetSubId = 1,
            source = source,
            target = null,
            verifiedAggregateRoot = verifiedAggregateRoot,
        ))
        assertEquals(false, CcTransitionPolicy.matchesDualMobileTypeTarget(
            dataSubId = 4,
            targetSubId = 1,
            source = source,
            target = target.copy(enhanced = true),
            verifiedAggregateRoot = verifiedAggregateRoot,
        ))
    }
}
