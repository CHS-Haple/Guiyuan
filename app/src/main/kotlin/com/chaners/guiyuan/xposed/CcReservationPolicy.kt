package com.chaners.guiyuan.xposed

import kotlin.math.min

internal object CcReservationPolicy {
    fun usesRevealReservation(sourceScene: SourceScene): Boolean =
        when (sourceScene) {
            SourceScene.HOME,
            SourceScene.KEYGUARD,
            -> true
            SourceScene.UNKNOWN -> false
        }

    fun reservationSourceChanged(
        previous: RenderModel,
        current: RenderModel,
        visualChanged: Boolean,
    ): Boolean =
        visualChanged ||
            previous.centerIndicator != current.centerIndicator ||
            previous.batteryPercent != current.batteryPercent ||
            previous.charging != current.charging ||
            previous.chargingIconResId != current.chargingIconResId ||
            previous.mobileLevel != current.mobileLevel ||
            previous.mobileUnavailableMark != current.mobileUnavailableMark ||
            previous.effectiveDataSubscriptionId != current.effectiveDataSubscriptionId ||
            previous.dualMobileLevels != current.dualMobileLevels

    data class ReservationSpan(
        val sourceLeft: Float,
        val sourceRight: Float,
        val targetLeft: Float,
        val targetRight: Float,
    )

    fun resolveReservationWidth(
        compactWidthPx: Int,
        spans: List<ReservationSpan>,
        progress: Float,
    ): Int {
        val compact = compactWidthPx.coerceAtLeast(0)
        if (compact == 0) return 0
        val p = CcTransitionPolicy.geometryProgress(progress)
        var peerSideLeft = -compact.toFloat()
        spans.forEach { span ->
            val currentLeft =
                span.sourceLeft +
                    (span.targetLeft - span.sourceLeft) * p
            peerSideLeft = min(peerSideLeft, currentLeft)
        }
        // paddingEnd reserves only space toward the native peer icons.
        // End-side drawing overflow is not extra native peer occupancy.
        return kotlin.math.ceil(-peerSideLeft).toInt()
    }


}
