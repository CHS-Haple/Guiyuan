package com.chaners.guiyuan.xposed.network

internal object StatusIconTintPolicy {
    internal fun statusIconTint(
        locationAwareTint: Int?,
        peerAppliedTint: Int?,
        managerFallbackTint: Int?,
        fallbackTint: Int?,
    ): Int? =
        locationAwareTint
            ?.takeIf(::isVisibleTint)
            ?: peerAppliedTint
                ?.takeIf(::isVisibleTint)
            ?: managerFallbackTint
                ?.takeIf(::isVisibleTint)
            ?: fallbackTint
                ?.takeIf(::isVisibleTint)

    internal fun isVisibleTint(color: Int): Boolean =
        color ushr 24 != 0
}
