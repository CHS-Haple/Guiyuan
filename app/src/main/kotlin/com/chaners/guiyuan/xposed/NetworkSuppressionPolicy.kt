package com.chaners.guiyuan.xposed

internal object NetworkSuppressionPolicy {
    fun suppressMobile(
        airplaneMode: Boolean?,
        presentation: NativePresentationResolver.Snapshot?,
        wasSuppressed: Boolean,
    ): Boolean {
        if (airplaneMode == true) {
            return true
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
}
