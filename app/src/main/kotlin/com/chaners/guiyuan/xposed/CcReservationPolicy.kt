package com.chaners.guiyuan.xposed

import kotlin.math.min
import kotlin.math.roundToInt

internal object CcReservationPolicy {
    fun usesSemanticTransitionReservation(sourceScene: SourceScene): Boolean =
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
        var left = -compact.toFloat()
        var right = 0f
        spans.forEach { span ->
            val currentLeft =
                span.sourceLeft +
                    (span.targetLeft - span.sourceLeft) * p
            val currentRight =
                span.sourceRight +
                    (span.targetRight - span.sourceRight) * p
            left = min(left, currentLeft)
            right = maxOf(right, currentRight)
        }
        return kotlin.math.ceil((right - left).coerceAtLeast(compact.toFloat())).toInt()
    }

    fun resolveFinalReservationWidth(
        compactWidthPx: Int,
        spans: List<ReservationSpan>,
    ): Int =
        resolveReservationWidth(
            compactWidthPx = compactWidthPx,
            spans = spans,
            progress = 1f,
        )

    fun resolveTransitionReservationWidth(
        compactWidthPx: Int,
        spans: List<ReservationSpan>,
        progress: Float,
    ): Int {
        val compact = compactWidthPx.coerceAtLeast(0)
        if (compact == 0) return 0
        val finalWidth =
            resolveFinalReservationWidth(
                compactWidthPx = compact,
                spans = spans,
            )
        val p = CcTransitionPolicy.geometryProgress(progress)
        return (
            compact +
                (finalWidth - compact) * p
            ).roundToInt().coerceAtLeast(compact)
    }

    fun resolveBatteryIslandNativePeerReservationWidth(
        compactWidthPx: Int,
        spans: List<ReservationSpan>,
        semanticWidthPx: Int,
        progress: Float,
        targetEndOffsetPx: Float = 0f,
    ): Int {
        val compact = compactWidthPx.coerceAtLeast(0)
        val semantic = semanticWidthPx.coerceAtLeast(compact)
        if (compact == 0) return 0
        val targetEndOffset =
            targetEndOffsetPx
                .takeIf(Float::isFinite)
                ?: 0f
        val p = CcTransitionPolicy.geometryProgress(progress)
        var left = -compact.toFloat()
        spans.forEach { span ->
            val currentLeft =
                span.sourceLeft +
                    (span.targetLeft + targetEndOffset - span.sourceLeft) * p
            left = min(left, currentLeft)
        }

        // statusIcons.paddingEnd reserves space only on the peer side of the
        // live QS_FAKE end edge (logical x=0). A projected Guiyuan span may
        // extend to x>0 while converging on the final QS surface; that
        // end-side extent is drawable occupancy, not native-peer intrusion.
        return kotlin.math
            .ceil((-left.coerceAtMost(0f)).coerceAtLeast(compact.toFloat()))
            .toInt()
            .coerceAtMost(semantic)
    }
}
