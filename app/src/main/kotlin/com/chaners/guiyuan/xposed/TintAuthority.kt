package com.chaners.guiyuan.xposed

internal object TintAuthority {
    fun resolveBatteryEvent(
        batteryState: TintState,
        liveStatusIconTint: Int?,
    ): TintState {
        val statusIconTint =
            visible(liveStatusIconTint)
                ?: visible(batteryState.appliedTint)
        return batteryState.copy(statusIconTint = statusIconTint)
    }

    fun resolveStatusIconEvent(
        previous: TintState?,
        liveStatusIconTint: Int?,
    ): TintState? {
        val statusIconTint = visible(liveStatusIconTint) ?: return previous
        val appliedTint =
            previous
                ?.appliedTint
                ?.takeIf(::isVisible)
                ?: statusIconTint
        return TintState(
            appliedTint = appliedTint,
            statusIconTint = statusIconTint,
        )
    }

    fun rebaseTransferred(
        transferred: TintState,
        liveStatusIconTint: Int?,
    ): TintState {
        val statusIconTint =
            visible(liveStatusIconTint)
                ?: visible(transferred.statusIconTint)
                ?: visible(transferred.appliedTint)
        return transferred.copy(statusIconTint = statusIconTint)
    }

    private fun visible(color: Int?): Int? =
        color?.takeIf(::isVisible)

    private fun isVisible(color: Int): Boolean =
        color ushr 24 != 0
}
