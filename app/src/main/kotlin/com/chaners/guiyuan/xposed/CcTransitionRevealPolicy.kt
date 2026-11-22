package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.finiteIn
import kotlin.math.min
import kotlin.math.sqrt

// Stateless reveal decisions. The native CC session remains the single writer.
internal object CcTransitionRevealPolicy {
    private const val LATENT_REVEAL_COMPLETE_FRACTION = 0.35f

    fun unmatchedExitOpacity(rawProgress: Float): Float {
        val remaining = 1f - CcTransitionPolicy.geometryProgress(rawProgress)
        return remaining * remaining * remaining
    }

    fun latentReservationProgress(
        compactWidthPx: Int,
        currentReservationPx: Int,
        requiredReservationPx: Int,
        visualWidthPx: Float,
    ): Float {
        if (!visualWidthPx.isFinite() || visualWidthPx <= 0f) return 0f
        val compact = compactWidthPx.coerceAtLeast(0).toFloat()
        val current = currentReservationPx.coerceAtLeast(0).toFloat()
        val required = requiredReservationPx.coerceAtLeast(compactWidthPx).toFloat()
        val start = maxOf(compact, required - visualWidthPx)
        if (required <= start) {
            return if (current >= required) 1f else 0f
        }
        val normalized =
            ((current - start) / (required - start))
                .coerceIn(0f, 1f)
        return normalized * normalized * (3f - 2f * normalized)
    }

    fun latentRevealVisibleFraction(
        current: FloatArray,
        target: FloatArray,
        visualExtent: Float,
        reservationProgress: Float,
    ): Float {
        require(current.size == 6 && target.size == 6)
        if (
            !visualExtent.isFinite() ||
            visualExtent <= 0f
        ) return 0f

        val reservation = reservationProgress.finiteIn(0f, 1f, 0f)
        if (reservation <= 0f) return 0f

        val deltaX = target[0] - current[0]
        val deltaY = target[1] - current[1]
        val remainingDistance =
            sqrt(deltaX * deltaX + deltaY * deltaY)
        if (remainingDistance >= visualExtent) return 0f

        val proximityProgress =
            (1f - remainingDistance / visualExtent)
                .coerceIn(0f, 1f)
        val proximity =
            acceleratedLatentRevealProgress(proximityProgress)
        val occupancy =
            acceleratedLatentRevealProgress(reservation)
        return min(proximity, occupancy)
    }

    fun acceleratedLatentRevealProgress(progress: Float): Float {
        val normalized = progress.finiteIn(0f, 1f, 0f) / LATENT_REVEAL_COMPLETE_FRACTION
        val phase = normalized.coerceIn(0f, 1f)
        return phase * phase * (3f - 2f * phase)
    }

}
