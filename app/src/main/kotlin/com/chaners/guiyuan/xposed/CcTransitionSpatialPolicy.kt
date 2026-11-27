package com.chaners.guiyuan.xposed

import kotlin.math.sqrt

internal object CcTransitionSpatialPolicy {
    // Keep a retargeted projected participant still at the held native fraction.
    // Continued panel motion, not a new timer, brings it to the live endpoint.
    fun rootAnchoredCarrier(
        carrier: FloatArray,
        root: FloatArray,
        rootRelativeX: Float,
    ): FloatArray {
        require(carrier.size == 6 && root.size == 6)
        return carrier.copyOf().also {
            it[0] = root[0] + rootRelativeX
        }
    }

    // Reject a native FINAL row while its Wi-Fi slot overlaps the battery carrier.
    fun wifiBeforeBattery(wifi: FloatArray, battery: FloatArray, rtl: Boolean): Boolean {
        if (wifi.size != 6 || battery.size != 6 ||
            !wifi.all(Float::isFinite) || !battery.all(Float::isFinite)
        ) return false
        val wifiWidth = vectorLength(wifi[2], wifi[3])
        val batteryWidth = vectorLength(battery[2], battery[3])
        if (wifiWidth <= 0f || batteryWidth <= 0f) return false
        return if (rtl) {
            wifi[0] - wifiWidth / 2f >= battery[0] + batteryWidth / 2f
        } else {
            wifi[0] + wifiWidth / 2f <= battery[0] - batteryWidth / 2f
        }
    }

    // Work in the shared root frame, excluding the offset we own so
    // held/reversed gestures cannot accumulate translation each frame.
    fun nativePeerTranslationX(
        baselineX: Float,
        appliedX: Float,
        fakePeerRootX: Float,
        finalPeerRootX: Float,
        localToRootScaleX: Float,
        progress: Float,
    ): Float? {
        if (!baselineX.isFinite() || !appliedX.isFinite() ||
            !fakePeerRootX.isFinite() || !finalPeerRootX.isFinite() ||
            !localToRootScaleX.isFinite() || kotlin.math.abs(localToRootScaleX) < 0.0001f
        ) return null
        val originalRootX =
            fakePeerRootX - (appliedX - baselineX) * localToRootScaleX
        val fullLocalTravel = (finalPeerRootX - originalRootX) / localToRootScaleX
        if (!fullLocalTravel.isFinite()) return null
        return (
            baselineX + fullLocalTravel * CcTransitionPolicy.geometryProgress(progress)
        ).takeIf(Float::isFinite)
    }

    fun followBatteryAnchor(
        target: FloatArray,
        previousBattery: FloatArray,
        currentBattery: FloatArray,
    ): FloatArray {
        require(target.size == 6 && previousBattery.size == 6 && currentBattery.size == 6)
        return target.copyOf().apply {
            this[0] += currentBattery[0] - previousBattery[0]
            this[1] += currentBattery[1] - previousBattery[1]
        }
    }

    fun aggregateAnchors(
        bounds: StatusPainter.TransitionBounds,
        rows: ParticipantVisualSnapshot.DualMobileRows,
        widthCompensation: Float,
        heightCompensation: Float,
        dotDiameter: Float,
    ): FloatArray {
        val top = rows.top.first()
        val bottom = rows.bottom.first()
        val topShape = rows.topShape
        val bottomShape = rows.bottomShape
        fun y(
            rect: ParticipantVisualSnapshot.NormalizedRect,
            shape: ParticipantVisualSnapshot.RowShape,
        ): Float {
            val normalized = if (shape == ParticipantVisualSnapshot.RowShape.DOTS) {
                rect.centerY
            } else {
                rect.bottom
            }
            val position = bounds.centerY +
                (normalized - 0.5f) * bounds.height * heightCompensation
            return if (shape == ParticipantVisualSnapshot.RowShape.BARS) {
                position - dotDiameter / 2f
            } else position
        }
        return floatArrayOf(
            bounds.centerX +
                ((top.centerX + bottom.centerX) / 2f - 0.5f) *
                    bounds.width * widthCompensation,
            y(top, topShape),
            y(bottom, bottomShape),
        )
    }

    fun relativeGeometryWidth(target: FloatArray, current: FloatArray): Float? =
        relativeAxisRatio(target, current, firstIndex = 2)

    fun relativeGeometryHeight(target: FloatArray, current: FloatArray): Float? =
        relativeAxisRatio(target, current, firstIndex = 4)

    private fun relativeAxisRatio(
        target: FloatArray,
        current: FloatArray,
        firstIndex: Int,
    ): Float? {
        require(target.size == 6)
        require(current.size == 6)
        val targetLength = vectorLength(target[firstIndex], target[firstIndex + 1])
        val currentLength = vectorLength(current[firstIndex], current[firstIndex + 1])
        if (targetLength <= 0f || currentLength <= 0f) return null
        return targetLength / currentLength
    }

    internal fun vectorLength(
        x: Float,
        y: Float,
    ): Float = sqrt(x * x + y * y)
}
