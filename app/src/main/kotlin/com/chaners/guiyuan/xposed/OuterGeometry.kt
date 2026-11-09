package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.SignalDotArrangement
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

internal object OuterGeometry {
    private const val PREVIOUS_DEFAULT_WEIGHT_SCALE = 1.10f

    const val RING_RADIUS = 50f
    const val BASE_RING_STROKE = 8.25f
    const val MOBILE_ORBIT_RADIUS = 51f
    const val DUAL_DOT_SPAN = 56f
    private const val DUAL_BASE_ROW_GAP = 14.3f
    private const val DUAL_BASE_OPTICAL_RISE = 1.2f
    private const val DUAL_BASE_DOT_RADIUS = 5.0215f
    private const val DUAL_BOTTOM_MARGIN = 2.1f
    private const val DUAL_MAX_ROW_TIGHTENING = 0.9f
    private const val DUAL_ROW_TIGHTENING_RATE = 3f
    private const val CANVAS_BOTTOM = 120f
    private const val MOBILE_CENTER_Y = 58f
    const val BASE_MOBILE_DOT_RADIUS = 4.9f * PREVIOUS_DEFAULT_WEIGHT_SCALE
    const val BASE_UNAVAILABLE_MARK_STROKE = 3.2f * PREVIOUS_DEFAULT_WEIGHT_SCALE
    const val BASE_UNAVAILABLE_MARK_HALF_EXTENT = 3.7f * PREVIOUS_DEFAULT_WEIGHT_SCALE
    const val DEFAULT_WEIGHT_SCALE = 1.00f
    const val MIN_WEIGHT_SCALE = 0.60f
    const val MAX_WEIGHT_SCALE = 2.00f

    private const val LOWER_OPENING_LEFT_DEGREES = 30.0
    private const val LOWER_OPENING_CENTER_DEGREES = 90.0
    private const val LOWER_OPENING_RIGHT_DEGREES = 150.0
    private const val DOT_COUNT = 4
    private const val SOLVER_ITERATIONS = 32

