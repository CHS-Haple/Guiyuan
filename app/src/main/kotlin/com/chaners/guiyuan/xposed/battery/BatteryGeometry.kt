package com.chaners.guiyuan.xposed.battery

import kotlin.math.ceil

internal data class BatteryArcSegments(
    val activeSweep: Float,
    val inactiveStart: Float,
    val inactiveSweep: Float,
)

internal fun batteryArcSegments(
    batteryPercent: Int,
    startDegrees: Float,
    maxSweep: Float,
    degreesPerPercent: Float,
): BatteryArcSegments {
    val boundedPercent = batteryPercent.coerceIn(0, 100)
    val activeSweep =
        (boundedPercent * degreesPerPercent)
            .coerceIn(0f, maxSweep)
    val inactiveSweep = (maxSweep - activeSweep).coerceAtLeast(0f)
    return BatteryArcSegments(
        activeSweep = activeSweep,
        inactiveStart = startDegrees + activeSweep,
        inactiveSweep = inactiveSweep,
    )
}

internal fun batteryTopBaseCenterY(
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

internal fun batteryTopCenterY(
    baseCenterY: Float,
    requestedOffset: Float,
): Float {
    if (!baseCenterY.isFinite() || !requestedOffset.isFinite()) {
        return baseCenterY
    }
    return baseCenterY - requestedOffset
}

internal fun batteryTopOverflowPx(
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
