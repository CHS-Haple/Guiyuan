package com.chaners.guiyuan.xposed

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OuterGeometryTest {
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
    fun dualRowsAreConcentricWithTheBatteryRingAndStayInsideCanvas() {
        assertEquals(
            OuterGeometry.RING_RADIUS,
            (OuterGeometry.DUAL_INNER_ORBIT_RADIUS +
                OuterGeometry.DUAL_OUTER_ORBIT_RADIUS) / 2f,
            0f,
        )
        for (scale in TEST_SCALES) {
            val geometry = OuterGeometry.resolve(scale)
            val dotRadius = geometry.dualMobileDotRadius
            assertEquals(minOf(4f * scale, 5f), dotRadius, 0.0001f)
            assertTrue("distinct rows scale=$scale", 12f > dotRadius * 2f)
            for (index in 0..3) {
                val angle = geometry.bottomDotAngle(index)
                val x = cos(angle).toFloat() * OuterGeometry.MOBILE_ORBIT_RADIUS
                val innerY = sqrt(
                    OuterGeometry.DUAL_INNER_ORBIT_RADIUS *
                        OuterGeometry.DUAL_INNER_ORBIT_RADIUS - x * x,
                )
                val outerY = sqrt(
                    OuterGeometry.DUAL_OUTER_ORBIT_RADIUS *
                        OuterGeometry.DUAL_OUTER_ORBIT_RADIUS - x * x,
                )
                val ringY = sqrt(
                    OuterGeometry.RING_RADIUS * OuterGeometry.RING_RADIUS - x * x,
                )
                assertTrue("SIM1 above SIM2 scale=$scale", innerY < outerY)
                assertTrue("rows do not overlap scale=$scale",
                    outerY - innerY > 2f * dotRadius)
                assertEquals("row midpoint scale=$scale", ringY, (innerY + outerY) / 2f, 0.5f)
                assertTrue("x scale=$scale", 60f + x - dotRadius > 0f && 60f + x + dotRadius < 120f)
                for (y in listOf(innerY, outerY)) {
                    assertTrue("y scale=$scale", 58f + y - dotRadius > 0f &&
                        58f + y + dotRadius < 120f)
                }
            }
        }
    }

    @Test
    fun outerWeightScalesRingDotsAndUnavailableMarkAsOneVisualFamily() {
        val base =
            OuterGeometry.resolve(
                OuterGeometry.DEFAULT_WEIGHT_SCALE,
            )
        val heavier = OuterGeometry.resolve(1.20f)

        assertEquals(base.ringStroke * 1.20f, heavier.ringStroke, 0.0001f)
        assertEquals(base.mobileDotRadius * 1.20f, heavier.mobileDotRadius, 0.0001f)
        assertEquals(
            base.unavailableMarkStroke * 1.20f,
            heavier.unavailableMarkStroke,
            0.0001f,
        )
        assertEquals(
            base.unavailableMarkHalfExtent * 1.20f,
            heavier.unavailableMarkHalfExtent,
            0.0001f,
        )
    }

    @Test
    fun ringAndDotsScaleProportionally() {
        for (scale in TEST_SCALES) {
            val geometry = OuterGeometry.resolve(scale)

            assertEquals(
                OuterGeometry.BASE_RING_STROKE * scale,
                geometry.ringStroke,
                0.0001f,
            )
            assertEquals(
                OuterGeometry.BASE_MOBILE_DOT_RADIUS * scale,
                geometry.mobileDotRadius,
                0.0001f,
            )
        }
    }

    @Test
    fun unavailableMarkScalesWithOuterWeightWithoutChangingItsIndependentProportions() {
        for (scale in TEST_SCALES) {
            val geometry = OuterGeometry.resolve(scale)

            assertEquals(
                OuterGeometry.BASE_UNAVAILABLE_MARK_STROKE * scale,
                geometry.unavailableMarkStroke,
                0.0001f,
            )
            assertEquals(
                OuterGeometry.BASE_UNAVAILABLE_MARK_HALF_EXTENT * scale,
                geometry.unavailableMarkHalfExtent,
                0.0001f,
            )
        }
    }

    @Test
    fun fourDotsRemainMirrorSymmetricAcrossSupportedScales() {
        for (scale in TEST_SCALES) {
            val geometry = OuterGeometry.resolve(scale)
            val angles =
                (0 until 4)
                    .map(geometry::bottomDotAngle)
                    .map(Math::toDegrees)

            assertEquals(
                "outer symmetry scale=$scale",
                180.0,
                angles[0] + angles[3],
                0.0001,
            )
            assertEquals(
                "inner symmetry scale=$scale",
                180.0,
                angles[1] + angles[2],
                0.0001,
            )
        }
    }

    @Test
    fun fiveVisualEdgeGapsStayBalancedAcrossSupportedScales() {
        for (scale in TEST_SCALES) {
            val geometry = OuterGeometry.resolve(scale)
            val ascendingAngles =
                (3 downTo 0)
                    .map(geometry::bottomDotAngle)

            val leftRingGap =
                ringToDotGap(
                    ringEndAngle = Math.toRadians(30.0),
                    dotAngle = ascendingAngles.first(),
                    geometry = geometry,
                )
            val rightRingGap =
                ringToDotGap(
                    ringEndAngle = Math.toRadians(150.0),
                    dotAngle = ascendingAngles.last(),
                    geometry = geometry,
                )
            val dotGaps =
                ascendingAngles
                    .zipWithNext()
                    .map { (left, right) ->
                        dotToDotGap(
                            firstAngle = left,
                            secondAngle = right,
                            geometry = geometry,
                        )
                    }

            val gaps = listOf(leftRingGap) + dotGaps + rightRingGap
            val spread = gaps.maxOrNull()!! - gaps.minOrNull()!!

            assertTrue(
                "edge-gap spread=$spread scale=$scale gaps=$gaps",
                spread < 0.01f,
            )
            assertTrue(
                "edge gaps must stay positive scale=$scale gaps=$gaps",
                gaps.all { it > 0f },
            )
        }
    }

    @Test
    fun invalidOrOutOfRangeWeightScaleFallsBackOrClampsSafely() {
        assertEquals(
            OuterGeometry.DEFAULT_WEIGHT_SCALE,
            OuterGeometry.normalizeWeightScale(Float.NaN),
            0f,
        )
        assertEquals(
            OuterGeometry.MIN_WEIGHT_SCALE,
            OuterGeometry.normalizeWeightScale(0f),
            0f,
        )
        assertEquals(
            OuterGeometry.MAX_WEIGHT_SCALE,
            OuterGeometry.normalizeWeightScale(5f),
            0f,
        )
    }

    private fun ringToDotGap(
        ringEndAngle: Double,
        dotAngle: Double,
        geometry: OuterGeometry.Resolved,
    ): Float {
        val ringX =
            cos(ringEndAngle).toFloat() * OuterGeometry.RING_RADIUS
        val ringY =
            sin(ringEndAngle).toFloat() * OuterGeometry.RING_RADIUS
        val dotX =
            cos(dotAngle).toFloat() * OuterGeometry.MOBILE_ORBIT_RADIUS
        val dotY =
            sin(dotAngle).toFloat() * OuterGeometry.MOBILE_ORBIT_RADIUS
        val centerDistance =
            sqrt(
                (dotX - ringX) * (dotX - ringX) +
                    (dotY - ringY) * (dotY - ringY),
            )
        return centerDistance -
            geometry.ringStroke / 2f -
            geometry.mobileDotRadius
    }

    private fun dotToDotGap(
        firstAngle: Double,
        secondAngle: Double,
        geometry: OuterGeometry.Resolved,
    ): Float {
        val firstX =
            cos(firstAngle).toFloat() * OuterGeometry.MOBILE_ORBIT_RADIUS
        val firstY =
            sin(firstAngle).toFloat() * OuterGeometry.MOBILE_ORBIT_RADIUS
        val secondX =
            cos(secondAngle).toFloat() * OuterGeometry.MOBILE_ORBIT_RADIUS
        val secondY =
            sin(secondAngle).toFloat() * OuterGeometry.MOBILE_ORBIT_RADIUS
        val centerDistance =
            sqrt(
                (secondX - firstX) * (secondX - firstX) +
                    (secondY - firstY) * (secondY - firstY),
            )
        return centerDistance - 2f * geometry.mobileDotRadius
    }

    private companion object {
        val TEST_SCALES =
            listOf(
                0.60f,
                1.00f,
                OuterGeometry.DEFAULT_WEIGHT_SCALE,
                1.50f,
                2.00f,
            )
    }
}
