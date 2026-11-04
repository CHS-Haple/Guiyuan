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
                    val lower = g.followingLowerHalfGap(dual, style, -5f)
                    val neutral = g.followingLowerHalfGap(dual, style, 0f)
                    val higher = g.followingLowerHalfGap(dual, style, 5f)
                    assertTrue("drawable gap", neutral in 0f..90f)
                    if (dual || style == SignalDotArrangement.HORIZONTAL) {
                        // Curved paired rows can exchange the closest shoulder during motion.
                        assertTrue("the opening follows the overall dot displacement", higher >= lower)
                    }
                }
            }
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
        assertEquals(1f, OuterGeometry.resolve(1f).centeredScale(true), 0f)
        assertTrue(OuterGeometry.resolve(2f).centeredScale(true) < 1f)
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

    @Test
    fun horizontalArrangementKeepsDotSizesAndSpacingWithFlatRows() {
        for (scale in TEST_SCALES) {
            val geometry = OuterGeometry.resolve(scale)
            for (row in 0..1) {
                val baseline = geometry.dualDotY(0, row, SignalDotArrangement.HORIZONTAL)
                for (index in 1..3) {
                    assertEquals(baseline, geometry.dualDotY(index, row, SignalDotArrangement.HORIZONTAL), 0.0001f)
                    assertEquals(
                        geometry.horizontalDotSpan / 3f,
                        geometry.horizontalDotX(index) - geometry.horizontalDotX(index - 1),
                        0.0001f,
                    )
                }
                assertTrue(58f + baseline + geometry.dualMobileDotRadius <= 117.9001f)
            }
            assertEquals(
                geometry.dualRowGap,
                geometry.dualDotY(0, 1, SignalDotArrangement.HORIZONTAL) -
                    geometry.dualDotY(0, 0, SignalDotArrangement.HORIZONTAL),
                0.0002f,
            )
        }
    }

    @Test
    fun bothDualStylesApplyTheSameSignedVerticalOffset() {
        for (scale in TEST_SCALES) {
            val geometry = OuterGeometry.resolve(scale)
            for (style in SignalDotArrangement.entries) {
                for (row in 0..1) {
                    for (index in 0..3) {
                        val original = geometry.dualDotY(index, row, style)
                        assertEquals(original - 8f, geometry.dualDotY(index, row, style, 8f), 0.0001f)
                        assertEquals(original + 10f, geometry.dualDotY(index, row, style, -10f), 0.0001f)
                    }
                }
            }
        }
    }

    @Test
    fun dualRowSourceCoordinatesRemainSharedAcrossWeights() {
        for (scale in TEST_SCALES) {
            val geometry = OuterGeometry.resolve(scale)
            val positions = (0 until 4).map(geometry::horizontalDotX)
            assertEquals(-positions[3], positions[0], 0.0001f)
            assertEquals(-positions[2], positions[1], 0.0001f)
            assertEquals(-geometry.horizontalDotSpan / 2f, positions.first(), 0.0001f)
            assertEquals(geometry.horizontalDotSpan / 2f, positions.last(), 0.0001f)

            for (index in 0 until 4) {
                val x = positions[index]
                val baseline =
                    sqrt(OuterGeometry.RING_RADIUS * OuterGeometry.RING_RADIUS - x * x) -
                        geometry.dualOpticalRise
                assertEquals(
                    baseline - geometry.dualRowGap / 2f,
                    geometry.dualDotY(index, 0),
                    0.0001f,
                )
                assertEquals(
                    baseline + geometry.dualRowGap / 2f,
                    geometry.dualDotY(index, 1),
                    0.0001f,
                )
            }
            assertTrue(
                geometry.dualDotY(0, 0) < geometry.dualDotY(1, 0) &&
                    geometry.dualDotY(1, 1) > geometry.dualDotY(0, 1),
            )
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
    fun leftRightDotsRemainMirroredAndClearAtSupportedRingWeights() {
        for (step in 0..12) {
            val weight = 0.7f + 0.05f * step
            val g = OuterGeometry.resolve(weight)
            val dotRadius = g.leftRightMobileDotRadius
            for (index in 0 until 4) {
                val leftX = g.dualDotX(index, 0, SignalDotArrangement.LEFT_RIGHT)
                val rightX = g.dualDotX(index, 1, SignalDotArrangement.LEFT_RIGHT)
                val leftY = g.dualDotY(index, 0, SignalDotArrangement.LEFT_RIGHT)
                val rightY = g.dualDotY(index, 1, SignalDotArrangement.LEFT_RIGHT)
                assertEquals("mirrored x", -leftX, rightX, 0.0001f)
                assertEquals("mirrored y", leftY, rightY, 0.0001f)
                assertTrue("left group remains left", leftX < 0f)
                assertTrue("right group remains right", rightX > 0f)
                assertTrue("lower group remains below center", leftY > 0f)
                if (index > 0) {
                    for (row in 0..1) {
                        val dx = g.dualDotX(index, row, SignalDotArrangement.LEFT_RIGHT) -
                            g.dualDotX(index - 1, row, SignalDotArrangement.LEFT_RIGHT)
                        val dy = g.dualDotY(index, row, SignalDotArrangement.LEFT_RIGHT) -
                            g.dualDotY(index - 1, row, SignalDotArrangement.LEFT_RIGHT)
                        assertTrue("dots must not overlap at weight $weight",
                            sqrt(dx * dx + dy * dy) >= 2f * dotRadius + 0.9f)
                    }
                }
            }
            for (row in 0..1) {
                val x = g.dualDotX(3, row, SignalDotArrangement.LEFT_RIGHT)
                val y = g.dualDotY(3, row, SignalDotArrangement.LEFT_RIGHT)
                val endpointX = if (row == 0) -OuterGeometry.RING_RADIUS
                    else OuterGeometry.RING_RADIUS
                assertTrue("upper ring cap must clear split dots at $weight",
                    sqrt((x - endpointX) * (x - endpointX) + y * y) >=
                        dotRadius + g.ringStroke / 2f + 1.9f)
            }
            assertEquals(90f,
                g.followingLowerHalfGap(true, SignalDotArrangement.LEFT_RIGHT, 0f), 0f)
        }
    }

    @Test
    fun leftRightDotsKeepWiderSpacingAndClearTheSharedBottomSlot() {
        for (step in 0..12) {
            val g = OuterGeometry.resolve(0.7f + step * 0.05f)
            val first = Math.toDegrees(
                kotlin.math.atan2(
                    g.dualDotY(0, 0, SignalDotArrangement.LEFT_RIGHT).toDouble(),
                    g.dualDotX(0, 0, SignalDotArrangement.LEFT_RIGHT).toDouble(),
                ),
            )
            val last = Math.toDegrees(
                kotlin.math.atan2(
                    g.dualDotY(3, 0, SignalDotArrangement.LEFT_RIGHT).toDouble(),
                    g.dualDotX(3, 0, SignalDotArrangement.LEFT_RIGHT).toDouble(),
                ),
            )
            assertTrue("each split group uses a 15 degree baseline", last - first >= 44.9)
            for (row in 0..1) {
                val nearestX = kotlin.math.abs(g.dualDotX(0, row, SignalDotArrangement.LEFT_RIGHT))
                val slotHalfWidth = 19f * 1.2f / 2f
                assertTrue("bottom slot must clear both SIM dot groups",
                    nearestX - slotHalfWidth - g.leftRightMobileDotRadius > 0.5f)
            }
        }
    }

    @Test
    fun disabledBottomSlotBalancesBothOpticalGapsWithoutShorteningTheHalfRing() {
        for (step in 0..12) {
            val g = OuterGeometry.resolve(0.7f + step * 0.05f, leftRightBottomSlotEnabled = false)
            val innerX = g.dualDotX(0, 0, SignalDotArrangement.LEFT_RIGHT)
            val rightInnerX = g.dualDotX(0, 1, SignalDotArrangement.LEFT_RIGHT)
            val ringSideX = -OuterGeometry.RING_RADIUS
            val outerX = g.dualDotX(3, 0, SignalDotArrangement.LEFT_RIGHT)
            val outerY = g.dualDotY(3, 0, SignalDotArrangement.LEFT_RIGHT)
            val centerClearance = rightInnerX - innerX - 2f * g.leftRightMobileDotRadius
            val ringClearance = sqrt(
                (outerX - ringSideX) * (outerX - ringSideX) + outerY * outerY,
            ) - g.leftRightMobileDotRadius - g.ringStroke / 2f
            assertTrue("split groups must remain optically separated", centerClearance > 1f)
            assertEquals("center and ring-side optical spacing", ringClearance, centerClearance, 0.1f)
            assertEquals(
                "the complete lower semicircle remains available to the dots",
                90f, g.followingLowerHalfGap(true, SignalDotArrangement.LEFT_RIGHT, 0f), 0f,
            )
            assertEquals(
                "the upper ring still spans half a circle",
                180f,
                com.chaners.guiyuan.xposed.battery.BatteryTopArcPolicy
                    .drawableRingArcs(topHalfDegrees = 0f, bottomHalfDegrees = 90f)
                    .sumOf { it.sweepDegrees.toDouble() }.toFloat(),
                0f,
            )
            for (index in 0..3) {
                assertEquals(
                    "mirror x",
                    -g.dualDotX(index, 0, SignalDotArrangement.LEFT_RIGHT),
                    g.dualDotX(index, 1, SignalDotArrangement.LEFT_RIGHT), 0.0001f,
                )
            }
        }
    }

    @Test
    fun leftRightDotsClearAtEverySupportedRingWeight() {
        for (step in 0..28) {
            val weight = 0.6f + step * 0.05f
            for (slotEnabled in listOf(true, false)) {
                val geom = OuterGeometry.resolve(weight, leftRightBottomSlotEnabled = slotEnabled)
                val radius = geom.leftRightMobileDotRadius
                assertTrue("split radius cannot exceed ordinary dual dot radius",
                    radius > 0f && radius <= geom.dualMobileDotRadius)
                for (i in 1..3) {
                    val dx = geom.dualDotX(i, 0, SignalDotArrangement.LEFT_RIGHT) -
                        geom.dualDotX(i - 1, 0, SignalDotArrangement.LEFT_RIGHT)
                    val dy = geom.dualDotY(i, 0, SignalDotArrangement.LEFT_RIGHT) -
                        geom.dualDotY(i - 1, 0, SignalDotArrangement.LEFT_RIGHT)
                    assertTrue("within-group circles must not overlap at $weight",
                        sqrt(dx * dx + dy * dy) > 2f * radius)
                }
                val innerGap = geom.dualDotX(0, 1, SignalDotArrangement.LEFT_RIGHT) -
                    geom.dualDotX(0, 0, SignalDotArrangement.LEFT_RIGHT) - 2f * radius
                assertTrue("SIM groups must not overlap at $weight", innerGap > 0.5f)
            }
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
