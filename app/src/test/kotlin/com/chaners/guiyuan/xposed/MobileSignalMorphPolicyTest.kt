package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileSignalMorphPolicyTest {
    private val morph = MobileSignalMorphPolicy

    @Test
    fun trimAndSpreadHaveTheSameShortDuration() {
        fun phase(p: Float): Float = morph.handoffProgress(p * p)
        assertEquals(0f, morph.trimProgress(phase(0f)), 0f)
        assertEquals(1f, morph.trimProgress(phase(0.12f)), 0.0001f)
        assertEquals(0f, morph.moveProgress(phase(0.12f)), 0.0001f)
        assertEquals(1f, morph.moveProgress(phase(0.60f)), 0.0001f)
        assertEquals(0f, morph.rowProgress(phase(0.60f)), 0.0001f)
        assertEquals(1f, morph.rowProgress(phase(0.72f)), 0.0001f)
        assertEquals(0f, morph.barProgress(phase(0.72f)), 0.0001f)
        assertEquals(1f, morph.barProgress(phase(1f)), 0f)
        assertEquals(0.12f, 0.72f - 0.60f, 0.0001f)
    }

    @Test
    fun rightmostDotsVanishInPlaceLeavingTheLeftmost() {
        for (i in 0..3) assertEquals(1f, morph.dotScale(i, 0f, 0f), 0f)
        assertEquals(1f, morph.dotScale(0, 1f, 0f), 0f)
        for (i in 1..3) assertEquals(0f, morph.dotScale(i, 1f, 0f), 0f)
        assertTrue(morph.dotScale(3, 0.2f, 0f) < morph.dotScale(2, 0.2f, 0f))
        assertTrue(morph.dotScale(2, 0.5f, 0f) < morph.dotScale(1, 0.5f, 0f))
        assertEquals(10f, morph.dotCoordinate(10f, 100f, 130f, 0f, 0f), 0f)
    }

    @Test
    fun dotTravelsBeforeRestoringLeftToRight() {
        assertEquals(100f, morph.dotCoordinate(10f, 100f, 130f, 1f, 0f), 0f)
        assertEquals(130f, morph.dotCoordinate(10f, 100f, 130f, 1f, 1f), 0f)
        for (i in 0..3) assertEquals(1f, morph.dotScale(i, 1f, 1f), 0f)
        assertTrue(morph.dotScale(1, 1f, 0.2f) > morph.dotScale(2, 1f, 0.2f))
        assertEquals(0f, morph.handoffProgress(Float.NaN), 0f)
    }
}
