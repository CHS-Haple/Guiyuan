package com.chaners.guiyuan.xposed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeVectorStrokeTest {
    @Test fun nativeEndpointsAndTransition() {
        assertEquals(120, NativeVectorStroke.duringTransition(120, 0f))
        assertEquals(110, NativeVectorStroke.duringTransition(120, 0.5f))
        assertEquals(100, NativeVectorStroke.duringTransition(120, 1f))
        assertEquals(60, NativeVectorStroke.duringTransition(60, 0f))
        assertEquals(100, NativeVectorStroke.duringTransition(60, 1f))
    }

    @Test fun vectorOffsetIsDensityIndependentAndMonotone() {
        val offsets = listOf(60, 80, 100, 120, 130).map {
            NativeVectorStroke.edgeOffset(it, 20f, 20f)
        }
        assertEquals(0f, offsets[2], 0f)
        assertTrue(offsets.zipWithNext().all { (a, b) -> a < b })
        // The original 20-unit Wi-Fi vector has rounded end curves smaller
        // than the unrestricted 0.4-unit inward offset at 60%.
        assertEquals(-0.24f, offsets[0], 0.0001f)
        assertEquals(-0.12f, offsets[1], 0.0001f)
        assertEquals(0.3f, offsets[4], 0.0001f)
        assertEquals(0.81f, NativeVectorStroke.edgeOffset(130, 54f, 56f), 0.0001f)
    }

    @Test fun inwardOffsetNeverConsumesTheSmallSourceEnds() {
        // Cap depends on vector viewport, not physical device pixels.
        assertEquals(-0.672f, NativeVectorStroke.edgeOffset(60, 56f, 56f), 0.0001f)
        assertEquals(-0.24f, NativeVectorStroke.edgeOffset(60, 20f, 20f), 0.0001f)
        assertEquals(0f, NativeVectorStroke.edgeOffset(100, 20f, 20f), 0f)
        val steps = (60..100 step 5).map { NativeVectorStroke.edgeOffset(it, 20f, 20f) }
        assertTrue(steps.zipWithNext().all { (a, b) -> a < b })
    }

    @Test fun outOfRangeWeightsAreBounded() {
        assertEquals(
            NativeVectorStroke.edgeOffset(60, 20f, 20f),
            NativeVectorStroke.edgeOffset(-100, 20f, 20f),
            0f,
        )
        assertEquals(
            NativeVectorStroke.edgeOffset(130, 20f, 20f),
            NativeVectorStroke.edgeOffset(500, 20f, 20f),
            0f,
        )
    }
}
