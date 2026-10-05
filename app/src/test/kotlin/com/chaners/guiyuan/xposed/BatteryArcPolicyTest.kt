package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryArcPolicyTest {
    @Test
    fun emptyBatteryUsesOnlyInactiveArc() {
        val result = resolve(0)

        assertEquals(0f, result.activeSweep, 0.001f)
        assertEquals(150f, result.inactiveStart, 0.001f)
        assertEquals(240f, result.inactiveSweep, 0.001f)
    }

    @Test
    fun halfBatteryPartitionsTheRingWithoutOverlap() {
        val result = resolve(50)

        assertEquals(120f, result.activeSweep, 0.001f)
        assertEquals(270f, result.inactiveStart, 0.001f)
        assertEquals(120f, result.inactiveSweep, 0.001f)
        assertEquals(240f, result.activeSweep + result.inactiveSweep, 0.001f)
    }

    @Test
    fun fullBatteryUsesOnlyActiveArc() {
        val result = resolve(100)

        assertEquals(240f, result.activeSweep, 0.001f)
        assertEquals(390f, result.inactiveStart, 0.001f)
        assertEquals(0f, result.inactiveSweep, 0.001f)
    }

    @Test
    fun percentIsClampedBeforePartitioning() {
        assertEquals(0f, resolve(-10).activeSweep, 0.001f)
        assertEquals(240f, resolve(140).activeSweep, 0.001f)
    }

    private fun resolve(percent: Int) =
        BatteryArcPolicy.resolve(
            batteryPercent = percent,
            startDegrees = 150f,
            maxSweep = 240f,
            degreesPerPercent = 2.4f,
        )
}
