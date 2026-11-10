package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.COMBINED_SCALE_DEFAULT
import com.chaners.guiyuan.settings.COMBINED_SCALE_MAX
import com.chaners.guiyuan.settings.COMBINED_SCALE_MIN
import com.chaners.guiyuan.settings.AIRPLANE_SIZE_SCALE_MAX as SETTINGS_AIRPLANE_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.AIRPLANE_SIZE_SCALE_MIN as SETTINGS_AIRPLANE_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.MOBILE_TYPE_SIZE_SCALE_MAX as SETTINGS_MOBILE_TYPE_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.MOBILE_TYPE_SIZE_SCALE_MIN as SETTINGS_MOBILE_TYPE_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.MOBILE_TYPE_WEIGHT_DEFAULT as SETTINGS_MOBILE_TYPE_WEIGHT_DEFAULT
import com.chaners.guiyuan.settings.MOBILE_TYPE_WEIGHT_MAX as SETTINGS_MOBILE_TYPE_WEIGHT_MAX
import com.chaners.guiyuan.settings.MOBILE_TYPE_WEIGHT_MIN as SETTINGS_MOBILE_TYPE_WEIGHT_MIN
import com.chaners.guiyuan.settings.NO_SIM_SIZE_SCALE_MAX as SETTINGS_NO_SIM_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.NO_SIM_SIZE_SCALE_MIN as SETTINGS_NO_SIM_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.WIFI_SIZE_SCALE_MAX as SETTINGS_WIFI_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.WIFI_SIZE_SCALE_MIN as SETTINGS_WIFI_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.SignalDotArrangement
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

internal fun nativeCenterTintEntryName(entryName: String): String {
    val base =
        entryName
            .removeSuffix("_darkmode")
            .removeSuffix("_tint")
    return base + "_tint"
}


internal object NativeOpticalGeometry {
    private const val MIN_OPTICAL_RATIO = 0.08f
    internal data class Resolved(
        val drawWidth: Float,
        val drawHeight: Float,
        val opticalLeft: Float,
        val opticalTop: Float,
        val opticalRight: Float,
        val opticalBottom: Float,
    ) {
        val opticalWidth: Float
            get() = (opticalRight - opticalLeft).coerceAtLeast(0f)

        val opticalHeight: Float
            get() = (opticalBottom - opticalTop).coerceAtLeast(0f)
    }

    fun resolve(
        currentIntrinsicWidth: Int,
        currentIntrinsicHeight: Int,
        currentOpticalLeft: Float,
        currentOpticalTop: Float,
        currentOpticalRight: Float,
        currentOpticalBottom: Float,
        fitIntrinsicWidth: Int,
        fitIntrinsicHeight: Int,
        fitOpticalLeft: Float,
        fitOpticalTop: Float,
        fitOpticalRight: Float,
        fitOpticalBottom: Float,
        centerX: Float,
        centerY: Float,
        maxWidth: Float,
        maxHeight: Float,
    ): Resolved? {
        if (
            currentIntrinsicWidth <= 0 ||
            currentIntrinsicHeight <= 0 ||
            fitIntrinsicWidth <= 0 ||
            fitIntrinsicHeight <= 0 ||
            maxWidth <= 0f ||
            maxHeight <= 0f
        ) {
            return null
        }

        val fitOpticalWidthRatio =
            (fitOpticalRight - fitOpticalLeft)
                .coerceAtLeast(MIN_OPTICAL_RATIO)
        val fitOpticalHeightRatio =
            (fitOpticalBottom - fitOpticalTop)
                .coerceAtLeast(MIN_OPTICAL_RATIO)
        val fitOpticalWidth =
            fitIntrinsicWidth * fitOpticalWidthRatio
        val fitOpticalHeight =
            fitIntrinsicHeight * fitOpticalHeightRatio
        if (fitOpticalWidth <= 0f || fitOpticalHeight <= 0f) return null

        val drawableScale =
            min(
                maxWidth / fitOpticalWidth,
                maxHeight / fitOpticalHeight,
            )
        if (!drawableScale.isFinite() || drawableScale <= 0f) return null

        val drawWidth = currentIntrinsicWidth * drawableScale
        val drawHeight = currentIntrinsicHeight * drawableScale
        val drawLeft = centerX - drawWidth / 2f
        val drawTop = centerY - drawHeight / 2f
        return Resolved(
            drawWidth = drawWidth,
            drawHeight = drawHeight,
            opticalLeft = drawLeft + currentOpticalLeft * drawWidth,
            opticalTop = drawTop + currentOpticalTop * drawHeight,
            opticalRight = drawLeft + currentOpticalRight * drawWidth,
            opticalBottom = drawTop + currentOpticalBottom * drawHeight,
        )
    }

