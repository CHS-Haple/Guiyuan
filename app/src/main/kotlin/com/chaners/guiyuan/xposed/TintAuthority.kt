package com.chaners.guiyuan.xposed

internal object TintAuthority {
    fun resolveStatusIconEvent(
        nativeTintObserved: Boolean,
        liveStatusIconTint: Int?,
    ): TintState? {
        if (nativeTintObserved) return null
        val tint = visible(liveStatusIconTint) ?: return null
        return TintState(appliedTint = tint, statusIconTint = tint)
    }

    fun rebaseTransferred(
        transferred: TintState,
        liveStatusIconTint: Int?,
    ): TintState =
        resolveStatusIconEvent(
            nativeTintObserved = false,
            liveStatusIconTint = liveStatusIconTint,
        ) ?: transferred.copy(
            statusIconTint =
                visible(transferred.statusIconTint)
                    ?: visible(transferred.appliedTint),
        )

    private fun visible(color: Int?): Int? =
        color?.takeIf { it ushr 24 != 0 }
}
