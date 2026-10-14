package com.chaners.guiyuan.xposed.network

import com.chaners.guiyuan.xposed.NativePresentationResolver

internal object NetworkSuppressionPolicy {
    // Pure suppression/tint decisions stay here; the owner keeps slot identity and View lifecycle.
    fun suppressMobile(
        airplaneMode: Boolean?,
        presentation: NativePresentationResolver.Snapshot?,
        wasSuppressed: Boolean,
    ): Boolean {
        if (airplaneMode == true) {
            return true
        }
        // A known incomplete binding must override the temporary sticky fallback.
        if (presentation?.platformBindingIncomplete == true) {
            return false
        }
        if (presentation?.nativeMobileReplacementReady == true) {
            return true
        }
        if (
            airplaneMode == false &&
            wasSuppressed &&
            isDuplicateRootRebindWindow(presentation)
        ) {
            return true
        }
        return airplaneMode == false &&
            wasSuppressed &&
            presentation?.mode == NativePresentationResolver.Mode.UNKNOWN
    }

    internal fun isDuplicateRootRebindWindow(
        presentation: NativePresentationResolver.Snapshot?,
    ): Boolean {
        presentation ?: return false
        if (presentation.mode != NativePresentationResolver.Mode.DUAL_SEPARATE) {
            return false
        }
        val activeSubscriptions = presentation.activeSubscriptionIds.size
        if (activeSubscriptions < 2) {
            return false
        }
        return presentation.boundRoots > activeSubscriptions ||
            presentation.activeBoundRoots > activeSubscriptions
    }

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
