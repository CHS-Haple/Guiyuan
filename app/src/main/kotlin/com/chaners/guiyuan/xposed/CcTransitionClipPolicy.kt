package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.finiteIn

internal object CcTransitionClipPolicy {
    fun horizontalClipBounds(
        bounds: StatusPainter.TransitionBounds,
        visibleFraction: Float,
        anchorRight: Boolean,
    ): FloatArray? {
        val (left, top, right, bottom) = bounds
        if (
            !left.isFinite() ||
            !top.isFinite() ||
            !right.isFinite() ||
            !bottom.isFinite() ||
            right <= left ||
            bottom <= top
        ) {
            return null
        }
        val fraction = visibleFraction.finiteIn(0f, 1f, 0f)
        if (fraction <= 0f) return null
        if (fraction >= 1f) return floatArrayOf(left, top, right, bottom)
        val width = (right - left) * fraction
        return if (anchorRight) {
            floatArrayOf(right - width, top, right, bottom)
        } else {
            floatArrayOf(left, top, left + width, bottom)
        }
    }

    fun expandedClipBounds(
        bounds: StatusPainter.TransitionBounds,
        widthScale: Float,
        heightScale: Float,
    ): StatusPainter.TransitionBounds {
        val resolvedWidthScale =
            widthScale
                .takeIf { it.isFinite() && it > 0f }
                ?.coerceAtLeast(1f)
                ?: 1f
        val resolvedHeightScale =
            heightScale
                .takeIf { it.isFinite() && it > 0f }
                ?.coerceAtLeast(1f)
                ?: 1f
        val halfWidth = bounds.width * resolvedWidthScale / 2f
        val halfHeight = bounds.height * resolvedHeightScale / 2f
        return StatusPainter.TransitionBounds(
            left = bounds.centerX - halfWidth,
            top = bounds.centerY - halfHeight,
            right = bounds.centerX + halfWidth,
            bottom = bounds.centerY + halfHeight,
        )
    }

    fun semanticFallbackBounds(
        preferredChildEntries: List<String>,
        isRtl: Boolean,
    ): StatusPainter.TransitionNormalizedBounds? {
        val logical =
            when {
                preferredChildEntries.contains("mobile_signal") ->
                    StatusPainter.TransitionNormalizedBounds(
                        left = 0.48f,
                        top = 0f,
                        right = 1f,
                        bottom = 1f,
                    )

                preferredChildEntries.contains("wifi_signal") ->
                    StatusPainter.TransitionNormalizedBounds(
                        left = 0f,
                        top = 0f,
                        right = 1f,
                        bottom = 1f,
                    )

                else -> null
            } ?: return null
        if (!isRtl || (logical.left == 0f && logical.right == 1f)) {
            return logical
        }
        return StatusPainter.TransitionNormalizedBounds(
            left = 1f - logical.right,
            top = logical.top,
            right = 1f - logical.left,
            bottom = logical.bottom,
        )
    }

}
