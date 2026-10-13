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
    fun dotsShrinkTowardTheFirstDotWithoutStacking() {
        for (i in 0..3) assertEquals(1f, morph.dotScale(i, 0f, 0f), 0f)
        assertEquals(1f, morph.dotScale(0, 1f, 0f), 0f)
        for (i in 1..3) {
            assertEquals(0.5f, morph.dotScale(i, 0.5f, 0f), 0.0001f)
            assertEquals(0f, morph.dotScale(i, 1f, 0f), 0f)
        }
        // Both the distance from the first dot and its radius shrink by the same ratio.
        assertEquals(40f, morph.dotCoordinate(70f, 10f, 100f, 130f, 0.5f, 0f, 0f), 0.0001f)
        assertEquals(10f, morph.dotCoordinate(70f, 10f, 100f, 130f, 1f, 0f, 0f), 0f)
        assertEquals(10f, morph.dotCoordinate(10f, 10f, 100f, 100f, 0.5f, 0f, 0f), 0f)
    }

    @Test
    fun oneDotMovesThenExpandsBeforeBarsGrow() {
        assertEquals(100f, morph.dotCoordinate(70f, 10f, 100f, 130f, 1f, 1f, 0f), 0f)
        assertEquals(130f, morph.dotCoordinate(70f, 10f, 100f, 130f, 1f, 1f, 1f), 0f)
        for (i in 0..3) assertEquals(1f, morph.dotScale(i, 1f, 1f), 0f)
        assertTrue(morph.dotScale(1, 1f, 0.2f) > morph.dotScale(2, 1f, 0.2f))
        assertEquals(0f, morph.handoffProgress(Float.NaN), 0f)
    }

    @Test
    fun neighboringStagesMeetWithoutAJumpOnReverse() {
        val stages = listOf(
            Triple(0f, 0f, 0f), Triple(1f, 0f, 0f),
            Triple(1f, 1f, 0f), Triple(1f, 1f, 1f),
        )
        val expected = listOf(70f, 10f, 100f, 130f)
        stages.forEachIndexed { index, (trim, move, spread) ->
            assertEquals(
                expected[index],
                morph.dotCoordinate(70f, 10f, 100f, 130f, trim, move, spread),
                0.0001f,
            )
        }
        assertEquals(0f, morph.rowProgress(0.60f), 0f)
        assertEquals(1f, morph.rowProgress(0.72f), 0.0001f)
    }
}
