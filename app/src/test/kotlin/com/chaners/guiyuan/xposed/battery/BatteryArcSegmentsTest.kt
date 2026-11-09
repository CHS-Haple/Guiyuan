package com.chaners.guiyuan.xposed.battery

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryArcSegmentsTest {
    @Test
    fun emptyBatteryUsesOnlyInactiveArc() {
        val result = arc(0)

        assertEquals(0f, result.activeSweep, 0.001f)
        assertEquals(150f, result.inactiveStart, 0.001f)
        assertEquals(240f, result.inactiveSweep, 0.001f)
    }

    @Test
    fun halfBatteryPartitionsTheRingWithoutOverlap() {
        val result = arc(50)

        assertEquals(120f, result.activeSweep, 0.001f)
        assertEquals(270f, result.inactiveStart, 0.001f)
        assertEquals(120f, result.inactiveSweep, 0.001f)
        assertEquals(240f, result.activeSweep + result.inactiveSweep, 0.001f)
    }

    @Test
    fun fullBatteryUsesOnlyActiveArc() {
        val result = arc(100)

        assertEquals(240f, result.activeSweep, 0.001f)
        assertEquals(390f, result.inactiveStart, 0.001f)
        assertEquals(0f, result.inactiveSweep, 0.001f)
    }

    @Test
    fun percentIsClampedBeforePartitioning() {
        assertEquals(0f, arc(-10).activeSweep, 0.001f)
        assertEquals(240f, arc(140).activeSweep, 0.001f)
    }

    private fun arc(percent: Int) =
        batteryArcSegments(
            batteryPercent = percent,
            startDegrees = 150f,
            maxSweep = 240f,
            degreesPerPercent = 2.4f,
        )
}
