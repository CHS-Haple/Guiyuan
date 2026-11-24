package com.chaners.guiyuan.xposed

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal object RingGapPolicy {
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
