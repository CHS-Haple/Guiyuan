package com.chaners.guiyuan.xposed

import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OuterGeometryDualRowTest {
    @Test
    fun dualRowsKeepTheirOpticalSpacingAcrossWeights() {
        assertEquals(56f, OuterGeometry.DUAL_DOT_SPAN, 0f)
        var lastDotRadius = 0f

        for (step in 0..28) {
            val scale = 0.6f + step * 0.05f
            val geometry = OuterGeometry.resolve(scale)
            val dotRadius = geometry.dualMobileDotRadius
            val gap = geometry.dualRowGap

            assertTrue("radius grows with weight $scale", dotRadius > lastDotRadius)
            lastDotRadius = dotRadius
            assertEquals("ring thickness follows same weight $scale", 5.0215f * scale, dotRadius, 0.0001f)
            val width = geometry.horizontalDotSpan
            assertTrue("column clearance $scale", width / 3f > 2f * dotRadius)
            assertTrue("two rows stay apart at $scale", gap > 2f * dotRadius)
            val naturalGap = 14.3f * scale
            val tightening = ((scale - 1f) * 3f).coerceIn(0f, 0.9f)
            assertEquals("optical pair gap at $scale", naturalGap - tightening, gap, 0.0002f)
            val middleX = width / 6f
            val middleY = sqrt(OuterGeometry.RING_RADIUS * OuterGeometry.RING_RADIUS - middleX * middleX)
            val expectedRise = maxOf(
                1.2f,
                58f + middleY + naturalGap / 2f + dotRadius + 2.1f - 120f,
            )
            assertEquals(
                "whole pair shifts together at $scale",
                expectedRise,
                geometry.dualOpticalRise,
                0.0002f,
            )
        }
    }

    @Test
    fun dualRowDotsStayInsideCanvasAndSeparatedAcrossWeights() {
        for (step in 0..28) {
            val scale = 0.6f + step * 0.05f
            val geometry = OuterGeometry.resolve(scale)
            val dotRadius = geometry.dualMobileDotRadius
            val gap = geometry.dualRowGap
            val width = geometry.horizontalDotSpan

            val dots = mutableListOf<Pair<Float, Float>>()
            for (index in 0..3) {
                val x = (index - 1.5f) * (width / 3f)
                val centerline = sqrt(
                    OuterGeometry.RING_RADIUS * OuterGeometry.RING_RADIUS - x * x,
                )
                val innerY = centerline - gap / 2f - geometry.dualOpticalRise
                val outerY = centerline + gap / 2f - geometry.dualOpticalRise

                assertEquals("pair gap at $scale", gap, outerY - innerY, 0.0001f)
                assertEquals(
                    "arc midpoint at $scale",
                    centerline - geometry.dualOpticalRise,
                    (innerY + outerY) / 2f,
                    0.0001f,
                )
                assertTrue("within canvas x at $scale",
                    60f + x - dotRadius > 0f && 60f + x + dotRadius < 120f)
                for (y in listOf(innerY, outerY)) {
                    assertTrue("within canvas y at $scale",
                        58f + y - dotRadius > 0f && 58f + y + dotRadius <= 117.9001f)
                    dots += x to y
                }
            }
            for (i in dots.indices) {
                for (j in i + 1 until dots.size) {
                    val dx = dots[i].first - dots[j].first
                    val dy = dots[i].second - dots[j].second
                    assertTrue("separate dots at $scale",
                        sqrt(dx * dx + dy * dy) > 2f * dotRadius)
                }
            }
        }
    }

    @Test
    fun dualRowsMatchAcceptedBoundarySizes() {
        val default = OuterGeometry.resolve(1f)
        assertEquals(4.565f * 1.10f, default.dualMobileDotRadius, 0.0001f)
        assertEquals(14.3f, default.dualRowGap, 0.0001f)
        assertEquals(14.3f * 0.7f, OuterGeometry.resolve(0.7f).dualRowGap, 0.0001f)
        assertEquals(17.69f, OuterGeometry.resolve(1.3f).dualRowGap, 0.0002f)
        assertEquals(56f, default.horizontalDotSpan, 0.0001f)
        assertTrue(default.dualOpticalRise >= 1.2f)

        val max = OuterGeometry.resolve(2f)
        assertEquals(10.043f, max.dualMobileDotRadius, 0.0001f)
        assertEquals(27.7f, max.dualRowGap, 0.0002f)
        assertEquals(62.258f, max.horizontalDotSpan, 0.0002f)
        assertEquals(7.53225f, OuterGeometry.resolve(1.5f).dualMobileDotRadius, 0.0001f)
        assertTrue(max.dualOpticalRise > default.dualOpticalRise)
    }
}
