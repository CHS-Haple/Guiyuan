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
}
