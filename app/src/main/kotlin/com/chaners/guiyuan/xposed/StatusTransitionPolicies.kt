package com.chaners.guiyuan.xposed

import kotlin.math.min
import kotlin.math.roundToInt

// Transition math stays stateless; StatusPainter owns drawing only.

internal object MobileTypeTransitionPolicy {
    fun resolveWeight(
        sourceWeight: Int,
        targetWeight: Int?,
        progress: Float,
    ): Int {
        val source = sourceWeight.coerceIn(1, 1000)
        val target = targetWeight?.coerceIn(1, 1000) ?: return source
        val normalized = progress.coerceIn(0f, 1f)
        return (source + (target - source) * normalized)
            .roundToInt()
            .coerceIn(1, 1000)
    }
}

internal object TransitionTypographyPolicy {
    private const val TARGET_STYLE_START = 0.42f
    private const val TARGET_STYLE_COMPLETE = 0.88f

    fun styleProgress(progress: Float): Float {
        val normalized =
            (
                (progress.coerceIn(0f, 1f) - TARGET_STYLE_START) /
                    (TARGET_STYLE_COMPLETE - TARGET_STYLE_START)
            ).coerceIn(0f, 1f)
        return normalized * normalized * (3f - 2f * normalized)
    }
}

internal object BatteryNumberFollowerPolicy {
    private const val CHARGING_HIDE_COMPLETE_RING_LIFETIME = 0.40f
    private const val CHARGING_TARGET_TRAVEL_COMPLETE = 0.80f
    private const val CHARGING_TARGET_REVEAL_START = 0.85f
    private const val CHARGING_TARGET_REVEAL_COMPLETE = 0.90f

    private const val chargingHideStartProgress = 0f
    private val chargingHideEndProgress =
        firstProgressAtOrAboveRingLifetime(CHARGING_HIDE_COMPLETE_RING_LIFETIME)
    private val chargingTargetRevealComplete =
        CHARGING_TARGET_REVEAL_COMPLETE

    fun chargingVisibleFraction(
        progress: Float,
        targetAvailable: Boolean,
    ): Float {
        val sourceVisible = chargingSourceVisibleFraction(progress)
        if (sourceVisible > 0f) return sourceVisible
        if (!targetAvailable) return 0f
        val reveal =
            (
                (progress.coerceIn(0f, 1f) - CHARGING_TARGET_REVEAL_START) /
                    (chargingTargetRevealComplete - CHARGING_TARGET_REVEAL_START)
            ).coerceIn(0f, 1f)
        return smooth(reveal)
    }

    internal fun chargingSourceVisibleFraction(progress: Float): Float {
        val ringLifetime = chargingRingLifetimeProgress(progress)
        return (
            1f -
                ringLifetime /
                    CHARGING_HIDE_COMPLETE_RING_LIFETIME
        ).coerceIn(0f, 1f)
    }

    fun chargingMotionProgress(progress: Float): Float {
        // Source and number remain one visual group while the charging glyph
        // is clipped directly against the ring-retract lifetime. Clipping starts
        // with retract and completes at the device-calibrated visual midpoint
        // (40% lifetime; ~32% retained arc under the current front-loaded curve).
        // Target travel begins only after that boundary.
        if (chargingSourceVisibleFraction(progress) > 0f) return 0f
        val hiddenTravel =
            (
                (progress.coerceIn(0f, 1f) - chargingHideEndProgress) /
                    (CHARGING_TARGET_TRAVEL_COMPLETE - chargingHideEndProgress)
            ).coerceIn(0f, 1f)
        return smooth(hiddenTravel)
    }

    internal fun chargingRingLifetimeProgress(progress: Float): Float =
        BatteryRingTransitionPolicy.transitionProgress(progress)

    internal fun chargingRingRemaining(progress: Float): Float =
        BatteryRingTransitionPolicy.remainingFraction(
            chargingRingLifetimeProgress(progress),
        )

