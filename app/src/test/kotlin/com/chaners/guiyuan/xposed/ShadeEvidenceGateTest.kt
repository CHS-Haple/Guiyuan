package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShadeEvidenceGateTest {
    @Test
    fun capturesStageBoundariesAndReverseDirectionWithoutPerFrameLogging() {
        val gate = ShadeEvidenceGate()
        val owner = Any()
        assertEquals("initial:collapsed", gate.observe(owner, 0f))
        assertEquals("opening:partial-low", gate.observe(owner, 0.1f))
        assertNull(gate.observe(owner, 0.2f))
        assertEquals("opening:partial-high", gate.observe(owner, 0.7f))
        assertEquals("opening:expanded", gate.observe(owner, 1f))
        assertEquals("closing:partial-high", gate.observe(owner, 0.8f))
        assertEquals("closing:partial-low", gate.observe(owner, 0.4f))
        assertEquals("closing:collapsed", gate.observe(owner, 0f))
    }

    @Test
    fun refusesNonFiniteProgressAndCapsLifetimeSamples() {
        val owner = Any()
        val gate = ShadeEvidenceGate(maxReports = 2)
        assertNull(gate.observe(owner, Float.NaN))
        assertNull(gate.observe(owner, Float.POSITIVE_INFINITY))
        assertNull(gate.observe(owner, -0.01f))
        assertEquals("initial:collapsed", gate.observe(owner, 0f))
        assertEquals("opening:partial-low", gate.observe(owner, 0.1f))
        assertNull(gate.observe(owner, 0.8f))
        gate.reset()
        assertTrue(gate.observe(owner, 0f) != null)
    }

    @Test
    fun newNativeCallbackRestartsStageObservationWithoutCreatingAnotherOwner() {
        val gate = ShadeEvidenceGate()
        assertEquals("initial:partial-low", gate.observe(Any(), 0.2f))
        assertEquals("initial:partial-low", gate.observe(Any(), 0.2f))
    }
}
