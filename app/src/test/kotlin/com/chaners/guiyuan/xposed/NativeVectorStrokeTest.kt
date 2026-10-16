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
        assertEquals(-0.4f, offsets[0], 0.0001f)
        assertEquals(0.3f, offsets[4], 0.0001f)
        assertEquals(0.81f, NativeVectorStroke.edgeOffset(130, 54f, 56f), 0.0001f)
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
