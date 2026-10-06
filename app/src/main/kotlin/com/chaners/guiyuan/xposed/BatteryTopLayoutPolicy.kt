package com.chaners.guiyuan.xposed

import kotlin.math.ceil

internal object BatteryTopLayout {
    fun resolveOpticalBaseCenterY(
        preferredCenterY: Float,
        defaultOpticalRise: Float,
    ): Float {
        if (!preferredCenterY.isFinite()) return 0f
        val rise =
            defaultOpticalRise
                .takeIf(Float::isFinite)
                ?.coerceAtLeast(0f)
                ?: 0f
        return preferredCenterY - rise
    }

    fun resolveCenterY(
        baseCenterY: Float,
        requestedOffset: Float,
    ): Float {
        if (!baseCenterY.isFinite() || !requestedOffset.isFinite()) {
            return baseCenterY
        }
        return baseCenterY - requestedOffset
    }

    fun resolveRequiredTopOverflowPx(
        transformScale: Float,
        transformOffsetY: Float,
        contentTopY: Float,
    ): Int {
        if (
            !transformScale.isFinite() ||
            transformScale <= 0f ||
            !transformOffsetY.isFinite() ||
            !contentTopY.isFinite()
        ) {
            return 0
        }
        val physicalTop =
            transformOffsetY +
                contentTopY * transformScale
        if (physicalTop >= 0f) return 0
        return ceil(-physicalTop).toInt() + 1
    }
}
