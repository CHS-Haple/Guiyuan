package com.chaners.guiyuan.xposed.battery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryRingTransitionPolicyTest {
    @Test
    fun symmetricGapRetractionConsumesActualVisibleLengthAcrossBothSides() {
        val arcs = BatteryTopArcPolicy.drawableRingArcs(34f, 34f)
        val half = BatteryRingTransitionPolicy.resolve(
            drawableArcs = arcs,
            batteryPercent = 50,
            progress = 0f,
        )
        assertEquals(listOf(arcs.first()), half.active)
        assertEquals(arcs, half.background)
        val retracted = BatteryRingTransitionPolicy.resolve(
            drawableArcs = arcs,
            batteryPercent = 75,
            progress = 0.6f,
        )
        val remaining = BatteryRingTransitionPolicy.remainingFraction(0.6f)
        assertEquals(224f * remaining,
            retracted.background.sumOf { it.sweepDegrees.toDouble() }.toFloat(), 0.001f)
    }

    @Test
    fun transitionProgressFinishesRingAtFortyFivePercentWithoutJump() {
        assertEquals(
            0f,
            BatteryRingTransitionPolicy.transitionProgress(0f),
            0.0001f,
        )
        assertEquals(
            0.5f,
            BatteryRingTransitionPolicy.transitionProgress(0.225f),
            0.0001f,
        )
        assertEquals(
            1f,
            BatteryRingTransitionPolicy.transitionProgress(0.45f),
            0.0001f,
        )
        assertEquals(
            1f,
            BatteryRingTransitionPolicy.transitionProgress(0.6f),
            0.0001f,
        )
        assertEquals(
            1f,
            BatteryRingTransitionPolicy.transitionProgress(1f),
            0.0001f,
        )
    }

    @Test
    fun remainingFractionUsesContinuousFrontLoadedCurve() {
        assertEquals(
            1f,
            BatteryRingTransitionPolicy.remainingFraction(0f),
            0.0001f,
        )
        assertEquals(
            0.615319f,
            BatteryRingTransitionPolicy.remainingFraction(0.25f),
            0.0001f,
        )
        assertEquals(
            0.179334f,
            BatteryRingTransitionPolicy.remainingFraction(0.5f),
            0.0001f,
        )
        assertEquals(
            0f,
            BatteryRingTransitionPolicy.remainingFraction(1f),
            0.0001f,
        )
        assertEquals(
            1f,
            BatteryRingTransitionPolicy.remainingFraction(Float.NaN),
            0.0001f,
        )
    }

    @Test
    fun globalCurveKeepsEarlyPaceAndSmoothTail() {
        fun remainingAtGlobal(progress: Float): Float =
            BatteryRingTransitionPolicy.remainingFraction(
                BatteryRingTransitionPolicy.transitionProgress(progress),
            )

        assertEquals(0.7337591f, remainingAtGlobal(0.0875f), 0.0001f)
        assertEquals(0.34119043f, remainingAtGlobal(0.175f), 0.0001f)
        assertEquals(0.01148136f, remainingAtGlobal(0.35f), 0.0001f)
        assertEquals(0f, remainingAtGlobal(0.45f), 0.0001f)
    }

    @Test
    fun terminalRoundCapTailEndsWhenArcLengthFallsBelowStrokeWidth() {
        assertFalse(
            BatteryRingTransitionPolicy.isTerminalCapDominated(
                remainingFraction = 0.05f,
                totalSweepDegrees = 240f,
                radiusPx = 50f,
                strokeWidthPx = 8.25f,
            ),
        )
        assertTrue(
            BatteryRingTransitionPolicy.isTerminalCapDominated(
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
            BatteryRingTransitionPolicy.resolve(
                drawableArcs =
                    listOf(
                        BatteryTopArcPolicy.Arc(
                            startDegrees = 150f,
                            sweepDegrees = 240f,
                        ),
                    ),
                batteryPercent = 75,
                progress = 0.5f,
            )

        val remaining = BatteryRingTransitionPolicy.remainingFraction(0.5f)
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
            BatteryRingTransitionPolicy.resolve(
                drawableArcs =
                    listOf(
                        BatteryTopArcPolicy.Arc(
                            startDegrees = 150f,
                            sweepDegrees = 90f,
                        ),
                        BatteryTopArcPolicy.Arc(
                            startDegrees = 300f,
                            sweepDegrees = 90f,
                        ),
                    ),
                batteryPercent = 100,
                progress = 0.25f,
            )

        val remaining = BatteryRingTransitionPolicy.remainingFraction(0.25f)
        assertEquals(2, result.background.size)
        assertEquals(90f, result.background[0].sweepDegrees, 0.0001f)
        assertEquals(180f * remaining - 90f, result.background[1].sweepDegrees, 0.0001f)
        assertEquals(result.background, result.active)
    }

    @Test
    fun noneExitPreservesActiveLength() {
        val result =
            BatteryRingTransitionPolicy.resolve(
                drawableArcs = listOf(
                    BatteryTopArcPolicy.Arc(150f, 240f),
                ),
                batteryPercent = 75,
                progress = 0.5f,
                exitDirection = BatteryRingTransitionPolicy.ExitDirection.NONE,
            )
        val remaining = BatteryRingTransitionPolicy.remainingFraction(0.5f)
        assertEquals(240f * remaining, result.background.single().sweepDegrees, 0.0001f)
        assertEquals(240f * 0.75f * remaining, result.active.single().sweepDegrees, 0.0001f)
    }

    @Test
    fun rightExitPreservesActiveLength() {
        val result =
            BatteryRingTransitionPolicy.resolve(
                drawableArcs = listOf(
                    BatteryTopArcPolicy.Arc(150f, 240f),
                ),
                batteryPercent = 75,
                progress = 0.5f,
                exitDirection = BatteryRingTransitionPolicy.ExitDirection.RIGHT,
            )
        val remaining = BatteryRingTransitionPolicy.remainingFraction(0.5f)
        assertEquals(150f, result.background.single().startDegrees, 0.0001f)
        assertEquals(240f * remaining, result.background.single().sweepDegrees, 0.0001f)
        assertEquals(150f, result.active.single().startDegrees, 0.0001f)
        assertEquals(240f * 0.75f * remaining, result.active.single().sweepDegrees, 0.0001f)
    }

    @Test
    fun completedRetractLeavesNoTransitionRing() {
        val result =
            BatteryRingTransitionPolicy.resolve(
                drawableArcs =
                    listOf(
                        BatteryTopArcPolicy.Arc(
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
