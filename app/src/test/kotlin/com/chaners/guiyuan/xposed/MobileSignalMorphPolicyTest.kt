package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileSignalMorphPolicyTest {
    private val morph = MobileSignalMorphPolicy

    @Test
    fun collapseEndsAtThirtyPercentWithoutChangingLaterPhases() {
        fun phase(p: Float): Float = morph.handoffProgress(p * p)
        assertEquals(0f, morph.trimProgress(phase(0f)), 0f)
        assertEquals(0.5f, morph.trimProgress(phase(0.15f)), 0.0001f)
        assertEquals(1f, morph.trimProgress(phase(0.30f)), 0.0001f)
        assertEquals(0f, morph.moveProgress(phase(0.30f)), 0.0001f)
        assertEquals(0.5f, morph.moveProgress(phase(0.435f)), 0.0001f)
        assertEquals(1f, morph.moveProgress(phase(0.57f)), 0.0001f)
        assertEquals(0f, morph.rowProgress(phase(0.57f)), 0.0001f)
        assertEquals(0.5f, morph.rowProgress(phase(0.645f)), 0.0001f)
        assertEquals(1f, morph.rowProgress(phase(0.72f)), 0.0001f)
        assertEquals(0f, morph.barProgress(phase(0.72f)), 0.0001f)
        assertEquals(1f, morph.barProgress(phase(1f)), 0f)
    }

    @Test
    fun dotsCollapseInReverseRevealOrderWithoutMovingTheAnchor() {
        for (i in 0..3) assertEquals(1f, morph.dotScale(i, morph.dotTrim(i, 0f), 0f), 0f)
        assertEquals(0f, morph.dotTrim(0, 1f), 0f)
        assertEquals(1f, morph.dotScale(0, morph.dotTrim(0, 1f), 0f), 0f)

        // Each third of the trim stage removes just one dot, right-to-left.
        val first = 1f / 6f
        val middle = 0.5f
        val last = 5f / 6f
        assertEquals(0.5f, morph.dotTrim(3, first), 0.0001f)
        assertEquals(0f, morph.dotTrim(2, first), 0f)
        assertEquals(0f, morph.dotTrim(1, first), 0f)
        assertEquals(1f, morph.dotTrim(3, middle), 0f)
        assertEquals(0.5f, morph.dotTrim(2, middle), 0.0001f)
        assertEquals(0f, morph.dotTrim(1, middle), 0f)
        assertEquals(1f, morph.dotTrim(2, last), 0f)
        assertEquals(0.5f, morph.dotTrim(1, last), 0.0001f)

        for (i in 1..3) {
            assertEquals(0f, morph.dotScale(i, morph.dotTrim(i, 1f), 0f), 0f)
        }
        // Position and radius use the same local trim; untouched dots stay still.
        assertEquals(
            40f,
            morph.dotCoordinate(70f, 10f, 100f, 130f, morph.dotTrim(3, first), 0f, 0f),
            0.0001f,
        )
        assertEquals(
            70f,
            morph.dotCoordinate(70f, 10f, 100f, 130f, morph.dotTrim(2, first), 0f, 0f),
            0f,
        )
        assertEquals(10f, morph.dotCoordinate(10f, 10f, 100f, 100f, 0f, 0f, 0f), 0f)
    }

    @Test
    fun collapseUsesTheExactReverseOfDotReveal() {
        for (index in 1..3) {
            for (step in 0..12) {
                val progress = step / 12f
                val shrinking =
                    morph.dotScale(index, morph.dotTrim(index, progress), 0f)
                val revealing =
                    morph.dotScale(index, 1f, 1f - progress)
                assertEquals(revealing, shrinking, 0.0001f)
            }
        }
    }

    @Test
    fun dualSimRowsUseTheSameNativeProgressPhases() {
        for (row in 0..1) {
            for (step in 0..100) {
                val fraction = step / 100f
                val phase = morph.handoffProgress(fraction * fraction)
                if (fraction <= 0.30f) {
                    assertEquals(0f, morph.moveProgress(phase), 0f)
                }
                if (fraction <= 0.57f) {
                    assertEquals(0f, morph.rowProgress(phase), 0f)
                }
                if (fraction <= 0.72f) {
                    assertEquals(0f, morph.barProgress(phase), 0f)
                }
            }
            assertEquals(1f, morph.barProgress(1f), 0f)
        }
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
        assertEquals(0f, morph.rowProgress(0.57f), 0f)
        assertEquals(1f, morph.rowProgress(0.72f), 0.0001f)
    }
}
