package com.chaners.guiyuan.xposed

internal object BatteryArcPolicy {
    internal data class Segments(
        val activeSweep: Float,
        val inactiveStart: Float,
        val inactiveSweep: Float,
    )

    fun resolve(
        batteryPercent: Int,
        startDegrees: Float,
        maxSweep: Float,
        degreesPerPercent: Float,
    ): Segments {
        val boundedPercent = batteryPercent.coerceIn(0, 100)
        val activeSweep =
            (boundedPercent * degreesPerPercent)
                .coerceIn(0f, maxSweep)
        val inactiveSweep = (maxSweep - activeSweep).coerceAtLeast(0f)
        return Segments(
            activeSweep = activeSweep,
            inactiveStart = startDegrees + activeSweep,
            inactiveSweep = inactiveSweep,
        )
    }
}
