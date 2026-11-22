package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.SignalDotArrangement
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal object RingGapPolicy {
    // Same circle-intersection span for curved and split signal-dot openings.    private fun ringIntersectionSpreadDegrees(        distance: Double,        ringRadius: Double,        reach: Double,    ): Double =        if (reach >= distance + ringRadius) 180.0 else Math.toDegrees(            acos(((distance * distance + ringRadius * ringRadius - reach * reach) /                (2.0 * distance * ringRadius)).coerceIn(-1.0, 1.0)),        )
    internal fun followingLowerHalfGap(
        geometry: OuterGeometry.Resolved,
            dual: Boolean,
            style: SignalDotArrangement,
            offsetY: Float,
            additionalContentHalfGap: Float,
        ): Float {
        with(geometry) {
            if (dual && style == SignalDotArrangement.BOTH_SIDES) {
                // The default layout reserves exactly the same lower half as
                // fixed mode. Only the *change* in actual dot occupancy moves
                // the opening when users move the entire lower assembly.
                val baseline = requiredBothSidesHalfGap(geometry, style, 0f)
                val current = requiredBothSidesHalfGap(geometry, style, offsetY)
                return max(
                    (90.0 + current - baseline).toFloat().coerceIn(72f, 110f),
                    additionalContentHalfGap.coerceIn(0f, 110f),
                )
            }
            val radius = if (dual) dualMobileDotRadius else mobileDotRadius
            val dotGap = if (!dual && style == SignalDotArrangement.CURVED) balancedEdgeGap
                else (horizontalDotSpan / (OuterGeometry.DOT_COUNT - 1) - 2f * radius).coerceAtLeast(0f)
            val reach = (radius + ringStroke / 2f + dotGap).toDouble()
            val ringRadius = OuterGeometry.RING_RADIUS.toDouble()
            var halfGap = 0.0
            for (row in 0 until (if (dual) 2 else 1)) {
                for (index in 0 until OuterGeometry.DOT_COUNT) {
                    val x = (if (dual) horizontalDotX(index) else singleDotX(index, style)).toDouble()
                    val y = (if (dual) dualDotY(index, row, style, offsetY)
                        else singleDotY(index, style, offsetY)).toDouble()
                    val distance = sqrt(x * x + y * y)
                    if (reach <= kotlin.math.abs(distance - ringRadius)) continue
                    val spread = ringIntersectionSpreadDegrees(distance, ringRadius, reach)
                    halfGap = max(halfGap, kotlin.math.abs(Math.toDegrees(atan2(y, x)) - 90.0) + spread)
                }
            }
            return halfGap.toFloat().coerceIn(0f, 90f)
        }
    }

    private fun requiredBothSidesHalfGap(
        geometry: OuterGeometry.Resolved,
            style: SignalDotArrangement,
            atOffset: Float,
        ): Double {
        with(geometry) {
            val clearance = (bothSidesMobileDotRadius + ringStroke / 2f + 2f).toDouble()
            val radius = OuterGeometry.RING_RADIUS.toDouble()
            var half = 0.0
            for (row in 0..1) for (index in 0 until OuterGeometry.DOT_COUNT) {
                val x = dualDotX(index, row, style).toDouble()
                val y = dualDotY(index, row, style, atOffset).toDouble()
                val d = sqrt(x * x + y * y)
                if (d <= 0.00001 || kotlin.math.abs(d - radius) >= clearance) continue
                val spread = ringIntersectionSpreadDegrees(d, radius, clearance)
                val angle = Math.toDegrees(atan2(y, x))
                half = max(half, kotlin.math.abs(angle - 90.0) + spread)
            }
            return half
        }
    }

    private const val LOWER_OPENING_LEFT_DEGREES = 30.0
    internal const val LOWER_OPENING_CENTER_DEGREES = 90.0
    private const val SOLVER_ITERATIONS = 32

    // Match the opening ring-to-dot gap with the nearest dot-to-dot gap.
    internal fun solveOuterDotStep(ringStroke: Float, dotRadius: Float): Float {
        var lowerStep = 1f
        var upperStep = 39.5f
        repeat(SOLVER_ITERATIONS) {
            val candidate = (lowerStep + upperStep) / 2f
            val difference =
                ringToDotEdgeGap(
                    stepDegrees = candidate,
                    ringStroke = ringStroke,
                    dotRadius = dotRadius,
                ) -
                    dotToDotEdgeGap(
                        stepDegrees = candidate,
                        dotRadius = dotRadius,
                    )
            if (difference > 0f) {
                lowerStep = candidate
            } else {
                upperStep = candidate
            }
        }

        return (lowerStep + upperStep) / 2f
    }

    private fun ringToDotEdgeGap(
        stepDegrees: Float,
        ringStroke: Float,
        dotRadius: Float,
    ): Float {
        val dotAngle =
            Math.toRadians(
                LOWER_OPENING_CENTER_DEGREES -
                    1.5 * stepDegrees,
            )
        val ringAngle = Math.toRadians(LOWER_OPENING_LEFT_DEGREES)
        val ringX = cos(ringAngle).toFloat() * OuterGeometry.RING_RADIUS
        val ringY = sin(ringAngle).toFloat() * OuterGeometry.RING_RADIUS
        val dotX = cos(dotAngle).toFloat() * OuterGeometry.MOBILE_ORBIT_RADIUS
        val dotY = sin(dotAngle).toFloat() * OuterGeometry.MOBILE_ORBIT_RADIUS
        val dx = dotX - ringX
        val dy = dotY - ringY
        return sqrt(dx * dx + dy * dy) -
            ringStroke / 2f -
            dotRadius
    }

    internal fun dotToDotEdgeGap(
        stepDegrees: Float,
        dotRadius: Float,
    ): Float {
        val stepRadians = Math.toRadians(stepDegrees.toDouble())
        val centerDistance =
            2f *
                OuterGeometry.MOBILE_ORBIT_RADIUS *
                sin(stepRadians / 2.0).toFloat()
        return centerDistance - 2f * dotRadius
    }
}
