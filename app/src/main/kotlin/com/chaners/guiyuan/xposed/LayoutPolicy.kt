package com.chaners.guiyuan.xposed

import kotlin.math.ceil

internal enum class RenderMode {
    PROJECTED,
    NATIVE_ONLY,
}

internal enum class MotionOwnership {
    NONE,
    COMBINED_STATUS,
    SYSTEM_UI,
}

internal data class LayoutConfig(
    val baseVisualSidePx: Float,
    val baseNeighborGapPx: Float,
    val userScale: Float,
) {
    init {
        require(baseVisualSidePx > 0f && baseVisualSidePx.isFinite())
        require(baseNeighborGapPx >= 0f && baseNeighborGapPx.isFinite())
        require(userScale > 0f && userScale.isFinite())
    }
}

internal data class HostLayout(
    val hostHeightPx: Float,
    val endAnchorPx: Float,
    val nativeSlotWidthPx: Float,
    val renderMode: RenderMode,
    val motionOwnership: MotionOwnership,
) {
    init {
        require(hostHeightPx > 0f && hostHeightPx.isFinite())
        require(endAnchorPx.isFinite())
        require(nativeSlotWidthPx >= 0f && nativeSlotWidthPx.isFinite())
    }
}

internal data class ResolvedLayout(
    val renderCombined: Boolean,
    val visualSidePx: Float,
    val neighborGapPx: Float,
    val requestedSlotWidthPx: Float,
    val appliedSlotWidthPx: Float,
    val visualLeftPx: Float,
    val visualTopPx: Float,
    val visualRightPx: Float,
    val visualBottomPx: Float,
    val slotLeftPx: Float,
    val slotRightPx: Float,
    val motionOwnership: MotionOwnership,
)

internal object LayoutPolicy {
    fun resolve(
        settings: LayoutConfig,
        host: HostLayout,
    ): ResolvedLayout {
        val visualSide = settings.baseVisualSidePx * settings.userScale
        val neighborGap = settings.baseNeighborGapPx * settings.userScale
        val requestedSlotWidth = visualSide + neighborGap

        val appliedSlotWidth = host.nativeSlotWidthPx

        val visualRight = host.endAnchorPx
        val visualLeft = visualRight - visualSide
        val visualTop = (host.hostHeightPx - visualSide) / 2f
        val visualBottom = visualTop + visualSide
        val slotRight = host.endAnchorPx
        val slotLeft = slotRight - appliedSlotWidth

        return ResolvedLayout(
            renderCombined = host.renderMode != RenderMode.NATIVE_ONLY,
            visualSidePx = visualSide,
            neighborGapPx = neighborGap,
            requestedSlotWidthPx = requestedSlotWidth,
            appliedSlotWidthPx = appliedSlotWidth,
            visualLeftPx = visualLeft,
            visualTopPx = visualTop,
            visualRightPx = visualRight,
            visualBottomPx = visualBottom,
            slotLeftPx = slotLeft,
            slotRightPx = slotRight,
            motionOwnership = host.motionOwnership,
        )
    }
}

internal object CompactReservationPolicy {
    /**
     * Painter shrink is centered inside the stable Battery carrier. Native peers only
     * reserve through the scaled visual's leading edge; the end-side transparent inset
     * stays inside the carrier. Ceil keeps the reservation outside visible pixels.
     */
    fun resolveCenteredVisualWidth(
        baseSlotWidthPx: Int,
        userScale: Float,
    ): Int {
        val base = baseSlotWidthPx.coerceAtLeast(0)
        if (base == 0) return 0
        val scale =
            userScale
                .takeIf(Float::isFinite)
                ?.coerceIn(0f, 1f)
                ?: 1f
        return ceil(base * (1f + scale) / 2f)
            .toInt()
            .coerceIn(0, base)
    }
}

internal object HomeLayoutResolver {
    fun resolve(
        hostWidthPx: Int,
        hostHeightPx: Int,
        baseCarrierWidthPx: Int,
        isRtl: Boolean,
    ): ResolvedLayout? =
        SteadyLayoutResolver.resolve(
            hostWidthPx = hostWidthPx,
            hostHeightPx = hostHeightPx,
            baseCarrierWidthPx = baseCarrierWidthPx,
            isRtl = isRtl,
        )
}

internal object SteadyLayoutResolver {
    fun resolve(
        hostWidthPx: Int,
        hostHeightPx: Int,
        baseCarrierWidthPx: Int,
        isRtl: Boolean,
    ): ResolvedLayout? {
        if (hostWidthPx <= 0 || hostHeightPx <= 0 || baseCarrierWidthPx <= 0) {
            return null
        }
        val carrierWidth = baseCarrierWidthPx.coerceAtMost(hostWidthPx)
        return LayoutPolicy.resolve(
            settings =
                LayoutConfig(
                    baseVisualSidePx = minOf(carrierWidth, hostHeightPx).toFloat(),
                    baseNeighborGapPx = 0f,
                    userScale = 1f,
                ),
            host =
                HostLayout(
                    hostHeightPx = hostHeightPx.toFloat(),
                    endAnchorPx = if (isRtl) carrierWidth.toFloat() else hostWidthPx.toFloat(),
                    nativeSlotWidthPx = carrierWidth.toFloat(),
                    renderMode = RenderMode.PROJECTED,
                    motionOwnership = MotionOwnership.SYSTEM_UI,
                ),
        )
    }
}
