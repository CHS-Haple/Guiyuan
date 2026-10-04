package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedStatusBatteryRingTransitionPolicyTest {
    @Test
    fun transitionProgressFinishesRingAtFortyFivePercentWithoutJump() {
        assertEquals(
            0f,
            CombinedStatusBatteryRingTransitionPolicy.transitionProgress(0f),
            0.0001f,
        )
        assertEquals(
            0.5f,
            CombinedStatusBatteryRingTransitionPolicy.transitionProgress(0.225f),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusBatteryRingTransitionPolicy.transitionProgress(0.45f),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusBatteryRingTransitionPolicy.transitionProgress(0.6f),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusBatteryRingTransitionPolicy.transitionProgress(1f),
            0.0001f,
        )
    }

    @Test
    fun remainingFractionUsesContinuousFrontLoadedCurve() {
        assertEquals(
            1f,
            CombinedStatusBatteryRingTransitionPolicy.remainingFraction(0f),
            0.0001f,
        )
        assertEquals(
            0.615319f,
            CombinedStatusBatteryRingTransitionPolicy.remainingFraction(0.25f),
            0.0001f,
        )
        assertEquals(
            0.179334f,
            CombinedStatusBatteryRingTransitionPolicy.remainingFraction(0.5f),
            0.0001f,
        )
        assertEquals(
            0f,
            CombinedStatusBatteryRingTransitionPolicy.remainingFraction(1f),
            0.0001f,
        )
        assertEquals(
            1f,
            CombinedStatusBatteryRingTransitionPolicy.remainingFraction(Float.NaN),
            0.0001f,
        )
    }

    @Test
    fun globalCurveKeepsBuild550EarlyPaceAndExtendsTailContinuously() {
        fun remainingAtGlobal(progress: Float): Float =
            CombinedStatusBatteryRingTransitionPolicy.remainingFraction(
                CombinedStatusBatteryRingTransitionPolicy.transitionProgress(progress),
            )

        assertEquals(0.7337591f, remainingAtGlobal(0.0875f), 0.0001f)
        assertEquals(0.34119043f, remainingAtGlobal(0.175f), 0.0001f)
        assertEquals(0.01148136f, remainingAtGlobal(0.35f), 0.0001f)
        assertEquals(0f, remainingAtGlobal(0.45f), 0.0001f)
    }

    @Test
    fun terminalRoundCapTailEndsWhenArcLengthFallsBelowStrokeWidth() {
        assertFalse(
            CombinedStatusBatteryRingTransitionPolicy.isTerminalCapDominated(
                remainingFraction = 0.05f,
                totalSweepDegrees = 240f,
                radiusPx = 50f,
                strokeWidthPx = 8.25f,
            ),
        )
        assertTrue(
            CombinedStatusBatteryRingTransitionPolicy.isTerminalCapDominated(
                remainingFraction = 0.03f,
                totalSweepDegrees = 240f,
                radiusPx = 50f,
                strokeWidthPx = 8.25f,
            ),
        )
    }

    @Test
    fun retractKeepsGrayPathAndBatteryFillOnSamePrefix() {
        val result =
            CombinedStatusBatteryRingTransitionPolicy.resolve(
                drawableArcs =
                    listOf(
                        CombinedStatusBatteryTopArcPolicy.Arc(
                            startDegrees = 150f,
                            sweepDegrees = 240f,
                        ),
                    ),
                batteryPercent = 75,
                progress = 0.5f,
            )

        val remaining = CombinedStatusBatteryRingTransitionPolicy.remainingFraction(0.5f)
        assertEquals(1, result.background.size)
        assertEquals(150f, result.background.single().startDegrees, 0.0001f)
        assertEquals(240f * remaining, result.background.single().sweepDegrees, 0.0001f)
        assertEquals(1, result.active.size)
        assertEquals(150f, result.active.single().startDegrees, 0.0001f)
        assertEquals(240f * 0.75f * remaining, result.active.single().sweepDegrees, 0.0001f)
    }

    @Test
    fun retractConsumesOrderedPathAcrossTopGap() {
        val result =
            CombinedStatusBatteryRingTransitionPolicy.resolve(
                drawableArcs =
                    listOf(
                        CombinedStatusBatteryTopArcPolicy.Arc(
                            startDegrees = 150f,
                            sweepDegrees = 90f,
                        ),
                        CombinedStatusBatteryTopArcPolicy.Arc(
                            startDegrees = 300f,
                            sweepDegrees = 90f,
                        ),
                    ),
                batteryPercent = 100,
                progress = 0.25f,
            )

        val remaining = CombinedStatusBatteryRingTransitionPolicy.remainingFraction(0.25f)
        assertEquals(2, result.background.size)
        assertEquals(90f, result.background[0].sweepDegrees, 0.0001f)
        assertEquals(180f * remaining - 90f, result.background[1].sweepDegrees, 0.0001f)
        assertEquals(result.background, result.active)
    }

    @Test
    fun nonePreservesBuild543ActiveLengthSemantics() {
        val result =
            CombinedStatusBatteryRingTransitionPolicy.resolve(
                drawableArcs = listOf(
                    CombinedStatusBatteryTopArcPolicy.Arc(150f, 240f),
                ),
                batteryPercent = 75,
                progress = 0.5f,
                exitDirection = CombinedStatusBatteryRingTransitionPolicy.ExitDirection.NONE,
            )
        val remaining = CombinedStatusBatteryRingTransitionPolicy.remainingFraction(0.5f)
        assertEquals(240f * remaining, result.background.single().sweepDegrees, 0.0001f)
        assertEquals(240f * 0.75f * remaining, result.active.single().sweepDegrees, 0.0001f)
    }

    @Test
    fun rightExitPreservesBuild543ActiveLengthSemantics() {
        val result =
            CombinedStatusBatteryRingTransitionPolicy.resolve(
                drawableArcs = listOf(
                    CombinedStatusBatteryTopArcPolicy.Arc(150f, 240f),
                ),
                batteryPercent = 75,
                progress = 0.5f,
                exitDirection = CombinedStatusBatteryRingTransitionPolicy.ExitDirection.RIGHT,
            )
        val remaining = CombinedStatusBatteryRingTransitionPolicy.remainingFraction(0.5f)
        assertEquals(150f, result.background.single().startDegrees, 0.0001f)
        assertEquals(240f * remaining, result.background.single().sweepDegrees, 0.0001f)
        assertEquals(150f, result.active.single().startDegrees, 0.0001f)
        assertEquals(240f * 0.75f * remaining, result.active.single().sweepDegrees, 0.0001f)
    }

    @Test
    fun leftExitClearsLeftSideFirst() {
        val result =
            CombinedStatusBatteryRingTransitionPolicy.resolve(
                drawableArcs = listOf(
                    CombinedStatusBatteryTopArcPolicy.Arc(150f, 240f),
                ),
                batteryPercent = 100,
                progress = 0.5f,
                exitDirection = CombinedStatusBatteryRingTransitionPolicy.ExitDirection.LEFT,
            )
        val remaining = CombinedStatusBatteryRingTransitionPolicy.remainingFraction(0.5f)
        assertEquals(150f + 240f * (1f - remaining), result.background.single().startDegrees, 0.0001f)
        assertEquals(240f * remaining, result.background.single().sweepDegrees, 0.0001f)
    }

    @Test
    fun leftExitDefaultsToOriginalBatteryFillIntersection() {
        val progress = 0.12f
        val result =
            CombinedStatusBatteryRingTransitionPolicy.resolve(
                drawableArcs = listOf(
                    CombinedStatusBatteryTopArcPolicy.Arc(150f, 240f),
                ),
                batteryPercent = 75,
                progress = progress,
                exitDirection = CombinedStatusBatteryRingTransitionPolicy.ExitDirection.LEFT,
            )
        val remaining = CombinedStatusBatteryRingTransitionPolicy.remainingFraction(progress)
        val retainedStart = 240f * (1f - remaining)
        val originalActiveEnd = 240f * 0.75f
        assertTrue(retainedStart < originalActiveEnd)
        assertEquals(
            150f + retainedStart,
            result.active.single().startDegrees,
            0.0001f,
        )
        assertEquals(
            originalActiveEnd - retainedStart,
            result.active.single().sweepDegrees,
            0.0001f,
        )
    }

    @Test
    fun leftExitCarriesBatteryFillWithoutShrinkingWhileHollowRemains() {
        val progress = 0.12f
        val result =
            CombinedStatusBatteryRingTransitionPolicy.resolve(
                drawableArcs = listOf(
                    CombinedStatusBatteryTopArcPolicy.Arc(150f, 240f),
                ),
                batteryPercent = 75,
                progress = progress,
                exitDirection = CombinedStatusBatteryRingTransitionPolicy.ExitDirection.LEFT,
                followRetractEndpoint = true,
            )
        val remaining = CombinedStatusBatteryRingTransitionPolicy.remainingFraction(progress)
        val retainedStart = 240f * (1f - remaining)
        val originalActiveSweep = 240f * 0.75f
        assertTrue(240f * remaining > originalActiveSweep)
        assertEquals(150f + retainedStart, result.active.single().startDegrees, 0.0001f)
        assertEquals(originalActiveSweep, result.active.single().sweepDegrees, 0.0001f)
    }

    @Test
    fun leftExitCarriesFixedFillAcrossTopGapWhileHollowRemains() {
        val progress = 0.12f
        val result =
            CombinedStatusBatteryRingTransitionPolicy.resolve(
                drawableArcs =
                    listOf(
                        CombinedStatusBatteryTopArcPolicy.Arc(150f, 90f),
                        CombinedStatusBatteryTopArcPolicy.Arc(300f, 90f),
                    ),
                batteryPercent = 50,
                progress = progress,
                exitDirection = CombinedStatusBatteryRingTransitionPolicy.ExitDirection.LEFT,
                followRetractEndpoint = true,
            )
        val activeSweep = result.active.sumOf { it.sweepDegrees.toDouble() }.toFloat()
        val backgroundSweep = result.background.sumOf { it.sweepDegrees.toDouble() }.toFloat()
        assertTrue(backgroundSweep > 90f)
        assertEquals(90f, activeSweep, 0.0001f)
        assertTrue(result.active.size >= 1)
    }

    @Test
    fun leftExitUsesLiveBatteryPercentWithoutMovingRetractEndpoint() {
        val progress = 0.12f
        val arcs =
            listOf(
                CombinedStatusBatteryTopArcPolicy.Arc(150f, 240f),
            )
        val before =
            CombinedStatusBatteryRingTransitionPolicy.resolve(
                drawableArcs = arcs,
                batteryPercent = 36,
                progress = progress,
                exitDirection = CombinedStatusBatteryRingTransitionPolicy.ExitDirection.LEFT,
                followRetractEndpoint = true,
            )
        val after =
            CombinedStatusBatteryRingTransitionPolicy.resolve(
                drawableArcs = arcs,
                batteryPercent = 37,
                progress = progress,
                exitDirection = CombinedStatusBatteryRingTransitionPolicy.ExitDirection.LEFT,
                followRetractEndpoint = true,
            )

        assertEquals(
            before.background.single().startDegrees,
            after.background.single().startDegrees,
            0.0001f,
        )
        assertEquals(
            before.active.single().startDegrees,
            after.active.single().startDegrees,
            0.0001f,
        )
        assertEquals(240f * 0.36f, before.active.single().sweepDegrees, 0.0001f)
        assertEquals(240f * 0.37f, after.active.single().sweepDegrees, 0.0001f)
    }

    @Test
    fun leftExitStartsShrinkingFillOnlyAfterHollowIsExhausted() {
        val progress = 0.5f
        val result =
            CombinedStatusBatteryRingTransitionPolicy.resolve(
                drawableArcs = listOf(
                    CombinedStatusBatteryTopArcPolicy.Arc(150f, 240f),
                ),
                batteryPercent = 75,
                progress = progress,
                exitDirection = CombinedStatusBatteryRingTransitionPolicy.ExitDirection.LEFT,
                followRetractEndpoint = true,
            )
        val remaining = CombinedStatusBatteryRingTransitionPolicy.remainingFraction(progress)
        val retainedSweep = 240f * remaining
        assertTrue(retainedSweep < 240f * 0.75f)
        assertEquals(retainedSweep, result.active.single().sweepDegrees, 0.0001f)
        assertEquals(result.background.single().startDegrees, result.active.single().startDegrees, 0.0001f)
    }

    @Test
    fun leftExitBatteryFillAndBackgroundReachZeroTogether() {
        val completed =
            CombinedStatusBatteryRingTransitionPolicy.resolve(
                drawableArcs = listOf(
                    CombinedStatusBatteryTopArcPolicy.Arc(150f, 240f),
                ),
                batteryPercent = 36,
                progress = 1f,
                exitDirection = CombinedStatusBatteryRingTransitionPolicy.ExitDirection.LEFT,
                followRetractEndpoint = true,
            )
        assertTrue(completed.background.isEmpty())
        assertTrue(completed.active.isEmpty())
    }

    @Test
    fun completedRetractLeavesNoTransitionRing() {
        val result =
            CombinedStatusBatteryRingTransitionPolicy.resolve(
                drawableArcs =
                    listOf(
                        CombinedStatusBatteryTopArcPolicy.Arc(
                            startDegrees = 150f,
                            sweepDegrees = 240f,
                        ),
                    ),
                batteryPercent = 63,
                progress = 1f,
            )

        assertTrue(result.background.isEmpty())
        assertTrue(result.active.isEmpty())
        assertEquals(0f, result.remainingFraction, 0.0001f)
    }
}
