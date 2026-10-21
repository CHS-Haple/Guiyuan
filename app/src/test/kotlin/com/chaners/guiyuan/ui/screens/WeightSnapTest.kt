package com.chaners.guiyuan.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightSnapTest {
    @Test
    fun buildsAnchorsOnlyInsideVerifiedFontAxis() {
        assertEquals((150..700 step 50).map(Int::toFloat), weightSnapPoints(150..700))
        assertEquals((400..600 step 50).map(Int::toFloat), weightSnapPoints(375..625))
        assertEquals(listOf(650f, 700f), weightSnapPoints(630..710))
        assertEquals((200..1000 step 50).map(Int::toFloat), weightSnapPoints(200..1000))
        assertEquals(emptyList<Float>(), weightSnapPoints(301..349))
    }

    @Test
    fun magnetThresholdStaysWithinNativeContinuousSliderLimits() {
        assertEquals(0.02f, weightSnapThreshold(150..700), 0.00001f)
        assertTrue(weightSnapThreshold(200..1000) < 0.02f)
        assertTrue(weightSnapThreshold(200..1000) * 800f <= 12.5f)
        assertEquals(0.02f, weightSnapThreshold(630..710), 0.00001f)
    }
}
