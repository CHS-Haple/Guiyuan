package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.SignalDotArrangement
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OuterGeometryBothSidesTest {
    private fun assertBothSidesNeighborClearance(
        geometry: OuterGeometry.Resolved,
        index: Int,
        weight: Float,
        dotRadius: Float,
    ) {
        for (row in 0..1) {
            val dx = geometry.dualDotX(index, row, SignalDotArrangement.BOTH_SIDES) -
                geometry.dualDotX(index - 1, row, SignalDotArrangement.BOTH_SIDES)
            val dy = geometry.dualDotY(index, row, SignalDotArrangement.BOTH_SIDES) -
                geometry.dualDotY(index - 1, row, SignalDotArrangement.BOTH_SIDES)
            assertTrue("dots must not overlap at weight $weight",
                sqrt(dx * dx + dy * dy) >= 2f * dotRadius + 0.9f)
        }
    }

    @Test
    fun bothSidesDotsRemainMirroredAndClearAtSupportedRingWeights() {
        for (step in 0..12) {
            val weight = 0.7f + 0.05f * step
            val g = OuterGeometry.resolve(weight)
            val dotRadius = g.bothSidesMobileDotRadius
            for (index in 0 until 4) {
                val leftX = g.dualDotX(index, 0, SignalDotArrangement.BOTH_SIDES)
                val rightX = g.dualDotX(index, 1, SignalDotArrangement.BOTH_SIDES)
                val leftY = g.dualDotY(index, 0, SignalDotArrangement.BOTH_SIDES)
                val rightY = g.dualDotY(index, 1, SignalDotArrangement.BOTH_SIDES)
                assertEquals("mirrored x", -leftX, rightX, 0.0001f)
                assertEquals("mirrored y", leftY, rightY, 0.0001f)
                assertTrue("left group remains left", leftX < 0f)
                assertTrue("right group remains right", rightX > 0f)
                assertTrue("lower group remains below center", leftY > 0f)
                if (index > 0) assertBothSidesNeighborClearance(g, index, weight, dotRadius)
            }
            for (row in 0..1) {
                val x = g.dualDotX(3, row, SignalDotArrangement.BOTH_SIDES)
                val y = g.dualDotY(3, row, SignalDotArrangement.BOTH_SIDES)
                val endpointX = if (row == 0) -OuterGeometry.RING_RADIUS
                    else OuterGeometry.RING_RADIUS
                assertTrue("upper ring cap must clear split dots at $weight",
                    sqrt((x - endpointX) * (x - endpointX) + y * y) >=
                        dotRadius + g.ringStroke / 2f + 1.9f)
            }
            assertEquals(90f,
                g.followingLowerHalfGap(true, SignalDotArrangement.BOTH_SIDES, 0f), 0f)
        }
    }

    @Test
    fun bothSidesDotsKeepWiderSpacingAndClearTheSharedBottomSlot() {
        for (step in 0..12) {
            val g = OuterGeometry.resolve(0.7f + step * 0.05f)
            val first = Math.toDegrees(
                kotlin.math.atan2(
                    g.dualDotY(0, 0, SignalDotArrangement.BOTH_SIDES).toDouble(),
                    g.dualDotX(0, 0, SignalDotArrangement.BOTH_SIDES).toDouble(),
                ),
            )
            val last = Math.toDegrees(
                kotlin.math.atan2(
                    g.dualDotY(3, 0, SignalDotArrangement.BOTH_SIDES).toDouble(),
                    g.dualDotX(3, 0, SignalDotArrangement.BOTH_SIDES).toDouble(),
                ),
            )
            assertTrue("bottom icon requires a 16 degree minimum", last - first >= 47.9)
            for (row in 0..1) {
                val nearestX = kotlin.math.abs(g.dualDotX(0, row, SignalDotArrangement.BOTH_SIDES))
                val slotHalfWidth = 19f * 1.2f / 2f
                assertTrue("bottom slot must clear both SIM dot groups",
                    nearestX - slotHalfWidth - g.bothSidesMobileDotRadius > 0.5f)
            }
        }
    }

    @Test
    fun emptyBottomUsesWiderGroupsAtAllSupportedWeights() {
        for (weight in listOf(0.6f, 1f, 1.5f, 2f)) {
            val icon = OuterGeometry.resolve(weight, bothSidesBottomIconVisible = true)
            val empty = OuterGeometry.resolve(weight, bothSidesBottomIconVisible = false)
            assertTrue("no-icon layout needs wider dot spacing",
                empty.bothSidesStepDegrees > icon.bothSidesStepDegrees)
            assertEquals(icon.ringStroke, empty.ringStroke, 0f)
        }
    }

    @Test
    fun disabledBottomSlotBalancesBothOpticalGapsWithoutShorteningTheHalfRing() {
        for (step in 0..12) {
            val g = OuterGeometry.resolve(0.7f + step * 0.05f, bothSidesBottomIconVisible = false)
            val innerX = g.dualDotX(0, 0, SignalDotArrangement.BOTH_SIDES)
            val rightInnerX = g.dualDotX(0, 1, SignalDotArrangement.BOTH_SIDES)
            val ringSideX = -OuterGeometry.RING_RADIUS
            val outerX = g.dualDotX(3, 0, SignalDotArrangement.BOTH_SIDES)
            val outerY = g.dualDotY(3, 0, SignalDotArrangement.BOTH_SIDES)
            val centerClearance = rightInnerX - innerX - 2f * g.bothSidesMobileDotRadius
            val ringClearance = sqrt(
                (outerX - ringSideX) * (outerX - ringSideX) + outerY * outerY,
            ) - g.bothSidesMobileDotRadius - g.ringStroke / 2f
            assertTrue("split groups must remain optically separated", centerClearance > 1f)
            assertEquals("center and ring-side optical spacing", ringClearance, centerClearance, 0.1f)
            assertEquals(
                "the complete lower semicircle remains available to the dots",
                90f, g.followingLowerHalfGap(true, SignalDotArrangement.BOTH_SIDES, 0f), 0f,
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
                    -g.dualDotX(index, 0, SignalDotArrangement.BOTH_SIDES),
                    g.dualDotX(index, 1, SignalDotArrangement.BOTH_SIDES), 0.0001f,
                )
            }
        }
    }

    @Test
    fun bothSidesDotsClearAtEverySupportedRingWeight() {
        for (step in 0..28) {
            val weight = 0.6f + step * 0.05f
            for (slotEnabled in listOf(true, false)) {
                val geom = OuterGeometry.resolve(weight, bothSidesBottomIconVisible = slotEnabled)
                val radius = geom.bothSidesMobileDotRadius
                assertTrue("split radius cannot exceed ordinary dual dot radius",
                    radius > 0f && radius <= geom.dualMobileDotRadius)
                for (i in 1..3) {
                    val dx = geom.dualDotX(i, 0, SignalDotArrangement.BOTH_SIDES) -
                        geom.dualDotX(i - 1, 0, SignalDotArrangement.BOTH_SIDES)
                    val dy = geom.dualDotY(i, 0, SignalDotArrangement.BOTH_SIDES) -
                        geom.dualDotY(i - 1, 0, SignalDotArrangement.BOTH_SIDES)
                    assertTrue("within-group circles must not overlap at $weight",
                        sqrt(dx * dx + dy * dy) > 2f * radius)
                }
                val innerGap = geom.dualDotX(0, 1, SignalDotArrangement.BOTH_SIDES) -
                    geom.dualDotX(0, 0, SignalDotArrangement.BOTH_SIDES) - 2f * radius
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