    // Offset viewport centers by native ink, rather than transparent padding,
    // to keep the displayed icon fixed as its size changes.
    fun viewportCenterForInkAnchor(
        anchorX: Float,
        anchorY: Float,
        drawWidth: Float,
        drawHeight: Float,
        opticalCenterX: Float,
        inkCenterY: Float,
    ): Pair<Float, Float> = Pair(
        anchorX - (opticalCenterX - 0.5f) * drawWidth,
        anchorY - (inkCenterY - 0.5f) * drawHeight,
    )
}


internal object CenterGeometry {
    const val DEFAULT_WIFI_SIZE_SCALE = 1.00f
    const val DEFAULT_MOBILE_TYPE_SIZE_SCALE = 1.00f
    const val MIN_WIFI_SIZE_SCALE = SETTINGS_WIFI_SIZE_SCALE_MIN
    const val MAX_WIFI_SIZE_SCALE = SETTINGS_WIFI_SIZE_SCALE_MAX
    const val DEFAULT_AIRPLANE_SIZE_SCALE = 1.00f
    const val MIN_AIRPLANE_SIZE_SCALE = SETTINGS_AIRPLANE_SIZE_SCALE_MIN
    const val MAX_AIRPLANE_SIZE_SCALE = SETTINGS_AIRPLANE_SIZE_SCALE_MAX
    const val DEFAULT_NO_SIM_SIZE_SCALE = 1.00f
    const val MIN_NO_SIM_SIZE_SCALE = SETTINGS_NO_SIM_SIZE_SCALE_MIN
    const val MAX_NO_SIM_SIZE_SCALE = SETTINGS_NO_SIM_SIZE_SCALE_MAX
    const val MIN_MOBILE_TYPE_SIZE_SCALE = SETTINGS_MOBILE_TYPE_SIZE_SCALE_MIN
    const val MAX_MOBILE_TYPE_SIZE_SCALE = SETTINGS_MOBILE_TYPE_SIZE_SCALE_MAX
    const val DEFAULT_MOBILE_TYPE_WEIGHT = SETTINGS_MOBILE_TYPE_WEIGHT_DEFAULT
    const val MIN_MOBILE_TYPE_WEIGHT = SETTINGS_MOBILE_TYPE_WEIGHT_MIN
    const val MAX_MOBILE_TYPE_WEIGHT = SETTINGS_MOBILE_TYPE_WEIGHT_MAX

    private const val BASE_WIFI_MAX_WIDTH = 58f
    private const val BASE_WIFI_MAX_HEIGHT = 45f
    private const val BASE_AIRPLANE_MAX_SIZE = 58f
    private const val BASE_NO_SIM_MAX_SIZE = 54f
    private const val BASE_MOBILE_TYPE_TEXT_SIZE = 39f
    private const val BASE_MOBILE_TYPE_SUFFIX_SIZE = 23f
    private const val BASE_MOBILE_TYPE_SUFFIX_RISE = 8f

    data class Resolved(
        val wifiScale: Float,
        val airplaneScale: Float,
        val noSimScale: Float,
        val mobileTypeScale: Float,
        val wifiMaxWidth: Float,
        val wifiMaxHeight: Float,
        val airplaneMaxSize: Float,
        val noSimMaxSize: Float,
        val mobileTypeTextSize: Float,
        val mobileTypeSuffixSize: Float,
        val mobileTypeSuffixRise: Float,
        val mobileTypeWeight: Int,
        val combinedScale: Float,
    )

