package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.BATTERY_TOP_VERTICAL_OFFSET_DEFAULT
import com.chaners.guiyuan.settings.COMBINED_SCALE_DEFAULT
import com.chaners.guiyuan.settings.COMBINED_SCALE_MAX
import com.chaners.guiyuan.settings.COMBINED_SCALE_MIN
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
