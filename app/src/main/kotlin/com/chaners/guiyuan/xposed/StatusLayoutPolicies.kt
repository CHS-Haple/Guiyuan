package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.BATTERY_TOP_VERTICAL_OFFSET_DEFAULT
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
import com.chaners.guiyuan.xposed.network.CenterIndicator
import com.chaners.guiyuan.settings.ContentLayout
import com.chaners.guiyuan.settings.topOffsetYUi
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal object TopInfoOffsetPolicy {
    fun readoutRequestedOffset(
        layout: ContentLayout,
        rawOffset: Float,
    ): Float =
        if (layout != ContentLayout.BATTERY_CENTER) {
            rawOffset
        } else {
            BATTERY_TOP_VERTICAL_OFFSET_DEFAULT
        }

    fun networkTranslationDelta(
        layout: ContentLayout,
        rawOffset: Float,
        indicator: CenterIndicator? = null,
        wifiOffset: Float = 0f,
        mobileTypeOffset: Float = 0f,
    ): Float =
        if (layout == ContentLayout.BATTERY_CENTER) {
            when (indicator) {
                is CenterIndicator.Wifi -> -wifiOffset
                is CenterIndicator.MobileType -> -mobileTypeOffset
                else -> -topOffsetYUi(rawOffset)
            }
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
    fun morphProgress(
        main: String,
        suffix: String,
        nativeTargetAvailable: Boolean,
        progress: Float,
    ): Float =
        if (main == "5G" && suffix == "A" && nativeTargetAvailable && progress.isFinite()) {
            progress.coerceIn(0f, 1f)
        } else {
            0f
        }

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

internal object MobileTypeSpacing {
    data class Ink(val left: Float, val right: Float)
    data class Fit(val suffixX: Float, val left: Float, val right: Float)

    fun fit(main: Ink, suffix: Ink, clearance: Float): Fit? {
        if (
            !main.left.isFinite() || !main.right.isFinite() ||
            !suffix.left.isFinite() || !suffix.right.isFinite() ||
            !clearance.isFinite() ||
            main.right <= main.left || suffix.right <= suffix.left
        ) return null

        // Keep the visible edge gap stable as native glyph advances change with weight.
        val suffixX = main.right - suffix.left + clearance.coerceAtLeast(0f)
        val left = min(main.left, suffixX + suffix.left)
        val right = max(main.right, suffixX + suffix.right)
        return Fit(suffixX, left, right)
            .takeIf { it.suffixX.isFinite() && it.left.isFinite() && it.right.isFinite() }
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

internal object MobileDotRelocationPolicy {
    fun shouldInterpolate(
        previous: CenterIndicator?,
        current: CenterIndicator,
        centeredBefore: Boolean,
        centeredNow: Boolean,
    ): Boolean =
        previous != null && previous != current && centeredBefore != centeredNow

    // The caller reuses the center content's enter progress. No second clock.
    fun coordinate(from: Float, to: Float, progress: Float): Float =
        from + (to - from) * progress.coerceIn(0f, 1f)
}

internal object CenteredMobileTypePositionPolicy {
    // Only dot clearance limits upward motion; overflow is handled by the render surface.
    fun translation(
        sourceTop: Float,
        mobileBottom: Float,
        requested: Float,
        clearance: Float,
    ): Float = max(requested, mobileBottom + clearance - sourceTop)
}