    fun resolve(
        wifiScale: Float,
        mobileTypeScale: Float,
        mobileTypeWeight: Int,
        airplaneScale: Float = DEFAULT_AIRPLANE_SIZE_SCALE,
        noSimScale: Float = DEFAULT_NO_SIM_SIZE_SCALE,
        combinedScale: Float = COMBINED_SCALE_DEFAULT,
    ): Resolved {
        val wifi =
            wifiScale.takeIf(Float::isFinite)?.coerceIn(MIN_WIFI_SIZE_SCALE, MAX_WIFI_SIZE_SCALE)
                ?: DEFAULT_WIFI_SIZE_SCALE
        val airplane =
            airplaneScale.takeIf(Float::isFinite)
                ?.coerceIn(MIN_AIRPLANE_SIZE_SCALE, MAX_AIRPLANE_SIZE_SCALE)
                ?: DEFAULT_AIRPLANE_SIZE_SCALE
        val noSim =
            noSimScale.takeIf(Float::isFinite)
                ?.coerceIn(MIN_NO_SIM_SIZE_SCALE, MAX_NO_SIM_SIZE_SCALE)
                ?: DEFAULT_NO_SIM_SIZE_SCALE
        val mobile =
            mobileTypeScale.takeIf(Float::isFinite)
                ?.coerceIn(MIN_MOBILE_TYPE_SIZE_SCALE, MAX_MOBILE_TYPE_SIZE_SCALE)
                ?: DEFAULT_MOBILE_TYPE_SIZE_SCALE
        val weight =
            mobileTypeWeight.coerceIn(MIN_MOBILE_TYPE_WEIGHT, MAX_MOBILE_TYPE_WEIGHT)
        val combined =
            combinedScale
                .takeIf(Float::isFinite)
                ?.coerceIn(COMBINED_SCALE_MIN, COMBINED_SCALE_MAX)
                ?: COMBINED_SCALE_DEFAULT
        return Resolved(
            wifiScale = wifi,
            airplaneScale = airplane,
            noSimScale = noSim,
            mobileTypeScale = mobile,
            wifiMaxWidth = BASE_WIFI_MAX_WIDTH * wifi,
            wifiMaxHeight = BASE_WIFI_MAX_HEIGHT * wifi,
            airplaneMaxSize = BASE_AIRPLANE_MAX_SIZE * airplane,
            noSimMaxSize = BASE_NO_SIM_MAX_SIZE * noSim,
            mobileTypeTextSize = BASE_MOBILE_TYPE_TEXT_SIZE * mobile,
            mobileTypeSuffixSize = BASE_MOBILE_TYPE_SUFFIX_SIZE * mobile,
            mobileTypeSuffixRise = BASE_MOBILE_TYPE_SUFFIX_RISE * mobile,
            mobileTypeWeight = weight,
            combinedScale = combined,
        )
    }
}

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

    private const val LOWER_OPENING_RIGHT_DEGREES = 150.0
    private const val DOT_COUNT = 4

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

        val step = RingGapPolicy.solveOuterDotStep(ringStroke = ringStroke, dotRadius = dotRadius)
        val firstCenter =
            RingGapPolicy.LOWER_OPENING_CENTER_DEGREES.toFloat() -
                1.5f * step
        val gap =
            RingGapPolicy.dotToDotEdgeGap(
                stepDegrees = step,
                dotRadius = dotRadius,
            )

        val dualDotRadius = DUAL_BASE_DOT_RADIUS * normalized
        // Eight circles no longer fit the lower semicircle if their radii grow
        // without bound. Limit only the split presentation at extreme weights.
        val splitDotRadius = min(dualDotRadius, 6.5f)
        val bothSidesStep = bothSidesDotStep(bothSidesBottomIconVisible, splitDotRadius)
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
        val dualOpticalRise = computeDualOpticalRise(horizontalDotSpan, naturalRowGap, dualDotRadius)

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

    private fun bothSidesDotStep(
        bothSidesBottomIconVisible: Boolean,
        splitDotRadius: Float,
    ): Double {
        val minimumChord = 2f * splitDotRadius + 1f
        return max(
            if (bothSidesBottomIconVisible) 16.0 else 18.0,
            Math.toDegrees(2.0 * kotlin.math.asin(
                (minimumChord / (2f * RING_RADIUS)).coerceIn(0f, 1f).toDouble(),
            )),
        )
    }

    private fun computeDualOpticalRise(
        horizontalDotSpan: Float,
        naturalRowGap: Float,
        dualDotRadius: Float,
    ): Float {
        val middleColumnX = horizontalDotSpan / (DOT_COUNT - 1) / 2f
        val middleArcY = sqrt(RING_RADIUS * RING_RADIUS - middleColumnX * middleColumnX)
        return max(
                DUAL_BASE_OPTICAL_RISE,
                MOBILE_CENTER_Y + middleArcY + naturalRowGap / 2f + dualDotRadius +
                    DUAL_BOTTOM_MARGIN - CANVAS_BOTTOM,
            )
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
}
