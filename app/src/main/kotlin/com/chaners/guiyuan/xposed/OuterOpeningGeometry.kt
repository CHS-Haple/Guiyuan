package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.OuterGeometry.DOT_COUNT
import com.chaners.guiyuan.xposed.OuterGeometry.RING_RADIUS
import com.chaners.guiyuan.settings.SignalDotArrangement
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt

// Collision-based ring openings are derived from immutable rendered dot geometry.
// The coordinate getters remain on OuterGeometry.Resolved; this pure calculation owns no state.
// Same circle-intersection span for curved and split signal-dot openings.
private fun ringIntersectionSpreadDegrees(
    distance: Double,
    ringRadius: Double,
    reach: Double,
): Double =
    if (reach >= distance + ringRadius) 180.0 else Math.toDegrees(
        acos(((distance * distance + ringRadius * ringRadius - reach * reach) /
            (2.0 * distance * ringRadius)).coerceIn(-1.0, 1.0)),
    )

// Balance visible ring-to-dot clearance against adjacent dot spacing.
internal fun OuterGeometry.Resolved.followingLowerHalfGap(
    dual: Boolean,
    style: SignalDotArrangement,
    offsetY: Float,
    additionalContentHalfGap: Float = 0f,
): Float {
    if (dual && style == SignalDotArrangement.BOTH_SIDES) {
        // The default layout reserves exactly the same lower half as
        // fixed mode. Only the *change* in actual dot occupancy moves
        // the opening when users move the entire lower assembly.
        val baseline = requiredBothSidesHalfGap(style, 0f)
        val current = requiredBothSidesHalfGap(style, offsetY)
        return max(
            (90.0 + current - baseline).toFloat().coerceIn(72f, 110f),
            additionalContentHalfGap.coerceIn(0f, 110f),
        )
    }
    val radius = if (dual) dualMobileDotRadius else mobileDotRadius
    val dotGap = if (!dual && style == SignalDotArrangement.CURVED) balancedEdgeGap
        else (horizontalDotSpan / (DOT_COUNT - 1) - 2f * radius).coerceAtLeast(0f)
    val reach = (radius + ringStroke / 2f + dotGap).toDouble()
    val ringRadius = RING_RADIUS.toDouble()
    var halfGap = 0.0
    for (row in 0 until (if (dual) 2 else 1)) {
        for (index in 0 until DOT_COUNT) {
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


private fun OuterGeometry.Resolved.requiredBothSidesHalfGap(
    style: SignalDotArrangement,
    atOffset: Float,
): Double {
    val clearance = (bothSidesMobileDotRadius + ringStroke / 2f + 2f).toDouble()
    val radius = RING_RADIUS.toDouble()
    var half = 0.0
    for (row in 0..1) for (index in 0 until DOT_COUNT) {
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
