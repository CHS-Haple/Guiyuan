package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.finiteIn
import com.chaners.guiyuan.xposed.battery.BatteryRingTransitionPolicy

internal object CcTransitionPolicy {
    fun geometryProgress(raw: Float): Float = raw.finiteIn(0f, 1f, 0f)

    fun retargetWeight(progress: Float, anchor: Float): Float {
        val p = geometryProgress(progress)
        val at = geometryProgress(anchor)
        if (at <= 0f || at >= 1f) return 0f
        return if (p <= at) p / at else (1f - p) / (1f - at)
    }

    fun retargetShift(
        priorShift: Float,
        priorAnchor: Float?,
        progress: Float,
        nativeDelta: Float,
    ): Float {
        val retained = priorAnchor?.let {
            priorShift * retargetWeight(progress, it)
        } ?: 0f
        return retained - nativeDelta
    }

    fun handoffMotionProgress(
        expansionProgress: Float,
        finalAppearanceAlpha: Float,
        finalAppearanceActive: Boolean,
    ): Float {
        val expansion = geometryProgress(expansionProgress)
        if (!finalAppearanceActive) return expansion
        val appearance = finalAppearanceAlpha.finiteIn(0f, 1f, 0f)
        return expansion + (1f - expansion) * appearance
    }

    fun mobileSignalShapeProgress(rawProgress: Float): Float {
        val p = geometryProgress(rawProgress)
        return p * p
    }

    fun horizontalExitDirection(
        source: FloatArray,
        target: FloatArray,
    ): BatteryRingTransitionPolicy.ExitDirection {
        if (source.size != 6 || target.size != 6) {
            return BatteryRingTransitionPolicy.ExitDirection.NONE
        }
        val sourceCenterX = source[0] + (source[2] + source[4]) * 0.5f
        val targetCenterX = target[0] + (target[2] + target[4]) * 0.5f
        if (!sourceCenterX.isFinite() || !targetCenterX.isFinite()) {
            return BatteryRingTransitionPolicy.ExitDirection.NONE
        }
        return when {
            targetCenterX - sourceCenterX < -0.5f ->
                BatteryRingTransitionPolicy.ExitDirection.LEFT
            targetCenterX - sourceCenterX > 0.5f ->
                BatteryRingTransitionPolicy.ExitDirection.RIGHT
            else -> BatteryRingTransitionPolicy.ExitDirection.NONE
        }
    }

    fun batteryRingExitDirection(
        liveCenterDirection: BatteryRingTransitionPolicy.ExitDirection,
        nativeBatteryIslandActive: Boolean,
        targetRowRtl: Boolean,
    ): BatteryRingTransitionPolicy.ExitDirection {
        // The ring must retain its exit side when the native center glyph
        // disappears. Battery-Island reflow also makes the live X unreliable.
        if (
            !nativeBatteryIslandActive &&
            liveCenterDirection != BatteryRingTransitionPolicy.ExitDirection.NONE
        ) {
            return liveCenterDirection
        }
        return if (targetRowRtl) {
            BatteryRingTransitionPolicy.ExitDirection.RIGHT
        } else {
            BatteryRingTransitionPolicy.ExitDirection.LEFT
        }
    }

    // The carried dot pair shares one translation before its native rows split.
    fun dualMobileRowMotionGeometry(
        source: FloatArray,
        target: FloatArray,
        groupTravelX: Float,
        groupTravelY: Float,
        progress: Float,
        scalePolicy: StatusPainter.TransitionScalePolicy,
    ): FloatArray {
        require(source.size == 6 && target.size == 6)
        val together = MobileSignalStagePolicy.lockstepProgress(progress)
        val paired = source.copyOf().apply {
            this[0] += groupTravelX * together
            this[1] += groupTravelY * together
        }
        val normal = CcTransitionMorphPolicy.interpolateSimilarityGeometry(
            source = source,
            target = target,
            progress = progress,
            scalePolicy = scalePolicy,
        )
        return CcTransitionMorphPolicy.interpolateGeometry(
            source = paired,
            target = normal,
            progress = MobileSignalStagePolicy.moveProgress(progress),
        )
    }


    fun interpolateCarriedSourceToRootTarget(
        source: FloatArray,
        target: FloatArray,
        sourceCarrier: FloatArray,
        currentCarrier: FloatArray,
        progress: Float,
        scalePolicy: StatusPainter.TransitionScalePolicy,
    ): FloatArray {
        require(
            source.size == 6 &&
                target.size == 6 &&
                sourceCarrier.size == 6 &&
                currentCarrier.size == 6,
        )
        val carriedSource =
            CcCarrierGeometryPolicy.rebaseSourceToCurrentCarrier(
                source = source,
                sourceCarrier = sourceCarrier,
                currentCarrier = currentCarrier,
            )
        return CcTransitionMorphPolicy.interpolateSimilarityGeometry(
            source = carriedSource,
            target = target,
            progress = geometryProgress(progress),
            scalePolicy = scalePolicy,
        )
    }

}
