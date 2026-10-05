package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.BATTERY_TOP_VERTICAL_OFFSET_DEFAULT
import com.chaners.guiyuan.settings.COMBINED_SCALE_DEFAULT
import com.chaners.guiyuan.settings.COMBINED_SCALE_MAX
import com.chaners.guiyuan.settings.COMBINED_SCALE_MIN
import com.chaners.guiyuan.settings.AIRPLANE_SIZE_SCALE_MAX as SETTINGS_AIRPLANE_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.AIRPLANE_SIZE_SCALE_MIN as SETTINGS_AIRPLANE_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.MOBILE_TYPE_SIZE_SCALE_MAX as SETTINGS_MOBILE_TYPE_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.MOBILE_TYPE_SIZE_SCALE_MIN as SETTINGS_MOBILE_TYPE_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.NO_SIM_SIZE_SCALE_MAX as SETTINGS_NO_SIM_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.NO_SIM_SIZE_SCALE_MIN as SETTINGS_NO_SIM_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.WIFI_SIZE_SCALE_MAX as SETTINGS_WIFI_SIZE_SCALE_MAX
import com.chaners.guiyuan.settings.WIFI_SIZE_SCALE_MIN as SETTINGS_WIFI_SIZE_SCALE_MIN
import com.chaners.guiyuan.settings.ContentLayout
import com.chaners.guiyuan.settings.batteryTopVerticalOffsetUi
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal object TopInfoOffsetPolicy {
    fun readoutRequestedOffset(
        layout: ContentLayout,
        rawOffset: Float,
    ): Float =
        if (layout == ContentLayout.NETWORK_CENTER) {
            rawOffset
        } else {
            BATTERY_TOP_VERTICAL_OFFSET_DEFAULT
        }

    fun networkTranslationDelta(
        layout: ContentLayout,
        rawOffset: Float,
    ): Float =
        if (layout == ContentLayout.BATTERY_CENTER) {
            -batteryTopVerticalOffsetUi(rawOffset)
        } else {
            0f
        }
}

internal object MobileTypeScalePolicy {
    fun localValue(
        baseValue: Float,
        canvasScale: Float,
        combinedScale: Float,
        scaleWithCanvas: Boolean,
    ): Float {
        if (scaleWithCanvas || !canvasScale.isFinite() || canvasScale <= 0f) {
            return baseValue
        }
        val userScale =
            combinedScale
                .takeIf(Float::isFinite)
                ?.coerceIn(COMBINED_SCALE_MIN, COMBINED_SCALE_MAX)
                ?: COMBINED_SCALE_DEFAULT
        return baseValue * userScale / canvasScale
    }
}

internal object MobileTypeSuffixPolicy {
    fun verticalOffset(
        suffix: String,
        magnitude: Float,
    ): Float =
        if (suffix.trim().uppercase() == "A") {
            magnitude
        } else {
            -magnitude
        }
}

internal object NativeCenterResourceVariantPolicy {
    fun tintEntryName(entryName: String): String {
        val base =
            entryName
                .removeSuffix("_darkmode")
                .removeSuffix("_tint")
        return base + "_tint"
    }
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
}


internal data class NativeRenderTransform(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
)

