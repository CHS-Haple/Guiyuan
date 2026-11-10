package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.battery.BatteryRingTransitionPolicy
import kotlin.math.roundToInt

internal object CcTintPolicy {
    fun selectNativeTransitionTint(
        statusIconPeerTint: Int?,
        cachedTint: Int?,
    ): Int? {
        fun valid(color: Int?): Int? =
            color?.takeIf { candidate -> candidate ushr 24 != 0 }
        return valid(statusIconPeerTint)
            ?: valid(cachedTint)
    }

    fun transitionTintProgress(progress: Float): Float {
        val ringPhase =
            BatteryRingTransitionPolicy
                .transitionProgress(progress)
                .coerceIn(0f, 1f)
        return ringPhase * ringPhase * (3f - 2f * ringPhase)
    }

    fun resolveTransitionTint(
        source: Int,
        target: Int,
        progress: Float,
        tinted: Boolean,
        transitionEnabled: Boolean,
    ): Int =
        when {
            !tinted -> target
            !transitionEnabled -> source
            else ->
                interpolateColor(
                    source = source,
                    target = target,
                    progress = progress,
                )
        }

    fun interpolateColor(
        source: Int,
        target: Int,
        progress: Float,
    ): Int {
        val p = transitionTintProgress(progress)
        fun channel(
            fromShift: Int,
            toShift: Int = fromShift,
        ): Int {
            val from = source ushr fromShift and 0xff
            val to = target ushr toShift and 0xff
            return (from + (to - from) * p)
                .roundToInt()
                .coerceIn(0, 255)
        }
        return (
            channel(24) shl 24 or
                (channel(16) shl 16) or
                (channel(8) shl 8) or
                channel(0)
        )
    }
}
