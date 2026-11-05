package com.chaners.guiyuan.xposed

import kotlin.math.ceil

internal data class SteadyLayout(
    val left: Int,
    val right: Int,
    val visualWidth: Int,
)

internal object SteadyLayoutResolver {
    fun resolve(
        hostWidthPx: Int,
        hostHeightPx: Int,
        baseCarrierWidthPx: Int,
        isRtl: Boolean,
    ): SteadyLayout? {
        if (hostWidthPx <= 0 || hostHeightPx <= 0 || baseCarrierWidthPx <= 0) {
            return null
        }
        val carrier = baseCarrierWidthPx.coerceAtMost(hostWidthPx)
        val right = if (isRtl) carrier else hostWidthPx
        return SteadyLayout(
            left = right - carrier,
            right = right,
            // The visual is capped by host height; the native carrier is not.
            visualWidth = minOf(carrier, hostHeightPx),
        )
    }
}

/** The exact native Home privacy chip occupies the BatteryContainer end only in two states. */
internal object HomePrivacyLayoutPolicy {
    fun occupiesEnd(nativeStateName: String?): Boolean =
        nativeStateName == "START_SHOW_PRIVACY" ||
            nativeStateName == "COMPLETE_SHOW_PRIVACY"
}

/** Reads the *already laid out* native status-icons edge, not an estimated chip width. */
internal object HomePrivacyAnchorPolicy {
    fun resolve(
        hostWidthPx: Int,
        hostHeightPx: Int,
        baseCarrierWidthPx: Int,
        statusIconsEndPx: Int,
        isRtl: Boolean,
    ): SteadyLayout? {
        val normal = SteadyLayoutResolver.resolve(
            hostWidthPx, hostHeightPx, baseCarrierWidthPx, isRtl,
        ) ?: return null
        val left = if (isRtl) statusIconsEndPx else statusIconsEndPx - baseCarrierWidthPx
        val right = left + baseCarrierWidthPx
        if (left < 0 || right > hostWidthPx) return null
        return SteadyLayout(left = left, right = right, visualWidth = normal.visualWidth)
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
