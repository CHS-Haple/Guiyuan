package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.SignalDotArrangement
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OuterGeometryTest {
    @Test
    fun splitBothSidesFollowAndFixedMatchAtDefaultThenRespondToOffset() {
        for (weight in listOf(0.6f, 1f, 1.5f, 2f)) {
            for (iconVisible in listOf(false, true)) {
                val g = OuterGeometry.resolve(weight, bothSidesBottomIconVisible = iconVisible)
                val fixedOpening = 90f
                val neutral = g.followingLowerHalfGap(true, SignalDotArrangement.BOTH_SIDES, 0f)
                val lower = g.followingLowerHalfGap(true, SignalDotArrangement.BOTH_SIDES, -5f)
                val higher = g.followingLowerHalfGap(true, SignalDotArrangement.BOTH_SIDES, 5f)
                assertEquals("neutral follow gap must match fixed half-ring", fixedOpening, neutral, 0.0001f)
                assertTrue("follow gap should close when dots rise", higher > neutral)
                assertTrue("follow gap should open when dots lower", lower < neutral)
                assertTrue("new lower info slot must also participate in avoidance",
                    g.followingLowerHalfGap(
                        dual = true, style = SignalDotArrangement.BOTH_SIDES, offsetY = 0f,
                        additionalContentHalfGap = 105f,
                    ) >= 105f)
            }
        }
    }

    @Test
    fun followingOpeningApproximatelyPreservesTheCurvedDefault() {
        val g = OuterGeometry.resolve(1f)
        assertEquals(60f, g.followingLowerHalfGap(false, SignalDotArrangement.CURVED, 0f), 0.1f)
        assertEquals(60f, g.followingLowerHalfGap(true, SignalDotArrangement.CURVED, 0f), 1.5f)
    }

    @Test
    fun followingOpeningTracksDotOffsetsAndWeights() {
        for (weight in listOf(0.6f, 1f, 1.5f, 2f)) {
            val g = OuterGeometry.resolve(weight)
            for (dual in listOf(false, true)) {
                for (style in SignalDotArrangement.entries) {
                    assertFollowingOpening(g, dual, style)
                }
            }
        }
    }

    private fun assertFollowingOpening(
        geometry: OuterGeometry.Resolved,
        dual: Boolean,
        style: SignalDotArrangement,
    ) {
        val lower = geometry.followingLowerHalfGap(dual, style, -5f)
        val neutral = geometry.followingLowerHalfGap(dual, style, 0f)
        val higher = geometry.followingLowerHalfGap(dual, style, 5f)
        assertTrue("drawable gap", neutral in 0f..90f)
        if (dual || style == SignalDotArrangement.HORIZONTAL) {
            // Curved paired rows can exchange the closest shoulder during motion.
            assertTrue("the opening follows the overall dot displacement", higher >= lower)
        }
    }

    @Test
    fun signalDotArrangementAppliesToSingleSimAndTransitionSourceGeometry() {
        for (step in 0..28) {
            val geometry = OuterGeometry.resolve(0.6f + step * 0.05f)
            val horizontalY = geometry.singleDotY(0, SignalDotArrangement.HORIZONTAL, 0f)
            for (index in 0..3) {
                val angle = geometry.bottomDotAngle(index)
                assertEquals(
                    cos(angle).toFloat() * OuterGeometry.MOBILE_ORBIT_RADIUS,
                    geometry.singleDotX(index, SignalDotArrangement.CURVED), 0.0001f,
                )
                assertEquals(
                    sin(angle).toFloat() * OuterGeometry.MOBILE_ORBIT_RADIUS,
                    geometry.singleDotY(index, SignalDotArrangement.CURVED, 0f), 0.0001f,
                )
                assertEquals(geometry.horizontalDotX(index),
                    geometry.singleDotX(index, SignalDotArrangement.HORIZONTAL), 0f)
                assertEquals("horizontal row at every weight", horizontalY,
                    geometry.singleDotY(index, SignalDotArrangement.HORIZONTAL, 0f), 0.0001f)
                assertEquals(horizontalY - 3f,
                    geometry.singleDotY(index, SignalDotArrangement.HORIZONTAL, 3f), 0.0001f)
            }
            val radius = geometry.mobileDotRadius
            assertTrue(58f + horizontalY + radius < 120f)
        }
    }

    @Test
    fun defaultOpticalBaselineRestoresPreferred825Ring() {
        val geometry =
            OuterGeometry.resolve(
                OuterGeometry.DEFAULT_WEIGHT_SCALE,
            )

        assertEquals(
            8.25f,
            geometry.ringStroke,
            0.0001f,
        )
        assertEquals(
            4.9f * 1.10f,
            geometry.mobileDotRadius,
            0.0001f,
        )
        assertEquals(
            3.2f * 1.10f,
            geometry.unavailableMarkStroke,
            0.0001f,
        )
        assertEquals(
            3.7f * 1.10f,
            geometry.unavailableMarkHalfExtent,
            0.0001f,
        )
    }


    @Test
    fun centeredHorizontalDotsRemainInsideRingAcrossFullWeightRange() {
        for (step in 0..28) {
            val weight = 0.6f + step * 0.05f
            val geometry = OuterGeometry.resolve(weight)
            for (dual in listOf(false, true)) {
                assertCenteredDotsInsideRing(geometry, weight, dual)
            }
        }
        assertEquals(1f, OuterGeometry.resolve(1f).centeredScale(true), 0f)
        assertTrue(OuterGeometry.resolve(2f).centeredScale(true) < 1f)
    }

    private fun assertCenteredDotsInsideRing(
        geometry: OuterGeometry.Resolved,
        weight: Float,
        dual: Boolean,
    ) {
        val scale = geometry.centeredScale(dual)
        val radius = (if (dual) geometry.dualMobileDotRadius else geometry.mobileDotRadius) * scale
        val rows = if (dual) listOf(-0.5f, 0.5f) else listOf(0f)
        val allowed = OuterGeometry.RING_RADIUS - geometry.ringStroke / 2f - 2f
        for (row in rows) {
            val y = row * geometry.dualRowGap * scale
            for (dot in 0..3) {
                val x = geometry.horizontalDotX(dot) * scale
                assertTrue(
                    "centered dot crosses ring weight=$weight dual=$dual",
                    sqrt(x * x + y * y) + radius <= allowed + 0.001f,
                )
            }
        }
        assertTrue("scale never enlarges dots", scale in 0f..1f)
    }

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
