package com.chaners.guiyuan.xposed.battery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryRingLeftExitTest {
    @Test
    fun leftExitClearsLeftSideFirst() {
        val result =
            BatteryRingTransitionPolicy.resolve(
                drawableArcs = listOf(
                    BatteryTopArcPolicy.Arc(150f, 240f),
                ),
                batteryPercent = 100,
                progress = 0.5f,
                exitDirection = BatteryRingTransitionPolicy.ExitDirection.LEFT,
            )
        val remaining = BatteryRingTransitionPolicy.remainingFraction(0.5f)
        assertEquals(150f + 240f * (1f - remaining), result.background.single().startDegrees, 0.0001f)
        assertEquals(240f * remaining, result.background.single().sweepDegrees, 0.0001f)
    }

    @Test
    fun leftExitDefaultsToOriginalBatteryFillIntersection() {
        val progress = 0.12f
        val result =
            BatteryRingTransitionPolicy.resolve(
                drawableArcs = listOf(
                    BatteryTopArcPolicy.Arc(150f, 240f),
                ),
                batteryPercent = 75,
                progress = progress,
                exitDirection = BatteryRingTransitionPolicy.ExitDirection.LEFT,
            )
        val remaining = BatteryRingTransitionPolicy.remainingFraction(progress)
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
            BatteryRingTransitionPolicy.resolve(
                drawableArcs = listOf(
                    BatteryTopArcPolicy.Arc(150f, 240f),
                ),
                batteryPercent = 75,
                progress = progress,
                exitDirection = BatteryRingTransitionPolicy.ExitDirection.LEFT,
                followRetractEndpoint = true,
            )
        val remaining = BatteryRingTransitionPolicy.remainingFraction(progress)
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
            BatteryRingTransitionPolicy.resolve(
                drawableArcs =
                    listOf(
                        BatteryTopArcPolicy.Arc(150f, 90f),
                        BatteryTopArcPolicy.Arc(300f, 90f),
                    ),
                batteryPercent = 50,
                progress = progress,
                exitDirection = BatteryRingTransitionPolicy.ExitDirection.LEFT,
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
                BatteryTopArcPolicy.Arc(150f, 240f),
            )
        val before =
            BatteryRingTransitionPolicy.resolve(
                drawableArcs = arcs,
                batteryPercent = 36,
                progress = progress,
                exitDirection = BatteryRingTransitionPolicy.ExitDirection.LEFT,
                followRetractEndpoint = true,
            )
        val after =
            BatteryRingTransitionPolicy.resolve(
                drawableArcs = arcs,
                batteryPercent = 37,
                progress = progress,
                exitDirection = BatteryRingTransitionPolicy.ExitDirection.LEFT,
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
            BatteryRingTransitionPolicy.resolve(
                drawableArcs = listOf(
                    BatteryTopArcPolicy.Arc(150f, 240f),
                ),
                batteryPercent = 75,
                progress = progress,
                exitDirection = BatteryRingTransitionPolicy.ExitDirection.LEFT,
                followRetractEndpoint = true,
            )
        val remaining = BatteryRingTransitionPolicy.remainingFraction(progress)
        val retainedSweep = 240f * remaining
        assertTrue(retainedSweep < 240f * 0.75f)
        assertEquals(retainedSweep, result.active.single().sweepDegrees, 0.0001f)
        assertEquals(result.background.single().startDegrees, result.active.single().startDegrees, 0.0001f)
    }

    @Test
    fun leftExitBatteryFillAndBackgroundReachZeroTogether() {
        val completed =
            BatteryRingTransitionPolicy.resolve(
                drawableArcs = listOf(
                    BatteryTopArcPolicy.Arc(150f, 240f),
                ),
                batteryPercent = 36,
                progress = 1f,
                exitDirection = BatteryRingTransitionPolicy.ExitDirection.LEFT,
                followRetractEndpoint = true,
            )
        assertTrue(completed.background.isEmpty())
        assertTrue(completed.active.isEmpty())
    }
}