    internal fun sourceHideWindow(): Pair<Float, Float> =
        Pair(chargingHideStartProgress, chargingHideEndProgress)

    internal fun targetRevealWindow(): Pair<Float, Float> =
        Pair(CHARGING_TARGET_REVEAL_START, chargingTargetRevealComplete)

    private fun firstProgressAtOrAboveRingLifetime(threshold: Float): Float {
        var low = 0f
        var high = 1f
        repeat(12) {
            val mid = (low + high) / 2f
            if (chargingRingLifetimeProgress(mid) >= threshold) {
                high = mid
            } else {
                low = mid
            }
        }
        return high
    }

    private fun smooth(value: Float): Float =
        value * value * (3f - 2f * value)
}

internal object MobileSignalMorphPolicy {
    private const val STABLE_MAX_BAR_HEIGHT = 54f
    private val BAR_HEIGHT_RATIOS = floatArrayOf(0.56f, 0.70f, 0.84f, 1f)

    fun rowProgress(shapeProgress: Float): Float =
        smoothPhase(
            value = shapeProgress,
            start = 0f,
            end = 0.5f,
        )

    fun barProgress(shapeProgress: Float): Float =
        smoothPhase(
            value = shapeProgress,
            start = 0.5f,
            end = 1f,
        )

    fun outerSimilarityScale(
        targetWidthRatio: Float?,
        targetHeightRatio: Float?,
    ): Float {
        val width =
            targetWidthRatio
                ?.takeIf { it.isFinite() && it > 0f }
                ?: 1f
        val height =
            targetHeightRatio
                ?.takeIf { it.isFinite() && it > 0f }
                ?: 1f
        return min(width, height).coerceAtMost(1f).coerceAtLeast(0.001f)
    }

    fun exactTargetAxisCompensation(
        targetAxisRatio: Float?,
        outerScale: Float,
    ): Float {
        val target =
            targetAxisRatio
                ?.takeIf { it.isFinite() && it > 0f }
                ?: return 1f
        val scale =
            outerScale
                .takeIf { it.isFinite() && it > 0f }
                ?: 1f
        return (target / scale).coerceAtLeast(0.001f)
    }

    fun targetMaxBarHeight(
        sourceBoundsHeight: Float,
        diameter: Float,
        targetHeightRatio: Float?,
    ): Float {
        val nativeCap =
            targetHeightRatio
                ?.takeIf { ratio -> ratio.isFinite() && ratio > 0f }
                ?.let { ratio ->
                    val targetOpticalHeight = sourceBoundsHeight * ratio
                    val roundCapAllowance = diameter / 2f
                    (targetOpticalHeight - roundCapAllowance)
                        .coerceAtLeast(diameter)
                }
                ?: STABLE_MAX_BAR_HEIGHT
        return min(STABLE_MAX_BAR_HEIGHT, nativeCap)
            .coerceAtLeast(diameter)
    }

    fun targetBarHeight(
        index: Int,
        maxBarHeight: Float,
        diameter: Float,
    ): Float {
        val ratio = BAR_HEIGHT_RATIOS.getOrElse(index) { 1f }
        return (maxBarHeight * ratio).coerceAtLeast(diameter)
    }

    fun sharedBottomExpansion(
        maxBarHeight: Float,
        diameter: Float,
        barProgress: Float,
    ): Float {
        val sharedDownwardGrowth =
            (maxBarHeight - diameter)
                .coerceAtLeast(0f) / 2f
        return sharedDownwardGrowth * barProgress.coerceIn(0f, 1f)
    }

    private fun smoothPhase(
        value: Float,
        start: Float,
        end: Float,
    ): Float {
        val normalized =
            if (!value.isFinite() || end <= start) {
                0f
            } else {
                ((value - start) / (end - start)).coerceIn(0f, 1f)
            }
        return normalized * normalized * (3f - 2f * normalized)
    }
}