    data class Resolved(
        val weightScale: Float,
        val ringStroke: Float,
        val mobileDotRadius: Float,
        val dualMobileDotRadius: Float,
        val bothSidesMobileDotRadius: Float,
        val horizontalDotSpan: Float,
        val dualRowGap: Float,
        val dualOpticalRise: Float,
        val unavailableMarkStroke: Float,
        val unavailableMarkHalfExtent: Float,
        val firstDotCenterDegrees: Float,
        val dotCenterStepDegrees: Float,
        val balancedEdgeGap: Float,
        val bothSidesBottomIconVisible: Boolean,
        val bothSidesStepDegrees: Double,
        val bothSidesWithSlotFirstDegrees: Double,
        val bothSidesBalancedFirstDegrees: Double,
    ) {
        fun bottomDotAngle(index: Int): Double {
            require(index in 0 until DOT_COUNT)
            val ascendingIndex = DOT_COUNT - 1 - index
            return Math.toRadians(
                (
                    firstDotCenterDegrees +
                        ascendingIndex * dotCenterStepDegrees
                ).toDouble(),
            )
        }

        fun centeredScale(dual: Boolean): Float {
            val dotRadius = if (dual) dualMobileDotRadius else mobileDotRadius
            val y = if (dual) dualRowGap / 2f else 0f
            val outer = sqrt((horizontalDotSpan / 2f) * (horizontalDotSpan / 2f) + y * y) + dotRadius
            val inner = (RING_RADIUS - ringStroke / 2f - 2f).coerceAtLeast(0f)
            return (inner / outer).coerceIn(0f, 1f)
        }

        fun singleDotX(index: Int, style: SignalDotArrangement): Float =
            if (style == SignalDotArrangement.HORIZONTAL) horizontalDotX(index)
            else cos(bottomDotAngle(index)).toFloat() * MOBILE_ORBIT_RADIUS

        fun singleDotY(index: Int, style: SignalDotArrangement, offsetY: Float): Float =
            if (style == SignalDotArrangement.HORIZONTAL) {
                // Horizontal row: same x reference and baseline as the paired horizontal layout.
                sqrt(RING_RADIUS * RING_RADIUS -
                    (horizontalDotSpan / 6f) * (horizontalDotSpan / 6f)) - dualOpticalRise - offsetY
            } else sin(bottomDotAngle(index)).toFloat() * MOBILE_ORBIT_RADIUS

        fun horizontalDotX(index: Int): Float =
            (index - (DOT_COUNT - 1) / 2f) * (horizontalDotSpan / (DOT_COUNT - 1))

        fun dualDotRadius(style: SignalDotArrangement): Float =
            if (style == SignalDotArrangement.BOTH_SIDES) bothSidesMobileDotRadius
            else dualMobileDotRadius

        fun dualDotX(index: Int, row: Int, style: SignalDotArrangement): Float =
            if (style == SignalDotArrangement.BOTH_SIDES) {
                cos(bothSidesDotAngle(index, row)).toFloat() * RING_RADIUS
            } else horizontalDotX(index)

        private fun bothSidesDotAngle(index: Int, row: Int): Double {
            require(index in 0 until DOT_COUNT && row in 0..1)
            val first = if (bothSidesBottomIconVisible) {
                bothSidesWithSlotFirstDegrees
            } else {
                bothSidesBalancedFirstDegrees
            }
            val angle = first + index * bothSidesStepDegrees
            return Math.toRadians(if (row == 0) angle else 180.0 - angle)
        }

        // Balance visible ring-to-dot clearance against adjacent dot spacing.
        fun followingLowerHalfGap(
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
                    val spread = if (reach >= distance + ringRadius) 180.0 else Math.toDegrees(
                        acos(((distance * distance + ringRadius * ringRadius - reach * reach) /
                            (2.0 * distance * ringRadius)).coerceIn(-1.0, 1.0)),
                    )
                    halfGap = max(halfGap, kotlin.math.abs(Math.toDegrees(atan2(y, x)) - 90.0) + spread)
                }
            }
            return halfGap.toFloat().coerceIn(0f, 90f)
        }


        private fun requiredBothSidesHalfGap(
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
                val spread = if (clearance >= d + radius) 180.0 else Math.toDegrees(
                    acos(((d * d + radius * radius - clearance * clearance) /
                        (2.0 * d * radius)).coerceIn(-1.0, 1.0)),
                )
                val angle = Math.toDegrees(atan2(y, x))
                half = max(half, kotlin.math.abs(angle - 90.0) + spread)
            }
            return half
        }


        fun dualDotY(
            index: Int,
            row: Int,
            style: SignalDotArrangement = SignalDotArrangement.CURVED,
            offsetY: Float = 0f,
        ): Float {
            if (style == SignalDotArrangement.BOTH_SIDES) {
                return sin(bothSidesDotAngle(index, row)).toFloat() * RING_RADIUS - offsetY
            }
            val x = if (style == SignalDotArrangement.HORIZONTAL) horizontalDotSpan / 6f else horizontalDotX(index)
            val offset = if (row == 0) -dualRowGap / 2f else dualRowGap / 2f
            return sqrt(RING_RADIUS * RING_RADIUS - x * x) +
                offset - dualOpticalRise - offsetY
        }
    }

    fun normalizeWeightScale(value: Float): Float =
        value
            .takeIf(Float::isFinite)
            ?.coerceIn(MIN_WEIGHT_SCALE, MAX_WEIGHT_SCALE)
            ?: DEFAULT_WEIGHT_SCALE

    fun resolve(weightScale: Float, bothSidesBottomIconVisible: Boolean = true): Resolved {
        val normalized = normalizeWeightScale(weightScale)
        val ringStroke = BASE_RING_STROKE * normalized
        val dotRadius = BASE_MOBILE_DOT_RADIUS * normalized
        val unavailableMarkStroke = BASE_UNAVAILABLE_MARK_STROKE * normalized
        val unavailableMarkHalfExtent = BASE_UNAVAILABLE_MARK_HALF_EXTENT * normalized

        val step = solveOuterDotStep(ringStroke = ringStroke, dotRadius = dotRadius)
        val firstCenter =
            LOWER_OPENING_CENTER_DEGREES.toFloat() -
                1.5f * step
        val gap =
            dotToDotEdgeGap(
                stepDegrees = step,
                dotRadius = dotRadius,
            )

        val dualDotRadius = DUAL_BASE_DOT_RADIUS * normalized
        // Eight circles no longer fit the lower semicircle if their radii grow
        // without bound. Limit only the split presentation at extreme weights.
        val splitDotRadius = min(dualDotRadius, 6.5f)
        val minimumChord = 2f * splitDotRadius + 1f
        val bothSidesStep = max(
            if (bothSidesBottomIconVisible) 16.0 else 18.0,
            Math.toDegrees(2.0 * kotlin.math.asin(
                (minimumChord / (2f * RING_RADIUS)).coerceIn(0f, 1f).toDouble(),
            )),
        )
        val ringClearance = ringStroke / 2f + splitDotRadius + 2f
        val endMargin = Math.toDegrees(2.0 * kotlin.math.asin(
            (ringClearance / (2f * RING_RADIUS)).coerceIn(0f, 1f).toDouble(),
        ))
        val outermostAllowed = 180.0 - 3.0 * bothSidesStep - endMargin
        val withSlotFirst = minOf(112.0, outermostAllowed)
        val balancedFirst = balanceBothSidesFirst(
            outermostAllowed = outermostAllowed,
            bothSidesStep = bothSidesStep,
            splitDotRadius = splitDotRadius,
            ringStroke = ringStroke,
        )
        // Wider only at extreme weights, when neighboring dots would otherwise touch.
        val horizontalDotSpan = max(DUAL_DOT_SPAN, 6f * dualDotRadius + 2f)
        val naturalRowGap = DUAL_BASE_ROW_GAP * normalized
        val rowTightening =
            ((normalized - DEFAULT_WEIGHT_SCALE) * DUAL_ROW_TIGHTENING_RATE)
                .coerceIn(0f, DUAL_MAX_ROW_TIGHTENING)
        val dualRowGap = naturalRowGap - rowTightening
        val middleColumnX = horizontalDotSpan / (DOT_COUNT - 1) / 2f
        val middleArcY = sqrt(RING_RADIUS * RING_RADIUS - middleColumnX * middleColumnX)
        val dualOpticalRise =
            max(
                DUAL_BASE_OPTICAL_RISE,
                MOBILE_CENTER_Y + middleArcY + naturalRowGap / 2f + dualDotRadius +
                    DUAL_BOTTOM_MARGIN - CANVAS_BOTTOM,
            )

        return Resolved(
            weightScale = normalized,
            ringStroke = ringStroke,
            mobileDotRadius = dotRadius,
            dualMobileDotRadius = dualDotRadius,
            bothSidesMobileDotRadius = splitDotRadius,
            horizontalDotSpan = horizontalDotSpan,
            dualRowGap = dualRowGap,
            dualOpticalRise = dualOpticalRise,
            unavailableMarkStroke = unavailableMarkStroke,
            unavailableMarkHalfExtent = unavailableMarkHalfExtent,
            firstDotCenterDegrees = firstCenter,
            dotCenterStepDegrees = step,
            balancedEdgeGap = gap,
            bothSidesBottomIconVisible = bothSidesBottomIconVisible,
            bothSidesStepDegrees = bothSidesStep,
            bothSidesWithSlotFirstDegrees = withSlotFirst,
            bothSidesBalancedFirstDegrees = balancedFirst,
        )
    }

    // Match the opening ring-to-dot gap with the nearest dot-to-dot gap.
    private fun solveOuterDotStep(ringStroke: Float, dotRadius: Float): Float {
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

    private fun balanceBothSidesFirst(
        outermostAllowed: Double,
        bothSidesStep: Double,
        splitDotRadius: Float,
        ringStroke: Float,
    ): Double {
        // When neither bottom feature is enabled, balance *visible edge-to-edge*
        // clearance, not merely angular center spacing. The half-circle ends
        // at 0°/180° and is never shortened to balance the split dot clusters.
        fun visualChordGap(angle: Double, halfWidthSum: Double): Double =
            2.0 * RING_RADIUS * sin(Math.toRadians(angle / 2.0)) - halfWidthSum
        var balancedLower = 90.0
        var balancedUpper = outermostAllowed
        repeat(20) {
            val first = (balancedLower + balancedUpper) / 2.0
            val inner = visualChordGap(2.0 * first - 180.0, 2.0 * splitDotRadius)
            val edge = visualChordGap(
                180.0 - first - 3.0 * bothSidesStep,
                splitDotRadius + ringStroke / 2.0,
            )
            if (inner > edge) balancedUpper = first else balancedLower = first
        }
        return (balancedLower + balancedUpper) / 2.0
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
        val ringX = cos(ringAngle).toFloat() * RING_RADIUS
        val ringY = sin(ringAngle).toFloat() * RING_RADIUS
        val dotX = cos(dotAngle).toFloat() * MOBILE_ORBIT_RADIUS
        val dotY = sin(dotAngle).toFloat() * MOBILE_ORBIT_RADIUS
        val dx = dotX - ringX
        val dy = dotY - ringY
        return sqrt(dx * dx + dy * dy) -
            ringStroke / 2f -
            dotRadius
    }

    private fun dotToDotEdgeGap(
        stepDegrees: Float,
        dotRadius: Float,
    ): Float {
        val stepRadians = Math.toRadians(stepDegrees.toDouble())
        val centerDistance =
            2f *
                MOBILE_ORBIT_RADIUS *
                sin(stepRadians / 2.0).toFloat()
        return centerDistance - 2f * dotRadius
    }
}
