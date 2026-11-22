package com.chaners.guiyuan.xposed

import kotlin.math.min

internal object CcTransitionMorphPolicy {
    fun interpolateGeometry(
        source: FloatArray,
        target: FloatArray,
        progress: Float,
    ): FloatArray {
        require(source.size == 6 && target.size == 6)
        val p = progress.coerceIn(0f, 1f)
        return FloatArray(6) { index ->
            source[index] + (target[index] - source[index]) * p
        }
    }

    fun interpolateSimilarityGeometry(
        source: FloatArray,
        target: FloatArray,
        progress: Float,
        scalePolicy: StatusPainter.TransitionScalePolicy,
    ): FloatArray {
        require(source.size == 6 && target.size == 6)
        val p = progress.coerceIn(0f, 1f)
        val sourceWidth = CcTransitionSpatialPolicy.vectorLength(source[2], source[3])
        val sourceHeight = CcTransitionSpatialPolicy.vectorLength(source[4], source[5])
        val targetWidth = CcTransitionSpatialPolicy.vectorLength(target[2], target[3])
        val targetHeight = CcTransitionSpatialPolicy.vectorLength(target[4], target[5])
        if (
            sourceWidth <= 0f ||
            sourceHeight <= 0f ||
            targetWidth <= 0f ||
            targetHeight <= 0f
        ) {
            return source.copyOf()
        }
        val rawTargetScale =
            min(
                targetWidth / sourceWidth,
                targetHeight / sourceHeight,
            )
        val targetScale =
            when (scalePolicy) {
                StatusPainter.TransitionScalePolicy.TARGET ->
                    rawTargetScale

                StatusPainter.TransitionScalePolicy.SHRINK_ONLY ->
                    min(rawTargetScale, 1f)
            }
        val scale = 1f + (targetScale - 1f) * p
        return floatArrayOf(
            source[0] + (target[0] - source[0]) * p,
            source[1] + (target[1] - source[1]) * p,
            source[2] * scale,
            source[3] * scale,
            source[4] * scale,
            source[5] * scale,
        )
    }


}