internal object NativeRenderGeometry {
    internal data class PixelBounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    )

    fun resolvePixelBounds(
        centerX: Float,
        centerY: Float,
        drawWidth: Float,
        drawHeight: Float,
        transform: NativeRenderTransform,
    ): PixelBounds {
        val physicalCenterX =
            transform.offsetX + centerX * transform.scale
        val physicalCenterY =
            transform.offsetY + centerY * transform.scale
        val width =
            max(1, (drawWidth * transform.scale).roundToInt())
        val height =
            max(1, (drawHeight * transform.scale).roundToInt())
        val left = (physicalCenterX - width / 2f).roundToInt()
        val top = (physicalCenterY - height / 2f).roundToInt()
        return PixelBounds(
            left = left,
            top = top,
            right = left + width,
            bottom = top + height,
        )
    }
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
    const val DEFAULT_MOBILE_TYPE_WEIGHT = 800
    const val MIN_MOBILE_TYPE_WEIGHT = 500
    const val MAX_MOBILE_TYPE_WEIGHT = 950

    private const val BASE_WIFI_MAX_WIDTH = 58f
    private const val BASE_WIFI_MAX_HEIGHT = 45f
    private const val BASE_AIRPLANE_MAX_SIZE = 58f
    private const val BASE_NO_SIM_MAX_SIZE = 54f
    private const val BASE_MOBILE_TYPE_TEXT_SIZE = 39f
    private const val BASE_MOBILE_TYPE_SUFFIX_SIZE = 23f
    private const val BASE_MOBILE_TYPE_SUFFIX_RISE = 8f

    data class Resolved(
        val wifiSizeScale: Float,
        val airplaneSizeScale: Float,
        val noSimSizeScale: Float,
        val mobileTypeSizeScale: Float,
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
        wifiSizeScale: Float,
        mobileTypeSizeScale: Float,
        mobileTypeWeight: Int,
        airplaneSizeScale: Float = DEFAULT_AIRPLANE_SIZE_SCALE,
        noSimSizeScale: Float = DEFAULT_NO_SIM_SIZE_SCALE,
        combinedScale: Float = COMBINED_SCALE_DEFAULT,
    ): Resolved {
        val normalizedWifi =
            wifiSizeScale.takeIf(Float::isFinite)?.coerceIn(MIN_WIFI_SIZE_SCALE, MAX_WIFI_SIZE_SCALE)
                ?: DEFAULT_WIFI_SIZE_SCALE
        val normalizedAirplane =
            airplaneSizeScale.takeIf(Float::isFinite)
                ?.coerceIn(MIN_AIRPLANE_SIZE_SCALE, MAX_AIRPLANE_SIZE_SCALE)
                ?: DEFAULT_AIRPLANE_SIZE_SCALE
        val normalizedNoSim =
            noSimSizeScale.takeIf(Float::isFinite)
                ?.coerceIn(MIN_NO_SIM_SIZE_SCALE, MAX_NO_SIM_SIZE_SCALE)
                ?: DEFAULT_NO_SIM_SIZE_SCALE
        val normalizedMobile =
            mobileTypeSizeScale.takeIf(Float::isFinite)
                ?.coerceIn(MIN_MOBILE_TYPE_SIZE_SCALE, MAX_MOBILE_TYPE_SIZE_SCALE)
                ?: DEFAULT_MOBILE_TYPE_SIZE_SCALE
        val normalizedWeight =
            mobileTypeWeight.coerceIn(MIN_MOBILE_TYPE_WEIGHT, MAX_MOBILE_TYPE_WEIGHT)
        val normalizedCombined =
            combinedScale
                .takeIf(Float::isFinite)
                ?.coerceIn(COMBINED_SCALE_MIN, COMBINED_SCALE_MAX)
                ?: COMBINED_SCALE_DEFAULT
        return Resolved(
            wifiSizeScale = normalizedWifi,
            airplaneSizeScale = normalizedAirplane,
            noSimSizeScale = normalizedNoSim,
            mobileTypeSizeScale = normalizedMobile,
            wifiMaxWidth = BASE_WIFI_MAX_WIDTH * normalizedWifi,
            wifiMaxHeight = BASE_WIFI_MAX_HEIGHT * normalizedWifi,
            airplaneMaxSize = BASE_AIRPLANE_MAX_SIZE * normalizedAirplane,
            noSimMaxSize = BASE_NO_SIM_MAX_SIZE * normalizedNoSim,
            mobileTypeTextSize = BASE_MOBILE_TYPE_TEXT_SIZE * normalizedMobile,
            mobileTypeSuffixSize = BASE_MOBILE_TYPE_SUFFIX_SIZE * normalizedMobile,
            mobileTypeSuffixRise = BASE_MOBILE_TYPE_SUFFIX_RISE * normalizedMobile,
            mobileTypeWeight = normalizedWeight,
            combinedScale = normalizedCombined,
        )
    }
}

internal object OuterGeometry {
    private const val PREVIOUS_DEFAULT_WEIGHT_SCALE = 1.10f

    const val RING_RADIUS = 50f
    const val BASE_RING_STROKE = 8.25f
    const val MOBILE_ORBIT_RADIUS = 51f
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
        val unavailableMarkStroke: Float,
        val unavailableMarkHalfExtent: Float,
        val firstDotCenterDegrees: Float,
        val dotCenterStepDegrees: Float,
        val balancedEdgeGap: Float,
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
    }

    fun normalizeWeightScale(value: Float): Float =
        value
            .takeIf(Float::isFinite)
            ?.coerceIn(MIN_WEIGHT_SCALE, MAX_WEIGHT_SCALE)
            ?: DEFAULT_WEIGHT_SCALE

    fun resolve(weightScale: Float): Resolved {
        val normalized = normalizeWeightScale(weightScale)
        val ringStroke = BASE_RING_STROKE * normalized
        val dotRadius = BASE_MOBILE_DOT_RADIUS * normalized
        val unavailableMarkStroke = BASE_UNAVAILABLE_MARK_STROKE * normalized
        val unavailableMarkHalfExtent = BASE_UNAVAILABLE_MARK_HALF_EXTENT * normalized

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

        val step = (lowerStep + upperStep) / 2f
        val firstCenter =
            LOWER_OPENING_CENTER_DEGREES.toFloat() -
                1.5f * step
        val gap =
            dotToDotEdgeGap(
                stepDegrees = step,
                dotRadius = dotRadius,
            )

        return Resolved(
            weightScale = normalized,
            ringStroke = ringStroke,
            mobileDotRadius = dotRadius,
            unavailableMarkStroke = unavailableMarkStroke,
            unavailableMarkHalfExtent = unavailableMarkHalfExtent,
            firstDotCenterDegrees = firstCenter,
            dotCenterStepDegrees = step,
            balancedEdgeGap = gap,
        )
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


internal object VisualIntensity {
    fun resolveCanvasAlpha(
        color: Int,
        semanticAlpha: Int,
        opacity: Float,
    ): Int =
        (
            (color ushr 24) *
                (semanticAlpha.coerceIn(0, 255) / 255f) *
                opacity.coerceIn(0f, 1f)
        ).toInt().coerceIn(0, 255)

    fun resolveDrawableAlpha(opacity: Float): Int =
        (255f * opacity.coerceIn(0f, 1f))
            .roundToInt()
            .coerceIn(0, 255)
}
